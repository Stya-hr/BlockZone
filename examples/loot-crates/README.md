# 场景箱与地图战利品控制

四款场景箱默认都是普通的 27 格容器，右键打开库存，支持存取物品。地图管理员可以通过编辑器为指定容器启用战利品特性：右键执行 Loot Table，把完整物品堆叠作为 `blockzone:loot_drop` 实体弹出，不再打开库存界面；每轮只能搜刮一次。槽位只用于普通储物，战利品抽取由 Loot Table 决定。

原库存不会因启用或停用而被清空，也不参与 Loot Table 抽取。停用特性后恢复普通交互。破坏普通场景箱会正常掉落库存物品和箱子。

## 地图中启用或停用

创造物品栏的功能方块分类提供四款箱子，也可放置：

```mcfunction
/give @s blockzone:loot_crate
/setblock ~ ~ ~ blockzone:tactical_loot_crate[facing=north]
/fpsm map modify battlezone <map_name> settings loot edit
```

编辑器扫描地图内支持的容器（包括未加载区块），支持场景点击、框选、列表多选、坐标/方块 ID/Loot Table 搜索，以及批量配置表和随机种子。勾选“启用战利品箱特性”，应用到草稿后保存，即把选中的普通容器交给地图战利品控制；取消勾选并保存可恢复普通容器。列表中的 normal/loot/open 分别表示普通、待搜刮、已搜刮。

应用只修改草稿，保存后才写入世界并更新地图场景快照。保存会验证管理员权限、对局状态、箱子类型和原配置是否改变；冲突时拒绝整批修改，快照保存失败会回滚配置。编辑器最多支持 4096 区块、2048 个容器，会话有效期 15 分钟。

WASD 移动相机、空格上升、Shift 下降，右键拖动转向，滚轮缩放，F6 聚焦选中容器；相机操作不会移动玩家。

种子 0 每次恢复后的首次搜刮重新随机，非零种子用于复现。抽取支持枪械和弹药的完整 NBT；玩家幸运值不影响战利品权重，表条件可以引用 THIS_ENTITY 玩家。未知或错误的表不会消耗容器，合法空表会消耗。重复右键不会重复生成。

## 限制范围和地图恢复

只有地图区域内、明确启用特性的容器受战利品规则控制。部署和战斗阶段只有该地图的存活参赛玩家可以搜刮；对局中及快照操作期间禁止破坏和修改已控制的容器。普通容器、地图外容器保留原有交互。即使配置元数据仍留在容器上，离开所属地图区域后也作为普通容器使用。重叠地图区域不允许启用。

掉落战利品带白色轮廓高光，走过不会自动拾取。准星对准后使用原版使用键（默认右键）拾取；持 TaCZ 枪械时使用其配置的互动键（默认 O），右键保留瞄准。场景箱和战利品均加入 TaCZ 互动白名单，尊重 TaCZ 的黑名单配置。拾取距离最多 3 格且不能隔墙，弹出后一秒可拾取；库存插入沿用原版规则：生存模式库存满时物品保留，容量不足时只拾取能放下的部分。保留原版物品所有者、数量、NBT 和拾取统计，不额外检查参赛身份或阶段。

专用渲染器取消旋转与上下浮动：普通平面物品贴地显示，立体物品和 TaCZ 枪械使用自身 GROUND 模型。

地图快照保存完整容器库存和配置。对局重置恢复启用状态、Loot Table、种子与关闭状态，并清理该局掉落物，包括落到地图外的物品。启用配置保存在方块实体的 ForgeData.BlockzoneLoot 中，包含 Map、Enabled、LootTable、LootTableSeed、Opened；它不是容器自己的普通 LootTable 标签。

## 适配其他模组的普通容器

使用 Container 或 Forge ITEM_HANDLER 接口的容器可通过数据包标签接入。在数据包的 `data/blockzone/tags/blocks/loot_containers.json` 中加入方块 ID：

```json
{
  "replace": false,
  "values": [{"id": "othermod:storage_box", "required": false}]
}
```

执行 /reload 后，这种容器会出现在地图编辑器中，但不会自动转换。需要管理员选中并明确启用，才接管右键和搜刮状态。模组原有库存、外观和其他系统保持原实现；通用层不修改模组的自动化库存接口。

默认标签包括本模组四款箱子、原版单箱、陷阱单箱和木桶。双箱及多方块容器暂不走通用适配。普通单箱使用原生箱盖事件，含 OPEN 属性的容器切换该状态；无法通用触发的特殊动画需独立适配。扩展模组可通过 `LootContainerAdapters.register(Adapter)` 提供 supports、facing 和 setOpened 钩子。

## 数据包战利品表

内置示例 `blockzone:chests/common` 使用原版剑、铁锭和食物。正式枪械池应提供对应物品 ID 和 NBT。可引用其他 Loot Table，使用权重、条件、数量函数和 set_nbt；修改后执行 /reload。

`tacz-demo/` 可复制到世界 datapacks/（需要 TaCZ 默认枪包）。编辑器选择 `blockzone_demo:chests/weapons` 并启用特性，开箱会弹出装有 30 发弹药的 AK-47 和 32 发配套弹药。

## 场景模型与贴花

| 外观 | 方块 ID | 箱体尺寸（模型单位，不含护角与把手） |
|---|---|---|
| 军用 | `blockzone:loot_crate` | 24 × 10.5 × 16 |
| 战术 | `blockzone:tactical_loot_crate` | 32 × 8 × 14 |
| 医疗 | `blockzone:medical_loot_crate` | 18 × 12 × 14 |
| 旧野战 | `blockzone:weathered_loot_crate` | 22 × 9.5 × 16 |

四款箱子均支持现有 Loot Table 批量编辑器。医疗箱取代工业箱。凹槽、压筋、护角、木板缝使用实际几何；编号、文字和医疗十字使用零厚度、仅单面渲染的贴图面，与承载面留出间距。贴片随箱盖一起变换到打开状态。

`blockbench/` 提供每款关闭和打开状态的 `.bbmodel`，包含嵌入纹理和 body/lid 分组。模型适配 Minecraft 1.20.1 Java 格式；打开状态的旋转已经烘焙到坐标。游戏资源位于 `assets/blockzone/models/block/loot_crates/`，纹理位于 `assets/blockzone/textures/block/loot_crates/`。

重新生成源项目与基础游戏 JSON：

```sh
python3 examples/loot-crates/tools/generate_models.py
python3 examples/loot-crates/tools/check_models.py
```

生成后可在 Blockbench 中编辑并导出 Java Block/Item JSON。检查器验证源项目与导出坐标、UV、模型边界以及打开/关闭状态的外露共面重叠；实际游戏远距离深度精度仍需客户端观察。
