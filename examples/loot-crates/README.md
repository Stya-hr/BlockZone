# 战利品补给箱

`blockzone:loot_crate` 是一次性搜刮方块。右键打开后，服务端执行配置的 Loot Table，把完整物品堆叠作为 `blockzone:loot_drop` 实体向箱子前方弹出。不打开容器界面，也没有可供漏斗抽取的库存。

掉落物使用原版物品物理和碰撞拾取，弹出后一秒可以拾取；数量和物品 NBT 完整保留。专用渲染器取消旋转和上下浮动：普通贴图物品平放在地面，立体物品及 TaCZ 枪械沿用自身的 `GROUND` 模型。不会把平面图标自动转换成精细的三维模型；可以通过资源包或模组提供地面模型。

## 放置与配置

管理员（OP 2）可以从创造物品栏的功能方块分类取得箱子，或使用：

```mcfunction
/give @s blockzone:loot_crate
/setblock ~ ~ ~ blockzone:loot_crate[facing=north]
/fpsm map modify battlezone <map_name> settings loot edit
```

编辑器扫描地图内所有箱子（包括未加载区块），使用与 sequence 编辑器相同的独立相机在实际地图场景中显示箱子轮廓与标记，支持直接点击、拖动框选、Shift 追加、列表多选、搜索后全选，批量指定已加载的 Loot Table 和随机种子。WASD 移动相机，空格上升、Shift 下降，右键拖动转向，滚轮缩放，F6 聚焦已选箱子（未选时聚焦所有箱子）；相机操作不会移动玩家。应用按钮只修改草稿，保存后才写入世界并更新当前地图场景快照；默认同时关闭选中箱子。保存前会检查权限、对局状态和箱子是否被其他操作修改，发现冲突整批拒绝。编辑器支持最多 4096 区块、2048 个箱子；会话有效期 15 分钟。

种子设为 `0` 时，每次恢复后的首次开箱都会重新随机；固定的非零种子便于复现（物资结果仍可能受表中条件或全局战利品修改器影响）。玩家幸运值不参与抽取，战利品表可以通过 `THIS_ENTITY` 条件引用开箱玩家。

方块实体保存原版命名的 `LootTable`、`LootTableSeed`，以及 `Opened`。也支持地图制作时直接指定 NBT：

```mcfunction
/setblock <x> <y> <z> blockzone:loot_crate{LootTable:"yourpack:chests/weapons",LootTableSeed:0L,Opened:0b}
```

未知战利品表会提示错误并保留未打开状态。合法的空表会消耗箱子。两个玩家同时打开或重复右键，只会生成一次。

## 对局与地图恢复

地图范围内只有当前地图的存活参赛玩家可以在部署和战斗阶段打开箱子、拾取该地图的战利品。旁观者不能搜刮。对局中，以及场景保存或恢复期间，禁止通过玩家放置、破坏和批量编辑器修改箱子。箱子不能被活塞推动，具有防爆抗性。

地图外的箱子可独立使用，非旁观玩家能直接搜刮。批量编辑器保存时自动更新场景快照。通过 NBT 手动配置时，先确认箱子关闭，再保存场景快照：

```mcfunction
/fpsm map modify battlezone <map_name> snapshot save
```

等待快照保存结束后再开始比赛。对局重置会恢复箱子的表、种子和关闭状态，并清理该局战利品（包括落到地图范围外的物品；未加载区块中的物品会在重新加载时清理）。物品在对局中不会自然超时消失，也不会合并成旋转的原版掉落堆。

编辑器关闭箱子时，已经弹出的物品仍留在世界中。生存模式挖掉独立箱子时，掉落的箱子物品保留战利品表、种子和是否打开的状态，重新放置已打开箱子不能重复领取。

## 数据包

内置示例 `blockzone:chests/common` 位于 `data/blockzone/loot_tables/chests/common.json`，使用原版剑、铁锭和食物。正式枪械池应在数据包中提供，并为枪械、弹药配置正确的物品 ID 和 NBT。可以引用其他 Loot Table，使用权重、条件、数量函数和 `set_nbt`；修改后执行 `/reload` 即可用于下次未打开箱子的抽取。

箱子外观由 `assets/blockzone/models/block/loot_crate.json` 和 `loot_crate_open.json` 提供，可用资源包替换。

`tacz-demo/` 是可直接复制到世界 `datapacks/` 的示例数据包（需要 TaCZ 默认枪包）。执行 `/reload`，将箱子设置为 `blockzone_demo:chests/weapons`，会弹出一把装有 30 发子弹的 AK-47 和 32 发配套弹药。
