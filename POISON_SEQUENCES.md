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

使用 FPSMatch 原生 `Setting`、Codec 和房间设置保存流程。数值参数直接在原配置 UI 编辑；`poison_sequences` 显示为可编辑、可还原默认值的 JSON 文本。当前 FPSMatch 版本不会按嵌套 Codec 自动生成列表/坐标表单，因此通过逐圈命令快速修改，或在 UI 粘贴完整 JSON。保存权限和网络同步沿用 FPSMatch。

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
