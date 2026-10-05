package dev.stya.blockzone.data.snapshot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

/** Stable snapshot paths and compressed NBT replacement, separate from world restoration. */
final class SceneSnapshotStorage {
    private SceneSnapshotStorage() { }

    static Path path(Path worldRoot, String mapName) {
        String safeName = mapName.replaceAll("[^A-Za-z0-9._-]", "_");
        String suffix = Integer.toUnsignedString(mapName.hashCode(), 16);
        return worldRoot.resolve("data/blockzone/battlezone-scenes")
                .resolve(safeName + "-" + suffix + ".battlezone-scene.nbt");
    }

    static CompoundTag read(Path path) throws IOException { return NbtIo.readCompressed(path.toFile()); }

    static void write(Path path, CompoundTag data) throws IOException {
        Files.createDirectories(path.getParent());
        Path temporary = temporaryPath(path);
        NbtIo.writeCompressed(data, temporary.toFile());
        try {
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicMoveUnsupported) {
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    static void discardTemporary(Path path) throws IOException { Files.deleteIfExists(temporaryPath(path)); }
    private static Path temporaryPath(Path path) { return path.resolveSibling(path.getFileName() + ".tmp"); }
}
