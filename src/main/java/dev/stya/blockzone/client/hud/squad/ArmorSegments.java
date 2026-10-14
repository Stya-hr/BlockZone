package dev.stya.blockzone.client.hud.squad;

import dev.stya.blockzone.squad.SquadMemberStatus;

final class ArmorSegments {
    void render(SquadHudGraphics g, SquadMemberStatus member, float x, float y, float width, boolean local) {
        int count = member.plateSlots();
        float height = local ? 12 : 7;
        float segment = (width - Math.max(0, count - 1) * 8) / Math.max(1, count);
        for (int i = 0; i < count; i++) {
            float left = x + i * (segment + 8);
            g.rect(left, y, segment, height - 2, SquadHudStyle.TRACK);
            g.rect(left, y, segment * member.armorFraction(i), height - 2, SquadHudStyle.FILL);
            g.rect(left, y + height - 2, segment, 2, SquadHudStyle.ARMOR);
        }
    }
}
