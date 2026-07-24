# 工作项记录: ggtest-normalize

工作项标识: ggtest-normalize
描述: GGTEST 结果规范化——I/T/R 规范化、nosort/rowsort/valuesort、MD5 与官方 C 实现逐字节兼容、hash-threshold 语义。可用固定样例验收，不必真跑库。父项 epic：`ggtest-core`。
路径等级: full
Spec 门禁: required
Spec 用户确认: required
Design 门禁: skipped（规范化算法、哈希拼接与阈值语义已在 Spec 合同级写死；无额外模块边界或技术选型待决）
Review 门禁: required
源分支: 不适用（非 Git 工作区，跳过提交与合并操作）
目标分支: 不适用（非 Git 工作区，跳过提交与合并操作）
文档影响: 新增 docs/features/ggtest-normalize/
状态: awaiting-spec-approval
后续步骤: 当前用户会话确认 Spec；通过后进入 planning（Design 已 skipped），再调度 Planner。确认前不得调度 Planner。
阻塞原因: none
恢复条件: none
恢复后的目标状态: none

> 权威工作流、门禁与状态说明见 [docs/README.md](../README.md)。
> 父项总览：[docs/manager/ggtest-core.md](ggtest-core.md)；总览 Spec：[docs/features/ggtest-core/spec.md](../features/ggtest-core/spec.md)。

## 依赖

- 上游：无（可与 parser 并行；不依赖库执行）
- 下游：`ggtest-runner-sqlite`（query 比对依赖本项）

## 验收范围（对齐原 ggtest-core Spec）

- P0-2 哈希兼容
- P0-4 结果规范化
- P0-5 排序模式
- P1-3 valuesort

## 继承的全局约束（勿无故重开）

- Q5 hash-threshold 默认 8
- MD5 与官方 C 实现逐字节兼容（官方语料既有哈希为判据）
- 产品名 GGTEST；Java 17；Maven

## 进度笔记

- 2026-07-24：由 `ggtest-core` 拆分登记；路径 full；Spec/Review required；Design skipped。
- 2026-07-24：Analyst 产出 `docs/features/ggtest-normalize/spec.md`（验收 P0-2/4/5、P1-3）。状态 → `awaiting-spec-approval`。未调度 Planner。
