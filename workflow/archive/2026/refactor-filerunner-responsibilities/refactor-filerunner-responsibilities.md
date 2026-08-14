# 工作项: refactor-filerunner-responsibilities

描述: 拆分 FileRunner 的职责：将 JDBC 连接管理、引擎路由、PG schema 隔离、override 写回编排抽离为独立类，降低 FileRunner 的耦合度（CA-010）
目标分支: main
源分支: refactor-filerunner-responsibilities
基线提交: a6c8719bc48099cf772a6bd1807876dd4577259c
文档影响: `workflow/archive/2026/refactor-filerunner-responsibilities/design.md`、`plan.md`、`dev-notes.md`、`review.md`

> **本文件须保存为 `workflow/archive/2026/refactor-filerunner-responsibilities/refactor-filerunner-responsibilities.md`**，文件名与目录同名。
> 流程定义见 `workflow/WORKFLOW.md`；看板见 `workflow/STATUS.md`。
> 本工作项的全部产物平铺在 `workflow/archive/2026/refactor-filerunner-responsibilities/`，无子目录、无版本后缀。
> 表内只填枚举、短标签或路径；理由与长说明写进「进度笔记」。

## 门禁

| 路径等级 | Spec | Spec 用户确认 | Design | Review |
|---|---|---|---|---|
| standard | skipped | not-required | required | required |

## 状态

| 状态 | 下一步 | 阻塞原因 | 恢复条件 | 恢复后目标 |
|---|---|---|---|---|
| archived | — |  |  |  |

## 子项（仅 tracking 项填写）

| 子项 id | 状态 |
|---|---|
| — | |

## 进度笔记

- CA-010：FileRunner 当前 210 行，同时承担连接管理（`openConnection`）、引擎路由（`runSqliteFile`/`runPostgresFile`）、PG schema 隔离、override 写回编排、parser 调用编排、sanitize。拆分目标：`ConnectionFactory`（连接创建）、引擎策略（多态路径）、`OverrideCoordinator`（override 写回委托）。
- Spec 跳过：纯内部重构，不改变外部行为或 API 契约。
- 2026-08-14：按 ggnote `WORKFLOW.md` 标准迁移工作流目录（记录与产物合并为同一目录；权威文件改为 `workflow/WORKFLOW.md`）。
