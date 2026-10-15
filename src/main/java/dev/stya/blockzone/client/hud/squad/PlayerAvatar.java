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
        // Fill the inside of the 2px frame; no second dark inset around the face.
        int inset = SquadHudStyle.AVATAR_BORDER;
        g.gui.pose().translate(x + inset, y + inset, 0);
        g.gui.blit(skin, 0, 0, size - 2 * inset, size - 2 * inset, 8, 8, 8, 8, 64, 64);
        g.gui.blit(skin, 0, 0, size - 2 * inset, size - 2 * inset, 40, 8, 8, 8, 64, 64);
        g.gui.pose().popPose();
        overlay.render(g, member, x, y, size);
    }
    private final AvatarStatusOverlay overlay = new AvatarStatusOverlay();
}
