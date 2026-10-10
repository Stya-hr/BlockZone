# 编辑器 UI 组件

组件位于 `src/main/java/dev/stya/blockzone/client/editor/`，使用 Modern UI 控件。

| 组件 | 职责 |
| --- | --- |
| `EditorTheme` | 按钮圆角、边框、背景和交互状态，公共背景与强调色 |
| `EditorControls` | 按钮、文本、横纵布局、分隔线和表单分组 |
| `EditorForm` | 带标签输入框、复选框、折叠帮助，以及输入同步和焦点状态 |
| `EditorSidebar` | 标题、滚动内容和固定底部操作区 |
| `EditorStatusBar` | 左下角无背景单行数量摘要 |
| Modern UI `Toast` | 现成的操作反馈和错误提示，自动消失、不打断编辑 |
| `EditorRowAdapter` | 可复用列表行和选择高亮 |
| `EditorPanel` | Fragment 生命周期、组件组合入口、窗口适配和跨线程调度 |
| `ModernEditorScreen` | Minecraft 与 Modern UI 的渲染和输入桥接 |

页面在 `buildPanel` 中组合控件，通过 `updatePanel` 发布状态；保存由页面覆盖 `save` 方法，侧栏通过回调调用，不依赖具体页面类型。

## 修改约定

- 控件只在 Modern UI 线程创建或修改。游戏草稿操作通过 `EditorPanel.action` 调度到游戏线程，并检查当前页面身份。
- 页面向 UI 发布不可变快照，不在控件回调中直接读取或修改游戏世界。
- `EditorForm.syncValues` 更新输入时抑制变更回调，避免世界拖动造成递归编辑。
- 新页面优先组合现有组件；公共外观修改在 `EditorTheme` 完成。侧栏宽度、窗口缩放和重建仍统一由 `EditorPanel` 管理。
- 组件拆分不改变保存、退出确认、选择或草稿应用流程。
