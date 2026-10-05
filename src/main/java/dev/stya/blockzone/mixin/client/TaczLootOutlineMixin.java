package dev.stya.blockzone.mixin.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.stya.blockzone.client.loot.LootRenderBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** TaCZ's Bedrock renderer bypasses the buffer passed to its item renderer. */
@Pseudo
@Mixin(targets = "com.tacz.guns.client.model.bedrock.BedrockModel", remap = false)
public abstract class TaczLootOutlineMixin {
    @Redirect(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/client/renderer/RenderType;IIFFFF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;getBuffer(Lnet/minecraft/client/renderer/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;", remap = true), remap = false)
    private VertexConsumer blockzone$lootOutline(MultiBufferSource.BufferSource original, RenderType type) {
        var scoped = LootRenderBuffers.current();
        return (scoped == null ? original : scoped).getBuffer(type);
    }
}
