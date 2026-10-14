package dev.stya.blockzone.client.hud.squad;

import dev.stya.blockzone.squad.SquadMemberStatus;

final class PlayerIdentity {
    void render(SquadHudGraphics g, SquadMemberStatus member, float x, float y, float width, boolean local) {
        g.rect(x, y, 18, 18, SquadHudStyle.memberColor(member.number()));
        g.text(Integer.toString(member.number()), x + 4, y + 2, 18, SquadHudStyle.TEXT);
        float offset = local ? 34 : 39;
        g.name(member.name(), x + offset, y + 1, local ? 20 : 18, width - offset,
                member.online() ? SquadHudStyle.NAME : SquadHudStyle.MUTED);
    }
}
