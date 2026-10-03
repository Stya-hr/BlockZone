# 毒圈序列配置

Battlezone 地图的 `battlezone.poison_sequences` 设置保存完整路径数组。数组位置即序列编号（从 1 开始），每局等概率随机选一套。此设置非空时取代旧的 `poison_phases`、`poison_final_centers`、`poison_center_x/z`；旧设置保留，数组为空时继续使用旧逻辑。

以下是 `poison_sequences` 的值示例，坐标和半径单位均为方块：

```json
[
  {
    "circles": [
      {"x": 0, "z": 0, "radius": 100, "wait_seconds": 0, "shrink_seconds": 0},
      {"x": 20, "z": 10, "radius": 60, "wait_seconds": 30, "shrink_seconds": 60},
      {"x": 35, "z": 15, "radius": 0, "wait_seconds": 15, "shrink_seconds": 30}
    ]
  },
  {
    "circles": [
      {"x": 0, "z": 0, "radius": 100, "wait_seconds": 0, "shrink_seconds": 0},
      {"x": -20, "z": -10, "radius": 50, "wait_seconds": 20, "shrink_seconds": 45},
      {"x": -30, "z": -15, "radius": 0, "wait_seconds": 10, "shrink_seconds": 20}
    ]
  }
]
```

每套路径至少有一圈。第一圈用于部署，其等待和缩圈时间不参与计时，建议均填 0；后续每圈的时间表示从上一圈开始等待，再移动并缩到此圈的时间。部署阶段结束后才启动计时。第一圈决定随机航线和按 G 部署的水平范围。各圈半径只能保持或递减。

圈的水平投影必须完全处于地图 X/Z 边界内。超出边界时向在线管理员和日志提示，运行时平移圈心，不更改配置或半径。如果半径大到无法放进地图，或序列为空、半径递增，则保留配置并拒绝启动，需修正后再开始。

```text
/fpsm map modify battlezone <地图名> debug zone show <序列编号>
/fpsm map modify battlezone <地图名> debug zone hide
```

预览整套路径，不同阶段用不同颜色的球面经纬网格显示；只对执行命令的管理员可见，聊天栏按顺序列出各圈坐标和半径，半径为 0 时显示中心标记。比赛中预览选中的序列使用本局实际位置；预览其他序列不会改变当前比赛。旧配置以序列 1 预览。

此变更更新网络协议，客户端与服务端需要同时更新模组。
