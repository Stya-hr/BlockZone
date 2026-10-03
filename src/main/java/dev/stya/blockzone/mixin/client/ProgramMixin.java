package dev.stya.blockzone.mixin.client;

import com.mojang.blaze3d.shaders.Program;
import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import dev.stya.blockzone.client.battlezone.BattlezoneMaterialShaderSource;
import org.apache.commons.io.IOUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.Charset;

@Mixin(Program.class)
public abstract class ProgramMixin {
    @Redirect(method = "compileShaderInternal", at = @At(value = "INVOKE",
            target = "Lorg/apache/commons/io/IOUtils;toString(Ljava/io/InputStream;Ljava/nio/charset/Charset;)Ljava/lang/String;", remap = false))
    private static String blockzone$materialSource(InputStream input, Charset charset, Program.Type type,
            String name, InputStream original, String sourceName, GlslPreprocessor preprocessor) throws IOException {
        return BattlezoneMaterialShaderSource.transform(type == Program.Type.VERTEX, name, IOUtils.toString(input, charset));
    }
}
