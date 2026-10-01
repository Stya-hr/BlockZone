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

1. 从仓库根目录启动 Forge 开发客户端：

   ```sh
   ./gradlew runClient
   ```

2. 保持客户端运行；通过游戏里的 MCP overlay 或暂停菜单入口启用/释放鼠标后，可以切换焦点到 Codex。游戏仍需真实显示环境来渲染，但无需让窗口保持前台。

3. 使用 `minecraft-mod-mcp` 工具确认连接并测试：先调用 `ping` 或 `get_minecraft_status`，再按需用 `screenshot`、`get_player_info`、`get_world_info`、`press_key`、`click`、`type_text` 等工具。需要控制模式时遵循模组工具提示，先在游戏 overlay/暂停界面启用；结束时用 `exit_control_mode`，完成后关闭客户端。

4. 如果工具显示没有检测到模组，确认 `runClient` 已完成启动、jar 放在 `run/client/mods/` 且版本匹配。可在终端运行 `npx -y minecraft-mod-mcp status` 查看连接状态。

禁止用桌面焦点/坐标自动化、`xdotool` 或 `Xvfb` 来驱动测试。MCP 是对实际客户端的控制通道；需要服务端逻辑时，另行启动本地 Forge 服务端并让开发客户端连接。

# 仓库协作习惯

- 每项开发工作从 `main` 创建独立的功能或修复分支，不直接在 `main` 上实现。
- 按功能范围提交，提交标题遵循 [CONTRIBUTING.md](CONTRIBUTING.md) 的 Conventional Commits 规范。
- 完成实现与必要验证后，将分支合并回 `main`；有远端协作流程时使用 Pull Request 和评审，没有远端时使用本地合并。
- 不把工作区中与当前任务无关的改动加入提交，也不覆盖他人的未提交工作。
