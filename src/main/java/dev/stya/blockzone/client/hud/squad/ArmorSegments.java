package dev.stya.blockzone.client.hud.squad;

import dev.stya.blockzone.squad.SquadMemberStatus;

final class ArmorSegments {
    void render(SquadHudGraphics g, SquadMemberStatus member, float x, float y, float width, boolean local) {
        int count = member.plateSlots();
        float height = local ? 12 : 7;
        float segment = (width - Math.max(0, count - 1) * 8) / Math.max(1, count);
        for (int i = 0; i < count; i++) {
            float left = x + i * (segment + 8);
            VitalBar.render(g, left, y, segment, height, member.armorFraction(i), SquadHudStyle.ARMOR);
        }
    }
}
