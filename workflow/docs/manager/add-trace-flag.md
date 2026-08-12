# 工作项记录: add-trace-flag

工作项标识: add-trace-flag
描述: 新增 `--trace` CLI 标志，执行 SQL 语句时实时打印正在执行的 SQL 到 stderr（诊断用途，不影响 stdout 报告）。
目标分支: main
文档影响: README（`--trace` 选项说明）、`--help`。

## 切片门禁

| sub-feature-id | 路径等级 | 源分支 | Spec | Spec 门禁 | Spec 用户确认 | Design 门禁 | Review 门禁 |
|---|---|---|---|---|---|---|---|
| add-trace-flag | standard | main | N/A | skipped | N/A | skipped | required |

## 切片状态

| sub-feature-id | 状态 | 后续步骤 | 阻塞原因 | 恢复条件 | 恢复后目标 |
|---|---|---|---|---|---|
| add-trace-flag | done | — | | | |

## 进度笔记

### 登记背景（2026-08-12）

用户要求实现 `--trace` 功能：运行 SQL 时打印正在执行的 SQL。

**设计决策（内联，跳过独立 design.md）：** 在 `SqlLogicTestRunner` 注入一个 nullable `PrintStream traceStream`（setter），在 `runStatement` / `runQuery` 调用 executor 前，若 traceStream 非空则打印 SQL。CLI 层 `--trace` flag 经 `CliOptions` 传到 `FileRunner`，`FileRunner` 在构造 runner 后 `setTraceStream(options.trace() ? err : null)`。输出到 stderr（与既有错误诊断一致），不影响 stdout 报告。
