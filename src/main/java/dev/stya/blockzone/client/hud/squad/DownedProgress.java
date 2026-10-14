package dev.stya.blockzone.client.hud.squad;

import dev.stya.blockzone.squad.SquadMemberStatus;
import net.minecraft.network.chat.Component;

final class DownedProgress {
    void render(SquadHudGraphics g, SquadMemberStatus member, float x, float y, float width, boolean local) {
        boolean rescuing = member.rescueTicks() > 0;
        int color = rescuing ? SquadHudStyle.RESCUE : SquadHudStyle.DANGER;
        String text = rescuing ? Component.translatable("hud.blockzone.squad.reviving").getString()
                : Component.translatable("hud.blockzone.squad.downed", (member.bleedTicks() + 19) / 20).getString();
        float size = local ? 16 : 12;
        g.text(g.fit(text, width, size), x, y, size, color);
        float height = local ? 6 : 4;
        float top = y + (local ? 22 : 12);
        g.rect(x, top, width, height, SquadHudStyle.TRACK);
        g.rect(x, top, width * member.downedFraction(), height - 2, SquadHudStyle.FILL);
        g.rect(x, top + height - 2, width, 2, color);
    }
}
