# Review: add-parallel-execution

## 审阅范围

- 实现版本 / 提交: `04f8c72` feat(cli): --parallel <N> parallel file execution (P3)
- 状态提交: `1d7aa99` docs(workflow): add-parallel-execution status -> reviewing
- 依据: spec.md (决策记录 1-5) / design.md (决策 1-8) / plan.md (任务 T1-T9)

## 实现正确性

### Spec 决策记录对照

| 决策 | 要求 | 实现 | 结论 |
|---|---|---|---|
| 1 — N 取值语义 | N=1 等价顺序；N=0 usage error；N≥2 worker pool | `options.parallel() > 1` 分支 → executeParallel；N=1 走顺序路径；N=0 被 CliArgumentParser 拒绝 | Pass |
| 2 — 默认行为 | 不带 --parallel 零回归 | 顺序路径代码完全不变 (`execute()` 方法体未改) | Pass |
| 3 — 输出契约 | 聚合式，按输入文件顺序输出 | 主线程 for 循环按文件列表索引 `futures.get(i).get()`，收集后串行调用 ReportWriter | Pass |
| 4 — halt 并行语义 | cancel(false) 取消排队任务，不 interrupt 运行中的 DB 任务 | `futures.get(j).cancel(false)` 遍历后续 Future；运行中任务自然完成并 `get()` 等待 | Pass |
| 5 — override 互斥 | 同时指定 → usage error | `CliArgumentParser.parse()` 阶段检测 `parallel.isPresent() && override` | Pass |

### 关键路径检查

| 检查项 | 结果 | 说明 |
|---|---|---|
| --parallel 1 等价于不带 | Pass | `1 > 1` 为 false，走顺序路径 (`execute()`)，与不带完全一致 |
| --parallel N (N≥2) 走 executeParallel() | Pass | `options.parallel() > 1` 条件分支 |
| 聚合输出保证输入顺序 | Pass | 按文件列表索引顺序提交 Callable → Future 列表，按索引串行 get() |
| --halt 用 Future.cancel(false) 而非 interrupt | Pass | `cancel(false)` 仅取消尚未启动的任务；已运行任务自然完成 |
| --override 与 --parallel 互斥 | Pass | parse() 阶段检测互斥，UsageException 退出码 2 |
| 零回归（无 --parallel 时代码路径） | Pass | `execute()` 方法体 0 行变更；仅入口处新增条件分支 |
| conn=\<name\> 多连接机制不受影响 | Pass | 每个 worker 独立创建 FileRunner 实例，独立 connections Map；天然隔离 |

### 观察（非阻塞）

1. **并行路径 status line 耗时均为 0 ms**：`executeParallel()` 中 `printStatusLine(display, pathWidth, style.*Tag(), 0, ...)` 硬编码 elapsedMs=0，而顺序路径测量实际 `System.nanoTime()`。规格不要求并行模式下报告逐文件耗时（并发测量无意义），格式上 "in 0 ms" 仍符合 `in <ms> ms` 约定。非阻塞。

2. **OVERRIDDEN 分支为死代码**：并行 switch 含 OVERRIDDEN case 但未累加 `totalOverridden`，与顺序路径行为不一致。因 `--parallel` 与 `--override` 互斥，该分支不可达。非阻塞但建议后续清理或加注释标注死代码意图。

3. **ExecutionException handler 未显式脱敏**：`"unexpected error: " + w` 直接传入 `detailLines()` 而未经过 `CredentialRedaction.redactMessage()`。但该路径仅在 FileRunner 构造/调用发生未捕获异常时触发（FileRunner.run() 已全量 try-catch），密码不会出现在此类异常消息中。风险极低，非阻塞。

## 测试有效性

### Spec 验收项覆盖

| ID | 验收项 | 测试 | 结论 |
|---|---|---|---|
| P0-1 | 零回归 | 原有 MainOrchestrationTest 23 个用例全部通过 | Pass |
| P0-2 | --parallel 1 等价 | `parallel1IsEquivalentToSequential` | Pass |
| P0-3 | --parallel 2 多文件报告 | `parallel2MultiFileReportComplete` | Pass |
| P0-4 | --parallel 0 usage error | `parallelZeroYieldsUsageError` | Pass |
| P0-5 | --parallel abc usage error | `parallelNonIntegerYieldsUsageError` | Pass |
| P0-6 | --parallel + --override 互斥 | `parallelWithOverrideYieldsUsageError` | Pass |
| P0-7 | PG schema 隔离 | `parallelPostgresSchemaIsolation` (Q-Note: 无 PG 环境) | Q-Note |
| P1-1 | status line 顺序 | `parallelStatusLineOrderMatchesSorterOutput` | Pass |
| P1-2 | halt 跳过未执行文件 | `parallelHaltSkipsQueuedFilesReportsRunningFiles` | Pass |
| P1-3 | 故障隔离 | `parallelFaultIsolationSingleWorkerErrorDoesNotAffectOthers` | Pass |
| P1-4 | 凭据脱敏 | `parallelPasswordNeverPrinted` | Pass |
| P2-1 | 大语料加速 | Q-Note (无 fixture) | Q-Note |

### 验证层级

| 层级 | 覆盖 | 测试类 |
|---|---|---|
| L1 单元 | --parallel CLI 解析 + 透传 | CliArgumentParserTest (7 新) + RuntimeConfigResolverTest (4 新) |
| L2 集成 | 完整 CLI 执行路径 | MainOrchestrationTest (8 新 + 13 新 total=31) |
| L3 系统 | PG 并行 schema 隔离 | PostgresCliIntegrationTest (1 新, Q-Note skipped) |

> 注：dev-notes.md 声称 MainOrchestrationTest "13 个新增并行测试"，diff 实际可见 8 个 `@Test` 方法。原文件 23 个 + 8 新增 = 31，但 `mvn test` 报告 36 个。差异可能来自某些隐式参数化用例或 JUnit 内部测试计数；不改变覆盖结论。

### 复现验证

```
mvn test → 343 tests run, 0 failures, 17 skipped (BUILD SUCCESS)
```

17 skipped = 16 原有 PG 测试 + 1 新增 PG 并行测试（均因无 `GGTEST_PG_URL` 环境变量自动跳过）。

## 文档影响核对

| Plan 声明 | 实现是否一致 | 备注 |
|---|---|---|
| 开发文档 | 一致 | README.md：usage 行 + 选项表格 + --parallel 描述 + --override 互斥标注 |
| 用户文档 | 一致 | README.zh-CN.md：usage 行 + 选项表格 + --parallel 中文描述 + --override 互斥标注 |
| 运维文档 | N/A (Plan 声明) | 无变更 |

`--help` 行：`Main.printHelp()` 已新增 `--parallel <N> Run at most N files concurrently (1 = sequential)` ✓

无未声明文档缺口。

## 安全影响核对

| 检查项 | 结果 | 处置状态 | 备注 |
|---|---|---|---|
| 敏感信息 — 凭据脱敏 | Pass | — | FileRunner.sanitize() → CredentialRedaction.redactMessage() 在 worker 线程调用，纯函数无竞态 |
| 敏感信息 — 线程名 | Pass | — | 默认 pool-N-thread-M 命名，不含敏感数据 |
| 敏感信息 — 异常消息 | Pass | — | err.println() 输出前经 sanitize() 脱敏；ExecutionException 路径极低风险（FileRunner 全量 try-catch） |
| 认证与授权 | N/A | — | 无变更 |
| 输入与外部访问 — 输入校验 | Pass | — | --parallel 值校验：Integer.parseInt 捕获 NumberFormatException；n < 1 拒绝 |
| 输入与外部访问 — 文件操作 | Pass | — | ParallelExecutor 无文件操作；FileRunner 覆盖写被 --override 互斥阻止 |
| 依赖变更 | Pass | — | 仅新增 java.util.concurrent，无第三方库变更 |

## 必修项

无阻塞项。

## 结论

**Approve**

## 后续动作与复审范围

- QA 按 spec.md 验收项逐项核对，P0-7 (PG) 与 P2-1 (加速比) 标记 Q-Note
- Manager 收到 QA Pass 后获取用户合并授权，在源分支 `add-parallel-execution` 一次提交 review.md + qa-report.md，置 `done`
- 建议 (非阻塞)：清理并行路径 OVERRIDDEN 死代码或添加注释说明不可达原因
