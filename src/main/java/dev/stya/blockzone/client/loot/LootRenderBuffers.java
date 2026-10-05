package dev.stya.blockzone.client.loot;

import net.minecraft.client.renderer.MultiBufferSource;

/** Scope the entity's outline buffer to custom renderers that otherwise use global buffers. */
public final class LootRenderBuffers {
    private static final ThreadLocal<MultiBufferSource> CURRENT = new ThreadLocal<>();
    private LootRenderBuffers() { }
    public static MultiBufferSource current() { return CURRENT.get(); }
    public static MultiBufferSource set(MultiBufferSource buffers) {
        var previous = CURRENT.get();
        if (buffers == null) CURRENT.remove(); else CURRENT.set(buffers);
        return previous;
    }
}
