package dev.stya.blockzone.client.ui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ModernUiAssetsTest {
    @Test void textureTransferKeepsChannelsAlphaAndTranslucentEdges() {
        assertEquals(0x12785634,ModernUiAssets.swapRedBlue(0x12345678));
        assertEquals(0x12345678,ModernUiAssets.swapRedBlue(ModernUiAssets.swapRedBlue(0x12345678)));
        assertEquals(0x807F3F1F,ModernUiAssets.unpremultiply(0x80402010));
        assertEquals(0xFF123456,ModernUiAssets.unpremultiply(0xFF123456));
        assertEquals(0,ModernUiAssets.unpremultiply(0));
    }
}
