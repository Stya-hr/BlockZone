package dev.stya.blockzone.game.battlezone;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Persistent baseline of a Battlezone arena, captured only by an explicit admin action. */
final class BattlezoneSceneSnapshot {
    private static final Logger LOGGER = LoggerFactory.getLogger(BattlezoneSceneSnapshot.class);
    private static final int FORMAT_VERSION = 3;
    private static final int PIECE_SIZE = 16;
    private static final int RESTORE_BLOCK_BATCH_SIZE = 1024;
    private static final long RESTORE_NANOS_PER_TICK = 5_000_000L;
    private static final long MAX_BLOCKS = 25_000_000L;
    private static final String FILE_SUFFIX = ".battlezone-scene.nbt";

    private final BattlezoneMap map;
    private CompoundTag data;
    private Operation operation;
    private boolean valid;
    private boolean lastOperationSucceeded = true;

    BattlezoneSceneSnapshot(BattlezoneMap map) {
        this.map = map;
    }

    boolean beginSave() {
        if (map.getMapArea() == null || isBusy()) {
            return false;
        }
        Bounds bounds = currentBounds();
        if (bounds.volume() > MAX_BLOCKS) {
            LOGGER.warn("Battlezone scene snapshot for map {} exceeds the {} block limit", map.getMapName(), MAX_BLOCKS);
            valid = false;
            return false;
        }
        int pieces = pieceCount(bounds);
        operation = Operation.capture(map.getServerLevel(), bounds, pieces, snapshotPath(), createInitialData(map.getServerLevel(), bounds, pieces));
        valid = false;
        lastOperationSucceeded = false;
        return true;
    }

    boolean load() {
        Path path = snapshotPath();
        if (!Files.isRegularFile(path)) {
            valid = false;
            return false;
        }
        try {
            CompoundTag loaded = NbtIo.readCompressed(path.toFile());
            if (!matchesCurrentMap(loaded) || loaded.getList("pieces", Tag.TAG_COMPOUND).size() != loaded.getInt("piece_count")) {
                LOGGER.warn("Battlezone scene snapshot for map {} does not match its dimension or area", map.getMapName());
                valid = false;
                return false;
            }
            data = loaded;
            valid = true;
            return true;
        } catch (Exception exception) {
            LOGGER.error("Failed to load Battlezone scene snapshot for map {}", map.getMapName(), exception);
            valid = false;
            return false;
        }
    }

    boolean exists() {
        return Files.isRegularFile(snapshotPath());
    }

    boolean beginRestore() {
        if (!valid || isBusy() || data == null || !matchesCurrentMap(data)) {
            return false;
        }
        operation = Operation.restore(map.getServerLevel(), currentBounds(), data);
        lastOperationSucceeded = false;
        return true;
    }

    void tick() {
        if (operation == null) {
            return;
        }
        try {
            if (operation.type == OperationType.CAPTURE) {
                tickCapture(operation);
            } else {
                tickRestore(operation);
            }
        } catch (Exception exception) {
            LOGGER.error("Battlezone scene {} failed for map {}", operation.type.description, map.getMapName(), exception);
            if (operation.type == OperationType.CAPTURE) {
                try {
                    Files.deleteIfExists(operation.temporaryPath);
                } catch (IOException cleanupException) {
                    exception.addSuppressed(cleanupException);
                }
                valid = false;
            } else {
                valid = false;
            }
            operation = null;
            lastOperationSucceeded = false;
        }
    }

    boolean isBusy() {
        return operation != null;
    }

    boolean hasValidSnapshot() {
        return valid && data != null && matchesCurrentMap(data);
    }

    boolean lastOperationSucceeded() {
        return lastOperationSucceeded;
    }

    private void tickCapture(Operation current) throws IOException {
        if (current.index < current.pieceCount) {
            Piece piece = pieceAt(current.bounds, current.index);
            current.pieces.add(capturePiece(current.level, piece));
            current.index++;
            return;
        }

        current.data.put("pieces", current.pieces);
        current.data.put("entities", captureEntities(current.level, current.bounds));
        Path parent = current.path.getParent();
        Files.createDirectories(parent);
        NbtIo.writeCompressed(current.data, current.temporaryPath.toFile());
        try {
            Files.move(current.temporaryPath, current.path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicMoveUnsupported) {
            Files.move(current.temporaryPath, current.path, StandardCopyOption.REPLACE_EXISTING);
        }
        data = current.data;
        valid = true;
        lastOperationSucceeded = true;
        operation = null;
        LOGGER.info("Saved Battlezone scene snapshot for map {}", map.getMapName());
    }

    private void tickRestore(Operation current) throws IOException {
        if (!current.restoreEntitiesRemoved) {
            removeCurrentNonPlayerEntities(current.level, current.bounds);
            current.restoreEntitiesRemoved = true;
        }
        long tickStart = System.nanoTime();
        while (current.index < current.pieceCount) {
            if (current.restoreCursor == null) {
                current.restoreCursor = createRestoreCursor(current.level, current.pieces.getCompound(current.index));
            }
            if (!restorePieceBatch(current.level, current.restoreCursor)) {
                break;
            }
            restoreBlockEntities(current.level, current.restoreCursor.blockEntities);
            finishRestoredPiece(current, current.restoreCursor);
            current.index++;
            current.restoreCursor = null;
            if (System.nanoTime() - tickStart >= RESTORE_NANOS_PER_TICK) {
                return;
            }
        }
        if (current.index < current.pieceCount) {
            return;
        }

        if (!current.lightUpdatesSettled) {
            if (current.level.getLightEngine().hasLightWork()) {
                return;
            }
            current.lightUpdatesSettled = true;
        }
        Iterator<Map.Entry<Long, LevelChunk>> refreshes = current.chunksToRefresh.entrySet().iterator();
        while (refreshes.hasNext()) {
            Map.Entry<Long, LevelChunk> entry = refreshes.next();
            refreshChunkForTrackingPlayers(current.level, entry.getValue());
            refreshes.remove();
            if (System.nanoTime() - tickStart >= RESTORE_NANOS_PER_TICK) {
                return;
            }
        }

        restoreEntities(current.level, current.data.getList("entities", Tag.TAG_COMPOUND));
        lastOperationSucceeded = true;
        operation = null;
        LOGGER.info("Restored Battlezone scene snapshot for map {}", map.getMapName());
    }

    private CompoundTag capturePiece(ServerLevel level, Piece piece) {
        ensureChunksLoaded(level, piece);
        if (piece.isWholeSection()) {
            int sectionY = piece.minY >> 4;
            LevelChunk chunk = level.getChunk(piece.minX >> 4, piece.minZ >> 4);
            int sectionIndex = level.getSectionIndex(sectionY << 4);
            CompoundTag result = piece.save();
            result.putBoolean("whole_section", true);
            result.putByteArray("section_data", writeSection(chunk.getSections()[sectionIndex]));
            result.put("block_entities", captureBlockEntities(level, piece));
            return result;
        }

        Map<BlockState, Integer> paletteIds = new HashMap<>();
        List<BlockState> palette = new ArrayList<>();
        int[] states = new int[piece.volume()];
        ListTag blockEntities = captureBlockEntities(level, piece);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int index = 0;
        for (int y = piece.minY; y <= piece.maxY; y++) {
            for (int z = piece.minZ; z <= piece.maxZ; z++) {
                for (int x = piece.minX; x <= piece.maxX; x++) {
                    pos.set(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    Integer paletteId = paletteIds.get(state);
                    if (paletteId == null) {
                        paletteId = palette.size();
                        paletteIds.put(state, paletteId);
                        palette.add(state);
                    }
                    states[index++] = paletteId;

                }
            }
        }

        CompoundTag result = piece.save();
        ListTag paletteTag = new ListTag();
        palette.forEach(state -> paletteTag.add(NbtUtils.writeBlockState(state)));
        result.put("palette", paletteTag);
        result.put("states", new IntArrayTag(states));
        result.put("block_entities", blockEntities);
        return result;
    }

    private ListTag captureBlockEntities(ServerLevel level, Piece piece) {
        ListTag blockEntities = new ListTag();
        for (int chunkX = piece.minX >> 4; chunkX <= piece.maxX >> 4; chunkX++) {
            for (int chunkZ = piece.minZ >> 4; chunkZ <= piece.maxZ >> 4; chunkZ++) {
                LevelChunk chunk = level.getChunk(chunkX, chunkZ);
                for (BlockPos pos : new ArrayList<>(chunk.getBlockEntitiesPos())) {
                    if (pos.getX() >= piece.minX && pos.getX() <= piece.maxX
                            && pos.getY() >= piece.minY && pos.getY() <= piece.maxY
                            && pos.getZ() >= piece.minZ && pos.getZ() <= piece.maxZ) {
                        BlockEntity blockEntity = level.getBlockEntity(pos);
                        if (blockEntity != null) {
                            blockEntities.add(blockEntity.saveWithFullMetadata());
                        }
                    }
                }
            }
        }
        return blockEntities;
    }

    private byte[] writeSection(LevelChunkSection section) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            section.write(buffer);
            return ByteBufUtil.getBytes(buffer);
        } finally {
            buffer.release();
        }
    }

    private RestoreCursor createRestoreCursor(ServerLevel level, CompoundTag pieceData) {
        Piece piece = Piece.load(pieceData);
        if (pieceData.getBoolean("whole_section")) {
            return RestoreCursor.section(piece, pieceData.getByteArray("section_data"),
                    pieceData.getList("block_entities", Tag.TAG_COMPOUND));
        }
        ListTag paletteTag = pieceData.getList("palette", Tag.TAG_COMPOUND);
        BlockState[] palette = new BlockState[paletteTag.size()];
        for (int index = 0; index < paletteTag.size(); index++) {
            palette[index] = NbtUtils.readBlockState(
                    level.registryAccess().lookupOrThrow(Registries.BLOCK), paletteTag.getCompound(index));
        }

        int[] states = pieceData.getIntArray("states");
        if (states.length != piece.volume()) {
            throw new IllegalStateException("Invalid Battlezone scene piece state count");
        }
        return RestoreCursor.blocks(piece, palette, states,
                pieceData.getList("block_entities", Tag.TAG_COMPOUND));
    }

    private boolean restorePieceBatch(ServerLevel level, RestoreCursor cursor) {
        if (cursor.sectionData != null) {
            restoreWholeSection(level, cursor);
            return true;
        }
        int endIndex = Math.min(cursor.stateIndex + RESTORE_BLOCK_BATCH_SIZE, cursor.states.length);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        while (cursor.stateIndex < endIndex) {
            int stateIndex = cursor.stateIndex++;
            int paletteId = cursor.states[stateIndex];
            if (paletteId < 0 || paletteId >= cursor.palette.length) {
                throw new IllegalStateException("Invalid Battlezone scene palette index " + paletteId);
            }
            int x = cursor.piece.minX + stateIndex % cursor.sizeX;
            int z = cursor.piece.minZ + stateIndex / cursor.sizeX % cursor.sizeZ;
            int y = cursor.piece.minY + stateIndex / (cursor.sizeX * cursor.sizeZ);
            pos.set(x, y, z);
            int chunkX = x >> 4;
            int chunkZ = z >> 4;
            long chunkKey = ChunkPos.asLong(chunkX, chunkZ);
            LevelChunk chunk = cursor.touchedChunks.computeIfAbsent(chunkKey, ignored -> level.getChunk(chunkX, chunkZ));
            chunk.setBlockState(pos, cursor.palette[paletteId], false);
        }
        return cursor.stateIndex >= cursor.states.length;
    }

    private void restoreWholeSection(ServerLevel level, RestoreCursor cursor) {
        Piece piece = cursor.piece;
        int sectionY = piece.minY >> 4;
        int sectionIndex = level.getSectionIndex(sectionY << 4);
        LevelChunk chunk = level.getChunk(piece.minX >> 4, piece.minZ >> 4);
        LevelChunkSection previous = chunk.getSections()[sectionIndex];
        LevelChunkSection restored = new LevelChunkSection(level.registryAccess().registryOrThrow(Registries.BIOME));
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(cursor.sectionData));
        try {
            restored.read(buffer);
        } finally {
            buffer.release();
        }

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BitSet changedPositions = new BitSet(PIECE_SIZE * PIECE_SIZE * PIECE_SIZE);
        for (int y = 0; y < PIECE_SIZE; y++) {
            for (int z = 0; z < PIECE_SIZE; z++) {
                for (int x = 0; x < PIECE_SIZE; x++) {
                    BlockState oldState = previous.getBlockState(x, y, z);
                    BlockState newState = restored.getBlockState(x, y, z);
                    if (oldState != newState) {
                        changedPositions.set((y << 8) | (z << 4) | x);
                        if (LightEngine.hasDifferentLightProperties(level,
                                pos.set(piece.minX + x, piece.minY + y, piece.minZ + z), oldState, newState)) {
                            level.getLightEngine().checkBlock(pos);
                        }
                    }
                }
            }
        }

        cursor.touchedChunks.put(chunk.getPos().toLong(), chunk);
        if (changedPositions.isEmpty()) {
            return;
        }
        for (BlockPos blockEntityPos : new ArrayList<>(chunk.getBlockEntitiesPos())) {
            if (blockEntityPos.getY() >= piece.minY && blockEntityPos.getY() <= piece.maxY) {
                chunk.removeBlockEntity(blockEntityPos);
            }
        }
        chunk.getSections()[sectionIndex] = restored;
        for (int packed = changedPositions.nextSetBit(0); packed >= 0; packed = changedPositions.nextSetBit(packed + 1)) {
            int x = packed & 15;
            int z = packed >> 4 & 15;
            int y = packed >> 8;
            BlockState state = restored.getBlockState(x, y, z);
            for (Map.Entry<Heightmap.Types, Heightmap> heightmap : chunk.getHeightmaps()) {
                heightmap.getValue().update(x, piece.minY + y, z, state);
            }
        }
        chunk.setUnsaved(true);
    }

    private void finishRestoredPiece(Operation current, RestoreCursor cursor) {
        for (LevelChunk chunk : cursor.touchedChunks.values()) {
            long chunkKey = chunk.getPos().toLong();
            if (current.lastPieceByChunk.getOrDefault(chunkKey, -1) == current.index) {
                chunk.setUnsaved(true);
                current.chunksToRefresh.put(chunkKey, chunk);
            }
        }
    }

    private void restoreBlockEntities(ServerLevel level, ListTag blockEntities) {
        for (int blockEntityIndex = 0; blockEntityIndex < blockEntities.size(); blockEntityIndex++) {
            CompoundTag blockEntityData = blockEntities.getCompound(blockEntityIndex);
            BlockPos blockEntityPos = new BlockPos(
                    blockEntityData.getInt("x"), blockEntityData.getInt("y"), blockEntityData.getInt("z"));
            BlockEntity blockEntity = level.getBlockEntity(blockEntityPos);
            if (blockEntity != null) {
                blockEntity.load(blockEntityData);
                blockEntity.setChanged();
            }
        }
    }

    private void refreshChunkForTrackingPlayers(ServerLevel level, LevelChunk chunk) {
        List<ServerPlayer> players = level.getChunkSource().chunkMap.getPlayers(chunk.getPos(), false);
        if (players.isEmpty()) {
            return;
        }
        ClientboundLevelChunkWithLightPacket packet = new ClientboundLevelChunkWithLightPacket(
                chunk, level.getLightEngine(), null, null);
        for (ServerPlayer player : players) {
            player.connection.send(packet);
        }
    }

    private ListTag captureEntities(ServerLevel level, Bounds bounds) {
        ListTag entities = new ListTag();
        for (Entity entity : level.getEntitiesOfClass(Entity.class, bounds.toAabb(), value -> !(value instanceof Player) && value.getVehicle() == null)) {
            CompoundTag saved = new CompoundTag();
            if (entity.saveAsPassenger(saved)) {
                entities.add(saved);
            }
        }
        return entities;
    }

    private void restoreEntities(ServerLevel level, ListTag entities) {
        for (int index = 0; index < entities.size(); index++) {
            EntityType.loadEntityRecursive(entities.getCompound(index), level, entity -> {
                level.addFreshEntity(entity);
                return entity;
            });
        }
    }

    private CompoundTag createInitialData(ServerLevel level, Bounds bounds, int pieces) {
        CompoundTag result = new CompoundTag();
        result.putInt("version", FORMAT_VERSION);
        result.putString("map", map.getMapName());
        result.putString("dimension", level.dimension().location().toString());
        result.put("min", NbtUtils.writeBlockPos(bounds.minPos()));
        result.put("max", NbtUtils.writeBlockPos(bounds.maxPos()));
        result.putInt("piece_count", pieces);
        return result;
    }

    private boolean matchesCurrentMap(CompoundTag snapshot) {
        Bounds bounds = currentBounds();
        return snapshot.getInt("version") == FORMAT_VERSION
                && map.getMapName().equals(snapshot.getString("map"))
                && map.getServerLevel().dimension().location().toString().equals(snapshot.getString("dimension"))
                && snapshot.contains("min", CompoundTag.TAG_COMPOUND)
                && snapshot.contains("max", CompoundTag.TAG_COMPOUND)
                && bounds.minPos().equals(NbtUtils.readBlockPos(snapshot.getCompound("min")))
                && bounds.maxPos().equals(NbtUtils.readBlockPos(snapshot.getCompound("max")))
                && snapshot.getInt("piece_count") == pieceCount(bounds);
    }

    private void removeCurrentNonPlayerEntities(ServerLevel level, Bounds bounds) {
        for (Entity entity : new ArrayList<>(level.getEntitiesOfClass(Entity.class, bounds.toAabb(), value -> !(value instanceof Player)))) {
            entity.discard();
        }
    }

    private void ensureChunksLoaded(ServerLevel level, Piece piece) {
        int minChunkX = piece.minX >> 4;
        int maxChunkX = piece.maxX >> 4;
        int minChunkZ = piece.minZ >> 4;
        int maxChunkZ = piece.maxZ >> 4;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                level.getChunkAt(new BlockPos(chunkX << 4, level.getMinBuildHeight(), chunkZ << 4));
            }
        }
    }

    private Bounds currentBounds() {
        BlockPos first = map.getMapArea().pos1();
        BlockPos second = map.getMapArea().pos2();
        return new Bounds(
                Math.min(first.getX(), second.getX()), Math.min(first.getY(), second.getY()), Math.min(first.getZ(), second.getZ()),
                Math.max(first.getX(), second.getX()), Math.max(first.getY(), second.getY()), Math.max(first.getZ(), second.getZ()));
    }

    private int pieceCount(Bounds bounds) {
        return bounds.piecesX() * bounds.piecesY() * bounds.piecesZ();
    }

    private Piece pieceAt(Bounds bounds, int index) {
        int countX = bounds.piecesX();
        int countZ = bounds.piecesZ();
        int pieceX = index % countX;
        int pieceZ = index / countX % countZ;
        int pieceY = index / (countX * countZ);
        int sectionMinX = ((bounds.minX >> 4) + pieceX) << 4;
        int sectionMinY = ((bounds.minY >> 4) + pieceY) << 4;
        int sectionMinZ = ((bounds.minZ >> 4) + pieceZ) << 4;
        return new Piece(Math.max(sectionMinX, bounds.minX), Math.max(sectionMinY, bounds.minY), Math.max(sectionMinZ, bounds.minZ),
                Math.min(sectionMinX + PIECE_SIZE - 1, bounds.maxX),
                Math.min(sectionMinY + PIECE_SIZE - 1, bounds.maxY),
                Math.min(sectionMinZ + PIECE_SIZE - 1, bounds.maxZ));
    }

    private Path snapshotPath() {
        String safeName = map.getMapName().replaceAll("[^A-Za-z0-9._-]", "_");
        String suffix = Integer.toUnsignedString(map.getMapName().hashCode(), 16);
        return map.getServerLevel().getServer().getWorldPath(LevelResource.ROOT)
                .resolve("data/blockzone/battlezone-scenes")
                .resolve(safeName + "-" + suffix + FILE_SUFFIX);
    }

    private record Bounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        private long volume() {
            return (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        }

        private int piecesX() {
            return (maxX >> 4) - (minX >> 4) + 1;
        }

        private int piecesY() {
            return (maxY >> 4) - (minY >> 4) + 1;
        }

        private int piecesZ() {
            return (maxZ >> 4) - (minZ >> 4) + 1;
        }

        private BlockPos minPos() {
            return new BlockPos(minX, minY, minZ);
        }

        private BlockPos maxPos() {
            return new BlockPos(maxX, maxY, maxZ);
        }

        private AABB toAabb() {
            return new AABB(minX, minY, minZ, maxX + 1.0D, maxY + 1.0D, maxZ + 1.0D);
        }
    }

    private record Piece(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        private int volume() {
            return (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        }

        private boolean isWholeSection() {
            return volume() == PIECE_SIZE * PIECE_SIZE * PIECE_SIZE
                    && (minX & 15) == 0 && (minY & 15) == 0 && (minZ & 15) == 0;
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("min_x", minX);
            tag.putInt("min_y", minY);
            tag.putInt("min_z", minZ);
            tag.putInt("max_x", maxX);
            tag.putInt("max_y", maxY);
            tag.putInt("max_z", maxZ);
            return tag;
        }

        private static Piece load(CompoundTag tag) {
            return new Piece(tag.getInt("min_x"), tag.getInt("min_y"), tag.getInt("min_z"),
                    tag.getInt("max_x"), tag.getInt("max_y"), tag.getInt("max_z"));
        }
    }

    private static final class RestoreCursor {
        private final Piece piece;
        private final int sizeX;
        private final int sizeZ;
        private final BlockState[] palette;
        private final int[] states;
        private final byte[] sectionData;
        private final ListTag blockEntities;
        private final Map<Long, LevelChunk> touchedChunks = new HashMap<>();
        private int stateIndex;

        private RestoreCursor(Piece piece, BlockState[] palette, int[] states, byte[] sectionData, ListTag blockEntities) {
            this.piece = piece;
            this.sizeX = piece.maxX - piece.minX + 1;
            this.sizeZ = piece.maxZ - piece.minZ + 1;
            this.palette = palette;
            this.states = states;
            this.sectionData = sectionData;
            this.blockEntities = blockEntities;
        }

        private static RestoreCursor section(Piece piece, byte[] sectionData, ListTag blockEntities) {
            return new RestoreCursor(piece, null, null, sectionData, blockEntities);
        }

        private static RestoreCursor blocks(Piece piece, BlockState[] palette, int[] states, ListTag blockEntities) {
            return new RestoreCursor(piece, palette, states, null, blockEntities);
        }
    }

    private enum OperationType {
        CAPTURE("capture"), RESTORE("restore");

        private final String description;

        OperationType(String description) {
            this.description = description;
        }
    }

    private static final class Operation {
        private final OperationType type;
        private final ServerLevel level;
        private final Bounds bounds;
        private final int pieceCount;
        private final Path path;
        private final Path temporaryPath;
        private final CompoundTag data;
        private final ListTag pieces;
        private final Map<Long, Integer> lastPieceByChunk;
        private final Map<Long, LevelChunk> chunksToRefresh = new LinkedHashMap<>();
        private RestoreCursor restoreCursor;
        private boolean restoreEntitiesRemoved;
        private boolean lightUpdatesSettled;
        private int index;

        private Operation(OperationType type, ServerLevel level, Bounds bounds, int pieceCount, Path path, CompoundTag data) {
            this.type = type;
            this.level = level;
            this.bounds = bounds;
            this.pieceCount = pieceCount;
            this.path = path;
            this.temporaryPath = path == null ? null : path.resolveSibling(path.getFileName() + ".tmp");
            this.data = data;
            this.pieces = type == OperationType.CAPTURE ? new ListTag() : data.getList("pieces", Tag.TAG_COMPOUND);
            this.lastPieceByChunk = type == OperationType.RESTORE ? findLastPieceByChunk(this.pieces) : Map.of();
        }

        private static Operation capture(ServerLevel level, Bounds bounds, int pieceCount, Path path, CompoundTag data) {
            return new Operation(OperationType.CAPTURE, level, bounds, pieceCount, path, data);
        }

        private static Operation restore(ServerLevel level, Bounds bounds, CompoundTag data) {
            return new Operation(OperationType.RESTORE, level, bounds, data.getInt("piece_count"), null, data);
        }

        private static Map<Long, Integer> findLastPieceByChunk(ListTag pieces) {
            Map<Long, Integer> lastPieceByChunk = new HashMap<>();
            for (int pieceIndex = 0; pieceIndex < pieces.size(); pieceIndex++) {
                Piece piece = Piece.load(pieces.getCompound(pieceIndex));
                for (int chunkX = piece.minX >> 4; chunkX <= piece.maxX >> 4; chunkX++) {
                    for (int chunkZ = piece.minZ >> 4; chunkZ <= piece.maxZ >> 4; chunkZ++) {
                        lastPieceByChunk.put(ChunkPos.asLong(chunkX, chunkZ), pieceIndex);
                    }
                }
            }
            return lastPieceByChunk;
        }
    }
}
