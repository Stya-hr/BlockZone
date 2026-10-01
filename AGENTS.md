# 开发文档
- 理解[FPSMatch开发者文档](https://fpsmatch.ptcrys.net/master/docs/developer/)
- 学习[依赖FPSMatch的开发示例模组](https://github.com/PhasetransCrystal/BlockOffensive)

# 非前台客户端测试
- 本项目为 Minecraft 1.20.1 Forge。按需运行 `./gradlew -PblockzoneDebugBridge=true runClient`，Gradle 会只给客户端开发运行加载 DebugBridge（版本 2.0.0），默认构建和服务端运行不增加此调试依赖。客户端维持在后台时，通过 MCP API 驱动，不要用桌面焦点、坐标键鼠自动化或虚拟显示器。
- `run/client/config/debugbridge.json` 已预置开发测试配置：`developer_mode_accepted`、`run_command_enabled` 和 `session_control_enabled` 为 `true`。这是本地忽略文件，不提交到 Git；新检出仓库时可按需创建该 JSON。DebugBridge 默认仅绑定本机 `127.0.0.1:9876`；两个功能开关开启后，MCP 可以发游戏命令、加入/离开服务器和退出客户端。
- 安装本机 MCP Server：`codex mcp add mcdev-mcp -- npx -y mcdev-mcp serve`，然后重新启动 Codex 会话。这里仅使用其运行时工具，不需要 `mcdev-mcp init` 的 Minecraft 源码索引。启动客户端后先用 `mc_wait_for_bridge` 确认连接，再用 `mc_snapshot`、`mc_screen_inspect`、`mc_screenshot`、`mc_execute`、`mc_join_server` 等工具观察和操作；结束时用 `mc_quit_client`。
- DebugBridge/MCP 控制的是实际客户端，因此仍会渲染游戏画面，但操作不要求将游戏切到前台。需要验证像素渲染/着色器时读取 `mc_screenshot`；验证比赛与服务端逻辑时运行本地 Forge 服务端并让该客户端连接。
- 若只需要脚本驱动真实客户端而不需要 MCP，可使用 MC Pilot（`@kzheart_/mc-pilot`），它注明支持 Forge 1.20.1，并提供聊天、移动、背包、界面和截图 CLI。纯 Mineflayer Minecraft MCP 是协议机器人，不加载本项目客户端模组，不能代替 Forge 客户端渲染或框架自定义网络协议测试。

# 仓库协作习惯

- 每项开发工作从 `main` 创建独立的功能或修复分支，不直接在 `main` 上实现。
- 按功能范围提交，提交标题遵循 [CONTRIBUTING.md](CONTRIBUTING.md) 的 Conventional Commits 规范。
- 完成实现与必要验证后，将分支合并回 `main`；有远端协作流程时使用 Pull Request 和评审，没有远端时使用本地合并。
- 不把工作区中与当前任务无关的改动加入提交，也不覆盖他人的未提交工作。
