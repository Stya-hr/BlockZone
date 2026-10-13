# 饰品、玩家装备与完整初始背包

第一行从左到右为 `accessory`、`accessory`、`equipment.helmet`、`equipment.vest`、
`equipment.legs`、`equipment.feet`。四个装备格直接读写玩家原版 HEAD、CHEST、LEGS、FEET，
没有额外装备副本。两个饰品槽各容纳一个饰品。

## 初始装备 Capability 负责整个背包

继续使用 `BattlezoneLoadoutCapability`，不增加第二套初始物品配置。
它在开局前验证并按局固定完整初始配置，负责清空原版物品栏、装备栏、鼠标携带物和制作格，
再向指定目的地发放物品。比赛结束仍通过现有玩家快照恢复原版物品。

```text
/fpsm map modify battlezone <地图名> capability loadout get
/fpsm map modify battlezone <地图名> capability loadout set <JSON数组>
/fpsm map modify battlezone <地图名> capability loadout clear
/fpsm map modify battlezone <地图名> capability loadout reset
```

以下是包含原版四部位装备、武器、道具和备弹的示例，未写出的槽为空：

```json
[
  {"slot":"equipment.helmet","item":"blockzone:tactical_helmet"},
  {"slot":"equipment.vest","item":"blockzone:plate_carrier"},
  {"slot":"equipment.legs","item":"blockzone:tactical_leggings"},
  {"slot":"equipment.feet","item":"blockzone:tactical_boots"},
  {"slot":"weapon","item":"tacz:modern_kinetic_gun","nbt":"{GunId:\"tacz:ak47\",GunCurrentAmmoCount:0,GunFireMode:\"AUTO\"}"},
  {"slot":"item.armor_plates","item":"blockzone:armor_plate","count":2},
  {"slot":"ammo","item":"tacz:ammo","count":64,"nbt":"{AmmoId:\"tacz:762x39\"}"}
]
```

添加饰品时，使用实际已注册的饰品物品，例如
`{"slot":"accessory","item":"yourmod:charm","nbt":"{Quality:2}"}`。
仅有基类不会自动注册或生成一个有战斗效果的饰品。

初始装备仅接受当前槽位类型。旧别名与已移除槽位直接报错，不自动迁移。

## 类型与数量校验

槽位规则新增可选 `type` 字段。未填写时按配置键对应的槽位类型推导；显式填写错误类型会拒绝整次更新。

```json
{"accessory":{"type":"accessory","allowed":["#curios:ring"],"limit":1}}
```

| 类型 | 允许对象 | 数量 |
| --- | --- | --- |
| `accessory` | `AccessoryItem` 或 Curios API 饰品／Curios 标签物品 | 1 |
| `equipment` | 与原版部位匹配的 ArmorItem | 1 |
| `weapon` | FPSMatch GunCompatManager 识别的枪械 | 1 |
| `item` | 规则允许的普通道具，排除护甲、枪械与饰品 | 规则上限和原生堆叠上限 |
| `ammo` | 规则允许的 TaCZ／铳械协议弹药或普通物品弹药，排除护甲、枪械与饰品 | 规则上限和原生堆叠上限 |

`allowed` 与固定类型取交集，`*` 也不能绕过类型。物品 ID 或已加载标签含错误类型时拒绝配置；
可选模组尚未安装的物品 ID 保留为不匹配，实际插入始终再次检查类型。
动态标签重载不会绕过运行时检查。武器、饰品、四部位装备的 `limit` 与所有 `item_limits` 必须为 1。

初始配置还检查真实物品注册、NBT、原生堆叠数、目的地重复、槽位规则和同弹种跨槽重复。
更新背包规则时，也会核对当前初始配置；更新初始配置时，核对地图配置的下一局规则。
如需同时大幅修改两者，可先 `loadout clear`，设置规则，再写完整初始配置。
存档恢复先校验各自结构与类型，避免两项 Capability 的读取顺序误拒绝合法配置；
开局再对完整恢复后的两项配置交叉校验。运行中配置修改不改变本局已经固定的规则和物品。

## 饰品基类

继承 `dev.stya.blockzone.equipment.AccessoryItem`，然后通过自己的 DeferredRegister 注册物品：

```java
public final class CharmItem extends AccessoryItem {
    public CharmItem() { super(new Item.Properties()); }

    @Override
    public void accessoryTick(Player player, int slot, ItemStack stack) {
        if (player.level().isClientSide) return;
        // 饰品效果；slot 为 0 或 1。
    }
}
```

基类强制最大堆叠数为 1，提供 `canEquip`、`canUnequip`、`onEquip`、`onUnequip`、
`accessoryTick`。服务端管理实际装备；tick 在客户端和服务端均可能调用，效果应自行区分侧。
Curios 的装备／卸下回调沿用其原生语义，也可能因装备 NBT 更新触发。
比赛重置和结束属于强制清理；玩家主动丢弃则尊重 `canUnequip`，不会绕过锁定饰品。

未安装 Curios 时，两个饰品存于玩家 `PlayerPanelInventory`，由本模组调用基类生命周期。
安装 Curios 后，原生饰品注册为其 `ICurioItem` 适配器，效果由 Curios 生命周期驱动，
本模组不会重复执行同一个饰品的 tick。

## 可选 Curios API 兼容

编译使用 Curios Forge 1.20.1 **5.14.1**，运行依赖为可选；不安装时可以独立运行。
安装时客户端与服务端均需有匹配版本。第三方 API 引用隔离在可选桥接类中。

资源定义了玩家专用 `blockzone_accessory` 槽位组，大小为 2，使用 `blockzone:accessory` 验证器。
面板直接操作该组的真实 Curios handler，不把同一件饰品再次复制到备弹存储。
`AccessoryView` 只用于所属客户端显示；服务端的 Curios handler 是唯一物品来源。
识别第三方 `ICurio` 能力及 `curios:*` 标签饰品，并保留其原生 tick、属性、装备与卸下限制。

进入比赛前通过 Curios `saveInventory(true)` 暂存并清空比赛外整套 Curios 装备，
结束通过 `loadInventory` 恢复。比赛期间其他 Curios 槽位不能再次装备物品来绕过两个饰品的限制。
原始快照随玩家 Capability 持久化；死亡、克隆和结束流程保持比赛物品与比赛外物品分离。
实际死亡的比赛饰品由本模组掉落一次，Curios 的掉落覆盖阻止再次生成同一件物品。

需要 API 模组本身认可该物品能装备到 `blockzone_accessory` 上下文；不会伪造另一种槽位名称，
也不会绕过第三方物品自己的 `canEquip/canUnequip`。

## 存档与同步

存储统一为当前面板顺序的 17 格，NBT 必须包含 `Schema: 2` 与 `Size: 17`。
旧格式存档直接拒绝，且不会修改当前物品。

网络协议为 **23**，服务端和客户端需一起更新。丢弃请求携带槽位物品版本，
同一物品的动态 NBT、Curios tick 或数量变化不影响丢弃；被新物品替换后，旧点击会被拒绝。

## 验证

单元测试覆盖固定类型、数量约束、默认类型推导、旧槽位拒绝和第一行布局。
GameTest 覆盖整套初始背包、原版四部位装备、错误类型／数量、重复目的地／弹种、饰品拾取与锁定、
Curios 唯一来源、原生验证器、禁止额外槽位、死亡克隆、比赛外恢复、动态 NBT 点击、旧格式拒绝和当前格式往返。
Curios 安装与未安装使用两个独立运行环境；测试辅助物品只在 Forge GameTest 开发模式注册。

验证结果：122 项单元测试；Curios 5.14.1 + 铳械协议环境与未安装 Curios 环境各 34 项 GameTest。
真实客户端通过 minecraft-mod-mcp 检查 E 键、第一行 2+4 布局、原生及第三方饰品拾取、
Curios 原生 tick，以及动态 NBT 饰品的左键丢弃和第三方饰品右键清空。测试服已保存退出。

初始装备条目的 `slot` 填槽位类型（如 `accessory`、`weapon`、`item.tactical`、`ammo`），同类条目按列表顺序分配到该类型的空格。条目数超过该类型格子数时拒绝配置；同种弹药仍然只能占一个格子。
