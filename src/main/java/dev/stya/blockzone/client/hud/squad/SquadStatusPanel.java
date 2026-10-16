package dev.stya.blockzone.client.hud.squad;

import dev.stya.blockzone.squad.SquadMemberStatus;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;

/** QHD design-space layout; every child shares one aspect-preserving transform. */
public final class SquadStatusPanel {
    private final TeammateStatusRow teammate = new TeammateStatusRow();
    private final LocalPlayerStatus local = new LocalPlayerStatus();
    private final SquadVitalsAnimation animations = new SquadVitalsAnimation();
    private int page;
    private String scope = "";
    public void render(GuiGraphics gui, List<SquadMemberStatus> members, UUID localId) {
        animations.update(members, SquadClientState.scope(), System.nanoTime() / 1_000_000_000.0);
        var self = members.stream().filter(member -> member.id().equals(localId)).findFirst().orElse(null);
        if (self == null) return;
        var others = members.stream().filter(member -> !member.id().equals(localId))
                .sorted(java.util.Comparator.comparingInt(SquadMemberStatus::number)).toList();
        var window = net.minecraft.client.Minecraft.getInstance().getWindow();
        float scale = dev.stya.blockzone.client.hud.common.HudCanvas.scale();
        double viewportHeight = window.getHeight() / window.getGuiScale();
        gui.pose().pushPose();
        try {
            // Left/bottom anchoring also preserves geometry on ultrawide and non-16:9 screens.
            gui.pose().translate(SquadHudStyle.LEFT * scale,
                    viewportHeight - (SquadHudStyle.DESIGN_HEIGHT - SquadHudStyle.LOCAL_TOP) * scale, 0);
            gui.pose().scale(scale, scale, 1);
            var g = new SquadHudGraphics(gui, animations);
            var layout = SquadRosterLayout.create(others.size(), (float)(viewportHeight / scale - 184), gui.guiWidth() / scale);
            if (!scope.equals(SquadClientState.scope())) {
                scope = SquadClientState.scope(); page = 0;
                while (SquadPageKey.KEY.consumeClick()) { /* Discard input from the previous map. */ }
            }
            page = Math.min(page, layout.pages() - 1);
            while (SquadPageKey.KEY.consumeClick()) page = (page + 1) % layout.pages();
            for (int i = layout.start(page); i < layout.end(page); i++) {
                int slot = i - layout.start(page);
                gui.pose().pushPose();
                gui.pose().translate(layout.x(slot), 0, 0);
                teammate.render(g, others.get(i), layout.y(slot));
                gui.flush();
                gui.pose().popPose();
            }
            if (layout.pages() > 1) {
                int keyWidth = dev.stya.blockzone.client.inventory.InventoryPromptRenderer.renderKey(gui,
                        net.minecraft.client.Minecraft.getInstance().font, SquadPageKey.KEY.getKey(), 300, 76, 20);
                g.text((page + 1) + " / " + layout.pages(), 308 + keyWidth, 80, 12, SquadHudStyle.TEXT);
            }
            local.render(g, self);
            gui.flush();
        } finally { gui.pose().popPose(); }
    }
}
