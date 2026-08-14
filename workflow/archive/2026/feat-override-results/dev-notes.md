# Developer Notes: feat-override-results

> Implementation record for Reviewer / QA reference.
> 合同权威：[`spec.md`](./spec.md)；技术设计：[`design.md`](./design.md)；任务拆解：[`plan.md`](./plan.md)。

## 验证命令与结果

### 定向测试（开发迭代）

```
mvn -q test -Dtest=SqlLogicTestParserTest,SqlLogicTestRunnerTest,OverrideWriterTest,FileRunnerTest,CliArgumentParserTest,RuntimeConfigResolverTest,CliReportAcceptanceTest,MainOrchestrationTest
```

结果：**BUILD SUCCESS**，203 tests run，0 failures，1 skipped（FileRunnerTest 的 PG 门控用例，`GGTEST_PG_URL` 未设）。

### 全量测试

```
mvn -q test
```

结果：**BUILD SUCCESS**，**321 tests run，0 failures，16 skipped**（全部为 PG/Corpus 门控用例）。
基线（实现前）为 262 tests；本特性新增 **59 tests**。

### 打包回归

```
mvn -q clean package
```

结果：**BUILD SUCCESS**，321 tests run，0 failures，18 skipped（含 FileRunnerTest PG 门控）。

## 各任务完成情况

| 任务 | 状态 | 说明 |
|---|---|---|
| T1 Parser 携带 expected 区间 | ✅ | `QueryRecord.expectedHeaderLine/expectedBodyEndLine`、`StatementRecord.errorMsgStartColumn`；次构造器保留旧签名。parser 在逐行解析时填充。7 个新 parser 测试。 |
| T2 Runner in-scope 判定 + golden 透传 | ✅ | `RecordOutcome.OVERRIDDEN`、`RecordResult.overrideText` + `overridden()` 工厂、`FileRunResult.overriddenCount()`。`SqlLogicTestRunner` 新增 `overrideEnabled`；纯 query mismatch / statement error 消息失配 → OVERRIDDEN；label 冲突 / 执行失败 / 极性翻转 → FAILED。10 个新 runner 测试。 |
| T3 `OverrideWriter` | ✅ | 新增类。`rewrite` 检测 EOL、按行号倒序应用 override（query 区间替换 / statement 列号前缀保留）。`writeAtomically` 同目录 temp + `ATOMIC_MOVE`，回退 `REPLACE_EXISTING`。14 个新测试（含 CRLF / 文件尾换行 / 空块 / 只读 FS / 原子性）。 |
| T4 `FileRunner` 接入写回 + 桶判定 | ✅ | `FileBucket.OVERRIDDEN`、`FileOutcome.overridden()`。`runWithExecutor` 在 `runner.run` 后收集 OVERRIDDEN → 读原文 → rewrite → writeAtomically；失败转 hardFailure。桶判定顺序：aborted→不写/hardFailure；failedCount>0→FAILED；overriddenCount>0→OVERRIDDEN；仅 skipped→SKIPPED；否则 PASSED。5 个新 FileRunner 测试。 |
| T5 CLI 接线 | ✅ | `--override` 精确长选项（仿 `--halt`），`ParsedArguments.override`、`CliOptions.override`（次构造器保留旧签名）、`RuntimeConfigResolver` CLI-only 透传、`toString` 补 `override=`。11 个新 CLI 测试。 |
| T6 报告 / 退出码 | ✅ | `ReportStyle.overriddenTag()` → `[OVERRIDDEN]`（CYAN）。`CliSession` 加 `totalOverridden` + `case OVERRIDDEN`；`printTotal` 仅 `options.override()` 时追加 `overridden=N`。退出码优先级不变。 |
| T7 集成 / 回归 | ✅ | MainOrchestrationTest 新增 14 个集成用例覆盖 P0-2/P0-3 幂等、P0-4 无 mismatch 不写、P0-5 字节保留、P0-6 混合退出码、P0-7 选项解析、P1-1 statement error 重写、P1-2 scope 外不覆盖、P1-3 execute-only、P1-5 `--halt` 组合、P1-4 写回失败退出码 2。 |
| T8 README | ✅ | `README.md` / `README.zh-CN.md` synopsis + 选项表新增 `--override` 行。 |

新增 / 修改文件：

- **新增**（2）：`src/main/java/com/ggtest/cli/OverrideWriter.java`、`src/test/java/com/ggtest/cli/OverrideWriterTest.java`
- **修改-main**（11）：`model/QueryRecord.java`、`model/StatementRecord.java`、`parser/SqlLogicTestParser.java`、`runner/RecordOutcome.java`、`runner/RecordResult.java`、`runner/FileRunResult.java`、`runner/SqlLogicTestRunner.java`、`cli/CliArgumentParser.java`、`cli/ParsedArguments.java`、`cli/RuntimeConfigResolver.java`、`cli/CliOptions.java`、`cli/FileOutcome.java`、`cli/FileRunner.java`、`cli/ReportStyle.java`、`cli/ReportWriter.java`、`cli/CliSession.java`
- **修改-test**（5）：`parser/SqlLogicTestParserTest.java`、`runner/SqlLogicTestRunnerTest.java`、`cli/FileRunnerTest.java`、`cli/CliArgumentParserTest.java`、`cli/RuntimeConfigResolverTest.java`、`cli/MainOrchestrationTest.java`
- **修改-doc**（2）：`README.md`、`README.zh-CN.md`

## 关键实现抉择

1. **EOL 处理**：`OverrideWriter.rewrite` 直接对全文按检测到的 EOL 切分为行表（含尾部空串标记），编辑后 `String.join` 重连——不单独 strip/add trailing EOL。这天然保留了 CRLF / LF / 文件尾换行（有/无）。空 expected 块 override 后，`----` 与块后空行分隔之间的新 body 行被插入，空行保留（行为正确）。

2. **`expectedBodyEndLine` 计算**：由 `expectedHeaderLine + expected.size()` 得出，无需在 `readExpectedResults` 内追踪行号。空块时 `end == header`，哨兵 `bodyStart > end` 自然成立。

3. **`errorMsgStartColumn` 计算**：parser 在 de-CR 的原始 header 行上，从 `indexOfToken` 找到的 `error` token 末尾跳过空白得到消息起始列。prefix `header.substring(0, col)` 保留 `statement error` + 原始分隔，后缀替换为实际 summary。

4. **runner in-scope 分支**：`runQuery` 中仅当 `overrideEnabled && resultMismatch && !labelConflict` 时产 OVERRIDDEN（execution failure / type signature error 在更早的 early-return 中已返回 FAILED，不会到达此分支）。`runStatement` 的 ERROR 分支在消息子串失配时，`overrideEnabled` → OVERRIDDEN（载荷 = 实际 error summary）。

5. **`--halt` 交互**：`OVERRIDDEN != FAILED`，故 runner 的 `haltOnFirstFailure`（按 `RecordOutcome.FAILED` 触发）与 `CliSession` 的 `bucket == FAILED` 停跑检查都自然忽略 in-scope override。同文件内 scope 外 FAILED 仍触发 halt，但之前已 override 的记录仍被写回（写回在 FileRunner 文件级、runner.run 返回后，先于 CliSession 跨文件停跑）。

## 验证缺口

- **PG 专有 statement error 消息文本** 未在最低验证层实测（与 Plan §验证缺口一致）。override 的 in-scope 判定 / 重写机制与引擎无关；sqlite `:memory:` 已覆盖 query result mismatch 与 statement error 消息失配的完整路径。如需 PG 实测消息文本，须设 `GGTEST_PG_URL`。
- **label conflict + result mismatch 同时存在时的 override 边界**：`overrideEnabled_labelConflict_notOverridden` 测试中，第二条 query 同时有 result mismatch 和 label conflict → 维持 FAILED（符合 Spec P1-2）。第一条 query 有 result mismatch 但无 label conflict（首次见到 label）→ OVERRIDDEN。

## 留给 Reviewer 关注的点

1. **`OverrideWriter` 安全面**（Plan §进入 QA 的条件 - 安全影响）：`writeAtomically` 仅写 `FileRunner` 传入的 runner 已解析的目标 `Path`（来自 `TestFileCollector.collect` 的 positional input），无用户可控路径拼接。temp 文件创建在目标文件所在目录（`Files.createTempFile(dir, ...)`），prefix 固定。无路径遍历 / 越权写风险。

2. **原子写回退**：`AtomicMoveNotSupportedException` 回退 `REPLACE_EXISTING`。Linux 同目录始终走 ATOMIC。测试覆盖了「只读目录 → temp 创建失败 → 原文件不变」。

3. **混合 EOL**（罕见）：`rewrite` 按「全文是否含 `\r\n`」检测 EOL 并归一。混合 EOL 文件会被归一为检测值——Spec 合同为「不强制改换行符」，可接受。
