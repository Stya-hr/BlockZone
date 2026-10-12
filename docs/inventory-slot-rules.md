# 背包槽位规则（逻辑层）

每张 Battlezone 地图拥有独立的 `BattlezoneInventoryCapability`。它继承 FPSMatch
`MapCapability`，通过 `FPSMCapability.Savable` 和现有地图配置文件保存规则。
现有地图未配置该能力时使用默认规则；规则在每局开始时固定，修改于下一局生效。

本阶段完成槽位模型、配置、准入检查和可序列化的物品存储基础。面板仍为静态预览，
尚未接入鼠标操作、玩家物品同步、死亡掉落或物品使用；规则也不会限制原版背包。

## 配置命令

需要管理员权限（等级 2），`<地图名>` 替换为已有 Battlezone 地图名。

```text
/fpsm map modify battlezone <地图名> capability inventory get
/fpsm map modify battlezone <地图名> capability inventory slot weapon.primary {"allowed":["@gun:tacz:ak47","@gun:tacz:m4a1"],"limit":1}
/fpsm map modify battlezone <地图名> capability inventory slot item.medkit {"allowed":["minecraft:golden_apple","#my_pack:medical_items"],"limit":8}
/fpsm map modify battlezone <地图名> capability inventory slot item.battery {"allowed":[],"limit":64}
/fpsm map modify battlezone <地图名> capability inventory reset
```

`inventory slot <槽位ID> <JSON规则>` 只修改指定槽位，保留其他槽位配置。
`inventory set <JSON>` 可批量替换整份配置，例如：

```json
{
  "weapon.primary": {"allowed": ["@gun:*"], "limit": 1},
  "ammo.1": {"allowed": ["tacz:ammo{AmmoId:\"tacz:9mm\"}"], "limit": 64}
}
```

批量替换时未填写的槽位恢复默认规则。未知槽位 ID、错误选择器、非法数量或保存失败
会拒绝整次更新。沿用已有 `CapabilityConfiguration` 的保存验证与失败回滚。

## 筛选规则

`allowed` 中的条目满足任意一个即可放入；空列表表示禁止放入。

| 条目 | 含义 |
| --- | --- |
| `minecraft:golden_apple` | 物品注册 ID |
| `#my_pack:medical_items` | 物品标签；数据包重载后使用当前标签内容 |
| `tacz:ammo{AmmoId:"tacz:9mm"}` | 物品 ID 加部分 NBT 匹配，允许实际物品存在其他 NBT |
| `#my_pack:medical_items{Quality:2}` | 标签加部分 NBT 匹配 |
| `@gun:*` | FPSMatch 已注册枪械适配器支持的所有枪 |
| `@gun:tacz:ak47` | 由 FPSMatch 枪械适配器识别的具体枪型 ID |
| `*` | 任意非空物品 |

`limit` 为 1..1024，同时受物品自身最大堆叠数限制。不存在的物品或空标签不会匹配。
规则变严格或数量上限降低时，已经保存的物品不会被删除，仍可取出。

## 槽位 ID

按面板从上到下、每行从左到右排列，索引固定为 0..16。

| 索引 | ID | 默认允许 | 默认数量上限 |
| --- | --- | --- | --- |
| 0 | `equipment.backpack` | 空列表，待整合包指定 | 1 |
| 1 | `equipment.armor_expansion` | `blockzone:armor_expansion` | 1 |
| 2 | `equipment.helmet` | `blockzone:tactical_helmet` | 1 |
| 3 | `equipment.vest` | `blockzone:plate_carrier` | 1 |
| 4 | `equipment.shield` | `minecraft:shield` | 1 |
| 5 | `equipment.utility` | 空列表，待整合包指定 | 1 |
| 6 | `weapon.primary` | `@gun:*` | 1 |
| 7 | `weapon.secondary` | `@gun:*` | 1 |
| 8 | `item.grenade` | 空列表，待整合包指定 | 64 |
| 9 | `item.battery` | 空列表，待整合包指定 | 64 |
| 10 | `item.medkit` | 空列表，待整合包指定 | 64 |
| 11 | `item.armor_plates` | `blockzone:armor_plate` | 64 |
| 12..16 | `ammo.1` .. `ammo.5` | `tacz:ammo`，各槽可独立修改 | 64 |

空列表默认值避免猜测其他模组的背包、手雷、电池和医疗物品 ID。

## 复用和接入

- FPSMatch：`MapCapability`、`Savable`、`FPSMCapabilityManager` 处理地图能力生命周期与持久化；
  `GunCompatManager` / `IGunProvider` 处理枪型识别。
- 原版：`ItemPredicate` 处理物品、标签与 NBT 匹配，`TagParser` 解析 SNBT。
- Forge：`ItemStackHandler` 处理模拟插入、堆叠、取出和 NBT 存储。
- 本仓库：复用 `ConfigurationCapabilityCommand`、`CapabilityConfiguration`，
  沿用起始装备与战斗规则能力的配置流程。

当前 FPSMatch 只提供 MAP、TEAM 两类 Capability。规则属于地图；未来玩家级背包持久化
可使用 Forge 玩家 Capability。FPSMatch 的 `ShopSlot` 含价格、购买和退款逻辑，
TaCZ 的 `GunSmithTableIngredient` 只包装配方 Ingredient 和数量，都不适合充当这里的存储槽。

其他逻辑通过 `CapabilityMap.getMapCapability(map, BattlezoneInventoryCapability.class)`
取得能力，再调用 `rule(InventorySlot)` 或 `createInventory()`。后者返回绑定该地图规则的
`PanelInventory`，提供 Forge `IItemHandler` / `IItemHandlerModifiable` 接口及 NBT 序列化。
需要由后续玩家存储层持有该实例，不能每次操作都重新创建。直接替换也会验证物品和数量。
读取存档保留已有物品，不会按新规则清空背包；存档大小必须是 17。

## 验证

```sh
./gradlew test --tests 'dev.stya.blockzone.inventory.*'
./gradlew runGameTestServer
```

单元测试覆盖配置序列化、原子拒绝、槽位 ID、地图隔离与按局固定规则。
GameTest 在 Forge 环境覆盖物品/NBT/标签筛选、模拟插入、堆叠限制、直接替换、
规则收紧后的物品保留与取出，以及固定大小的 NBT 存储。
