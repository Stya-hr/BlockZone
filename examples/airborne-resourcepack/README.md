# 空中载具与降落伞资源包

`./gradlew airborneResourcePack` 生成 `build/distributions/blockzone-airborne-resourcepack.zip`，包含默认模型、贴图和本说明。解压后编辑，再压缩或将文件夹放入客户端 `resourcepacks/` 启用；`pack.mcmeta` 须位于资源包根目录。

两套模型均使用 Minecraft 1.20.1 原版方块／物品 JSON 格式，可在 Blockbench 中选择 **Java Block/Item（Java 方块／物品）** 编辑并导出。无需额外动画模组或重新编译 Blockzone。

`blockbench/` 包含运输机、降落伞和螺旋桨的 `.bbmodel` 编辑工程，带部件分组和内嵌贴图。游戏实际读取下表中的 JSON 和 PNG；修改工程后需要导出并覆盖对应文件。使用较新版 Blockbench 时选择兼容 Minecraft 1.20.1 的 Java 模型导出，保留标准单轴旋转限制。

| 用途 | 资源包中的路径 |
| --- | --- |
| 运输机模型 | `assets/blockzone/models/airborne/transport_aircraft.json` |
| 旋转螺旋桨模型 | `assets/blockzone/models/airborne/propeller.json` |
| 降落伞模型 | `assets/blockzone/models/airborne/parachute.json` |
| 常亮航行灯与光晕 | `assets/blockzone/models/airborne/aircraft_lights.json` |
| 闪烁信标与光晕 | `assets/blockzone/models/airborne/aircraft_beacon.json` |
| 运输机贴图 | `assets/blockzone/textures/block/airborne/transport_aircraft.png` |
| 飞机三角裁切贴图 | `assets/blockzone/textures/block/airborne/aircraft_triangles.png` |
| 飞机灯光贴图 | `assets/blockzone/textures/block/airborne/aircraft_lights.png` |
| 降落伞贴图 | `assets/blockzone/textures/block/airborne/parachute.png` |

模型可以直接包含 `elements`，也可以用 `parent` 引用资源包中的其他模型。贴图通过 `textures` 和各面的 UV 指定，可以引用其他命名空间。纹理支持透明像素。原版旋转限制为单轴及 -45、-22.5、0、22.5、45 度；Blockbench 的 Java 格式会遵守这些限制。

飞机的机翼前缘、尾翼和机头采用透明裁切面与配套斜边封口，形成三角轮廓，仍为标准 Java JSON。修改 `aircraft_triangles.png` 时保留透明区域与 UV 对应关系；只涂满贴图会使三角面变回矩形。工程内已包含两张贴图。

## 坐标和显示变换

两套模型以 JSON 坐标 `(8, 8, 8)` 为原点，+Y 为上方，-Z 为前方。

- 运输机采用 8 倍基础缩放，JSON 中 2 个单位对应游戏中 1 格。原点位于航线位置上方 1.5 格，默认翼展 12 格。
- 降落伞采用 4 倍基础缩放，JSON 中 4 个单位对应游戏中 1 格。原点位于玩家脚部，默认伞翼在脚部上方约 4.75 格。

可通过标准 `display.fixed` 调整大小、旋转和挂载位置。例如：

```json
"display": {
  "fixed": {
    "rotation": [0, 0, 0],
    "translation": [0, 2, 0],
    "scale": [1.2, 1.2, 1.2]
  }
}
```

`translation` 仍使用原版 1/16 格单位，并受基础缩放影响；模型绕 `(8, 8, 8)` 缩放和旋转。模型的 `display.fixed` 仅调整显示，不影响航线和跳伞运动。

螺旋桨使用独立的标准 JSON，原点 `(8, 8, 8)` 为桨轴，沿 Z 轴旋转，采用 8 倍缩放。四个固定挂点相对运输机原点为 `(±4.25, 0.625, -2.375)` 和 `(±2.25, 0.625, -2.375)` 格；运输机模型不应再包含静态桨叶。螺旋桨默认共用运输机贴图，也可在资源包中指定独立贴图。修改机身的 `display.fixed` 不会移动这些挂点，可通过螺旋桨自身的 `display.fixed` 调整桨叶尺寸。

动画由模组驱动：螺旋桨持续旋转，展开的降落伞绕背带轻微摆动，各玩家的摆动相位不同。资源包只需替换这些标准 JSON 和贴图，无需额外动画模组。

常亮灯包含左红右绿航行灯、白色尾灯和机头下方的着陆灯；顶部红色信标每秒亮约 0.15 秒。两套灯光模型采用与机身相同的原点和 8 倍缩放，使用全亮、透明混合显示，位置和光晕都由 JSON 定义。灯光为视觉自发光，不改变世界方块亮度。替换载具时可一起替换灯光模型；将对应模型的 `elements` 设置为空数组即可关闭这一类灯光。

在游戏中用 **F3+T** 重载资源即可看到模型和贴图改动。
