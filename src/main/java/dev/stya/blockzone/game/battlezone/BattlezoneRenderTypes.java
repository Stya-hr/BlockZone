package dev.stya.blockzone.game.battlezone;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = "blockzone", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public abstract class BattlezoneRenderTypes extends RenderType {
    private static ShaderInstance boundaryShader;
    private static ShaderInstance sphereShader;
    private static final RenderType SPHERE = create("battlezone_sphere", DefaultVertexFormat.POSITION,
            VertexFormat.Mode.QUADS, 256, false, false, CompositeState.builder()
            .setShaderState(new ShaderStateShard(() -> sphereShader))
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL)
            .setOutputState(PARTICLES_TARGET).setWriteMaskState(COLOR_WRITE).createCompositeState(false));

    static RenderType sphere() { return SPHERE; }
    static ShaderInstance sphereShader() { return sphereShader; }
    private static final Map<ResourceLocation, RenderType> BOUNDARY_TYPES = new HashMap<>();

    private BattlezoneRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode,
                                 int bufferSize, boolean affectsCrumbling, boolean sortOnUpload,
                                 Runnable setup, Runnable clear) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setup, clear);
    }

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                ResourceLocation.fromNamespaceAndPath("blockzone", "battlezone_sphere"),
                DefaultVertexFormat.POSITION), shader -> sphereShader = shader);
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                ResourceLocation.fromNamespaceAndPath("blockzone", "battlezone_boundary"),
                DefaultVertexFormat.NEW_ENTITY), shader -> boundaryShader = shader);
    }

    static RenderType boundary(ResourceLocation texture) {
        return BOUNDARY_TYPES.computeIfAbsent(texture, location -> create("battlezone_boundary",
                DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, false, true,
                CompositeState.builder()
                        .setShaderState(new ShaderStateShard(() -> boundaryShader))
                        .setTextureState(new TextureStateShard(location, false, false))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setCullState(NO_CULL)
                        .setOutputState(PARTICLES_TARGET)
                        .setWriteMaskState(COLOR_WRITE)
                        .createCompositeState(false)));
    }
}
