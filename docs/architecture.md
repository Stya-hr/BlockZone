# 职责与状态所有权

保持单个 Gradle 项目，以 Java 包划分职责。新逻辑优先进入所属功能包，避免让地图入口和网络通道承载具体实现。

| 部分 | 职责与拥有的状态 |
| --- | --- |
| `map.battlezone.BattlezoneMap` | FPSMatch 生命周期入口、比赛阶段、加入与退出协调、玩家加入前状态及本局战利品标识 |
| `BattlezoneTeamRoster` | 根据人数配置协调队伍数量与容量；实际队伍状态由 FPSMatch `MapTeams` 管理 |
| `BattlezoneSceneLifecycle` | 场景快照有效性、区域变更后的延迟保存与恢复操作；存储实现位于 `data.snapshot` |
| `BattlezoneEliminationContext` / `BattlezoneEliminationRule` | 不可变淘汰规则输入与胜负计算 |
| `BattlezonePresentationSync` | 区域与运输载具的展示数据构造、接收者选择和同步频率 |
| `map.battlezone.capability` | 持久化能力配置及每局配置冻结，分别委托战斗、毒圈和装备实现 |
| `combat` / `deployment` / `zone` | 战斗恢复与倒地救援、部署与降落、毒圈推进等服务端玩法 |
| `CombatStateSync` / `FlightStateSync` | 各自功能的服务端展示缓存及同步；缓存不是权威玩法状态 |
| `net.BlockzoneNetwork` | 共享通道、数据包注册与无状态传输，不拥有玩家或比赛状态 |
| `net.battlezone` / `net.editor` | 数据包编码、解码及线程切换；处理后委托功能服务 |
| `editor.loot` / `editor.zone` | 编辑草稿、校验与服务端编辑会话；会话负责权限、冲突检查及保存 |
| `client` | 客户端展示状态、输入、界面及渲染；不作为服务端玩法的状态来源 |
| `data.persistence` / `data.snapshot` | 配置持久化适配与世界、玩家快照存储 |

依赖方向：数据包处理器 → 功能服务 → 地图或能力配置；功能同步器 → 网络传输。客户端保存编辑内容时只构造数据包，不调用服务端会话服务。

## 本次名称调整

- `BattlezoneNetwork` → `BlockzoneNetwork`：通道服务于整个模组，包括编辑器。
- `BattlezoneVisualSync` → `BattlezonePresentationSync`：明确负责客户端展示同步。
- `BattlezoneMatchContext` → `BattlezoneEliminationContext`：明确该上下文只用于淘汰判定。
- `WorldRenderer` → `BattlezoneWorldRenderer`；`ClientRenderEvents` → `BattlezoneRenderEvents`；`ClientPacketHandler` → `BattlezoneClientPacketHandler`：注明功能范围。
- `LootEditorSessions` / `PoisonEditorSessions` 从网络包移至对应编辑器功能包。

本次保留游戏类型、设置键、能力类名、资源标识、数据包注册顺序与通道版本；已有地图配置无需迁移。
