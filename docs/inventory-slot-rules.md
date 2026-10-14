# 背包槽位规则（逻辑层）

每张 Battlezone 地图拥有独立的 `BattlezoneInventoryCapability`。它继承 FPSMatch
`MapCapability`，通过 `FPSMCapability.Savable` 和现有地图配置文件保存规则。
现有地图未配置该能力时使用默认规则；规则在每局开始时固定，修改于下一局生效。

槽位规则接入武器、原版四部位护甲、饰品、道具和备弹。面板显示实际装备并支持丢弃和同组槽位拖动交换。第一行是两个饰品槽加头、胸、腿、脚四个玩家装备槽。
完整初始背包由现有 `BattlezoneLoadoutCapability` 负责，参见 [饰品与初始背包](inventory-accessories.md)。

## 配置命令

需要管理员权限（等级 2），`<地图名>` 替换为已有 Battlezone 地图名。

```text
/fpsm map modify battlezone <地图名> capability inventory get
/fpsm map modify battlezone <地图名> capability inventory type weapon {"allowed":["@gun:tacz:ak47","@gun:tacz:m4a1"],"limit":1}
/fpsm map modify battlezone <地图名> capability inventory type item.tactical {"allowed":["minecraft:golden_apple","#my_pack:medical_items"],"limit":8}
/fpsm map modify battlezone <地图名> capability inventory type item.tactical {"allowed":[],"limit":64}
/fpsm map modify battlezone <地图名> capability inventory reset
```

`inventory type <槽位类型> <JSON规则>` 修改该类型全部格子的规则，保留其他类型配置。
`inventory set <JSON>` 可批量替换整份配置，例如：

```json
{
  "weapon": {"allowed": ["@gun:*"], "limit": 1},
  "ammo": {"allowed": ["tacz:ammo{AmmoId:\"tacz:9mm\"}"], "limit": 64}
}
```

批量替换时未填写的类型恢复默认规则。未知槽位类型、错误选择器、非法数量或保存失败
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
| `*` | 当前槽位类型内的任意非空物品 |

`limit` 为未单独配置数量的物品提供默认上限。可选的 `item_limits` 为 `allowed` 中的
选择器设置独立上限，例如同一战术道具槽放金苹果最多 8 个、纸最多 16 个、蜂蜜瓶最多 2 个：

```text
/fpsm map modify battlezone <地图名> capability inventory type item.tactical {"allowed":["minecraft:golden_apple","minecraft:paper","minecraft:honey_bottle"],"limit":4,"item_limits":{"minecraft:golden_apple":8,"minecraft:paper":16,"minecraft:honey_bottle":2}}
```

`item_limits` 的键必须与 `allowed` 中某个选择器完全一致，也支持标签、NBT 与枪型选择器。
物品命中独立数量规则时覆盖 `limit`；命中多条独立数量规则时取最小值。
未填写 `item_limits` 时使用槽位默认上限。每个槽位仍只存一叠物品，数量限制按该槽位计算，
不是跨槽位的总携带量限制。

武器、四个护甲槽和饰品槽的数量固定为 1；其他槽位数量上限为 1..1024，同时受物品自身最大堆叠数限制。不存在的物品或空标签不会匹配。
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

## 槽位类型

下表按存储索引列出；第一行的显示顺序为两个饰品加头、胸、腿、脚。
存储与面板顺序一致，共 17 格，按下表类型配置。同类格子不加编号，并共用规则；带编号的旧配置键不再接受。

| 索引 | 配置类型 | 默认允许 | 默认数量上限 |
| --- | --- | --- | --- |
| 0 | `accessory` | `*`，仍须是饰品 | 1 |
| 1 | `accessory` | `*`，仍须是饰品 | 1 |
| 2 | `equipment.helmet` | `blockzone:tactical_helmet` | 1 |
| 3 | `equipment.vest` | `blockzone:plate_carrier` | 1 |
| 4 | `equipment.legs` | `blockzone:tactical_leggings` | 1 |
| 5 | `equipment.feet` | `blockzone:tactical_boots` | 1 |
| 6 | `weapon` | `@gun:*` | 1 |
| 7 | `weapon` | `@gun:*` | 1 |
| 8 | `item.throwable` | 空列表，待整合包指定 | 64 |
| 9 | `item.tactical` | 空列表，待整合包指定 | 64 |
| 10 | `item.tactical` | 空列表，待整合包指定 | 64 |
| 11 | `item.armor_plates` | `blockzone:armor_plate` | 64 |
| 12..16 | `ammo` | `tacz:ammo`，同类格子共用规则 | 64 |

中间四格从左至右为投掷物、战术道具、战术道具、护甲板。两个战术道具槽共用允许物品和数量上限配置。

投掷物和战术道具默认允许列表为空，由整合包指定物品或标签。

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
读取存档保留已有物品，不会按新规则清空背包；当前存储为 17 格，必须包含 `Schema: 2` 与 `Size: 17`；旧格式直接拒绝，不自动迁移。

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
  归位。快捷栏第 1 至 6 格中不符合对应槽位的普通物品移至其他空位；超出容量的物品落回地面，不复制或吞掉。
  切换手持武器仍使用快捷栏；面板支持按住攻击键（默认左键）拖动交换主副武器位置。
- 淘汰时武器和备弹生成绑定当局的地面战利品；实际死亡时备弹加入原版死亡掉落，
  克隆玩家不重复复制已掉落弹药。比赛结束或离场时清空当局备弹，原版物品由已有玩家快照恢复。
- 备弹变化时只同步给所属玩家，武器沿用原版同步。网络版本为 23，服务端与客户端需更新一致。

普通物品弹药需由模组使用者加入弹药槽允许规则。无限弹药、虚拟备弹等 TaCZ 枪械配置
仍遵循其原生设置；物品存储上限不改写枪械配置。

## 面板交互

- 接管 Forge 原版背包打开事件（生存和创造背包），不注册独立打开键。
  开关和关闭提示使用 `options.keyInventory`，默认 E，原版改键立即生效。
- 道具槽使用实际快捷栏第 3 至 6 格（投掷物、战术道具、战术道具、护甲板），可用原版物品使用流程。
  匹配弹药规则的物品优先进入备弹槽。道具和备弹显示当前数量 / 服务端配置上限；空槽显示 0 / 上限。
- 悬停高亮，顶部显示物品名称。TaCZ 枪械右上角按其 API 显示配件与内置配件。
- 攻击绑定丢 1 个，潜行 + 攻击绑定丢最多 10 个，使用绑定全部丢弃（默认左键、Shift + 左键、右键）。
  提示读取原版已注册的 `keyAttack`、`keyShift`、`keyUse`，改键后的键盘和鼠标绑定均用于实际操作。服务端校验比赛、局 ID 和槽位物品版本，
  数量取服务端实际物品，掉落失败则回滚；战利品绑定当局并保留完整 NBT。
- 点击空白处关闭；方向键、跳跃或疾跑输入关闭并转交同一次输入。移动中不会打开面板。
  潜行绑定保留给批量丢弃，不触发移动关闭。
- 按键提示由 `InventoryPromptRenderer` 统一提供，安装 Smart Key Prompts 时使用其公开的
  `KeyRenderer.drawKeyBoardKey` 渲染器及 `KeyUtils.translateKey` 原生键名解析，未安装时使用自带样式。
  Shift、Ctrl、Alt、空格、回车、Tab、退格、方向键和鼠标左／中／右键优先使用 SVG 图形，其他键显示实际绑定键名。
  组合键逐键独立绘制；图标宽度参与布局测量，按下态跟随实际键盘／鼠标状态。
  1.1.3-hotfix 自带按下/未按下键帽纹理，没有独立的鼠标图形资源；兼容层复用这些键帽材质，并叠加本模组的 SVG 图形。无需添加强制依赖。
  已适配 Smart Key Prompts Forge 1.20.1 的 1.1.3-hotfix API（前置 OELib）。
  OELib 0.2.4.1 的网络通道要求服务端同步安装该库，否则 Forge 会拒绝联机。

## 验证

```sh
./gradlew test --tests 'dev.stya.blockzone.inventory.*'
./gradlew runGameTestServer
```

单元测试覆盖配置序列化、旧槽位拒绝、错误数量配置的原子拒绝、槽位 ID、地图隔离与按局固定规则。
GameTest 在 Forge 环境覆盖物品/NBT/标签筛选、不同物品的独立上限、重叠数量规则、模拟插入、堆叠限制、直接替换、
规则收紧后的物品保留与取出，以及固定大小的 NBT 存储。
弹药测试覆盖动态占槽、同弹种禁止溢出、耗尽释放、NBT 差异和重复存档的原子拒绝。
可选加载 Unidict 时，还检查其实际生成的弹药以及原生寻弹扣除接口。
玩家集成测试覆盖真实物品拾取、归属与延迟、第三把枪拒绝、换弹脚本、存档校验、死亡掉落和克隆。
单元与服务端测试数量见 [当前验证记录](inventory-accessories.md#验证)；真实客户端通过 `minecraft-mod-mcp` 验证铳械协议
2.0.1 环境下拾取 64 发备弹后，换弹扣除 30 发，剩余 34 发，枪内为弹匣 29 发加枪膛 1 发。

面板真实客户端验证（Smart Key Prompts 1.1.3-hotfix + OELib 0.2.4.1）：空槽数量上限、六个配件格及已安装瞄具、
悬停高亮与名称、左键备弹 64 → 63、右键清空、方向键关闭、空白关闭与绑定键开关均通过 MCP 检查。
Shift 批量数量由服务端 GameTest 覆盖，未通过 MCP 模拟原生 Shift + 鼠标组合键。

原版背包事件接管通过 MCP 验证：E 打开/关闭面板、旧独立 I 键不再响应、创造模式背包同样接管、
W 关闭、Smart Key Prompts 原生键帽分开显示 `[Shift][左键]`，关闭提示显示当前原版 E 绑定。
