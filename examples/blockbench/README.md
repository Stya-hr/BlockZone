# Blockzone 战术装备

本目录提供可直接在 Blockbench 打开的 `.bbmodel` 源项目：头盔、普通背心、带扩容配件背心、护膝裤、战术靴、插板、扩容配件。使用方块几何和 128×128 像素材质，贴近 Minecraft 与现有场景的风格。

插板项目包含 `animation.blockzone.plating`，时长 2 秒（40 tick），依次取板、举板、插入和收手。游戏同时提供第一人称双手插板与第三人称手臂姿势，受伤或取消使用立即终止。

`python tools/generate_equipment_assets.py` 可从统一几何定义重新生成项目、物品 JSON、穿戴模型 JSON 和材质。该脚本只依赖 Python 标准库。脚本重新生成会覆盖本目录中的编辑，编辑前请另存。

穿戴导出位于 `assets/blockzone/models/equipment/`，物品导出位于 `assets/blockzone/models/item/`。前者采用 Minecraft HumanoidModel 的像素坐标和骨骼（head/body/left_leg/right_leg），由客户端构建穿戴模型；后者是标准 Java Block/Item JSON。

## 对局装备与扩容

默认开局穿戴战术头盔、双槽插板背心、护膝裤和战术靴，并携带两块插板；初始护甲值为零。每板护甲量由地图的战斗 Capability 配置控制。

手持 `blockzone:armor_expansion` 使用，安装到正在穿戴的插板背心上，扩容为三槽。按 N 拆卸（可在按键设置中修改），配件返回背包，背包满时掉落。安装不补充护甲；拆卸将护甲上限和当前超额护甲降至两槽。背心被脱下后，对局中的护甲归零。

通过地图的 `BattlezoneLoadoutCapability` 指定穿戴和初始物品，例如：

```mcfunction
fpsm map modify battlezone flat160 capability loadout set [{"slot":"head","item":"blockzone:tactical_helmet"},{"slot":"chest","item":"blockzone:plate_carrier"},{"slot":"legs","item":"blockzone:tactical_leggings"},{"slot":"feet","item":"blockzone:tactical_boots"},{"slot":"hotbar.0","item":"blockzone:armor_plate","count":2},{"slot":"hotbar.1","item":"blockzone:armor_expansion"}]
```

槽位支持 `head/chest/legs/feet/offhand`、`hotbar.0` 至 `hotbar.8`、`inventory.0` 至 `inventory.26`。`count` 默认为 1，`nbt` 为可选 SNBT 字符串，可配置 TaCZ 枪械等模组物品。预装扩容的背心可使用 `"nbt":"{BlockzoneArmorExpansion:1b}"`。配置在下一局开始时生效，中途加入使用该局同一份配置；非法物品、重复槽位和错误 NBT 会拒绝保存，开局也会再次校验，不会发放部分装备。结算后仍恢复入场前的玩家状态和物品。

使用 `capability loadout get` 查看配置，`clear` 清空初始装备，`reset` 恢复默认装备（前缀同上）。命令自动保存到该地图配置的 `capabilities` 字段。旧 `settings startingLoadout` 已移除，旧装备配置不会迁移，需要重新配置。

战斗参数通过以下命令配置，修改在下一局生效：

```mcfunction
fpsm map modify battlezone flat160 capability combat set {"matchHealth":100,"armorPlatePoints":50}
```

开局使用 FPSMatch 原生 `autoStart`、`autoStartTime`、`readyStartEnabled`、`readyStartTime`；时间单位为 tick。旧 `countdownSeconds`、血量和毒圈 Settings 已删除，需要重新配置。

毒圈编辑器仍可通过 `fpsm map modify battlezone flat160 settings sequence edit` 打开。路径命令改为 `capability zone sequence ...`，完整配置使用 `capability zone get|set <JSON>|reset`，修改自动保存。世界编辑、相机、形状选择、预览和保存功能保留。战利品编辑器仍使用 `settings loot edit`。

地图配置文件现在同时保存地图能力和队伍能力；不保存对局成员或本局分数。
