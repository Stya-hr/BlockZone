package dev.stya.blockzone.client.ui;

import icyllis.modernui.fragment.Fragment;
import icyllis.modernui.mc.MuiScreen;
import icyllis.modernui.mc.ScreenCallback;
import icyllis.modernui.mc.UIManager;
import icyllis.modernui.view.KeyEvent;
import icyllis.modernui.view.MotionEvent;
import icyllis.modernui.core.Core;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Shared native screen lifecycle and coordinate input routing for all mod interfaces. */
public abstract class ModernUiScreen extends Screen implements MuiScreen, ScreenCallback {
    private boolean attached;
    private int mouseButtons;
    private int windowWidth, windowHeight;
    protected ModernUiScreen(Component title) { super(title); }

    @Override protected void init() { attach(); }
    private void attach() {
        if (!attached) {
            UIManager.getInstance().initScreen(this);
            attached = true;
        }
    }
    @Override public void tick() {
        attach();
        var window = net.minecraft.client.Minecraft.getInstance().getWindow();
        if (window.getWidth() != windowWidth || window.getHeight() != windowHeight) {
            windowWidth = window.getWidth(); windowHeight = window.getHeight();
            UIManager.getInstance().initScreen(this);
        }
    }
    @Override public void render(GuiGraphics graphics, int x, int y, float partial) {
        attach();
        UIManager.getInstance().render(graphics, x, y, partial);
    }
    @Override public void removed() {
        mouseButtons = 0;
        if (attached) UIManager.getInstance().removed(this);
        attached = false;
        super.removed();
    }
    // Forward screen-local coordinates so input remains correct with GUI scaling and synthetic input providers.
    private void pointer(double x, double y, int action, int button, double scroll) {
        var window = net.minecraft.client.Minecraft.getInstance().getWindow();
        int modifiers = (hasControlDown() ? KeyEvent.META_CTRL_ON : 0) | (hasShiftDown() ? KeyEvent.META_SHIFT_ON : 0);
        var event = MotionEvent.obtain(Core.timeNanos(), action, button,
                (float)(x * window.getWidth() / Math.max(1, width)),
                (float)(y * window.getHeight() / Math.max(1, height)), modifiers, mouseButtons, 0);
        if (action == MotionEvent.ACTION_SCROLL) event.setAxisValue(MotionEvent.AXIS_VSCROLL, (float)scroll);
        UIManager.getInstance().getDecorView().getViewRoot().enqueueInputEvent(event);
    }
    @Override public void mouseMoved(double x, double y) { pointer(x, y, MotionEvent.ACTION_HOVER_MOVE, 0, 0); }
    @Override public boolean mouseClicked(double x, double y, int button) {
        int bit = 1 << button;
        mouseButtons |= bit;
        pointer(x, y, MotionEvent.ACTION_BUTTON_PRESS, bit, 0);
        if (mouseButtons == bit) pointer(x, y, MotionEvent.ACTION_DOWN, bit, 0);
        return true;
    }
    @Override public boolean mouseReleased(double x, double y, int button) {
        int bit = 1 << button;
        mouseButtons &= ~bit;
        pointer(x, y, MotionEvent.ACTION_BUTTON_RELEASE, bit, 0);
        if (mouseButtons == 0) pointer(x, y, MotionEvent.ACTION_UP, bit, 0);
        return true;
    }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        pointer(x, y, MotionEvent.ACTION_MOVE, 0, 0); return true;
    }
    @Override public boolean mouseScrolled(double x, double y, double delta) {
        pointer(x, y, MotionEvent.ACTION_SCROLL, 0, delta); return true;
    }
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        UIManager.getInstance().onKeyPress(key, scan, modifiers); return true;
    }
    @Override public boolean keyReleased(int key, int scan, int modifiers) {
        UIManager.getInstance().onKeyRelease(key, scan, modifiers); return true;
    }
    @Override public boolean charTyped(char ch, int modifiers) { return UIManager.getInstance().onCharTyped(ch); }
    @Override public Screen self() { return this; }
    @Override public abstract Fragment getFragment();
    @Override public ScreenCallback getCallback() { return this; }
    @Override public Screen getPreviousScreen() { return null; }
    @Override public boolean isMenuScreen() { return false; }
    @Override public void onBackPressed() { UIManager.getInstance().getOnBackPressedDispatcher().onBackPressed(); }
    @Override public boolean isBackKey(int key, KeyEvent event) { return key == KeyEvent.KEY_ESCAPE; }
    @Override public boolean shouldClose() { onClose(); return false; }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean hasDefaultBackground() { return false; }
    @Override public boolean shouldBlurBackground() { return false; }
}
