# 工作项记录: fix-pg-teardown-once

工作项标识: fix-pg-teardown-once
描述: 审计 Tech Debt Low — PG teardown：try 内 hardFailure 时 finally 可能二次 DROP → 统一失败路径，避免双 teardown。来源：`agents/audit/2026-07-26-src.md`。
路径等级: fast
源分支: fix-pg-teardown-once
目标分支: main
文档影响: agents/features/fix-pg-teardown-once/；登记册 CA-006

> 权威工作流、门禁与状态说明见 [agents/README.md](../../README.md)。
> 活跃状态见 [STATUS.md](../../manager/STATUS.md)。

## 切片（未拆分时仅一行，sub-feature-id = feature-id）

| sub-feature-id | Spec | Spec 门禁 | Spec 用户确认 | Design 门禁 | Review 门禁 | 状态 | 后续步骤 |
|---|---|---|---|---|---|---|---|
| fix-pg-teardown-once | N/A | skipped（单点失败路径整理） | not-required | skipped | skipped（fast） | **done** | none |

阻塞原因: none
恢复条件: none
恢复后的目标状态: none

## Plan 确认

- **approved**（2026-07-26）：用户授权；依据 `agents/features/fix-pg-teardown-once/plan.md`

## Manager 决策（用户 2026-07-26 授权自行决断）

- 路径 fast；统一 finally teardown；合入前停合并授权。
- 与 `fix-cli-credential-redaction` 同触 `CliSession`：可并行但注意合并冲突；优先脱敏项先合或同批协调。

## 进度笔记

- 2026-07-26：登记；关联 Tech Debt 6 / CA-006。
- 2026-07-26：Plan approved → planned → 调度 Developer（worktree）。

## 合入授权

- **approved**（2026-07-26）：用户批准合入全部五分支 → `main`；优先 rebase + FF；**不 push**。
- 状态：**done**（授权后关闭；合入见 git）。

