package dev.stya.blockzone.client.loot;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.stya.blockzone.loot.LootDropEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemDisplayContext;

public final class LootDropRenderer extends EntityRenderer<LootDropEntity> {
    private final ItemRenderer items;

    public LootDropRenderer(EntityRendererProvider.Context context) {
        super(context);
        items = context.getItemRenderer();
        shadowRadius = 0.2F;
        shadowStrength = 0.65F;
    }

    @Override
    public void render(LootDropEntity entity, float yaw, float partialTicks, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        var stack = entity.getItem();
        if (stack.isEmpty()) return;
        var model = items.getModel(stack, entity.level(), null, entity.getId());
        pose.pushPose();
        pose.mulPose(Axis.YP.rotation(entity.getSpin(partialTicks)));
        var itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId.getNamespace().equals("tacz") && itemId.getPath().equals("modern_kinetic_gun")) {
            // TaCZ's default GROUND pose stands the weapon on its magazine. Turn it onto its side.
            pose.translate(0, 0.08, 0);
            pose.mulPose(Axis.ZP.rotationDegrees(90));
        } else if (model.isCustomRenderer()) {
            pose.translate(0, 0.035, 0);
        } else if (!model.isGui3d()) {
            // Generated sprite items have thickness; lay them flat instead of leaving them upright in midair.
            pose.translate(0, 0.035, 0);
            pose.mulPose(Axis.XP.rotationDegrees(90));
        } else {
            // Respect the item's GROUND transform, including TaCZ's own 3D gun renderer.
            pose.translate(0, model.getTransforms().ground.scale.y() * 0.25F, 0);
        }
        items.render(stack, ItemDisplayContext.GROUND, false, pose, buffers, light, OverlayTexture.NO_OVERLAY, model);
        pose.popPose();
        super.render(entity, yaw, partialTicks, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(LootDropEntity entity) { return TextureAtlas.LOCATION_BLOCKS; }
}
