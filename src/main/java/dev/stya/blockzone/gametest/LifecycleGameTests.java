package dev.stya.blockzone.gametest;

import com.ptcrys.fpsmatch.core.capability.FPSMCapability;
import com.ptcrys.fpsmatch.core.capability.FPSMCapabilityManager;
import com.ptcrys.fpsmatch.core.capability.map.MapCapability;
import com.ptcrys.fpsmatch.core.data.AreaData;
import com.ptcrys.fpsmatch.core.map.BaseMap;
import dev.stya.blockzone.map.battlezone.BattlezoneMap;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("blockzone")
@PrefixGameTestTemplate(false)
public final class LifecycleGameTests {
    private static BattlezoneMap map(GameTestHelper helper) {
        var pos = helper.absolutePos(BlockPos.ZERO);
        return new BattlezoneMap(helper.getLevel(), "lifecycle_" + UUID.randomUUID(), new AreaData(pos, pos.offset(2, 2, 2)));
    }

    public static final class Probe extends MapCapability {
        int ticks, resets, destroys;
        public Probe(BaseMap map) { super(map); }
        @Override public void tick() { ticks++; }
        @Override public void reset() { resets++; }
        @Override public void destroy() { destroys++; }
    }

    @GameTest(template = "empty")
    public static void frameworkOwnsCapabilityTickResetAndRemoval(GameTestHelper helper) {
        if (!FPSMCapabilityManager.isRegistered(Probe.class))
            FPSMCapabilityManager.register(FPSMCapabilityManager.CapabilityType.MAP, Probe.class,
                    new FPSMCapability.Factory<BaseMap, Probe>() {
                        @Override public Probe create(BaseMap map) { return new Probe(map); }
                    });
        var map = map(helper);
        check(map.getCapabilityMap().add(Probe.class), "Attach probe capability");
        var probe = map.getCapabilityMap().get(Probe.class).orElseThrow();
        map.mapTick();
        check(probe.ticks == 1, "Framework ticks capability exactly once");
        map.reset();
        check(probe.resets == 1, "Map reset resets every attached capability exactly once");
        map.getCapabilityMap().remove(Probe.class);
        check(probe.destroys == 1, "Framework removal destroys capability exactly once");
        map.mapTick();
        check(probe.ticks == 1, "Removed capability no longer ticks");
        map.getCapabilityMap().clear();
        helper.succeed();
    }

    private static net.minecraft.server.level.ServerPlayer player(GameTestHelper helper) {
        var id = UUID.randomUUID();
        var player = net.minecraftforge.common.util.FakePlayerFactory.get(helper.getLevel(),
                new com.mojang.authlib.GameProfile(id, "life_" + id.toString().substring(0, 8)));
        player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        player.getInventory().clearContent();
        player.getInventory().setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.APPLE, 3));
        return player;
    }

    private static final class CancelLeave {
        final BattlezoneMap map;
        CancelLeave(BattlezoneMap map) { this.map = map; }
        @net.minecraftforge.eventbus.api.SubscribeEvent
        public void leave(com.ptcrys.fpsmatch.common.event.FPSMapEvent.PlayerEvent.LeaveEvent event) {
            if (event.getMap() == map) event.setCanceled(true);
        }
    }

    @GameTest(template = "empty")
    public static void canceledLeavePreservesSessionAndSuccessfulLeaveRestoresOnce(GameTestHelper helper) {
        var map = map(helper);
        var player = player(helper);
        check(map.join(player).isSuccess(), "Join through FPSMatch");
        player.setAbsorptionAmount(12);
        var cancel = new CancelLeave(map);
        var bus = net.minecraftforge.common.MinecraftForge.EVENT_BUS;
        bus.register(cancel);
        try {
            map.leave(player);
            check(map.getMapTeams().getTeamByPlayer(player).isPresent(), "Canceled leave preserves membership");
            check(player.getAbsorptionAmount() == 12, "Canceled leave does not clear combat state");
        } finally { bus.unregister(cancel); }
        map.leave(player);
        check(map.getMapTeams().getTeamByPlayer(player).isEmpty(), "Successful leave removes membership");
        check(player.isCreative(), "Restore pre-match game mode");
        check(player.getInventory().getItem(0).is(net.minecraft.world.item.Items.APPLE)
                && player.getInventory().getItem(0).getCount() == 3, "Restore pre-match inventory");
        player.getInventory().setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK));
        map.leave(player);
        check(player.isCreative() && player.getInventory().getItem(0).is(net.minecraft.world.item.Items.STICK),
                "Repeated leave cannot mutate restored state");
        map.getCapabilityMap().clear();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void disconnectThenLeaveQueuesOneRestorationForLogin(GameTestHelper helper) {
        var map = map(helper);
        var player = player(helper);
        check(map.join(player).isSuccess(), "Join through FPSMatch");
        player.getInventory().setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE));
        map.handlePlayerDisconnect(player);
        map.leave(player);
        check(player.getInventory().getItem(0).is(net.minecraft.world.item.Items.STONE), "Disconnect defers snapshot restoration");
        var login = new net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent(player);
        dev.stya.blockzone.data.snapshot.PlayerStateSnapshot.onLogin(login);
        check(player.isCreative() && player.getInventory().getItem(0).is(net.minecraft.world.item.Items.APPLE),
                "Login restores pre-match state");
        player.getInventory().setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK));
        dev.stya.blockzone.data.snapshot.PlayerStateSnapshot.onLogin(login);
        check(player.getInventory().getItem(0).is(net.minecraft.world.item.Items.STICK), "Snapshot consumed once");
        map.getCapabilityMap().clear();
        helper.succeed();
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
