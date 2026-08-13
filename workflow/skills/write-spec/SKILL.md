---
name: write-spec
description: >-
  Writes requirements and behavioral specs to
  workflow/docs/features/<feature-id>/spec.md.
  Invoked by Analyst when Spec gate requires it, scheduled by Manager after
  work item registration and before /planner. Use when Spec gate requires it,
  path is full/standard with unclear contracts, or user asks for 规格/spec.
  Skip for fast/small fixes. Do not use for task breakdown (that is plan.md).
---

# write-spec — 需求与规格

## 调用者与门禁

- 调用者：Analyst。
- 调用条件：工作项记录中对应 `feature-id` 的 Spec 门禁为 `required`。
- 执行时机：Manager 登记工作项后、Planner 开始 Design 或 Plan 前。
- 产出：`workflow/docs/features/<feature-id>/spec.md`（模板 `workflow/docs/_templates/spec.md`）。

本 Skill 仅定义需求与规格。实施任务拆分属于 Planner 的 `plan.md`（写在同一目录）。

## 适用条件

- `full` 路径的 Spec 门禁必须为 `required`。
- `standard` 路径涉及新增行为、公开接口、状态转换、错误约定或跨模块合同时，Spec 门禁必须为 `required`。
- `fast` 路径默认跳过；仅以工作项记录中的 Spec 门禁判定是否执行。

## 调研

必须读取用户表述、`workflow/docs/manager/<feature-id>.md`、`workflow/README.md`、相关源码和现有文档。执行本 Skill 不要求 `plan.md` 已存在。

## 必含内容

每份 Spec 必须包含背景与目标、非目标、范围与可见行为、合同、验收条件和开放问题。合同必须覆盖适用的 API、数据、状态和错误约定；不适用时标记 `N/A`。验收条件使用 Given-When-Then，并标记 P0 或 P1。

每条 P0 必须可验证。

`full` 路径的 Spec，以及工作项记录标注存在业务歧义的 `standard` 路径 Spec，必须提示当前用户会话取得用户确认。

## 边界

必须遵循 `workflow/docs/standards/documentation.md`。禁止编写 Plan、Design 或业务代码，禁止修改 `workflow/docs/manager/STATUS.md` 或工作项记录。

## 执行与交接

1. 确认 `<feature-id>` 与 Spec 门禁。
2. 完成调研，编写对应 Spec 文件，并依据必含内容自检。
3. Git：若源分支已声明，按 `workflow/docs/standards/git.md` 在源分支提交；尚未声明则可将文件留在工作区并报告 Manager。非 Git 跳过提交。
4. 向 Manager 报告产出路径、验证结果和用户确认要求后**立即返回**（不得阻塞等待确认）。状态建议为 `speccing`；需要确认时建议进入 `awaiting-spec-approval`。

Manager 随后调度 Planner。Design 门禁为 `required` 时调用 `design-architecture`；范围含界面且需 UI/UX 决策时调用 `design-ui`；否则可直接进入 Plan。Manager 不得执行上述 Design skill。
