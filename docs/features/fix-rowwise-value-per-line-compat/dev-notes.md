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
| T6 README 中英「期望结果」 | 完成 |
| T7 验证 + 本文件 | 完成 |

### 关键路径

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
- `README.md` / `README.zh-CN.md`（期望结果小节）

## 验证证据

| 命令 | 结果 |
|---|---|
| `mvn -q clean test` | 退出码 0；Tests run=218, Failures=0, Errors=0, Skipped=18 |
| `grep -r 'mixed expected line shapes' src/main` | 零命中 |
| `./bin/ggtest --engine sqlite --url 'jdbc:sqlite::memory:' examples/select4.test` | 退出码 0；`TOTAL: passed=1 failed=0 skipped=0` |

关键用例：`queryHead_separatorPipe_noLabel_bindsDelim`、`p0_6_queryHead_trailingSeparatorToken_isLabelNotDeclaration`、`p0_7_expectationHeader_separatorRemoved_throwsReadableParseException`、`p0_3_defaultSpaceRowWiseAbolished_singleValueFails`、`p0_4_select4Shape_spacedTextValuePerLinePasses`、`p0_5_mixedTokenCounts_returnsFailedCompareNotThrow`、`p0_9_valuePerLineSpacedTextFixturePasses`。

可选 PG 冒烟：未执行（非必选）。

## 文档影响

- 用户文档：README 中英「Expected results / 期望结果」已改新语法。
- 开发文档：本文件；fixtures / Javadoc 随实现更新。
- **未改** `docs/manager/*`、`STATUS.md`、spec.md、plan.md。

## 本地 `examples/` 待更新清单（不入库）

| 文件 | 位置 | 问题 | 建议改法 |
|---|---|---|---|
| `examples/demo.slt` | ~91–95 | `query III` + `----` + `1 2 3`（旧默认空格行式） | 改每值一行 `1`/`2`/`3`，或 `query III nosort separator \|` + 同行式正文 |
| `examples/demo.slt` | ~97–101 | `---- separator \|` | `query IIT nosort separator \|` + 恰 `----` |
| `examples/demo.slt` | ~103–107 | `----` + `4 5 6`（旧空格行式） | 同 ~91–95 |
| `examples/demo2.slt` | ~12–15、~24–27 | `---- separator \|` | query 头 `separator \|` + 恰 `----` |

`examples/select*.test` 官方每值一行语料在本合同下可直接跑（select4 已冒烟 0 失败）；勿提交 `examples/`。

## 未解决风险

- 依赖旧默认空格行式或 `---- separator` 的本地/外部 `.test`/`.slt` 会比对失败或解析错误，需按上表迁移。
- 并行 `mvn package` 与 `mvn clean test` 曾使 `ExecutableJarManifestTest` 偶发失败；串行重跑已绿。

## 建议复测范围（Reviewer / QA）

- 全量 `mvn -q clean test`；`src/main` 无 `mixed expected line shapes`。
- sqlite select4 全量冒烟；可选其余 `select*.test` 与 PG。
- 抽查 T1/T2 消歧、token≠C 失败结果、fixtures、README 中英与 Spec 一致；确认未入库 `examples/`。

## 后续角色

Reviewer（Review 门禁 **required**）。
