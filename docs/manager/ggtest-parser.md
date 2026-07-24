# 工作项记录: ggtest-parser

工作项标识: ggtest-parser
描述: GGTEST 解析器——解析 `.test`/`.slt`（及不强制扩展名的单文件）sqllogictest 输入，产出记录模型；解析错误须带文件名与行号。不连库。父项 epic：`ggtest-core`。
路径等级: full
Spec 门禁: required
Spec 用户确认: required
Design 门禁: required（解析器模块边界、记录模型形状与错误报告边界需在 Design 明确）
Review 门禁: required
源分支: 不适用（非 Git 工作区，跳过提交与合并操作）
目标分支: 不适用（非 Git 工作区，跳过提交与合并操作）
文档影响: 新增 docs/features/ggtest-parser/；实现阶段可能更新项目 README 中与解析/输入格式相关的说明
状态: awaiting-spec-approval
后续步骤: 当前用户会话确认 Spec（建议优先确认本项）；通过后进入 designing，再调度 Planner。确认前不得调度 Planner。
阻塞原因: none
恢复条件: none
恢复后的目标状态: none

> 权威工作流、门禁与状态说明见 [docs/README.md](../README.md)。
> 父项总览：[docs/manager/ggtest-core.md](ggtest-core.md)；总览 Spec：[docs/features/ggtest-core/spec.md](../features/ggtest-core/spec.md)。

## 依赖

- 上游：无（四子项中最先可独立开干）
- 下游：`ggtest-runner-sqlite`、`ggtest-cli-corpus`（消费解析产物）

## 验收范围（对齐原 ggtest-core Spec）

- P0-7 解析错误定位

## 继承的全局约束（勿无故重开）

- 产品名 GGTEST；Java 17；Maven；CLI 优先（本项不交付 CLI）
- `.slt` 与 `.test` 等价；单文件路径不强制扩展名
- 用户自备语料路径（本项可不连库、不跑语料）

## 进度笔记

- 2026-07-24：由 `ggtest-core` 拆分登记；路径 full；Spec/Design/Review 门禁均为 required。
- 2026-07-24：Analyst 产出 `docs/features/ggtest-parser/spec.md`（验收 P0-7 + P1-a/b/c）。状态 → `awaiting-spec-approval`。未调度 Planner。
