package dev.stya.blockzone.mixin.client;

import dev.stya.blockzone.client.editor.PoisonWorldEditor;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LevelRenderer.class)
public abstract class EditorChunkVisibilityMixin {
    /** Keep loaded ground sections visible when the editor camera flies above the usual vertical range. */
    @Redirect(method = "getRelativeFrom", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;abs(I)I", ordinal = 1))
    private int blockzone$editorVerticalDistance(int distance) {
        return (PoisonWorldEditor.hasFreeCamera() || dev.stya.blockzone.client.editor.LootWorldEditor.hasFreeCamera()) ? 0 : Mth.abs(distance);
    }
}
