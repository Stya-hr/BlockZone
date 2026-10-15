package dev.stya.blockzone.client.ui;

import icyllis.modernui.core.Context;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.text.TextPaint;
import icyllis.modernui.view.View;
import java.util.List;

/** Passive native view. Only immutable drawing snapshots cross from Minecraft to the UI thread. */
public final class ModernUiView extends View {
    private final java.util.function.Supplier<List<ModernUiGraphics.Frame>> frames;
    private final Paint paint=new Paint();
    private final TextPaint text=new TextPaint();
    public ModernUiView(Context context, java.util.function.Supplier<List<ModernUiGraphics.Frame>> frames) {
        super(context);this.frames=frames;setFocusable(false);setClickable(false);setWillNotDraw(false);
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for(var frame:frames.get()) frame.draw(canvas,paint,text);
    }
}
