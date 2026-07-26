# 工作项记录: fix-shared-defaults

工作项标识: fix-shared-defaults
描述: 审计 Finding Low §4 + Tech Debt Low — 收敛 `DEFAULT_HASH_THRESHOLD` 与 `DEFAULT_COLUMN_SEPARATOR` 为单一权威常量（normalize 导出，CLI/parser 引用）。来源：`docs/audit/2026-07-26-src.md`。
路径等级: fast
源分支: fix-shared-defaults
目标分支: main
文档影响: docs/features/fix-shared-defaults/；登记册 CA-004 / CA-005

> 权威工作流、门禁与状态说明见 [docs/README.md](../README.md)。
> 活跃状态见 [STATUS.md](STATUS.md)。

## 切片（未拆分时仅一行，sub-feature-id = feature-id）

| sub-feature-id | Spec | Spec 门禁 | Spec 用户确认 | Design 门禁 | Review 门禁 | 状态 | 后续步骤 |
|---|---|---|---|---|---|---|---|
| fix-shared-defaults | N/A | skipped（常量收敛，无新行为合同） | not-required | skipped | skipped（fast；范围明确单点） | **done** | none |

阻塞原因: none
恢复条件: none
恢复后的目标状态: none

## Plan 确认

- **approved**（2026-07-26）：用户授权；依据 `docs/features/fix-shared-defaults/plan.md`

## Manager 决策（用户 2026-07-26 授权自行决断）

- 路径 fast；Spec/Design/Review 跳过；Plan 由 Manager 拍板确认。
- 权威：`ResultComparer`（或 normalize 包）导出常量；`CliArgumentParser` / `SqlLogicTestParser` 引用。
- 合入前停合并授权。

## 进度笔记

- 2026-07-26：登记；关联 Finding 4 + Tech Debt 7。
- 2026-07-26：Plan approved → planned → 调度 Developer（worktree）。

## 合入授权

- **approved**（2026-07-26）：用户批准合入全部五分支 → `main`；优先 rebase + FF；**不 push**。
- 状态：**done**（授权后关闭；合入见 git）。

