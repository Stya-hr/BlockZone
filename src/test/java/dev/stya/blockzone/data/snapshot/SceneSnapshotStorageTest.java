package dev.stya.blockzone.data.snapshot;

import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class SceneSnapshotStorageTest {
    @TempDir Path world;

    @Test void compressedSaveReplacesSnapshotWithoutLeavingTemporaryFile() throws Exception {
        var path = SceneSnapshotStorage.path(world, "arena");
        var data = new CompoundTag();
        data.putInt("version", 3);
        data.putString("dimension", "minecraft:overworld");
        SceneSnapshotStorage.write(path, data);
        assertEquals(data, SceneSnapshotStorage.read(path));
        data.putInt("piece_count", 4);
        SceneSnapshotStorage.write(path, data);
        assertEquals(data, SceneSnapshotStorage.read(path));
        assertFalse(Files.exists(path.resolveSibling(path.getFileName() + ".tmp")));
    }

    @Test void mapNamesStayInSnapshotDirectoryAndRetainExistingFilenameFormat() {
        var first = SceneSnapshotStorage.path(world, "a/b");
        var second = SceneSnapshotStorage.path(world, "a?b");
        assertEquals(world.resolve("data/blockzone/battlezone-scenes"), first.getParent());
        assertEquals("a_b-" + Integer.toUnsignedString("a/b".hashCode(), 16) + ".battlezone-scene.nbt",
                first.getFileName().toString());
        assertNotEquals(first, second);
    }
}
