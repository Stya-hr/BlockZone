package dev.stya.blockzone.client.hud.flight;

import dev.stya.blockzone.client.hud.common.HudCanvas;
import java.util.Locale;
import net.minecraft.network.chat.Component;

/** Shared vertical tape for altitude and speed; invalid samples remain explicitly unknown. */
public final class FlightGauge {
    public void render(HudCanvas g, float x, float y, Component title, double value, double step, String unit) {
        g.panel(x, y, 136, 240);
        g.text(title, x + 12, y + 12, 14, HudCanvas.MUTED);
        if (Double.isFinite(value)) {
            double center = Math.floor(value / step) * step;
            for (int i = -3; i <= 3; i++) {
                double mark = center + i * step;
                float row = (float)(y + 120 - (mark - value) / step * 22);
                if (mark < 0 || row < y + 44 || row > y + 192) continue;
                g.rect(x + 92, row, 24, 1, HudCanvas.MUTED);
                g.right(Component.literal(String.format(Locale.ROOT, "%.0f", mark)), x + 82, row - 6, 12, HudCanvas.MUTED);
            }
            g.rect(x + 86, y + 119, 38, 3, HudCanvas.TEXT);
        }
        String number = Double.isFinite(value) ? String.format(Locale.ROOT, "%.0f", value) : "N/A";
        g.text(g.fit(Component.literal(number), 100, 28), x + 12, y + 200, 28, HudCanvas.TEXT);
        g.right(Component.literal(unit), x + 124, y + 221, 12, HudCanvas.MUTED);
    }
}
