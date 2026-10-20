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
    public void render(GuiGraphics gui, List<SquadMemberStatus> members, UUID localId) {
        animations.update(members, SquadClientState.scope(), System.nanoTime() / 1_000_000_000.0);
        var self = members.stream().filter(member -> member.id().equals(localId)).findFirst().orElse(null);
        if (self == null) return;
        var others = members.stream().filter(member -> !member.id().equals(localId))
                .sorted(java.util.Comparator.comparingInt(SquadMemberStatus::number)).toList();
        var window = net.minecraft.client.Minecraft.getInstance().getWindow();
        float scale = dev.stya.blockzone.client.hud.common.HudCanvas.scale();
        double viewportHeight = window.getHeight() / window.getGuiScale();
        float contentScale = SquadHudStyle.CONTENT_SCALE;
        float localTop = dev.stya.blockzone.client.compat.XaeroMinimapCompat.active()
                ? (float)(viewportHeight / scale) - dev.stya.blockzone.client.compat.XaeroMinimapCompat.BOTTOM
                    - LocalPlayerStatus.HEIGHT * contentScale - 26
                : (float)(viewportHeight / scale) - (SquadHudStyle.DESIGN_HEIGHT - SquadHudStyle.LOCAL_TOP);
        gui.pose().pushPose();
        try {
            // Left/bottom anchoring also preserves geometry on ultrawide and non-16:9 screens.
            gui.pose().translate((SquadHudStyle.LEFT + dev.stya.blockzone.client.compat.XaeroMinimapCompat.squadOffset()) * scale,
                    localTop * scale, 0);
            gui.pose().scale(scale * contentScale, scale * contentScale, 1);
            var g = new SquadHudGraphics(gui, animations);
            var layout = SquadRosterLayout.create(others.size(), localTop / contentScale);
            for (int i = 0; i < layout.count(); i++) {
                int slot = i;
                gui.pose().pushPose();
                gui.pose().translate(layout.x(slot), 0, 0);
                teammate.render(g, others.get(i), layout.y(slot));
                gui.flush();
                gui.pose().popPose();
            }
            local.render(g, self);
            gui.flush();
        } finally { gui.pose().popPose(); }
    }
}
