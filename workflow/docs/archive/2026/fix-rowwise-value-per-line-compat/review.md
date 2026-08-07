# Review: fix-rowwise-value-per-line-compat

## 审阅范围

| 项 | 内容 |
|---|---|
| 工作项 | `fix-rowwise-value-per-line-compat`（未拆分；`standard`；Review **required**） |
| 依据 | [`spec.md`](./spec.md)（OQ-1 = 移除）、[`plan.md`](./plan.md)、[`dev-notes.md`](./dev-notes.md)；`workflow/workflow/docs/manager/fix-rowwise-value-per-line-compat.md`；`workflow/workflow/docs/standards/{documentation,quality,security,git}.md` |
| 实现版本 | 分支 `fix-rowwise-value-per-line-compat`；commit **`8a0c236`**（相对 `main` 唯一实现提交）；目标 `main` |
| 审阅内容 | parser / model / normalize / runner 合同变更；T1–T7 全部任务；fixtures 迁移；README 中英；Plan 验收 A1–A14；安全与 Git 合规 |
| 未纳入 | 不改业务代码/测试；不作 QA 结论；不合并；不提交本报告 |

## 结论

**Approve**

无阻塞项。合同七项核验点全部通过，Reviewer 独立复跑 `mvn -q clean test`（218 run / 0 fail / 0 error / 18 skip）与 select4 sqlite 冒烟（`TOTAL: passed=1 failed=0 skipped=0`），与 dev-notes 一致。建议 Manager 调度 QA（QA 须独立复验 A1–A14，含 select4 全量）。

## 实现正确性

对照用户核验点与 Plan 验收表（证据均为 Reviewer 独立核对）：

| 验收 | 要求 | 证据 | 结果 |
|---|---|---|---|
| A3 / P0-3、P0-4 | 纯 `----` = 每值一行；无行式推断；`src/main` 无 `mixed expected line shapes` | `ExpectedResultExpander.expand`：`columnSeparator.isEmpty()` → 直接返回物理行；推断分支与混形抛错已删除；`rg 'mixed expected line shapes' src/main` 零命中（仅测试中断言其不存在） | 通过 |
| A2 / A6 / A11 / P0-2、P0-6、P1-1 | 行式仅 query 头 `separator <delim>`；消歧正确 | `SqlLogicTestParser.parseQuery`：剩 1 token → label（含 `separator` 本身）；剩 `[separator, d]` → 声明；剩 `[lbl, separator, d]` → label + 声明；其余（含 `[lbl, separator]`、delim 后多余 token）→ `ParseException`。token 经空白切分，delim 天然无空白，多字符允许（`::` 用例） | 通过 |
| A7 / P0-7 | `---- separator` 移除 → 可读解析错误 | `requireExactExpectationHeader` 仅接受 trim 后恰 `----`；`---- separator …` 报「removed; declare separator <delim> on the query header」；顶层同形亦报错；不静默当正文/SQL | 通过 |
| A5 / P0-5、P1-2 | token ≠ C → 比对失败（运行继续），非抛错中止 | `ResultComparer.compare` 捕获 expander 的 `IllegalArgumentException` 并返回 `passed()==false` 的 `CompareResult`；消息含行号、实际 token 数、`C`（`row-wise expected line 2 has 2 token(s) but type signature requires 3 column(s)`）；空 token → `(empty)` | 通过 |
| 核验点 5 | `QueryRecord.columnSeparator` = `Optional<String>`；无 `DEFAULT_COLUMN_SEPARATOR` | record 签名为 `Optional<String>`（紧凑构造校验非空、无空白）；`SqlLogicDefaults` / `SqlLogicDefaultsTest` 已删除；`rg 'DEFAULT_COLUMN_SEPARATOR\|SqlLogicDefaults\|explicitColumnSeparator' src` 零命中 | 通过 |
| A10 / A14 / P0-10 | fixtures / README 迁移；`examples/` 未入库 | `rowwise-default-space.test` → `value-per-line-spaced-text.test`（含空格 TEXT，select4 形状）；pipe / mixed fixtures 改 query 头语法；README 中英「Expected results / 期望结果」与新合同一致；`git ls-files examples/` = 0 | 通过 |
| A1 / A9 / P0-1、P0-9 | 验证证据 218/0/0；select4 failed=0 | Reviewer 独立复跑，见「验证证据摘要」 | 通过 |
| A8 / P0-8 | 哈希单行优先、口径不变 | expander 先判 `parseHashExpectation` + 单行；有/无声明 delim 均短路返回；单测 `p0_8_hashFormUnchanged_withAndWithoutDeclaredSeparator` | 通过 |
| A12 / P1-4 | 声明仅本条 `QueryRecord`，不继承 | 无文件级状态；`p1_4_nextQueryExactDashes_doesNotInheritSeparator` + `rowwise-mixed.test` | 通过 |
| A13 / P1-5 | 失败呈值行粒度 Diff | `buildDiffSummary` 未改；`p0_8_rowWiseRowsortPassesNosortFailsWithDiff` 断言 `-   ` / `+   ` 前缀 | 通过 |

行为保全核对：每值一行路径返回物理行不经 sort/flatten，与旧实现的非行式路径一致（旧代码 `rowWiseCount == 0` 时同样直接返回），与 Plan T4 一致，无回归。`ResultSorter` / `ResultHasher` / `ValueNormalizer` / CLI 未触碰，符合 Plan「不改」清单。越界检查：diff 仅含 Plan 触碰路径 + 本切片文档；未改 `workflow/workflow/docs/manager/*`、`STATUS.md`、归档。

## 测试有效性

| 要求 | 证据 | 结果 |
|---|---|---|
| 消歧全分支可证伪 | 声明 / 多字符 delim / label+声明 / 行尾 `separator` 作 label / delim 后多余 token / 拼写错 六用例；断言具体 `Optional` 值而非恒真 | 通过 |
| 旧语法负向用例 | `---- separator \|`、`---- separator ::`、空 delim、行尾空格、顶层同形均断言 `ParseException` 及消息关键词 | 通过 |
| token ≠ C 语义重写 | `p0_5_mixedTokenCounts_returnsFailedCompareNotThrow` 断言失败结果（非异常）且消息含行号/`C`，并断言无混形文案 | 通过 |
| select4 回归形状 | `p0_4_select4Shape_spacedTextValuePerLinePasses`（单测）+ `value-per-line-spaced-text.test`（Runner L2）双层锁定 | 通过 |
| L2 全量 + 冒烟 | 见「验证证据摘要」；18 skip 为 PG 门控（无 `GGTEST_PG_URL`），与 Plan 一致 | 通过 |

## 文档影响核对

| Plan 声明 | 实现是否一致 | 备注 |
|---|---|---|
| 用户文档：README 中英「期望结果」 | 是 | 两语言均：纯 `----` = 每值一行唯一默认；行式示例改 query 头；delim 约束、恰 C、trim、`(empty)`、无引号层齐备；已删默认空格行式与 `---- separator` 示例。`README.zh-CN.md` 为整文件新入库，属 Plan T6 / 工作项文档影响声明范围 |
| 开发文档：dev-notes / Javadoc / fixtures | 是 | `dev-notes.md` 含命令、退出码、关键用例与 `examples/` 自行更新清单；`QueryRecord` / `ExpectedResultExpander` / parser Javadoc 已随合同重写 |
| 运维文档 | N/A | 与 Plan 一致 |

## 安全影响核对

| 检查项 | 结果 | 备注 |
|---|---|---|
| 敏感信息 | 通过 | diff 无密钥/凭据/`.env`；连接串仅 `jdbc:sqlite::memory:` 占位形态 |
| 输入处理 | 通过 | 变更面为 `.test` 文本解析：非法输入均落可读 `ParseException` 或失败比对，无注入面扩大 |
| 认证/授权/文件操作/外部访问/依赖 | N/A | 无变更；无新增依赖 |

无未解决安全问题。

## Git 合规

| 检查 | 结果 |
|---|---|
| 工作分支 | `fix-rowwise-value-per-line-compat`（源）→ `main`（目标），与工作项记录一致 |
| 提交 `8a0c236` | Conventional Commits（`fix(rowwise): …`）；21 文件 = 实现 + 测试 + fixtures + README + 本切片 spec/plan/dev-notes；单一逻辑变更 |
| 禁止提交项 | 通过：无构建产物、无 `.env`；`examples/` 未入库；`workflow/workflow/docs/manager/*`、`STATUS.md` 未提交 |

## 必修项

| ID | 位置 | 问题 | 状态 |
|---|---|---|---|
| — | — | 无 | — |

## 非阻塞建议

| ID | 严重度 | 位置 | 说明 |
|---|---|---|---|
| N1 | low | `SqlLogicTestParser.parseQuery` | `query III separator \| extra`（P1-1）报错文案为 "unexpected tokens in query header after label"，语义上应指向 `separator <delim>` 之后；仍属可读解析错误，合同不约束具体措辞 |
| N2 | low | `looksLikeRemovedSeparatorExpectationHeader` | `----separator…`（无空格粘连）也命中「removed」提示；本就是非法头且仍报解析错误，仅提示语略宽 |

## 验证证据摘要（审阅者独立，2026-07-26）

| 命令 | 结果 |
|---|---|
| `mvn -q clean test` | 退出码 0；surefire 汇总 **218** run / **0** fail / **0** error / **18** skip |
| `rg -n 'mixed expected line shapes' src/main` | 零命中 |
| `rg -n 'DEFAULT_COLUMN_SEPARATOR\|SqlLogicDefaults\|explicitColumnSeparator' src` | 零命中 |
| `git ls-files examples/` | 0 个文件（未入库） |
| `mvn -q package -DskipTests` + `./bin/ggtest --engine sqlite --url 'jdbc:sqlite::memory:' examples/select4.test` | 退出码 0；`TOTAL: passed=1 failed=0 skipped=0` |

### 未验证缺口

| 未验证项 | 原因 | 风险 | 恢复条件 | 复测范围 |
|---|---|---|---|---|
| PG 门控测试（18 skip）与 PG 冒烟 | 无 `GGTEST_PG_URL`；Plan 列为可选 | PG executor 路径未实跑（本变更不触碰 db 层，风险低） | 提供 PG 环境 | QA 可选 |
| 其余 `select*.test` 全量 | Plan 将全量复验划归 QA | select4 以外官方位点未在 Review 实跑 | QA 阶段执行 | QA 必跑 select4 全量，其余可选 |

## 后续动作

1. Manager：Review 门禁（required）已满足 → 可调度 **QA**（本报告 ≠ QA Pass）。
2. QA：独立复验 A1–A14；全量 select4（其余 `select*.test` 与 PG 可选）；写 `qa-report.md`。
3. 用户侧：本地 `examples/demo.slt` / `demo2.slt` 按 dev-notes 清单自行迁移（不入库）。
4. 本 `review.md` 不由 Reviewer 提交；由 Manager 按 `git.md` §1.4 择机提交。

## 合入前文档/示例补充复审（2026-07-26）

### 范围与版本

- 审阅版本：commit `8a0c236` + 当前工作区未提交的 `README.md`、`README.zh-CN.md`、`examples/demo.slt`、`examples/demo_zh.slt`、`examples/demo2.slt` 与 `dev-notes.md` 补充。
- 本轮仅审阅用户文档与示例；工作区无 Java 变更。原实现轮 **Approve** 与 QA 结论不在本轮重判范围。
- 本节取代原报告中“`examples/` 不入库”及要求用户自行迁移 demo 的陈述；本轮仅 `demo.slt`、`demo_zh.slt`、`demo2.slt` 纳入范围，`select*.test` 仍为本地语料。
- 依据：工作项记录「本轮追加范围」、Spec rowwise 合同、Plan 文档影响，以及 `documentation.md`、`quality.md`、`security.md`、`git.md`。

### 核对结果

| 检查项 | 结论与证据 |
|---|---|
| `demo.slt` 公开功能覆盖 | 通过：包含 `statement ok/error`；query 类型 I/T/R；`nosort`/`rowsort`/`valuesort`；label；query 头 `separator \|` 与 `separator ,`；纯 `----` 每值一行及含空格 TEXT；NULL/`(empty)`；execute-only；`skipif`/`onlyif`；`hash-threshold` + MD5；`halt` 后不可执行记录；文件头给出 SQLite/PostgreSQL 命令。 |
| `demo_zh.slt` 行为对等 | 通过：SQL、记录顺序、query 头、期望值、条件与 halt 均与 `demo.slt` 对等，仅说明性注释和单文件运行命令语言化。 |
| README 中英 | 通过：均明确纯 `----` 无空格行式推断、行式仅由 query 头 `separator <delim>` 声明，且 `---- separator …` 为已移除语法；运行命令与成功报告指向 `demo.slt` + `demo_zh.slt`。 |
| 示例可执行性 | Reviewer 独立执行 `./bin/ggtest --engine sqlite --url jdbc:sqlite::memory: examples/demo.slt examples/demo_zh.slt`，退出码 0，`TOTAL: passed=2 failed=0 skipped=0`；`demo2.slt` 单独冒烟亦为 `passed=1 failed=0 skipped=0`。 |
| 文档影响 | 通过：README 中英、主 showcase、中文 showcase、demo2 文件头与 dev-notes 均已覆盖；无运维文档影响。 |
| 安全影响 | 通过：检查范围为本轮 README 与示例内容；未发现真实凭据、令牌、私钥或生产连接信息。PostgreSQL 命令仅使用 localhost、用户名与 `[--password ...]` 占位符；无认证、授权、依赖、文件或外部网络行为变更。 |
| Git 合规 | 通过但需选择性入库：当前位于源分支 `fix-rowwise-value-per-line-compat`，本轮零 commit；`examples/select*.test` 为本地未跟踪语料，不属于本轮入库范围，后续不得因 `examples/` 整目录添加而误提交。Reviewer 不执行 add/commit/push。 |

### 发现项与结论

阻塞项：无。非阻塞建议：无。

**Approve** — 合入前文档/示例补充满足工作项清单；原 Review Approve 仍有效。本轮新增用户可见示例应由 QA 按工作项安排执行 demo SQLite 冒烟；本结论不替代 QA。
