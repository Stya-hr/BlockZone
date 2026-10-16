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
    private final boolean clearBeforeDraw;
    private final Paint paint=new Paint();
    private final TextPaint text=new TextPaint();
    public ModernUiView(Context context, java.util.function.Supplier<List<ModernUiGraphics.Frame>> frames) {
        this(context,frames,false);
    }
    ModernUiView(Context context, java.util.function.Supplier<List<ModernUiGraphics.Frame>> frames, boolean clearBeforeDraw) {
        super(context);this.frames=frames;this.clearBeforeDraw=clearBeforeDraw;
        setFocusable(false);setClickable(false);setWillNotDraw(false);
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // Clear the shared transparent surface once, before screens draw above the HUD.
        if(clearBeforeDraw)canvas.drawColor(0,icyllis.modernui.graphics.BlendMode.CLEAR);
        for(var frame:frames.get()) frame.draw(canvas,paint,text);
    }
}
