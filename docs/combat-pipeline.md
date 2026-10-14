# 伤害与死亡接入

BlockZone 复用 FPSMatch 1.3.0 snapshot 的伤害记录、死亡汇总和击杀统计，不直接调用框架事件处理器，也不重复订阅原版死亡事件来结算。

- FPSMatch `onPlayerHurt` → `BattlezoneMap.recordHurtData` → 框架伤害统计与 BlockZone 恢复计时。
- `LivingDamageEvent` 保留为最终致命伤害拦截点：可救援时取消致命伤害并进入倒地；普通死亡及补杀继续进入 FPSMatch。
- FPSMatch 汇总枪击信息 → `BattlezoneMap.handleDeath(DeathContext)` → 原有掉落、清理与旁观 → `BattlezoneDeathEvent`。

## 后续击杀信息入口

在 Forge 事件总线上订阅 `BattlezoneDeathEvent`，读取 `getContext()` 获得 FPSMatch 的攻击者、死者、武器和命中标记。`getCategory()` 复用框架分类器；`isPoisonZone()` 区分毒圈；`getReason()` 区分直接淘汰、流血和失去可救援队友导致的淘汰。事件只在有效的存活 → 淘汰转换后发出，不能取消。此时本模组的淘汰处理已完成，框架后续击杀统计尚可能继续执行。

上下文由框架持有且可变，订阅者应只读使用，发送网络包或保留数据时复制 `ItemStack` 和所需字段。攻击者允许为空，不将环境死亡伪装为自杀。显示图标时使用来源类别和武器信息，不读取攻击者当前手持物品。这里没有实现 HUD 或网络包。

## 倒地来源

`DownedDeathContext` 只保留倒地扩展所需的信息：同 tick、同直接子弹实体的 `FPSMGunDamageEvent` 与击倒伤害匹配，保存到 FPSMatch 的 `DeathContext`。不以“刚被枪击过”判断其他伤害。

流血或失去可救援队友时通过原版 `ServerPlayer.die` 发出死亡事件，交由 FPSMatch 拦截与结算，不再制造额外伤害，再在 `handleDeath` 恢复原武器、爆头、穿墙、穿烟和开镜标记。补杀走补杀本身的框架上下文，不继承击倒者的标记。救起、离开、重置和服务器停止时清理；取消死亡留下的待处理信息会过期。

击杀归属继续使用 FPSMatch 从最终伤害源解析出的攻击者，本次不改变计分规则。框架伤害记录发生在 `LivingHurtEvent` 阶段，伤害统计仍沿用其现有口径。

## 毒圈

`blockzone:poison_zone` 是原生数据包伤害类型，注册为 FPSMatch `ENVIRONMENT` 类别。保留原魔法伤害的绕过护甲及女巫抗性标签，不覆盖其他 `minecraft:magic` 伤害。

## 验证

`./gradlew test` 运行现有单元测试；`./gradlew runGameTestServer` 在独立 `run/gametest` 世界运行服务端测试。`CombatPipelineGameTests` 覆盖伤害类型与标签注册、倒地标记恢复、不同伤害源隔离、一次性消费和清理过期。真实客户端枪击、救援及击杀显示仍需按 AGENTS.md 使用 minecraft-mod-mcp 验证。

## 能力生命周期

毒圈与战斗能力使用 FPSMatch 的 `MapCapability.tick()`，不再通过全局服务器/玩家 tick 推进毒圈、恢复和倒地计时。倒地缓存按所属地图推进、过期和清理；能力移除通过框架 `destroy()` 清理。玩家 HUD 同步仍保留玩家 tick。

此 snapshot 的 `BaseMap.reset()` 不会调用能力 reset，`BattlezoneMap.reset()` 显式调用一次 `getCapabilityMap().resetAll()`。配置预检和冻结仍在 start 中按依赖顺序完成，`init()` 仅表示能力挂载，不作为比赛开始事件。

## 离开与断线

`BattlezoneMap` 按成功入场建立会话标记。`leave()` 先交由 FPSMatch 执行可取消的离开流程，成员确实移除后才统一清理战斗、空降、背包会话和同步缓存，并恢复入场快照；重复 leave 是空操作。

FPSMatch 的 `handlePlayerDisconnect()` 统一清理运行态。比赛中框架保留离线队员，入场快照也随之保留；通过框架 `LoggedInEvent` 恢复会话标记后，真正离开时才恢复快照。离线期间实际离开或比赛重置则把快照排队到下次登录。登录恢复优先于框架普通登录处理。重置强制移除被取消离开的成员时，仍沿用同一清理入口。原有倒地和空降 Forge logout 监听已移除。

## 击杀统计与结算

`BattlezoneStatisticsCapability` 由 FPSMatch 挂载并自动注册/注销事件监听。它在框架完成击杀、爆头和助攻记账后消费 `FPSMapEvent.PlayerEvent.KillEvent`，通过 `map.statistics().lastKill()` 提供只读数值快照，不再次增减框架计数。击杀归属、队杀、助攻选择和计分规则沿用 FPSMatch；事件可能表示未获计数的队杀，因此以快照计数为准。

地图胜利规则先确定胜者，再调用 `super.victory()`。能力消费 `FPSMapEvent.VictoryEvent` 的玩家及队伍快照，生成 `map.getMatchResult()`。结果包含地图名、胜者（平局为空）、结束原因和比赛 tick 数，保留到下一次成功开局；重置清理名单不会清掉已完成结果，能力销毁会释放结果。框架快照中的名字 Component 按只读使用。

流血或队伍覆灭淘汰直接进入原版死亡事件，避免 `Float.MAX_VALUE` 人工伤害进入框架伤害及助攻记录。框架继续负责死亡汇总、一次性击杀和爆头计数。`BattlezoneDeathEvent` 仍用于击杀信息，最终战绩从框架 KillEvent/VictoryEvent 或上述快照读取。没有新增奖励、HUD 或战绩持久化规则。

服务端测试覆盖能力生命周期、离开取消和重复调用、保留离线队员的重连、延迟死亡经真实 ServerPlayer 死亡方法进入框架后的计数及结算，以及平局和结果的生命周期。测试玩家使用无网络连接处理器，真实客户端连线与救援仍需单独验证。

## 战斗参数配置

战斗参数由每张地图的 `combat` 能力保存，开局时冻结整份配置。比赛中执行 set/reset 仅修改下一局配置，不改变当前玩家的倒地、救援或恢复规则。沿用 FPSMatch 地图配置文件保存机制，失败会回滚。

| 字段 | 默认值 | 有效范围 | 含义 |
| --- | --- | --- | --- |
| `matchHealth` | 100 | 1–1024 | 对局最大血量 |
| `armorPlatePoints` | 50 | 1–1024 | 每片护甲点数 |
| `downedHealth` | 50 | 1–1024 | 倒地初始血量，实际不超过玩家最大血量 |
| `bleedSeconds` | 60 | 0.05–3600 | 倒地流血时间，救援期间仍继续计时 |
| `rescueSeconds` | 8 | 0.05–3600 | 连续救援耗时，中断会清空进度 |
| `rescueDistance` | 2.5 | 0.1–64 | 救援最大距离，单位为格 |
| `reviveHealthFraction` | 0.3 | 0.001–1 | 救起后血量占最大血量的比例 |
| `downedSpeedFraction` | 0.25 | 0–1 | 倒地速度占正常速度的比例 |
| `recoveryDelaySeconds` | 5 | 0–3600 | 受伤后的回血等待时间 |
| `recoveryIntervalSeconds` | 0.25 | 0.05–3600 | 每次回血间隔 |
| `recoveryHealthFraction` | 0.05 | 0.001–1 | 每次回复对局最大血量的比例 |

所有数值必须有限；比例填写 0–1 数值。时间向上取整到服务器 tick（每秒 20 tick）。首次回血发生在等待时间加一个回血间隔后，护甲受击也会重新计时。流血与救援同 tick 到期时，优先处理流血淘汰。

```mcfunction
/fpsm map modify battlezone flat160 capability combat get
/fpsm map modify battlezone flat160 capability combat set {"matchHealth":100,"armorPlatePoints":50,"downedHealth":50,"bleedSeconds":60,"rescueSeconds":8,"rescueDistance":2.5,"reviveHealthFraction":0.3,"downedSpeedFraction":0.25,"recoveryDelaySeconds":5,"recoveryIntervalSeconds":0.25,"recoveryHealthFraction":0.05}
/fpsm map modify battlezone flat160 capability combat reset
```

`set` 提交完整配置，所有字段必填；未实现旧配置迁移或缺失字段默认补齐。已有旧 combat 配置应使用 reset 保存完整默认配置后重新设置。网络协议升为 25，救援同步包携带流血总时长、救援总时长与救援距离，客户端提示和进度条使用服务端快照。
