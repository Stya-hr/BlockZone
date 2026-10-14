package dev.stya.blockzone.client.hud.squad;

import dev.stya.blockzone.squad.SquadMemberStatus;

final class PlayerIdentity {
    void render(SquadHudGraphics g, SquadMemberStatus member, float x, float y, float width, boolean local) {
        g.rect(x, y, 18, 18, SquadHudStyle.memberColor(member.number()));
        String index = Integer.toString(member.number());
        float size = Math.min(18, 16f * 8 / Math.max(1, net.minecraft.client.Minecraft.getInstance().font.width(index)));
        float indexWidth = net.minecraft.client.Minecraft.getInstance().font.width(index) * size / 8;
        g.text(index, x + (18 - indexWidth) / 2, y + (18 - size) / 2 + 2, size, SquadHudStyle.TEXT);
        float offset = local ? 34 : 39;
        g.name(member.name(), x + offset, y + 1, local ? 20 : 18, width - offset,
                member.online() ? SquadHudStyle.NAME : SquadHudStyle.MUTED);
    }
}
