---
name: design-ui
description: >-
  Writes UI/UX design decisions to workflow/docs/features/.../ui-design.md for
  work items with user-facing screens, flows, or interactive surfaces. Invoked
  by Planner after Spec is satisfied and before Plan when the scope includes
  UI/UX. Skip for backend-only, API-only, docs-only, or fast copy/CSS tweaks
  with no layout or information-architecture decisions. Do not use for module
  boundaries or tech stack (that is design-architecture / design.md).
---

# design-ui — 界面与交互设计

## 调用者与时机

- 调用者：Planner。
- **不是**新门禁：无 STATUS 字段；按范围按需调用。
- 执行时机：Spec 门禁已满足后、Plan 编写前。可与 `design-architecture` 并行或先后（二者互不替代）。

**输入：**

- `workflow/docs/manager/<feature-id>.md`
- Spec（若有）：`workflow/docs/features/<feature-id>/spec.md`
- 仓库既有设计系统、组件库、品牌或布局约定（若有）

**产出：** `workflow/docs/features/<feature-id>/ui-design.md`（模板 `workflow/docs/_templates/ui-design.md`）

## 何时调用 / 跳过

| 调用 | 跳过 |
|---|---|
| 新增或重做页面、关键界面流、表单向导、导航信息架构 | 纯后端 / API / 脚本 / 文档 |
| 布局层级、视觉方向、组件选用有决策空间 | 仅改文案、颜色 token 微调、无布局/IA 变更的小样式 |
| 响应式断点或无障碍行为需要约定 | `fast` 且 Plan 可直接写清触碰路径与验收、无需单独 UI 决策 |

无法判断时：若 Spec 含用户可见界面且存在开放的布局/交互问题，则调用；否则跳过并在 Plan 元信息将「依据 UI」标为 `N/A`。

## 设计范围

只记录需要决策的界面与交互事项：

- 用户与关键场景
- 信息架构与关键界面（首屏组成）
- 关键用户流程（步骤、分支、空态 / 加载 / 错误）
- 布局层级与视觉方向（含需避开的通用 AI 审美套路，若适用）
- 组件与交互约定（含既有设计系统复用）
- 响应式与无障碍要点
- 对 Plan / Developer 的可执行约束

## 边界

| 属于 | 不属于（勿写入本文件） |
|---|---|
| Spec | 行为合同、数据/错误约定、Given-When-Then 验收 |
| `design.md` | 模块边界、分层、技术选型 |
| `plan.md` | 任务拆分、验证命令 |

发现 Spec 缺少必要的可见行为或验收时，**停止**并向 Manager 报告，不得在 `ui-design.md` 补写需求合同。

禁止编写实现代码、Plan 或修改工作项状态 / `feature-id`。

## 必含与自检

按模板填写；不适用节标 `N/A` 并一句说明。有可行替代方案时简要对比并写决策理由。

初稿完成后按 `workflow/docs/standards/documentation.md` §B 原位整理。

Git：源分支已声明则按 `workflow/docs/standards/git.md` 在源分支提交；未声明则留工作区并报告 Manager。

## 后续

返回 Planner。Planner 在同一目录编写 `plan.md` 时必须引用本文件（若已产出），并将界面任务与验证落到 Plan，不得整份复述 UI 稿。
