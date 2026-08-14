# Dev Notes: fix-parallel-halt-race

## 实现摘要

按 Design v1.1（决策 5 已据实现期发现修订）完成 T1-T5：

- **T1 `ParallelExecutor`**：暴露 package-private `executor()`；移除 `submitAll`（`grep` 确认仅 `CliSession.executeParallel` 引用）。
- **T2/T3 `CliSession.executeParallel`**：重写为受控分派——
  - `ExecutorCompletionService<IndexedTimedOutcome>` 包装线程池。
  - `IndexedTimedOutcome(index, outcome, elapsedMs)`：因 CompletionService 不提供 Future→索引映射，任务自带索引返回。
  - 待分派队列 `Deque<Integer> pending`（输入索引）+ `int running` + `boolean halted`。
  - 派发循环：`while (running < N && !pending.isEmpty() && !halted) submit`。
  - 收割循环：`while (running > 0) { ecs.take(); running--; ...; if (!halted && halt && FAILED) halted=true; refill }`。
  - halt 只停止派发，**不** cancel/中断已分派任务（满足 spec「等待运行中 DB 操作完成」）。
  - 结果按输入（排序）索引升序遍历输出；`results[i]==null`（未分派）的文件跳过——不出现在 stdout、不计入 TOTAL。
  - Callable 全量 try-catch → hardFailure `IndexedTimedOutcome`（沿用 add-parallel-execution 决策 6），`take().get()` 永不抛；另保留对 `take()` `InterruptedException` 的中断处理。
- **T4 fixture + 测试**：见下「决策 5 偏离」。
- **T5 回归**：见 qa-report.md。

## 决策 5 偏离：失败触发器改用 parse error（无 DB 工作）

Design v1.0 原定 `1-fail`（少 op 断言失败）+ `2-slow`（多 op 通过），靠工作量比让失败文件先完成。**实测证伪**：

| 文件 | 冷启动（CLI 直跑） | 暖 JVM（mvn test） |
|---|---|---|
| 失败文件（少 op，DB） | 168 ms | 211 ms |
| 并发文件（多 op，DB） | 162 ms（38 op） | 206 ms |

SQLite 首批文件承担 ~200ms 一次性预热（驱动加载/连接初始化/JIT），使两个 DB 文件**几乎同时**完成，op 差异（38 op ≈ 6ms）被预热噪声淹没。无论冷/暖、无论 op 数，失败文件都无法可靠先完成 → flaky 依旧。

**修订方案（Design v1.1 决策 5）：** 失败文件改为 `1-parse-error.test`（单行非法记录）→ `FileRunner` parse 阶段即返回 `hardFailure`（`FAILED` 桶 + hardError），**零 DB 工作、零连接**，~µs 完成，确定地先于任何 DB 文件。`2-pass.test` 为普通通过文件（dispatched，运行中），`3-queued.test` 为通过文件（pending，被跳过）。

trade-off：触发器由断言失败变为 hard error（parse），退出码 1 → 2。spec `--halt` 对二者一视同仁（均 `FAILED` 桶 → 停止派发），skip 机制等价覆盖；并行 hard-error halt 路径此前无覆盖，本切片顺带补足。

## 与 add-parallel-execution 的关系

- 取代 `add-parallel-execution/design.md` 决策 5（`submitAll + Future.cancel(false)`）。原决策的 `cancel(false)` 假设「能取消排队中任务」忽略了 worker 自主抽干无界队列的竞态：快机器过度取消（连已分派任务都被取消）、慢机器欠取消（排队任务被捞起执行）。
- 受控分派把「分派/未分派」判定权收归主线程，从结构上消除该竞态。
- 输出契约不变（聚合式、按输入顺序、status line + error block + Error section + TOTAL）；退出码优先级不变。

## 触碰文件

- `src/main/java/com/ggtest/cli/ParallelExecutor.java`（改）
- `src/main/java/com/ggtest/cli/CliSession.java`（重写 `executeParallel`）
- `src/test/resources/fixtures/cli/parallel-halt/{1-parse-error,2-pass,3-queued}.test`（新建）
- `src/test/java/com/ggtest/cli/MainOrchestrationTest.java`（改目标用例）

明确未触碰：`CliSession.execute()`（顺序路径）、`FileRunner`、`ReportWriter`、`ConnectionFactory`、`PostgresSchemaIsolation`、CLI 解析链。

## 开发者验证

- `mvn -q compile`：通过。
- 目标用例连跑 5/5 通过（见 qa-report.md V1）。
- `mvn test` 全量 343 通过 / 0 失败 / 0 错误（V2）。

## 重构（QA 后用户实测反馈）：恢复按序流式输出

**问题：** Design v1.0/v1.1 决策 3 采用「end-dump」（全部完成后按序一次性输出）。用户在源分支实测 `--parallel 10 ./sqllogictest/test/`（622 文件 / 1.1G）时报告「为什么要等运行完了一起输出」。QA 第 1 轮遗漏此点——测试以 `Main.run` 返回后捕获整段 stdout，流式与否对单测不可观察。

**修订（Design v1.2 决策 3 → in-order streaming）：** 引入 `int nextToPrint` 指针 + `IndexedTimedOutcome[] results` 滑窗；收割到任一结果后，`while (nextToPrint < len && results[nextToPrint] != null)` 立即打印并推进。一个慢文件只暂阻塞它自身及后续的打印（执行仍并行），不再整批等待。halt-skip 与 P1-1 顺序不变。

**验证：** `./sqllogictest/test/evidence/`（12 文件）`--parallel 4` 实测，status line 按完成+顺序在 +262ms/+266ms/+270ms… 陆续出现（非末尾一次性），首批慢文件完成后快文件立即流式。目标用例仍 5/5；全量 343/0/0。
