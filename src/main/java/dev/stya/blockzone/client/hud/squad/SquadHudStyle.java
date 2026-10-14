package dev.stya.blockzone.client.hud.squad;

/** Exact design coordinates from HUD/Screen/Viewport=QHD.svg (2560 x 1440). */
final class SquadHudStyle {
    static final int DESIGN_WIDTH = 2560, DESIGN_HEIGHT = 1440;
    static final float LEFT = 82, LOCAL_TOP = 1256, TEAM_STEP = 53, LAST_TEAM_TOP = 1214;
    static final int TEXT = 0xFFF3F7F8, NAME = 0xFFDCE5F1, NAME_OUTLINE = 0xFF1C7700;
    static final int MUTED = 0xFF8C999F, AVATAR_BACKGROUND = 0xFF10181E;
    static final int TRACK = 0x3310181E, HEALTH = 0xFF49C7A4, ARMOR = 0xFFFF57EE;
    static final int FILL = 0xFFFFFFFF, DANGER = 0xFFFF6D76, RESCUE = 0xFF49C7A4;
    static int memberColor(int number) { return dev.stya.blockzone.squad.SquadMemberColors.color(number); }
    private SquadHudStyle() {}
}
