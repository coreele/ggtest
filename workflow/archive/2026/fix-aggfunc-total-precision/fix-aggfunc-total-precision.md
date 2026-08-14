# 工作项: fix-aggfunc-total-precision

描述: `slt_lang_aggfunc.test` ~491：`total()` 大浮点期望 `-18446744073709550000.000` vs 实际 `-18446744073709552000.000`。属语料/SQLite/JDBC double 格式化漂移，与整数归一化（WI-1）独立。**已 cancelled / wontfix：已知限制，不专门对齐原版。**
目标分支: main
源分支: fix-aggfunc-total-precision
基线提交: a6c8719bc48099cf772a6bd1807876dd4577259c
文档影响: 无（未产出 spec/plan；空 features 目录已随归档迁入；不移植 SQLite float printf，不改语料）

> **本文件须保存为 `workflow/archive/2026/fix-aggfunc-total-precision/fix-aggfunc-total-precision.md`**，文件名与目录同名。
> 流程定义见 `workflow/WORKFLOW.md`；看板见 `workflow/STATUS.md`。
> 本工作项的全部产物平铺在 `workflow/archive/2026/fix-aggfunc-total-precision/`，无子目录、无版本后缀。
> 表内只填枚举、短标签或路径；理由与长说明写进「进度笔记」。

## 门禁

| 路径等级 | Spec | Spec 用户确认 | Design | Review |
|---|---|---|---|---|
| standard | skipped | not-required | skipped | required |

## 状态

| 状态 | 下一步 | 阻塞原因 | 恢复条件 | 恢复后目标 |
|---|---|---|---|---|
| cancelled | — |  |  |  |

## 子项（仅 tracking 项填写）

| 子项 id | 状态 |
|---|---|
| — | |

## 进度笔记

- 2026-08-06：Manager 登记；状态保持 **`backlog`**；不调度产出角色，直至恢复条件满足。
- 2026-08-06：WI-1 已 done；本项仍 **`backlog`**（用户要求勿自动推进）。
- 2026-08-06：用户确认 cancelled / wontfix（已知限制）；状态 `backlog` → **`cancelled`**；空 `workflow/archive/2026/fix-aggfunc-total-precision/` 与本工作项记录一并归档至 `workflow/archive/2026/fix-aggfunc-total-precision/`；不改产品代码、不改 `slt_lang_aggfunc.test`；按用户指示不主动 commit。
- 2026-08-14：按 ggnote `WORKFLOW.md` 标准迁移工作流目录（记录与产物合并为同一目录；权威文件改为 `workflow/WORKFLOW.md`）。
