# 武器状态面板

右下角面板使用 `Viewport=QHD.svg` 的 2560×1440 设计坐标。道具槽为 66×66、间隔 4；当前武器卡 262×65，备用卡缩短为 200×40，右边缘保持对齐。整体等比缩放，固定右边距 43、底边距 83 个设计像素，与小队面板采用相同的缩放方式。当前武器使用两侧白色 10px 括角及灰色横边，白色括角加厚为 3px，普通槽框为 2px、选中为 3px；备用枪图标保留 94×34 尺寸，左侧内边距收紧为 10px，键帽跟随卡片移动；弹药数字使用粗体，主武器字号 40/20，备用武器 20/20，备用弹量顶部对齐。

## 组件

- `WeaponHud`：注册 FPSMatch `IHudRenderer`，仅在 battlezone 对局、本地玩家存活且面板库存有效时替换原版快捷栏、物品名和 TaCZ 武器弹药 HUD，同时隐藏原版经验条及等级数字；离开对局恢复原版，旁观者不显示本地武器。
- `WeaponStatusPanel`：读取现有 `PlayerPanelInventory`，布局两个武器卡及三个道具槽。切换主副武器时当前武器变为大卡；选中道具或临时插板时保留最近使用的武器排列，清除武器高亮。
- `WeaponSlotCard`：皮肤感知的武器剪影、选中边框、实际快捷键。当前手持武器隐藏槽位按键提示，备用武器保留提示；主副武器的物理槽位身份保持不变。
- `AmmoReadout` / `WeaponAmmo`：弹匣和膛内弹数、备用弹量。备用弹药来自换弹桥接实际使用的面板弹药槽，支持 TaCZ 弹药、弹药箱、虚拟弹药和 Unidict 普通物品弹药。不修改枪械 NBT。无限弹药显示 `INF`，空弹匣显示红色，长数字在自己的区域内缩放。
- `UtilitySlotCard`：投掷物、两个战术道具共用的图标、数量、冷却遮罩、选中边框和按键提示。空槽保留框与键位，不生成虚构道具图标。
- `FireModeIndicator`：读取 TaCZ 射击模式与实际切换模式按键，直接使用 TaCZ 的 `fire_mode_semi/auto/burst.png` 图标区分射击模式，跟随资源包显示。
- `WeaponSlotSelection`：滚轮在五个可见槽位首尾循环，在原版处理快捷键之前消费第 6～9 槽按键；不干预长按插板的内部切槽。
- `WeaponHudGraphics`：设计坐标绘图、Modern UI 文本、已有 `GunHudIcon` 和 `InventoryPromptRenderer` 桥接。

## 操作和兼容性

沿用真实快捷栏槽位 0..4：主武器、副武器、投掷物、战术一、战术二，默认键为 1..5。面板读取实际键位绑定；安装 Smart Key Prompts 时使用其渲染器，否则使用白底文字键帽。插板继续使用现有长按操作和槽位，不新增面板槽位或改变服务器物品协议。

跳伞提示改为监听整个 HUD 的 Post 事件，不依赖已被取消的原版快捷栏 Post 事件。准星、TaCZ 热量条和其他功能 HUD 保留。

参考接口：[FPSMatch HUD 文档](https://fpsmatch.ptcrys.net/master/docs/developer/client/hud-manager/)、[BlockOffensive CSGameHud](https://github.com/PhasetransCrystal/BlockOffensive/blob/master/src/main/java/net/ptcrys/blockoffensive/client/screen/hud/CSGameHud.java)。当前项目以本地依赖的 `com.ptcrys.fpsmatch` API 为准。

## 验证

148 项单元测试、48 项 GameTest 通过。GameTest 检查膛内弹与弹匣合计、换弹库存与普通库存隔离、消耗后的备用弹量更新和 HUD 读取不修改枪械数据。

真实客户端通过 minecraft-mod-mcp 验证主副武器交换大小卡、五槽按键、投掷物数量 8→7、战术道具冷却及数量 4→3、换弹读数 20/60→31/49、长按插板护甲达到 100、退出对局恢复原版快捷栏。选中第五槽后按 6 和 9，服务器查询 `SelectedItemSlot` 仍为 4。滚轮正反向循环通过单元测试；MCP 释放鼠标的控制模式未触发原生快捷栏滚轮事件，此项未记为实机通过。

截图（不提交）：`run/client/recordings/weapon-hud-primary.png`、`weapon-hud-secondary.png`、`weapon-hud-detail.png`。测试中暂时允许雪球、金苹果、末影珍珠进入道具槽，结束后已恢复原有空白允许列表、重置地图并关闭调试，正常停止服务端保存世界，关闭客户端。

后续实机录屏：`run/client/recordings/weapon-and-armor-hud-test.mp4`。使用真实主副武器及道具，验证当前武器槽位按键提示隐藏、备用武器提示保留，本地护甲板槽随实际消耗 2→1→0，护甲增加至 100，空板槽图标变暗。对局开始会重置经验，因此在对局中再次设置等级为 7，服务器查询确认 `XpLevel=7` 时经验条和等级数字均不可见；退出对局恢复原版快捷栏和经验条。测试账号原始等级为 0，结束后已查询确认恢复为 0；道具规则、调试模式和测试环境均已还原。

上一版 SVG 对齐录屏：`run/client/recordings/weapon-and-armor-hud-svg.mp4`，35 秒、1920×1026、30fps。已实机确认 32px 护甲板槽与两排血条上下对齐、白色括角边框、备用枪位置、粗体弹药与顶部对齐、竖条射击模式图标，覆盖切枪、选中道具及长按插板消耗。完整视频解码检查通过；148 项单元测试再次通过。配套特写为 `weapon-and-armor-hud-svg-detail.png`。测试后恢复道具允许列表、经验等级 0，重置地图并关闭调试，服务端正常保存退出，客户端已关闭。

当前样式调整：护甲板槽扩大为 40px、底边仍与生命条对齐，改为原生物品图标；射击模式恢复 TaCZ 图标，槽位边框加厚。此前录屏保留作历史验证记录。

原生图标版本实机验证：`run/client/recordings/native-hud-verification.mp4`，45 秒、1920×1026、30fps。通过 minecraft-mod-mcp 操作 AK 与 B93R，已检查自动、连发、单发三种 TaCZ 原生图标；40px 护甲槽直接显示真实物品，长按插板数量 2→1→0、护甲到 100，耗尽后不显示占位图标。已确认底边对齐及加厚槽框，完整视频解码无错误。特写为 `native-hud-verification-detail.png`。测试后恢复道具规则、重置地图、关闭调试，服务端正常保存退出，客户端关闭；无临时展示模组。

护甲槽后续放大为 48px，物品图标为 36px；左侧间距及底边对齐保持不变，数量与下方键帽的纵向位置保持不变，开火指示向右移动并维持 8px 间距。
