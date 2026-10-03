# 边界警戒线资源包

默认红色双线和循环 CAUTION 字样全部来自 PNG 材质，可通过资源包替换：

`assets/blockzone/textures/effect/battlezone_warning_fence.png`

资源包根目录的 `pack.mcmeta`（Minecraft 1.20.1）：

```json
{"pack":{"pack_format":15,"description":"自定义边界警戒线"}}
```

PNG 支持透明和半透明，尺寸和长宽比不限。整张图片表示一个重复单元；渲染高度为 0.6 格，重复宽度为 `0.6 × 图片宽度 / 图片高度`。默认图片为 320 × 60，重复宽度为 3.2 格。顶部、底部线条应延伸到图片左右边缘，以便连续拼接。

玩家碰撞箱距边界 2 格以内逐渐显示，1.25 格以内完全显示；每侧最多显示 24 格，两端各 2 格渐隐。文字的水平位置固定在世界坐标上，四侧从地图内部看均为正向。

已有地图的 `battlezone.boundary_texture` 设置仍支持指定其他资源位置。资源重载会重新读取图片比例；加载失败时回退到默认材质。

默认素材可用 `python3 tools/generate_caution_texture.py` 重新生成。
