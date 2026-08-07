# Dev Notes: ggtest-rowwise-expected

## 实现说明

- 标识：`ggtest-rowwise-expected`；分支 `ggtest-rowwise-expected` → `main`。
- T1–T5 完成（含第四次修订**废止 R3**：删引号层）；**未** commit / merge / push。
- 合同偏差：无。相对旧实现：移除文件级 `SeparatorRecord`/`FileState.S`；期望头本条绑定（R1）；显式仅 trim（R2）；**删除** `splitLiteralRespectingQuotes` / `unquote` 及未闭合引号路径（已废止 R3）。

### 变更路径

| 路径 | 动作 |
|---|---|
| `model/QueryRecord.java` | 增 `columnSeparator` / `explicitColumnSeparator` |
| `model/SeparatorRecord.java` | **删除** |
| `model/SqlTestRecord.java` | 去 permits |
| `parser/SqlLogicTestParser.java` | `parseQuery` 期望头分流；顶层 `---- separator` 非法 |
| `normalize/ExpectedResultExpander.java` | 显式 `splitLiteral` + `strip`；**无**引号层 |
| `normalize/ResultComparer.java` | `compare(..., S, explicit, ...)`；Javadoc 无去引号 |
| `runner/SqlLogicTestRunner.java` | 本条 `S`/`explicit`；无文件级 separator |
| parser / normalize / runner 测试 | P0 无引号；P1-4 字面引号不通过 |
| `fixtures/runner/rowwise-*.test` | 期望头；`rowwise-pipe-separator` 裸 `hello world` |
| `README.md` | R1/R2；无引号壳书写；含 `S` 换分隔符或每值一行 |

未触碰：`workflow/workflow/docs/manager/*`、冻结合同 `spec.md`/`design.md`/`plan.md`、`.env`、`examples/demo2.slt`、`ValueNormalizer` / MD5 算法。

### 行为摘要

1. Parser：SQL 后恰 `----` → `S=" "`、`explicit=false`；`---- separator <delim>` → 本条 delim、`explicit=true`；顶层 `---- separator` → `ParseException`；无 `SeparatorRecord`。
2. Normalize：显式路径 `splitLiteral` → 每 token `strip` → 展开；trim 后 token **原文**即单元格。默认路径不 trim、连续空格仍空 token。
3. Runner：`runQuery` 从 `QueryRecord` 传 comparer；无文件级覆盖。

### 验证（L3）

| 命令 | 结果 |
|---|---|
| `mvn -q clean test` | BUILD SUCCESS；Failures=0、Errors=0（Tests run: 196，Skipped: 17） |

### P0 覆盖

| ID | 证据 |
|---|---|
| P0-1 | `ResultComparerTest.p0_1_*`；`rowwise-default-space.test` |
| P0-2 | parser `p0_2_targetWriting_iitPipeBareText*`；comparer `p0_2_*`；`rowwise-pipe-separator.test`（`1 \| 1 \| hello world`） |
| P0-3 | parser `p0_3_*`；runner `p0_3_*`；`rowwise-mixed.test` |
| P0-4 | `ResultComparerTest.p0_4_*` |
| P0-5 | `ResultComparerTest.p0_5_cellContainingSeparator_*`（含 `S` 失败；换 `,` / 每值一行通过） |
| P0-6 | `p0_6_valuePerLine*`；smoke / NormalizeAcceptance |
| P0-7 | `ResultComparerTest.p0_7_*`；`rowwise-mixed` 哈希行 |
| P0-8 | `ResultComparerTest.p0_8_*` |
| P0-9 | `RunnerAcceptanceTest.p0_*_rowWise*`；fixtures 无文件顶全局；不依赖 demo2 |

### P1

已覆盖：空 delim、非法 `----…`、错误拼写、顶层 separator 非法、空 token/`(empty)`、连续空格（默认）、单列、混用、Diff 值行、P1-4（`'hello world'` 计入原文、不因去引号通过）。缺口：无（相对本修订合同）。

### 风险

无阻塞。工作区未 commit；须重新 Review `Approve` 后再 QA；**不合入 main**（未授权）。
