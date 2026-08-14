# 工作项: add-trace-flag

描述: 新增 `--trace` CLI 标志，执行 SQL 语句时实时打印正在执行的 SQL 到 stderr（诊断用途，不影响 stdout 报告）。
目标分支: main
源分支: add-trace-flag
基线提交: a6c8719bc48099cf772a6bd1807876dd4577259c
文档影响: README（`--trace` 选项说明）、`--help`。

> **本文件须保存为 `workflow/archive/2026/add-trace-flag/add-trace-flag.md`**，文件名与目录同名。
> 流程定义见 `workflow/WORKFLOW.md`；看板见 `workflow/STATUS.md`。
> 本工作项的全部产物平铺在 `workflow/archive/2026/add-trace-flag/`，无子目录、无版本后缀。
> 表内只填枚举、短标签或路径；理由与长说明写进「进度笔记」。

## 门禁

| 路径等级 | Spec | Spec 用户确认 | Design | Review |
|---|---|---|---|---|
| standard | skipped | not-required | skipped | required |

## 状态

| 状态 | 下一步 | 阻塞原因 | 恢复条件 | 恢复后目标 |
|---|---|---|---|---|
| archived | — |  |  |  |

## 子项（仅 tracking 项填写）

| 子项 id | 状态 |
|---|---|
| — | |

## 进度笔记

### 登记背景（2026-08-12）

用户要求实现 `--trace` 功能：运行 SQL 时打印正在执行的 SQL。

**设计决策（内联，跳过独立 design.md）：** 在 `SqlLogicTestRunner` 注入一个 nullable `PrintStream traceStream`（setter），在 `runStatement` / `runQuery` 调用 executor 前，若 traceStream 非空则打印 SQL。CLI 层 `--trace` flag 经 `CliOptions` 传到 `FileRunner`，`FileRunner` 在构造 runner 后 `setTraceStream(options.trace() ? err : null)`。输出到 stderr（与既有错误诊断一致），不影响 stdout 报告。
- 2026-08-14：按 ggnote `WORKFLOW.md` 标准迁移工作流目录（记录与产物合并为同一目录；权威文件改为 `workflow/WORKFLOW.md`）。
