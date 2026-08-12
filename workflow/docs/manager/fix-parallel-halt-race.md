# 工作项记录: fix-parallel-halt-race

工作项标识: fix-parallel-halt-race
描述: 修复 `--parallel <N> --halt` 并发竞态，使「任一文件 FAILED → 取消已提交但未分派的任务 → 等待运行中任务完成 → 仅报告已执行文件」确定性成立；消除 `MainOrchestrationTest.parallelHaltSkipsQueuedFilesReportsRunningFiles` 的 flaky 失败。
目标分支: main
文档影响: 受影响——`add-parallel-execution` 的 design.md 决策 5（`submitAll + Future.cancel(false)`）将被取代为本切片 design 的受控分派；README 无面向用户语义变化（`--halt` 可观察行为收紧为符合既有 spec）。本切片 dev-notes 记录实现差异。

> 权威流程见 [workflow/README.md](../../README.md)；活跃状态见 [STATUS.md](STATUS.md)。
>
> **切片级：** 路径等级、源分支、门禁、状态、阻塞。**工作项级：** 目标分支。
> 未拆分：产物在 `workflow/docs/features/<feature-id>/`。已拆分：根目录仅总览 Spec；切片在 `<feature-id>-<sub>/`。
> 归档后本文件迁至 `workflow/docs/archive/YYYY/<feature-id>/manager.md`（须修正相对链接）；`workflow/docs/manager/` 仅保留活跃项与 STATUS。
>
> 表内只填枚举、短标签或链接；较长理由写入「进度笔记」（见 `workflow/docs/standards/documentation.md` §B）。

## 切片门禁（未拆分时一行，sub-feature-id = feature-id）

| sub-feature-id | 路径等级 | 源分支 | Spec | Spec 门禁 | Spec 用户确认 | Design 门禁 | Review 门禁 |
|---|---|---|---|---|---|---|---|
| fix-parallel-halt-race | standard | fix-parallel-halt-race | N/A（合同见 add-parallel-execution/spec.md §halt + 决策 #4） | skipped | N/A | required | required |

> 总览行：路径等级与门禁、源分支均可 `N/A`。`Review=skipped` 仅 `fast`；理由写进度笔记。

## 切片状态

| sub-feature-id | 状态 | 后续步骤 | 阻塞原因 | 恢复条件 | 恢复后目标 |
|---|---|---|---|---|---|
| fix-parallel-halt-race | done | — | | | |

> 无阻塞则后三列留空。长说明优先进度笔记。

## 进度笔记

### 登记背景（2026-08-12）

CI 红：`MainOrchestrationTest.parallelHaltSkipsQueuedFilesReportsRunningFiles` flaky。已在 `df43b2a`（grammar sync 之前）复现同一失败，确认为**先存在的缺陷**，非回归。

**根因（`CliSession.executeParallel` lines 160-215）：** 实现用 `submitAll` 一次性把全部任务塞进 `newFixedThreadPool(N)` 的无界队列，主循环按提交顺序 `future.get()`，遇 FAILED 才 `cancel(false)` 后续 future。但「`cancel(false)`」与「worker 从队列捞任务」竞态：

- 快机器（CI）：`multi-fail` ~1ms 完成 → 主循环立刻 cancel，此时其余 worker 线程尚未真正开始执行其 firstTask → 连带已分派文件一并取消 → 只报告失败文件（`passed=0`）→ 断言 `contains("a.test")` 失败（line 661）。
- 慢机器（本地）：worker 在 cancel 前捞起全部任务 → 三个文件全跑全报告（`passed=2`）→ `assertEquals(1, extractPassed)` 失败（line 663）。

两个方向均违反 `add-parallel-execution/spec.md` 决策 #4 与 P1-2：过度取消已分派任务 / 欠取消排队任务。

**路径判定：** standard。动 orchestration 并发核心；Design required（分派机制选型）；Review required。Spec skipped——不新增行为合同，仅让既有合同（`add-parallel-execution/spec.md` §halt + 决策 #4）确定性成立。

**修复方向（待 Design 细化）：** 以受控分派取代 `submitAll`（lazy submit / `ExecutorCompletionService` + halt 即停止派发），使「未分派」边界确定；并调整测试 fixture 使失败文件 reliably 先于并发文件完成，从而「排队文件被跳过」可确定地验证。现有顺序 `--halt` 与 `--parallel`（无 halt）行为须零回归。
