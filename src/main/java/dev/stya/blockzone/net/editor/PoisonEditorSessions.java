package dev.stya.blockzone.net.editor;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.ptcrys.fpsmatch.core.FPSMCore;
import dev.stya.blockzone.map.battlezone.BattlezoneMap;
import dev.stya.blockzone.net.battlezone.BattlezoneNetwork;
import dev.stya.blockzone.zone.PoisonPath;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerPlayer;

public final class PoisonEditorSessions {
    private record Session(UUID token, BattlezoneMap map, List<PoisonPath> original) {}
    private static final Map<ServerPlayer, Session> SESSIONS = new WeakHashMap<>();
    private PoisonEditorSessions() {}
    public static String json(List<PoisonPath> paths) {
        return PoisonPath.CODEC.listOf().encodeStart(JsonOps.INSTANCE, paths).getOrThrow(false, m -> {}).toString();
    }
    public static void open(ServerPlayer player, BattlezoneMap map) {
        if (!player.hasPermissions(2) || player.serverLevel() != map.getServerLevel())
            throw new IllegalArgumentException("Enter the map dimension as an operator to edit");
        var paths = map.configuredPoisonSequences();
        checkSize(paths);
        String payload = json(paths);
        if (payload.length() > 262144) throw new IllegalArgumentException("Configuration is too large for the world editor");
        var session = new Session(UUID.randomUUID(), map, List.copyOf(paths));
        SESSIONS.put(player, session);
        var area = map.getMapArea();
        BattlezoneNetwork.sendEditor(player, new PoisonEditorS2CPacket(session.token, map.getMapName(),
                map.getServerLevel().dimension().location(), area.pos1(), area.pos2(), payload));
    }
    private static void checkSize(List<PoisonPath> paths) {
        if (paths.isEmpty() || paths.size() > 128 || paths.stream().anyMatch(p -> p.circles().isEmpty() || p.circles().size() > 128))
            throw new IllegalArgumentException("Editor supports 1..128 sequences and circles per sequence");
    }
    static void save(ServerPlayer player, SavePoisonEditorC2SPacket packet) {
        if (player == null) return;
        try {
            var session = SESSIONS.get(player);
            if (session == null || !session.token.equals(packet.token()) || !player.hasPermissions(2))
                throw new IllegalArgumentException("Editing session expired or permission denied");
            var map = session.map;
            if (player.serverLevel() != map.getServerLevel()
                    || FPSMCore.getInstance().getMapByTypeWithName("battlezone", map.getMapName()).orElse(null) != map)
                throw new IllegalArgumentException("Map unloaded or you left its dimension");
            if (!map.configuredPoisonSequences().equals(session.original))
                throw new IllegalArgumentException("Configuration changed elsewhere. Reopen the editor before saving");
            var paths = PoisonPath.CODEC.listOf().parse(JsonOps.INSTANCE, JsonParser.parseString(packet.pathsJson()))
                    .getOrThrow(false, m -> {});
            checkSize(paths);
            var file = map.getConfigFile();
            if (file == null) throw new IllegalArgumentException("No settings file is available for this map");
            var target = file.toPath();
            byte[] previousFile;
            try { previousFile = java.nio.file.Files.exists(target) ? java.nio.file.Files.readAllBytes(target) : null; }
            catch (java.io.IOException failure) { throw new IllegalArgumentException("Cannot read the settings file", failure); }
            map.setPoisonSequences(paths);
            try {
                map.saveConfig();
                // FPSMatch logs I/O errors rather than propagating them. Verify persistence before acknowledging.
                var stored = JsonParser.parseString(java.nio.file.Files.readString(target)).getAsJsonObject().getAsJsonObject("capabilities").getAsJsonObject("capabilities")
                        .getAsJsonObject("BattlezoneZoneCapability").get("poisonSequences");
                var persisted = PoisonPath.CODEC.listOf().parse(JsonOps.INSTANCE, stored).getOrThrow(false, m -> {});
                if (!persisted.equals(paths)) throw new IllegalArgumentException("Saved settings did not match the draft");
            } catch (Exception failure) {
                map.setPoisonSequences(session.original);
                try {
                    if (previousFile != null) java.nio.file.Files.write(target, previousFile);
                    else java.nio.file.Files.deleteIfExists(target);
                } catch (java.io.IOException restoreFailure) { failure.addSuppressed(restoreFailure); }
                throw new IllegalArgumentException("Unable to persist settings. Draft was not applied", failure);
            }
            SESSIONS.put(player, new Session(session.token, map, paths));
            BattlezoneNetwork.sendEditor(player, new PoisonEditorResultS2CPacket(packet.token(), true,
                    "Saved. Active matches keep their original path."));
        } catch (RuntimeException failure) {
            BattlezoneNetwork.sendEditor(player, new PoisonEditorResultS2CPacket(packet.token(), false,
                    failure.getMessage() == null ? "Unable to save" : failure.getMessage()));
        }
    }
}
