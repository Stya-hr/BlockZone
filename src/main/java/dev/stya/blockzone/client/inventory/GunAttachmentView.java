package dev.stya.blockzone.client.inventory;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

/** Optional gun API adapter for installed and built-in attachments. */
public final class GunAttachmentView {
    public record Attachment(Component name, ItemStack item) {}
    public static List<Attachment> of(ItemStack stack) {
        return !stack.isEmpty() && ModList.get().isLoaded("tacz") ? Tacz.of(stack) : empty();
    }
    private static List<Attachment> empty() {
        return List.of("scope", "muzzle", "stock", "grip", "laser", "extended_mag").stream()
                .map(type -> new Attachment(Component.translatable("attachment.blockzone." + type), ItemStack.EMPTY)).toList();
    }
    private static final class Tacz {
        static List<Attachment> of(ItemStack stack) {
            var gun = com.tacz.guns.api.item.IGun.getIGunOrNull(stack);
            if (gun == null) return empty();
            return java.util.Arrays.stream(com.tacz.guns.api.item.attachment.AttachmentType.values())
                    .filter(type -> type != com.tacz.guns.api.item.attachment.AttachmentType.NONE)
                    .map(type -> {
                        var item = gun.getAttachment(stack, type);
                        if (item.isEmpty()) item = gun.getBuiltinAttachment(stack, type);
                        return new Attachment(Component.translatable("attachment.blockzone." + type.name().toLowerCase(java.util.Locale.ROOT)), item);
                    }).toList();
        }
    }
    private GunAttachmentView() {}
}
