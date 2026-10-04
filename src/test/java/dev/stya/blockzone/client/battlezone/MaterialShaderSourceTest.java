package dev.stya.blockzone.client.battlezone;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MaterialShaderSourceTest {
    private static final String SOURCE = "#version 150\nout vec4 fragColor;\nvoid main() { fragColor = vec4(1); }\n";
    @Test void transformsWorldMaterialsAndPreservesOriginalAlpha() {
        String fragment = MaterialShaderSource.transform(false, "rendertype_solid", SOURCE);
        assertTrue(fragment.contains("void blockzoneOriginalMain()"));
        assertTrue(fragment.contains("fragColor.rgb ="));
        assertFalse(fragment.contains("fragColor.a ="));
        assertFalse(fragment.contains("DepthSampler"));
        assertTrue(fragment.contains("dot(delta.xz, delta.xz)"));
    }
    @Test void leavesSkyGuiAndCustomShadersAlone() {
        for (String name : new String[]{"position", "rendertype_sky", "rendertype_gui", "blockzone:battlezone_zone"}) {
            assertEquals(SOURCE, MaterialShaderSource.transform(false, name, SOURCE));
        }
    }
    @Test void derivesWorldPositionFromTheMaterialVertex() {
        String vertex = MaterialShaderSource.transform(true, "minecraft:particle", SOURCE);
        assertTrue(vertex.contains("BlockzoneClipToWorld * gl_Position"));
        assertTrue(vertex.startsWith("#version 150\n"));
    }
}
