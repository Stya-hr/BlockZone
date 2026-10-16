package dev.stya.blockzone.editor.zone;

/** Validation shared by per-field feedback and draft application. */
public final class PoisonEditorInput {
    public static boolean valid(int index, String text) {
        try {
            double value = Double.parseDouble(text);
            if (!Double.isFinite(value)) return false;
            return switch (index) {
                case 0, 1 -> true;
                case 2 -> value >= 0 && value <= 30000000;
                case 3, 4 -> Integer.parseInt(text) >= 0 && Integer.parseInt(text) <= 1000000;
                case 5 -> value >= 0 && value <= Float.MAX_VALUE;
                default -> false;
            };
        } catch (RuntimeException invalid) { return false; }
    }
    private PoisonEditorInput() {}
}
