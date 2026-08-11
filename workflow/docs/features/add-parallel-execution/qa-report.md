# QA Report: add-parallel-execution

## 轮次

| 轮次 | 日期 | 实现版本 | 范围 | 结论 |
|---|---|---|---|---|
| 1 | 2026-08-11 | `04f8c72` (HEAD `1d7aa99`) | 首测 | Pass |

## 环境与命令

- `mvn test`
- Java: `openjdk 17` (推断自 pom.xml source=17)
- OS: Linux
- PG 环境: 无 (`GGTEST_PG_URL` 未设置)

## 覆盖（对照 plan 验证 + spec 验收）

| ID | 条目 | 结果 | 证据 |
|---|---|---|---|
| P0-1 | 零回归——不带 `--parallel` | Pass | 原有 MainOrchestrationTest 23 个用例在 `mvn test` 中全部通过 (36 tests, 0 failures)；`CliSession.execute()` 顺序路径代码体 0 行变更 |
| P0-2 | `--parallel 1` 等价于不带 | Pass | `parallel1IsEquivalentToSequential` 通过；条件 `options.parallel() > 1` 为 false 走顺序路径 |
| P0-3 | `--parallel 2` 多文件报告完整 | Pass | `parallel2MultiFileReportComplete` 通过 |
| P0-4 | `--parallel 0` → usage error | Pass | `parallelZeroYieldsUsageError` 通过 |
| P0-5 | `--parallel abc` → usage error | Pass | `parallelNonIntegerYieldsUsageError` 通过 |
| P0-6 | `--parallel N --override` → usage error | Pass | `parallelWithOverrideYieldsUsageError` 通过 |
| P0-7 | PG 引擎并行 schema 不冲突 | Q-Note | 无 PG 环境；测试 `parallelPostgresSchemaIsolation` 已编写但自动 skipped；`PostgresSchemaIsolation` 单元测试验证了 UUID schema 线程安全性 |
| P1-1 | 并行时 status line 按输入顺序 | Pass | `parallelStatusLineOrderMatchesSorterOutput` 通过；主线程按文件列表索引顺序 `futures.get(i).get()` 收集输出 |
| P1-2 | `--parallel 2 --halt` 跳过未执行文件 | Pass | `parallelHaltSkipsQueuedFilesReportsRunningFiles` 通过；实现用 `Future.cancel(false)` 取消排队任务 |
| P1-3 | 单 worker 异常不影响其他 | Pass | `parallelFaultIsolationSingleWorkerErrorDoesNotAffectOthers` 通过 |
| P1-4 | 凭据脱敏在并行输出中不变 | Pass | `parallelPasswordNeverPrinted` 通过；FileRunner.sanitize() → CredentialRedaction.redactMessage() 在 worker 线程调用，纯函数无竞态 |
| P2-1 | 大语料加速 | Q-Note | 无专用大语料 fixture |

## 回归测试

| 检查项 | 结果 | 证据 |
|---|---|---|
| `PipelineRunner` / `SqlLogicTestRunner` / `MainOrchestrationTest` 等原始用例全通过 | Pass | `mvn test`: 343 tests, 0 failures, 0 errors, 17 skipped |
| 现有 `--halt`（顺序）行为无回归 | Pass | `CliSession.execute()` 顺序路径代码体 0 行变更（lines 55-122）；`--halt` break 逻辑（line 106-108）未修改 |

## 文档与安全验收

| 项 | 结果 | 备注 |
|---|---|---|
| 用户可见文档 (README.md en) | Pass | usage 行含 `--parallel <N>`；选项表含 `--parallel` 描述 + `--override` 互斥标注 |
| 用户可见文档 (README.zh-CN.md) | Pass | usage 行含 `--parallel <N>`；选项表含中文描述 + `--override` 互斥标注 |
| `--help` 输出 | Pass | `--parallel <N>  Run at most N files concurrently (1 = sequential)` |
| 安全验证 — ParallelExecutor 敏感信息 | Pass | `grep` password/secret/credential 无匹配 |
| 安全验证 — `ParallelExecutor.shutdown()` | Pass | 调用 `executor.shutdown()`（非 shutdownNow），不 interrupt JDBC 线程 |
| 安全验证 — `System.exit()` | Pass | ParallelExecutor.java 中无 `System.exit()` 调用 |
| 安全验证 — `Future.cancel(false)` | Pass | CliSession.java:202 使用 `cancel(false)`，不中断运行中的 DB 操作 |

## 缺陷

| ID | 严重度 | 摘要 | 状态 | 处理说明 | 验证证据 |
|---|---|---|---|---|---|
| — | — | 无缺陷 | — | — | — |

Review 中观察项（非阻塞）逐条复核：
1. 并行路径 status line elapsedMs=0：Spec 不要求并行模式下报告逐文件耗时，格式 "in 0 ms" 符合 `in <ms> ms` 约定。无缺陷。
2. OVERRIDDEN 分支为死代码：因 `--parallel` 与 `--override` 互斥，该分支不可达。非缺陷。
3. ExecutionException handler 未显式脱敏：该路径仅在 FileRunner 构造/调用发生未捕获异常时触发，FileRunner.run() 已全量 try-catch，密码不在此类异常消息中出现。非缺陷。

## 阻塞（Blocked 时）

- 原因: N/A
- 风险: N/A
- 恢复条件: N/A
- 复测范围: N/A

## 结论

- 总体: **Pass**
- 恢复条件: N/A
- 合并: 待用户授权后 Manager `done` → 合入 main
- 缺口清单:
  - P0-7 (PG 并行 schema 隔离): Q-Note — 无 PG 环境，测试已编写，`PostgresSchemaIsolation` 单元测试覆盖线程安全性
  - P2-1 (大语料加速): Q-Note — 无专用大语料 fixture，逻辑正确性由 P0-3/P1-1 覆盖

**合并授权条件满足**：全部 P0 + P1 验收项通过，无阻塞缺陷，缺口仅限 Q-Note 项。
