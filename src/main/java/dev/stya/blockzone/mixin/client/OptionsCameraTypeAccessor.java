package dev.stya.blockzone.mixin.client;

import net.minecraft.client.CameraType;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Options.class)
public interface OptionsCameraTypeAccessor {
    @Accessor("cameraType")
    void blockzone$setCameraType(CameraType cameraType);
}
