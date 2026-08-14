# 工作项: fix-parallel-halt-race

描述: 修复 `--parallel <N> --halt` 并发竞态，使「任一文件 FAILED → 取消已提交但未分派的任务 → 等待运行中任务完成 → 仅报告已执行文件」确定性成立；消除 `MainOrchestrationTest.parallelHaltSkipsQueuedFilesReportsRunningFiles` 的 flaky 失败。
目标分支: main
源分支: fix-parallel-halt-race
基线提交: a6c8719bc48099cf772a6bd1807876dd4577259c
文档影响: 受影响——`add-parallel-execution` 的 design.md 决策 5（`submitAll + Future.cancel(false)`）将被取代为本切片 design 的受控分派；README 无面向用户语义变化（`--halt` 可观察行为收紧为符合既有 spec）。本切片 dev-notes 记录实现差异。

> **本文件须保存为 `workflow/archive/2026/fix-parallel-halt-race/fix-parallel-halt-race.md`**，文件名与目录同名。
> 流程定义见 `workflow/WORKFLOW.md`；看板见 `workflow/STATUS.md`。
> 本工作项的全部产物平铺在 `workflow/archive/2026/fix-parallel-halt-race/`，无子目录、无版本后缀。
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

CI 红：`MainOrchestrationTest.parallelHaltSkipsQueuedFilesReportsRunningFiles` flaky。已在 `df43b2a`（grammar sync 之前）复现同一失败，确认为**先存在的缺陷**，非回归。

**根因（`CliSession.executeParallel` lines 160-215）：** 实现用 `submitAll` 一次性把全部任务塞进 `newFixedThreadPool(N)` 的无界队列，主循环按提交顺序 `future.get()`，遇 FAILED 才 `cancel(false)` 后续 future。但「`cancel(false)`」与「worker 从队列捞任务」竞态：

- 快机器（CI）：`multi-fail` ~1ms 完成 → 主循环立刻 cancel，此时其余 worker 线程尚未真正开始执行其 firstTask → 连带已分派文件一并取消 → 只报告失败文件（`passed=0`）→ 断言 `contains("a.test")` 失败（line 661）。
- 慢机器（本地）：worker 在 cancel 前捞起全部任务 → 三个文件全跑全报告（`passed=2`）→ `assertEquals(1, extractPassed)` 失败（line 663）。

两个方向均违反 `add-parallel-execution/spec.md` 决策 #4 与 P1-2：过度取消已分派任务 / 欠取消排队任务。

**路径判定：** standard。动 orchestration 并发核心；Design required（分派机制选型）；Review required。Spec skipped——不新增行为合同，仅让既有合同（`add-parallel-execution/spec.md` §halt + 决策 #4）确定性成立。

**修复方向（待 Design 细化）：** 以受控分派取代 `submitAll`（lazy submit / `ExecutorCompletionService` + halt 即停止派发），使「未分派」边界确定；并调整测试 fixture 使失败文件 reliably 先于并发文件完成，从而「排队文件被跳过」可确定地验证。现有顺序 `--halt` 与 `--parallel`（无 halt）行为须零回归。
- 2026-08-14：按 ggnote `WORKFLOW.md` 标准迁移工作流目录（记录与产物合并为同一目录；权威文件改为 `workflow/WORKFLOW.md`）。
