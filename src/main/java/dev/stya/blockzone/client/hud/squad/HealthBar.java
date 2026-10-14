package dev.stya.blockzone.client.hud.squad;

import dev.stya.blockzone.squad.SquadMemberStatus;

final class HealthBar {
    void render(SquadHudGraphics g, SquadMemberStatus member, float x, float y, float width, boolean local) {
        float height = local ? 16 : 5;
        g.rect(x, y, width, height - 2, SquadHudStyle.TRACK);
        g.rect(x, y, width * member.healthFraction(), height - 2, SquadHudStyle.FILL);
        g.rect(x, y + height - 2, width, 2, SquadHudStyle.HEALTH);
    }
}
