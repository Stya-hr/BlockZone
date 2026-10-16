package dev.stya.blockzone.client.hud.squad;

/** Shared inset frame and colored lower edge, preserving the reference bar bounds. */
final class VitalBar {
    static void render(SquadHudGraphics g, float x, float y, float width, float height,
                       VitalBarMotion motion, int fillColor, int accent) {
        float fill = motion.value();
        float trail = motion.trail();
        float edge = Math.min(1, height / 5);
        float underline = Math.min(2, height / 3);
        g.rect(x, y, width, height, dev.stya.blockzone.client.hud.common.HudCanvas.PANEL_BACKGROUND);
        // A quiet outline keeps empty health and plate capacity readable over terrain.
        g.rect(x, y, width, edge, 0x997C8B95);
        g.rect(x, y + edge, edge, height - edge, 0x997C8B95);
        g.rect(x + width - edge, y + edge, edge, height - edge, 0x997C8B95);
        g.rect(x + edge, y + edge, (width - 2 * edge) * trail,
                height - edge - underline, 0xFFB96159);
        g.rect(x + edge, y + edge, (width - 2 * edge) * fill,
                height - edge - underline, fillColor);
        g.rect(x, y + height - underline, width, underline, (accent & 0x00FFFFFF) | 0x44000000);
        g.rect(x, y + height - underline, width * fill, underline, accent);
    }
    private VitalBar() {}
}
