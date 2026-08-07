# Spec: ggtest-rowwise-expected

> 需求与规格（Plan 之前完成）。任务拆解见后续同目录 `plan.md`；架构选型见同目录 `design.md`（Design 门禁 required）。
>
> **feature-id**：`ggtest-rowwise-expected` · **sub-feature-id**：`ggtest-rowwise-expected`（未拆分）
> **适用对象**：Planner（Design/Plan）、Developer、QA、Manager
> **前置条件**：工作项 [`agents/docs/manager/ggtest-rowwise-expected.md`](../../manager/ggtest-rowwise-expected.md)；归档 [`agents/docs/archive/2026/ggtest-core/ggtest-core-normalize/spec.md`](../../archive/2026/ggtest-core/ggtest-core-normalize/spec.md)；实现依据 `ResultSorter.normalizeAndSort` → `flatten`、`ResultComparer.splitExpectedLines`；失败 Diff 见归档 [`agents/docs/archive/2026/ggtest-cli-report/spec.md`](../../archive/2026/ggtest-cli-report/spec.md)。
> **阅读顺序**：背景与目标 → 非目标 → 范围与可见行为 → 合同 → 验收 → 已决议 / 开放问题。
> **预期结果**：按已决议合同实现并验收行式期望、本条期望头 `---- separator`、显式 trim，以及「每值一行」兼容与哈希/Diff 硬约束；**无**行式期望单引号语法壳。
> **失败处理**：偏离本 Spec 或归档 normalize / cli-report 合同时修回；不得破坏官方「每值一行」语料。
>
> **确认状态（full 路径）**：2026-07-25 **approved**（OQ-1…OQ-7）；2026-07-26 第三次修订（R1/R2/R3）曾批准；**2026-07-26 第四次：废止 R3（「不要加引号层」）已批准**，本文件为冻结合同。IC-1 **作废**。原 OQ-2 文件级作用域 **废止**（以 R1 为准）。**R3 已废止**。Design/Plan/实现须随本修订回写后重审。

## 背景与目标

- 用户按 sqllogictest-rs 写期望 `1 2 3`（`query III` + `----`）失败；实际侧规范化为每值一行。根因（勿再调查）：归档 normalize 要求期望/实际均「每个值一行」；`flatten` + `splitExpectedLines` 仅按换行切分。与官方 C 一致；rs 为「一行一整结果行、列间分隔」。
- **目标书写（已确认；期望侧无引号壳）**：

```text
query IIT nosort
SELECT 1, 1, 'hello world'
---- separator |
1 | 1 | hello world
```

  SQL 侧 `'hello world'` 是 SQL；期望侧裸文本 `hello world`（非期望语法引号壳）。
- **目标**：（1）行式期望兼容（按 `C` 与本条 `S` 推断；同文件可与每值一行混用）；（2）`---- separator <delim>` 写在本条期望头（默认 U+0020；**仅本条**）；（3）显式 separator 时 token trim；（4）默认仍每值一行，不破坏官方语料/既有 fixtures；（5）哈希与 Diff **值行**粒度不变；（6）**不**引入行式期望单引号/`''` 语法壳。

## 非目标

- 重查根因；改 I/T/R 单值规范化（`%d` / `%.3f` / `%s` / `(empty)` / `@` 等）。
- 改 MD5 或「每规范化值 + `\n`」官方 C 逐字节拼接口径。
- 入库 `.env` 或 `examples/` 未跟踪语料（含 `examples/demo2.slt`）。
- 改写官方大语料；冻结公共库 API 签名（属 Design）。
- 本阶段编写 Plan、Design、业务代码。
- **废止** `---separator …`；**禁止**裸 `separator`（无 `----`）主方案。
- **废止** `---- separator` 作为文件级、向后至 EOF 的顶层指令主路径（类 `hash-threshold`）。
- **已废止 R3**：行式期望无单引号语法壳；不处理 `''`；不提供 `splitLiteralRespectingQuotes` / `unquote`（或等价）合同能力。

## 范围与可见行为

| 形态 | 含义 | 典型来源 |
|---|---|---|
| **每值一行**（默认） | 期望头 `----` 后每行一个规范化单元格值 | 官方 C / 归档 normalize / 既有 fixtures |
| **行式**（新增） | 期望头后每行一整结果行；列以本条 `S` 分隔；token 为裸文本 | sqllogictest-rs；显式 `|` 见目标书写 |

- 多列可用行式通过；「每值一行」与官方语料不因本项系统性失败。
- 期望先行再展开，复用 `nosort`/`rowsort`/`valuesort`；哈希字节流不变；`[Diff]` 值行粒度。
- 单列无特例。实际侧：规范化 → `SortMode` 值序列（归档 normalize）。细则见合同。
- 单元格含当前 `S`：换分隔符或每值一行；**不得**依赖引号包裹。

## 合同

### API / 接口

- **比对入口**：类型签名、排序模式、hash-threshold、本条 `S`、期望文本（期望头之后）、实际原始行列 → 通过/失败及 Diff。
- **列分隔符 / 期望头（必须）**：扩展恰四个短横的 `----`；写在该 query 期望块头上（例 `---- separator |`）。语法见「`----` 行解析」。关键字 **`separator`**（`seperator` 不生效或明确报错）。**废止** `---separator …`。**禁止**裸 `separator <delim>`（IC-1 作废）。
- **R1**：`---- separator <delim>` 既是本条期望正文开始边界，又只为本条设 `S`。下一条仅用普通 `----` → 默认 `S`=U+0020。**禁止**文件级向后至 EOF 的顶层指令主路径。
- **解析（用户可见）**：`parseQuery` 读 SQL 后：下一行 `---- separator …` → 开期望块 + 该 delim；恰 `----` → 期望块 + 默认空格。独立顶层 `SeparatorRecord`：移除或非法（Design 选最小破坏）；用户可见以「期望头上、仅本条」为准。fixtures 不得把指令放文件顶当全局指令。
- 不冻结对外库 API；不依赖 JDBC；语义在 normalize + parser/runner 的本条分隔符。
- **禁止**将引号感知 split / unquote 作为行式期望合同能力（**已废止 R3**）。

### 数据 / 状态

| 项 | 合同 |
|---|---|
| 期望正文 | 期望头之后：每值一行、行式或多列哈希单行；同文件按条自动推断，允许混用 |
| 类型签名 | `I`/`T`/`R`；列数 `C` = 签名长度；目标书写示例为 `IIT` |
| 默认分隔符 | 单个 U+0020；本条无 `separator` 子句时行式按此分列 |
| 作用域（R1） | **单条 record**；废止「向后至 EOF / 类 hash-threshold」及文件级顶层指令主路径 |
| `----` 行族 | 以 `----` 开头按「`----` 行解析」；语义为**期望头**（非顶层文件指令） |
| 空 delim | **非法** → 可读解析失败 |
| 多字符 delim | **允许** |
| 空格作 delim 字面量 | **允许**（关键字后可选一个前导空白，再接字面量；可恢复 U+0020） |
| 行式推断 | 非哈希整段期望：用本条 `S` 拆每物理行；**若每行 token 数均 = `C`** → 整段行式并展开；否则 → 每值一行。同段部分行 =`C`、部分否且每值一行也无法对齐 → **失败**（须可读：行号/token 数/`C` 等） |
| split 规则 | 按字面量 `S` 切分，**不压缩**连续分隔符；连续 `S` 产生空 token（默认空格亦然；`S=\|` 时 `1\|\|3` → `["1","","3"]`）。**无**引号感知切分 |
| 显式 trim（R2） | 本条期望头带 `separator` 时：split 后对每个 token **trim 两侧空白**（故 `1 \| 1 \| hello world` 在 `S=\|` 合法，第三 token 为 `hello world`）。仅默认 `----`：保持既有默认空格行式，**不**因本决议破坏连续空格→空 token |
| **已废止 R3（引号）** | 行式期望无单引号语法壳；不处理 `''`；不对 token 去外层引号。显式路径：单元格 = trim 后 token **原文**；默认空格路径：既有硬切规则。期望侧写 `'hello world'` 时，`'` 是单元格内容的一部分（仍走既有 I/T/R 规范化），**不是**语法壳 |
| 空 token / `(empty)` | 空 token = 空串单元格，T 规范化后与 `(empty)` 对齐；行式亦可写 token `(empty)` |
| 单列 | `C=1` 常等价；**无特例** |
| 单元格含当前 `S` | 换不含于单元格的分隔符后写行式，或改用每值一行；否则接受推断/比对失败。**禁止**依赖引号包裹绕过 |
| 实际 / 排序 | 规范化 → `SortMode` 值序列；期望**先行再展开**，复用三种排序 |
| 哈希 | `N values hashing to <md5>`；`N`=规范化值个数；MD5=各值+`\n` 的 UTF-8 摘要。**禁止**改算法/拼接口径；行式/trim 只影响期望展开 |
| Diff | 值行粒度；保留 `[Diff] (-expected|+actual)` |

#### `----` 行解析

1. 行以 `----` 开头（通常在 query SQL 之后，作为期望头）。
2. 整行 trim 后恰等于 `----` → 期望块分隔符，本条 `S` = U+0020。
3. `----` 之后为：可选空白 + `separator` +（可选一个前导空白）+ `<delim>` 至行尾 → 本条期望头且本条 `S=<delim>`；空 `<delim>` → 解析失败。须覆盖 `---- separator |`。
4. 其他以 `----` 开头但不匹配上述两种 → **解析失败**（可读），避免静默当 SQL/期望正文。
5. 关键字拼写 `separator`；`seperator` 不生效或明确报错。
6. 独立顶层（非某条 query 期望头）的 `---- separator …`：用户可见主路径上移除或非法（Design 选）；不得再呈现为文件级作用域指令。

### 错误与约束

- **必须**：官方「每值一行」继续可用；哈希与官方 C 逐字节兼容；本条期望头可配置 `---- separator`；R2 显式 trim；空 delim、非法 `----…` 可读失败。
- **禁止**：入库 `examples/demo2.slt`、`.env`；静默猜测推断/非法指令；`---separator`；裸 `separator` 主方案；文件级 separator 主路径；行式期望单引号/`''` 语法壳及 `splitLiteralRespectingQuotes` / `unquote` 合同能力。
- 行式覆盖：在 `src/test/resources/fixtures/` **新增或修订**最小 fixture（含无引号目标书写与单条作用域）；不得依赖 `demo2.slt`。清理/更新曾依赖引号壳的验收、fixtures、单测与 README。

## 验收（Given-When-Then）

前置：固定样例或受控 fixture；哈希可摘自既有样例。

### P0

- **P0-1 默认空格行式不回归**：Given `query III`、实际 `(1,2,3)`、期望头恰 `----`、期望 `1 2 3`（无 separator）→ When 比对 → Then 通过（行式展开三值）；连续空格仍产生空 token（不因 R2 改变）。
- **P0-2 用户目标书写（IIT + `\|`，无引号壳）**：Given

```text
query IIT nosort
SELECT 1, 1, 'hello world'
---- separator |
1 | 1 | hello world
```

  与实际三列 `(1, 1, "hello world")`（或等价 JDBC 结果）→ When 解析并比对 → Then 通过：本条 `S=\|`、显式 trim 后三值对齐 `IIT`（第三单元格裸 `hello world`，无去引号步骤）。
- **P0-3 单条作用域**：Given 连续两条 query：第一条期望头 `---- separator |` 且行式用 `|`；第二条期望头恰 `----` 且行式用默认空格（如 `1 2 3`）→ When 解析并比对 → Then 两条均通过；第二条**不得**继承第一条的 `|`。
- **P0-4 显式 trim**：Given 本条 `---- separator |`、期望行 `1 | 2 | 3`（分隔符两侧有空白）→ When 比对实际 `(1,2,3)` → Then 通过（trim 后 token 为 `1`/`2`/`3`）。
- **P0-5 含 `S` 须换分隔符或每值一行**：Given 单元格文本含当前 `S`（例：`S=\|` 且值含 `|`）→ When 仍用行式且不换分隔符 → Then 推断或比对失败（可读）；Given 改用不含于单元格的 `S`，或改写为每值一行 → When 比对 → Then 可通过。**不**接受引号包裹作为合同解法。
- **P0-6 每值一行不回归**：Given 受控 fixture（如 `query-normalize-smoke.test`）及/或 `select*.test` 同风格样例 → When 比对/端到端（范围 Plan 定）→ Then 归档 normalize 通过，无本项系统性失败。
- **P0-7 哈希口径不变**：Given hash-threshold 走 `N values hashing to <md5>`（含行式展开路径）→ When 算 `N`/MD5 → Then 与官方 C/归档一致。
- **P0-8 排序可区分**：Given 多行多列与对应期望 → When `rowsort`/`nosort`（及按需 `valuesort`）→ Then 符合归档；先行再展开；失败可出 Diff。
- **P0-9 受控 fixture**：Given `src/test/resources/fixtures/` 最小行式 fixture（至少：默认空格多列；`---- separator |` + **无引号**目标书写；单条作用域两条 query）→ When 按 Plan 执行 → Then 通过；不依赖/不提交 `demo2.slt` 或 `.env`；不得把 separator 放文件顶当全局指令；**不得**再依赖单引号壳 fixture。

### P1

- **P1-1 边界**：空 delim；非法 `----…`；单元格含 `S`；空 token/`(empty)`；连续空格（仅默认 `----`）；单列；`seperator`；独立顶层 separator（若保留为非法）→ 空 delim/非法行/错误拼写/非法顶层可读失败或不生效；含 `S` 须换分隔符或每值一行；其余与合同一致；无静默猜测；**无**「未闭合引号」合同分支。
- **P1-2 Diff**：失败块含 `[Diff] (-expected|+actual)`；值行粒度；非整段 `reason=`。
- **P1-3 混用**：同文件行式与每值一行、以及有/无本条 `separator` 的相邻 query，各按本条推断/`S`/trim 正确通过或可读失败（无引号壳语义）。
- **P1-4 期望侧字面引号非语法**：Given 显式 separator、期望 token 写为 `'hello world'`（含两侧 `'` 字符）→ When 展开并比对实际裸 `hello world` → Then **失败**或按原文比对（引号计入单元格）；**不得**因「去引号」而通过。

## 已决议

| 编号 | 决议 |
|---|---|
| OQ-1 | 自动推断行式；同文件允许与每值一行混用 |
| OQ-2 | 必须可配置列分隔符；关键字 `separator`；**废止** `---separator`；**改用** `---- separator <delim>`；语法见「`----` 行解析」；默认单空格；空 delim 非法；多字符与空格字面量允许；推断/空 token/`(empty)`/连续空格/单列见合同。**IC-1 作废**。**作用域以 R1 为准**（废止原「向后至 EOF / 类 hash-threshold」） |
| **R1**（保留） | 作用域 = 单条 record。`---- separator <delim>` 写在该 query 期望块头上：既是期望正文开始边界，又只为本条设 `S`。废止文件级顶层指令主路径。下一条仅用普通 `----` → 默认 `S`=U+0020。`parseQuery`：SQL 后 `---- separator …` → 开期望块+该 delim；恰 `----` → 期望块+默认空格。独立顶层 `SeparatorRecord`：移除或非法（Design 选） |
| **R2**（保留） | 显式 separator 时：split 后对各 token trim 两侧空白。仅默认 `----`：保持既有默认空格行式（连续空格→空 token） |
| **R3**（**已废止**） | ~~含空格须 `'…'`；`''`；split → trim → 去引号~~。**废止**：行式期望不再有单引号语法壳；不再处理 `''`；不再 `splitLiteralRespectingQuotes` / `unquote`。含空格：用显式 `S`（如 `\|`）+ trim 后裸文本，或每值一行。单元格含当前 `S` → 换分隔符或每值一行 |
| OQ-3 | 期望先行再展开，复用三种 `SortMode` |
| OQ-4 | MD5/拼接硬约束不变；行式/trim 只影响期望展开（不再含「引号展开」） |
| OQ-5 | 新增兼容；默认仍每值一行；不破坏官方语料 |
| OQ-6 | `[Diff]` 值行粒度（cli-report 一致） |
| OQ-7 | `fixtures/` 新增/修订最小行式 fixture；禁止提交 `demo2.slt` |

## 开放问题

无未决开放问题。原 IC-1 **作废**；原 OQ-2 文件级「类 hash-threshold」作用域 **已废止**，以 **R1** 为准；**R3 已废止**。独立顶层 `SeparatorRecord` 移除 vs 非法属 Design 选型，不影响用户可见合同。
