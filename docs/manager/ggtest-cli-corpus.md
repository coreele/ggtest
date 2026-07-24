# 工作项记录: ggtest-cli-corpus

工作项标识: ggtest-cli-corpus
描述: GGTEST CLI（`ggtest`）、统计报告、退出码、目录递归收集 `*.test`/`*.slt`、官方语料硬验收（select1；P1 含 select1–3）、零豁免。父项 epic：`ggtest-core`。
路径等级: full
Spec 门禁: required
Spec 用户确认: required
Design 门禁: skipped（CLI 参数语义、退出码与收集规则已在 Spec 合同级写死；无额外模块边界待决。组装依赖上游 runner/parser）
Review 门禁: required
源分支: 不适用（非 Git 工作区，跳过提交与合并操作）
目标分支: 不适用（非 Git 工作区，跳过提交与合并操作）
文档影响: 新增 docs/features/ggtest-cli-corpus/；实现阶段更新项目 README 为工具使用说明
状态: awaiting-spec-approval
后续步骤: 当前用户会话确认 Spec；通过后进入 planning（Design 已 skipped）。Design/Plan 建议在 runner 就绪后再开。确认前不得调度 Planner。
阻塞原因: none
恢复条件: none
恢复后的目标状态: none

> 权威工作流、门禁与状态说明见 [docs/README.md](../README.md)。
> 父项总览：[docs/manager/ggtest-core.md](ggtest-core.md)；总览 Spec：[docs/features/ggtest-core/spec.md](../features/ggtest-core/spec.md)。

## 依赖

- 上游：`ggtest-parser`、`ggtest-normalize`、`ggtest-runner-sqlite`（端到端硬验收）
- 下游：无（四子项中最后集成验收）

## 验收范围（对齐原 ggtest-core Spec）

- P0-1 官方语料跑通（select1.test）
- P1-1 目录与多文件
- P1-5 语料批量跑通（select1/2/3）
- P1-6 显式 `.slt` 单文件

## 继承的全局约束（勿无故重开）

- Q7/Q8 零豁免硬验收；Q9 P1-5=select1/2/3
- Q4 用户自备语料路径
- Q5 hash-threshold 默认 8（CLI 可覆盖）
- `.slt` 与 `.test` 等价
- 产品名 GGTEST；Java 17；Maven；CLI 优先

## 进度笔记

- 2026-07-24：由 `ggtest-core` 拆分登记；路径 full；Spec/Review required；Design skipped。
- 2026-07-24：Analyst 产出 `docs/features/ggtest-cli-corpus/spec.md`（验收 P0-1、P1-1/5/6）。状态 → `awaiting-spec-approval`。未调度 Planner。
