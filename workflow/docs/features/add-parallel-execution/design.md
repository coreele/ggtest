# Design: add-parallel-execution

## 背景与约束

- 现状 `CliSession.execute()` (`src/main/java/com/ggtest/cli/CliSession.java:46-114`) 对文件列表顺序 for 循环执行，大语料场景 wall-clock 时间为各文件执行时间之和。
- 新增 `--parallel <N>` 允许最多 N 个文件并发执行，在 PG schema 隔离、SQLite `:memory:` 隔离的保障下减少总耗时。
- Spec 已确认不可变更合同（见 `spec.md` §决策记录）：聚合式输出、`--halt` 取消语义、`--override` 互斥、N=1 等价顺序。
- 约束：`--parallel <N>` 不能 break 现有顺序执行行为（零回归）、不能 interrupt 运行中的 DB 操作、输出顺序必须与输入文件顺序一致。

## 方案对比与决策

### 决策 1: 并发模型选型

| 方案 | 概要 | 优点 | 缺点 | 比较依据 |
|---|---|---|---|---|
| A | `Executors.newFixedThreadPool(N)` | 资源可控，线程上限 = N，恰好映射 `--parallel N`；任务提交→Future 排序天然支持顺序收集 | 无 | N 即 pool size；`--halt` 需 `Future.cancel(false)`；shutdown 语义明确 |
| B | `Executors.newCachedThreadPool()` | 弹性伸缩 | 线程数不可控；无天然上限 | 无法满足 `最多 N 个文件同时执行` 语义 |
| C | 手动 `Thread` | 无 | 无任务队列/取消支持；资源管理手工 | 工程不可接受 |

**决策:** 选 A。`newFixedThreadPool(N)` 将 `--parallel N` 直接映射为 pool size；`Future.cancel(false)` 取消排队中任务而不中断运行中线程，满足 Spec `--halt` 语义。shutdown 流程：`shutdown()` 后 `awaitTermination`（不强行 `shutdownNow`）。

### 决策 2: 结果聚合模型

| 方案 | 概要 | 优点 | 缺点 | 比较依据 |
|---|---|---|---|---|
| A | `List<Future<FileOutcome>>` 保持输入顺序索引 → 按序 get() | 天然顺序；简洁 | get() 阻塞等待每个未完成 Future | Spec 要求输出按输入文件顺序；错误隔离（单 Future 异常不影响其他） |
| B | `ConcurrentLinkedQueue` 即到即取 | 先完成先获得 | 无法保证输入顺序 | Spec P1-1 明确要求顺序输出，不满足 |
| C | `CompletableFuture.allOf` + 回调 | 灵活 | 过度设计 | 无额外需求 |

**决策:** 选 A。按文件列表顺序创建 Future，按序 get() 收集 `FileOutcome`，确保输出顺序与输入一致。即使 worker 内部抛异常也已包装为 hardError `FileOutcome`（见决策 6），get() 不会抛 `ExecutionException`。

### 决策 3: FileRunner 线程安全

| 方案 | 概要 | 优点 | 缺点 | 比较依据 |
|---|---|---|---|---|
| A | 每个 worker 创建独立 FileRunner 实例 | 最简单；无共享可变状态；构造函数参数不变 | 多实例轻微开销（可忽略） | 每个 worker 有独立的 connections Map、独立的 OverrideCoordinator，零共享 |
| B | 共享单一 FileRunner 实例 | 少创建对象 | `run()` 内状态虽是栈局部，但实例字段（如 `OverrideCoordinator`）带来未来维护风险 | `--parallel --override` 已被 Spec 禁用，但方案 A 更安全，避免维护者误加共享状态 |

**决策:** 选 A。每个 worker 线程 `new FileRunner(options, err, reportWriter)`，构造函数参数不可变引用、零共享。`OverrideCoordinator` 不参与并行路径（Spec 禁用 `--parallel --override`），但独立实例消除潜在风险。

### 决策 4: 输出线程安全

| 方案 | 概要 | 优点 | 缺点 | 比较依据 |
|---|---|---|---|---|
| A | 聚合模式：worker 只返回 `FileOutcome`，主线程聚合后按序输出 | 完全无竞态；100% 保证顺序 | 需将 CliSession 的"执行"与"输出"解耦 | Spec 要求聚合式输出，是唯一满足顺序与零交错要求的方案 |
| B | worker 直接 `synchronized(PrintStream out)` 输出 | 实现简单 | 输出顺序不可控（按完成时间乱序） | Spec 明确要求顺序输出，不满足 |

**决策:** 选 A。并行执行阶段 worker 仅创建 `FileOutcome` 数据对象，不触碰 ReportWriter/PrintStream。所有 worker 完成后，主线程按输入文件顺序遍历 `Future<FileOutcome>` 结果，调用 ReportWriter 输出——与 CliSession 现有输出逻辑相同，仅执行部分从顺序 for 改为并行提交。

### 决策 5: --halt 取消机制

| 方案 | 概要 | 优点 | 缺点 | 比较依据 |
|---|---|---|---|---|
| A | `Future.cancel(false)` | 取消排队中任务，不中断运行中任务；JDK 标准行为 | 无 | Spec 明确"不 interrupt 运行中的 DB 操作" |
| B | 共享 `volatile boolean halted` + FileRunner 检查 | 可精细控制 | 需侵入 FileRunner | 不必要 |
| C | `ExecutorService.shutdownNow()` | 简单 | 会 interrupt 所有 running 线程（线程池级 shutdown） | 对 JDBC 操作不安全（事务未提交、连接未关闭） |

**决策:** 选 A。任一文件判定 FAILED 时，遍历尚未完成的 Future 调用 `cancel(false)`——排队中任务永不起动，运行中任务自然完成。与 Spec 语义完全一致。

### 决策 6: 错误/异常隔离

| 方案 | 概要 | 优点 | 缺点 | 比较依据 |
|---|---|---|---|---|
| A | `Callable<FileOutcome>` + `Future.get()` 解包 `ExecutionException` | 保留原始异常类型 | 需额外解包逻辑 | — |
| B | worker 内部全量 try-catch，返回 hardError `FileOutcome` | Future 永不满错误；主线程 get() 无异常路径 | 丢失异常链类型（不影响功能） | 更简洁，与 Spec 错误隔离合同一致 |

**决策:** 选 B。每个 Callable worker 在最外层 try-catch，所有异常（ParseException/FatalDatabaseException/RuntimeException）均转换为 `FileOutcome.hardFailure(detailLines)` 返回。主线程 `Future.get()` 永不抛 `ExecutionException`——因为 FileRunner.run() 本身已全量 try-catch 返回 FileOutcome（见 `FileRunner.java:41-133`），并行 Callable 仅需再包裹一层以防 `Future<FileOutcome>` 签名外的意外。

### 决策 7: CLI 解析

| 方案 | 概要 | 优点 | 缺点 | 比较依据 |
|---|---|---|---|---|
| A | 仿照 `--hash-threshold`（int 取值）全链路 | 风格一致；链路清晰 | 需额外互斥检查 | 唯一自然路径 |
| B | 仿照 `--halt`（boolean flag） | — | N 需要取值，不适用 | — |

**决策:** 选 A。全链路：
1. `CliArgumentParser.parse()` — 解析 `--parallel <N>`，校验正整数/非数字 → UsageException；同时检查 `--override` 共存 → UsageException
2. `ParsedArguments` — 新增 `Optional<Integer> parallel` 字段
3. `RuntimeConfigResolver.resolve()` — 透传 `parallel` 到 `CliOptions`
4. `CliOptions` — 新增 `int parallel` 字段（默认 0 表示未指定）
5. `Main.printHelp()` — 新增 `--parallel` 行

### 决策 8: 并发执行对现有 --halt 路径的影响

| 方案 | 概要 | 优点 | 缺点 | 比较依据 |
|---|---|---|---|---|
| A | `CliSession.execute()` 增加条件分支 | 零回归；现有代码不变 | 两个代码路径（顺序 + 并行） | Spec P0-1 要求字节一致 |
| B | 统一抽象层覆盖顺序与并行 | 理论更优雅 | 过度设计；改动范围大 | 违背"零回归"原则 |

**决策:** 选 A。`if (options.parallel() > 1)` 走 `executeParallel()` 新方法，`else` 走现行顺序路径（代码完全不改）。`--parallel 1` 走顺序路径。

## 模块边界与分层

```
CliArgumentParser (解析) → ParsedArguments → RuntimeConfigResolver (合并)
→ CliOptions (含 parallel 字段) → CliSession (执行编排)
  ├─ [parallel ≤ 1] execute() 顺序路径（不变）
  └─ [parallel > 1] executeParallel() 并行路径
       ├─ ParallelExecutor (线程池封装) — 新建组件
       │    └─ ExecutorService (newFixedThreadPool)
       ├─ FileRunner[] (每 worker 独立实例) — 现有组件
       └─ ReportWriter (主线程聚合后调用) — 现有组件
```

**依赖方向:** `CliSession` → `ParallelExecutor` (新建)；`ParallelExecutor` 仅依赖 `java.util.concurrent`，不依赖本项目的 cli/db/runner 包。`FileRunner`、`ReportWriter`、`ConnectionFactory`、`PostgresSchemaIsolation` 无变更。

`ParallelExecutor` 职责：封装 `ExecutorService` 生命周期（创建、shutdown、awaitTermination），暴露 `submitTasks(List<Callable<FileOutcome>>)` → `List<Future<FileOutcome>>` + `shutdown()` 方法。不感知 CliOptions 语义。

## 模块影响

| 模块 | 变更类型 | 说明 |
|---|---|---|
| `CliArgumentParser` | 修改 | 新增 `--parallel` 解析 + `--override` 互斥检查 |
| `ParsedArguments` | 修改 | 新增 `Optional<Integer> parallel` |
| `RuntimeConfigResolver` | 修改 | 透传 `parallel` |
| `CliOptions` | 修改 | 新增 `int parallel` 字段 |
| `Main.printHelp()` | 修改 | 新增 `--parallel` 帮助行 |
| `CliSession` | 修改 | 新增 `executeParallel()` 方法；`execute()` 增加条件分支 |
| `ParallelExecutor` | 新建 | 线程池封装（`com.ggtest.cli` 包内，package-private） |
| `FileRunner` | 不变 | 保持现有实现 |
| `ReportWriter` | 不变 | 保持现有实现 |
| `ConnectionFactory` | 不变 | 完全无状态 |
| `PostgresSchemaIsolation` | 不变 | 已线程安全（UUID schema 名） |

## 风险

| 风险 | 影响 | 缓解 |
|---|---|---|
| PG 连接数爆炸（N 个文件 × 每文件多连接） | PG 服务端拒绝连接或性能下降 | 文档提示；N 由用户控制；不设硬上限 |
| ReportWriter 共享 `PrintStream out` 导致竞态 | 输出交错/乱序 | 聚合模式（决策 4）：worker 不调 ReportWriter，主线程串行输出 |
| `Future.get()` 阻塞主线程等待慢文件 | 主线程等待最后一个文件完成才能输出 | 这是正确行为（聚合模式要求全部完成后才输出） |
| 并行执行增加 JVM 内存占用 | 大语料场景 OOM | 与顺序执行相比，仅额外占用 N 个 FileRunner 实例 + N 个线程栈；N 由用户控制 |

## 对 Plan 与 Developer 的要点

### Plan

- 任务拆解围绕上述 8 项决策展开：CLI 解析→线程池封装→结果聚合→执行编排→halt→互斥→测试→文档
- 触碰路径以 `CliSession`、`ParallelExecutor`（新建）、`CliArgumentParser`、`ParsedArguments`、`CliOptions`、`RuntimeConfigResolver`、`Main.printHelp()` 为核心
- 零下游（`FileRunner`/`ReportWriter`/`ConnectionFactory`/`PostgresSchemaIsolation` 均不变）

### Developer

- `--parallel` 取值：正整数 N≥1 → pool size；0/负数/非数字 → UsageException
- `--parallel 1` 走顺序路径（`execute()` 不变），`N ≥ 2` 走 `executeParallel()`
- `--override` 与 `--parallel` 互斥：在 `CliArgumentParser.parse()` 阶段检测，两个 flag 同时为 true → UsageException
- 并行路径输出逻辑与顺序路径完全一致：status line → error block → Error section → TOTAL；顺序由 `List<Future<FileOutcome>>` 按输入索引保证
- `--halt` 在并行路径：任一 FAILED → 遍历剩余未 done 的 Future → `cancel(false)` → 仅已执行文件参与报告
- 不引入 `ExecutorService.shutdownNow()`；不 interrupt JDBC 线程
