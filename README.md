# BlockZone

![WIP](https://img.shields.io/badge/🚧_WIP-Under_Construction-yellow?style=for-the-badge&labelColor=black)

基于 Minecraft Forge 和 FPSMatch 的大逃杀玩法模组，玩法方向对齐《战地 6》的大逃杀模式。

## 功能

- 队伍加入、部署航线、跳伞、毒圈与胜负结算。
- 护甲板、倒地与队友救援。
- 多种战利品箱，以及箱子配置和毒圈的游戏内编辑器。
- 比赛结束后的场景与玩家状态恢复。

## 环境

- Java 17
- Minecraft 1.20.1 / Forge 47.4.10
- FPSMatch 1.3.0 Forge snapshot
- TaCZ、Kotlin for Forge
- 客户端需要 Modern UI 3.12.0.1 或更新版本（编辑器使用其控件与布局）

编辑器保留原有命令入口、世界操作和保存流程；参数集中在可滚动侧栏，保存与退出固定在底部，操作说明可在“操作帮助”中展开。地图场景不使用全屏背景或模糊。

开发依赖版本见 `gradle.properties`，Gradle 会解析开发运行所需依赖。

## 开发

```sh
./gradlew build
./gradlew runServer
./gradlew runClient
```

构建产物位于 `build/libs/`。客户端和服务端运行目录分别为 `run/client/` 与 `run/server/`，地图需要通过 FPSMatch 配置。

客户端测试流程见 [AGENTS.md](AGENTS.md)，提交规范见 [CONTRIBUTING.md](CONTRIBUTING.md)。