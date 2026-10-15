package dev.stya.blockzone.client.inventory;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.view.ViewBox;
import com.github.weisj.jsvg.parser.LoaderContext;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.mojang.blaze3d.platform.NativeImage;
import dev.stya.blockzone.BlockZone;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** SVG is the source asset; GPU textures are rebuilt at the actual display resolution. */
public final class InventorySvgAssets {
    public record Texture(ResourceLocation id, int width, int height) { }
    private static final Map<String, Texture> CACHE = new HashMap<>();

    private InventorySvgAssets() { }

    public static Texture get(String name, int width, int height) {
        Texture cached = CACHE.get(name);
        if (cached != null && cached.width == width && cached.height == height) return cached;
        Minecraft mc = Minecraft.getInstance();
        ResourceLocation source = ResourceLocation.fromNamespaceAndPath(BlockZone.MOD_ID,
                "gui/inventory/" + name + ".svg");
        try (var stream = mc.getResourceManager().open(source)) {
            SVGDocument document = new SVGLoader().load(stream, null, LoaderContext.createDefault());
            if (document == null) throw new IOException("Invalid SVG: " + source);
            BufferedImage raster = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            var graphics = raster.createGraphics();
            try {
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                document.render(null, graphics, new ViewBox(0, 0, width, height));
            } finally {
                graphics.dispose();
            }
            NativeImage image = new NativeImage(width, height, false);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int argb = raster.getRGB(x, y);
                    int abgr = (argb & 0xFF00FF00) | ((argb >>> 16) & 0xFF) | ((argb & 0xFF) << 16);
                    image.setPixelRGBA(x, y, abgr);
                }
            }
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(BlockZone.MOD_ID,
                    "dynamic/inventory/" + name);
            if (cached != null) mc.getTextureManager().release(cached.id);
            DynamicTexture texture = new DynamicTexture(image);
            texture.setFilter(true, false);
            mc.getTextureManager().register(id, texture);
            Texture result = new Texture(id, width, height);
            CACHE.put(name, result);
            return result;
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot render inventory SVG " + source, exception);
        }
    }

    static void clear() {
        var manager = Minecraft.getInstance().getTextureManager();
        CACHE.values().forEach(texture -> manager.release(texture.id));
        CACHE.clear();
    }
}
