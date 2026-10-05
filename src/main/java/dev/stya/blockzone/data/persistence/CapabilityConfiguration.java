package dev.stya.blockzone.data.persistence;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.ptcrys.fpsmatch.core.capability.FPSMCapability;
import com.ptcrys.fpsmatch.core.capability.map.MapCapability;
import java.nio.file.Files;

/** FPSMatch logs write failures; verify the file before acknowledging a configuration update. */
public final class CapabilityConfiguration {
    private CapabilityConfiguration() { }
    public static <T, C extends MapCapability & FPSMCapability.Savable<T>> void save(C cap, T value) {
        var file = cap.getHolder().getConfigFile();
        if (file == null) throw new IllegalArgumentException("No map configuration file available");
        var target = file.toPath();
        byte[] oldFile;
        try { oldFile = Files.exists(target) ? Files.readAllBytes(target) : null; }
        catch (java.io.IOException failure) { throw new IllegalArgumentException("Cannot read configuration", failure); }
        var previous = cap.read();
        cap.write(value);
        try {
            cap.getHolder().saveConfig();
            var stored = JsonParser.parseString(Files.readString(target)).getAsJsonObject()
                    .getAsJsonObject("capabilities").getAsJsonObject("capabilities").get(cap.getClass().getSimpleName());
            var decoded = cap.codec().parse(JsonOps.INSTANCE, stored).getOrThrow(false, message -> {});
            if (!cap.read().equals(decoded)) throw new IllegalStateException("Saved configuration differs from requested value");
        } catch (Exception failure) {
            cap.write(previous);
            try {
                if (oldFile == null) Files.deleteIfExists(target); else Files.write(target, oldFile);
            } catch (java.io.IOException restoreFailure) { failure.addSuppressed(restoreFailure); }
            throw new IllegalArgumentException("Unable to persist configuration; change rolled back", failure);
        }
    }
}
