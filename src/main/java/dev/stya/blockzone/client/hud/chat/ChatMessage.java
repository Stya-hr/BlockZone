package dev.stya.blockzone.client.hud.chat;

import java.util.UUID;

/** A display snapshot; signed messages and interactive history remain owned by vanilla chat. */
public record ChatMessage(UUID sender, String name, String body, long receivedAt) {
    public float opacity(long now) {
        return Math.max(0, Math.min(1, (10_000 - (now - receivedAt)) / 2_000f));
    }
}
