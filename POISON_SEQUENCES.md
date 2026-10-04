# 毒圈序列配置

`battlezone.poison_sequences` 是完整路径数组。序列和圈的命令编号均从 1 开始，每局随机选一套。第一圈在部署开始时就存在，部署完成前不读秒；后续圈按顺序等待、移动和收缩。

```json
[
  {"circles": [
    {"x":100,"z":-1190,"radius":160,"wait_seconds":0,"shrink_seconds":0,"damage_multiplier":1},
    {"x":120,"z":-1180,"radius":75,"wait_seconds":60,"shrink_seconds":50,"damage_multiplier":2},
    {"x":130,"z":-1170,"radius":0,"wait_seconds":30,"shrink_seconds":40,"damage_multiplier":4}
  ]}
]
```

- `x/z`：绝对圈心坐标；球心 Y 为地图最低 Y。
- `radius`：绝对半径（方块），可缩小、保持或扩大。
- `wait_seconds/shrink_seconds`：从上一圈开始等待，再移动缩到当前圈的时间。第一圈的两个时间不参与计时，建议填 0。
- `damage_multiplier`：该圈的圈外伤害系数。实际每秒伤害为 `poison_damage_per_second × damage_multiplier`，2 点生命为一颗心。缺失系数兼容为 1，0 表示不伤害；拒绝负数和非有限数。

系数在抵达对应圈后生效，并保持到下一圈收缩完成。因此等待和收缩期间用出发圈的系数；最终圈使用最终系数。部署保护仍有效。每局选中的圈位置、时间、系数和基础伤害在启动时固定，修改设置不会改变已开始的比赛。

## 逐圈命令

下列命令都接在 `/fpsm map modify battlezone <地图名>` 后面：

```text
settings sequence list
settings sequence add
settings sequence 1 get
settings sequence 1 remove
settings sequence 1 circle add <x> <z> <radius> <wait_seconds> <shrink_seconds> <damage_multiplier>
settings sequence 1 circle 2 set <x> <z> <radius> <wait_seconds> <shrink_seconds> <damage_multiplier>
settings sequence 1 circle 2 remove
settings save
```

`sequence add` 添加一套默认完整路径。不能删除最后一套序列或一套路径的最后一圈；错误更新不会部分写入。超过地图边界的圈心会提示并在运行时平移，保存坐标和半径不变；无法容纳的半径会被拒绝。原始 JSON 仍可通过 `settings set poison_sequences <JSON数组>` 配置，非法路径在预览和启动时拒绝使用。

例：修改城市地图序列 1 的第 2 圈：

```text
/fpsm map modify battlezone city24 settings sequence 1 circle 2 set 100 -1190 120 60 60 2
/fpsm map modify battlezone city24 settings save
```

## FPSMatch 配置 UI

使用 FPSMatch 原生 `Setting`、Codec 和房间设置保存流程。数值参数直接在原配置 UI 编辑；`poison_sequences` 显示为可编辑、可还原默认值的 JSON 文本。当前 FPSMatch 版本不会按嵌套 Codec 自动生成列表/坐标表单，因此日常编辑推荐下方的世界内编辑器，逐圈命令和 JSON 粘贴保留作导入工具。保存权限和网络同步沿用 FPSMatch。

旧 `poison_center_x/z`、`poison_final_centers`、`poison_phases` 已从设置列表移除。加载旧文件时把每个终圈候选迁移成独立完整路径，保留半径比例、等待和收缩时间，系数为 1；中间随机圈心改为候选终圈心，运行时仍按边界平移。已有非空完整序列优先。原文件加载时不覆盖，执行 `settings save` 后只保存有效设置。

## 路线与预览

只配置 `deployment_speed` 和 `deployment_height`，线路按首圈的水平投影随机生成，切分面积差不超过 8%，无需经过圆心。速度为 0.1–100 方块/秒，高度为绝对 Y，仅要求有限数值，已移除地图顶部和 Y=2000 限制。高度自行避开建筑，圈外不能按 G 部署。

```text
/fpsm map modify battlezone city24 settings set deployment_height 2500
/fpsm map modify battlezone city24 debug zone show 1
/fpsm map modify battlezone city24 debug zone hide
/fpsm help
```

预览整套路径，不同圈使用不同颜色的球面经纬网格，零半径显示中心标记，仅执行命令的管理员可见；比赛中预览当前序列使用本局固定路径。

## 世界内编辑

管理员进入地图维度，运行 `/fpsm map modify battlezone <地图名> settings sequence edit`。
透明面板覆盖在游戏世界上，预览球面网格和地图边界；圈心高度沿用地图毒圈定义，不单独编辑 Y。

- 面板中左键拖动移动当前圈心；Shift + 左键拖动调整半径；滚轮以 1 格调整半径，Ctrl + 滚轮以 10 格调整；右键拖动观察视角。
- 数值输入即时更新预览。圈心 X/Z、半径、等待时间、过渡时间和伤害系数均可直接输入。
- 切换序列/圈，复制序列、新增（复制）圈、删除、调整圈的先后顺序。至少保留一套序列和一个圈。
- 点“观察世界”后正常移动观察，按 O 返回面板；观察中 [ / ] 切换圈，Shift + 左键将圈心放在准星选中的方块位置，Shift + 滚轮调整半径。
- 当前圈为白色；越界原始圈为橙色，自动平移后的实际圈为青色；无法容纳的圈为红色，保存前需要减小半径。保存仍保留原始圈心坐标。
- “保存配置”同时写回内存和 FPSMatch 设置文件；退出未保存草稿会提示。保存需要管理员权限且仍位于对应维度；其他人修改配置后会拒绝覆盖，需要重新打开编辑器。

编辑器不启动比赛、不改变游戏模式，不影响当前比赛已选定的路径。为控制预览和网络开销，一次支持最多 128 套序列，每套最多 128 个圈；JSON/命令导入不受编辑器数量限制。
