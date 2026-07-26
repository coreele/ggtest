# QA Report: fix-rowwise-value-per-line-compat

## 轮次

| 轮次 | 日期 | 实现版本 | 环境 | 范围 | 结论 |
|---|---|---|---|---|---|
| 1 | 2026-07-26 | `8a0c236` @ `fix-rowwise-value-per-line-compat` | 本地 macOS；JDK/Maven；sqlite 内存；语料 `examples/select4.test`（未入库） | Spec P0/P1 + Plan A1–A14；L2 + select4 全量 | **Pass** |
| 2 | 2026-07-26 | `8a0c236` + 当前工作区文档/示例补充 @ `fix-rowwise-value-per-line-compat` | 本地 macOS；sqlite 内存 | 合入前 README 路径核对；`demo.slt` / `demo_zh.slt` 冒烟 | **Pass** |

## 入口门禁

| 条件 | 结果 |
|---|---|
| Plan 用户确认已持久化 | **满足** — 工作项：Plan **approved**（2026-07-26）；OQ-1=移除 |
| Review 门禁（standard / required） | **满足** — `review.md` **Approve** |
| 源分支 | **满足** — 当前分支 `fix-rowwise-value-per-line-compat`；HEAD `8a0c236` |
| 可验收实现 + Plan 验证要求 | **满足** |

## 环境与命令（QA 独立执行）

| 命令 | 结果 |
|---|---|
| `mvn -q clean test` | 退出码 0；**Tests run=218, Failures=0, Errors=0, Skipped=18** |
| `rg 'mixed expected line shapes' src/main` | **零命中** |
| `mvn -q package -DskipTests` 后 `./bin/ggtest --engine sqlite --url 'jdbc:sqlite::memory:' examples/select4.test` | 退出码 0；`TOTAL: passed=1 failed=0 skipped=0`（~5952 ms） |
| `git ls-files examples/` | **0** |
| 可选 PG 同语料（`--engine postgres --url 'jdbc:postgresql://localhost:5432/postgres' --user postgres`） | **环境失败**：5432 开但 `connection failed`；`passed=0 failed=1` — Plan 可选，**非阻塞** |

显式 `--engine sqlite`（避免本机 `.env` 默认 postgres）。`clean test` 会清 JAR，冒烟前须 `package`。

## 覆盖（Spec P0/P1 ↔ Plan A1–A14）

| ID | 条目 | 结果 | 证据 |
|---|---|---|---|
| A1 / P0-1 | select4 全量 0 失败（含空格 TEXT 位点） | **Pass** | sqlite `failed=0`；`p0_4_select4Shape_spacedTextValuePerLinePasses`；`value-per-line-spaced-text.test` |
| A2 / P0-2、P1-3 | query 头 `separator \|` 行式通过 | **Pass** | `queryHead_separatorPipe_noLabel_bindsDelim`；`rowwise-pipe-separator.test`；README 中英示例 |
| A3 / P0-3 | 默认空格行式废止：`1 2 3`→单值比对失败 | **Pass** | `p0_3_defaultSpaceRowWiseAbolished_singleValueFails`：`passed()==false`，`expectedView()==["1 2 3"]` |
| A4 / P0-4 | 含空格 TEXT 每值一行；无混形错 | **Pass** | `p0_4_*` + fixture；无声明时 expander 直接返回物理行；`rg` 零命中 |
| A5 / P0-5、P1-2 | token≠C→比对失败（行号/实际/C），非中止；空→`(empty)` | **Pass** | `p0_5_mixedTokenCounts_returnsFailedCompareNotThrow`；`p1_2_rowWise_emptyTokenAndEmptyLiteral_alignWithTextEmpty` |
| A6 / P0-6 | 行尾 `separator` 作 label | **Pass** | `p0_6_queryHead_trailingSeparatorToken_isLabelNotDeclaration` |
| A7 / P0-7 | `---- separator`→可读解析错误（OQ-1） | **Pass** | `p0_7_expectationHeader_separatorRemoved_throwsReadableParseException`；文案含 `removed; declare separator <delim> on the query header` |
| A8 / P0-8 | 哈希单行优先、口径不变 | **Pass** | `p0_8_hashFormUnchanged_withAndWithoutDeclaredSeparator`；expander 先 `parseHashExpectation` |
| A9 / P0-9 | `mvn` 绿；`src/main` 无混形文案 | **Pass** | 218/0/0/18；`rg` 零命中 |
| A10 / P0-10 | fixtures / README 中英迁新语法 | **Pass** | `value-per-line-*` + pipe/mixed；README：纯 `----`=每值一行；行式=query 头；`---- separator`=解析错误 |
| A11 / P1-1 | `separator \| extra`→解析错误 | **Pass** | `p1_1_queryHead_separatorThenExtraToken_throwsReadableParseException` |
| A12 / P1-4 | 声明仅本条、不继承 | **Pass** | `p1_4_nextQueryExactDashes_doesNotInheritSeparator`；`rowwise-mixed.test` |
| A13 / P1-5 | 失败呈值行粒度 `[Diff]` | **Pass** | `p0_8_rowWiseRowsortPassesNosortFailsWithDiff` |
| A14 | 未入库 `examples/` | **Pass** | `git ls-files examples/` = 0 |

## 回归

| 范围 | 结果 | 证据 |
|---|---|---|
| L2 全量 | **Pass** | 218/0/0/18（18 skip=PG 门控，预期） |
| select4 sqlite | **Pass** | `failed=0` |
| 无混形 / 无旧默认分隔符 API | **Pass** | `src/main` 无混形文案；`src` 无 `DEFAULT_COLUMN_SEPARATOR` / `SqlLogicDefaults` / `explicitColumnSeparator` |
| 其余 `select*.test` | **未跑**（可选） | Plan 仅强制 select4 |
| PG 冒烟 / PG 门控 | **未实跑成功** | 连接失败；见下表 |

## 文档与安全

| 项 | 结果 |
|---|---|
| README 中英「期望结果」 | **Pass** — 与 Spec 一致；无默认空格行式正例 |
| `dev-notes.md` | **Pass** — L2/冒烟证据 + 本地 `examples/` 迁移提示 |
| 安全（`security.md`） | **Pass / 允许合并** — 范围：`.test` 解析与比对；无凭据/`.env` 入库；非法输入→`ParseException` 或失败比对；无认证/授权/外部访问/依赖变更。发现项：无 |

## 已知未实跑（非阻塞）

| 未验证项 | 原因 | 风险 | 恢复条件 | 复测范围 |
|---|---|---|---|---|
| PG 冒烟 + 18 PG 门控测 | postgres 连接失败 | 低；不改 db/CLI executor | 提供可达 PG（`GGTEST_PG_*` 或等价） | PG 门控 + 可选 select4 PG |
| 其余 `select*.test` | Plan 可选 | 低 | 有语料时 sqlite 全量跑 | 对应语料 |

## 缺陷

无。

## 结论

- **总体：Pass**
- 恢复条件：N/A
- 合并：**待用户授权**（质量条件已满足，可请求 merge 授权；QA 不 commit `qa-report.md`、不合并）

## 轮次 2：合入前文档/示例回归

### 版本、范围与门禁

- 日期：2026-07-26。
- 实现版本：`8a0c236` + 当前工作区未提交的 README、示例与相关文档补充；分支 `fix-rowwise-value-per-line-compat`。
- 范围：仅验收本轮用户可见 README 与示例；未重跑已在轮次 1 通过的 Maven / select4。
- 门禁：Plan `approved`；standard / required Review 已对本轮补充给出 **Approve**；存在可执行示例。

### 命令与证据

| 检查 | 独立结果 |
|---|---|
| `./bin/ggtest --engine sqlite --url jdbc:sqlite::memory: examples/demo.slt examples/demo_zh.slt` | 退出码 **0**；`TOTAL: passed=2 failed=0 skipped=0`；两文件均 `[PASSED]` |
| README 示例路径 | `README.md` 与 `README.zh-CN.md` 均指向 `examples/demo.slt`、`examples/demo_zh.slt`；两路径存在，且已由上条命令实际跑通 |
| rowwise 用户文档 | 中英文 README 均声明纯 `----` 为每值一行、行式仅使用 query 头 `separator <delim>`，并说明 `---- separator …` 已移除 |

### 回归、文档与安全

- 回归：主展示与中文展示联合运行通过；`demo2.slt` 为可选，本轮未重跑（Reviewer 已独立通过），不构成证据缺口。
- 文档：运行前置条件、命令、成功 `TOTAL` 示例、退出码与失败报告示例齐备；README 指向的本轮示例路径有效。
- 安全：检查本轮 README 与 `examples/demo*.slt`；未发现真实凭据、令牌、私钥或生产连接信息；命令仅含 sqlite 内存 URL / PostgreSQL 占位参数。发现项：无；处置状态：N/A；允许继续合并。

### 缺陷与结论

- 缺陷：无。
- 阻塞：无。
- **本轮结论：Pass**。
