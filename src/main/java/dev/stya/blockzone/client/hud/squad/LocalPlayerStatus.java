package dev.stya.blockzone.client.hud.squad;

import dev.stya.blockzone.squad.SquadMemberStatus;

final class LocalPlayerStatus {
    static final int WIDTH = 383 + ArmorPlateSlot.SIZE + 8 + 24, HEIGHT = 64;
    private final PlayerAvatar avatar = new PlayerAvatar();
    private final PlayerIdentity identity = new PlayerIdentity();
    private final FiringIndicator firing = new FiringIndicator();
    private final PlayerVitals vitals = new PlayerVitals();
    private final ArmorPlateSlot plates = new ArmorPlateSlot();
    void render(SquadHudGraphics g, SquadMemberStatus member) {
        avatar.render(g, member, 0, 0, 64, true);
        identity.render(g, member, 72, 7.5f, 300, true);
        vitals.render(g, member, 72, 32, 300, true);
        // Counter-scale only the quick plate slot, count and key; its bottom follows the vitals.
        g.gui.pose().pushPose();
        try {
            g.gui.pose().translate(383, HEIGHT - ArmorPlateSlot.SIZE / SquadHudStyle.CONTENT_SCALE, 0);
            float plateScale = 1 / SquadHudStyle.CONTENT_SCALE;
            g.gui.pose().scale(plateScale, plateScale, 1);
            plates.render(g, member.plating(), 0, 0);
            g.gui.flush();
        } finally { g.gui.pose().popPose(); }
        firing.render(g, member, 383 + ArmorPlateSlot.SIZE / SquadHudStyle.CONTENT_SCALE + 8, 36, 24);
    }
}
