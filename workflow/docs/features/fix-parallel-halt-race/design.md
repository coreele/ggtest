# Design: fix-parallel-halt-race

## 背景与约束

- `CliSession.executeParallel()`（`src/main/java/com/ggtest/cli/CliSession.java:129-236`）当前用 `ParallelExecutor.submitAll()` 把**全部**文件任务一次性塞进 `newFixedThreadPool(N)` 的无界队列，主循环按提交顺序 `future.get()`，遇 FAILED 才 `cancel(false)` 后续 future（lines 160-215）。
- 该设计有并发竞态（见 manager 进度笔记根因）：`cancel(false)` 与 worker 从队列捞任务赛跑，导致快机器过度取消（连已分派任务都被取消）、慢机器欠取消（排队任务被捞起执行）。两个方向均违反 `add-parallel-execution/spec.md` 决策 #4 与 P1-2。
- 不可变更合同（继承自 `add-parallel-execution/spec.md`，本切片不新增、只让其成立）：
  - 任一文件 FAILED → 取消已提交但**未分派**的任务 → 等待**已分派**任务完成（不 interrupt 运行中 DB 操作）→ 仅报告已执行文件。
  - 不 interrupt 运行中 JDBC 线程；不使用 `shutdownNow()`。
  - `--parallel 1` 走顺序路径（`execute()`），零回归；status line 按输入（排序后）文件顺序输出（P1-1）。
  - 退出码优先级不变（hardError → 2；failed > 0 → 1；否则 0）。
- 约束：仅修复并行 `--halt`；顺序 `--halt`（`execute()` lines 55-127）零变更；`--parallel`（无 halt）行为零回归。

## 方案对比与决策

### 决策 1: 分派模型——受控分派（lazy submit）取代 submitAll

| 方案 | 概要 | 优点 | 缺点 | 比较依据 |
|---|---|---|---|---|
| A | `ExecutorCompletionService` + 受控分派：主线程维护待分派队列，`running < N && !halted` 时才 submit 下一个；`take()` 收割完成结果 | 「未分派」边界由主线程显式控制，**确定**——halt 后不再 submit，未分派文件永不起动 | 需重写 `executeParallel` 主体；结果需按索引缓冲后按输入顺序输出 | 根因就是无界队列被 worker 自主抽干；只有主线程控制 submit 时机才能消除竞态 |
| B | 保留 `submitAll`，改用自定义阻塞队列在 halt 后停止出队 | 复用现有结构 | 需侵入 JDK 线程池队列语义；halt 后已在队列中的任务状态仍模糊 | 工程复杂、易错；本质仍把分派权交给线程池 |
| C | 共享 `volatile boolean halted` + FileRunner/runner 周期检查 | 可中途中止运行中文件 | 侵入 FileRunner 与 runner；违反「等待运行中 DB 操作完成」合同 | 不符合 spec |

**决策:** 选 A。受控分派把「分派 / 未分派」的判定权收归主线程：一个文件要么已被主线程 `submit`（已分派，须让其完成并报告），要么仍在待分派队列里（halt 后不再 submit，确定地不执行、不报告）。这使 spec 决策 #4 的「已提交但未分派」从竞态变为确定。

> 取代 `add-parallel-execution/design.md` 决策 5（`submitAll + Future.cancel(false)`）。原决策的隐含假设「`cancel(false)` 能取消排队中任务」忽略了 worker 自主抽干队列的竞态；本切片以受控分派修正。

### 决策 2: halt 触发与「已分派」语义

| 方案 | 概要 | 比较依据 |
|---|---|---|
| A | 收割到 FAILED 且 `options.halt()` → 置 `halted=true`，**立即停止**后续 submit；已 submit 的任务（含刚收割的 FAILED 文件之外仍运行的）继续自然完成 | spec「等待正在执行的任务完成」；已 submit 即「已分派」，必须报告其真实结果 |
| B | halt 后对仍运行任务 `cancel(true)` 中断 | 违反「不 interrupt DB 操作」 |
| C | halt 后 `shutdownNow()` | 同上，且影响线程池 |

**决策:** 选 A。`halted` 只用于「停止派发新文件」，绝不去取消/中断已分派任务。收割循环继续 `take()` 直到 `running == 0`，把仍运行任务的完成结果一并按真实桶报告。

### 决策 3: 输出顺序——缓冲后按输入顺序输出

| 方案 | 概要 | 优点 | 缺点 | 比较依据 |
|---|---|---|---|---|
| A | 完成（含 halt）后，按输入（排序后）索引顺序遍历；仅有结果的文件输出 status line + error block；未分派的文件跳过 | 严格满足 P1-1（输入顺序）；与顺序路径输出格式一致；与 `add-parallel-execution/design.md` 决策 4「聚合后按序输出」吻合 | 全部完成后才统一输出（非流式） | 测试在 `Main.run` 返回后捕获整段 stdout，流式与否不可观察；聚合输出更贴合 spec 原意 |
| B | 完成即输出（completion 顺序） | 早出结果 | 违反 P1-1（完成顺序 ≠ 输入顺序） | 不满足 |

**决策:** 选 A。`Map<Integer, TimedFileOutcome>`（或数组）按输入索引存结果；收割结束后按索引升序输出已执行文件，未执行文件不出现在 stdout、不计入 TOTAL——与 spec P1-2「未执行文件不在 stdout」一致。Error section / TOTAL 逻辑复用现有 `reportWriter`，仅数据源从「按序 get()」改为「按序遍历结果容器」。

### 决策 4: ParallelExecutor 接口调整

| 方案 | 概要 | 比较依据 |
|---|---|---|
| A | 暴露 `ExecutorService executor()`（package-private）供 `CliSession` 包装为 `ExecutorCompletionService`；保留 `shutdown()` / `awaitTermination()` | 最小改动；CompletionService 是 JDK 标准用法 |
| B | 在 `ParallelExecutor` 内封装 CompletionService 与分派循环 | 把编排逻辑放进线程池封装层，职责混淆 | 

**决策:** 选 A。`ParallelExecutor` 仍只管线程池生命周期；分派/收割/聚合编排在 `CliSession.executeParallel`（属 CLI 编排职责）。删除 `submitAll`（不再使用）；如其他处无引用则一并移除以避免死代码。

### 决策 5: 测试确定性策略（fixture 完成顺序可控）

根因之外，即便用受控分派，「未分派文件是否会被派发」仍取决于**失败文件是否先于并发文件完成**——因为并发文件先完成会腾出 worker 槽位、在 halt 前触发下一文件派发。故要让「排队文件被跳过」可重复验证，必须使失败文件**确定地**先完成。

| 方案 | 概要 | 比较依据 |
|---|---|---|
| A | 新增自包含 fixture 子目录 `src/test/resources/fixtures/cli/parallel-halt/`，三文件按数字前缀保证排序与完成顺序：`1-fail.test`（极少语句、首查询即失败 → 最快完成）、`2-slow.test`（大量语句全通过 → 显著慢于 1-fail，确保其完成前 1-fail 已失败触发 halt）、`3-queued.test`（合法通过内容，但应永不执行） | 完成顺序由语句工作量决定（同引擎下确定），不依赖 sleep/时序；数字前缀同时锁定排序；自包含不污染其他测试 |
| B | 复用现有 `multi-fail.test`（慢、失败）+ `nested/a.test`（快） | multi-fail 比 nested/a 慢 → nested/a 先完成腾槽 → 排队文件被派发；断言无法成立 |
| C | 在 fixture 中引入显式延迟（sleep/大查询） | sleep 不可靠且拖慢测试；fixture 不应有生产式副作用 |

**决策:** 选 A。受控分派 + `1-fail` 先完成的 fixture 组合，使下述结果**确定**：`1-fail` 报告 `[FAILED]` 触发 halt；`2-slow`（运行中）完成并报告 `[PASSED]`；`3-queued` 永不被分派，不出现在 stdout、不计入 TOTAL。验证 spec「跳过未分派文件」与「报告运行中文件」两条合同。

> 数量选取：`1-fail` ≈ 3 条语句；`2-slow` ≈ 40 条语句。同引擎下工作量比 ≈ 13×，足以在任意机器上保证 `1-fail` 先完成，且整体测试耗时仍 < 50 ms。

## 模块边界与分层

```
CliArgumentParser → ParsedArguments → RuntimeConfigResolver → CliOptions (不变)
→ CliSession (执行编排)
  ├─ [parallel ≤ 1] execute() 顺序路径（不变，零回归）
  └─ [parallel > 1] executeParallel() 并行路径（重写分派/收割/聚合）
       ├─ ParallelExecutor（暴露 ExecutorService；移除 submitAll）
       │    └─ ExecutorService (newFixedThreadPool) 包装为 ExecutorCompletionService
       ├─ FileRunner[]（每分派任务独立实例，不变）
       └─ ReportWriter（主线程聚合后调用，不变）
```

**依赖方向不变：** `CliSession` → `ParallelExecutor`（仅新增读取 `executor()`）；`ParallelExecutor` 仅依赖 `java.util.concurrent`。

## 模块影响

| 模块 | 变更类型 | 说明 |
|---|---|---|
| `CliSession.executeParallel()` | 重写 | 受控分派（CompletionService + lazy submit）+ halt 停止派发 + 缓冲按序输出；取代 `submitAll + cancel(false)` |
| `ParallelExecutor` | 修改 | 暴露 `ExecutorService executor()`（package-private）；移除未再使用的 `submitAll`（确认无其他引用） |
| `CliSession.execute()`（顺序路径） | 不变 | 零回归 |
| `FileRunner` / `ReportWriter` / `ConnectionFactory` / `PostgresSchemaIsolation` | 不变 | — |
| `MainOrchestrationTest.parallelHaltSkipsQueuedFilesReportsRunningFiles` | 修改 | 改用 `parallel-halt/` 三 fixture；断言 `1-fail` FAILED、`2-slow` PASSED、`3-queued` 不出现、passed=1 failed=1、exit=1 |
| 新增 fixture `parallel-halt/{1-fail,2-slow,3-queued}.test` | 新建 | 自包含；数字前缀锁定排序 |

## 风险

| 风险 | 影响 | 缓解 |
|---|---|---|
| 重写 `executeParallel` 引入新并发缺陷 | 死锁 / 结果丢失 / 输出乱序 | 收割循环不变量明确（`running` 计数 + `pending` 队列）；输出按输入索引遍历缓冲结果保证 P1-1；现有并行测试（无 halt 报告完整、故障隔离、凭据脱敏、status line 顺序、`--parallel 1` 等价）作回归网 |
| `2-slow` 仍可能在极快机器上先于 `1-fail` 完成 | 排队文件被派发 → 测试 flaky | 工作量比 ≈13× 提供稳定裕度；且即便发生，是确定的完成顺序问题，可调大 `2-slow` 语句数 |
| CompletionService `take()` 阻塞 | 若任务异常未包装导致 `take().get()` 抛 `ExecutionException` 可能中断循环 | Callable 保留全量 try-catch 包装为 hardError `TimedFileOutcome`（沿用 add-parallel-execution 决策 6），`get()` 永不抛 |
| 移除 `submitAll` 误伤其他调用方 | 编译失败 | Plan 触碰路径已限定；Developer 改前 `grep` 确认无其他引用 |

## 对 Plan 与 Developer 的要点

### Plan

- 任务拆解：①ParallelExecutor 接口调整 → ②executeParallel 受控分派重写 → ③halt 停止派派语义 → ④缓冲按序输出 → ⑤新增 fixture + 改测试 → ⑥全量并行/顺序回归。
- 触碰路径：`CliSession.java`、`ParallelExecutor.java`、`MainOrchestrationTest.java`、`src/test/resources/fixtures/cli/parallel-halt/*`。
- 最低验证层：`mvn -q test`（全量）+ 针对性 `mvn -q -Dtest='MainOrchestrationTest#parallelHaltSkipsQueuedFilesReportsRunningFiles' test` 反复 ≥5 次确认稳定。
- 文档影响：本切片 dev-notes 记录与 `add-parallel-execution/design.md` 决策 5 的取代关系；README 无面向用户语义变化。

### Developer

- `executeParallel` 主结构：`ExecutorCompletionService<TimedFileOutcome> ecs = new ExecutorCompletionService<>(executor.executor());`；`Deque<Integer> pending`（输入索引）；`int running`；`boolean halted`。
- 派发循环：`while (running < N && !pending.isEmpty() && !halted) { submit(pending.pollFirst()); running++; }`
- 收割循环：`while (running > 0) { Future<TimedFileOutcome> f = ecs.take(); running--; TimedFileOutcome t = unwrap(f); store(t, itsIndex); if (!halted && options.halt() && t.outcome().bucket()==FAILED) halted=true; refill per dispatch loop; }`
- 完成后按输入索引升序遍历结果容器，仅对存在结果的下标输出（status line + FAILED 的 error block），累计 passed/failed/skipped/overridden 与 hardError、failedPaths；最后 `printErrorSection` / `printTrailingBlankIfNeeded` / `printTotal`。
- 退出码逻辑不变。
- `shutdown()` 后 `awaitTermination(30s)` 不变；不引入 `shutdownNow()` / `cancel(true)`。
- 测试断言更新见决策 5；`3-queued.test` 内容须为合法通过文件（防御性地不应硬错）。
