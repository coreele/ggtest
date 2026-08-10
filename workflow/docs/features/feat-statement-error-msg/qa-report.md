# QA Report: feat-statement-error-msg

- 验收日期: 2026-08-10
- QA: QA (subagent)
- 路径等级: standard
- 依据: spec.md (P0/P1) / plan.md (V1-V4) / review.md (R2 Approve)

## 结论

Pass

## 验证执行结果

| ID | 命令 | 结果 |
|---|---|---|
| V1 | `mvn test -pl . -Dtest=SqlLogicTestParserTest` | BUILD SUCCESS — Tests run: 38, Failures: 0, Errors: 0, Skipped: 0 |
| V2 | `mvn test -pl . -Dtest=SqlLogicTestRunnerTest` | BUILD SUCCESS — Tests run: 34, Failures: 0, Errors: 0, Skipped: 0 |
| V3 | `mvn test -pl . -Dtest=RunnerAcceptanceTest` | BUILD SUCCESS — Tests run: 11, Failures: 0, Errors: 0, Skipped: 0 |
| V4 | `mvn test -pl .`（全量回归） | BUILD SUCCESS — Tests run: 262, Failures: 0, Errors: 0, Skipped: 16（PG 门控） |

V1–V4 均由 QA 独立在本机执行（分支 `feat-statement-error-msg`），结果与 R1/R2 一致，无回归。Skipped 16 为 PostgreSQL 门控用例，与本功能无关。

## 验收点核对（P0/P1）

### P0

1. **statement error 无消息向后兼容（执行失败 → PASSED）** — 通过
   - 证据：`SqlLogicTestRunnerTest.statementErrorWithoutMessageBackwardCompatible_pass`（`SqlLogicTestRunnerTest.java:513-520`），executor 失败、expectedErrorMsg=null → PASSED；fixture `p0-4-statement-error-message.test:15-16` 记录 3 → PASSED，于 `RunnerAcceptanceTest.java:155` 断言。
   - 实现：`SqlLogicTestRunner.java:174`（`expectedMsg != null && !expectedMsg.isEmpty()` 判空跳过匹配，仅验证失败）。

2. **statement error + 消息匹配通过（case-insensitive sub-string）→ PASSED** — 通过
   - 证据：`statementErrorWithMatchingMessagePasses`（`SqlLogicTestRunnerTest.java:467-474`），actual=`SQL error: no such table: missing`、expected=`no such table` → PASSED；case-insensitive 由 `statementErrorMessageMatchingIsCaseInsensitive`（`SqlLogicTestRunnerTest.java:492-499`，actual=`SQL Error: NO SUCH TABLE: Missing`）覆盖；fixture 记录 4 → PASSED。
   - 实现：`SqlLogicTestRunner.java:176`（`actual.toLowerCase(Locale.ROOT).contains(expectedMsg.toLowerCase(Locale.ROOT))`）。

3. **statement error + 消息不匹配 → 失败报告含 `statement error message mismatch`** — 通过
   - 证据：`statementErrorWithNonMatchingMessageFails`（`SqlLogicTestRunnerTest.java:477-489`），actual=`syntax error near INSERT`、expected=`no such table` → FAILED，断言 reason 含 `message mismatch` 且含 `no such table`；fixture 记录 5 → FAILED，`RunnerAcceptanceTest.java:163` 断言 reason4 含 `message mismatch`。
   - 实现：`SqlLogicTestRunner.java:177-180`（diff 风格首行 `statement error message mismatch`）。

4. **statement error + 消息但执行成功 → 失败：`statement expected to fail but succeeded`** — 通过
   - 证据：`statementErrorWithMessageButExecutionSucceeds`（`SqlLogicTestRunnerTest.java:502-510`），executor 成功 → FAILED，`assertEquals` 逐字断言 `statement expected to fail but succeeded`；fixture 记录 6 → FAILED，`RunnerAcceptanceTest.java:165` 同样逐字断言。
   - 实现：`SqlLogicTestRunner.java:170-171`（先判 `result.succeeded()`）。

### P1

5. **解析单 token 消息** — 通过
   - 证据：`statementErrorWithSingleTokenMessage`（`SqlLogicTestParserTest.java:578-591`），输入 `statement error no_such_table` → `assertEquals("no_such_table", stmt.expectedErrorMsg())`。
   - 实现：`SqlLogicTestParser.java:152-162`（`tokens.length > 2` 取 keyword 后子串）。

6. **解析多 token 消息（含空格）** — 通过
   - 证据：`statementErrorWithMultiTokenMessage`（`SqlLogicTestParserTest.java:594-606`），输入 `statement error no such table: missing` → `assertEquals("no such table: missing", ...)`，内部空白保留。

7. **解析含 `#` 的消息（不剥离）** — 通过
   - 证据：`statementErrorWithHashInMessage`（`SqlLogicTestParserTest.java:609-621`），输入 `statement error table#1 not found` → `assertEquals("table#1 not found", ...)`。
   - 实现：statement 分支未调用 `stripTrailingHashComment`（仅 `skipif/onlyif` 调用，见 `SqlLogicTestParser.java:96-98` 区分）。

附加核对：
- `statement ok` 拒绝多 token：`statementOkRejectsExtraTokens`（`SqlLogicTestParserTest.java:639-647`）断言报错 `statement ok does not take additional operands`，与 spec「OK 不变」一致。
- 空/仅空格消息视为无消息：`SqlLogicTestParser.java:157-160`（`raw.trim()` 为空 → 保持 null），符合 spec「错误与约束」。
- OK 分支不读取 `expectedErrorMsg`（`SqlLogicTestRunner.java:165-168`），防御性忽略，向后兼容。
- 测试为真实行为断言（parser 测试 `assertEquals` 精确值/`assertNull`；runner 测试断言 outcome 与 failureReason 文案；acceptance 测试断言 outcome 序列、计数与失败原因），非仅编译通过。

## 缺陷登记（若有，Fail 时必填）

无。

## 备注

- 验证均可执行，无风险/阻塞。PostgreSQL 真实 JDBC errorSummary 格式与 Unicode 消息为 Plan 已记录的已知验证缺口（风险低，匹配逻辑不依赖真实 DB），不影响结论。
- 当前分支 `feat-statement-error-msg`，实现位于切片源分支（R1 Git 合规问题已解决）。改动未提交，属 Manager 合并阶段处置范畴，QA 不改代码/不提交。
