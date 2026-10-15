package dev.stya.blockzone.client.hud.squad;

import dev.stya.blockzone.squad.SquadMemberStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

final class PlayerAvatar {
    void render(SquadHudGraphics g, SquadMemberStatus member, float x, float y, int size, boolean local) {
        g.rect(x, y, size, size, SquadHudStyle.AVATAR_BACKGROUND);
        var connection = Minecraft.getInstance().getConnection();
        var info = connection == null ? null : connection.getPlayerInfo(member.id());
        ResourceLocation skin = info == null ? DefaultPlayerSkin.getDefaultSkin(member.id()) : info.getSkinLocation();
        g.gui.flush();
        g.gui.pose().pushPose();
        g.gui.pose().translate(x + 4, y + 4, 0);
        g.gui.blit(skin, 0, 0, size - 8, size - 8, 8, 8, 8, 8, 64, 64);
        g.gui.blit(skin, 0, 0, size - 8, size - 8, 40, 8, 8, 8, 64, 64);
        g.gui.pose().popPose();
        overlay.render(g, member, x, y, size);
    }
    private final AvatarStatusOverlay overlay = new AvatarStatusOverlay();
}
