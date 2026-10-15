package dev.stya.blockzone.client.ui;

import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.text.TextDirectionHeuristics;
import icyllis.modernui.text.TextPaint;
import icyllis.modernui.text.TextShaper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

/** Records game-thread UI state; Modern UI owns all actual 2D drawing on its UI thread. */
public final class ModernUiGraphics extends GuiGraphics {
    interface Draw { void draw(Canvas canvas, Paint paint, TextPaint text); }
    public record Frame(List<Draw> commands, float scale) {
        void draw(Canvas canvas, Paint paint, TextPaint text) {
            canvas.save();
            canvas.scale(scale, scale);
            for (var command : commands) command.draw(canvas, paint, text);
            canvas.restore();
        }
    }
    private final List<Draw> commands = new ArrayList<>();
    private float alpha = 1;
    public ModernUiGraphics() { super(Minecraft.getInstance(), Minecraft.getInstance().renderBuffers().bufferSource()); }
    public Frame frame() { return new Frame(List.copyOf(commands), (float)Minecraft.getInstance().getWindow().getGuiScale()); }
    private void record(Draw draw) {
        var m = pose().last().pose();
        float x = m.m30(), y = m.m31(), sx = m.m00(), sy = m.m11();
        commands.add((c, p, t) -> {
            c.save(); c.translate(x, y); c.scale(sx, sy);
            draw.draw(c, p, t); c.restore();
        });
    }
    public void rect(float x, float y, float width, float height, int color) {
        if (width <= 0 || height <= 0) return;
        record((c, p, t) -> { p.setColor(color); c.drawRect(x, y, x + width, y + height, p); });
    }
    public void line(float x1, float y1, float x2, float y2, float thickness, int color) {
        record((c, p, t) -> { p.setColor(color); c.drawLine(x1, y1, x2, y2, Paint.Cap.BUTT, thickness, p); });
    }
    @Override public void fill(int x1, int y1, int x2, int y2, int color) { rect(x1, y1, x2 - x1, y2 - y1, color); }
    @Override public void fill(int x1, int y1, int x2, int y2, int z, int color) { fill(x1,y1,x2,y2,color); }
    @Override public void fill(RenderType type, int x1, int y1, int x2, int y2, int color) { fill(x1,y1,x2,y2,color); }
    @Override public void fill(RenderType type, int x1, int y1, int x2, int y2, int z, int color) { fill(x1,y1,x2,y2,color); }
    @Override public void flush() { }
    @Override public void setColor(float r, float g, float b, float a) { alpha = a; }
    @Override public int drawString(Font font, String value, float x, float y, int color, boolean shadow) {
        if (value == null || value.isEmpty()) return 0;
        int argb = (color & 0xFF000000) == 0 ? color | 0xFF000000 : color;
        int width = font.width(value);
        record((c, p, t) -> {
            t.setTextSize(8); t.setTypeface(icyllis.modernui.ModernUI.getSelectedTypeface());
            char[] chars = value.toCharArray();
            var shaped = TextShaper.shapeText(chars, 0, chars.length, TextDirectionHeuristics.FIRSTSTRONG_LTR, t);
            // Match the existing Minecraft-space advance, preserving reference layout and hit geometry.
            c.save(); c.translate(x, y);
            if (shaped.getAdvance() > 0) c.scale(width / shaped.getAdvance(), 1);
            if (shadow) { p.setColor(0x40000000); c.drawShapedText(shaped, .5f, 7.5f, p); }
            p.setColor(argb); c.drawShapedText(shaped, 0, 7, p); c.restore();
        });
        return width;
    }
    @Override public int drawString(Font font, String value, int x, int y, int color, boolean shadow) { return drawString(font,value,(float)x,y,color,shadow); }
    @Override public int drawString(Font font, Component value, int x, int y, int color, boolean shadow) { return drawString(font,value.getString(),x,y,color,shadow); }
    @Override public int drawString(Font font, FormattedCharSequence value, float x, float y, int color, boolean shadow) {
        var text = new StringBuilder(); value.accept((index, style, codepoint) -> { text.appendCodePoint(codepoint); return true; });
        return drawString(font,text.toString(),x,y,color,shadow);
    }
    @Override public int drawString(Font font, FormattedCharSequence value, int x, int y, int color, boolean shadow) { return drawString(font,value,(float)x,y,color,shadow); }
    @Override public void renderComponentTooltip(Font font, List<Component> lines, int mouseX, int mouseY) {
        int width=lines.stream().mapToInt(font::width).max().orElse(0);
        int x=Math.min(mouseX+12,guiWidth()-width-8),y=Math.min(mouseY+8,guiHeight()-lines.size()*12-8);
        rect(x-4,y-4,width+8,lines.size()*12+8,0xF0182028);
        for(int i=0;i<lines.size();i++)drawString(font,lines.get(i),x,y+i*12,0xFFF3F7F8,false);
    }
    public void asset(String key, int width, int height, java.util.function.Consumer<GuiGraphics> renderer) {
        var pixels=ModernUiAssets.asset(key,width,height,renderer);
        image(pixels,0,0,width,height,0,0,pixels.width(),pixels.height());
    }
    private void image(ModernUiAssets.Pixels pixels, float x, float y, float w, float h, float u, float v, float uw, float vh) {
        float opacity = alpha;
        record((c, p, t) -> {
            var image = pixels.image();
            if (image == null) return;
            p.setColor(0xFFFFFFFF); p.setAlphaF(opacity); p.setFilter(false);
            c.drawImage(image, u, v, u + uw, v + vh, x, y, x + w, y + h, p);
        });
    }
    @Override public void blit(ResourceLocation id, int x, int y, int w, int h, float u, float v, int uw, int vh, int tw, int th) {
        var pixels = ModernUiAssets.texture(id);
        if (pixels != null) image(pixels,x,y,w,h,u*pixels.width()/tw,v*pixels.height()/th,uw*pixels.width()/(float)tw,vh*pixels.height()/(float)th);
    }
    @Override public void blit(ResourceLocation id, int x, int y, int z, float u, float v, int w, int h, int tw, int th) { blit(id,x,y,w,h,u,v,w,h,tw,th); }
    @Override public void blit(ResourceLocation id, int x, int y, float u, float v, int w, int h, int tw, int th) { blit(id,x,y,w,h,u,v,w,h,tw,th); }
    @Override public void renderItem(ItemStack stack, int x, int y) {
        if (stack.isEmpty()) return;
        var pixels = ModernUiAssets.item(stack);
        image(pixels,x,y,16,16,0,0,pixels.width(),pixels.height());
    }
}
