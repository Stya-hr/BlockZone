package dev.stya.blockzone.client.ui;

import dev.stya.blockzone.BlockZone;
import icyllis.modernui.mc.MuiModApi;
import icyllis.modernui.mc.UIManager;
import icyllis.modernui.view.WindowManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.ArrayList;
import java.util.List;

/** FPSMatch still selects HUDs; Modern UI owns their single passive overlay view. */
@Mod.EventBusSubscriber(modid=BlockZone.MOD_ID,value=Dist.CLIENT)
public final class ModernUiLayer {
    private static final List<ModernUiGraphics> pending=new ArrayList<>();
    private static volatile List<ModernUiGraphics.Frame> frames=List.of();
    private static volatile ModernUiView view;
    private static boolean attaching;
    public static boolean hudVisible() {
        var mc=net.minecraft.client.Minecraft.getInstance();
        return mc.player!=null&&!mc.options.hideGui
                &&(mc.screen==null||mc.screen instanceof dev.stya.blockzone.client.inventory.InventoryPanelScreen);
    }
    public static ModernUiGraphics hud() {
        var graphics=new ModernUiGraphics();pending.add(graphics);return graphics;
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void begin(RenderGuiEvent.Pre event) {pending.clear();}
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void end(RenderGuiEvent.Post event) {
        frames=hudVisible()
                ? pending.stream().map(ModernUiGraphics::frame).toList():List.of();
        if(view!=null){view.postInvalidate();return;}
        var manager=UIManager.getInstance();
        if(attaching||manager==null||manager.getDecorView()==null)return;
        attaching=true;
        MuiModApi.postToUiThread(()->{
            view=new ModernUiView(icyllis.modernui.ModernUI.getInstance(),()->frames,true);
            var params=new WindowManager.LayoutParams();
            params.width=params.height=-1;
            params.flags=WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
            manager.getDecorView().addView(view,0,params);
        });
    }
    @SubscribeEvent
    public static void visibility(net.minecraftforge.event.TickEvent.RenderTickEvent event) {
        if(event.phase==net.minecraftforge.event.TickEvent.Phase.START && !hudVisible()) {
            frames=List.of();var current=view;if(current!=null)current.postInvalidate();
        }
    }
    public static void invalidateTexture(net.minecraft.resources.ResourceLocation id){ModernUiAssets.invalidate(id);}
    public static void clearAssets(){ModernUiAssets.clear();}
    private ModernUiLayer(){}
}
