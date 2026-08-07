# Dev notes: fix-rowwise-value-per-line-compat

> 实施记录（Developer）。Review / QA 依据本文件 + Plan 验收表。

## 完成内容

| 任务 | 结果 |
|---|---|
| T1 parser 单测（消歧 / `---- separator` 移除） | 完成 |
| T2 normalize 单测（每值一行 / 声明行式 / token≠C 失败结果） | 完成 |
| T3 model + parser | 完成：`QueryRecord.columnSeparator` → `Optional<String>`；query 头 `separator <delim>`；恰 `----` 期望头；删除 `SqlLogicDefaults` / `DEFAULT_COLUMN_SEPARATOR` |
| T4 normalize + runner | 完成：无声明 → 物理行；有声明 → split/trim/恰 C；token≠C → `CompareResult.passed()==false`；删除 `mixed expected line shapes` |
| T5 fixtures | 完成：`rowwise-default-space.test` → `value-per-line-spaced-text.test`；pipe/mixed 改 query 头语法 |
| T6 README 中英「期望结果」 | 完成（实现轮）；合入前补充再改运行示例与报告样例 |
| T7 验证 + 本文件 | 完成 |
| 合入前补充：demo / demo_zh / README | 完成（本轮；**零 commit**） |

### 变更路径

- `src/main/java/com/ggtest/model/QueryRecord.java`
- `src/main/java/com/ggtest/model/SqlLogicDefaults.java`（删除）
- `src/main/java/com/ggtest/parser/SqlLogicTestParser.java`
- `src/main/java/com/ggtest/normalize/ExpectedResultExpander.java`
- `src/main/java/com/ggtest/normalize/ResultComparer.java`
- `src/main/java/com/ggtest/runner/SqlLogicTestRunner.java`
- `src/test/java/com/ggtest/parser/SqlLogicTestParserTest.java`
- `src/test/java/com/ggtest/normalize/ResultComparerTest.java`
- `src/test/java/com/ggtest/runner/RunnerAcceptanceTest.java` / `SqlLogicTestRunnerTest.java`
- `src/test/java/com/ggtest/cli/ReportWriterTest.java`
- `src/test/java/com/ggtest/model/SqlLogicDefaultsTest.java`（删除）
- `src/test/resources/fixtures/runner/value-per-line-spaced-text.test`（新）；`rowwise-pipe-separator.test` / `rowwise-mixed.test`（改）；`rowwise-default-space.test`（删）
- `README.md` / `README.zh-CN.md`
- `examples/demo.slt`（修订）；`examples/demo_zh.slt`（新增）；`examples/demo2.slt`（已删除，独特用例并入 demo）

## 验证证据

| 命令 | 结果 |
|---|---|
| `mvn -q clean test` | 退出码 0；Tests run=218, Failures=0, Errors=0, Skipped=18 |
| `grep -r 'mixed expected line shapes' src/main` | 零命中 |
| `./bin/ggtest --engine sqlite --url 'jdbc:sqlite::memory:' examples/select4.test` | 退出码 0；`TOTAL: passed=1 failed=0 skipped=0` |
| `./bin/ggtest --engine sqlite --url jdbc:sqlite::memory: examples/demo.slt examples/demo_zh.slt` | 退出码 0；`TOTAL: passed=2 failed=0 skipped=0` |

关键用例：`queryHead_separatorPipe_noLabel_bindsDelim`、`p0_6_queryHead_trailingSeparatorToken_isLabelNotDeclaration`、`p0_7_expectationHeader_separatorRemoved_throwsReadableParseException`、`p0_3_defaultSpaceRowWiseAbolished_singleValueFails`、`p0_4_select4Shape_spacedTextValuePerLinePasses`、`p0_5_mixedTokenCounts_returnsFailedCompareNotThrow`、`p0_9_valuePerLineSpacedTextFixturePasses`。

可选 PG 冒烟：未执行（非必选）。

## 合入前补充（2026-07-26；零 commit）

- **demo2 决策：并入主 showcase 后删除** — `demo.slt` / `demo_zh.slt` 已覆盖 `|` / `,` 行式、含空格 TEXT 每值一行、SQL `''` → `'`；`examples/demo2.slt` 已移除。
- **demo 覆盖**：`statement ok/error`；`query` I/T/R、`nosort`/`rowsort`/`valuesort`、label、`separator`；纯 `----` 每值一行；NULL/`(empty)`；execute-only；`skipif`/`onlyif`；`hash-threshold` + MD5；`halt`；文件头 sqlite/postgres 命令。
- **README**：运行示例与报告样例改为 `demo.slt` + `demo_zh.slt`；「Expected results / 期望结果」仍为 query 头 `separator`、勿宣传空格猜行式 / `---- separator`。
- **未改** Java、`agents/manager/*`、`STATUS.md`、spec/plan、`architecture-overview`；未删 `examples/select*.test`。

## 文档影响

- 用户文档：README 中英期望结果 + 本轮运行/报告样例。
- 开发文档：本文件；fixtures / Javadoc 随实现更新。
- **未改** `agents/manager/*`、`STATUS.md`、spec.md、plan.md。

## 未解决风险

- 依赖旧默认空格行式或 `---- separator` 的外部 `.test`/`.slt` 会比对失败或解析错误。
- 并行 `mvn package` 与 `mvn clean test` 曾使 `ExecutableJarManifestTest` 偶发失败；串行重跑已绿。

## 建议复测范围（Reviewer / QA）

- 冒烟：`./bin/ggtest --engine sqlite --url jdbc:sqlite::memory: examples/demo.slt examples/demo_zh.slt`。
- 抽查 README 中英运行示例与报告样例文件名。
- 原实现轮：`mvn -q clean test`；select4 sqlite；`src/main` 无 `mixed expected line shapes`（已 Approve/Pass，本轮未改 Java）。

## 后续角色

Reviewer（合入前文档/示例轻量确认；原 Approve 仍有效时可记轻量确认后 QA 冒烟 demo）。
