# BlockZone 资源

所有物品统一在 `registry/BlockzoneItems` 注册，装备与四款场景箱集中展示在 **BlockZone** 创造物品栏中。方块、方块实体和实体分别在同一 `registry` 包中注册。

内置资源包位于 `src/main/resources/resourcepacks/blockzone/`，包含装备、场景箱、运输机、伞具、材质、语言和着色器。安装模组后自动启用，可在资源包界面看到；普通资源包放在其上方即可覆盖默认资源。资源 ID 与此前相同。

只维护游戏实际读取的 JSON 模型、PNG 材质等资源，不依赖 Blockbench 工程或模型生成器。动画由模组代码驱动。修改资源后使用 F3+T 重载。

运行 `./gradlew resourcePack` 导出 `build/distributions/blockzone-resourcepack.zip`；`assemble` 同时构建此完整包。导出的包与模组内置包使用同一份文件。

内置包中的 `data/blockzone/loot_tables/chests/weapons.json` 提供 TaCZ 枪械与弹药战利品，表 ID 为 `blockzone:chests/weapons`。仅安装 TaCZ 时加载该内置服务端数据；客户端材质包始终加载。通用战利品表仍为 `blockzone:chests/common`。

地图管理员通过 `/fpsm map modify battlezone <map> settings loot edit` 配置箱子的战利品表，通过 `settings sequence edit` 打开毒圈编辑器。开局装备使用 `capability loadout get|set <JSON>|clear|reset`；血量与单位插板护甲使用 `capability combat set {"matchHealth":100,"armorPlatePoints":50}`，修改在下一局生效。
