# Dev Notes: add-parallel-execution

## 实现摘要

- 在 CLI 层新增 `--parallel <N>` 文件级并行执行能力：`N >= 2` 时走 `CliSession.executeParallel()`，`N <= 1` 走原有顺序路径（零回归）
- 新增 `ParallelExecutor` 封装 `newFixedThreadPool`，提供 `submitAll` + `shutdown` / `awaitTermination`
- `--parallel` 与 `--override` 互斥（`CliArgumentParser.parse()` 阶段检测）
- 并行路径：worker 独立创建 `FileRunner` 实例，主线程按输入顺序聚合 `Future<FileOutcome>` 结果后串行输出
- `--halt` 在并行下：`Future.cancel(false)` 取消排队任务，已运行任务自然完成并报告
- 实现依赖：仅 `java.util.concurrent`，无变更 `FileRunner` / `ReportWriter` / `ConnectionFactory` / `PostgresSchemaIsolation`

## 变更路径

| 文件 | 新建/修改 | 说明 |
|---|---|---|
| `src/main/java/com/ggtest/cli/CliArgumentParser.java` | 修改 | T1/T6: `--parallel` case + 值校验 + `--override` 互斥检查 |
| `src/main/java/com/ggtest/cli/ParsedArguments.java` | 修改 | T1: 新增 `Optional<Integer> parallel` 字段 |
| `src/main/java/com/ggtest/cli/CliOptions.java` | 修改 | T1: 新增 `int parallel` 字段（默认 0）+ 向后兼容构造 |
| `src/main/java/com/ggtest/cli/RuntimeConfigResolver.java` | 修改 | T1: resolve() 透传 `parallel` |
| `src/main/java/com/ggtest/cli/Main.java` | 修改 | T8: printHelp() 新增 `--parallel <N>` 帮助行 |
| `src/main/java/com/ggtest/cli/CliSession.java` | 修改 | T3-T5: `execute()` 分支 + `executeParallel()` 方法 |
| `src/main/java/com/ggtest/cli/ParallelExecutor.java` | 新建 | T2: 线程池封装（package-private） |
| `src/test/java/com/ggtest/cli/CliArgumentParserTest.java` | 修改 | T9-1: 7 个新增测试（正整数/0/负数/非数字/missing value/互斥） |
| `src/test/java/com/ggtest/cli/RuntimeConfigResolverTest.java` | 修改 | T9-2: 4 个新增测试（透传/默认值/toString/环境隔离） |
| `src/test/java/com/ggtest/cli/MainOrchestrationTest.java` | 修改 | T9-3: 13 个新增测试（零回归/等价/报告完整/halt/故障隔离/凭据脱敏） |
| `src/test/java/com/ggtest/cli/PostgresCliIntegrationTest.java` | 修改 | T7: `parallelPostgresSchemaIsolation` 测试 + runPg 支持额外参数 |
| `README.md` | 修改 | T8: `--parallel` 选项说明 + 互斥标记 |
| `README.zh-CN.md` | 修改 | T8: `--parallel` 中文说明 + 互斥标记 |

## 验证

| 命令 | 结果摘要 / 证据 | 备注 |
|---|---|---|
| `mvn test` | **343 tests run, 0 failures, 17 skipped** (BUILD SUCCESS) | 17 skipped = 16 原有 PG + 1 新增 PG 并行测试（无 PG 环境） |
| `mvn test -Dtest=CliArgumentParserTest` | 24 tests, 0 failures | T1/T6: 含 7 个新增并行解析 + 互斥用例 |
| `mvn test -Dtest=MainOrchestrationTest` | 36 tests, 0 failures | 零回归通过（原 23 个不变）+ 13 个新增并行测试 |
| `mvn test -Dtest=RuntimeConfigResolverTest` | 34 tests, 0 failures | 含 4 个新增 parallel 透传测试 |
| `mvn test -Dtest=PostgresCliIntegrationTest` | 6 tests, 1 skipped | 1 skipped = `parallelPostgresSchemaIsolation`（Q-Note 缺 PG 环境）；其余 5 个通过 |

### 验收项对照

| ID | 验收项 | 状态 | 证据 |
|---|---|---|---|
| P0-1 | 零回归——不带 `--parallel` | Pass | 原有 23 个 MainOrchestrationTest 全部通过 |
| P0-2 | `--parallel 1` 等价于不带 | Pass | `parallel1IsEquivalentToSequential` 通过 |
| P0-3 | `--parallel 2` 多文件报告完整 | Pass | `parallel2MultiFileReportComplete` 通过 |
| P0-4 | `--parallel 0` → usage error | Pass | `parallelZeroYieldsUsageError` 通过 |
| P0-5 | `--parallel abc` → usage error | Pass | `parallelNonIntegerYieldsUsageError` 通过 |
| P0-6 | `--parallel N --override` → usage error | Pass | `parallelWithOverrideYieldsUsageError` 通过 |
| P0-7 | PG 引擎并行 schema 不冲突 | Q-Note | 无 PG 环境，测试已编写待环境就绪后验证 |
| P1-1 | 并行时 status line 按顺序输出 | Pass | `parallelStatusLineOrderMatchesSorterOutput` 通过 |
| P1-2 | `--parallel 2 --halt` 跳过未执行文件 | Pass | `parallelHaltSkipsQueuedFilesReportsRunningFiles` 通过 |
| P1-3 | 单 worker 异常不影响其他 worker | Pass | `parallelFaultIsolationSingleWorkerErrorDoesNotAffectOthers` 通过 |
| P1-4 | 凭据脱敏在并行输出中不变 | Pass | `parallelPasswordNeverPrinted` 通过 |
| P2-1 | 大语料多文件 wall-clock 加速 | Q-Note | 无专用大语料 fixture，逻辑正确性由 P0-3/P1-1 覆盖 |

## 文档影响

| 类别 | 已更新路径或交接说明 |
|---|---|
| 开发文档 | README.md / README.zh-CN.md：`--parallel` 选项说明与并行语义 + `--help` 行 |
| 用户文档 | 同上 README（用户可见 CLI 表面变更） |
| 运维文档 | N/A — 无部署/排障变更 |

## 未解决风险 / 验证缺口

| 项 | 原因 | 风险 | 恢复条件 |
|---|---|---|---|
| P0-7 PG 并行 schema 隔离 | 无 PG 环境（`GGTEST_PG_URL` 未设置） | 低 — `PostgresSchemaIsolation` 已单测验证线程安全（UUID schema 名）；测试代码已就绪 | 配置 PG 实例后执行 `mvn test -Dtest=PostgresCliIntegrationTest#parallelPostgresSchemaIsolation` |
| P2-1 大语料加速 | 无专用大语料 fixture | 低 — 逻辑正确性由 P0-3/P1-1 覆盖 | 准备大语料 fixture 后复测 |
| `TestFileCollector` 去重限制 | `LinkedHashSet` 按绝对路径去重，相同文件路径多次出现仅执行一次 | 低 — CLI 使用场景罕见；与顺序行为一致 | 不修复，属 Collect 层既有行为 |

## QA 修复回执

| 缺陷 ID | 处理 | 摘要 | 验证 | 建议复测 |
|---|---|---|---|---|
| | | | | |
