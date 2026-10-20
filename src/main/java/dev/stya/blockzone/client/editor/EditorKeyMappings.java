package dev.stya.blockzone.client.editor;

import com.mojang.blaze3d.platform.InputConstants;
import dev.stya.blockzone.BlockZone;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = BlockZone.MOD_ID, value = Dist.CLIENT)
public final class EditorKeyMappings {
    private enum Context implements IKeyConflictContext {
        EDITOR {
            @Override public boolean isActive() {
                return PoisonWorldEditor.session != null || LootWorldEditor.screen != null;
            }
        },
        POISON_WORLD {
            @Override public boolean isActive() {
                return PoisonWorldEditor.session != null && Minecraft.getInstance().screen == null;
            }
        };

        @Override public boolean conflicts(IKeyConflictContext other) { return other == this; }
    }

    private static final String CATEGORY = "key.categories.blockzone.editor";
    public static final KeyMapping FOCUS_CAMERA = new KeyMapping("key.blockzone.editor.focus_camera", Context.EDITOR,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F6, CATEGORY);
    public static final KeyMapping OPEN_POISON = new KeyMapping("key.blockzone.editor.open_poison", Context.POISON_WORLD,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_O, CATEGORY);
    public static final KeyMapping PREVIOUS_CIRCLE = new KeyMapping("key.blockzone.editor.previous_circle", Context.POISON_WORLD,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_BRACKET, CATEGORY);
    public static final KeyMapping NEXT_CIRCLE = new KeyMapping("key.blockzone.editor.next_circle", Context.POISON_WORLD,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_BRACKET, CATEGORY);

    private EditorKeyMappings() { }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        while (FOCUS_CAMERA.consumeClick()) {
            if (PoisonWorldEditor.session != null) PoisonEditorCamera.focus();
            else if (LootWorldEditor.screen != null) LootEditorCamera.focus();
        }
        while (OPEN_POISON.consumeClick()) {
            if (PoisonWorldEditor.session != null && mc.screen == null && !PoisonWorldEditor.saving)
                mc.setScreen(new PoisonEditorScreen());
        }
        while (PREVIOUS_CIRCLE.consumeClick()) selectCircle(-1);
        while (NEXT_CIRCLE.consumeClick()) selectCircle(1);
    }

    private static void selectCircle(int offset) {
        if (PoisonWorldEditor.session != null && Minecraft.getInstance().screen == null
                && !PoisonWorldEditor.saving && PoisonWorldEditor.draft != null)
            PoisonWorldEditor.draft.selectCircle(PoisonWorldEditor.draft.circle() + offset);
    }

    @Mod.EventBusSubscriber(modid = BlockZone.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent
        public static void register(RegisterKeyMappingsEvent event) {
            event.register(FOCUS_CAMERA);
            event.register(OPEN_POISON);
            event.register(PREVIOUS_CIRCLE);
            event.register(NEXT_CIRCLE);
        }
    }
}
