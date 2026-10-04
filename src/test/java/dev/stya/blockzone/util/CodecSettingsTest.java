package dev.stya.blockzone.util;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.ptcrys.fpsmatch.common.packet.mapselect.MapRoomSettingInfo;
import dev.stya.blockzone.util.battlezone.PoisonSettingsMigration;
import dev.stya.blockzone.util.battlezone.PoisonPath;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class CodecSettingsTest {
    @Test void codecParserAllowsUnrestrictedFiniteAltitudeAndRejectsInvalidInput() {
        var height = CodecSettings.create("battlezone", "deploymentHeight", CodecSettings.FINITE_DOUBLE, 64.0);
        assertTrue(height.parse("2500"));
        assertEquals(2500, height.get());
        assertTrue(height.parse("-500"));
        assertEquals(-500, height.get());
        for (String invalid : List.of("1e999", "null", "[]", "NaN"))
            assertThrows(RuntimeException.class, () -> CodecSettings.parse(CodecSettings.FINITE_DOUBLE, invalid));
        assertThrows(RuntimeException.class, () -> CodecSettings.parse(Codec.doubleRange(.1, 100), "101"));
    }

    @Test void nativeUiReceivesEditableJsonAndCodecDefaultInsteadOfJavaRecordText() {
        var codec = PoisonPath.CODEC.listOf();
        var initial = List.of(PoisonSettingsMigration.defaults(80, 80, 80));
        var setting = CodecSettings.create("battlezone", "poisonSequences", codec, initial);
        var changed = List.of(new PoisonPath(List.of(new PoisonPath.Circle(1, 2, 30, 0, 0, 3))));
        setting.set(changed);
        var previous = new MapRoomSettingInfo("poisonSequences", "unusable record text", "unusable default text", true,
                "setting.battlezone.poisonSequences", MapRoomSettingInfo.SettingType.OTHER, "description", false, 0, 0, 1, "battlezone");
        var result = CodecSettings.forUi(setting, previous);
        assertEquals(MapRoomSettingInfo.SettingType.STRING, result.type());
        assertTrue(result.editable());
        assertEquals(changed, codec.parse(JsonOps.INSTANCE, JsonParser.parseString(result.value())).result().orElseThrow());
        assertEquals(initial, codec.parse(JsonOps.INSTANCE, JsonParser.parseString(result.defaultValue())).result().orElseThrow());
        assertTrue(setting.parse(result.defaultValue()));
        assertEquals(initial, setting.get());
        var readOnly = new MapRoomSettingInfo("poisonSequences", "", "", false, "translation", MapRoomSettingInfo.SettingType.OTHER);
        assertFalse(CodecSettings.forUi(setting, readOnly).editable());
    }
}
