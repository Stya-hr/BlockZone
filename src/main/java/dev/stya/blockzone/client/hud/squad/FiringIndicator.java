package dev.stya.blockzone.client.hud.squad;

import dev.stya.blockzone.squad.SquadMemberStatus;

/** Compact rifle with muzzle rays, blinking while server-confirmed fire is recent. */
final class FiringIndicator {
    void render(SquadHudGraphics g, SquadMemberStatus member, float x, float y, float size) {
        if (!member.online() || !member.living() || member.downed() || !member.firing()) return;
        if ((System.nanoTime() / 120_000_000L) % 2 != 0) return;
        float unit = size / 16;
        int color = 0xFFFFCC66;
        g.rect(x, y + 6 * unit, 10 * unit, 3 * unit, color);
        g.rect(x + 8 * unit, y + 7 * unit, 4 * unit, unit, color);
        g.line(x + 4 * unit, y + 9 * unit, x + 5 * unit, y + 12 * unit, 2 * unit, color);
        g.line(x + 12 * unit, y + 5 * unit, x + 15 * unit, y + 3 * unit, unit, color);
        g.line(x + 13 * unit, y + 7 * unit, x + 16 * unit, y + 7 * unit, unit, color);
        g.line(x + 12 * unit, y + 9 * unit, x + 15 * unit, y + 11 * unit, unit, color);
    }
}
