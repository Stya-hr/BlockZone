package dev.stya.blockzone.client.hud.squad;

import dev.stya.blockzone.squad.SquadMemberStatus;
import net.minecraft.network.chat.Component;

final class PlayerVitals {
    private final HealthBar health = new HealthBar();
    private final ArmorSegments armor = new ArmorSegments();
    private final DownedProgress downed = new DownedProgress();
    void render(SquadHudGraphics g, SquadMemberStatus member, float x, float y, float width, boolean local) {
        if (!member.online()) {
            float size = local ? 16 : 12;
            g.text(g.fit(Component.translatable("hud.blockzone.squad.offline").getString(), width, size), x, y, size, SquadHudStyle.MUTED);
        } else if (!member.living()) {
            // The reference eliminated row keeps only the portrait and identity.
        } else if (member.downed()) downed.render(g, member, x, y, width, local);
        else {
            armor.render(g, member, x, y, width, local);
            health.render(g, member, x, y + (local ? 16 : 11), width, local);
        }
    }
}
