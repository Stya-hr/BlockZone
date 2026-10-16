package dev.stya.blockzone.client.hud.chat;

import java.util.UUID;

/** A display snapshot; signed messages and interactive history remain owned by vanilla chat. */
public record ChatMessage(UUID sender, String name, String body, long receivedAt, dev.stya.blockzone.chat.ChatChannel channel) {
    public ChatMessage(UUID sender, String name, String body, long receivedAt) { this(sender, name, body, receivedAt, null); }
    public float opacity(long now) {
        float remaining = Math.max(0, Math.min(1, (10_000 - (now - receivedAt)) / 2_000f));
        return remaining * remaining * (3 - 2 * remaining);
    }
}
