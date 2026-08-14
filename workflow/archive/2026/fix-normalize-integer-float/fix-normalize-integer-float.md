# 工作项: fix-normalize-integer-float

描述: 修复 `query I` 对浮点样数字字符串（如 JDBC `getString` 的 `"1.0"`/`"5.0"`）误走 `Long.parseLong` 失败并归一为 `"0"` 的问题；对齐官方 sqllogictest `%d`（可解析数值后向零截断）。消除 `slt_lang_aggfunc.test` 中 total()/avg() 及连带 label-sum 冲突等位点。不含 sum 溢出语义与 total 大浮点精度漂移（见并列工作项）。
目标分支: main
源分支: fix-normalize-integer-float
基线提交: a6c8719bc48099cf772a6bd1807876dd4577259c
文档影响: workflow/features 产物已迁入本归档目录；默认不改 README 产品合同

> **本文件须保存为 `workflow/archive/2026/fix-normalize-integer-float/fix-normalize-integer-float.md`**，文件名与目录同名。
> 流程定义见 `workflow/WORKFLOW.md`；看板见 `workflow/STATUS.md`。
> 本工作项的全部产物平铺在 `workflow/archive/2026/fix-normalize-integer-float/`，无子目录、无版本后缀。
> 表内只填枚举、短标签或路径；理由与长说明写进「进度笔记」。

## 门禁

| 路径等级 | Spec | Spec 用户确认 | Design | Review |
|---|---|---|---|---|
| fast | skipped | not-required | skipped | required |

## 状态

| 状态 | 下一步 | 阻塞原因 | 恢复条件 | 恢复后目标 |
|---|---|---|---|---|
| archived | — |  |  |  |

## 子项（仅 tracking 项填写）

| 子项 id | 状态 |
|---|---|
| — | |

## 进度笔记

- 2026-08-06：登记 → planning → awaiting-plan-approval → planned → developing → reviewing → qa → **done**；合入 `main`（`8ed63db`）；归档至本目录；不 push。
- 2026-08-14：按 ggnote `WORKFLOW.md` 标准迁移工作流目录（记录与产物合并为同一目录；权威文件改为 `workflow/WORKFLOW.md`）。
