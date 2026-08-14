# Plan: add-trace-flag

## 元信息

- 工作项标识: add-trace-flag
- 路径等级: standard
- Spec 门禁: skipped（无合同风险，行为明确）
- Design 门禁: skipped（技术选型明确：runner 注入 PrintStream）
- Review 门禁: required
- 最低验证层: L3（单元 + 集成）
- 验证命令: `mvn test`（零回归）+ `--trace` 手工验证（stderr 含 SQL，stdout 不受影响）

## 目标摘要

新增 `--trace` CLI 标志：开启后，每条 SQL 在执行前打印到 stderr。stdout 报告格式/退出码/既有行为零回归。

## 任务拆解（TDD）

1. **T1: CLI 解析链路**（完成条件：`--trace` 解析到 `CliOptions.trace()==true`）
   - `CliArgumentParser.parse()`：新增 `case "--trace" -> trace = true;`（boolean flag，仿 `--halt`）
   - `ParsedArguments`：新增 `boolean trace` 字段
   - `RuntimeConfigResolver.resolve()`：透传 `trace`
   - `CliOptions`：新增 `boolean trace` 字段
   - `Main.printHelp()`：新增 `--trace` 行
   - TDD：`CliArgumentParserTest` / `RuntimeConfigResolverTest` 加 `--trace` 用例

2. **T2: runner trace 注入**（完成条件：trace 开启时 SQL 打印到指定 stream）
   - `SqlLogicTestRunner`：新增 `private PrintStream traceStream`（nullable）+ `setTraceStream(PrintStream)` setter
   - `runStatement`（line 207）/ `runQuery`（line 237）：executor 调用前，若 `traceStream != null` 则 `traceStream.println(sql)`
   - TDD：`SqlLogicTestRunnerTest` 加 trace 用例（FakeDatabaseExecutor，验证 trace stream 收到 SQL）

3. **T3: FileRunner 接线**（完成条件：`--trace` 开启时 stderr 含每条 SQL）
   - `FileRunner.run()`：构造 runner 后 `runner.setTraceStream(options.trace() ? err : null)`
   - 集成测试：`MainOrchestrationTest` 加一条用例——`--trace` 运行 pass.test，stderr 含 `CREATE TABLE` / `INSERT` / `SELECT`；stdout 仍含 `[PASSED]` + TOTAL

4. **T4: README + 全量回归**
   - README：选项表加 `--trace` 行
   - `mvn test` 全量通过

## 触碰路径

| 文件 | 改动 |
|---|---|
| `cli/CliArgumentParser.java` | `--trace` case |
| `cli/ParsedArguments.java` | `trace` 字段 |
| `cli/RuntimeConfigResolver.java` | 透传 `trace` |
| `cli/CliOptions.java` | `trace` 字段 |
| `cli/Main.java` | printHelp |
| `cli/FileRunner.java` | `setTraceStream` |
| `runner/SqlLogicTestRunner.java` | `traceStream` + setter + 打印 |
| `test/.../CliArgumentParserTest.java` | trace 解析用例 |
| `test/.../RuntimeConfigResolverTest.java` | trace 透传用例 |
| `test/.../SqlLogicTestRunnerTest.java` | trace 打印用例 |
| `test/.../MainOrchestrationTest.java` | trace 集成用例 |
| `README.md` | 选项表 |

## 验收

| ID | 要求 | 结果 |
|---|---|---|
| V1 | `--trace` 解析为 CliOptions.trace=true | |
| V2 | runner trace 开启时 SQL 打印到 stream | |
| V3 | `--trace` 运行文件：stderr 含 SQL，stdout 不受影响 | |
| V4 | 不带 `--trace` 零回归：`mvn test` 全绿 | |
| V5 | Review Approve | |
| V6 | QA Pass | |

## 文档影响

| 类别 | 更新 |
|---|---|
| 用户文档 | README：`--trace` 选项 |
| 开发文档 | N/A |
| 运维文档 | N/A |

## 交接顺序

Developer(TDD) → Reviewer(Approve) → QA(Pass) → 用户合并授权 → done 提交 → 合入 main
