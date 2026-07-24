---
name: design-architecture
description: Use when the Planner must resolve module boundaries, layering, or technology choices before writing a Plan.
---

# design-architecture — 结构设计

## 调用者与门禁

- 调用者：Planner。
- 调用条件：工作项记录中的 Design 门禁为 `required`。
- 执行时机：Spec 门禁满足后、Plan 编写前。
- 输入：
  - `docs/manager/<feature-id>.md`；
  - `docs/features/<feature-id>/spec.md`（Spec 门禁为 `required` 时）。
- 产出：`docs/features/<feature-id>/design.md`。

## 设计范围

仅记录需要决策的结构事项：

- 模块边界与职责；
- 分层及依赖方向；
- 技术选型、备选方案与取舍；
- 决策对模块、迁移、风险和验证策略的影响。

`design.md` 必须说明设计背景、约束、候选方案、决策、影响与风险。存在可行替代方案时，必须记录比较依据。

## 边界

API 形状、数据约束、错误约定和行为验收属于 Spec。发现这些合同信息缺失或存在歧义时，必须停止并向 Manager 报告，不得在 `design.md` 中替代 Spec 作出需求决策。

禁止编写实现代码、实施任务拆分、Plan 或工作项状态。禁止修改 `feature-id`，产出必须位于既有的 `docs/features/<feature-id>/`。

## 后续步骤

`design.md` 完成后返回 Planner。Planner 依据已满足门禁的 Spec、工作项记录和 `design.md` 编写 `plan.md`。
