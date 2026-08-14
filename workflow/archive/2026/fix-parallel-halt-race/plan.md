# Plan: fix-parallel-halt-race

## 元信息

- 工作项标识: fix-parallel-halt-race
- sub-feature-id: fix-parallel-halt-race（未拆分）
- 依据 Spec: N/A（合同继承 `add-parallel-execution/spec.md` §`--halt` 在并行下的语义 + 决策 #4，不新增行为）
- 依据 Design: workflow/archive/2026/fix-parallel-halt-race/design.md
- 依据 UI: N/A（无 UI 变更）
- 路径等级: standard
- Review 门禁: required
- 最低验证层: L3（单元/集成测试 + 稳定性复测）
- 验证命令: `mvn -q test`（全量）；`mvn -q -Dtest='MainOrchestrationTest#parallelHaltSkipsQueuedFilesReportsRunningFiles' test`（针对性，须连跑 ≥5 次稳定通过）
- 预期证据: 全量测试通过；目标用例反复 ≥5 次零失败；现有并行/顺序用例零回归

## 适用工程规范

- `workflow/agents/standards/documentation.md`
- `workflow/agents/standards/git.md`
- `workflow/agents/standards/quality.md`
- `workflow/agents/standards/security.md`

## 目标摘要

修复 `CliSession.executeParallel()` 的并发竞态：以受控分派（`ExecutorCompletionService` + lazy submit）取代 `submitAll + Future.cancel(false)`，使 `--parallel <N> --halt` 下「任一文件 FAILED → 停止派发未分派任务 → 等待已分派任务完成 → 仅报告已执行文件」确定性成立；消除 `MainOrchestrationTest.parallelHaltSkipsQueuedFilesReportsRunningFiles` 的 flaky 失败；顺序 `--halt` 与无 halt 并行行为零回归。

## 任务拆解

1. **T1: `ParallelExecutor` 接口调整**（完成条件：暴露 package-private `executor()`；移除 `submitAll` 且无编译/引用残留）
   - 新增 `ExecutorService executor()`（package-private getter，供 `CliSession` 包装为 `ExecutorCompletionService`）
   - 删除 `submitAll(List<Callable<T>>)`（改前 `grep` 确认仅 `CliSession.executeParallel` 引用）
   - 保留 `shutdown()` / `awaitTermination(long, TimeUnit)`

2. **T2: 重写 `CliSession.executeParallel()` 分派/收割**（完成条件：受控分派 N 个并发，错误隔离，结果按输入顺序聚合；无 halt 下多文件报告与现状一致）
   - `ExecutorCompletionService<TimedFileOutcome> ecs = new ExecutorCompletionService<>(executor.executor())`
   - `Deque<Integer> pending`（输入索引）+ `int running` + `boolean halted`
   - 派发循环：`while (running < N && !pending.isEmpty() && !halted) { ecs.submit(taskFor(pending.pollFirst())); running++; }`
   - 收割循环：`while (running > 0) { Future<TimedFileOutcome> f = ecs.take(); running--; TimedFileOutcome t = unwrap(f); results[idx]=t; if (!halted && options.halt() && t.outcome().bucket()==FAILED) halted=true; 派发循环(); }`
   - Callable 保留全量 try-catch → hardError `TimedFileOutcome`（沿用 add-parallel-execution 决策 6），`take().get()` 永不抛
   - 收割结束后按输入索引升序遍历 `results`，仅对非空下标输出（status line；FAILED 追加 error block）并累计计数

3. **T3: halt 停止派发 + 聚合输出**（完成条件：FAILED 触发 halt 后不再 submit；已分派任务继续完成并按真实桶报告；未分派文件不出现在 stdout、不计入 TOTAL）
   - `halted=true` 仅用于停止派发循环，**不**对已 submit 的 future 调用任何 cancel（满足「等待运行中 DB 操作完成」）
   - 计数/退出码逻辑与顺序路径一致（hardError → 2；failed>0 → 1；else 0）
   - `printErrorSection` / `printTrailingBlankIfNeeded` / `printTotal` 复用现有 `reportWriter`，数据源改为按索引遍历

4. **T4: 新增 fixture + 改 `parallelHaltSkipsQueuedFilesReportsRunningFiles`**（完成条件：该用例确定地通过——`1-parse-error` FAILED、`2-pass` PASSED、`3-queued` 不出现，passed=1 failed=1 exit=2）
   - 新建 `src/test/resources/fixtures/cli/parallel-halt/1-parse-error.test`（单行非法记录 → parse 即 hardFailure，无 DB 工作，~µs 完成）
   - 新建 `src/test/resources/fixtures/cli/parallel-halt/2-pass.test`（合法通过；dispatched，运行中）
   - 新建 `src/test/resources/fixtures/cli/parallel-halt/3-queued.test`（合法通过；pending，应永不执行）
   - 修改测试：传三个 fixture + `--parallel 2 --halt`；断言更新为上述确定结果（exit=2，因触发器为 hardError）
   - 理据：实测 SQLite 首批文件 ~200ms 共享预热淹没 op 差异，op 数无法可靠拉开完成顺序；改用「无 DB 工作的失败文件」确定先于任何 DB 文件完成（详见 design.md 决策 5）

5. **T5: 全量回归**（完成条件：`mvn -q test` 通过；现有并行用例 `parallel1IsEquivalentToSequential` / `parallel2MultiFileReportComplete` / `parallelStatusLineOrderMatchesSorterOutput` / `parallelFaultIsolationSingleWorkerErrorDoesNotAffectOthers` / `parallelPasswordNeverPrinted` 等零回归；顺序 `--halt` 用例 `haltStopsAfterFirstFailingFileAndDoesNotStartLaterFiles` / `haltWithHardErrorExitsTwoAndDoesNotStartLaterFiles` 零回归）
   - 目标用例连跑 ≥5 次确认稳定
   - 复核无对 `submitAll` 的残留引用

## 依赖与顺序

```
T1 (ParallelExecutor 接口) ─→ T2 (executeParallel 受控分派)
                                ├─→ T3 (halt 停止派发 + 聚合输出)
                                │     └─→ T4 (fixture + 测试)
                                │           └─→ T5 (全量回归)
                                └─ (T2/T3 可在同一重写中完成)
```

## 触碰路径

| 文件 | 新建/修改 | 改动类型 |
|---|---|---|
| `src/main/java/com/ggtest/cli/ParallelExecutor.java` | 修改 | 暴露 `executor()`；移除 `submitAll` |
| `src/main/java/com/ggtest/cli/CliSession.java` | 修改 | 重写 `executeParallel()`（受控分派 + halt + 按序聚合）；`execute()` 顺序路径不动 |
| `src/test/resources/fixtures/cli/parallel-halt/1-parse-error.test` | 新建 | 即时失败（parse，无 DB） |
| `src/test/resources/fixtures/cli/parallel-halt/2-pass.test` | 新建 | 合法通过（dispatched，运行中） |
| `src/test/resources/fixtures/cli/parallel-halt/3-queued.test` | 新建 | 合法通过（应被跳过） |
| `src/test/java/com/ggtest/cli/MainOrchestrationTest.java` | 修改 | 改 `parallelHaltSkipsQueuedFilesReportsRunningFiles` 的 fixture 与断言 |

> 明确**不触碰**：`CliSession.execute()`（顺序路径）、`FileRunner`、`ReportWriter`、`ConnectionFactory`、`PostgresSchemaIsolation`、CLI 解析链（`CliArgumentParser`/`ParsedArguments`/`CliOptions`/`RuntimeConfigResolver`/`Main`）。

## 验收与验证

| ID | 要求或命令 | 预期证据 | 结果（实施后填） |
|---|---|---|---|
| V1 | `mvn -q -Dtest='MainOrchestrationTest#parallelHaltSkipsQueuedFilesReportsRunningFiles' test` 连跑 ≥5 次 | 每次均通过；stdout 确定含 `1-parse-error` `[FAILED]` 与 `2-pass` `[PASSED]`，不含 `3-queued`；passed=1 failed=1 exit=2 | |
| V2 | `mvn -q test`（全量） | 全部通过 | |
| V3 | 零回归：顺序 `--halt` 用例（`haltStopsAfterFirstFailingFileAndDoesNotStartLaterFiles`、`haltWithHardErrorExitsTwoAndDoesNotStartLaterFiles`、`corpusHaltRecordDoesNotTriggerCliHalt`） | 通过；行为不变 | |
| V4 | 零回归：无 halt 并行用例（`parallel1IsEquivalentToSequential`、`parallel2MultiFileReportComplete`、`parallelStatusLineOrderMatchesSorterOutput`、`parallelFaultIsolationSingleWorkerErrorDoesNotAffectOthers`、`parallelPasswordNeverPrinted` 等） | 通过；报告完整、顺序正确 | |
| V5 | Review 门禁（required）Approve | `review.md` 结论 Approve | |
| V6 | QA 验收 Pass | `qa-report.md` 结论 Pass（含 V1 ≥5 次复测） | |

## 验证缺口

| 项 | 原因 | 风险 | 恢复条件 |
|---|---|---|---|
| 多机/多 CI runner 时序复测 | 本地与 CI 单次跑无法穷尽所有调度时序 | 低——受控分派已从结构上消除 worker 自主抽干队列的竞态；失败文件用 parse error（无 DB 工作）确定先于任何 DB 文件完成，完成顺序与时序/机器无关 | 如未来再现 flake，复查是否有新增 DB-free 快速失败路径 |

## 文档影响

| 类别 | 更新路径或 N/A 理由 |
|---|---|
| 开发文档 | 本切片 `dev-notes.md` 记录「受控分派 取代 add-parallel-execution/design.md 决策 5（submitAll + cancel(false)）」的实现差异与理由 |
| 用户文档 | N/A——`--halt` 面向用户的可观察语义不变（只是从「竞态下偶尔违反」收紧为「始终符合既有 spec」），README 无需改动 |
| 运维文档 | N/A——无部署/排障变更 |

## Review 门禁与进入 QA 条件

- Review 门禁：`required`（standard 路径）。Developer 完成且 V1-V4 通过后调度 Reviewer；取得 Approve 方可进 QA。
- 进入 QA 条件：Review Approve + 全量 `mvn -q test` 通过 + 目标用例 ≥5 次稳定通过。

## 交接顺序

1. Developer 实施与开发者验证（V1-V4 自测） →
2. Reviewer 审阅（Review 门禁 required）→ Approve →
3. QA 验收（V1-V6；含目标用例 ≥5 次复测） → Pass →
4. 用户合并授权 → Manager 源分支置 `done` 并与未入库 `review.md`/`qa-report.md` 一次提交 → 合入 `main`

## 修订记录

| 日期 | 摘要 |
|---|---|
| 2026-08-12 | 初稿：基于 Design v1.0，任务拆解 T1-T5；Spec skipped（合同继承 add-parallel-execution） |
