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

### 决策 3: 输出顺序——按序流式（in-order streaming）

| 方案 | 概要 | 优点 | 缺点 | 比较依据 |
|---|---|---|---|---|
| A | 按序流式：结果按输入索引存入滑窗缓冲；每当「下一个待打印索引」已有结果就立即打印并推进 | 严格满足 P1-1（输入顺序）；**流式**——大语料下文件一完成即可见进度（仅被尚未完成的更早索引暂时阻塞）；与原 `submitAll + 按序 get()` 的 UX 一致 | 一个慢文件会暂阻塞它及后续的打印（但仍并行执行，只是输出等待） | spec 要求输入顺序；真实大语料（`./sqllogictest/test/`，622 文件）需要可见进度 |
| B | 全部完成后一次性按序输出（end-dump） | 实现略简 | **大语料下全程无输出直到结束**——UX 倒退；用户实测 `--parallel 10 ./sqllogictest/test/` 时即报告此问题 | 不可接受 |
| C | 完成即输出（completion 顺序） | 早出结果 | 违反 P1-1（完成顺序 ≠ 输入顺序） | 不满足 |

**决策:** 选 A（in-order streaming）。`IndexedTimedOutcome[] results`（按输入索引）+ `int nextToPrint` 指针；收割到任一结果后，执行 `while (results[nextToPrint] != null) { 打印; nextToPrint++; }`——把「已完成且前面都已打印」的结果立即输出。未分派文件（`results[i]==null`）永不打印、不计入 TOTAL（满足 spec P1-2）。Error section / TOTAL 仍在所有 status line 之后输出。

> 取代 Design v1.0/v1.1 决策 3 的方案「end-dump」。修订起因：用户在源分支实测 `--parallel 10 ./sqllogictest/test/` 发现全程无输出（QA 第 1 轮遗漏——测试以 `Main.run` 返回后捕获整段 stdout，流式与否对测试不可观察，故未被发现）。in-order streaming 恢复原 `submitAll` 路径的流式 UX，同时保留按序与 halt-skip 正确性。

### 决策 4: ParallelExecutor 接口调整

| 方案 | 概要 | 比较依据 |
|---|---|---|
| A | 暴露 `ExecutorService executor()`（package-private）供 `CliSession` 包装为 `ExecutorCompletionService`；保留 `shutdown()` / `awaitTermination()` | 最小改动；CompletionService 是 JDK 标准用法 |
| B | 在 `ParallelExecutor` 内封装 CompletionService 与分派循环 | 把编排逻辑放进线程池封装层，职责混淆 | 

**决策:** 选 A。`ParallelExecutor` 仍只管线程池生命周期；分派/收割/聚合编排在 `CliSession.executeParallel`（属 CLI 编排职责）。删除 `submitAll`（不再使用）；如其他处无引用则一并移除以避免死代码。

### 决策 5: 测试确定性策略（即时失败触发器）

根因之外，即便用受控分派，「未分派文件是否会被派发」仍取决于**失败文件是否先于并发文件完成**——因为并发文件先完成会腾出 worker 槽位、在 halt 前触发下一文件派发。故要让「排队文件被跳过」可重复验证，必须使失败文件**确定地**先完成。

**实现期关键发现（见 dev-notes.md）：** 实测（冷/暖 JVM 均然）SQLite 首批文件承担 ~200ms 一次性预热（驱动/连接/JIT），使「失败断言文件」与「并发 DB 文件」**几乎同时**完成（211ms vs 206ms），op 数差异被预热噪声淹没。因此「用 op 数让失败文件先完成」不可靠。

| 方案 | 概要 | 比较依据 |
|---|---|---|
| A | 失败文件**不含任何 DB 工作**（`1-parse-error.test` 单行非法记录 → parse 即 hardFailure，~µs 完成，无连接），确定地先于任何 DB 文件（`2-pass.test`，承担预热）完成；`3-queued.test` 合法通过内容，应永不执行 | 完成顺序由「有无 DB 工作」决定（确定），与机器/JVM 预热/时序**完全无关**；零时序依赖 |
| B | `1-fail`（少 op 断言失败）+ `2-slow`（多 op 通过）靠 op 数拉开完成顺序 | 实测 ~200ms 共享预热淹没 op 差异，211ms vs 206ms 仍并驾齐驱；flaky |
| C | 在 fixture 中引入显式延迟（sleep/大查询） | sleep 不可靠且拖慢测试；fixture 不应有生产式副作用 |

**决策:** 选 A。失败文件用 parse error（`FAILED` 桶 + hardError，但**零 DB 工作**）→ 确定地、跨环境地在任何 DB 文件之前完成 → halt 在并发文件仍运行时触发 → `3-queued` 永不被分派。

> 取代原 Design v1.0 决策 5 的方案 A（op 数拉开）。trade-off：触发器由「断言失败」变为「hard error（parse）」，退出码由 1 变 2。spec `--halt` 语义对二者一视同仁（均为 `FAILED` 桶 → 停止派发），故 skip 机制被等价覆盖；hard error 触发路径此前仅顺序用例覆盖，本切片顺带补足并行 hard-error halt 覆盖。

受控分派 + `1-parse-error` 先完成的组合，使下述结果**确定**：`1-parse-error` 报告 `[FAILED]`（hardError）触发 halt；`2-pass`（运行中）完成并报告 `[PASSED]`；`3-queued` 永不被分派，不出现在 stdout、不计入 TOTAL。验证 spec「跳过未分派文件」与「报告运行中文件」两条合同。

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
| `MainOrchestrationTest.parallelHaltSkipsQueuedFilesReportsRunningFiles` | 修改 | 改用 `parallel-halt/` 三 fixture；断言 `1-parse-error` FAILED、`2-pass` PASSED、`3-queued` 不出现、passed=1 failed=1、exit=2（hardError） |
| 新增 fixture `parallel-halt/{1-parse-error,2-pass,3-queued}.test` | 新建 | 自包含；数字前缀锁定排序；`1-parse-error` 无 DB 工作 |

## 风险

| 风险 | 影响 | 缓解 |
|---|---|---|
| 重写 `executeParallel` 引入新并发缺陷 | 死锁 / 结果丢失 / 输出乱序 | 收割循环不变量明确（`running` 计数 + `pending` 队列）；输出按输入索引遍历缓冲结果保证 P1-1；现有并行测试（无 halt 报告完整、故障隔离、凭据脱敏、status line 顺序、`--parallel 1` 等价）作回归网 |
| `2-slow` 仍可能在极快机器上先于失败文件完成 | 排队文件被派发 → 测试 flaky | 已规避：失败文件改用 parse error（无 DB 工作），确定先于任何 DB 文件完成；不依赖工作量比 |
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
- 测试断言更新见决策 5；`3-queued.test` 内容须为合法通过文件（防御性地不应硬错）；`1-parse-error.test` 为单行非法记录（parse 即失败，无 DB 工作）。
