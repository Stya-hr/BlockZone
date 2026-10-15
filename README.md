# BlockZone

![WIP](https://img.shields.io/badge/🚧_WIP-Under_Construction-yellow?style=for-the-badge&labelColor=black)

基于 Minecraft Forge 和 FPSMatch 的大逃杀玩法模组，玩法方向对齐《战地 6》的大逃杀模式。

## 功能

- 队伍加入、部署航线、跳伞、毒圈与胜负结算。
- 护甲板、倒地与队友救援。
- [按组件拆分的小队状态 HUD](docs/squad-status-hud.md)，显示头像、成员编号、生命、护甲与倒地状态。
- 多种战利品箱，以及箱子配置和毒圈的游戏内编辑器。
- 比赛结束后的场景与玩家状态恢复。

## 环境

- Java 17
- Minecraft 1.20.1 / Forge 47.4.10
- FPSMatch 1.3.0 Forge snapshot
- TaCZ、Kotlin for Forge
- 客户端需要 Modern UI 3.12.0.1 或更新版本（编辑器使用其控件与布局）

编辑器保留原有命令入口、世界操作和保存流程；参数集中在可滚动侧栏，保存与退出固定在底部，操作说明可在“操作帮助”中展开。地图场景不使用全屏背景或模糊。

项目以[官方 Forge 1.20.1-47.4.10 MDK](https://maven.minecraftforge.net/net/minecraftforge/forge/1.20.1-47.4.10/forge-1.20.1-47.4.10-mdk.zip) 为底稿，保留其构建脚本结构、Maven 发布配置、资源替换与 JAR 元数据配置，使用 ForgeGradle 6 与 Gradle 8.8 Wrapper。现有 Java 代码、资源与测试迁入 MDK 的 `src/` 目录，并接入 FPSMatch 等依赖、Mixin 和独立资源包任务。开发依赖版本见 `gradle.properties`，Gradle 会下载开发运行所需依赖，无须复制本机缓存或手动放入依赖 JAR。

## 开发

```sh
git clone https://github.com/Stya-hr/BlockZone.git
cd BlockZone
./gradlew build
./gradlew genIntellijRuns
./gradlew runServer
./gradlew runClient
```

Windows 使用 `gradlew.bat` 替代 `./gradlew`。IDE 的项目 SDK 与 Gradle JVM 均选择 JDK 17，并以 Gradle 项目导入；IntelliJ IDEA 运行配置通过 `genIntellijRuns` 生成，Eclipse 使用 `genEclipseRuns`。

首次初始化需要联网下载 Gradle、Forge、Minecraft 和模组依赖。若初始化失败，请保留 `./gradlew build --stacktrace` 的输出，并检查 Java 版本及依赖下载是否成功。

构建产物位于 `build/libs/`。客户端和服务端运行目录分别为 `run/client/` 与 `run/server/`，地图需要通过 FPSMatch 配置。

客户端测试流程见 [AGENTS.md](AGENTS.md)，提交规范见 [CONTRIBUTING.md](CONTRIBUTING.md)。
