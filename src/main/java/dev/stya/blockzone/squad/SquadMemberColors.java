package dev.stya.blockzone.squad;

/** Stable index palette shared by every viewer; never wraps a four-color array. */
public final class SquadMemberColors {
    private static final int[] COLORS = {0xFF34C759, 0xFF0088FF, 0xFFFFCC00, 0xFFC38AFF,
            0xFFFF8A3D, 0xFF00D5D5, 0xFFFF68AF, 0xFFA5DE32};
    public static int color(int index) {
        if (index < 1) throw new IllegalArgumentException("Squad member index starts at 1");
        if (index <= COLORS.length) return COLORS[index - 1];
        // Golden-angle spacing for arbitrarily configured squads. Number remains the primary identifier.
        float hue = (float)((index * 137.50776405) % 360) / 60;
        float chroma = .72f, secondary = chroma * (1 - Math.abs(hue % 2 - 1)), minimum = .22f;
        float r, g, b;
        if (hue < 1) { r = chroma; g = secondary; b = 0; }
        else if (hue < 2) { r = secondary; g = chroma; b = 0; }
        else if (hue < 3) { r = 0; g = chroma; b = secondary; }
        else if (hue < 4) { r = 0; g = secondary; b = chroma; }
        else if (hue < 5) { r = secondary; g = 0; b = chroma; }
        else { r = chroma; g = 0; b = secondary; }
        return 0xFF000000 | Math.round((r + minimum) * 255) << 16
                | Math.round((g + minimum) * 255) << 8 | Math.round((b + minimum) * 255);
    }
    private SquadMemberColors() {}
}
