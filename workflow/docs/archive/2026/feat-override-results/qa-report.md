# QA Report: feat-override-results
- 验收日期: 2026-08-10 / QA: QA (subagent)
- 路径等级: full / 依据: spec.md (P0/P1) / plan.md (验收与验证) / review.md

## 结论: Pass

## 验证执行结果

| 命令 | 结果 |
|---|---|
| `mvn -q test` | **BUILD SUCCESS** / Tests run: 321, Failures: 0, Errors: 0, Skipped: 16 (PG/Corpus gate) |
| `mvn -q clean package` | **BUILD SUCCESS** / Tests run: 321, Failures: 0, Errors: 0, Skipped: 18 (PG/Corpus gate) |

## 验收点核对（P0/P1）

### P0

| # | 验收点 | 证据 | 结论 |
|---|---|---|---|
| **P0-1** 默认关闭：无 --override → FAILED+diff、不写文件、退出码1 | `FileRunner.java:140` (`options.override()` 守卫)、`SqlLogicTestRunner.java:253` (`overrideEnabled=false` 默认)、`ReportWriter.java:59-65` (条件 showOverridden)；`MainOrchestrationTest.java:268-276` `defaultOff_totalHasNoOverriddenSegment` (exit=1, 无 `overridden=`)、`FileRunnerTest.java:192-211` `overrideDisabled_queryMismatch_fileNotRewritten` (文件/mtime 不变)、`SqlLogicTestRunnerTest.java:621-631` `overrideDisabled_queryMismatchRemainsFailed` (FAILED, overriddenCount=0) | ✅ 通过 |
| **P0-2** query mismatch 重写：--override → expected 块被 actualView 替换、`----` 保留、非 FAILED、退出码0 | `SqlLogicTestRunner.java:237-255` (in-scope→OVERRIDDEN)、`FileRunner.java:140-147` (收集→写回)、`OverrideWriter.java:111-124` (区间替换)；`MainOrchestrationTest.java:256-265` `overrideEnabled_queryMismatch_showsOverriddenTagTotalAndExitsZero` (exit=0, `[OVERRIDDEN]`, `overridden=1`)、`FileRunnerTest.java:167-189` `overrideEnabled_queryMismatch_fileRewrittenAndOverridden` (OVERRIDDEN bucket, 文件含实际值)、`SqlLogicTestRunnerTest.java:536-549` `overrideEnabled_pureQueryMismatch_yieldsOverriddenWithActualView` (OVERRIDDEN, overrideText=actualView) | ✅ 通过 |
| **P0-3** 幂等：改写后再跑 → 全 PASSED、不再改写 | `MainOrchestrationTest.java:318-336` `overrideThenRerun_isIdempotent` (run2 全 PASSED 无 OVERRIDDEN、文件不变；run3 无 --override 亦 PASSED)、`OverrideWriterTest.java:184-193` `rewriteThenParse_isIdempotent` | ✅ 通过 |
| **P0-4** 无 mismatch 不写：--override + 全 PASSED → 文件/mtime 不变 | `FileRunner.java:142` (`overrides.isEmpty()`→跳过)；`MainOrchestrationTest.java:339-358` `overrideEnabled_noMismatch_fileAndMtimeUnchanged` (exit=0, 文件内容+mtime 不变)、`FileRunnerTest.java:214-236` `overrideEnabled_allPassed_fileNotRewritten` (PASSED bucket, 内容+mtime 不变) | ✅ 通过 |
| **P0-5** 文件其余字节不变：仅 expected body 行变化 | `OverrideWriter.java:112-124` (仅替换 body 区间)；`MainOrchestrationTest.java:361-388` `overrideEnabled_restOfFileByteIdenticalExceptBody` (注释/空白/SQL/其他记录逐字节一致，仅 `wrong`→`42`)、`OverrideWriterTest.java:28-38` `singleQueryOverride_replacesBodyPreservesRest` | ✅ 通过 |
| **P0-6** 退出码优先级：override A + scope外FAILED B → A非FAILED、B FAILED、退出码1 | `CliSession.java:107-112` (hardError→2, failedCount>0→1, else→0)、`FileRunner.java:150-159` (桶判定)；`MainOrchestrationTest.java:279-297` `overrideEnabled_mixedOverrideAndScopeOutFailed_exitsOne` (A overridden、B FAILED、exit=1) | ✅ 通过 |
| **P0-7** 选项解析：--over/-override → usage/2 | `CliArgumentParser.java:62-63` (精确 case + default→UsageException)；`CliArgumentParserTest.java:98-150` (默认 false、设置、重复等价、`-override`→UsageException、`--over`→UsageException)、`MainOrchestrationTest.java:300-315` (`-override`/`--over`→exit=2、无 `[PASSED]`、无 `TOTAL:`) | ✅ 通过 |

### P1

| # | 验收点 | 证据 | 结论 |
|---|---|---|---|
| **P1-1** statement error 重写：消息替换、极性保留、SQL不变 | `SqlLogicTestRunner.java:198-203` (overrideEnabled→OVERRIDDEN)、`OverrideWriter.java:126-138` (列号前缀保留)；`MainOrchestrationTest.java:391-404` `overrideEnabled_statementErrorMessageRewritten` (极性保留、oldmsg 被替换、SQL 不变)、`SqlLogicTestRunnerTest.java:578-588` `overrideEnabled_statementErrorMessageMismatch_yieldsOverridden` (OVERRIDDEN, overrideText=实际 summary) | ✅ 通过 |
| **P1-2** scope外不覆盖：label冲突/执行失败/签名错/极性错 → 仍FAILED | `SqlLogicTestRunner.java:253-255` (仅 resultMismatch+!labelConflict→OVERRIDDEN)、`SqlLogicTestRunner.java:217-218` (exec fail→FAILED early-return)；`SqlLogicTestRunnerTest.java:552-563` (label冲突→FAILED)、`SqlLogicTestRunnerTest.java:566-575` (执行失败→FAILED)、`SqlLogicTestRunnerTest.java:591-598` (stmt ok失败→FAILED)、`SqlLogicTestRunnerTest.java:602-607` (stmt error实际成功→FAILED)、`MainOrchestrationTest.java:406-418` (stmt ok失败→exit=1, 文件不变)、`MainOrchestrationTest.java:420-443` (label冲突→exit≥1)、`FileRunnerTest.java:239-266` (scope外+in-scope混合→FAILED bucket, in-scope 仍写回) | ✅ 通过 |
| **P1-3** execute-only 不生成 expected 块 | `SqlLogicTestRunner.java:237` (hasExpectedResults 条件)；`MainOrchestrationTest.java:446-460` `overrideEnabled_executeOnlyQuery_fileUnchanged` (文件不变)、`SqlLogicTestRunnerTest.java:610-618` `overrideEnabled_executeOnlyQueryNotCompared` (PASSED, 0 overriddenCount) | ✅ 通过 |
| **P1-4** 只读FS：写回失败→硬错误/2、原文件未损 | `OverrideWriter.java:77-100` (temp+ATOMIC_MOVE, try-finally 删 temp)、`FileRunner.java:172-184` (IOException→hardFailure)；`OverrideWriterTest.java:156-174` `writeFailureLeavesOriginalIntact` (IOException, 原文完整)、`FileRunnerTest.java:269-301` `overrideEnabled_writeFailure_isHardErrorAndOriginalIntact` (hardError, 原文不变)、`MainOrchestrationTest.java:495-511` `overrideEnabled_writeFailure_exitsTwo` (exit=2, "override write failed") | ✅ 通过 |
| **P1-5** --override --halt：override不触发halt、scope外触发、停跑前已override写回 | `SqlLogicTestRunner.java:170-174` (haltOnFirstFailure 仅按 FAILED)、`CliSession.java:98-100` (bucket==FAILED 停跑)；`SqlLogicTestRunnerTest.java:644-658` `overrideEnabled_doesNotTriggerHalt` (OVERRIDDEN 不触发 halt)、`MainOrchestrationTest.java:463-492` `overrideEnabled_withHalt_inScopeOverrideThenScopeOutFailed` (第1条 override 已写回、第2条 scope外 FAILED 触发 halt、exit=1) | ✅ 通过 |
| **P1-6** 致命中止不写文件 | `FileRunner.java:129-138` (aborted 在 override 检查前→hardFailure 短路)；`SqlLogicTestRunnerTest.java:362-376` `fatalDatabaseFailureAbortsFileAndKeepsEarlierResults` (aborted=true, abortReason 非空)。**集成端对端测试缺口**：无 `--override` + `FatalDatabaseException` → 文件不变 + exit=2 的 MainOrchestrationTest 用例——详见「备注」。 | ✅ 通过（代码逻辑正确，测试缺口为已知低风险，见备注） |
| **P1-7** 原子性：写回失败原文件完整 | `OverrideWriter.java:82-99` (temp+ATOMIC_MOVE, try-finally 删 temp，原文件从未被打开写)；`OverrideWriterTest.java:156-174` `writeFailureLeavesOriginalIntact` (失败后原文件可被原样解析) | ✅ 通过 |
| **P1-8** README 文档 | `README.md:46,59` (synopsis 含 `[--override]`，选项表含 `--override` 行，语义与 Spec §CLI 接口一致)、`README.zh-CN.md:34,48` (同) | ✅ 通过 |

## 缺陷登记

| # | 严重程度 | 描述 | 复现指令 | 建议 |
|---|---|---|---|---|
| 无 | — | — | — | — |

## 备注

### P1-6 集成测试缺口说明

Review 指出 P1-6（致命中止不写文件）缺集成端对端测试：
- **代码层面**：`FileRunner.java:129-138` 的 `result.aborted()` 检查位于 `options.override()` 守卫（line 140）**之前**，无条件短路，不进入写回路径。逻辑正确，路径简单。
- **证据层面**：`SqlLogicTestRunnerTest.java:362-376` (`fatalDatabaseFailureAbortsFileAndKeepsEarlierResults`) 确认 runner 层 `result.aborted()=true` 且 `abortReason` 非空。
- **缺失**：无 `MainOrchestrationTest` 用例构造 `FatalDatabaseException` + `--override` → exit=2 + 文件不变的集成验证。

**QA 评估**：可接受。理由：
1. abort 短路逻辑为无条件 if-guard，结构简单，不依赖 override 状态的任何分支。
2. Runner 层 abort 行为已有单元测试覆盖。
3. `FatalDatabaseException` 触发需要特定 SQL/引擎条件，集成测试构造复杂度高，低回报。
4. Review 已确认代码逻辑正确，且将此标记为低严重度。

此为已知低风险缺口，不构成 Blocking 缺陷。建议未来回归中若发现 abort 路径变更，补加集成端对端用例；当前不阻塞 Pass。

### 其他
- 15 个 P0/P1 验收点 (P0-1~P0-7, P1-1~P1-8) 均有对应单元/集成测试证据，无空口宣称。
- `mvn test` 与 `mvn clean package` 均 **0 failures, 0 errors**，全量测试 321 用例，跳过 16（PG/Corpus gate，与 override 无关）。
- 基线 262→321，本特性新增 **59 tests**，无回归。
- Review 结论为 **Approve**，无 blocking 项。
