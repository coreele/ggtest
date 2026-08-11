# 工作项记录: add-parallel-execution

工作项标识: add-parallel-execution
描述: `--parallel <N>` 并行执行独立 .slt 文件（大语料加速）
目标分支: main
文档影响: README（en + zh）新增 `--parallel` 选项与并行语义说明；用户文档

> 权威流程见 [workflow/README.md](../../README.md)；活跃状态见 [STATUS.md](STATUS.md)。

## 切片门禁（未拆分，sub-feature-id = add-parallel-execution）

| sub-feature-id | 路径等级 | 源分支 | Spec | Spec 门禁 | Spec 用户确认 | Design 门禁 | Review 门禁 |
|---|---|---|---|---|---|---|---|
| add-parallel-execution | standard | add-parallel-execution | [spec.md](../features/add-parallel-execution/spec.md) | required | required | required | required |

## 切片状态

| sub-feature-id | 状态 | 后续步骤 | 阻塞原因 | 恢复条件 | 恢复后目标 |
|---|---|---|---|---|---|
| add-parallel-execution | qa | QA | | | |

## 进度笔记

- 2026-08-11 登记。来源 `TODO:23-25`（P3 并行文件执行）。原文：`--parallel <N> 并行执行独立文件`；`需注意 PG schema 隔离的并发安全（每文件独立 schema 已支持）`；`大语料（select1/2/3 数千条）下显著加速`。
- 路径等级 standard：新增公开 CLI 表面（`--parallel`）+ 行为/状态转换（顺序→并发执行模型）+ 跨模块（CLI 解析↔执行编排↔JDBC 资源生命周期↔输出聚合）。
- Spec 用户确认 required：存在业务歧义（N=0/1 语义、`--halt` 在并发下的重新定义、输出契约、`--override` 互斥）。
- Design 门禁 required：并发模型（ExecutorService/thread pool）、线程安全聚合、输出序列化。
- 2026-08-11：用户会话确认 Plan；检查 `conn=<name>` 不受并行影响（per-file 机制天然隔离）。状态 → planned，调度 Developer。
- 2026-08-11：Developer 实施完成（commit `04f8c72`）。12 文件修改 + 2 文件新建，444 行新增。T1-T9 全部完成，343 tests / 0 failures / 17 skipped。状态 → reviewing，调度 Reviewer。
- 2026-08-11：Reviewer 审阅版本 `04f8c72`，结论 **Approve**（无阻塞项，review.md 保留工作区）。状态 → qa，调度 QA。
