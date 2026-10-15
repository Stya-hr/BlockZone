package dev.stya.blockzone.client.editor;

import dev.stya.blockzone.client.ui.ModernUiScreen;
import icyllis.modernui.fragment.Fragment;
import net.minecraft.network.chat.Component;

/** Editor drafts stay on the game thread; the common native screen owns lifecycle and input. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = dev.stya.blockzone.BlockZone.MOD_ID, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public abstract class ModernEditorScreen extends ModernUiScreen {
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void hideExperience(net.minecraftforge.client.event.RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay() != net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.EXPERIENCE_BAR.type()) return;
        var screen = net.minecraft.client.Minecraft.getInstance().screen;
        if (screen instanceof ModernEditorScreen || screen instanceof EditorConfirmScreen
                || PoisonWorldEditor.hasFreeCamera() || LootWorldEditor.hasFreeCamera()) event.setCanceled(true);
    }
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
