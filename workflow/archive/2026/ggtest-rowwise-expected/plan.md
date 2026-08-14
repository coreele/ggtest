# Plan: ggtest-rowwise-expected

> 实施与验证计划。需求依据见 [`spec.md`](./spec.md)；架构依据见 [`design.md`](./design.md)。
>
> **适用对象**：Developer（实施）、Reviewer（审阅）、QA（验收）。
> **前置条件**：Spec **approved**（2026-07-26 R1/R2/R3，**第四次修订已废止 R3**，保留 R1/R2）；Design 已按同修订回写；本 Plan 为合入前修订后的实施计划（**用户已确认废止 R3**，Plan 随本修订可直接供 Developer 实施；Manager 持久化确认后调度）；Java 17 + Maven；源分支 `ggtest-rowwise-expected`。
> **阅读顺序**：元信息 → 目标摘要 → 任务拆解 → 依赖与顺序 → 触碰路径 → 验证 → 验收 → 文档影响 → 交接。
> **预期结果**：Developer 可按 TDD 交付本条期望头 `---- separator`、显式 trim（**无引号层**）、移除文件级 separator、**删除引号实现**，并通过 P0-1…P0-9；QA 可独立复核。
> **失败处理**：验证失败按「验证」节证据定位；无法执行时按「无法执行验证时的处理」记录原因/风险/恢复条件。

## 元信息

- 工作项标识: ggtest-rowwise-expected（未拆分，sub-feature-id = feature-id）
- 依据 Spec: [workflow/archive/2026/ggtest-rowwise-expected/spec.md](./spec.md)
- 依据 Design: [workflow/archive/2026/ggtest-rowwise-expected/design.md](./design.md)
- 路径等级: full
- Review 门禁: required（进入 QA 前须 Reviewer `Approve`；合入前修订后须**重新** Approve）
- 最低验证层: L3（单元测试 + 构建 + 受控 fixture / runner 路径）
- 验证命令:
  - `mvn -q clean test`
  - `mvn -q -DskipTests package`（可选，确认打包无回归）

## 适用工程规范

- [文档工程](../../standards/documentation.md)
- [Git 协作](../../standards/git.md)
- [质量与验证](../../standards/quality.md)
- [安全](../../standards/security.md)

## 目标摘要

按 R1/R2（**已废止 R3**）与 Design D1–D4，将「文件级 `SeparatorRecord` + `FileState.S`」回改为**本条期望头**，显式路径**仅** trim（无引号层），并**删除已实现的引号能力**：parser 绑定 `QueryRecord.S`/`explicit` 且移除顶层类型；normalize 显式 split→trim→展开、默认空格旧规则，**删除** `splitLiteralRespectingQuotes` / `unquote` 及未闭合引号分支；runner 按本条传参；fixtures/README 用无引号裸文本目标书写并清理依赖引号的测试。验收 **P0-1…P0-9**；不改 I/T/R 与 MD5。**禁止** `---separator`、裸 `separator`、文件级 separator 主路径、行式期望单引号语法壳、`demo2.slt`/`.env`。

## 任务拆解

TDD：每任务先写失败测试，再最小实现至通过。行为以 Spec 为准；结构以 Design 为准。合入前已有实现须按本 Plan **回改**，不得保留文件级可见行为。

### T1 — parser/query 期望头 + 移除顶层 `SeparatorRecord`

- 路径：`QueryRecord` 增补字段；删除 `SeparatorRecord.java` 与 `SqlTestRecord` permits；`SqlLogicTestParser` + `SqlLogicTestParserTest`。
- 行为（D1/D2）：
  - `parseQuery`：SQL 后恰 `----` → 期望块 + 默认空格、`explicit=false`；`---- separator <delim>` → 期望块 + 本条 delim、`explicit=true`；空 delim / 非法 `----…` / `seperator` → 可读失败。
  - 顶层不再产出 `SeparatorRecord`；顶层 `---- separator …` → `ParseException`（可读：须为 query 期望头）。
  - delim **禁止** `splitTokens` 整取；**禁止** `---separator` / 裸 `separator`。
- 完成条件：期望头 `|` 绑定本条；下一条恰 `----` 解析为默认空格；顶层 separator 失败；既有非 separator parser 用例 Pass。

### T2 — normalize：显式 trim + 删除引号实现 + 比对入参

- 路径：`ExpectedResultExpander`、`ResultComparer`（± 测试）；**禁止**改 `ValueNormalizer`/`ResultHasher` 算法。
- 行为（D3/D4，**已废止 R3**）：`compare(..., S, explicit, ...)`；旧重载默认空格且非显式。显式：不压缩 split（`splitLiteral`）→ 对每 token trim 两侧空白 → 再按 `C` 推断/展开；trim 后 token **原文**即单元格。默认：既有空格行式（连续空格→空 token，不因 R2 trim）。哈希短路径不展开；Diff 值行粒度不变。
- **删除**：`splitLiteralRespectingQuotes`、`unquote` 及未闭合引号 `IllegalArgumentException` 分支；`tokenize` 显式分支改为 `splitLiteral` + `token.strip()`（不 `unquote`）；同步删除依赖引号/未闭合引号的单测（如 `'hello world'` 去引号、`'it''s fine'`、未闭合失败断言）。
- 完成条件（单测）：P0-1、P0-2（**无引号**目标书写展开）、P0-4 trim、P0-5 含 `S` 须换分隔符或每值一行、P0-7 哈希口径、P0-8 排序/Diff；P1-4 期望侧 `'hello world'` 计入单元格原文（**不因去引号通过**）；宜含空 token/`(empty)`/混用失败（P1）。

### T3 — runner：本条 `S`/`explicit` 接线

- 路径：`SqlLogicTestRunner`（移除 `FileState.columnSeparator` 与 `SeparatorRecord` 分支）；`SqlLogicTestRunnerTest` / 相关穷尽 switch。
- 行为：`runQuery` 从 `QueryRecord` 传 comparer；无文件级覆盖。
- 完成条件：原「文件级 separator 覆盖后续 query」类测试改为**单条作用域**断言（P0-3）；既有 smoke 不回归。

### T4 — 受控 fixtures（P0-9）

- 路径：修订/新增 `src/test/resources/fixtures/`（至少：`rowwise-default-space`；`rowwise-pipe-separator` **改为期望头** `---- separator |` + **无引号裸文本**目标书写（如 `1 | 1 | hello world`）；单条作用域两条 query；宜含 `rowwise-mixed`）；驱动测试（runner/CLI 既有模式）。
- **清理**：删除/改写任何依赖单引号语法壳的 fixture 行与断言（现 `rowwise-pipe-separator.test` 的 `1 | 1 | 'hello world'` → 裸 `hello world`），并同步实际侧结果为裸文本单元格。
- 禁止：文件顶全局 `---- separator`；行式期望单引号语法壳；`examples/demo2.slt`、`.env`；已废止 `---separator`。
- 完成条件：P0-2/P0-3/P0-9 端到端 Pass；`mvn -q clean test` 全绿。

### T5 — 文档与实施记录

- 路径：`README.md`（本条期望头语法/作用域/显式 trim；示例用**无引号**目标书写；**无**文件级作用域、`---separator`、**无**单引号语法壳说明；若旧文含「含空格须加引号」须删除，改为「用显式 `S` + trim 裸文本或每值一行」）；`dev-notes.md`（L3 证据、相对旧实现的回改与**删引号**说明、合同偏差）；按需 Javadoc。
- 完成条件：README 可独立说明 R1/R2（**已废止 R3**）书写约定，且无残留引号壳描述；`dev-notes.md` 可追溯 P0-1…P0-9。

## 依赖与顺序

```text
T1 (parser/model 期望头 + 移除 SeparatorRecord)
 → T2 (normalize 删引号 + 显式 trim/展开)
 → T3 (runner 本条接线)
 → T4 (fixtures 期望头形式)
 → T5 (README / dev-notes)
```

- 各任务内部 TDD。
- T2 可不依赖真实 DB；T3/T4 需要时复用内存 SQLite runner/CLI。
- Git：在源分支 `ggtest-rowwise-expected` 上实施（已存在则继续；仅在 Manager 调度 Developer 且本修订 Plan 已批准后）。

## 触碰路径

| 路径 | 动作 |
|---|---|
| `src/main/java/com/ggtest/model/QueryRecord.java` | 修改（增 `columnSeparator` / `explicitColumnSeparator`） |
| `src/main/java/com/ggtest/model/SeparatorRecord.java` | **删除** |
| `src/main/java/com/ggtest/model/SqlTestRecord.java` | 修改 permits |
| `src/main/java/com/ggtest/parser/SqlLogicTestParser.java` | 修改 |
| `src/main/java/com/ggtest/normalize/ExpectedResultExpander.java` | 修改 |
| `src/main/java/com/ggtest/normalize/ResultComparer.java` | 修改 |
| `src/main/java/com/ggtest/normalize/ResultSorter.java` | 按需最小修改 |
| `src/main/java/com/ggtest/runner/SqlLogicTestRunner.java` | 修改 |
| `src/test/java/com/ggtest/parser/**`、`normalize/**`、`runner/**` | 修改 |
| `src/test/resources/fixtures/runner/rowwise-*.test` 等 | 修订/新增（期望头形式） |
| `README.md` | 更新 |
| `workflow/archive/2026/ggtest-rowwise-expected/dev-notes.md` | Developer 更新 |

**禁止触碰**：`workflow/docs/manager/*`、`STATUS.md`、本切片 `spec.md`（合同已冻）、`.env`、`examples/demo2.slt`；不得改 MD5 拼接算法语义。

## 验证

- **最低验证层**：L3。理由：变更跨 parser → model → normalize → runner；须用受控 fixture 证明本条期望头、单条作用域与显式 trim（无引号层）端到端通过，删引号后无残留语义，并回归每值一行 smoke。
- **验证命令**：
  1. `mvn -q clean test` — 全量 Surefire。
  2. （建议）`mvn -q -DskipTests package` — shaded jar 可构建。
- **预期证据**：
  - `BUILD SUCCESS`；Failures=0、Errors=0。
  - 测试名或 `dev-notes` 可追溯 P0-1…P0-9。
  - fixtures 无文件顶全局 separator；无对 `demo2.slt` / `.env` 依赖；产物中无 `---separator`。
- **无法执行验证时的处理**：缺 JDK 17/Maven → 记入 `dev-notes.md`；风险：无法确认行式/回归；恢复：安装后重跑上述命令。不得以缺少官方大语料或未跟踪 examples 为由跳过 P0。

## 验收

对齐 [`spec.md`](./spec.md)（合入前修订后编号）：

| ID | 要求摘要 | 验证策略 | 预期证据 |
|---|---|---|---|
| P0-1 | 默认空格行式 `1 2 3`；连续空格仍空 token | T2 ± T4 | Pass |
| P0-2 | 目标书写 `IIT` + `---- separator \|` + **无引号**裸文本（`1 \| 1 \| hello world`） | T1–T4 | Pass |
| P0-3 | 单条作用域：下一条恰 `----` 不继承 `\|` | T1+T3+T4 | 两条均通过 |
| P0-4 | 显式 trim（`1 \| 2 \| 3`） | T2 ± T4 | Pass |
| P0-5 | 单元格含当前 `S`：换分隔符或每值一行；**不**接受引号包裹 | T2 | 推断/比对失败可读；换 `S`/每值一行可通过 |
| P0-6 | 每值一行不回归 | 既有 smoke / normalize | 无系统性失败 |
| P0-7 | 哈希 `N`/MD5 口径不变 | T2 ± fixture | 与归档一致 |
| P0-8 | rowsort/nosort（及按需 valuesort）；失败 Diff | T2 | 符合归档 |
| P0-9 | fixtures 期望头形式；无文件顶全局指令 | T4 | 受控路径 Pass；不依赖 demo2 |

P1-1…P1-4：优先在 T1/T2/T4 覆盖（含非法顶层 separator；P1-4 期望侧 `'hello world'` 计入单元格原文，**不因去引号通过**；**无**「未闭合引号」合同分支）；未全覆盖项须在 `dev-notes.md` 声明缺口供 QA。

## 文档影响

| 类别 | 更新路径或 N/A 理由 |
|---|---|
| 开发文档 | `README.md`（期望头/`separator`/trim；**删**引号壳说明）；`QueryRecord` / `compare` Javadoc；`dev-notes.md` |
| 用户文档 | `README.md` 同上（CLI 用户可见书写约定；无引号壳）；无独立用户手册 |
| 运维文档 | N/A：无部署/监控/运维面变更 |

可选（非阻塞）：若架构总览仍列举文件级 `SeparatorRecord`，可在同 PR 增量改 `workflow/archive/2026/architecture-overview/design.md`——非本 Plan 必做。

## Review 门禁与进入 QA

- Review 门禁：**required**（full）；合入前合同修订后须**重新**取得 Reviewer `Approve`。
- Developer 完成 T1–T5 且验证通过 → Manager → **Reviewer**（测试有效性、文档影响、安全影响）→ **`Approve`** 后方可进入 QA。
- QA：独立按 Spec P0-1…P0-9（及已声明的 P1）与本 Plan 验证层验收 → `qa-report.md`。
- 本轮完成后仍停合并授权；**不合入 main**，除非用户另行授权。

## 安全影响

- 仅解析/比对本地语料文本；不新增网络或凭据处理。
- 禁止将 `.env` 或含密钥样例入库。Reviewer 按 [安全规范](../../standards/security.md) 确认。

## 交接顺序

1. 本修订 Plan 经用户决议批准路径 → Manager 持久化确认 → 状态 `planned`（Planner **不**自行改状态）。
2. Developer：在源分支 `ggtest-rowwise-expected` 执行 T1–T5 + 更新 `dev-notes.md`。
3. Reviewer：重新 `Approve`（进入 QA 前置）。
4. QA：独立验收 → `qa-report.md`；仍停合并授权。

## 修订记录

| 日期 | 摘要 |
|---|---|
| 2026-07-25 | 初稿：D1–D3；T1–T5；L3；当时 P0-1…P0-6；分支 `ggtest-rowwise-expected` |
| 2026-07-25 | OQ-2 书写二次修订：废止 `---separator`；改用 `---- separator`（当时仍为文件级作用域） |
| 2026-07-26 | 合入前 R1/R2/R3：期望头本条作用域；移除顶层 `SeparatorRecord`；显式 trim/引号；fixtures 改期望头；验收对齐 P0-1…P0-9 |
| 2026-07-26 | 第四次修订**废止 R3**（无引号层）：T2 删 `splitLiteralRespectingQuotes`/`unquote`/未闭合分支，显式仅 trim；T4/T5 清理依赖引号的 fixtures/测试/README；P0-2 改无引号目标书写、P0-5 改含 `S` 处理；用户已确认，Plan 随修订可直接供 Developer |
