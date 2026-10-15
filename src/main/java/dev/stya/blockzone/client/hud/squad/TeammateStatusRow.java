package dev.stya.blockzone.client.hud.squad;

import dev.stya.blockzone.squad.SquadMemberStatus;

final class TeammateStatusRow {
    private final PlayerAvatar avatar = new PlayerAvatar();
    private final PlayerIdentity identity = new PlayerIdentity();
    private final FiringIndicator firing = new FiringIndicator();
    private final PlayerVitals vitals = new PlayerVitals();
    void render(SquadHudGraphics g, SquadMemberStatus member, float y) {
        avatar.render(g, member, 0, y, 32, false);
        identity.render(g, member, 43, y + (member.living() ? -8.5f : .5f), 151, false);
        vitals.render(g, member, 43, y + 16, 151, false);
        firing.render(g, member, 202, y + 8, 16);
    }
}
