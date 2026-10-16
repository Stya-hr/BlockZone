package dev.stya.blockzone.client.hud.flight;

import dev.stya.blockzone.client.hud.common.HudCanvas;
import java.util.Locale;
import net.minecraft.network.chat.Component;

/** Background-free vertical tape, mirrored so both scales face the player. */
public final class FlightGauge {
    public void render(HudCanvas g, float x, float y, Component title, double value, double step, String unit, boolean left) {
        float rail = left ? x + 88 : x + 8;
        label(g, title, rail, y - 24, 15, left, HudCanvas.TEXT);
        g.rect(rail, y, 1, 114, 0x99F3F7F8);
        g.rect(rail, y + 166, 1, 114, 0x99F3F7F8);
        if (Double.isFinite(value)) {
            double minorStep = step / 2;
            double center = Math.floor(value / minorStep) * minorStep;
            for (int i = -11; i <= 11; i++) {
                double mark = center + i * minorStep;
                float row = (float)(y + 140 - (mark - value) / minorStep * 13);
                if (mark < 0 || row < y || row > y + 280 || Math.abs(row - y - 140) < 26) continue;
                boolean major = Math.abs(mark / step - Math.rint(mark / step)) < 0.001;
                float length = major ? 12 : 6;
                g.rect(left ? rail - length : rail, row, length, 1, major ? HudCanvas.TEXT : 0x99F3F7F8);
                if (major) label(g, Component.literal(String.format(Locale.ROOT, "%.0f", mark)),
                        rail, row - 7, 14, left, HudCanvas.MUTED);
            }
        }
        // A short break in the tape leaves the live reading clear without a backing plate.
        g.rect(left ? rail - 16 : rail, y + 140, 16, 1, HudCanvas.TEXT);
        String number = Double.isFinite(value) ? String.format(Locale.ROOT, "%.0f", value) : "N/A";
        var reading = Component.literal(number);
        var units = Component.literal(unit);
        float numberWidth = g.textWidth(reading, 26);
        float width = numberWidth + 7 + g.textWidth(units, 13);
        float start = left ? rail - 20 - width : rail + 20;
        shadowText(g, reading, start, y + 127, 26, HudCanvas.TEXT);
        shadowText(g, units, start + numberWidth + 7, y + 127 + (26 - 13) * 7f / 8, 13, HudCanvas.MUTED);
        g.rect(left ? rail - 6 : rail, y, 6, 1, HudCanvas.TEXT);
        g.rect(left ? rail - 6 : rail, y + 279, 6, 1, HudCanvas.TEXT);
    }
    private void label(HudCanvas g, Component text, float rail, float y, float size, boolean left, int color) {
        float x = left ? rail - 20 - g.textWidth(text, size) : rail + 20;
        shadowText(g, text, x, y, size, color);
    }
    private void shadowText(HudCanvas g, Component text, float x, float y, float size, int color) {
        g.text(text, x + 1, y + 1, size, 0x99000000);
        g.text(text, x, y, size, color);
    }
}
