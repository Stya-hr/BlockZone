# 背包静态面板

进入世界后按 **I** 打开，按 **I / Tab / Esc** 关闭；可以在控制设置中修改打开键。
此阶段只展示设计中的固定内容，丢弃、请求、装备和数量均未接入游戏逻辑。

布局以导出的 2560 × 1440 画框为基准，保持纵横比并锚定底部中央。
打开时隐藏原版 HUD，底板保持透明，游戏不会暂停。

## 素材

- `src/main/resources/assets/blockzone/gui/inventory/`：独立 SVG 源文件。
- `src/main/resources/assets/blockzone/textures/gui/inventory/`：游戏使用的 2× PNG。
- 12 个图标：背包、护甲板、头盔、护甲、盾牌、加号、步枪、手雷、电池、医疗包、物品护甲板、弹药。
- `labels` 保留设计导出的描边文字和按键提示，供静态预览使用；接入逻辑时应替换为可本地化的动态文字。
- 步枪原图使用 lighten 混合，导出脚本将其黑底转换为透明度以适配游戏的 alpha 混合。
- 不包含设计中的世界截图和小地图。

重新生成素材（需要 `rsvg-convert`）：

```sh
python tools/export_inventory_assets.py /path/to/BlockZone.zip
```

## 验证

已通过 Gradle 编译与资源处理，并使用 `minecraft-mod-mcp` 连接现有测试服检查：
面板打开、Tab / Esc 关闭、透明图标、灰显状态及原版快捷栏隐藏。
尚未接入任何物品操作；请验收外观后再合并功能分支。
