package dev.stya.blockzone.client.inventory;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

/** Uses the same skin-aware HUD texture as TaCZ, fitted at its native aspect ratio. */
public final class GunHudIcon {
    private record Size(int width, int height) { }
    private static final Map<ResourceLocation, Optional<Size>> SIZES = new HashMap<>();

    public static boolean draw(GuiGraphics gui, ItemStack stack, int x, int y, int width, int height) {
        if (!ModList.get().isLoaded("tacz")) return false;
        var texture = Tacz.texture(stack);
        if (texture.isEmpty()) return false;
        var size = SIZES.computeIfAbsent(texture.get(), GunHudIcon::readSize);
        if (size.isEmpty()) return false;
        float factor = Math.min((float) width / size.get().width, (float) height / size.get().height);
        gui.flush();
        gui.setColor(1, 1, 1, 1);
        RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc();
        gui.pose().pushPose();
        gui.pose().translate(x + (width - size.get().width * factor) / 2F,
                y + (height - size.get().height * factor) / 2F, 0);
        gui.pose().scale(factor, factor, 1);
        gui.blit(texture.get(), 0, 0, 0, 0, size.get().width, size.get().height, size.get().width, size.get().height);
        gui.pose().popPose();
        gui.flush(); RenderSystem.disableBlend();
        return true;
    }

    private static Optional<Size> readSize(ResourceLocation texture) {
        var resource = Minecraft.getInstance().getResourceManager().getResource(texture);
        if (resource.isEmpty()) return Optional.empty();
        try (var stream = resource.get().open(); var image = NativeImage.read(stream)) {
            return Optional.of(new Size(image.getWidth(), image.getHeight()));
        } catch (IOException failure) {
            org.apache.logging.log4j.LogManager.getLogger().warn("Cannot read gun HUD texture {}", texture, failure);
            return Optional.empty();
        }
    }

    public static void clear() { SIZES.clear(); }

    private static final class Tacz {
        static Optional<ResourceLocation> texture(ItemStack stack) {
            return com.tacz.guns.api.TimelessAPI.getGunDisplay(stack)
                    .map(com.tacz.guns.client.resource.GunDisplayInstance::getHUDTexture);
        }
    }
    private GunHudIcon() { }
}
