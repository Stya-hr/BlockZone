package dev.stya.blockzone.client.ui;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import icyllis.modernui.graphics.Bitmap;
import icyllis.modernui.graphics.Image;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import java.util.HashMap;
import java.util.Map;

/** Imports Minecraft-only textures and baked item models once; UI compositing stays in Modern UI. */
final class ModernUiAssets {
    static final class Pixels {
        private final int width, height;
        private final int[] colors;
        private Image image;
        Pixels(NativeImage source, boolean flip) {
            width=source.getWidth(); height=source.getHeight(); colors=new int[width*height];
            for(int y=0;y<height;y++) for(int x=0;x<width;x++) {
                int abgr=source.getPixelRGBA(x,flip?height-1-y:y);
                int argb=swapRedBlue(abgr);
                colors[y*width+x]=flip?unpremultiply(argb):argb;
            }
        }
        int width(){return width;} int height(){return height;}
        Image image() {
            if(image==null) try(var bitmap=Bitmap.createBitmap(width,height,Bitmap.Format.RGBA_8888)) {
                // Core 3.12.0 setPixels(int[]) shifts the Java array base offset as well as the index.
                // Write explicit RGBA bytes, avoiding its misaligned source pointer.
                var pixels=org.lwjgl.system.MemoryUtil.memIntBuffer(bitmap.getAddress(),colors.length);
                for(int argb:colors)pixels.put(swapRedBlue(argb));
                image=Image.createTextureFromBitmap(bitmap);
            }
            return image;
        }
        void close(){if(image!=null){image.close();image=null;}}
    }
    private static final Map<ResourceLocation,Pixels> TEXTURES=new HashMap<>();
    private static final Map<String,Pixels> ITEMS=new java.util.LinkedHashMap<>(64,.75f,true);
    private static TextureTarget itemTarget;
    static Pixels texture(ResourceLocation id) {
        return TEXTURES.computeIfAbsent(id, key -> {
            var mc=Minecraft.getInstance();
            var resource=mc.getResourceManager().getResource(key);
            try {
                if(resource.isPresent()) try(var stream=resource.get().open();var image=NativeImage.read(stream)) {return new Pixels(image,false);}
                int previous=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
                try {
                    RenderSystem.bindTexture(mc.getTextureManager().getTexture(key).getId());
                    int width=GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D,0,GL11.GL_TEXTURE_WIDTH);
                    int height=GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D,0,GL11.GL_TEXTURE_HEIGHT);
                    if(width<=0||height<=0)return null;
                    try(var image=new NativeImage(width,height,false)){image.downloadTexture(0,false);return new Pixels(image,false);}
                } finally {RenderSystem.bindTexture(previous);}
            } catch(java.io.IOException e) {throw new IllegalStateException("Cannot import UI texture "+key,e);}
        });
    }
    static Pixels item(ItemStack stack) {
        var copy=stack.copy();copy.setCount(1);
        String key=copy.save(new net.minecraft.nbt.CompoundTag()).toString();
        return asset(key,128,128,gui->{gui.pose().scale(8,8,1);gui.renderItem(copy,0,0);});
    }
    static Pixels asset(String key,int width,int height,java.util.function.Consumer<GuiGraphics> renderer) {
        if(ITEMS.size()>=256&&!ITEMS.containsKey(key)) {
            var first=ITEMS.entrySet().iterator();var old=first.next().getValue();first.remove();
            icyllis.modernui.mc.MuiModApi.postToUiThread(old::close);
        }
        return ITEMS.computeIfAbsent(key,ignored->bake(width,height,renderer));
    }
    private static Pixels bake(int width,int height,java.util.function.Consumer<GuiGraphics> renderer) {
        var mc=Minecraft.getInstance();
        if(itemTarget==null||itemTarget.width!=width||itemTarget.height!=height) {
            if(itemTarget!=null)itemTarget.destroyBuffers();
            itemTarget=new TextureTarget(width,height,true,Minecraft.ON_OSX);
        }
        var projection=new Matrix4f(RenderSystem.getProjectionMatrix());
        var sorting=RenderSystem.getVertexSorting();
        var model=RenderSystem.getModelViewStack();model.pushPose();model.setIdentity();RenderSystem.applyModelViewMatrix();
        try {
            itemTarget.setClearColor(0,0,0,0);itemTarget.clear(Minecraft.ON_OSX);itemTarget.bindWrite(true);
            RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0,width,height,0,-1000,1000),VertexSorting.ORTHOGRAPHIC_Z);
            var gui=new GuiGraphics(mc,mc.renderBuffers().bufferSource());
            renderer.accept(gui);gui.flush();
            int previous=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            try(var image=new NativeImage(width,height,false)) {
                RenderSystem.bindTexture(itemTarget.getColorTextureId());image.downloadTexture(0,false);
                return new Pixels(image,true);
            } finally {RenderSystem.bindTexture(previous);}
        } finally {
            model.popPose();RenderSystem.applyModelViewMatrix();RenderSystem.setProjectionMatrix(projection,sorting);
            mc.getMainRenderTarget().bindWrite(true);RenderSystem.defaultBlendFunc();
        }
    }
    static void invalidate(ResourceLocation id) {
        var old=TEXTURES.remove(id);
        if(old!=null)icyllis.modernui.mc.MuiModApi.postToUiThread(old::close);
    }
    static void clear() {
        var old=new java.util.ArrayList<>(TEXTURES.values());old.addAll(ITEMS.values());TEXTURES.clear();ITEMS.clear();
        icyllis.modernui.mc.MuiModApi.postToUiThread(()->old.forEach(Pixels::close));
        if(itemTarget!=null){itemTarget.destroyBuffers();itemTarget=null;}
    }
    static int swapRedBlue(int color) {
        return (color&0xFF00FF00)|((color&255)<<16)|((color>>>16)&255);
    }
    static int unpremultiply(int color) {
        int alpha=color>>>24;
        if(alpha==0||alpha==255)return color;
        int red=Math.min(255,((color>>>16)&255)*255/alpha);
        int green=Math.min(255,((color>>>8)&255)*255/alpha);
        int blue=Math.min(255,(color&255)*255/alpha);
        return (alpha<<24)|(red<<16)|(green<<8)|blue;
    }
    private ModernUiAssets(){}
}
