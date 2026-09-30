# 工作项: fix-sql-hash-comments

描述: SQL 体内 `#` 注释行剥离 — 对齐官方 sqllogictest 规格
目标分支: main
源分支: fix-sql-hash-comments
基线提交: 67647eaa46803b54e764f356f2e7ba18d3ca8d8f
文档影响: N/A

> **本文件须保存为 `workflow/workspace/fix-sql-hash-comments/main.md`**。
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
| developing | Developer |  |  |  |

## 进度笔记

- 2026-08-07 登记。P1：官方 sqllogictest 要求剥离 SQL 体中的 `#` 注释行；当前仅顶层与 skipif/onlyif 头处理。`SqlLogicTestParser.readSqlBody()` 与 query 体循环需跳过**行首为 `#`** 的行（对齐官方 C `nextLine()` 的 `zLine[0]!='#'` 与 rs `strip_prefix('#')`）；**行内 `#` 不处理**（避免误伤字符串字面量、PG `#>` 运算符等）。
- 2026-08-14：按 ggnote `WORKFLOW.md` 标准迁移工作流目录（记录与产物合并为同一目录；权威文件改为 `workflow/WORKFLOW.md`）。
- 2026-09-30：Manager 创建源分支 `fix-sql-hash-comments`。基线由登记时预留的 `a6c8719` 调整为实际分支点 `67647ea`（main 当前 HEAD）——原登记仅留名未建分支，且实现须基于最新 parser 代码。状态 `backlog → planning`。
- 2026-09-30：Planner 产出 `plan.md`（fast 路径，Spec/Design/Review skipped，最低验证层 unit）。状态 `planning → developing`，调度 Developer。
