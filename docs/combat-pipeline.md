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

流血或失去可救援队友时仍通过原有 `player.hurt` 进入框架死亡管线，再在 `handleDeath` 恢复原武器、爆头、穿墙、穿烟和开镜标记。补杀走补杀本身的框架上下文，不继承击倒者的标记。救起、离开、重置和服务器停止时清理；取消死亡留下的待处理信息会过期。

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

FPSMatch 的 `handlePlayerDisconnect()` 统一清理运行态并把快照排队到下次登录，随后框架调用 leave 时不再清理或恢复第二次。登录恢复优先于框架普通登录处理。重置强制移除被取消离开的成员时，仍沿用同一清理入口。原有倒地和空降 Forge logout 监听已移除。
