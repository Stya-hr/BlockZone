package dev.stya.blockzone.client.editor;

import dev.stya.blockzone.client.ui.ModernUiScreen;
import icyllis.modernui.fragment.Fragment;
import net.minecraft.network.chat.Component;

/** Editor drafts stay on the game thread; the common native screen owns lifecycle and input. */
public abstract class ModernEditorScreen extends ModernUiScreen {
    final EditorPanel fragment = new EditorPanel(this);
    volatile boolean textFocused;
    volatile float panelFraction = .3f;
    ModernEditorScreen(Component title) { super(title); }
    abstract void buildPanel(EditorPanel ui);
    void updatePanel(EditorPanel ui) {}
    void save() {}
    boolean hasSaveAction() { return true; }
    final boolean editingText() { return textFocused; }
    final int panelLeft() { return Math.max(0, width - Math.round(width * panelFraction)); }
    final void refreshPanel() { fragment.refresh(); }
    final void clearTextFocus() { textFocused = false; fragment.clearTextFocus(); }
    @Override public Fragment getFragment() { return fragment; }
    @Override public void tick() { super.tick(); fragment.update(); }
    @Override public void removed() { textFocused = false; super.removed(); }
}
