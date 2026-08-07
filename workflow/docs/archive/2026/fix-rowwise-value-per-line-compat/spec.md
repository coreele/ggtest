# Spec: fix-rowwise-value-per-line-compat

> 需求与规格（Plan 之前完成）。任务拆解见后续同目录 `plan.md`。
>
> **feature-id**：`fix-rowwise-value-per-line-compat` · **sub-feature-id**：`fix-rowwise-value-per-line-compat`（未拆分）
> **适用对象**：Planner、Developer、QA、Manager
> **前置条件**：工作项 [`workflow/workflow/docs/manager/fix-rowwise-value-per-line-compat.md`](../../manager/fix-rowwise-value-per-line-compat.md)；只读参考归档 [`workflow/workflow/docs/archive/2026/ggtest-rowwise-expected/spec.md`](../../archive/2026/ggtest-rowwise-expected/spec.md)（本 Spec **有意取代**其合同）。
> **阅读顺序**：背景与目标 → 非目标 → 范围与可见行为 → 合同 → 验收 → 已决议 / 开放问题。
> **预期结果**：纯 `----` 永远每值一行；行式仅经 query 头 `separator <delim>`；移除行式推断与 `---- separator` 期望头；select4 位点通过。
> **失败处理**：偏离本 Spec 时修回；不得再引入空格猜行式或 `mixed expected line shapes`。
>
> **确认状态（standard）**：合同由用户 2026-07-26 逐条拍板；Spec 用户确认 **not-required**。唯一开放问题 **OQ-1** 随 Plan 确认一并拍板。

## 背景与目标

- **原缺陷**：`examples/select4.test` ~47398（`query ITII` / `rowsort`）为官方「每值一行」；TEXT `table tn7 row 92` 含空格，被行式空格推断误判为混形，抛 `mixed expected line shapes for 4 column(s)...`。
- **合同层根因**：归档 `ggtest-rowwise-expected` 在纯 `----` 下按「每行恰 C 个 token」猜行式。用户决定**不做**混形回退小修，而是**取代**该归档合同。
- **目标**：
  1. 纯 `----` **永远**每值一行；移除行式推断与 `mixed expected line shapes`。
  2. 行式改为 query 头显式 `separator <delim>`；声明后严格拆分并校验列数。
  3. 移除 `---- separator <delim>` 期望头（默认；见 OQ-1）。
  4. 哈希单行、sort / flatten、Diff 值行粒度不变。

## 非目标

- 不改 I / T / R 单值规范化（`%d` / `%.3f` / `%s` / `(empty)` / `@` 等）。
- 不改 MD5 或「每规范化值 + `\n`」官方 C 拼接口径。
- 不改 `nosort` / `rowsort` / `valuesort` 语义（期望仍先行展开再排序 / flatten）。
- 不入库用户本地 `examples/`（含 `select*.test`、`demo.slt`、`demo2.slt`），除非用户另说；dev-notes 可提示自行更新。
- 不编写 Plan、Design 或业务代码；不重新引入行式期望单引号 / `''` 语法壳。

## 范围与可见行为

| 形态 | 触发 | 含义 |
|---|---|---|
| **每值一行** | 恰 `----`，且 query 头**无** `separator <delim>` | 每物理行 = 一个单元格（含空格 TEXT 整行保留） |
| **行式** | query 头 `separator <delim>` + 恰 `----` | 每物理行 = 一整结果行；按 `delim` 拆成恰 `C` 个 token |
| **哈希** | 单行 `N values hashing to …` | 优先识别；有无 `separator` 均不走行式拆分 |

| 场景 | 旧（归档，取代） | 新（本 Spec） |
|---|---|---|
| `query III` + `----` + `1 2 3` | 行式三值 | 单值 `"1 2 3"`（对 3 值实际比对失败，预期） |
| `query ITII` + `----` + 含空格 TEXT | 可能混形抛错 | 每值一行通过 |
| 行式含空格单元格 | `---- separator \|` + 行式正文 | `query … separator \|` + 恰 `----` + 同行式正文 |
| `---- separator \|` | 合法期望头 | 可读解析错误（不得静默当正文）— OQ-1 |
| `query … separator`（行尾无 delim） | （旧：label 后多余 token 本非法） | `separator` 作 **label** |

目标书写：

```text
query IIT nosort separator |
SELECT 1, 1, 'hello world'
----
1 | 1 | hello world
```

## 合同

### API / 接口

**Query 头：**

```text
query <类型串> [nosort|rowsort|valuesort] [label] [separator <delim>]
```

- `<类型串>`：既有 `I`/`T`/`R`；`C` = 签名长度。
- 顺序：类型串 → 可选 sort → 可选 label → 可选 `separator <delim>`。
- **`separator` 消歧**（官方语料 8884 条 query 头零冲突）：
  1. `separator` 后恰一个 token → 分隔符声明，该 token 为 `delim`。
  2. `separator` 为行尾最后 token → **label**（向后兼容）。
  3. `delim` 须为空白切分产生的单 token（**不得**含空白）。
  4. `delim` 后再有 token → **可读解析错误**。
- 依据：旧文法 label 后多余 token 本就解析错误；新含义不改变任何原本合法文件行为。
- 关键字 `separator`；错误拼写不生效或明确报错。

**期望头：**

- **合法**：trim 后恰 `----`。
- **移除**：`---- separator <delim>`（及 `----` 后带 `separator` 的期望头）。遇之 → **可读解析错误**；**禁止**静默当期望正文或 SQL（OQ-1 默认）。
- 其他非法 `----…` → 可读解析失败。

**比对语义：** 类型签名、排序模式、本条 `separator`/`delim`（若有）、期望正文、实际行列 → 通过/失败及既有 Diff。不冻结对外库 API；语义在 parser + normalize。

### 数据 / 状态

| 项 | 合同 |
|---|---|
| 纯 `----`、无 query 头 `separator` | **永远**每值一行；**禁止**「每行恰 C token → 行式」猜测；**禁止** `mixed expected line shapes` |
| 声明 `separator <delim>` | 每物理行按 `delim` **字面量**拆分（**不**压缩连续分隔符）；必须恰 `C` 个 token，否则**可读失败**（行号、实际 token 数、`C`）；token 两侧 **trim**；空 token → `(empty)` |
| 展开后 | 复用既有 sort / flatten、哈希、Diff **值行**粒度 |
| 哈希 | `N values hashing to …` **优先**；有无 separator 皆然；口径不变 |
| 多字符 `delim` | **允许**（仍须单 token、无空白） |
| 单元格含当前 `delim` | 换分隔符或改每值一行；**禁止**依赖引号 |
| 单列 / 作用域 | `C=1` 无特例；`separator` 仅本条 `QueryRecord` |
| 取代归档 | **有意取代** `ggtest-rowwise-expected`；默认空格行式不再支持 |

### 错误与约束

| 情形 | 要求 |
|---|---|
| 行式 token 数 ≠ `C` | 可读失败：行号、实际 token 数、`C` |
| `separator <delim>` 后再有 token | 可读解析错误 |
| `---- separator …` | 可读解析错误；不得静默当正文（OQ-1 默认） |
| 非法 `----…` | 可读解析失败 |
| `src/main` | **禁止** `mixed expected line shapes` 文案/抛错路径 |

**必须**：含空格 TEXT 的官方每值一行可用；select4 位点通过。  
**禁止**：空格猜行式；混形抛错；静默非法期望头；入库用户本地 `examples/`（除非用户另说）。

## 验收（Given-When-Then）

### P0

- **P0-1 select4**：Given `examples/select4.test`（~47398 `query ITII rowsort`，含 `table tn7 row 92`）→ When `./bin/ggtest --engine sqlite --url 'jdbc:sqlite::memory:' examples/select4.test` → Then **0 失败**。
- **P0-2 显式 separator 行式**：Given

```text
query IIT nosort separator |
SELECT 1, 1, 'hello world'
----
1 | 1 | hello world
```

  与实际 `(1, 1, "hello world")` → When 解析并比对 → Then 通过。
- **P0-3 默认空格行式废止**：Given `query III`、恰 `----`、正文 `1 2 3`、实际 `(1,2,3)` → When 比对 → Then **失败**（期望单值 `"1 2 3"`）。
- **P0-4 含空格每值一行**：Given `query ITII`、恰 `----`、含空格 TEXT 的每值一行期望 → When 展开/比对 → Then 不抛 `mixed expected line shapes`。
- **P0-5 token ≠ C**：Given `separator |`、`C=3`、某行 token 数 ≠ 3 → When 展开/比对 → Then 可读失败（行号、实际 token 数、`C`）。
- **P0-6 separator 作 label**：Given `query III nosort separator`（无 delim）→ When 解析 → Then `separator` 为 label，非分隔符声明。
- **P0-7 移除 `---- separator`**：Given 期望头 `---- separator |` → When 解析 → Then 可读解析错误，不得当期望正文。（OQ-1 否决则改按开放问题替代行为。）
- **P0-8 哈希**：Given 单行 `N values hashing to <md5>`（有/无 `separator`）→ When 识别/比对 → Then 走哈希路径；`N`/MD5 口径不变。
- **P0-9 单测 + 无混形文案**：Given 实现完成 → When `mvn -q clean test` → Then 通过；且 `src/main` 无 `mixed expected line shapes`。
- **P0-10 fixtures / README**：Given `rowwise-default-space.test`、`rowwise-mixed.test`、`rowwise-pipe-separator.test` 及旧合同单测 → When 按新合同修订 → Then 与本 Spec 一致；`README.md` / `README.zh-CN.md`「Expected results / 期望结果」改新语法（query 头 `separator`；纯 `----` = 每值一行；不再宣传默认空格行式 / `---- separator`）。

### P1

- **P1-1** Given `query III separator | extra` → When 解析 → Then 可读解析错误。
- **P1-2** Given `separator |`、`1||3`（`C=3`）→ When 展开 → Then `["1","","3"]`，空 → `(empty)`。
- **P1-3** Given `separator |`、`1 | 2 | 3` → When 比对 `(1,2,3)` → Then 通过。
- **P1-4** Given 第一条 `separator |` 行式；第二条仅恰 `----` 每值一行 → When 比对 → Then 第二条不继承 `|`。
- **P1-5** Given 比对失败 → When 报告 → Then 值行粒度 `[Diff] (-expected|+actual)`。

## 已决议

| 编号 | 决议（用户 2026-07-26） |
|---|---|
| D-1 | 纯 `----` 永远每值一行；移除推断与 `mixed expected line shapes` |
| D-2 | 行式仅 query 头 `separator <delim>`；恰 C、trim、空 → `(empty)`；复用 sort/flatten/哈希/Diff |
| D-3 | 消歧：+一 token → 声明；行尾单独 → label；delim 单 token 无空白；其后多余 → 解析错误 |
| D-4 | 取代归档 `ggtest-rowwise-expected`；默认空格行式不再支持 |
| D-5 | 哈希单行优先，不受影响 |
| D-6 | 不入库本地 `examples/`；README 中英改新语法；fixtures/单测迁移 |

## 开放问题

| 编号 | 问题 | 默认 | 若否决 | 拍板时机 |
|---|---|---|---|---|
| **OQ-1** | 是否确认移除 `---- separator <delim>`？ | **移除**；遇之可读解析错误 | **保留兼容**：仍解析为期望头 + 本条 `delim`（类归档 R1），并保留 query 头 `separator` | 随 Plan 确认 |

无其他未决项。以本文件（含 OQ-1 默认）为准，直至 Plan 确认改写 OQ-1。

## 文档影响

| 类 | 说明 |
|---|---|
| 用户文档 | `README.md`、`README.zh-CN.md`「Expected results / 期望结果」→ query 头 `separator`；删除默认空格行式与 `---- separator` 示例（OQ-1 否决则保留后者） |
| 开发文档 | fixtures / 单测 / 注释随合同迁移（Plan 列任务） |
| 运维文档 | N/A |
| 归档 | 不改归档正文；本 Spec 声明取代即可 |
