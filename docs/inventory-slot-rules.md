# 背包槽位规则（逻辑层）

每张 Battlezone 地图拥有独立的 `BattlezoneInventoryCapability`。它继承 FPSMatch
`MapCapability`，通过 `FPSMCapability.Savable` 和现有地图配置文件保存规则。
现有地图未配置该能力时使用默认规则；规则在每局开始时固定，修改于下一局生效。

槽位规则已接入玩家的武器拾取与弹药管理。面板显示服务端同步的备弹以及实际装备的武器，
目前为只读显示，不提供鼠标拖动或交换。其他道具仍沿用原版背包与现有物品使用流程。

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

`limit` 为未单独配置数量的物品提供默认上限。可选的 `item_limits` 为 `allowed` 中的
选择器设置独立上限，例如同一医疗槽放金苹果最多 8 个、纸最多 16 个、蜂蜜瓶最多 2 个：

```text
/fpsm map modify battlezone <地图名> capability inventory slot item.medkit {"allowed":["minecraft:golden_apple","minecraft:paper","minecraft:honey_bottle"],"limit":4,"item_limits":{"minecraft:golden_apple":8,"minecraft:paper":16,"minecraft:honey_bottle":2}}
```

`item_limits` 的键必须与 `allowed` 中某个选择器完全一致，也支持标签、NBT 与枪型选择器。
物品命中独立数量规则时覆盖 `limit`；命中多条独立数量规则时取最小值。
旧配置不含 `item_limits` 时行为保持一致。每个槽位仍只存一叠物品，数量限制按该槽位计算，
不是跨槽位的总携带量限制。

所有数量上限为 1..1024，同时受物品自身最大堆叠数限制。不存在的物品或空标签不会匹配。
规则变严格或数量上限降低时，已经保存的物品不会被删除，仍可取出。

## 弹药动态占槽与互斥

五个弹药槽动态分配：同一弹种在一个背包中最多占一个弹药槽。自动放入调用
`PanelInventory.insertAmmo(stack, simulate)`，先寻找同弹种的已有槽，再寻找符合规则的空槽。
已有槽满了、规则不再允许放入，或 NBT 不兼容导致无法堆叠时，剩余物品原样返回，
不会溢出到另一个空槽。调用方只应扣除真正放入的数量。弹药耗尽后该槽可接收其他弹种。
模拟插入不改变背包；直接 `insertItem` 和 `setStackInSlot` 同样检查跨弹药槽互斥。

TaCZ 弹药通过 `IAmmo.getAmmoId()` 识别，弹药箱通过 `IAmmoBox.getAmmoId()` 识别；
同弹种的散弹与弹药箱也不能分别占用两个弹药槽。弹药箱数量仍为物品数量，不自动拆箱。
额外 NBT 不改变弹种，但堆叠仍遵守 Forge 的物品与 NBT 一致性要求。

兼容 TACZ：铳械协议（Unidict）的两种实体弹药形式：TaCZ 自定义弹药 ID，以及配置为弹药的
普通物品注册 ID。普通物品需要添加到对应槽位的 `allowed`，也可通过标签配置；不会硬编码
通用弹药类别或枪械原始口径。此处只管理实体弹药占槽，未来换弹接入应复用 TaCZ / Unidict
运行时的寻弹与扣除逻辑，玩家接入见下文。

例如铳械协议配置使用 `tacz_unidict:rifle` 作为 TaCZ 弹药 ID 时，可用
`tacz:ammo{AmmoId:"tacz_unidict:rifle"}` 筛选并配置独立数量；如果使用普通物品模式，
则填写对应物品注册 ID。以服务端实际配置和物品为准。

读取存档会预先检查弹药互斥；有重复弹种时拒绝整次载入并保留当前内容，
不会静默删除或合并存档中的物品。符合互斥要求的存档仍可保留超出新规则的旧物品。

参考：[Unidict 官方弹药匹配实现](https://github.com/Scarasol/Tacz-Unidict/blob/master/src/main/java/com/scarasol/tud/mixin/AmmoItemMixin.java)、
[普通物品弹药实现](https://github.com/Scarasol/Tacz-Unidict/blob/master/src/main/java/com/scarasol/tud/mixin/AbstractGunItemMixin.java)。

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

当前 FPSMatch 只提供 MAP、TEAM 两类 Capability。规则属于地图；玩家级备弹存储与持久化
使用 Forge 玩家 Capability。FPSMatch 的 `ShopSlot` 含价格、购买和退款逻辑，
TaCZ 的 `GunSmithTableIngredient` 只包装配方 Ingredient 和数量，都不适合充当这里的存储槽。

其他逻辑通过 `CapabilityMap.getMapCapability(map, BattlezoneInventoryCapability.class)`
取得能力，再调用 `rule(InventorySlot)` 或 `createInventory()`。后者返回绑定该地图规则的
`PanelInventory`，提供 Forge `IItemHandler` / `IItemHandlerModifiable` 接口及 NBT 序列化。
需要由玩家存储层持有该实例，不能每次操作都重新创建。直接替换也会验证物品和数量。
读取存档保留已有物品，不会按新规则清空背包；存档大小必须是 17。

## 玩家拾取与换弹

`PlayerPanelInventory` 通过 Forge 玩家 Capability 保存当局备弹，并绑定地图名与局 UUID。
只有实际参加当前 Battlezone 比赛的玩家启用此流程；旁观者与比赛外玩家使用原有行为。
存档恢复须校验地图和局 ID，退出、重置与下一局不会继承上一局备弹。

- 武器直接进入快捷栏第 1、2 格，面板武器槽只读取这两格，不复制枪械。
  使用数字键 1、2 或原版滚轮切枪；枪械完整 NBT（配件、弹匣、枪膛等）保持不变。
  两格已满或规则拒绝时，第三把枪留在地面。
- 弹药进入五个互斥槽，遵守 `allowed`、`limit` 和 `item_limits`。部分拾取只扣除已接收数量。
  原版物品的拾取延迟、Owner 限制、Forge 拾取事件、统计和拾取动画继续生效；
  本模组战利品仍需右键拾取，不会变成走过自动收集。
- TaCZ 的换弹检查、脚本扣弹和背包直读寻弹改为读取玩家 Capability 的弹药视图；
  不替换全局 `ForgeCapabilities.ITEM_HANDLER`，不重写 TaCZ 或铳械协议的匹配/换弹时序。
  比赛中不从原版其他背包格读取备弹；枪内弹药仍由 TaCZ 管理。
- 初始装备中的枪与弹药，以及通过原版菜单移动到其他格的枪与弹药，会在服务端 tick
  归位。快捷栏第 1、2 格的普通物品移至其他空位；超出容量的物品落回地面，不复制或吞掉。
  因此当前请用快捷栏切枪，面板本身不支持拖动。
- 淘汰时武器和备弹生成绑定当局的地面战利品；实际死亡时备弹加入原版死亡掉落，
  克隆玩家不重复复制已掉落弹药。比赛结束或离场时清空当局备弹，原版物品由已有玩家快照恢复。
- 备弹变化时只同步给所属玩家，武器沿用原版同步。网络版本为 19，服务端与客户端需更新一致。

普通物品弹药需由模组使用者加入弹药槽允许规则。无限弹药、虚拟备弹等 TaCZ 枪械配置
仍遵循其原生设置；物品存储上限不改写枪械配置。

## 验证

```sh
./gradlew test --tests 'dev.stya.blockzone.inventory.*'
./gradlew runGameTestServer
```

单元测试覆盖配置序列化、旧配置兼容、错误数量配置的原子拒绝、槽位 ID、地图隔离与按局固定规则。
GameTest 在 Forge 环境覆盖物品/NBT/标签筛选、不同物品的独立上限、重叠数量规则、模拟插入、堆叠限制、直接替换、
规则收紧后的物品保留与取出，以及固定大小的 NBT 存储。
弹药测试覆盖动态占槽、同弹种禁止溢出、耗尽释放、NBT 差异和重复存档的原子拒绝。
可选加载 Unidict 时，还检查其实际生成的弹药以及原生寻弹扣除接口。
玩家集成测试覆盖真实物品拾取、归属与延迟、第三把枪拒绝、换弹脚本、存档校验、死亡掉落和克隆。
已验证 115 项单元测试及 16 项 GameTest；真实客户端通过 `minecraft-mod-mcp` 验证铳械协议
2.0.1 环境下拾取 64 发备弹后，换弹扣除 30 发，剩余 34 发，枪内为弹匣 29 发加枪膛 1 发。
