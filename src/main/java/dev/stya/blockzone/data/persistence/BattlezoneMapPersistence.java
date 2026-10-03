package dev.stya.blockzone.data.persistence;

import dev.stya.blockzone.map.battlezone.BattlezoneMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.ptcrys.fpsmatch.common.event.register.RegisterFPSMSaveDataEvent;
import com.ptcrys.fpsmatch.core.FPSMCore;
import com.ptcrys.fpsmatch.core.data.AreaData;
import com.ptcrys.fpsmatch.core.map.BaseMap;
import com.ptcrys.fpsmatch.core.persistence.FPSMDataManager;
import com.ptcrys.fpsmatch.core.persistence.SaveHolder;
import dev.stya.blockzone.BlockZone;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

@Mod.EventBusSubscriber(modid = BlockZone.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BattlezoneMapPersistence {
    private static final Logger LOGGER = LoggerFactory.getLogger(BattlezoneMapPersistence.class);
    private static final String SAVE_FOLDER = "BattlezoneMaps";
    private static final String SAVE_FILE = "maps";
    private static final String SETTINGS_FOLDER = "BattlezoneSettings";
    private static final String SETTINGS_HOLDER_FILE_TYPE = "fpsmatch-settings-holder";

    private BattlezoneMapPersistence() {
    }

    @SubscribeEvent
    public static void registerSaveData(RegisterFPSMSaveDataEvent event) {
        SaveHolder<SavedMaps> saveHolder = new SaveHolder.Builder<>(SavedMaps.CODEC)
                .withVersion(1)
                .withInitializer(SavedMaps::empty)
                .withLoadHandler(BattlezoneMapPersistence::loadMaps)
                .withSaveHandler(BattlezoneMapPersistence::saveMaps)
                .build();
        event.registerData(SavedMaps.class, SAVE_FOLDER, saveHolder);

        // BaseMap.saveConfig/loadConfig ask FPSMatch for a per-map save folder keyed by the concrete map class.
        // The actual map definitions and settings remain in their own FPSMatch-managed files.
        SaveHolder<BattlezoneMap> settingsFolderHolder = new SaveHolder.Builder<BattlezoneMap>(
                Codec.STRING.xmap(ignored -> null, BattlezoneMap::getMapName))
                .withVersion(1)
                .withFileType(SETTINGS_HOLDER_FILE_TYPE)
                .withInitializer(() -> null)
                .withLoadHandler(ignored -> { })
                .withSaveHandler(ignored -> { })
                .build();
        event.registerData(BattlezoneMap.class, SETTINGS_FOLDER, settingsFolderHolder);
    }

    private static void saveMaps(FPSMDataManager dataManager) {
        FPSMCore core = FPSMCore.getInstance();
        List<BattlezoneMap> battlezoneMaps = core.getMapByClass(BattlezoneMap.class);
        battlezoneMaps.forEach(BattlezoneMap::saveConfig);
        List<SavedMap> maps = battlezoneMaps.stream().map(BattlezoneMapPersistence::toSavedMap).toList();
        dataManager.saveData(new SavedMaps(maps), SAVE_FILE, true);
    }

    private static SavedMap toSavedMap(BattlezoneMap map) {
        AreaData area = map.getMapArea();
        return new SavedMap(
                map.getMapName(),
                map.getServerLevel().dimension().location().toString(),
                area.pos1(),
                area.pos2());
    }

    private static void loadMaps(SavedMaps savedMaps) {
        FPSMCore core = FPSMCore.getInstance();
        MinecraftServer server = core.getServer();
        var factory = core.getPreBuildGame(BattlezoneMap.GAME_TYPE);
        if (factory == null) {
            LOGGER.error("Cannot restore Battlezone maps because the game type is not registered");
            return;
        }

        for (SavedMap savedMap : savedMaps.maps()) {
            ResourceLocation dimensionId = ResourceLocation.tryParse(savedMap.dimension());
            if (dimensionId == null) {
                LOGGER.warn("Cannot restore Battlezone map {}: invalid dimension {}", savedMap.name(), savedMap.dimension());
                continue;
            }

            ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, dimensionId));
            if (level == null) {
                LOGGER.warn("Cannot restore Battlezone map {}: dimension {} is not loaded", savedMap.name(), savedMap.dimension());
                continue;
            }

            BaseMap baseMap = factory.apply(level, savedMap.name(), new AreaData(savedMap.pos1(), savedMap.pos2()));
            if (!(baseMap instanceof BattlezoneMap map)) {
                LOGGER.error("Cannot restore Battlezone map {}: registered factory returned {}", savedMap.name(), baseMap.getClass().getName());
                continue;
            }

            if (!core.registerMap(BattlezoneMap.GAME_TYPE, map)) {
                LOGGER.warn("Skipped duplicate Battlezone map {} while restoring saved maps", savedMap.name());
            } else {
                try {
                    map.loadConfig();
                } catch (RuntimeException exception) {
                    LOGGER.error("Failed to restore settings for Battlezone map {}", savedMap.name(), exception);
                }
            }
        }
    }

    private record SavedMaps(List<SavedMap> maps) {
        private static final Codec<SavedMaps> CODEC = SavedMap.CODEC.listOf()
                .xmap(SavedMaps::new, SavedMaps::maps);

        private SavedMaps {
            maps = List.copyOf(maps);
        }

        private static SavedMaps empty() {
            return new SavedMaps(List.of());
        }
    }

    private record SavedMap(String name, String dimension, BlockPos pos1, BlockPos pos2) {
        private static final Codec<SavedMap> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("name").forGetter(SavedMap::name),
                Codec.STRING.fieldOf("dimension").forGetter(SavedMap::dimension),
                BlockPos.CODEC.fieldOf("pos1").forGetter(SavedMap::pos1),
                BlockPos.CODEC.fieldOf("pos2").forGetter(SavedMap::pos2)
        ).apply(instance, SavedMap::new));
    }
}
