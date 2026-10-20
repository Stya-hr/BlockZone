package dev.stya.blockzone.client.hud.action;

import dev.stya.blockzone.client.hud.common.HudCanvas;
import dev.stya.blockzone.client.inventory.GunHudIcon;
import dev.stya.blockzone.client.loot.DropOutlines;
import dev.stya.blockzone.client.ui.ModernUiGraphics;
import dev.stya.blockzone.loot.LootDropEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

/** REDSEC-style name, item preview and action strip, tethered to the actual ground item. */
final class GroundItemPanel {
    private static final float WIDTH = 480, HEIGHT = 252, HEADER = 42, FOOTER = 48;

    void render(HudCanvas g, LootDropEntity drop, float partial) {
        var anchor = DropOutlines.project(drop, partial, g.width, g.height);
        if (anchor == null) return;
        float x = Mth.clamp(anchor.x - WIDTH / 2, 24, g.width - WIDTH - 24);
        float y = anchor.y - HEIGHT - 128;
        if (y < 24) y = anchor.y + 128;
        y = Mth.clamp(y, 24, g.height - HEIGHT - 24);
        var graphics = (ModernUiGraphics) g.gui;
        float edgeX = Mth.clamp(anchor.x, x + 16, x + WIDTH - 16);
        float edgeY = anchor.y > y + HEIGHT ? y + HEIGHT : y;
        graphics.line(edgeX, edgeY, anchor.x, anchor.y, 1, 0xA6F3F7F8);
        g.rect(anchor.x - 3, anchor.y - 3, 6, 6, HudCanvas.TEXT);

        g.rect(x, y, WIDTH, HEIGHT, 0xEB1A2933);
        g.rect(x, y, WIDTH, HEADER, 0xF5071117);
        g.rect(x, y + HEIGHT - FOOTER, WIDTH, FOOTER, 0xF5071117);
        g.rect(x, y + HEADER, WIDTH, 1, 0x406E858F);
        g.rect(x, y + HEIGHT - FOOTER, WIDTH, 1, 0x406E858F);
        for (float cornerX : new float[]{x, x + WIDTH - 12}) {
            g.rect(cornerX, y, 12, 1, HudCanvas.TEXT);
            g.rect(cornerX, y + HEIGHT - 1, 12, 1, HudCanvas.TEXT);
        }
        for (float cornerX : new float[]{x, x + WIDTH - 1}) {
            g.rect(cornerX, y, 1, 12, HudCanvas.TEXT);
            g.rect(cornerX, y + HEIGHT - 12, 1, 12, HudCanvas.TEXT);
        }

        var stack = drop.getItem();
        var name = g.fit(stack.getHoverName(), WIDTH - (stack.getCount() > 1 ? 96 : 32), 24);
        g.text(name, x + 16, y + 9, 24, HudCanvas.TEXT);
        if (stack.getCount() > 1) g.right(net.minecraft.network.chat.Component.literal("×" + stack.getCount()),
                x + WIDTH - 16, y + 11, 20, HudCanvas.MUTED);
        if (!GunHudIcon.draw(g.gui, stack, Math.round(x + 32), Math.round(y + HEADER + 16),
                Math.round(WIDTH - 64), 128)) {
            g.gui.pose().pushPose();
            try {
                g.gui.pose().translate(x + (WIDTH - 128) / 2, y + HEADER + 16, 0);
                g.gui.pose().scale(8, 8, 1);
                g.gui.renderItem(stack, 0, 0);
            } finally { g.gui.pose().popPose(); }
        }

        float promptY = y + HEIGHT - FOOTER + 14;
        int keyWidth = g.key(Minecraft.getInstance().options.keyUse, Math.round(x + 16), Math.round(promptY));
        g.text(ActionStatusSource.tr("pickup"), x + 16 + keyWidth + 10, promptY, 20, HudCanvas.TEXT);
    }
}
