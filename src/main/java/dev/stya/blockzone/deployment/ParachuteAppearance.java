package dev.stya.blockzone.deployment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Match-scoped styles. Each palette pass uses every color before repeating it. */
public final class ParachuteAppearance {
    private static final int[] COLORS = {
            0xEF5350, 0x42A5F5, 0xFFCA28, 0x66BB6A, 0xAB47BC, 0xFF7043,
            0x26C6DA, 0xEC407A, 0x9CCC65, 0x5C6BC0, 0xFFA726, 0x26A69A
    };
    public static final int PATTERNS = 4;
    public static final int STYLE_COUNT = COLORS.length * PATTERNS;
    private final Map<String, Integer> teams = new HashMap<>();
    private final ArrayList<Integer> styles = new ArrayList<>();

    public ParachuteAppearance() {
        reset();
    }

    public void reset() {
        teams.clear();
        styles.clear();
        var colors = new ArrayList<Integer>();
        for (int color = 0; color < COLORS.length; color++) colors.add(color);
        Collections.shuffle(colors);
        var patternsByColor = new HashMap<Integer, ArrayList<Integer>>();
        for (int color : colors) {
            var patterns = new ArrayList<Integer>();
            for (int pattern = 0; pattern < PATTERNS; pattern++) patterns.add(pattern);
            Collections.shuffle(patterns);
            patternsByColor.put(color, patterns);
        }
        for (int pass = 0; pass < PATTERNS; pass++) {
            for (int color : colors) styles.add(patternsByColor.get(color).get(pass) * COLORS.length + color);
        }
    }

    public int forTeam(String team) {
        return teams.computeIfAbsent(team, ignored -> styles.get(teams.size() % STYLE_COUNT));
    }

    public static int color(int style) {
        return COLORS[Math.floorMod(style, COLORS.length)];
    }

    /** Nine canopy cells: alternating stripes, center band, split, or contrasting wings. */
    public static boolean accent(int style, int cell) {
        return switch (Math.floorMod(style / COLORS.length, PATTERNS)) {
            case 0 -> cell % 2 == 0;
            case 1 -> cell >= 3 && cell <= 5;
            case 2 -> cell < 4;
            default -> cell <= 1 || cell >= 7;
        };
    }
}
