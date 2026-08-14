# 工作项: refactor-cli-session-boundaries

描述: 审计 Tech Debt Medium — 拆分过大的 `CliSession`：抽出 `FileRunner` / `ReportWriter`（或等价边界），保持 `DatabaseExecutor` 隔离。来源：`workflow/audit/2026-07-26-src.md`。
目标分支: main
源分支: refactor-cli-session-boundaries
基线提交: a6c8719bc48099cf772a6bd1807876dd4577259c
文档影响: workflow/archive/2026/refactor-cli-session-boundaries/design.md、plan.md；登记册 CA-003

> **本文件须保存为 `workflow/archive/2026/refactor-cli-session-boundaries/refactor-cli-session-boundaries.md`**，文件名与目录同名。
> 流程定义见 `workflow/WORKFLOW.md`；看板见 `workflow/STATUS.md`。
> 本工作项的全部产物平铺在 `workflow/archive/2026/refactor-cli-session-boundaries/`，无子目录、无版本后缀。
> 表内只填枚举、短标签或路径；理由与长说明写进「进度笔记」。

## 门禁

| 路径等级 | Spec | Spec 用户确认 | Design | Review |
|---|---|---|---|---|
| standard | skipped | not-required | skipped | required |

## 状态

| 状态 | 下一步 | 阻塞原因 | 恢复条件 | 恢复后目标 |
|---|---|---|---|---|
| archived | — |  |  |  |

## 子项（仅 tracking 项填写）

| 子项 id | 状态 |
|---|---|
| — | |

## 进度笔记

- 2026-07-26：登记；关联 Tech Debt 5 / CA-003；暂 backlog 等依赖。
- 2026-07-26：依赖 QA Pass；Design+Plan 完成并 approved → developing。

## 合入授权

- **approved**（2026-07-26）：用户批准合入全部五分支 → `main`；优先 rebase + FF；**不 push**。
- 状态：**done**（授权后关闭；合入见 git）。
- 2026-08-14：按 ggnote `WORKFLOW.md` 标准迁移工作流目录（记录与产物合并为同一目录；权威文件改为 `workflow/WORKFLOW.md`）。
