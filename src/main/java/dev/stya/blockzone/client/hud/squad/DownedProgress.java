package dev.stya.blockzone.client.hud.squad;

import dev.stya.blockzone.squad.SquadMemberStatus;

final class DownedProgress {
    void render(SquadHudGraphics g, SquadMemberStatus member, float x, float y, float width, boolean local) {
        float height = local ? 32 : 16;
        VitalBar.render(g, x, y, width, height, g.animations.get(member.id()).health,
                SquadHudStyle.DANGER, SquadHudStyle.DANGER);
    }
}
