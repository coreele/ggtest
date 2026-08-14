# Plan: add-parallel-execution

## 元信息

- 工作项标识: add-parallel-execution
- sub-feature-id: add-parallel-execution（未拆分）
- 依据 Spec: workflow/archive/2026/add-parallel-execution/spec.md
- 依据 Design: workflow/archive/2026/add-parallel-execution/design.md
- 依据 UI: N/A（无 UI 变更）
- 路径等级: standard
- Review 门禁: required
- 最低验证层: L3（单元测试 + 集成测试）
- 验证命令: `mvn test`
- 预期证据: 所有测试通过（含新增测试）；`MainOrchestrationTest` 零回归（现有用例不改动）

## 适用工程规范

- `workflow/agents/standards/documentation.md`
- `workflow/agents/standards/git.md`
- `workflow/agents/standards/quality.md`
- `workflow/agents/standards/security.md`

## 目标摘要

在 `CliSession` 中增加 `--parallel <N>` 并行执行能力：文件级并行，最多 N 个文件同时执行；输出聚合后按输入顺序统一报告；支持 `--halt` 在并行下的取消语义；`--parallel` 与 `--override` 互斥；不带 `--parallel` 时零回归。

## 任务拆解

1. **T1: `--parallel` CLI 解析全链路**（完成条件：`--parallel 2` 正确解析为 `CliOptions.parallel=2`，`--parallel 0` / `--parallel abc` 抛出 UsageException）
   - `CliArgumentParser.parse()` 新增 `--parallel` 解析 + 值校验（参考 `--hash-threshold` 的 `Integer.parseInt` 模式）
   - `ParsedArguments` 新增 `Optional<Integer> parallel` 字段
   - `CliOptions` 新增 `int parallel` 字段（默认 0 = 未指定）
   - `RuntimeConfigResolver.resolve()` 透传 `parallel`
   - `Main.printHelp()` 新增 `--parallel` 帮助行

2. **T2: `ParallelExecutor` 组件**（完成条件：可提交 N 个 Callable，返回按提交顺序的 Future 列表；shutdown 后不再接受新任务；awaitTermination 正确等待完成）
   - 新建 `src/main/java/com/ggtest/cli/ParallelExecutor.java`
   - 封装 `ExecutorService`（`Executors.newFixedThreadPool`）
   - 提供 `submitAll(List<Callable<FileOutcome>>)` → `List<Future<FileOutcome>>`
   - 提供 `shutdown()` + `awaitTermination(long, TimeUnit)` 方法
   - package-private（不暴露到 `com.ggtest.cli` 包外）

3. **T3: 并行结果聚合**（完成条件：从 `List<Future<FileOutcome>>` 按序收集所有结果，错误隔离）
   - 在 `CliSession` 内部（或并行执行方法内）实现聚合逻辑
   - 按文件索引顺序 `Future.get()` 收集 `FileOutcome`
   - worker 异常已在 Callable 内转为 `hardError FileOutcome`；get() 不再包裹异常处理

4. **T4: `CliSession.executeParallel()` 新方法**（完成条件：并行执行路径能正确运行多文件并输出聚合报告）
   - `execute()` 增加条件分支：`options.parallel() > 1` → `executeParallel()`，else 走原顺序路径
   - `executeParallel()` 实现：创建 ParallelExecutor → 提交 Callable → 收集结果 → 按序输出 → shutdown
   - 每个 worker Callable 独立创建 `FileRunner` 实例
   - 输出逻辑与顺序路径一致（status line → error block → Error section → TOTAL）
   - 退出码计算与顺序路径一致（hardError → 2；failed>0 → 1；else 0）

5. **T5: `--halt` 取消逻辑**（完成条件：任一文件 FAILED 时取消尚未启动的任务，不中断运行中任务）
   - 在 `executeParallel()` 中，收集结果时检测到 FAILED 且 `options.halt()` → 遍历剩余未完成的 Future 调用 `cancel(false)`
   - 仅已执行（完成或被 cancel 前已启动）的文件参与报告
   - 不调用 `shutdownNow`，等待运行中任务自然完成后 `shutdown()`

6. **T6: `--override` 互斥检查**（完成条件：同时指定 `--parallel 2 --override` 抛出 UsageException，退出码 2）
   - 在 `CliArgumentParser.parse()` 中，解析完成后检查 `parallel.isPresent() && override` → UsageException

7. **T7: 适配 SG 集成测试**（完成条件：PG 并行执行下两文件 schema 不冲突，各自正常完成）
   - 新增或扩展 `PostgresCliIntegrationTest`：`--parallel 2 --engine postgres` 执行两个独立文件，验证 schema 隔离
   - 无 PG 环境时标记 Q-Note 缺口（不阻塞 Pass）

8. **T8: README (en + zh) + `--help` 文档**（完成条件：README 新增 `--parallel` 选项说明与并行语义；`--help` 输出包含 `--parallel` 行）
   - README.md（en）：新增 `--parallel` 选项描述
   - README_zh.md：新增中文说明
   - `--help` 行已在 T1 完成

9. **T9: 测试套件**（完成条件：覆盖 Spec P0/P1 全部验收项，CI `mvn test` 全部通过）
   - T9-1: `CliArgumentParserTest` — `--parallel` 解析（正整数/0/负数/非数字/missing value）、`--parallel` + `--override` 互斥
   - T9-2: `RuntimeConfigResolverTest` — `parallel` 透传
   - T9-3: `MainOrchestrationTest` — 零回归（现有用例不加 `--parallel` 不改动）、`--parallel 1` 等价、`--parallel 2` 多文件报告完整、status line 顺序、halt 跳过未执行文件、故障隔离（单 worker 异常不影响其他）、凭据脱敏
   - T9-4: PG 集成测试 — schema 隔离（T7）

## 依赖与顺序

```
T1 (CLI解析)
  ├─→ T2 (ParallelExecutor) + T3 (聚合逻辑)
  │     ├─→ T4 (executeParallel)
  │     │     ├─→ T5 (halt取消)
  │     │     └─→ T9 (测试套件，部分依赖 T4-T6)
  │     └─→ . . .
  ├─→ T6 (override互斥)
  └─→ T8 (--help 行 + README)
T7 (PG测试) 可并行于 T4-T6
T9 依赖 T1-T7 实现完成后执行
T8 (README) 在 T1 --help 行完成后可开始，最终在所有功能稳定后定稿
```

## 触碰路径

| 文件 | 新建/修改 | 改动类型 |
|---|---|---|
| `src/main/java/com/ggtest/cli/CliArgumentParser.java` | 修改 | 新增 `--parallel` case + 互斥检查 |
| `src/main/java/com/ggtest/cli/ParsedArguments.java` | 修改 | 新增 `Optional<Integer> parallel` 字段 |
| `src/main/java/com/ggtest/cli/CliOptions.java` | 修改 | 新增 `int parallel` 字段（默认 0） |
| `src/main/java/com/ggtest/cli/RuntimeConfigResolver.java` | 修改 | 透传 `parallel` |
| `src/main/java/com/ggtest/cli/CliSession.java` | 修改 | 新增 `executeParallel()`；`execute()` 增加分支 |
| `src/main/java/com/ggtest/cli/ParallelExecutor.java` | 新建 | 线程池封装 |
| `src/main/java/com/ggtest/cli/Main.java` | 修改 | `printHelp()` 新增 `--parallel` 行 |
| `src/test/java/com/ggtest/cli/CliArgumentParserTest.java` | 修改 | 新增并行解析测试用例 |
| `src/test/java/com/ggtest/cli/RuntimeConfigResolverTest.java` | 修改 | 新增 parallel 透传测试 |
| `src/test/java/com/ggtest/cli/MainOrchestrationTest.java` | 修改 | 新增并行执行测试用例；零回归检查（现有用例不修改） |
| `src/test/java/com/ggtest/cli/PostgresCliIntegrationTest.java` | 修改 | 新增并行 PG schema 隔离测试 |
| `README.md` | 修改 | 新增 `--parallel` 说明 |
| `README_zh.md` | 修改 | 新增 `--parallel` 说明 |

## 验收与验证

| ID | 要求或命令 | 预期证据 | 结果（实施后填） |
|---|---|---|---|
| P0-1 | `mvn test` — 现有 MainOrchestrationTest 全部通过（不加 `--parallel` 参数） | 零回归；所有现有测试通过 | |
| P0-2 | 新增测试：`--parallel 1` 行为与不带一致 | 输出字节一致，退出码一致 | |
| P0-3 | 新增测试：`--parallel 2` 多文件（pass/fail/skip）报告完整 | status line × N + error block + Error section + TOTAL 计数一致 | |
| P0-4 | 新增测试：`--parallel 0` → usage error | 退出码 2；stderr 含 `Error: usage` + `[WHY]` | |
| P0-5 | 新增测试：`--parallel abc` → usage error | 退出码 2；stderr 含 usage error | |
| P0-6 | 新增测试：`--parallel 2 --override` → usage error | 退出码 2；stderr 含互斥说明 | |
| P0-7 | Q-Note: `mvn test -Dtest=PostgresCliIntegrationTest#parallelSchemaIsolation`（需 PG 可用） | 两文件不同 schema，无冲突错误 | |
| P1-1 | 新增测试：3 文件并行，验证 status line 顺序与命令行一致 | a.test → b.test → c.test | |
| P1-2 | 新增测试：3 文件 + `--parallel 2 --halt`，fail 文件在中间 | 未执行文件不在 stdout；已执行文件按真实结果 | |
| P1-3 | 新增测试：bad-parse + pass + fail 并行 | parse error 不阻止其他文件正常完成；TOTAL 含全部 3 个文件 | |
| P1-4 | 新增测试：`--password secret --parallel 2` | stdout/stderr 不含 secret | |
| P2-1 | Q-Note：大语料加速（可选，无 fixture 时不阻塞） | wall-clock 减少 | |

## 验证缺口

| 项 | 原因 | 风险 | 恢复条件 |
|---|---|---|---|
| P0-7 | 无 PG 环境 | 低 — PostgresSchemaIsolation 已单测验证线程安全（UUID schema 名） | 配置 PG 实例（`GGTEST_PG_URL`）后执行 |
| P2-1 | 无专用大语料 fixture | 低 — 逻辑正确性由 P0-3/P1-1 覆盖 | 准备大语料 fixture 后复测 |

## 文档影响

| 类别 | 更新路径或 N/A 理由 |
|---|---|
| 开发文档 | README.md + README_zh.md：新增 `--parallel` 选项说明与并行语义 |
| 用户文档 | 同上 README（用户可见 CLI 表面变更） |
| 运维文档 | N/A — 无部署/排障变更（PG 连接上限已在 README 提示） |

## 交接顺序

1. Developer 实施与开发者验证 →
2. Reviewer（Review 门禁 required）→
3. QA 验收 →
4. 用户合并授权 → Manager `done` 一次提交 → 合入

## 修订记录

| 日期 | 摘要 |
|---|---|
| 2026-08-11 | 初稿：基于 Spec v1.0 + Design v1.0，任务拆解 T1-T9 |
