# 工作项: fix-sql-hash-comments

描述: SQL 体内 `#` 注释裁剪 — 对齐官方 sqllogictest 规格
目标分支: main
源分支: fix-sql-hash-comments
基线提交: a6c8719bc48099cf772a6bd1807876dd4577259c
文档影响: N/A

> **本文件须保存为 `workflow/workspace/fix-sql-hash-comments/fix-sql-hash-comments.md`**，文件名与目录同名。
> 流程定义见 `workflow/WORKFLOW.md`；看板见 `workflow/STATUS.md`。
> 本工作项的全部产物平铺在 `workflow/workspace/fix-sql-hash-comments/`，无子目录、无版本后缀。
> 表内只填枚举、短标签或路径；理由与长说明写进「进度笔记」。

## 门禁

| 路径等级 | Spec | Spec 用户确认 | Design | Review |
|---|---|---|---|---|
| fast | skipped | not-required | skipped | skipped |

## 状态

| 状态 | 下一步 | 阻塞原因 | 恢复条件 | 恢复后目标 |
|---|---|---|---|---|
| backlog | Planner |  |  |  |

## 进度笔记

- 2026-08-07 登记。P1：官方 sqllogictest 要求裁剪 SQL 体中的 `#` 注释；当前仅 skipif/onlyif 头处理。`SqlLogicTestParser.readSqlBody()` 需逐行 strip `#` 及后续内容。
- 2026-08-14：按 ggnote `WORKFLOW.md` 标准迁移工作流目录（记录与产物合并为同一目录；权威文件改为 `workflow/WORKFLOW.md`）。
