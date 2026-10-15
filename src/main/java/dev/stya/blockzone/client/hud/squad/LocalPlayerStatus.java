package dev.stya.blockzone.client.hud.squad;

import dev.stya.blockzone.squad.SquadMemberStatus;

final class LocalPlayerStatus {
    static final int WIDTH = 443 + ArmorPlateSlot.SIZE + 8 + 24, HEIGHT = 64;
    private final PlayerAvatar avatar = new PlayerAvatar();
    private final PlayerIdentity identity = new PlayerIdentity();
    private final FiringIndicator firing = new FiringIndicator();
    private final PlayerVitals vitals = new PlayerVitals();
    private final ArmorPlateSlot plates = new ArmorPlateSlot();
    void render(SquadHudGraphics g, SquadMemberStatus member) {
        avatar.render(g, member, 0, 0, 64, true);
        identity.render(g, member, 75, 7.5f, 360, true);
        vitals.render(g, member, 75, 32, 360, true);
        plates.render(g, member.plating(), 443, HEIGHT - ArmorPlateSlot.SIZE);
        firing.render(g, member, 443 + ArmorPlateSlot.SIZE + 8, 36, 24);
    }
}
