# 工作项: fix-ca018-search-path-validation

描述: 修复 setSearchPath 缺标识符校验 + 抽取共享 SchemaNames（CA-018）
目标分支: main
源分支: fix-ca018-search-path-validation
基线提交: a6c8719bc48099cf772a6bd1807876dd4577259c
文档影响: 

> **本文件须保存为 `workflow/archive/2026/fix-ca018-search-path-validation/fix-ca018-search-path-validation.md`**，文件名与目录同名。
> 流程定义见 `workflow/WORKFLOW.md`；看板见 `workflow/STATUS.md`。
> 本工作项的全部产物平铺在 `workflow/archive/2026/fix-ca018-search-path-validation/`，无子目录、无版本后缀。
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

- 来源：`workflow/audit/2026-08-13-src.md` Findings CA-018。
- 根因与修复方向见 [plan.md](plan.md)。
- 2026-08-13：Developer 实施完成 —— 新增 `com.ggtest.db.SchemaNames`（generate/isSafe/requireSafe，不引用 JDBC，requireSafe 抛 IllegalArgumentException 以满足 base 包 driver-agnostic 守护）；两个 isolation 类 prepare/teardown/setSearchPath 统一经 SchemaNames（setSearchPath 补校验）；新增 `SchemaNamesTest`（5 测试）。`SchemaNamesTest` 5/0；`mvn clean test` 365/0/0（34 既有 skip）。状态 → developing，待 Reviewer。
- 2026-08-14：按 ggnote `WORKFLOW.md` 标准迁移工作流目录（记录与产物合并为同一目录；权威文件改为 `workflow/WORKFLOW.md`）。
