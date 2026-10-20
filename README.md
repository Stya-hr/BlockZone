# BlockZone

基于 Minecraft Forge 与 FPSMatch 的大逃杀模组，开发中。支持小队、空中部署、跳伞、毒圈、护甲插板、倒地救援、战利品及游戏内配置编辑器。

## 环境

Java 17、Minecraft 1.20.1、Forge 47.4.10；依赖 FPSMatch、TaCZ、Kotlin for Forge，客户端需要 Modern UI 3.12.0.1 或更新版本。具体依赖版本见 `gradle.properties`。

## 构建与运行

```sh
./gradlew build
./gradlew runServer
./gradlew runClient
```

Windows 使用 `gradlew.bat`。首次运行需联网下载依赖。产物位于 `build/libs/`，运行目录为 `run/server/` 和 `run/client/`，地图通过 FPSMatch 配置。

开发与客户端测试约定见 [AGENTS.md](AGENTS.md)。
