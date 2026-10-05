package dev.stya.blockzone.net.editor;

import com.ptcrys.fpsmatch.core.FPSMCore;
import dev.stya.blockzone.BlockZone;
import dev.stya.blockzone.loot.LootContainerControl;
import dev.stya.blockzone.loot.LootContainerAdapters;
import dev.stya.blockzone.loot.LootCrateAccess;
import dev.stya.blockzone.map.battlezone.BattlezoneMap;
import dev.stya.blockzone.net.battlezone.BattlezoneNetwork;
import dev.stya.blockzone.util.editor.LootCrateEdit;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid = BlockZone.MOD_ID)
public final class LootEditorSessions {
    private static final Map<ServerPlayer, Session> SESSIONS = new WeakHashMap<>();
    private static final class Session {
        final UUID token = UUID.randomUUID();
        final BattlezoneMap map;
        final BlockPos first, second;
        final Map<LootCrateEdit.Position, LootCrateEdit> original = new HashMap<>();
        final long expires;
        int x, z;
        boolean scanning = true, saving;
        Session(BattlezoneMap map) {
            this.map = map; first = map.getMapArea().pos1(); second = map.getMapArea().pos2();
            x = Math.min(first.getX(), second.getX()) >> 4; z = Math.min(first.getZ(), second.getZ()) >> 4;
            expires = map.getServerLevel().getGameTime() + 18000;
        }
        boolean contains(BlockPos pos) {
            return pos.getX() >= Math.min(first.getX(), second.getX()) && pos.getX() <= Math.max(first.getX(), second.getX())
                    && pos.getY() >= Math.min(first.getY(), second.getY()) && pos.getY() <= Math.max(first.getY(), second.getY())
                    && pos.getZ() >= Math.min(first.getZ(), second.getZ()) && pos.getZ() <= Math.max(first.getZ(), second.getZ());
        }
        boolean valid(ServerPlayer player) {
            return player.hasPermissions(2) && !player.hasDisconnected() && player.serverLevel() == map.getServerLevel()
                    && map.getServerLevel().getGameTime() < expires && map.canEditLootCrates()
                    && first.equals(map.getMapArea().pos1()) && second.equals(map.getMapArea().pos2())
                    && FPSMCore.getInstance().getMapByTypeWithName("battlezone", map.getMapName()).orElse(null) == map;
        }
    }
    private LootEditorSessions() {}
    public static boolean open(ServerPlayer player, BattlezoneMap map) {
        if (!player.hasPermissions(2) || player.serverLevel() != map.getServerLevel() || !map.canEditLootCrates()) {
            player.sendSystemMessage(Component.translatable("editor.blockzone.loot.unavailable")); return false;
        }
        var old = SESSIONS.get(player);
        if (old != null && old.saving) return false;
        var session = new Session(map);
        long chunks = (long)((Math.max(session.first.getX(), session.second.getX()) >> 4) - session.x + 1)
                * ((Math.max(session.first.getZ(), session.second.getZ()) >> 4) - session.z + 1);
        if (chunks > 4096) { player.sendSystemMessage(Component.translatable("editor.blockzone.loot.too_large")); return false; }
        player.closeContainer();
        SESSIONS.put(player, session);
        player.sendSystemMessage(Component.translatable("editor.blockzone.loot.scanning"));
        return true;
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { SESSIONS.clear(); }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var iterator = SESSIONS.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next(); var player = entry.getKey(); var session = entry.getValue();
            if (session.saving) continue;
            if (!session.valid(player)) { iterator.remove(); continue; }
            if (!session.scanning) continue;
            long deadline = System.nanoTime() + 4_000_000L;
            for (int batch = 0; batch < 4 && session.scanning; batch++) {
                var level = session.map.getServerLevel();
                var chunk = level.getChunk(session.x, session.z);
                for (var pos : new ArrayList<>(chunk.getBlockEntitiesPos())) {
                    if (session.contains(pos) && LootContainerAdapters.find(level.getBlockEntity(pos)) != null) {
                        var data = LootContainerControl.data(level.getBlockEntity(pos)); session.original.put(data.position(), data);
                    }
                }
                if (session.original.size() > LootCrateEdit.MAX_CRATES) {
                    player.sendSystemMessage(Component.translatable("editor.blockzone.loot.too_large")); iterator.remove(); break;
                }
                if (++session.x > (Math.max(session.first.getX(), session.second.getX()) >> 4)) {
                    session.x = Math.min(session.first.getX(), session.second.getX()) >> 4;
                    if (++session.z > (Math.max(session.first.getZ(), session.second.getZ()) >> 4)) {
                        session.scanning = false;
                        var crates = session.original.values().stream().sorted(Comparator.comparingInt(LootCrateEdit::x)
                                .thenComparingInt(LootCrateEdit::z).thenComparingInt(LootCrateEdit::y)).toList();
                        var tables = level.getServer().getLootData().getKeys(LootDataType.TABLE).stream().map(ResourceLocation::toString)
                                .filter(id -> id.length() <= 256).sorted().limit(4096).toList();
                        BattlezoneNetwork.sendEditor(player, new LootEditorS2CPacket(session.token, session.map.getMapName(),
                                level.dimension().location(), session.first, session.second, crates, tables));
                    }
                }
                if (System.nanoTime() >= deadline) break;
            }
        }
    }
    static void save(ServerPlayer player, SaveLootEditorC2SPacket packet) {
        if (player == null) return;
        var session = SESSIONS.get(player);
        try {
            if (session == null || !session.token.equals(packet.token()) || !session.valid(player)
                    || session.scanning || session.saving) throw new IllegalArgumentException();
            var seen = new HashSet<LootCrateEdit.Position>();
            var tables = player.server.getLootData().getKeys(LootDataType.TABLE).stream().collect(java.util.stream.Collectors.toSet());
            if (packet.changes().isEmpty()) throw new IllegalArgumentException();
            for (var entry : packet.changes()) {
                var pos = new BlockPos(entry.x(), entry.y(), entry.z());
                var table = ResourceLocation.tryParse(entry.table());
                var original = session.original.get(entry.position());
                if (!seen.add(entry.position()) || original == null || !session.contains(pos)
                        || table == null || !tables.contains(table) || !LootCrateAccess.mayEdit(player.serverLevel(), pos)
                        || entry.opened() && !original.opened()
                        || LootContainerAdapters.find(player.serverLevel().getBlockEntity(pos)) == null
                        || !LootContainerControl.data(player.serverLevel().getBlockEntity(pos)).equals(original)
                        || entry.enabled() && LootCrateAccess.mapsAt(player.serverLevel(),pos).size()!=1) throw new IllegalArgumentException();
            }
            session.saving = true;
            boolean started = session.map.saveLootCrateEdits(packet.changes(), success -> {
                session.saving = false;
                if (success) packet.changes().forEach(entry -> session.original.put(entry.position(), entry));
                if (!player.hasDisconnected()) BattlezoneNetwork.sendEditor(player,
                        new LootEditorResultS2CPacket(packet.token(), success, success ? "editor.blockzone.loot.saved" : "editor.blockzone.loot.failed"));
            });
            if (!started) { session.saving = false; throw new IllegalArgumentException(); }
        } catch (RuntimeException failure) {
            BattlezoneNetwork.sendEditor(player, new LootEditorResultS2CPacket(packet.token(), false, "editor.blockzone.loot.conflict"));
        }
    }
}
