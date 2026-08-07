# 工作项记录: fix-sql-hash-comments

工作项标识: fix-sql-hash-comments
描述: SQL 体内 `#` 注释裁剪 — 对齐官方 sqllogictest 规格
路径等级: fast
源分支: fix-sql-hash-comments
目标分支: main
文档影响: N/A

> 权威流程见 [workflow/README.md](../../README.md)；活跃状态见 [STATUS.md](STATUS.md)。

## 切片门禁（未拆分）

| sub-feature-id | 路径等级 | 源分支 | Spec | Spec 门禁 | Spec 用户确认 | Design 门禁 | Review 门禁 |
|---|---|---|---|---|---|---|---|
| fix-sql-hash-comments | fast | fix-sql-hash-comments | 无 | skipped | not-required | skipped | skipped |

## 切片状态

| sub-feature-id | 状态 | 后续步骤 |
|---|---|---|
| fix-sql-hash-comments | backlog | Planner |

## 进度笔记

- 2026-08-07 登记。P1：官方 sqllogictest 要求裁剪 SQL 体中的 `#` 注释；当前仅 skipif/onlyif 头处理。`SqlLogicTestParser.readSqlBody()` 需逐行 strip `#` 及后续内容。
