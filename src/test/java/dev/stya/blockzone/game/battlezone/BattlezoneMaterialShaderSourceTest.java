package dev.stya.blockzone.game.battlezone;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BattlezoneMaterialShaderSourceTest {
    private static final String SOURCE = "#version 150\nout vec4 fragColor;\nvoid main() { fragColor = vec4(1); }\n";
    @Test void transformsWorldMaterialsAndPreservesOriginalAlpha() {
        String fragment = BattlezoneMaterialShaderSource.transform(false, "rendertype_solid", SOURCE);
        assertTrue(fragment.contains("void blockzoneOriginalMain()"));
        assertTrue(fragment.contains("fragColor.rgb ="));
        assertFalse(fragment.contains("fragColor.a ="));
        assertFalse(fragment.contains("DepthSampler"));
        assertTrue(fragment.contains("dot(delta, delta)"));
    }
    @Test void leavesSkyGuiAndCustomShadersAlone() {
        for (String name : new String[]{"position", "rendertype_sky", "rendertype_gui", "blockzone:battlezone_sphere"}) {
            assertEquals(SOURCE, BattlezoneMaterialShaderSource.transform(false, name, SOURCE));
        }
    }
    @Test void derivesWorldPositionFromTheMaterialVertex() {
        String vertex = BattlezoneMaterialShaderSource.transform(true, "minecraft:particle", SOURCE);
        assertTrue(vertex.contains("BlockzoneClipToWorld * gl_Position"));
        assertTrue(vertex.startsWith("#version 150\n"));
    }
}
