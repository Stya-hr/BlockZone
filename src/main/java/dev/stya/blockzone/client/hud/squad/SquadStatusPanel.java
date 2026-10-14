package dev.stya.blockzone.client.hud.squad;

import dev.stya.blockzone.squad.SquadMemberStatus;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;

/** QHD design-space layout; every child shares one aspect-preserving transform. */
public final class SquadStatusPanel {
    private final TeammateStatusRow teammate = new TeammateStatusRow();
    private final LocalPlayerStatus local = new LocalPlayerStatus();
    private final PlayerActionHints hints = new PlayerActionHints();
    public void render(GuiGraphics gui, List<SquadMemberStatus> members, UUID localId) {
        var self = members.stream().filter(member -> member.id().equals(localId)).findFirst().orElse(null);
        if (self == null) return;
        var others = members.stream().filter(member -> !member.id().equals(localId))
                .sorted(java.util.Comparator.comparingInt(SquadMemberStatus::number)).toList();
        var window = net.minecraft.client.Minecraft.getInstance().getWindow();
        float scale = (float)(Math.min(window.getWidth() / (float)SquadHudStyle.DESIGN_WIDTH,
                window.getHeight() / (float)SquadHudStyle.DESIGN_HEIGHT) / window.getGuiScale());
        double viewportHeight = window.getHeight() / window.getGuiScale();
        gui.pose().pushPose();
        try {
            // Left/bottom anchoring also preserves geometry on ultrawide and non-16:9 screens.
            gui.pose().translate(SquadHudStyle.LEFT * scale,
                    viewportHeight - (SquadHudStyle.DESIGN_HEIGHT - SquadHudStyle.LOCAL_TOP) * scale, 0);
            gui.pose().scale(scale, scale, 1);
            var g = new SquadHudGraphics(gui);
            for (int i = 0; i < others.size(); i++) {
                float y = SquadHudStyle.LAST_TEAM_TOP - SquadHudStyle.LOCAL_TOP - i * SquadHudStyle.TEAM_STEP;
                teammate.render(g, others.get(i), y);
            }
            local.render(g, self);
            if (self.online() && self.living() && !self.downed()) hints.render(g, 76);
            gui.flush();
        } finally { gui.pose().popPose(); }
    }
}
