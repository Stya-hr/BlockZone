package dev.stya.blockzone.client.hud.squad;

import dev.stya.blockzone.squad.SquadMemberStatus;
import net.minecraft.network.chat.Component;

final class LocalPlayerStatus {
    static final int WIDTH = 515, HEIGHT = 64;
    private final PlayerAvatar avatar = new PlayerAvatar();
    private final PlayerIdentity identity = new PlayerIdentity();
    private final FiringIndicator firing = new FiringIndicator();
    private final PlayerVitals vitals = new PlayerVitals();
    private final ArmorPlateSlot plates = new ArmorPlateSlot();
    void render(SquadHudGraphics g, SquadMemberStatus member) {
        avatar.render(g, member, 0, 0, 64, true);
        identity.render(g, member, 75, 7.5f, 360, true);
        vitals.render(g, member, 75, 32, 360, true);
        plates.render(g, member.plating(), 443, 24);
        firing.render(g, member, 491, 36, 24);
        if (member.plating()) g.text(Component.translatable("hud.blockzone.plating").getString(), 75, -24, 16, SquadHudStyle.TEXT);
        else if (member.rescueState() == 2) g.text(Component.translatable("hud.blockzone.rescuing").getString(), 75, -24, 16, SquadHudStyle.RESCUE);
    }
}
