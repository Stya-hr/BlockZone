# 开发文档
- 理解[FPSMatch开发者文档](https://fpsmatch.ptcrys.net/master/docs/developer/)
- 学习[依赖FPSMatch的开发示例模组](https://github.com/PhasetransCrystal/BlockOffensive)

# 非前台客户端测试（minecraft-mod-mcp）

本项目是 Minecraft 1.20.1 Forge，Gradle 开发客户端的工作目录为 `run/client`。需要通过真实 Forge 客户端测试时，优先使用 `minecraft-mod-mcp` 的本机 MCP stdio 桥接；游戏可以保持未聚焦并通过 MCP 操作。MCP 配置和测试模组均为本机配置，不提交到仓库。

## 一次性配置

1. 在 Codex 用户级配置注册 MCP（配置写在仓库外）：

   ```sh
   codex mcp add minecraft-mod-mcp -- npx -y minecraft-mod-mcp
   ```

   注册后重新启动 Codex 会话。不要在仓库创建 `.mcp.json` 或其它 MCP 配置文件。

2. 从 [`minecraft-mod-mcp` 发布页](https://github.com/langyo/minecraft-mod-mcp/releases)选择与 Minecraft 1.20.1、Forge 匹配的模组 jar，放到客户端的忽略目录。当前已验证的 v0.4.1 可以这样安装并核对 SHA-256：

   ```sh
   mkdir -p run/client/mods
   curl -fL https://github.com/langyo/minecraft-mod-mcp/releases/download/v0.4.1/minecraft-mcp-1.20.1-forge-v0.4.1.jar \
     -o run/client/mods/minecraft-mcp-1.20.1-forge-v0.4.1.jar
   sha256sum run/client/mods/minecraft-mcp-1.20.1-forge-v0.4.1.jar
   ```

   v0.4.1 的 SHA-256 应为 `aa0dbcbd4aa4e7a89b3f6215800fbd18cc75ef505c86e5a72e774c8080f17409`。仓库 `.gitignore` 忽略整个 `/run/`，因此本地模组和客户端配置不会被 Git 跟踪。升级模组时先核对发布页中的 Minecraft/Forge 版本和资产校验和。

## 每次测试

### 启动专用测试服和客户端

需要服务端逻辑或真实多人画面时，在两个终端分别启动服务端和客户端。专用目录通过 `blockzoneServerRunDir` 设置，避免开发测试读写默认 `run/server`：

```sh
# 终端 1：专用 Forge 测试服
./gradlew runServer -PblockzoneServerRunDir=run/server-visual-test

# 终端 2：开发客户端启动后直接连接测试服
./gradlew runClient -PblockzoneQuickPlayMultiplayer=127.0.0.1:25565
```

如果端口 `25565` 已占用，在 `run/server-visual-test/server.properties` 将 `server-port` 改为可用端口（例如 `25566`），客户端参数也使用相同端口。测试离线开发账号时，仅让测试服监听本机地址（`server-ip=127.0.0.1`），并在该测试服的 `server.properties` 设置 `online-mode=false`。首次启动按服务端提示接受 `eula.txt` 后再启动。

需要沿用已有地图和 FPSMatch/Battlezone 设置时，先停止源服务端，再将 `run/server` 复制到独立测试目录；不要让两个服务端同时打开同一世界：

```sh
mkdir -p run/server-visual-test
cp -a run/server/. run/server-visual-test/
```

检查复制目录里的 `server.properties`，确认端口和 `server-ip` 不会与其他本机服务冲突。已有 `run/server-visual-test` 时直接复用，不要反复覆盖其中的测试世界。

若要测其他玩家、队伍或实体交互，可再开一个终端启动第二个独立客户端：

```sh
./gradlew runClient2 -PblockzoneQuickPlayMultiplayer=127.0.0.1:25565
```

`runClient2` 使用独立目录 `run/client2` 和测试用户名 `BlockzoneTester2`。需要通过 MCP 控制这个客户端时，也把 `minecraft-mod-mcp` jar 放进 `run/client2/mods/`。

```sh
mkdir -p run/client2/mods
cp run/client/mods/minecraft-mcp-1.20.1-forge-v0.4.1.jar run/client2/mods/
```

### 操作与结束

等待服务端日志出现 `Done`，并等待客户端完成加载。通过 `minecraft-mod-mcp` 先调用 `ping` 或 `get_minecraft_status` 确认连接，再按需使用 `screenshot`、`get_player_info`、`get_world_info`、`press_key`、`click`、`type_text` 等工具。启用控制模式时遵循模组提示，通过游戏里的 MCP overlay 或暂停菜单启用；结束控制时调用 `exit_control_mode`。

结束测试时先在服务端控制台输入 `stop` 等待保存完成，再关闭客户端。若 MCP 没有检测到模组，确认相应客户端已启动完成，jar 位于该客户端目录的 `mods/` 下且版本匹配；可运行 `npx -y minecraft-mod-mcp status` 查看桥接状态。

禁止用桌面焦点/坐标自动化、`xdotool` 或 `Xvfb` 驱动测试。真实客户端需要显示环境渲染，但可以保持未聚焦并通过 MCP 操作。

# 仓库协作习惯

- 每个功能的开发工作从 `main` 创建功能或修复分支，不直接在 `main` 上实现。
- 按功能范围提交，提交标题遵循 [CONTRIBUTING.md](CONTRIBUTING.md) 的 Conventional Commits 规范。
- 在收到验收完成许可后才能合并到 `main`。
- 不把工作区中与当前任务无关的改动加入提交，也不覆盖他人的未提交工作。
