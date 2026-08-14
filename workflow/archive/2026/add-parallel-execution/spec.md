# Spec: add-parallel-execution

> 需求与规格（Plan 之前完成）。任务拆解见后续同目录 `plan.md`。
>
> **feature-id**：`add-parallel-execution` · **sub-feature-id**：`add-parallel-execution`（未拆分）

## 背景与目标

当前 `CliSession.execute()` 对文件列表执行顺序 `for` 循环——每文件完成后才启动下一个。大语料场景（select1/2/3 数千文件）下 wall-clock 时间 = 所有文件执行时间之和，受限于单文件 I/O 与 JDBC 延迟的累积。

本工作项引入 CLI 级文件粒度并行：`--parallel <N>` 允许最多 N 个独立文件同时执行，在 PG schema 隔离与 SQLite `:memory:` 隔离的保障下减少总耗时。

## 非目标

- 不并行单文件内的 statement / query 执行（仅文件粒度）
- 不引入分布式执行或多进程模型
- 不改变 `--engine` 的行为（PG schema 隔离、SQLite 连接模型均保持不变）
- 不承诺量化加速比指标（性能目标为宽松预期，见决策 8）

## 范围与可见行为

### CLI 表面

- 新增选项 `--parallel <N>`（N ≥ 1 整数）
- 不带 `--parallel`：完全保持现有顺序执行行为（字节一致，零回归）
- `--parallel 1`：等价于不带 `--parallel`（顺序执行）
- `--parallel <N>`（N ≥ 2）：文件级并行，最多 N 个文件同时执行
- `--parallel 0`：usage error，退出码 2
- `--parallel <非正整数>`（含负数、非数字）：usage error，退出码 2
- `--parallel` 与 `--override` 互斥：同时指定 → usage error，退出码 2
- `--parallel` 可与 `--color`、`--halt` 组合

### 输出契约（与顺序行为的差异）

| 行为 | 不带 `--parallel` | 带 `--parallel` |
|---|---|---|
| Status line 出现顺序 | 与命令行文件清单顺序一致 | 与命令行文件清单顺序一致（聚合后统一输出，非执行完成顺序） |
| 单文件输出原子性 | 天然原子 | 每个文件的 status line + error block 作为连续块，不与其他文件的输出交错 |
| 报告结构 | status line × N + Error section + TOTAL | 同左，仅执行方式变化 |
| 退出码 | hardError → 2；failed > 0 → 1；否则 0 | 同左 |
| 凭据脱敏 | 全局生效 | 不变 |

### `--halt` 在并行下的语义

顺序执行下 `--halt`：首个 FAILED 后 `break` 循环，已启动文件正常完成报告。

并行执行下 `--halt`：
- 任一文件判定为 FAILED 时，取消线程池中**尚未开始执行**的任务（已提交但未关联线程的文件）
- 等待**已开始执行**的任务完成（不 interrupt——DB 事务中断不安全）
- 已执行文件按真实结果报告（含 status line、error block）
- 未执行文件（被取消的）不在报告中出现，不计入 TOTAL 任何类别
- 退出码计算逻辑不变（hardError → 2；failed > 0 → 1）

默认（不带 `--halt`）：所有文件均执行并报告，退出码逻辑不变。

### `--override` 与 `--parallel` 互斥

并行执行中 override 回写源文件会与文件 parser 读产生竞态，且 golden-update 语义在并发下非幂等、不安全。互斥为硬约束。

### `--engine` 下的并行

| 引擎 | 并行行为 |
|---|---|
| `sqlite`（默认） | 每文件独立 JDBC Connection（`DriverManager.getConnection` 每次返回新连接）；`:memory:` db 隔离，并行安全 |
| `postgres` | 每文件独立 schema（`ggtest_<UUID>`），已线程安全；无全局连接池，连接数 = 并行文件数 × 每文件连接数 |

## 合同

### API / 接口

`--parallel <N>` 取值规则：

- N 必须是正整数（`≥ 1`）
- `--parallel 1`：行为与不带 `--parallel` 等价（顺序执行）
- `--parallel 0`：usage error，退出码 2，stderr 含 `[WHY]` 格式错误信息
- `--parallel <非正整数>`（负数、非数字字符串如 `abc`）：usage error，退出码 2
- N 的含义：最多同时执行的文件数（worker pool size）；不限制 N 的上界，但不保证超过可用处理器数时性能线性提升

互斥规则：

- `--parallel` 与 `--override` 互斥——同时指定 → usage error，退出码 2
- `--parallel` 可与 `--color <mode>`、`--halt`、`--hash-threshold <n>` 等组合

`--parallel` 不带值（即 `--parallel` 后无参数或后接另一个 `--xxx`）：usage error（missing value），退出码 2。

### 数据 / 状态

**聚合报告格式不变**（除 status line 顺序保证外）：

- Status line 格式：`<padded-path> .. [<TAG>] in <ms> ms`，与顺序行为一致
- Failed 文件的 error block（`at <file>:<line> : <why>` + diff body）紧接对应 status line 之后
- Error section（`Error: some test case failed:` + 文件清单）在所有 status line 之后、TOTAL 之前
- TOTAL 行格式：`TOTAL: passed=<n> failed=<n> skipped=<n>`（`--override` 不可用时 hidden；即 `--parallel` 下无 overridden 段）
- 凭据脱敏对所有输出行生效（与现状一致）

**并行聚合的线程安全约束**（不写实现，仅陈述行为合同）：

- `totalPassed` / `totalFailed` / `totalSkipped` 汇总结果必须等于顺序执行下相同文件集的汇总值（在相同输入、相同 DB 状态下）
- `hardError` 为 `true` 当且仅当至少一个文件发生 hard error
- `failedPaths` 列表包含所有实际执行且 FAILED 的文件路径

### 错误与约束

- `--parallel <N>` 中 N 非正整数 → usage error，退出码 2，stderr 含 `Error: usage` 和 `[WHY]` 说明
- `--parallel <N> --override` 同时 → usage error，退出码 2，stderr 含说明互斥
- PG 引擎：每个文件独立 schema（现有 `PostgresSchemaIsolation.prepare()` 使用 `UUID.randomUUID()`，已线程安全）；不限制 PG 连接上限（属运维配置，文档提示风险）
- SQLite 引擎：每个文件独立 JDBC 连接，无连接池复用
- 单文件的异常（parse error / connection failure / FatalDatabaseException）不传播到其他并行 worker；该文件标记为 hardError 并计入 `totalFailed`
- Worker 线程中的未捕获异常（非 FatalDatabaseException 的 RuntimeError）视为该文件 hardError，不终止 JVM 进程

## 验收（Given-When-Then）

### P0

- **P0-1 零回归——不带 `--parallel`**
  - Given 任意合法的 `ggtest` 调用（不包含 `--parallel`）
  - When 执行
  - Then stdout/stderr 输出与不带 `--parallel` 的现状**字节一致**（相同输入、相同引擎、相同环境）
  - 且退出码与现状一致

- **P0-2 `--parallel 1` 等价于不带**
  - Given 任意合法的 `ggtest` 调用，加入 `--parallel 1`
  - When 执行
  - Then stdout/stderr 输出与相同参数但不带 `--parallel` 的调用**字节一致**
  - 且退出码一致

- **P0-3 `--parallel 2` 多文件报告完整**
  - Given 至少 3 个互不依赖的 `.test` 文件（包含 pass、fail、skip 各种结果）
  - When `ggtest --parallel 2 --url jdbc:sqlite::memory: <files>` 执行
  - Then stdout 包含每个文件的 status line（`[PASSED]` / `[FAILED]` / `[SKIPPED]` 标签）
  - 且 failed 文件的 error block 完整（`at` 行 + diff body）
  - 且 Error section（若存在）列出所有失败文件
  - 且 TOTAL 行 `passed` / `failed` / `skipped` 计数与顺序执行同一文件集一致
  - 且退出码与顺序执行一致

- **P0-4 `--parallel 0` → usage error**
  - Given 任意合法调用，加入 `--parallel 0`
  - When 执行
  - Then 退出码 = 2
  - 且 stderr 包含 `Error: usage` 和 `[WHY]` 格式错误信息
  - 且 stdout 不含任何 status line 或 TOTAL

- **P0-5 `--parallel abc` → usage error**
  - Given 任意合法调用，加入 `--parallel abc`
  - When 执行
  - Then 退出码 = 2
  - 且 stderr 包含 usage error 信息

- **P0-6 `--parallel N --override` → usage error**
  - Given 任意合法调用，同时指定 `--parallel 2 --override`
  - When 执行
  - Then 退出码 = 2
  - 且 stderr 包含 usage error（指明互斥）
  - 且 stdout 不含 status line

- **P0-7 PG 引擎并行 schema 不冲突**
  - Given PG 可用（Q-Note：若无 PG 环境，本项标记为 Q-Note 缺口，不阻塞 Pass）
  - When `ggtest --parallel 2 --engine postgres <file_a> <file_b>` 执行
  - Then 两文件均正常完成（各自 `[PASSED]` 或 `[FAILED]`）
  - 且两文件使用不同 schema 名（可通过日志或测试验证）
  - 且无 "relation already exists" 或 schema 冲突错误

### P1

- **P1-1 并行时 status line 按输入文件顺序输出**
  - Given 文件清单 `[a.test, b.test, c.test]`，`b.test` 执行时间远小于 `a.test`
  - When `ggtest --parallel 3 --url jdbc:sqlite::memory: a.test b.test c.test` 执行
  - Then stdout 中 status line 出现顺序为 `a.test` → `b.test` → `c.test`（与命令行顺序一致）
  - 而非 `b.test` 先于 `a.test`（即不按执行完成早晚排序）

- **P1-2 `--parallel 2 --halt` 跳过未执行文件**
  - Given 3 个文件：`[pass.test, fail.test, pass2.test]`（依此次序）
  - When `ggtest --parallel 2 --halt --url jdbc:sqlite::memory: pass.test fail.test pass2.test` 执行
  - Then `fail.test` 判定为 `[FAILED]`
  - 且未执行的文件不在 stdout 中出现（无 status line，不计入 TOTAL）
  - 且已执行文件按真实结果报告
  - 且退出码按已执行文件计算结果（若有 hardError → 2；否则 failed>0 → 1）

- **P1-3 单 worker 异常不影响其他 worker（故障隔离）**
  - Given 3 个文件：`[fail.test, bad-parse.test, pass.test]`
  - When `ggtest --parallel 3 --url jdbc:sqlite::memory: fail.test bad-parse.test pass.test` 执行
  - Then `bad-parse.test` 的 parse error 不阻止 `fail.test` 和 `pass.test` 正常完成报告
  - 且 `pass.test` 显示 `[PASSED]`，`fail.test` 显示 `[FAILED]`
  - 且 TOTAL 计数包含全部 3 个文件的真实结果

- **P1-4 凭据脱敏在并行输出中不变**
  - Given 调用包含 `--password super-secret-credential --parallel 2` 且有 failed 文件（产生 error block）
  - When 执行
  - Then stdout 和 stderr 中均不出现 `super-secret-credential`

### P2（Nice-to-have，不阻塞 Pass）

- **P2-1 大语料多文件 wall-clock 加速**
  - Given 大语料场景（≥ 10 个独立文件，每个 ≥ 100 条 statement，总顺序执行时间 ≥ 5s）
  - When `ggtest --parallel <N> --url jdbc:sqlite::memory: <files>` 执行（N ≥ 2）
  - Then wall-clock 时间 < 顺序执行时间
  - Q-Note：本项不设量化阈值；无专用大语料 fixture 时标记为 Q-Note 缺口，不阻塞 Pass

## 决策记录（2026-08-11 用户已确认）

下列各项由用户会话于 2026-08-11 全部确认采纳，构成不可变更合同：

1. **`--parallel` 取值语义：** N=1 等价于顺序（零回归）；N=0 为 usage error；N≥2 为 worker pool size，最多 N 个文件并行。
2. **默认行为：** 不带 `--parallel` 完全保持现有顺序执行行为（零回归不变）。
3. **输出契约：** 聚合式——所有文件并行执行完成后，统一按输入文件顺序输出报告（与顺序行为的 status line 顺序一致，单文件 block 不交错）。
4. **`--halt` 并行语义：** 任一文件 FAILED → 取消已提交但未执行的任务 → 等待正在执行的任务完成（不 interrupt 运行中的 DB 操作）→ 仅报告已执行文件。
5. **`--override` 互斥：** `--parallel` 与 `--override` 互斥硬约束；同时指定 → usage error 退出码 2。
