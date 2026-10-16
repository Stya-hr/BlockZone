package dev.stya.blockzone.client.hud.chat;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

/** Only suppress messages whose information is already presented by the match HUD. */
final class HudChatFilter {
    static boolean replaced(Component message) {
        if (message.getContents() instanceof TranslatableContents translated) {
            if (translated.getKey().equals("commands.fpsm.debug.start.success")) return true;
            // Server command broadcasts wrap the original component in chat.type.admin.
            if (translated.getKey().equals("chat.type.admin")) {
                for (Object arg : translated.getArgs())
                    if (arg instanceof Component component && replaced(component)) return true;
            }
        }
        return message.getSiblings().stream().anyMatch(HudChatFilter::replaced);
    }
    private HudChatFilter() {}
}
