# Dev Notes: fix-onlyif-skipif-hash-comments

## 实现说明

- 分支：`fix-onlyif-skipif-hash-comments` → `main`。本轮**未** commit / push / merge。
- **T0**：自 `main` 创建并检出源分支。
- **T1 RED**（先于改 parser）：`SqlLogicTestParserTest` 新增
  - `onlyif_trailingHashComment_parsesDbName`（`onlyif sqlite # empty RHS`）
  - `skipif_trailingHashComment_parsesDbName`（`skipif mysql # not compatible`）
  - `onlyifAndSkipif_withoutTrailingHash_stillParse`
  - 证据：`mvn -q test -Dtest=SqlLogicTestParserTest#onlyif_trailingHashComment_parsesDbName,SqlLogicTestParserTest#skipif_trailingHashComment_parsesDbName,SqlLogicTestParserTest#onlyifAndSkipif_withoutTrailingHash_stillParse` → Errors=**2** / Failures=0；`ParseException: onlyif|skipif requires a database name`；无尾注释测 Pass。
- **T2 GREEN**：`onlyif`/`skipif` 在 `splitTokens` 前经 `stripTrailingHashComment` 剥离行尾 `# …`；整行 `#` 跳过未改；未改 record 模型/runner。`mvn -q test -Dtest=SqlLogicTestParserTest` → Tests=**33** / 0 / 0 / 0。
- **T3**：A4 必达 + L2 全量绿；`in1.test` 整文件 `failed=0`（nice-to-have）。
- **T4**：本文件。
- 禁止项：未改 Spec/Design/`agents/docs/manager/*`/`STATUS.md`；范围仅 onlyif/skipif 行尾 `#`。

### 变更路径

| 任务 | 路径 |
|---|---|
| T0 | 分支 `fix-onlyif-skipif-hash-comments` |
| T1 | `src/test/java/com/ggtest/parser/SqlLogicTestParserTest.java` |
| T2 | `src/main/java/com/ggtest/parser/SqlLogicTestParser.java` |
| T3 | 命令 only |
| T4 | `agents/docs/features/fix-onlyif-skipif-hash-comments/dev-notes.md` |

### 验收 A1–A5

| ID | 要求 | 结果 |
|---|---|---|
| A1 | `onlyif <engine> # …` → 正确 `dbName`，不抛「requires a database name」 | Pass（新测） |
| A2 | `skipif <engine> # …` 同上 | Pass（新测） |
| A3 | 无尾注释 `onlyif`/`skipif` 不回归 | Pass（新测 + 既有 `p1_a_*`） |
| A4 | `in1.test` 无该 parse error | Pass（CLI；且 `failed=0`） |
| A5 | 范围仅本缺陷 | Pass（diff 限 parser + 对应测试 + 本目录文档） |

### 验证

| 命令 | 结果 |
|---|---|
| T1 red（三方法，parser 未改） | Errors=**2**（`onlyif`/`skipif` requires a database name）；无尾注释 Pass |
| `mvn -q test -Dtest=SqlLogicTestParserTest` | Tests=**33** Failures=**0** Errors=**0** Skipped=**0** |
| `mvn -q clean test` | BUILD SUCCESS；Tests=**227** Failures=**0** Errors=**0** Skipped=**18** |
| `mvn -q -DskipTests package` | BUILD SUCCESS |
| `./bin/ggtest --engine sqlite --url jdbc:sqlite::memory: sqllogictest/test/evidence/in1.test`（package 后） | 无 `onlyif requires a database name`；`TOTAL: passed=1 failed=0 skipped=0` |

Skipped=18 为既有 PG/语料等门控 skip，与本修复无关。无 quality.md §6 缺口。

### 建议复测

1. Reviewer：plan A1–A5；仅 strip `onlyif`/`skipif` 行尾 `#`。
2. QA：定点 parser 单测、`mvn -q clean test`、`in1.test` CLI。

## QA 修复回执

| 缺陷 ID | 处理 | 摘要 | 验证 | 建议复测 |
|---|---|---|---|---|
| — | N/A | 本轮无 QA Fail | — | — |
