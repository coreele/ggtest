# Plan: feat-override-results

## 元信息

- 工作项标识: feat-override-results（未拆分，sub-feature-id = feature-id）
- 依据 Spec: [`workflow/docs/features/feat-override-results/spec.md`](./spec.md)（**唯一行为合同**）
- 依据 Design: [`./design.md`](./design.md)
- 路径等级: `full`
- Review 门禁: `required`（full）
- 最低验证层: **L2**（单元测试为主 + 临时目录 fixture 字节断言）
  - 理由：override 的判定 / 透传 / 重写 / 原子写均为进程内逻辑，无新外部系统。in-scope 判定在 `SqlLogicTestRunner` 单元层可覆盖；文本重写与原子写用 `Files.createTempDirectory` fixture 做字节级断言（含 CRLF / 文件尾换行 / 只读 FS / 写回失败原文件不变）；端到端经 `:memory:` SQLite + `Main.run` 覆盖状态行 / `TOTAL` / 退出码。PG 不必需——override 机制与引擎无关，sqlite 已能复现 in-scope mismatch 与 statement error 消息失配；PG 仅作可选冒烟。
- 验证命令:
  - 全量：`mvn -q test`
  - 定向（开发迭代）：`mvn -q test -Dtest=SqlLogicTestParserTest,SqlLogicTestRunnerTest,OverrideWriterTest,FileRunnerTest,CliArgumentParserTest,RuntimeConfigResolverTest,CliReportAcceptanceTest,MainOrchestrationTest`
  - 构建打包（确认无回归）：`mvn -q clean package`
  - 冒烟（可选，需先 `package`）：`./bin/ggtest --engine sqlite --url jdbc:sqlite::memory: <fixture>`

## 适用工程规范

- [文档工程](../../standards/documentation.md)
- [Git 协作](../../standards/git.md)（仅 Git 工作区）
- [质量与验证](../../standards/quality.md)
- [安全](../../standards/security.md)

## 目标摘要

依据已冻结 Spec，为 `ggtest` CLI 增加布尔开关 **`--override`**（默认关闭）。开启后：范围内的 mismatch（query result mismatch；`statement error <msg>` 消息子串失配）用**实际输出**重写源 `.slt` 中该记录的 expected 区间，该记录计 **overridden**（非 FAILED），`[OVERRIDDEN]` tag + `TOTAL ... overridden=N`；范围外 mismatch 仍 FAILED；无 mismatch 的文件不写、不改 mtime；文件其余字节逐字节保留；原子写，失败不损原文件。退出码优先级（`0`/`1`/`2`）不变。不改 parser / 比较 / 规范化语义。技术选型与模块改动面见 `design.md`。

## 任务拆解

> 每项「完成条件」= Reviewer/QA 可核验的客观证据。Spec 合同为权威；下列实现路径为建议，Developer 可在不变合同前提下调整内部结构。**TDD：每项先写/扩失败测试，再实现**。

### T1 Parser 携带 expected 区间（D1）
- `SqlLogicTestParser.parseQuery`：在消费 `----` 头处记录 `expectedHeaderLine`（`peekLineNumber()`），在 `readExpectedResults` 结束后记录 `expectedBodyEndLine`（最后消费行号；空块时 `bodyStart > end`）。
- `SqlLogicTestParser.parseStatement`：`expectation == ERROR && 有消息` 时，在**去 CR 的原始 header 行**上计算 `errorMsgStartColumn`（`error` token 后跳过空白的位置），否则 `-1`。
- `QueryRecord` / `StatementRecord`：canonical 构造器加新区间字段；**新增保留旧签名的次构造器**委托（缺省哨兵：query `0/0`、statement `-1`），使 `SqlLogicTestRunnerTest` / `ReportWriterTest` 既有构造点零改动。
- 完成条件：`SqlLogicTestParserTest` 新增/扩展用例断言 query 的 `expectedHeaderLine`/`expectedBodyEndLine`（含多记录、execute-only 为 0、空块边界）与 statement error 的 `errorMsgStartColumn`（含前导空白、多空格分隔、`statement error` 无消息时 -1）；既有 parser 用例全绿；既有 `new QueryRecord/StatementRecord` 调用点编译通过。

### T2 Runner in-scope 判定 + golden 透传（D2/D8）
- `RecordOutcome` 加 `OVERRIDDEN`；`RecordResult` 加 `Optional<String> overrideText` + 工厂 `overridden(record, text)`；`FileRunResult` 加 `overriddenCount()`。
- `SqlLogicTestRunner` 加构造参数 `boolean overrideEnabled`（新增重载，旧签名委托 `false`，保持兼容）。
- `runQuery`：`overrideEnabled` 时分支——仅「失败集合只含 result mismatch 且 `hasExpectedResults`」→ `OVERRIDDEN`（载荷 = actualView 行以 `\n` 拼接）；label 冲突 / 执行失败 / 类型签名错 → 维持 FAILED。
- `runStatement`：`overrideEnabled` 时，「实际失败 + expectedMsg 非空且子串失配」→ `OVERRIDDEN`（载荷 = 实际 error summary）；极性翻转 → FAILED。
- 不改 `ResultComparer` / `ResultSorter` / actualView 计算。
- 完成条件：`SqlLogicTestRunnerTest` 新增用例（`overrideEnabled=true`）：纯 query mismatch → `OVERRIDDEN` 且 `overrideText` = actualView；query mismatch + label 冲突 → FAILED；query 执行失败 / 类型签名错 → FAILED；statement error 消息失配 → `OVERRIDDEN`；statement ok 失败 / statement error 实际成功 → FAILED；execute-only 不比较；`overrideEnabled=false` 时全部维持 FAILED（不变性）。

### T3 `OverrideWriter`（新增）— 文本重写 + 原子落盘（D4/D7）
- 新增 `cli.OverrideWriter`：
  - `String rewrite(String content, List<Override> overrides)`：检测 EOL（`\r\n` 存在则用之，否则 `\n`）、按行表切分（保留文件尾换行信息）、按记录类型取区间（query→body 行区间替换；statement→header 行 `substring(0,errorMsgStartColumn)` 前缀保留 + 新消息）、多 override **行号倒序**应用、用检测 EOL 重连。
  - `void writeAtomically(Path target, String newText)`：同目录 temp + `Files.move(ATOMIC_MOVE)`，`AtomicMoveNotSupportedException` 回退 `REPLACE_EXISTING`，失败 `try-finally` 删 temp 并抛 `IOException`。
  - `Override` 值类型 = `(SqlTestRecord record, String newText)`（或等价）。
- 完成条件：新增 `OverrideWriterTest`（临时目录 fixture，字节级断言）：
  - 单 query override：`----` 头与块后空行保留、body 行被 actualView 替换、**其余字节逐位相同**；
  - statement error override：`statement error ` 前缀与极性保留、消息被替换、SQL 与其余不变；
  - 多 override 同文件一次重写（行号倒序正确）；
  - EOL 保留（LF 与 CRLF 各一）、文件尾换行保留（有/无）；
  - 只读文件（`setReadable(false)` 或只读目录）→ 抛异常且**原文件字节不变、仍可原样解析**（P1-4/P1-7）；
  - 模拟写回失败后原文件完整（P1-7）。

### T4 `FileRunner` 接入写回 + 桶判定（D3）
- `FileBucket` 加 `OVERRIDDEN`；`FileOutcome.overridden()`（无 hardError）。
- `FileRunner.runWithExecutor`：`options.override()` 时按 D3 流程——aborted 不写；收集 OVERRIDDEN；非空则现读原文 → `OverrideWriter.rewrite` → `writeAtomically`，失败转 `hardFailure`（细节含「写回失败」+ 列出本应 override 的记录）；桶判定（hardFailure→FAILED/hardError；`failedCount>0`→FAILED 但已写回；`overriddenCount>0`→OVERRIDDEN；仅 skipped→SKIPPED；否则 PASSED）；无 OVERRIDDEN 不写、不改 mtime。
- 完成条件：`FileRunnerTest` 新增用例（sqlite `:memory:` + 临时 fixture）：
  - override 开 + 单 query mismatch → 文件被改写、outcome = OVERRIDDEN；
  - override 关 → FAILED + **文件未被改写（mtime 不变）**（P0-1）；
  - aborted → **不写文件**（P1-6）；
  - 写回失败（只读 fixture）→ hardError + 原文件不变（P1-4）；
  - 剩余 scope 外 FAILED 同文件 → 写回 override 部分 + 桶 FAILED（P0-6/P1-2）。

### T5 CLI 接线（D5）
- `CliArgumentParser`：`case "--override" -> override = true;`；`ParsedArguments` 加 `boolean override`。
- `RuntimeConfigResolver`：`parsed.override()` 透传 `CliOptions`（CLI-only，不读 env/.env）。
- `CliOptions`：加 `boolean override`；`toString` 补 `override=...`（脱敏风格不变）。
- 完成条件：
  - `CliArgumentParserTest`：`--override` 开启；`--override --override` ≡ 单次（非 usage 错）；`-override`、`--over` 各抛 `UsageException`（消息含 unknown option 语义）。
  - `RuntimeConfigResolverTest`：`--override` → `CliOptions.override()==true`；未传 → `false`；`CliOptions.toString()` 不含 password 明文（沿用既有断言）。
  - 经 `Main.run`：`-override`/`--over` → 退出码 `2`、stdout 无 `[PASSED]`/`TOTAL`、不连库、不写文件（P0-7，参照既有 `missingUrlExitsTwoWithoutRunning` 断言模式）。

### T6 报告 / 退出码（D6）
- `ReportStyle.overriddenTag()`（`[OVERRIDDEN]`，配色与四态可辨）；`ReportWriter.printTotal` 支持「override 开启时追加 `overridden=N`」。
- `CliSession`：状态行 `switch` 加 `case OVERRIDDEN`（overridden tag + 计时，**不计 totalFailed**，计 totalOverridden，不打印失败细节）；退出码逻辑顺序不变。
- 完成条件（`CliReportAcceptanceTest` / `MainOrchestrationTest` 新增/扩展）：
  - override 开 + overridden 文件 → 状态行含 `[OVERRIDDEN]`、`TOTAL` 含 `overridden=N`、退出码 `0`（P0-2 报告面）；
  - override 开 + override 文件 A + scope 外 FAILED 文件 B → A 不计 failed、B FAILED、`TOTAL` 含 `overridden=`、退出码 `1`（P0-6）；
  - **默认关闭** → `TOTAL` 行**无** `overridden=` 段（与现状字节级一致）（P0-1）；
  - override 开 + 写回失败 → 退出码 `2`（P1-4 退出码面）。

### T7 组合与回归（覆盖 P0/P1 集成面）
- 完成条件（集成用例，sqlite `:memory:` + 临时 fixture，断言文件字节 / mtime / stdout / 退出码）：
  - **P0-2/P0-3 幂等**：override 改写后以相同 argv（含与不含 `--override`）再跑 → 全 PASSED、**文件不再被改写**（内容与 mtime 稳定）。
  - **P0-4 无 mismatch 不写**：全 PASSED + override → 文件内容与 mtime 均不变、退出码 `0`。
  - **P0-5 文件其余字节不变**：多记录文件（注释 / 空白 / 多条 statement/query）仅一条 query mismatch + override → 改写前后逐字节 diff 仅该 expected body 行。
  - **P1-1 statement error 重写**：`statement error <oldmsg>` 消息失配 + override → header 行消息被实际 summary 替换、极性仍 `error`、SQL 与其余不变、该记录非 FAILED。
  - **P1-2 scope 外不覆盖**：label conflict / query 执行失败 / 类型签名错 / statement ok 失败 + override → 该记录仍 FAILED、其源区间不被改写（同文件另有 in-scope 时仅后者被写回）。
  - **P1-3 execute-only 不生成 expected 块**：无 `----` query + override → 不比较、不插入、文件不变。
  - **P1-5 `--override --halt`**：单文件先 in-scope query mismatch 后 scope 外 FAILED 再有记录 + `--override --halt` → 第一条被 override 并写回、第二条 FAILED 触发 `--halt`（其后记录不执行、不以假 FAILED 出现）、退出码 `1`。
  - **P1-6 致命中止不写**：触发 `FatalDatabaseException`（aborted）+ override → 文件不被改写、报告硬错误、退出码 `2`。

### T8 README（用户文档，P1-8）
- `README.md` / `README.zh-CN.md`：CLI synopsis 行补 `[--override]`；选项表新增 `--override` 行，简述 golden-update 语义（用实际输出重写源文件期望结果；范围：query result mismatch / statement error 消息失配；默认关闭；不改 parser/比较语义）。
- 完成条件：两份 README 选项表均含 `--override` 且语义与 Spec §CLI 接口一致。

## 依赖与顺序

- **无依赖（可并行起手）**：T1（parser 区间）、T2（runner 判定，仅依赖 `RecordOutcome`/`RecordResult`/`FileRunResult` 模型层）、T5（CLI 接线，纯 plumbing）。
- T3（`OverrideWriter`）依赖 **T1**（消费 record 上的区间字段）。
- T4（`FileRunner` 写回）依赖 **T2**（OVERRIDDEN 结果）+ **T3**（写回器）+ **T5**（`options.override()`）。
- T6（报告/退出码）依赖 **T2**（`overriddenCount`）+ **T4**（`FileBucket.OVERRIDDEN`/`FileOutcome`）。
- T7（集成/回归）依赖 **T1–T6**。
- T8（README）与代码无强依赖，Review 前必须完成。
- 建议实施顺序：**T1 ‖ T2 ‖ T5 → T3 → T4 → T6 → T7 → T8**。

## 触碰路径（文件级）

- 解析与模型（T1）：`src/main/java/com/ggtest/parser/SqlLogicTestParser.java`、`src/main/java/com/ggtest/model/QueryRecord.java`、`src/main/java/com/ggtest/model/StatementRecord.java`
- runner 判定（T2）：`src/main/java/com/ggtest/runner/RecordOutcome.java`、`src/main/java/com/ggtest/runner/RecordResult.java`、`src/main/java/com/ggtest/runner/FileRunResult.java`、`src/main/java/com/ggtest/runner/SqlLogicTestRunner.java`
- 写回（T3）：`src/main/java/com/ggtest/cli/OverrideWriter.java`（**新增**）
- 文件接入（T4）：`src/main/java/com/ggtest/cli/FileRunner.java`、`src/main/java/com/ggtest/cli/FileOutcome.java`
- CLI 接线（T5）：`src/main/java/com/ggtest/cli/CliArgumentParser.java`、`src/main/java/com/ggtest/cli/ParsedArguments.java`、`src/main/java/com/ggtest/cli/RuntimeConfigResolver.java`、`src/main/java/com/ggtest/cli/CliOptions.java`
- 报告/退出码（T6）：`src/main/java/com/ggtest/cli/ReportStyle.java`、`src/main/java/com/ggtest/cli/ReportWriter.java`、`src/main/java/com/ggtest/cli/CliSession.java`
- 用户文档（T8）：`README.md`、`README.zh-CN.md`
- 测试（T1–T7）：
  - `src/test/java/com/ggtest/parser/SqlLogicTestParserTest.java`
  - `src/test/java/com/ggtest/runner/SqlLogicTestRunnerTest.java`
  - `src/test/java/com/ggtest/cli/OverrideWriterTest.java`（**新增**）
  - `src/test/java/com/ggtest/cli/FileRunnerTest.java`
  - `src/test/java/com/ggtest/cli/CliArgumentParserTest.java`
  - `src/test/java/com/ggtest/cli/RuntimeConfigResolverTest.java`
  - `src/test/java/com/ggtest/cli/CliReportAcceptanceTest.java`
  - `src/test/java/com/ggtest/cli/MainOrchestrationTest.java`
  - 新增 fixture（建议 `src/test/resources/fixtures/cli/override/`）：单 query mismatch、多记录（含注释/空白）、statement error 消息失配、含 label 冲突、execute-only、`--halt` 组合、aborted 触发（如 `halt` 前置致命 SQL）

## 验收与验证

> 合同权威：`spec.md` §验收（P0-1…P0-7、P1-1…P1-8）。下列把每条映射到验证手段与预期证据。

| 验收 | 验证手段（任务） | 预期证据 |
|---|---|---|
| **P0-1 默认关闭** | T4（override 关 → 不写）、T6（默认 TOTAL 无 `overridden=`）、T7（不变性） | 文件未改写、退出码 `1`、`TOTAL` 无 `overridden=` 段 |
| **P0-2 query mismatch 被重写** | T2 + T3 + T4 + T7 | expected 块被 actualView 替换、`----` 保留、记录非 FAILED、文件其余字节不变、退出码 `0` |
| **P0-3 重跑幂等** | T7 | 写回后再跑全 PASSED、文件不再改写 |
| **P0-4 无 mismatch 不写** | T4 + T7 | 内容与 mtime 均不变、退出码 `0` |
| **P0-5 文件其余字节不变** | T3（字节断言）+ T7（多记录 diff） | 仅 expected body 行变化 |
| **P0-6 退出码优先级** | T6 + T7 | A override 非 FAILED、B FAILED、退出码 `1` |
| **P0-7 选项解析** | T5 | 精确 `--override` 开启；`-override`/`--over` → 退出码 `2`、不连库、不写文件 |
| **P1-1 statement error 重写** | T2 + T3 + T7 | header 消息被替换、极性保留、SQL 不变、非 FAILED |
| **P1-2 scope 外不覆盖** | T2 + T7 | 仍 FAILED、其源区间不被改写 |
| **P1-3 execute-only 不生成** | T2 + T7 | 不比较、不插入、文件不变 |
| **P1-4 只读 FS** | T3 + T4 + T6 | 写回失败 → 硬错误、退出码 `2`、报告含「写回失败」、原文件未损 |
| **P1-5 `--override --halt`** | T7 | 第一条 override 并写回、第二条 FAILED 触发 `--halt`、退出码 `1` |
| **P1-6 致命中止不写** | T4 + T7 | 文件不被改写、报告硬错误、退出码 `2` |
| **P1-7 原子性（可观察）** | T3 | 写回失败后原文件完整可原样解析 |
| **P1-8 文档** | T8 | 两份 README 选项表含 `--override` |

预期证据：`mvn -q test` 全绿；`mvn -q clean package` 成功；两份 README 选项表含 `--override`（grep 可验证）。开发者验证结果记入 `workflow/docs/features/feat-override-results/dev-notes.md`。

## 验证缺口

- 无已知阻塞缺口。`mvn -q test` 所需为 JDK 17 + Maven + 内存 SQLite，仓库已具备。
- **PG 专有 statement error 消息文本** 不在最低验证层：override 的 in-scope 判定 / 重写机制与引擎无关，sqlite `:memory:` 已能复现 query result mismatch 与 statement error 消息失配（可用故意失配的 fixture）。如需 PG 实测消息文本，须设 `GGTEST_PG_URL`，属可选增强而非必需。
- 若 CI/本地因 Maven 仓库不可达、JDK 缺失等导致 `mvn test` 无法执行：记录具体障碍到 `dev-notes.md` 与工作项记录「阻塞原因/恢复条件」，评估风险（无法证明 P0/P1 通过），声明恢复条件（环境就绪后补跑 `mvn -q test` 与定向用例）。补跑完成前不得进入 QA Pass 或合并。

## 文档影响

| 类别 | 更新路径或 N/A 理由 |
|---|---|
| 开发文档 | 代码 Javadoc 更新点：`CliArgumentParser`（选项清单）、`CliOptions`（新字段）、`SqlLogicTestRunner`（override 判定与 in-scope 规则）、`RecordOutcome`/`RecordResult`/`FileRunResult`（OVERRIDDEN）、`QueryRecord`/`StatementRecord`（区间字段语义）、`FileRunner`/`CliSession`（override 写回与桶）、新增 `OverrideWriter`（重写/原子写契约）。无独立开发文档需新增。 |
| 用户文档 | `README.md`（§CLI synopsis + 选项表）、`README.zh-CN.md`（同）新增 `--override` 行；简述 golden-update 语义（默认关闭；范围内 mismatch 用实际输出重写源文件 expected 区间；文件其余字节保留；原子写）。 |
| 运维文档 | N/A。`--override` 为面向开发者/用户的运行时开关，不改变部署、监控、备份恢复、排障流程；退出码集合与优先级不变，既有运维告警阈值不受影响。 |

## 进入 QA 的条件

- Review 门禁为 `required`（full）：进入 QA 前必须取得 Reviewer `Approve`。
- Reviewer 须按 `workflow/docs/standards/quality.md` §3 检查：
  - **测试有效性**——覆盖 P0/P1 关键路径、无恒真断言、含边界（CRLF、文件尾换行、空 expected 块、多记录、aborted、只读）；
  - **文档影响**——与本 Plan「文档影响」一致（两份 README）；
  - **安全影响**——override 载荷仅为 actualView/error summary，不引入新脱敏面（文件操作属「文件操作」触发面：审查 `OverrideWriter` 路径遍历/越权写风险——仅写 runner 已解析的目标路径，无用户可控路径拼接；结论写入 `review.md`）。
- 取得 Approve 后，由 Manager 将状态推进至 QA 调度；QA 依据 Spec P0/P1 + 本 Plan 最低验证层独立验收，结论写入 `workflow/docs/features/feat-override-results/qa-report.md`。

## 交接顺序

1. **实施（Developer）**：按 T1‖T2‖T5 → T3 → T4 → T6 → T7 → T8 实现；执行 `mvn -q test` 与 `mvn -q clean package`；将验证结果与偏差记入 `workflow/docs/features/feat-override-results/dev-notes.md`。
2. **Review（Reviewer）**：依本 Plan「验收」「文档影响」「进入 QA 的条件」逐项核验，给出 Approve / Request changes；full 路径 Approve 是 QA 前置。
3. **QA**：Reviewer Approve 后由 Manager 调度；QA 依 Spec P0/P1 + 本 Plan 最低验证层独立验收，产出 `qa-report.md`（Pass / Fail / Blocked）。
4. **合并**：QA Pass 后，依工作项记录等待用户合并授权；Planner/Developer/Reviewer/QA 均不擅自 merge。

## 修订记录

| 日期 | 摘要 |
|---|---|
| 2026-08-10 | 初稿：依据 spec.md（已 approved）与 design.md 制定任务拆解、依赖、触碰路径、P0/P1 验收映射、文档影响与 QA 进入条件。 |
