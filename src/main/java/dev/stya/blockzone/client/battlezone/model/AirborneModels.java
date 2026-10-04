package dev.stya.blockzone.client.battlezone.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.stya.blockzone.BlockZone;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Standard block/item JSON models, baked and reloaded by Minecraft's resource-pack pipeline. */
@Mod.EventBusSubscriber(modid = BlockZone.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class AirborneModels {
    public static final ResourceLocation AIRCRAFT = model("transport_aircraft");
    public static final ResourceLocation PARACHUTE = model("parachute");
    public static final ResourceLocation PARACHUTE_PACK = model("parachute_pack");
    public static final ResourceLocation PROPELLER = model("propeller");
    public static final ResourceLocation AIRCRAFT_LIGHTS = model("aircraft_lights");
    public static final ResourceLocation AIRCRAFT_BEACON = model("aircraft_beacon");

    private AirborneModels() { }

    private static ResourceLocation model(String name) {
        return ResourceLocation.fromNamespaceAndPath(BlockZone.MOD_ID, "airborne/" + name);
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional event) {
        event.register(AIRCRAFT);
        event.register(PARACHUTE);
        event.register(PARACHUTE_PACK);
        event.register(PROPELLER);
        event.register(AIRCRAFT_LIGHTS);
        event.register(AIRCRAFT_BEACON);
    }

    public static void render(ResourceLocation id, float scale, PoseStack stack, VertexConsumer vertices, int light) {
        var minecraft = Minecraft.getInstance();
        var model = minecraft.getModelManager().getModel(id);
        stack.pushPose();
        stack.scale(scale, scale, scale);
        // The fixed display transform lets resource packs adjust scale, orientation and attachment.
        model = model.applyTransform(ItemDisplayContext.FIXED, stack, false);
        stack.translate(-.5, -.5, -.5);
        minecraft.getBlockRenderer().getModelRenderer().renderModel(stack.last(), vertices, null, model,
                1, 1, 1, light, OverlayTexture.NO_OVERLAY);
        stack.popPose();
    }
}
