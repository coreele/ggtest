# Dev Notes: feat-cli-halt

> 开发者实现记录。依据 `plan.md`（T1–T8）与 `spec.md`（合同）实施；TDD 全程先行测试。
> 作者：Developer。结论先行，证据紧随。

## 实现摘要

为 `ggtest` CLI 增加布尔选项 **`--halt`**（默认关闭），与官方 `sqllogictest --halt` 对齐：本进程**首次错误**（断言失败或硬错误）即停。未传时现有「文件内继续、多文件跑完、退出码 0/1/2」语义**完全不变**。合同全部落地，未偏离 Spec，未扩范围。

- **选项解析（T1）**：精确 `--halt`（无值）；重复 ≡ 单次（非 usage 错误）；`-halt`/`--hal` 等落入既有 `default -> throw new UsageException("unknown option: " + arg)`，退出码 `2`，不连库。
- **配置透传（T2）**：`CliOptions.halt` 为 CLI-only 字段，`RuntimeConfigResolver` 仅从 `ParsedArguments.halt()` 透传，**不**从 `GGTEST_*` env/.env 推断。
- **文件内首错即停（T3）**：runner 增加 `haltOnFirstFailure`；首条非 fatal `FAILED` 后，剩余 assertable 标 `SKIPPED`（专用原因常量 `SKIPPED_AFTER_FAILURE_HALT`，与语料 `halt` 的 `SKIPPED_AFTER_HALT` 区分），**不执行 SQL、不假失败**；`FatalDatabaseException` 路径不变。
- **跨文件全局停（T4）**：`CliSession` 每个文件返回后，若 `options.halt()` 且 `outcome.bucket() == FAILED` 则 `break`；后续文件不打开/解析/执行、不打印状态行、不计入 `TOTAL`。退出码逻辑不变。
- **与语料 `halt` 区分（T7）**：语料 `halt` 仅中止当前文件后续（skipped，**非错误**），不触发 CLI `--halt`；`FileRunResult.halted` 既有语义未重载。
- **凭据脱敏**：沿用既有 `CredentialRedaction`，无新增泄露面；`CliOptions.toString()` 补 `halt=…`（无密钥）。

## 关键实现决策

| 决策点 | 选择 | 理由 |
|---|---|---|
| runner 停跑机制 | 新增 3-arg 构造器 `SqlLogicTestRunner(executor, hashThreshold, haltOnFirstFailure)`；run 循环内局部 `haltedOnFirstFailure` 标志 | 实例每文件新建，线程安全语义不变；保留既有 1-arg/2-arg 构造器（README 库 API 不变） |
| `FileRunResult` 是否新增字段 | **否** | `halted` 专指语料 `halt` 记录，合同禁止重载；未执行记录为 `SKIPPED`（非 `FAILED`），自然不进入 `FileRunner` 失败明细；合同只要求「未执行记录不出现在失败明细」，已满足 |
| `CliSession` break 条件 | `options.halt() && outcome.bucket() == FAILED` | `FAILED` 桶涵盖断言失败与硬错误两类；`PASSED`/`SKIPPED` 文件继续；退出码优先级自然成立 |
| `ParsedArguments.halt` 类型 | `boolean halt` | 简单布尔标志，重复设置等价单次；缺席即 `false` |
| CLI-only 来源 | 仅 `ParsedArguments.halt()` 透传 | Spec 未规定 env/.env 来源；不读 `GGTEST_HALT` |

## 变更路径

**主代码（src/main/java/com/ggtest/）：**
- `cli/CliArgumentParser.java`：switch 新增 `case "--halt" -> halt = true`；Javadoc 选项清单补充 halt。
- `cli/ParsedArguments.java`：record 新增 `boolean halt` 字段（canonical null-safe）。
- `cli/CliOptions.java`：record 新增 `boolean halt` 字段（colorMode 与 inputs 之间）；`toString()` 补 `halt=…`；Javadoc。
- `cli/RuntimeConfigResolver.java`：`resolve` 末尾透传 `parsed.halt()`。
- `cli/FileRunner.java`：`runWithExecutor` 构造 runner 传入 `options.halt()`。
- `runner/SqlLogicTestRunner.java`：新增 3-arg 构造器与 `haltOnFirstFailure` 字段；run 循环首错后剩余 assertable 标 `SKIPPED`（新常量 `SKIPPED_AFTER_FAILURE_HALT`）；类 Javadoc 补充 `--halt` 例外。

**测试（src/test/java/com/ggtest/）：**
- `cli/CliArgumentParserTest.java`：+5 用例（默认 false / 单次开启 / 重复等价 / `-halt` 拒绝 / `--hal` 拒绝）。
- `cli/RuntimeConfigResolverTest.java`：+3 用例（默认 false / CLI 透传 true / 不从 env/.env 推断）；2 处既有 `CliOptions.toString` 用例补 `halt` 断言。
- `cli/FileRunnerTest.java`：+1 用例（多失败 fixture + halt → 仅 1 个 `[WHY]`/`at` 块）；新增 `sqliteOptionsWithHalt` 辅助。
- `cli/MainOrchestrationTest.java`：+4 用例（跨文件全局停 P0-3 / 默认关闭 P0-1 / 硬错误停跑 P0-4 / 语料 halt 区分 P0-6）。
- `runner/SqlLogicTestRunnerTest.java`：+4 用例（首错后剩余 SKIPPED / halt=false 仍继续 / fatal 路径不变 / 语料 halt 不被 inflate）；新增 `run(boolean, …)` 辅助。
- `cli/CredentialRedactionTest.java`：构造调用补 `halt=false`（无行为变更）。

**Fixture：**
- `src/test/resources/fixtures/cli/halt/corpus-halt.test`（含成功记录 + 语料 `halt` + 其后记录；供 P0-6）。

**用户文档：**
- `README.md`：CLI synopsis 补 `[--halt]`；选项表新增 `--halt` 行（默认 off，首错即停）。
- `README.zh-CN.md`：同步 synopsis 与选项表。

## TDD 与验证证据

每个任务执行 RED（测试因缺少目标行为失败）→ GREEN（最小实现通过）循环：

- **T1**：RED = `unknown option: --halt`（haltFlagIsSetWhenSupplied / repeatedHaltFlagIsEquivalentToSingle 各 ERROR）；GREEN 后 12 用例通过。
- **T2**：RED = `CliOptions` 无 `halt` 字段（编译期 unresolved）；GREEN 后 RuntimeConfigResolverTest 26 用例通过。
- **T3**：RED = 3-arg 构造器缺失（4 用例 ERROR）；GREEN 后 SqlLogicTestRunnerTest 28 用例通过；FileRunnerTest halt 用例 1 个 `[WHY]`/`at` 块。
- **T4**：RED = 第二文件仍被启动（`pass.test` 出现，`expected: <false> but was: <true>`）；GREEN 后 MainOrchestrationTest 14 用例通过。
- **T5/T6/T7**：随 T4 实现一并 GREEN（defaultOffReportsAllFailuresAndRunsLaterFiles / haltWithHardErrorExitsTwoAndDoesNotStartLaterFiles / corpusHaltRecordDoesNotTriggerCliHalt）。

**验证命令与结果摘要（实际执行）：**

| 命令 | 结果 |
|---|---|
| `mvn test -Dtest=CliArgumentParserTest` | Tests run: 12, Failures: 0, Errors: 0 |
| `mvn test -Dtest=RuntimeConfigResolverTest` | Tests run: 26, Failures: 0, Errors: 0 |
| `mvn test -Dtest=SqlLogicTestRunnerTest` | Tests run: 28, Failures: 0, Errors: 0 |
| `mvn test -Dtest=FileRunnerTest` | Tests run: 6, Failures: 0, Errors: 0, Skipped: 1（PG 门控） |
| `mvn test -Dtest=MainOrchestrationTest` | Tests run: 14, Failures: 0, Errors: 0 |
| `mvn test -Dtest=CliArgumentParserTest,RuntimeConfigResolverTest,FileRunnerTest,MainOrchestrationTest,SqlLogicTestRunnerTest,CliReportAcceptanceTest` | Tests run: 100, Failures: 0, Errors: 0, Skipped: 1（定向） |
| `mvn test`（全量） | Tests run: 250, Failures: 0, Errors: 1, Skipped: ~16（PG/corpus 门控） |
| `mvn clean package -Dtest=!NormalizeAcceptanceTest` | Tests run: 244, Failures: 0, Errors: 0, Skipped: 18；**BUILD SUCCESS**；`target/ggtest-0.1.0-SNAPSHOT.jar`（13.5 MB） |

**关于全量回归中的 1 个 Error（pre-existing，与本工作项无关）：**

- 失败项：`com.ggtest.normalize.NormalizeAcceptanceTest.p0_2_hashMatchesSelect1CorpusExcerpt` → `IllegalArgument fixture missing values section`（`extractValuesSection:120`）。
- 原因：Windows 无 `.gitattributes`，git 检出时将 fixture `src/test/resources/fixtures/normalize/p0-2-select1-hash.txt` 的 LF 转 CRLF（实测 37 个 CRLF）；测试以 `indexOf("values:\n")` 查找标记，CRLF 下匹配失败。git blob 仍为 LF。
- 影响面：仅 `normalize` 包，与 `--halt`（cli/runner）无任何代码或数据依赖。
- 证据：`git stash`（移除本工作项全部改动）后该测试**同样失败**，证明为既有问题。
- 风险：不影响 `--halt` 合同与回归；仅在 Windows CRLF 检出环境下影响该 normalize 用例。
- 恢复条件：新增 `.gitattributes` 强制测试资源 LF（如 `*.txt text eol=lf`）或设 `core.autocrlf=false` 重新检出。**此修复超出 feat-cli-halt 范围**（属工程基线问题），不在本工作项实施；建议作为独立 chore 工作项处理。

## 文档影响落实

| 类别 | Plan 声明 | 落实情况 |
|---|---|---|
| 开发文档 | Javadoc 更新点（CliArgumentParser/CliOptions/SqlLogicTestRunner/CliSession） | ✓ 已更新四处 Javadoc，说明 `--halt` 行为与例外 |
| 用户文档 | README.md / README.zh-CN.md synopsis + 选项表 | ✓ 两份均新增 `[--halt]` synopsis 行与 `--halt` 选项表行；简述对齐官方 *Stop when first error is seen*（默认关） |
| 运维文档 | N/A | ✓ 未改；退出码集合与优先级不变，运维告警阈值不受影响 |

## 未解决风险或阻塞

- **本工作项范围内：none。** 合同 P0-1…P0-6、P1-1…P1-2 全部由测试覆盖并通过。
- **超出本工作项范围（仅记录，不实施）**：Windows CRLF 导致 `NormalizeAcceptanceTest` 既有失败——完整「未验证项 → 原因 → 风险 → 恢复条件」见上「验证命令与结果摘要」末尾。已 `git stash` 证明与本工作项无关；建议作为独立 chore 工作项修复 `.gitattributes`（强制测试资源 LF），不在本工作项实施。

## 验收映射自检

- P0-1 默认关闭：`defaultOffReportsAllFailuresAndRunsLaterFiles`（3 个失败全报告，第二文件仍跑，退出码 1）+ 既有 `multiFailureLayoutUsesBlankSeparatorsAndFlushAt`。
- P0-2 单文件首错即停：`haltStopsAfterFirstFailureInOneFile`（FileRunner，1 个 `[WHY]`/`at`）+ `haltOnFirstFailureSkipsRemainingAssertablesAfterFirstFailure`（runner，剩余 SKIPPED 不执行）。
- P0-3 多文件全局停：`haltStopsAfterFirstFailingFileAndDoesNotStartLaterFiles`（第二文件 stdout 无，TOTAL.passed=0，退出码 1）。
- P0-4 硬错误 → 2：`haltWithHardErrorExitsTwoAndDoesNotStartLaterFiles`（`bad-parse.test` + `pass.test` + `--halt`，第二文件不启动，退出码 2）。
- P0-5 选项解析：`haltFlagIsSetWhenSupplied`（开启）+ `singleDashHaltIsRejectedAsUnknownOption` / `haltPrefixLongOptionIsRejectedAsUnknownOption`（usage，退出码 2 不连库，沿用既有 `missingUrlExitsTwoWithoutRunning` 模式）。
- P0-6 与语料 `halt` 区分：`corpusHaltRecordDoesNotTriggerCliHalt`（语料 halt 中止当前文件 skipped，非错误；第二文件仍执行；退出码 0）。
- P1-1 文档：两份 README 选项表含 `--halt`。
- P1-2 重复 `--halt`：`repeatedHaltFlagIsEquivalentToSingle`（非 usage 错误）。
