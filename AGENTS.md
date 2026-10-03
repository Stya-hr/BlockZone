# 开发文档
- 理解[FPSMatch开发者文档](https://fpsmatch.ptcrys.net/master/docs/developer/)
- 学习[依赖FPSMatch的开发示例模组](https://github.com/PhasetransCrystal/BlockOffensive)

# 客户端测试

- 真实客户端测试使用 `minecraft-mod-mcp`，可保持游戏未聚焦；禁止用桌面自动化、`xdotool` 或 `Xvfb` 驱动测试。
- MCP 在用户级配置注册，测试模组放入 `run/client/mods/`；本机配置和测试文件不提交到仓库。
- 使用独立测试服目录 `run/server-visual-test`，复用已有测试世界；不要覆盖它，也不要让两个服务端同时打开同一世界。
- 测试服仅监听 `127.0.0.1`，离线测试使用 `online-mode=false`；客户端连接端口与服务端配置一致。

```sh
./gradlew runServer -PblockzoneServerRunDir=run/server-visual-test
./gradlew runClient -PblockzoneQuickPlayMultiplayer=127.0.0.1:25566
```

等待服务端 `Done` 和客户端加载完成，通过 MCP `ping` 确认连接后操作。结束时退出控制模式，输入 `stop` 等待服务端保存，再关闭客户端。需要第二名玩家时使用 `runClient2`，目录为 `run/client2`。

# 仓库协作习惯

- 每个功能的开发工作从 `main` 创建功能或修复分支，不直接在 `main` 上实现。
- 按功能范围提交，提交标题遵循 [CONTRIBUTING.md](CONTRIBUTING.md) 的 Conventional Commits 规范。
- 完成实现与必要验证后，将分支合并回 `main`；有远端协作流程时使用 Pull Request 和评审，没有远端时使用本地合并。
- 不把工作区中与当前任务无关的改动加入提交，也不覆盖他人的未提交工作。
