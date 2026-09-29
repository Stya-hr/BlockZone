package dev.stya.blockzone.game.battlezone;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
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
import java.util.function.Function;

/** Persistent baseline of a Battlezone arena, captured only by an explicit admin action. */
final class BattlezoneSceneSnapshot {
    private static final Logger LOGGER = LoggerFactory.getLogger(BattlezoneSceneSnapshot.class);
    private static final int FORMAT_VERSION = 1;
    private static final String FILE_SUFFIX = ".battlezone-scene.nbt";

    private final BattlezoneMap map;
    private CompoundTag data;

    BattlezoneSceneSnapshot(BattlezoneMap map) {
        this.map = map;
    }

    boolean save() {
        if (map.getMapArea() == null) {
            return false;
        }
        try {
            CompoundTag snapshot = capture();
            Path destination = snapshotPath();
            Files.createDirectories(destination.getParent());
            Path temporary = destination.resolveSibling(destination.getFileName() + ".tmp");
            NbtIo.writeCompressed(snapshot, temporary.toFile());
            try {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicMoveUnsupported) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
            this.data = snapshot;
            LOGGER.info("Saved Battlezone scene snapshot for map {}", map.getMapName());
            return true;
        } catch (Exception exception) {
            LOGGER.error("Failed to save Battlezone scene snapshot for map {}", map.getMapName(), exception);
            return false;
        }
    }

    boolean load() {
        Path path = snapshotPath();
        if (!Files.isRegularFile(path)) {
            return false;
        }
        try {
            CompoundTag loaded = NbtIo.readCompressed(path.toFile());
            if (!matchesCurrentMap(loaded)) {
                LOGGER.warn("Battlezone scene snapshot for map {} does not match its dimension or area", map.getMapName());
                return false;
            }
            this.data = loaded;
            return true;
        } catch (Exception exception) {
            LOGGER.error("Failed to load Battlezone scene snapshot for map {}", map.getMapName(), exception);
            return false;
        }
    }

    boolean exists() {
        return Files.exists(snapshotPath());
    }

    boolean restore() {
        if (data == null || !matchesCurrentMap(data)) {
            return false;
        }
        ServerLevel level = map.getServerLevel();
        BlockPos min = minCorner();
        BlockPos max = maxCorner();
        try {
            ensureChunksLoaded(level, min, max);
            removeCurrentNonPlayerEntities(level, min, max);
            ListTag blocks = data.getList("blocks", CompoundTag.TAG_COMPOUND);
            for (int index = 0; index < blocks.size(); index++) {
                CompoundTag entry = blocks.getCompound(index);
                BlockPos pos = min.offset(entry.getInt("x"), entry.getInt("y"), entry.getInt("z"));
                BlockState state = NbtUtils.readBlockState(level.registryAccess().lookupOrThrow(Registries.BLOCK), entry.getCompound("state"));
                level.setBlock(pos, state, Block.UPDATE_ALL);
            }
            for (int index = 0; index < blocks.size(); index++) {
                CompoundTag entry = blocks.getCompound(index);
                if (!entry.contains("block_entity", CompoundTag.TAG_COMPOUND)) {
                    continue;
                }
                BlockPos pos = min.offset(entry.getInt("x"), entry.getInt("y"), entry.getInt("z"));
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity != null) {
                    blockEntity.load(entry.getCompound("block_entity"));
                    blockEntity.setChanged();
                    level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), Block.UPDATE_ALL);
                }
            }
            ListTag entities = data.getList("entities", CompoundTag.TAG_COMPOUND);
            for (int index = 0; index < entities.size(); index++) {
                EntityType.loadEntityRecursive(entities.getCompound(index), level, entity -> {
                    level.addFreshEntity(entity);
                    return entity;
                });
            }
            return true;
        } catch (Exception exception) {
            LOGGER.error("Failed to restore Battlezone scene for map {}", map.getMapName(), exception);
            return false;
        }
    }

    private CompoundTag capture() {
        ServerLevel level = map.getServerLevel();
        BlockPos min = minCorner();
        BlockPos max = maxCorner();
        CompoundTag snapshot = new CompoundTag();
        snapshot.putInt("version", FORMAT_VERSION);
        snapshot.putString("map", map.getMapName());
        snapshot.putString("dimension", level.dimension().location().toString());
        snapshot.put("min", NbtUtils.writeBlockPos(min));
        snapshot.put("max", NbtUtils.writeBlockPos(max));

        ListTag blocks = new ListTag();
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            BlockState state = level.getBlockState(pos);
            CompoundTag entry = new CompoundTag();
            entry.putInt("x", pos.getX() - min.getX());
            entry.putInt("y", pos.getY() - min.getY());
            entry.putInt("z", pos.getZ() - min.getZ());
            entry.put("state", NbtUtils.writeBlockState(state));
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity != null) {
                entry.put("block_entity", blockEntity.saveWithFullMetadata());
            }
            blocks.add(entry);
        }
        snapshot.put("blocks", blocks);

        ListTag entities = new ListTag();
        AABB bounds = new AABB(min, max.offset(1, 1, 1));
        for (Entity entity : level.getEntitiesOfClass(Entity.class, bounds, entity -> !(entity instanceof Player) && entity.getVehicle() == null)) {
            CompoundTag entityData = new CompoundTag();
            if (entity.saveAsPassenger(entityData)) {
                entities.add(entityData);
            }
        }
        snapshot.put("entities", entities);
        return snapshot;
    }

    private boolean matchesCurrentMap(CompoundTag snapshot) {
        return snapshot.getInt("version") == FORMAT_VERSION
                && map.getMapName().equals(snapshot.getString("map"))
                && map.getServerLevel().dimension().location().toString().equals(snapshot.getString("dimension"))
                && snapshot.contains("min", CompoundTag.TAG_COMPOUND)
                && snapshot.contains("max", CompoundTag.TAG_COMPOUND)
                && minCorner().equals(NbtUtils.readBlockPos(snapshot.getCompound("min")))
                && maxCorner().equals(NbtUtils.readBlockPos(snapshot.getCompound("max")));
    }

    private void removeCurrentNonPlayerEntities(ServerLevel level, BlockPos min, BlockPos max) {
        AABB bounds = new AABB(min, max.offset(1, 1, 1));
        for (Entity entity : new ArrayList<>(level.getEntitiesOfClass(Entity.class, bounds, entity -> !(entity instanceof Player)))) {
            entity.discard();
        }
    }

    private void ensureChunksLoaded(ServerLevel level, BlockPos min, BlockPos max) {
        int minChunkX = min.getX() >> 4;
        int maxChunkX = max.getX() >> 4;
        int minChunkZ = min.getZ() >> 4;
        int maxChunkZ = max.getZ() >> 4;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                level.getChunkAt(new BlockPos(chunkX << 4, level.getMinBuildHeight(), chunkZ << 4));
            }
        }
    }

    private BlockPos minCorner() {
        BlockPos first = map.getMapArea().pos1();
        BlockPos second = map.getMapArea().pos2();
        return new BlockPos(Math.min(first.getX(), second.getX()), Math.min(first.getY(), second.getY()), Math.min(first.getZ(), second.getZ()));
    }

    private BlockPos maxCorner() {
        BlockPos first = map.getMapArea().pos1();
        BlockPos second = map.getMapArea().pos2();
        return new BlockPos(Math.max(first.getX(), second.getX()), Math.max(first.getY(), second.getY()), Math.max(first.getZ(), second.getZ()));
    }

    private Path snapshotPath() {
        String safeName = map.getMapName().replaceAll("[^A-Za-z0-9._-]", "_");
        String suffix = Integer.toUnsignedString(map.getMapName().hashCode(), 16);
        return map.getServerLevel().getServer().getWorldPath(LevelResource.ROOT)
                .resolve("data/blockzone/battlezone-scenes")
                .resolve(safeName + "-" + suffix + FILE_SUFFIX);
    }
}
