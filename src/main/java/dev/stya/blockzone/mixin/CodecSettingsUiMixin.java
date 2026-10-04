package dev.stya.blockzone.mixin;

import com.ptcrys.fpsmatch.common.mapselect.MapRoomQueryService;
import com.ptcrys.fpsmatch.common.packet.mapselect.MapRoomSettingInfo;
import com.ptcrys.fpsmatch.core.data.Setting;
import dev.stya.blockzone.util.CodecSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Preserve native FPSMatch editing/permissions; present structured codecs as round-trippable JSON. */
@Mixin(value = MapRoomQueryService.class, remap = false)
public abstract class CodecSettingsUiMixin {
    @Inject(method = "settingInfo", at = @At("RETURN"), cancellable = true)
    private static <T> void blockzone$jsonSetting(Setting<T> setting, boolean editable, String gameType,
            CallbackInfoReturnable<MapRoomSettingInfo> callback) {
        callback.setReturnValue(CodecSettings.forUi(setting, callback.getReturnValue()));
    }
}
