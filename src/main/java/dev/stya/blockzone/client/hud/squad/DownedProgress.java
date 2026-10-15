package dev.stya.blockzone.client.hud.squad;

import dev.stya.blockzone.squad.SquadMemberStatus;
import net.minecraft.network.chat.Component;

final class DownedProgress {
    void render(SquadHudGraphics g, SquadMemberStatus member, float x, float y, float width, boolean local) {
        boolean rescuing = member.rescueTicks() > 0;
        String text = rescuing ? Component.translatable("hud.blockzone.squad.reviving").getString()
                : Component.translatable("hud.blockzone.squad.downed", (member.bleedTicks() + 19) / 20).getString();
        float height = local ? 32 : 16;
        VitalBar.render(g, x, y, width, height, g.animations.get(member.id()).health,
                SquadHudStyle.DANGER, SquadHudStyle.DANGER);
        float size = local ? 16 : 12;
        g.text(g.fit(text, width - 8, size), x + 4, y + (height - size) / 2, size, SquadHudStyle.TEXT);
    }
}
