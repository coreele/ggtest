# Plan: fix-rowwise-value-per-line-compat

> 实施与验证计划（重写版，取代 2026-07-26 混形回退小修 Plan）。standard；Spec 门禁已满足，Design 门禁 skipped。
>
> **适用对象**：Developer、Reviewer、QA、Manager。  
> **前置条件**：源分支 `fix-rowwise-value-per-line-compat`；JDK 17+、Maven 3.8+；[spec.md](./spec.md)（唯一合同依据，有意取代归档 `ggtest-rowwise-expected`）。  
> **阅读顺序**：元信息 → 目标 → 任务 → 依赖 → 触碰路径 → 验证 → 验收 → 待确认（OQ-1）→ 文档影响 → 交接。  
> **预期结果**：纯 `----` 永远每值一行；行式仅经 query 头 `separator <delim>`；`---- separator` 移除；select4 位点 0 失败；`mvn -q clean test` 退出码 0。  
> **失败处理**：见「无法执行验证时的处理」。  
> **确认**：本 Plan 须经用户确认（含 **OQ-1** 拍板）；Manager 持久化后才可设 `planned` 并调度 Developer。

## 元信息

- 工作项标识: fix-rowwise-value-per-line-compat（未拆分；sub-feature-id = feature-id）
- 依据 Spec: [workflow/docs/features/fix-rowwise-value-per-line-compat/spec.md](./spec.md)（required，已满足；合同由用户 2026-07-26 逐条拍板）
- 依据 Design: N/A（Design 门禁 skipped）；分层以 [architecture-overview/design.md](../architecture-overview/design.md) 为准（parser / model / normalize / runner 既有边界内实现）
- 路径等级: standard
- Review 门禁: **required**（进入 QA 前须 Reviewer `Approve`）
- 最低验证层: **L2**（单元测试 + 全量构建）+ sqlite 语料冒烟
- 验证命令: `mvn -q clean test`（必选）；`./bin/ggtest --engine sqlite --url 'jdbc:sqlite::memory:' examples/select4.test`（冒烟；QA 全量复验）
- 源分支: `fix-rowwise-value-per-line-compat` → `main`
- 关联工作项: [`workflow/docs/manager/fix-rowwise-value-per-line-compat.md`](../../manager/fix-rowwise-value-per-line-compat.md)

## 适用工程规范

- [文档工程](../../standards/documentation.md)
- [Git 协作](../../standards/git.md)
- [质量与验证](../../standards/quality.md)
- [安全](../../standards/security.md)

## 目标摘要

按 Spec 合同（D-1 ~ D-6）实施合同变更：

| # | 合同 | 落点 |
|---|---|---|
| 1 | 纯 `----` 永远每值一行；删除行式推断与 `mixed expected line shapes` | `ExpectedResultExpander`、`ResultComparer` |
| 2 | 行式仅经 query 头 `query <类型串> [nosort\|rowsort\|valuesort] [label] [separator <delim>]`；声明后每行按 `delim` 字面量拆分、恰 `C` token 否则可读失败、token trim、空 → `(empty)` | `SqlLogicTestParser`、`QueryRecord`、`ExpectedResultExpander` |
| 3 | 消歧：`separator`+恰一 token → 声明；`separator` 为行尾最后 token → label；`delim` 单 token 无空白（多字符允许）；delim 后多余 token → 解析错误 | `SqlLogicTestParser` |
| 4 | 移除 `---- separator <delim>` 期望头 → 可读解析错误（**OQ-1** 默认，随本 Plan 确认拍板） | `SqlLogicTestParser` |
| 5 | 哈希单行 `N values hashing to …` 优先识别，口径不变 | `ExpectedResultExpander`（保持） |

**禁止：** 空格猜行式；`src/main` 保留 `mixed expected line shapes` 文案/抛错路径；静默把 `---- separator` 当期望正文或 SQL；行式期望引号壳；改 MD5 / I / T / R 规范化与 sort/flatten 语义；入库用户本地 `examples/`（含 `select*.test`、`demo.slt`、`demo2.slt`）。

依据：spec.md「合同」「已决议」；工作项记录「用户决策」「失败证据」（select4 ~47398 `query ITII rowsort`，TEXT `table tn7 row 92` 被误判混形）。

## 任务拆解

TDD：T1、T2 **先**写失败单测，T3、T4 实现后转绿。`QueryRecord` 形状变更导致 T1–T4 期间编译耦合（runner / comparer 签名连动），允许在同一提交内收敛，但断言先于实现编写。

### T1 — 失败单测（parser）：query 头 `separator` 消歧 + `---- separator` 移除

`SqlLogicTestParserTest`：

1. **新增消歧用例**（Spec「合同 / API」；P0-2、P0-6、P1-1）：
   - `query IIT nosort separator |` → delim `|`、无 label（目标书写）；
   - `query I separator ::` → 多字符 delim `::`；
   - `query III nosort lbl separator |` → label `lbl` + delim `|`；
   - `query III nosort separator`（行尾）→ label `separator`、**非**声明（P0-6）；
   - `query III separator | extra` → 可读解析错误（P1-1）；
   - `query III nosort seperator |`（拼写错）→ 按 label + 多余 token → 解析错误（合同「错误拼写不生效或明确报错」）。
2. **重写 `---- separator` 用例**（P0-7，OQ-1 默认）：既有正向用例（`expectationHeader_pipeSeparator_bindsToThisQueryOnly`、`p0_3_nextQueryExactDashes_defaultsToSpaceNotInherited`、`p0_2_targetWriting_iitPipeBareTextExpectationHeader`、`expectationHeader_spaceLiteralDelim_allowed`、`expectationHeader_multiCharDelim_allowed`、`expectationHeader_emptyDelim_*`、`expectationHeader_trailingSpaceOnly*`）改为：`---- separator …` 一律可读解析错误（不得静默当正文/SQL）；作用域不继承（P1-4 parser 侧）改由新语法用例覆盖。
3. **保持**：恰 `----` 为合法期望头；顶层 `----…` 与其他非法 `----…` 可读解析失败；`QueryRecord` 断言改用新形状（见 T3）。

**完成条件：** 用例按新合同书写并在 T3 前失败（红）；不依赖 `examples/`。

### T2 — 失败单测（normalize）：声明才拆 + 每值一行 + select4 形状

`ResultComparerTest`：

1. **select4 回归形状**（P0-4）：类型 `ITII`、恰 `----`（无声明）、期望正文 `51732` / `table tn7 row 92` / `511` / `84280`，实际四值对齐 → 通过；不得出现 `mixed expected line shapes`。
2. **默认空格行式废止**（P0-3）：`query III` 形状 + 正文 `1 2 3`、实际 `(1,2,3)` → **失败**（期望为单值 `"1 2 3"`）；重写 `p0_1_defaultSpaceRowWiseExpectedPasses`、`p0_1_consecutiveSpacesStillEmptyToken_defaultPathNoTrim` 断言。
3. **声明行式**（P0-2、P1-2、P1-3）：delim `|`、`C=3`：`1 | 1 | hello world` 通过（trim）；`1||3` → `["1","(empty)","3"]`；`p0_2_*` / `p0_4_*` / `rowWise_emptyTokenAndEmptyLiteral_*` / `p1_4_literalQuotesAreCellContent_*` 迁移到「声明 delim」参数形态。
4. **重写 `mixedTokenCounts_throwsReadableAlignmentFailure`**（P0-5）：声明 `|`、`C=3`、某行 token ≠ 3 → 比对结果 `passed()==false`，失败摘要含行号、实际 token 数与 `C`；**不得**要求抛 `IllegalArgumentException`、不得中止整个文件运行。
5. **哈希优先**（P0-8）：单行 `N values hashing to <md5>` 有/无声明 delim 均走哈希路径、口径不变；`p0_7_*` 迁移（删除默认空格行式前提，改每值一行或声明形态）；`p0_8_rowWiseRowsortPassesNosortFailsWithDiff` 改声明形态（P1-5 Diff 值行粒度）。

**完成条件：** 用例按新合同书写并在 T4 前失败（红）。

### T3 — 实现（model + parser）

1. `QueryRecord`：`columnSeparator` 语义改为**仅显式声明才有**——收敛 `columnSeparator` + `explicitColumnSeparator` 两字段为 `Optional<String> columnSeparator`（空 = 每值一行）；校验声明值非空且不含空白。
2. `SqlLogicTestParser.parseQuery`：类型串 → 可选 sort → 剩余 token 消歧：
   - 剩 `[x]` → label（含 `x == "separator"`，P0-6）；
   - 剩 `[separator, d]` → 声明，delim = `d`（天然单 token 无空白）；
   - 剩 `[lbl, separator, d]` → label + 声明；
   - 其余多余 token（含 `separator` 后 ≥2 token、`[lbl, separator]`）→ 可读解析错误。
3. 期望头收敛（OQ-1 默认）：仅 trim 后恰 `----` 合法；`---- separator …` → 可读解析错误（消息提示已移除、改用 query 头 `separator <delim>`）；其他 `----…` 保持可读解析失败。删除 `parseSeparatorExpectationHeader` / `ExpectationHeader.explicit`；顶层 `----` 报错消息同步更新。
4. `SqlLogicDefaults.DEFAULT_COLUMN_SEPARATOR`：随「默认列分隔符」概念消亡删除（类仅含该常量则整类删除）；`ResultComparer.DEFAULT_COLUMN_SEPARATOR` 转发常量一并删除（Spec：不冻结对外库 API）。

**完成条件：** T1 转绿；`rg 'separator' src/main/java/com/ggtest/parser` 仅剩 query 头路径与报错文案。

### T4 — 实现（normalize + runner）

1. `ExpectedResultExpander.expand`：签名改 `Optional<String> columnSeparator`；顺序：单行哈希优先返回 → 无声明 → **直接返回物理行**（每值一行，无 token 计数）→ 有声明 → 每行 `splitLiteral` + token trim + 恰 `C` 校验（不符 → 可读失败：行号、实际 token 数、`C`）+ 空 token → `(empty)` → `ResultSorter.sortAndFlatten`。**删除**推断分支与 `mixed expected line shapes` 抛错；Javadoc 重写。
2. `ResultComparer`：explicit / 非 explicit 双路径收敛为一个带 `Optional<String>` 的主重载 + 每值一行便捷重载；token ≠ `C` 呈现为**失败的比对结果**（`passed()==false`、摘要可读），复用既有 `[Diff]` 值行粒度。
3. `SqlLogicTestRunner`：接线 `record.columnSeparator()`（Optional）到新签名；每条 query 作用域独立（不继承）。

**完成条件：** T2 转绿；`rg 'mixed expected line shapes' src/main` 零命中。

### T5 — fixtures 与 Runner 验收测试迁移

1. `rowwise-default-space.test`：默认空格行式不再合法 → 迁移为**每值一行**并加入含空格 TEXT 单元格（L2 锁 P0-4 select4 形状；文件可更名为语义相符名称）。
2. `rowwise-pipe-separator.test`：`---- separator |` → `query IIT nosort separator |` + 恰 `----`（P0-2 目标书写）。
3. `rowwise-mixed.test`：行式块改新语法；「下一条恰 `----` 不继承 `|`」块改每值一行断言作用域（P1-4）；哈希块保持（P0-8）。
4. `RunnerAcceptanceTest`：三个 fixture 用例的文件名 / 断言计数同步。

**完成条件：** `RunnerAcceptanceTest` 全绿；fixtures 与 Spec「范围与可见行为」一致。

### T6 — README 中英「Expected results / 期望结果」

`README.md`、`README.zh-CN.md` 对应小节（P0-10）：

1. 纯 `----` = 每值一行（唯一默认形态）；删除「默认空格行式」表行与 `---- separator` 示例。
2. 行式示例改 query 头：`query IIT nosort separator |` + `----` + `1 | 1 | hello world`。
3. 说明：delim 单 token 无空白、多字符允许；恰 `C` token 否则可读失败；token trim；空 token → `(empty)`；单元格含 delim 时换分隔符或改每值一行（无引号层）。

**完成条件：** 中英内容一致且与 Spec 合同一致。

### T7 — 全量验证与实施记录

1. `mvn -q clean test` → 退出码 0。
2. 冒烟：`./bin/ggtest --engine sqlite --url 'jdbc:sqlite::memory:' examples/select4.test` → 0 失败（本地有语料；P0-1）。
3. `rg -n 'mixed expected line shapes' src/main` → 零命中（P0-9）。
4. `dev-notes.md`：命令、退出码、关键用例名；**提示用户自行更新本地 `examples/`**（`demo.slt` 含默认空格行式、`demo2.slt` 含 `---- separator |`，新合同下前者比对失败、后者解析错误）；`examples/` 不入库。

**完成条件：** L2 + 冒烟证据齐全；未执行项按「无法执行验证」记录。

## 依赖与顺序

```text
T1（parser 红）──┐
T2（normalize 红）┼──► T3（model+parser）──► T4（normalize+runner）──► T5（fixtures/Runner）──► T7（全量验证/记录）
                 │                                                      T6（README）──────────┘
（T1、T2 可并行；T6 可与 T5 并行；T1–T4 允许同一提交内收敛编译耦合）
```

**禁止：** 无关重构；提交 `.env`（其余禁令见「目标摘要」与「触碰路径」）。

## 触碰路径

| 任务 | 路径 |
|---|---|
| T1 | `src/test/java/com/ggtest/parser/SqlLogicTestParserTest.java` |
| T2 | `src/test/java/com/ggtest/normalize/ResultComparerTest.java` |
| T3 | `src/main/java/com/ggtest/parser/SqlLogicTestParser.java`；`src/main/java/com/ggtest/model/QueryRecord.java`；`src/main/java/com/ggtest/model/SqlLogicDefaults.java`（删除或裁剪） |
| T4 | `src/main/java/com/ggtest/normalize/ExpectedResultExpander.java`；`ResultComparer.java`；`src/main/java/com/ggtest/runner/SqlLogicTestRunner.java` |
| T5 | `src/test/resources/fixtures/runner/rowwise-*.test`（迁移/更名）；`src/test/java/com/ggtest/runner/RunnerAcceptanceTest.java` |
| T6 | `README.md`；`README.zh-CN.md` |
| T7 | Maven 套件；`workflow/docs/features/fix-rowwise-value-per-line-compat/dev-notes.md` |

**不改：** `ValueNormalizer`、`ResultHasher`、`ResultSorter`、CLI 层。  
**禁止触碰：** `workflow/docs/manager/*`、`STATUS.md`、spec.md、归档、用户本地 `examples/`。

## 验证

| 项 | 内容 |
|---|---|
| 最低验证层 | **L2**（单元 + 全量构建）+ sqlite 语料冒烟 — 合同全落在 parser/normalize 纯逻辑，单测可完整锁定消歧、拆分、错误文案；select4 冒烟锁端到端位点（QA 复验全量） |
| 验证命令（必选） | `mvn -q clean test`；`rg -n 'mixed expected line shapes' src/main` |
| 验证命令（冒烟） | `./bin/ggtest --engine sqlite --url 'jdbc:sqlite::memory:' examples/select4.test` |
| 预期证据 | 退出码 0、Failures=0/Errors=0；select4 报告 0 失败；rg 零命中；T1/T2 用例名可追溯 |

### 无法执行验证时的处理

| 未验证项 | 原因 | 风险 | 恢复条件 |
|---|---|---|---|
| T7 `mvn` | JDK/Maven 不可用 | 消歧/拆分/错误路径回归未检出 | 环境恢复后重跑得退出码 0 |
| select4 冒烟 | 本地无 `examples/select4.test` 或无 sqlite 驱动 | P0-1 端到端未验 | 具备语料与引擎后重跑；期间以 T2/T5 的 select4 形状单测 + fixture 为替代证据；**不得**因此入库或修改 examples |
| T7 失败 | 实现偏离合同 | 误伤哈希/排序或新语法失效 | 修 T1–T4 后重跑；记入 `dev-notes.md` |

**禁止**静默跳过必选 L2。

## 验收

对应 spec.md「验收（Given-When-Then）」：

| ID | Spec 条目 | 证据 |
|---|---|---|
| A1 | P0-1 select4 全量 0 失败 | T7 冒烟 + QA 全量 |
| A2 | P0-2 / P1-3 `separator \|` 新语法通过 | T1/T2 用例 + T5 fixture |
| A3 | P0-3 默认空格行式废止（`1 2 3` = 单值） | T2 用例 |
| A4 | P0-4 含空格 TEXT 每值一行通过、无混形错 | T2 select4 形状用例 + T5 fixture |
| A5 | P0-5 / P1-2 token ≠ C 可读失败（行号/实际/`C`）；空 token → `(empty)` | T2 用例 |
| A6 | P0-6 `separator` 行尾作 label（向后兼容） | T1 用例 |
| A7 | P0-7 `---- separator` 可读解析错误（OQ-1 默认） | T1 用例 |
| A8 | P0-8 哈希单行优先、口径不变 | T2 用例 + T5 fixture |
| A9 | P0-9 `mvn -q clean test` 退出码 0；`src/main` 无 `mixed expected line shapes` | T7 |
| A10 | P0-10 fixtures / README 中英迁移到新语法 | T5、T6 + Review |
| A11 | P1-1 delim 后多余 token → 解析错误 | T1 用例 |
| A12 | P1-4 声明仅本条 QueryRecord，不继承 | T1/T5 |
| A13 | P1-5 失败呈值行粒度 `[Diff]` | T2 用例 |
| A14 | 未入库用户本地 `examples/` | diff / Review 检查 |

## 待确认（随本 Plan 拍板）

| 编号 | 问题 | 默认（本 Plan 按此编写） | 若否决的影响 |
|---|---|---|---|
| **OQ-1** | 是否确认移除 `---- separator <delim>` 期望头？ | **移除**：遇之可读解析错误（T1.2、T3.3、T5、T6 按移除编写） | 保留兼容解析（仍为期望头 + 本条 delim，类归档 R1），同时保留 query 头 `separator`；T1.2 改为兼容断言、T3.3 保留解析分支、T5/T6 保留旧示例说明 |

## Review 门禁与进入 QA

- Review：**required**。Reviewer `Approve` 为进入 QA 的前置。检查项：消歧实现与 Spec 合同逐条一致；`src/main` 无混形抛错路径；`mixedTokenCounts` 语义重写为失败结果而非异常；fixtures / README 与新语法一致；未入库 `examples/`；测试覆盖 P0/P1 关键路径（quality.md §3）。
- 进入 QA：T1–T7 完成；`dev-notes.md` 含 L2 + 冒烟证据；Review = Approve。
- QA：独立复验 A1–A14；**全量** select4（及本地其余 `select*.test` 可选）；写 `qa-report.md`。

## 文档影响

| 类别 | 更新路径或 N/A |
|---|---|
| 用户文档 | `README.md`、`README.zh-CN.md`「Expected results / 期望结果」（T6） |
| 开发文档 | `dev-notes.md`（含 examples 自行更新提示）；`ExpectedResultExpander` / `QueryRecord` / `SqlLogicTestParser` Javadoc 随实现更新；fixtures 迁移（T5） |
| 运维文档 | **N/A** — 无部署/排障变更 |

## 交接顺序

1. Planner 完成本 `plan.md` → **等待用户确认（含 OQ-1）**（本步）。
2. Manager 将确认（含 OQ-1 结论）持久化到工作项记录 → 状态 `planned` → 调度 Developer。
3. Developer：T1→T7（TDD）；写 `dev-notes.md`。
4. Reviewer：`review.md`；须 `Approve`。
5. QA：独立复验（含 select4 全量）；`qa-report.md`。
6. 合入：用户授权后；源 `fix-rowwise-value-per-line-compat` → `main`。Planner **不**改 STATUS / 工作项记录。

## 修订记录

| 日期 | 摘要 |
|---|---|
| 2026-07-26 | 按新 Spec 整体重写，取代混形回退小修 Plan；显式 `separator` 声明 + 移除推断与 `---- separator`；OQ-1 随 Plan 拍板；documentation.md §B 自检 精简 |
