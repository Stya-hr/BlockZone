package dev.stya.blockzone.client.hud.squad;

import dev.stya.blockzone.squad.SquadMemberStatus;

/** Avatar-only status signals; all marks are geometry, never font glyph substitutes. */
final class AvatarStatusOverlay {
    void render(SquadHudGraphics g, SquadMemberStatus member, float x, float y, float size) {
        int border = !member.online() ? SquadHudStyle.MUTED : !member.living()
                ? SquadHudStyle.DANGER : SquadHudStyle.TEXT;
        frame(g, x, y, size, border);
        if (!member.online() || !member.living()) g.rect(x + 2, y + 2, size - 4, size - 4, 0x9910181E);
        if (!member.online()) {
            float inset = size * .27f;
            g.line(x + inset, y + inset, x + size - inset, y + size - inset, size / 12, SquadHudStyle.TEXT);
            g.line(x + size - inset, y + inset, x + inset, y + size - inset, size / 12, SquadHudStyle.TEXT);
        } else if (member.living() && member.downed()) {
            g.rect(x + 2, y + 2, size - 4, size - 4, 0x9910181E);
            boolean rescue = member.rescueTicks() > 0;
            float progress = rescue ? member.rescueTicks() / (float)Math.max(1, member.rescueTotalTicks())
                    : member.bleedTicks() / (float)Math.max(1, member.bleedTotalTicks());
            progress(g, x, y, size, progress, rescue ? SquadHudStyle.RESCUE : SquadHudStyle.DANGER);
            int remaining = rescue ? Math.max(0, member.rescueTotalTicks() - member.rescueTicks()) : member.bleedTicks();
            float textSize = size >= 64 ? 24 : 16;
            g.centeredText(Integer.toString((remaining + 19) / 20), x + size / 2,
                    y + (size - textSize) / 2, textSize, SquadHudStyle.TEXT);
        }
        double now = System.nanoTime() / 1_000_000_000.0;
        double elapsed = now - g.animations.get(member.id()).hitAt;
        if (member.online() && member.living() && elapsed >= 0 && elapsed < .32) {
            int alpha = (int)(150 * (1 - elapsed / .32));
            g.rect(x + 2, y + 2, size - 4, size - 4, (alpha << 24) | 0xFF273D);
        }
    }
    private static void frame(SquadHudGraphics g, float x, float y, float size, int color) {
        g.rect(x, y, size, 2, color); g.rect(x, y + size - 2, size, 2, color);
        g.rect(x, y + 2, 2, size - 4, color); g.rect(x + size - 2, y + 2, 2, size - 4, color);
    }
    private static void progress(SquadHudGraphics g, float x, float y, float size, float fraction, int color) {
        float remaining = Math.max(0, Math.min(1, fraction)) * 4 * size;
        float top = Math.min(size, remaining); remaining -= top;
        float right = Math.min(size, remaining); remaining -= right;
        float bottom = Math.min(size, remaining); remaining -= bottom;
        g.rect(x, y, top, 2, color);
        g.rect(x + size - 2, y, 2, right, color);
        g.rect(x + size - bottom, y + size - 2, bottom, 2, color);
        g.rect(x, y + size - Math.min(size, remaining), 2, Math.min(size, remaining), color);
    }
}
