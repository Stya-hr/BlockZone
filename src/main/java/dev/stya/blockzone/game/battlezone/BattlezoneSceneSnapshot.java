package dev.stya.blockzone.game.battlezone;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Persistent baseline of a Battlezone arena, captured only by an explicit admin action. */
final class BattlezoneSceneSnapshot {
    private static final Logger LOGGER = LoggerFactory.getLogger(BattlezoneSceneSnapshot.class);
    private static final int FORMAT_VERSION = 2;
    private static final int PIECE_SIZE = 16;
    private static final long MAX_BLOCKS = 5_000_000L;
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
            current.pieces.add(capturePiece(current.level, pieceAt(current.bounds, current.index)));
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
        if (current.index == 0) {
            removeCurrentNonPlayerEntities(current.level, current.bounds);
        }
        if (current.index < current.pieceCount) {
            restorePiece(current.level, current.pieces.getCompound(current.index));
            current.index++;
            return;
        }

        restoreEntities(current.level, current.data.getList("entities", Tag.TAG_COMPOUND));
        lastOperationSucceeded = true;
        operation = null;
        LOGGER.info("Restored Battlezone scene snapshot for map {}", map.getMapName());
    }

    private CompoundTag capturePiece(ServerLevel level, Piece piece) {
        ensureChunksLoaded(level, piece);
        Map<BlockState, Integer> paletteIds = new HashMap<>();
        List<BlockState> palette = new ArrayList<>();
        int[] states = new int[piece.volume()];
        ListTag blockEntities = new ListTag();
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

                    BlockEntity blockEntity = level.getBlockEntity(pos);
                    if (blockEntity != null) {
                        blockEntities.add(blockEntity.saveWithFullMetadata());
                    }
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

    private void restorePiece(ServerLevel level, CompoundTag pieceData) {
        Piece piece = Piece.load(pieceData);
        ListTag paletteTag = pieceData.getList("palette", Tag.TAG_COMPOUND);
        List<BlockState> palette = new ArrayList<>(paletteTag.size());
        for (int index = 0; index < paletteTag.size(); index++) {
            palette.add(NbtUtils.readBlockState(level.registryAccess().lookupOrThrow(Registries.BLOCK), paletteTag.getCompound(index)));
        }

        int[] states = pieceData.getIntArray("states");
        if (states.length != piece.volume()) {
            throw new IllegalStateException("Invalid Battlezone scene piece state count");
        }
        ensureChunksLoaded(level, piece);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int index = 0;
        for (int y = piece.minY; y <= piece.maxY; y++) {
            for (int z = piece.minZ; z <= piece.maxZ; z++) {
                for (int x = piece.minX; x <= piece.maxX; x++) {
                    int paletteId = states[index++];
                    if (paletteId < 0 || paletteId >= palette.size()) {
                        throw new IllegalStateException("Invalid Battlezone scene palette index " + paletteId);
                    }
                    pos.set(x, y, z);
                    level.setBlock(pos, palette.get(paletteId), Block.UPDATE_CLIENTS);
                }
            }
        }

        ListTag blockEntities = pieceData.getList("block_entities", Tag.TAG_COMPOUND);
        for (int blockEntityIndex = 0; blockEntityIndex < blockEntities.size(); blockEntityIndex++) {
            CompoundTag blockEntityData = blockEntities.getCompound(blockEntityIndex);
            BlockPos blockEntityPos = new BlockPos(
                    blockEntityData.getInt("x"), blockEntityData.getInt("y"), blockEntityData.getInt("z"));
            BlockEntity blockEntity = level.getBlockEntity(blockEntityPos);
            if (blockEntity != null) {
                blockEntity.load(blockEntityData);
                blockEntity.setChanged();
                BlockState state = level.getBlockState(blockEntityPos);
                level.sendBlockUpdated(blockEntityPos, state, state, Block.UPDATE_ALL);
            }
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
        int minX = bounds.minX + pieceX * PIECE_SIZE;
        int minY = bounds.minY + pieceY * PIECE_SIZE;
        int minZ = bounds.minZ + pieceZ * PIECE_SIZE;
        return new Piece(minX, minY, minZ,
                Math.min(minX + PIECE_SIZE - 1, bounds.maxX),
                Math.min(minY + PIECE_SIZE - 1, bounds.maxY),
                Math.min(minZ + PIECE_SIZE - 1, bounds.maxZ));
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
            return (maxX - minX) / PIECE_SIZE + 1;
        }

        private int piecesY() {
            return (maxY - minY) / PIECE_SIZE + 1;
        }

        private int piecesZ() {
            return (maxZ - minZ) / PIECE_SIZE + 1;
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
        }

        private static Operation capture(ServerLevel level, Bounds bounds, int pieceCount, Path path, CompoundTag data) {
            return new Operation(OperationType.CAPTURE, level, bounds, pieceCount, path, data);
        }

        private static Operation restore(ServerLevel level, Bounds bounds, CompoundTag data) {
            return new Operation(OperationType.RESTORE, level, bounds, data.getInt("piece_count"), null, data);
        }
    }
}
