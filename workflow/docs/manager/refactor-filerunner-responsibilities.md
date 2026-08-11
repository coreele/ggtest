# 工作项记录: refactor-filerunner-responsibilities

工作项标识: refactor-filerunner-responsibilities
描述: 拆分 FileRunner 的职责：将 JDBC 连接管理、引擎路由、PG schema 隔离、override 写回编排抽离为独立类，降低 FileRunner 的耦合度（CA-010）
目标分支: main
文档影响: `workflow/docs/features/refactor-filerunner-responsibilities/design.md`、`plan.md`、`dev-notes.md`、`review.md`

> 权威流程见 [workflow/README.md](../../README.md)；活跃状态见 [STATUS.md](STATUS.md)。

## 切片门禁

| sub-feature-id | 路径等级 | 源分支 | Spec | Spec 门禁 | Spec 用户确认 | Design 门禁 | Review 门禁 |
|---|---|---|---|---|---|---|---|
| refactor-filerunner-responsibilities | standard | refactor-filerunner-responsibilities | N/A | skipped | N/A | required | required |

## 切片状态

| sub-feature-id | 状态 | 后续步骤 | 阻塞原因 | 恢复条件 | 恢复后目标 |
|---|---|---|---|---|---|---|
| refactor-filerunner-responsibilities | done | 待合入 main | | | |

## 进度笔记

- CA-010：FileRunner 当前 210 行，同时承担连接管理（`openConnection`）、引擎路由（`runSqliteFile`/`runPostgresFile`）、PG schema 隔离、override 写回编排、parser 调用编排、sanitize。拆分目标：`ConnectionFactory`（连接创建）、引擎策略（多态路径）、`OverrideCoordinator`（override 写回委托）。
- Spec 跳过：纯内部重构，不改变外部行为或 API 契约。
