# QA Report: fix-parallel-halt-race

## 验收范围

- 验收对象: 实现 `e48e0c9`（源分支 `fix-parallel-halt-race`）
- 依据: `plan.md` V1-V6；合同继承 `add-parallel-execution/spec.md` 决策 #4 / P1-2
- Review: `review.md` Approve
- 环境: 本地 Linux；SQLite（`:memory:`）；无 PG（PG 用例自动 skip）

## 第 1 轮：验收

| ID | 要求 | 命令 / 证据 | 结果 |
|---|---|---|---|
| V1 | 目标用例确定通过（连跑 ≥5 次） | `mvn -q -Dtest='MainOrchestrationTest#parallelHaltSkipsQueuedFilesReportsRunningFiles' test` → **5/5 通过** | Pass |
| V1-输出 | stdout 确定含 `1-parse-error [FAILED]` + `2-pass [PASSED]`、不含 `3-queued`、passed=1 failed=1、exit=2 | CLI 直跑复核（见 review.md §验证证据 / dev-notes）：`1-parse-error .. [FAILED] in 14ms`（无 DB）→ `2-pass .. [PASSED] in 152ms`（预热）；`3-queued` 缺席；`TOTAL: passed=1 failed=1 skipped=0`；exit=2 | Pass |
| V2 | 全量回归 | `mvn test` → **Tests run: 343, Failures: 0, Errors: 0, Skipped: 17**；BUILD SUCCESS | Pass |
| V3 | 顺序 `--halt` 零回归 | `haltStopsAfterFirstFailingFileAndDoesNotStartLaterFiles`、`haltWithHardErrorExitsTwoAndDoesNotStartLaterFiles`、`corpusHaltRecordDoesNotTriggerCliHalt`（含于 V2 全量） | Pass |
| V4 | 无 halt 并行零回归 | `parallel1IsEquivalentToSequential`、`parallel2MultiFileReportComplete`、`parallelStatusLineOrderMatchesSorterOutput`、`parallelFaultIsolationSingleWorkerErrorDoesNotAffectOthers`、`parallelPasswordNeverPrinted` 等（含于 V2 全量） | Pass |
| V5 | Review Approve | `review.md` 结论 Approve | Pass |
| V6 | QA 验收 | 本报告 | Pass |

### Spec P1-2 合同核对（并行 `--halt`）

| 合同 | 证据 | 结果 |
|---|---|---|
| 任一文件 FAILED → 取消未分派任务 | `3-queued` 不在 stdout、不计入 TOTAL（V1-输出） | Pass |
| 等待已分派任务完成（不 interrupt） | `2-pass` 被报告为真实 `[PASSED]`（V1-输出）；实现无 cancel/shutdownNow | Pass |
| 仅报告已执行文件 | 仅 `1-parse-error` + `2-pass` 出现，`3-queued` 缺席 | Pass |
| 退出码优先级不变 | hardError（parse）→ exit 2 | Pass |

### 确定性论证复核

失败文件 `1-parse-error` 无 DB 工作(parse 即 hardFailure)，实测 14ms vs 2-pass 152ms，完成顺序由「有无 DB 工作」决定，与机器/JVM 预热/调度时序无关——flaky 根因（worker 自主抽干队列的竞态）已由受控分派从结构上消除。dev-notes 记录的「op 数无法克服 ~200ms 共享预热」实测数据与本轮观察一致。

## 缺陷

无。

## 验证缺口

| 项 | 原因 | 风险 | 恢复条件 |
|---|---|---|---|
| 多 CI runner / 多机时序穷尽复测 | 单环境单次跑无法穷尽调度时序 | 低——确定性不依赖时序（parse error 无 DB 工作必先完成；受控分派消除队列抽干竞态） | 如未来再现，复查是否引入新的 DB-free 快速失败路径 |

## 结论

**Pass**（第 1 轮）

建议合并授权后，Manager 在源分支 `fix-parallel-halt-race` 将 STATUS→`done` 与 `review.md`/`qa-report.md` 一次提交，再合入 `main`。

---

## 第 2 轮：按序流式输出复审测

- 触发：第 1 轮 Pass 后，用户实测 `--parallel 10 ./sqllogictest/test/`（622 文件）发现全程无输出（end-dump）→ 回 `developing` → `49b1674` 恢复 in-order streaming → 复审 Approve（见 review.md 复审 round 2）。
- 复测对象：`49b1674`（源分支 `fix-parallel-halt-race`）。

| 项 | 命令 / 证据 | 结果 |
|---|---|---|
| 目标用例稳定 | `mvn -q -Dtest='MainOrchestrationTest#parallelHaltSkipsQueuedFilesReportsRunningFiles'` → 5/5 | Pass |
| 全量回归 | `mvn test` → 343 / 0 失败 / 0 错误 / 17 skipped；BUILD SUCCESS | Pass |
| 流式输出（用户场景） | `--parallel 4 ./sqllogictest/test/evidence/`（12 文件）：status line 按完成+顺序在 +262ms / +266ms / +270ms … 陆续出现，非末尾一次性；首批慢文件完成后快文件立即流式 | Pass |
| halt-skip 不变 | 目标用例 stdout 仍确定含 `1-parse-error [FAILED]` + `2-pass [PASSED]`、不含 `3-queued`、passed=1 failed=1 exit=2 | Pass |

### 缺陷

无（第 1 轮的 end-dump UX 问题已在 `49b1674` 修复并复测）。

## 最终结论

**Pass**
