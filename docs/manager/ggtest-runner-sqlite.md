# 工作项记录: ggtest-runner-sqlite

工作项标识: ggtest-runner-sqlite
描述: GGTEST Runner + 数据库执行器抽象 + 首期 SQLite JDBC 实现；串行执行；skipif/onlyif；halt→skipped；label；statement ok/error；query 比对（依赖 normalize）。父项 epic：`ggtest-core`。
路径等级: full
Spec 门禁: required
Spec 用户确认: required
Design 门禁: required（runner 与数据库执行器抽象边界、SQLite JDBC 适配器分层需 Design）
Review 门禁: required
源分支: 不适用（非 Git 工作区，跳过提交与合并操作）
目标分支: 不适用（非 Git 工作区，跳过提交与合并操作）
文档影响: 新增 docs/features/ggtest-runner-sqlite/；实现阶段可能更新 README 中执行器/连接相关说明
状态: awaiting-spec-approval
后续步骤: 当前用户会话确认 Spec；通过后进入 designing。建议 parser Spec 已确认且 normalize Spec 至少就绪后再开 Design/Plan。确认前不得调度 Planner。
阻塞原因: none
恢复条件: none
恢复后的目标状态: none

> 权威工作流、门禁与状态说明见 [docs/README.md](../README.md)。
> 父项总览：[docs/manager/ggtest-core.md](ggtest-core.md)；总览 Spec：[docs/features/ggtest-core/spec.md](../features/ggtest-core/spec.md)。

## 依赖

- 上游：`ggtest-parser`（记录模型）、`ggtest-normalize`（query 比对）
- 下游：`ggtest-cli-corpus`（CLI 调用 runner）

## 验收范围（对齐原 ggtest-core Spec）

- P0-3 statement 断言
- P0-6 条件控制
- P0-8 扩展点隔离
- P1-2 halt
- P1-4 label 一致性

## 继承的全局约束（勿无故重开）

- 首期仅 SQLite（JDBC）；多库扩展点在本项成型
- Q6 halt→skipped
- 目标库标识 `sqlite`（skipif/onlyif，大小写不敏感）
- 产品名 GGTEST；Java 17；Maven

## 进度笔记

- 2026-07-24：由 `ggtest-core` 拆分登记；路径 full；Spec/Design/Review 门禁均为 required。
- 2026-07-24：Analyst 产出 `docs/features/ggtest-runner-sqlite/spec.md`（验收 P0-3/6/8、P1-2/4）。状态 → `awaiting-spec-approval`。未调度 Planner。
