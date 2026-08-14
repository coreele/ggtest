# Review: fix-parallel-halt-race

## 审阅范围

- 实现提交: `e48e0c9` fix(cli): deterministic parallel --halt via controlled dispatch
- 登记提交: `7519272` docs(workflow): register fix-parallel-halt-race + design + plan
- 依据: 合同继承 `add-parallel-execution/spec.md`（§`--halt` 在并行下的语义 + 决策 #4）；`design.md` v1.1（决策 1-5）；`plan.md`（T1-T5）
- 触碰: `ParallelExecutor.java`、`CliSession.java`、`MainOrchestrationTest.java`、`fixtures/cli/parallel-halt/*`

## 实现正确性

### 合同对照（add-parallel-execution spec 决策 #4 / P1-2）

| 要求 | 实现 | 结论 |
|---|---|---|
| 任一文件 FAILED → 取消已提交但未分派的任务 | FAILED → `halted=true`，派发循环 `!halted` 守卫使其后 `pending` 中文件**永不被 submit** | Pass |
| 等待已分派任务完成（不 interrupt 运行中 DB 操作） | 不对任何 future 调用 cancel；收割循环 `while (running>0) ecs.take()` 直到已分派任务全部返回；无 `shutdownNow`/`cancel(true)` | Pass |
| 仅报告已执行文件 | `results[i]==null`（未分派）在输出循环 `continue`——无 status line、不计入 TOTAL | Pass |
| 输出按输入（排序）顺序（P1-1） | `results[]` 按输入索引存，输出循环 `for i in 0..size-1` 升序遍历 | Pass |
| 退出码优先级不变 | hardError→2；failed>0→1；else 0，逻辑体与顺序路径一致 | Pass |

### 并发正确性（核心）

| 不变量 | 验证 | 结论 |
|---|---|---|
| 在途任务 ≤ N（无队列被 worker 自主抽干） | 仅在 `running < N` 时 submit，且 submit 后 `running++`、reap 后 `running--`；`running` = 已 submit 未 reap 计数，主线程独占 | Pass |
| 「分派/未分派」由主线程决定（无竞态） | 任务唯一入口是主线程 `ecs.submit(...)`；线程池自身队列永不会积累（在途≤N=N threads） | Pass——这是对原 `submitAll + cancel(false)` 竞态的结构性消除 |
| halt 触发即时停止派发 | 收割到 FAILED 同一迭代内置 `halted=true`，紧随的 refill 守卫 `!halted` 立即生效 | Pass |
| 安全发布 | `IndexedTimedOutcome` 不可变（record，全部 final 字段）；ExecutorCompletionService 的 Future 建立 happens-before；`results`/`running`/`halted`/`pending` 仅主线程访问 | Pass |
| 错误隔离 | Callable 全量 `catch (Throwable)` → `hardFailure IndexedTimedOutcome`；单文件异常不影响其他 worker | Pass |

### 关键路径检查

| 检查项 | 结果 | 说明 |
|---|---|---|
| `--parallel 1` 走顺序路径（零回归） | Pass | `options.parallel() > 1` 判定不变；顺序 `execute()` 方法体 0 行变更 |
| 空 / 单 / 少于 N 文件 | Pass | 派发循环 `pending.isEmpty()` 守卫；N≥文件数时一次性全派发 |
| halt 由首个 FAILED 文件触发（含 hardError） | Pass | `outcome.bucket()==FileBucket.FAILED` 覆盖 assertionFailure 与 hardFailure（parse error 归此） |
| 多文件无 halt 全执行全报告 | Pass | `parallel2MultiFileReportComplete`、`parallelStatusLineOrderMatchesSorterOutput`、`parallelFaultIsolation…`、`parallelPasswordNeverPrinted` 回归通过 |
| 顺序 halt 路径未受影响 | Pass | `haltStopsAfterFirstFailingFileAndDoesNotStartLaterFiles`、`haltWithHardErrorExitsTwoAndDoesNotStartLaterFiles`、`corpusHaltRecordDoesNotTriggerCliHalt` 回归通过 |

### 观察（非阻塞）

1. **`future.get()` 的 catch 块为不可达防御代码**：Callable 已 `catch (Throwable)`，`get()` 不会抛 `ExecutionException`；`get()` 对已完成 Future 不阻塞故不会抛 `InterruptedException`。该 catch 内构造 `IndexedTimedOutcome(-1, …)` 且以 `result.index() >= 0` 守卫丢弃。死代码但防御性合理；非阻塞。

2. **`ecs.take()` 的 InterruptedException 处理为 `break`**：与原实现「逐 future 转 interrupted hardFailure 并继续」略有差异。该路径仅在 JVM 关停等异常中断时触发；`break` 后 `pool.shutdown() + awaitTermination(30s)` 仍等待在途任务。异常关停路径下精确报告非目标，非阻塞。

3. **防御性 `"unexpected error: " + w` 未走 `sanitize()`**：与原 `add-parallel-execution` review 观察 #3 同形态；该路径不可达（Callable 全量 catch），密码不会出现于此类异常消息。风险极低，非阻塞。

## 测试有效性

### 确定性论证（fixture 策略）

| 维度 | 评估 | 结论 |
|---|---|---|
| 失败文件确定先完成 | `1-parse-error.test` 单行非法记录 → parse 阶段即 `hardFailure`，**零 DB 工作、零连接**，~µs；任何 DB 文件（`2-pass`）承担 ~200ms 预热 → 1-parse-error 确定先完成，与机器/JVM/时序无关 | Pass |
| 排队文件确定被跳过 | halt 在 `2-pass` 仍运行时触发 → 派发循环停止 → `3-queued` 永不 submit | Pass |
| 运行中文件确定被报告 | `2-pass` 已分派 → 收割循环等待其完成 → 按真实桶 PASSED 报告 | Pass |
| trade-off 记录 | 触发器为 hard error（parse）→ exit 2；spec `--halt` 对 FAILED 桶一视同仁，机制等价；并行 hard-error halt 路径获新覆盖 | 可接受 |

### 验证证据

- `mvn -Dtest='MainOrchestrationTest#parallelHaltSkipsQueuedFilesReportsRunningFiles'`：连跑 5/5 通过（qa-report V1）。
- `mvn test`：343 通过 / 0 失败 / 0 错误（qa-report V2）。
- CLI 直跑输出复核：`1-parse-error [FAILED] in 14ms`（无 DB）→ `2-pass [PASSED] in 152ms`（预热）；`3-queued` 缺席；`TOTAL: passed=1 failed=1 skipped=0`；exit=2。输出按输入顺序。

### 测试有效性结论

断言非空泛：分别绑定三个不同 fixture 名与计数/退出码；`assertFalse(out.contains("3-queued.test"))` 与 `assertEquals(2, exitCode)` 对 skip 行为与 hardError 退出码构成有效正向+负向覆盖。

## 文档影响核对

| Plan 声明 | 实现 | 备注 |
|---|---|---|
| 开发文档 | 一致 | `dev-notes.md` 记录决策 5 偏离（预热实测）+ 取代 add-parallel-execution 决策 5 |
| design.md 决策 5 | 已据实现期发现修订为 v1.1（parse-error 触发器） | 修订链路完整：design → plan → dev-notes 一致 |
| 用户文档 | N/A（Plan 声明） | `--halt` 可观察语义收紧为符合既有 spec，README 无需改动 |
| 运维文档 | N/A | 无部署/排障变更 |

无未声明文档缺口。

## 安全影响核对

| 检查项 | 结果 | 备注 |
|---|---|---|
| 凭据脱敏 | Pass | 报告路径沿用 FileOutcome.detailLines()（经 sanitize/CredentialRedaction）；`parallelPasswordNeverPrinted` 回归通过 |
| 异常消息脱敏 | Pass（极低风险） | 仅不可达防御 catch 未脱敏；同原 review 观察，非阻塞 |
| 输入校验 | Pass | `--parallel` 解析链未变更 |
| 依赖变更 | Pass | 仅 `java.util.concurrent`（ExecutorCompletionService），无第三方新增 |

## 必修项

无阻塞项。

## 结论

**Approve**

## 后续动作与复审范围

- QA 按 plan V1-V6 验收（含目标用例 ≥5 次复测 + 全量回归）。
- Manager 收到 QA Pass 后获取用户合并授权，在源分支 `fix-parallel-halt-race` 将 STATUS→`done` 与未入库 `review.md`/`qa-report.md` **一次提交**，再合入 `main`（rebase + FF）。
- 非 fast-forward / 冲突 → 返回 Manager 与用户决策。

## 复审（round 2）：按序流式输出

- 触发：QA 第 1 轮后用户实测 `--parallel 10 ./sqllogictest/test/`（622 文件）发现全程无输出（end-dump，决策 3 v1.0/1.1）。
- 变更提交：`49b1674` fix(cli): stream parallel status lines in input order。
- 范围：仅 `CliSession.executeParallel` 输出时机；`results[]`+`nextToPrint` 滑窗，收割后立即 drain 已就绪的按序结果。分派/收割/halt 逻辑未变。

| 检查项 | 结果 | 说明 |
|---|---|---|
| P1-1 输入顺序保持 | Pass | `nextToPrint` 严格递增；仅打印 `results[nextToPrint]`，顺序 = 输入顺序 |
| halt-skip 不变 | Pass | 未分派文件 `results[i]==null`，drain 跳过；目标用例仍 5/5 |
| 流式可见 | Pass | `./sqllogictest/test/evidence/`（12 文件，`--parallel 4`）实测 status line 在 +262/+266/+270ms… 陆续出现，非末尾一次性 |
| 计数/退出码/聚合收尾 | Pass | counters 在 drain 中累加；`printErrorSection`/`printTotal` 仍在所有 status line 之后；全量 343/0/0 |

无新阻塞项。先前观察（防御 catch 不可达、interrupt 路径 break、异常消息脱敏）均不变。

**复审结论：Approve**
