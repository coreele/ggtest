# Spec: feat-override-results

> 需求与规格（Plan 之前完成）。任务拆解见后续同目录 `plan.md`。
>
> **feature-id**：`feat-override-results` · **sub-feature-id**：`feat-override-results`（未拆分）
> **适用对象**：Planner、Developer、Reviewer、QA、Manager。
> **前置条件**：工作项 [`workflow/archive/2026/feat-override-results/feat-override-results.md`](../../manager/feat-override-results.md)；CLI/报告/退出码基线 [`workflow/archive/2026/feat-cli-halt/spec.md`](../../archive/2026/feat-cli-halt/spec.md)；比对/失败来源 `runner/SqlLogicTestRunner.java`、`normalize/ResultComparer.java`；解析与记录结构 `parser/SqlLogicTestParser.java`、`model/QueryRecord.java`、`model/StatementRecord.java`、`model/SourceLocation.java`。
> **阅读顺序**：背景与目标 → 非目标 → 调研发现 → 范围与可见行为 → 合同 → 错误与约束 → 验收 → 开放问题。
> **预期结果**：`--override`（golden-update）合同冻结：in-scope mismatch 用实际输出重写源 `.slt` 的 expected 区间，记录计 overridden 而非 FAILED；可进入 Design/Plan。
> **失败处理**：偏离本 Spec 须先修订 Spec；不得静默改变未启用 `--override` 时的失败/退出码语义，不得损坏源文件。
>
> **Spec 状态**：已确认（2026-08-10，Spec 用户确认 `approved`）。

## 背景与目标

### 现状

- ggtest 的期望结果**内嵌**在 `.slt` 文件中（`query` 记录的 `----` 块、`statement error <msg>` 行内消息）。
- 实际输出与期望不一致时，runner 产出 `RecordOutcome.FAILED` 并附 git 风格 diff；**无任何重写机制**。
- 引擎升级、SQL 行为变更、规范化规则调整后，批量更新期望结果只能**手工编辑**每条记录，易错且低效。
- CLI 选项解析为精确长选项、无前缀匹配（`--halt` 等先例）；退出码 `0`/`1`/`2` 已冻结。

### 目标

新增布尔开关 **`--override`**（golden-update 模式）：对**范围内的** mismatch，用**实际输出重写源 `.slt` 文件中该记录的 expected 区间**，该记录计为 **overridden（非 FAILED）**；范围外的 mismatch 仍按现状 FAILED；无 mismatch 的文件不被改动。

典型用途：引擎/规范化升级后一次性接受新输出、批量校正过期 goldens。

## 非目标

- **不自动提交 git**：仅改写工作区文件；版本控制由用户决定。
- **不翻转 statement 极性**：`statement ok`↔`statement error` 语义不变。
- **不自动修正 label 冲突**：label 冲突是跨记录一致性约束，无单一 golden（见「调研发现」）。
- **不为 execute-only query 自动生成 expected 块**：无 `----` 的 query 不参与比较，override 不为其插入期望。
- **不覆盖非 golden 的失败**：query 执行失败、类型签名不匹配、`statement ok` 失败等不属于「期望值失配」（见「调研发现」）。
- **不改 parser/比较/规范化语义**：override 复用既有实际输出（actualView）；仅新增「判定 + 写回」路径。
- **不引入结构化报告 / 独立 `--help` 全文**；报告以 README 为用户可见合同面。

## 调研发现（影响 Plan/Design，仅供背景，不规定实现）

1. **mismatch 来源与是否有「可写回的 golden 值」**（见 `SqlLogicTestRunner.runQuery`/`runStatement`）：

   | 来源 | 触发条件 | 是否有 golden 值 | 本 Spec 取舍 |
   | --- | --- | --- | --- |
   | query result mismatch | `hasExpectedResults && !comparison.passed()` | 有：`ResultComparer.CompareResult.actualView()`（含 hash 形式） | **覆盖（核心 P0）** |
   | statement error 消息失配 | `statement error <msg>` 且 `expectedErrorMsg` 子串失配 | 有：实际 error summary | **覆盖（P1）** |
   | label conflict | 同 label 多次产出不同 result view | 无（跨记录约束，无单一正确值） | 不覆盖 |
   | query execution failed | `executeQuery` 失败 | 无（无可写结果行） | 不覆盖 |
   | 类型签名不匹配 | actual 不符合声明 type signature | 无（签名错误，非值错误） | 不覆盖 |
   | statement ok 失败 / statement error 极性错 | 极性不符 | 无（极性翻转非 golden 重写） | 不覆盖 |

2. **源区间定位**：`SourceLocation` 仅含 `(sourceName, startLine)`；`QueryRecord` 保留 `expectedResults` 原文行，但**不保留** `----` 头与 expected 块的行范围/字符区间。因此「重写」必须能精确定位每条记录的 expected 区间（query：`----` 头之后到块末尾的行集合；statement error：header 行内 `error` 之后的消息片段）。该区间在 parser 逐行解析时是可知的；具体「由 parser 携带区间」还是「写回前重新定位」归 Design。

3. **golden 文本透传**：query 的 `actualView` 目前仅在 `runQuery` 内部可得，未出现在 `RecordResult`/`FileRunResult`。写回路径需要得到每条 mismatch 记录的 golden 文本（query 行集合 / statement error summary）；透传方式归 Design。

4. **写回接入点**：`FileRunner.run(parser, file, display)` 持有文件 `Path` 与 `FileRunResult`，是文件级「运行后写回」的自然接入点；`CliSession` 负责状态行/TOTAL/退出码。

5. **既有 `--halt` 先例**：`--override` 作为无值布尔开关，解析路径与 `--halt` 完全一致（`CliArgumentParser` → `ParsedArguments` → `RuntimeConfigResolver` → `CliOptions`）。

## 范围与可见行为

### 行为表

| 场景 | `--override` 关闭（默认） | `--override` 开启 |
| --- | --- | --- |
| query result mismatch | FAILED + diff；不改文件 | 用 actualView 重写该记录 expected 块（`----` 头保留）；计 **overridden**；非 FAILED |
| statement error 消息失配 | FAILED；不改文件 | 用实际 error summary 重写 header 内 `expectedErrorMsg`（极性仍 `error`）；计 **overridden**；非 FAILED（P1） |
| label conflict | FAILED | FAILED（**不覆盖**） |
| query execution failed | FAILED | FAILED（**不覆盖**） |
| 类型签名不匹配 | FAILED | FAILED（**不覆盖**） |
| `statement ok` 失败 / 极性错 | FAILED | FAILED（**不覆盖**） |
| execute-only query（无 `----`） | 不比较（现状） | 不比较；**不**插入 expected 块 |
| 文件无任何 in-scope mismatch | PASSED；不改文件 | PASSED；**不写文件**（保留内容与 mtime） |

### 记录判定

- in-scope mismatch 记录：判定为 **overridden**，**不得**以 `FAILED` 出现，**不得**抬高失败计数；在报告中以 **`[OVERRIDDEN]` tag** 展示（与 PASSED/FAILED 区分），并在 `TOTAL` 行追加 **`overridden=N`** 计数。是否新增 `RecordOutcome`/桶 枚举由 Design 决定。
- scope 外 mismatch 记录：仍 `FAILED`，计入失败计数。

### 文件级

- 文件含 ≥1 条 overridden 记录（且无剩余 FAILED、无硬错误）：文件计为 overridden，**写回一次**（含该文件全部 in-scope override）。
- 文件无 in-scope mismatch：**不写文件**（内容与 mtime 不变）。
- 文件含剩余 FAILED（scope 外）：仍**写回**已 override 的部分；文件仍计 FAILED（退出码 `1`）。

### 报告 / 退出码

- 报告：overridden 记录在状态行以 **`[OVERRIDDEN]` tag** 展示，与 PASSED/FAILED 区分；`TOTAL` 行追加 **`overridden=N`** 计数。
- 退出码语义**不变**（`0`/`1`/`2`）：曾有硬错误 → `2`；否则有剩余 FAILED → `1`；否则（含「仅 override，无剩余 FAILED」）→ `0`。
- v1 **不**提供 CI golden-drift 信号：不新增独立退出码、不新增 `--override-fail-on-change` 等选项；仅 override 且无剩余 FAILED → 退出码 `0`。

## 合同

### CLI 接口

| 项 | 合同 |
| --- | --- |
| 选项 | 精确 **`--override`**（无值布尔），风格与 `--halt` 一致 |
| 短形式/前缀 | **禁止** `-override`；**禁止** `--over` 等前缀 → usage，退出码 `2`，不连库 |
| 默认 | **关闭**；省略时行为完全同现状（所有 mismatch 仍 FAILED，不写文件） |
| 重复 | 多次 `--override` ≡ 一次（允许，非 usage 错误） |
| 组合 | 与 `--halt`/`--engine`/`--color`/`--hash-threshold` 等互不冲突；组合语义见「错误与约束」 |
| 库 API | N/A |
| 文档 | README 选项表须列 `--override`，简述 golden-update 语义（用实际输出重写源文件期望结果） |

### 文件改写合同

- **仅**改写发生 in-scope mismatch 的记录的 expected 区间；文件其余字节（注释、空白、statement、其它记录、header、SQL 正文、换行风格）**原样保留**。
- **query**：替换 `----` 头**之后**的 expected 结果行集合；`----` 头本身保留；新内容为实际输出的规范化视图（actualView，按当前 `hash-threshold` 自然产生明文或 `N values hashing to <md5>` 形式）。`----` 头与下一记录之间的空行分隔约定保持与现状一致。
- **statement error**：替换 header 行内 `error` 之后的 `expectedErrorMsg` 片段；极性 `error` 不变；SQL 正文与其余内容不动。
- **每文件至多一次写回**：同文件多条 in-scope override 在**同一次重写**中应用。
- **原子写（行为要求）**：写回须原子——中途失败不得产生半截或空文件；原文件在新内容完整落盘前保持完好（具体机制如 temp+rename 归 Design）。
- **无 in-scope override → 不打开写、不改 mtime**。
- **编码**：UTF-8；保留原文件既有换行风格（不强制改换行符）。

### 数据 / 状态

N/A。不引入持久状态；不写 STATUS、不提交 git。

## 错误与约束

- **只读 FS / 文件不可写**：该文件存在 in-scope mismatch 但写回失败 → 该文件计**硬错误**（退出码 `2`），报告须写明「写回失败」并列出本应 override 的记录；原文件**不得被损坏**。其它已处理文件按各自结果。
- **`--override` + `--halt`**（独立组合）：
  - in-scope mismatch 计 overridden（非 FAILED），**不**触发 `--halt` 停跑。
  - scope 外 FAILED 仍触发 `--halt`（文件内首错即停、跨文件停跑，沿用 `--halt` 合同）。
  - 停跑前已 override 的记录：该文件仍按合同写回这些 override。
- **致命中止**（`FatalDatabaseException` / `FileRunResult.aborted()`）：该文件**不写回**（信息不完整），保留原文件；此前已计算的 override **不落盘**。
- **多 mismatch 同文件**：同一次重写应用全部 in-scope override；scope 外 mismatch 保留为 FAILED（不阻塞同文件内 in-scope 部分的写回）。
- **开跑前/中硬错误**（parse/IO/连接/schema 隔离与 teardown）：该文件**不写回**；不影响「无 mismatch 文件不被改写」的语义。
- **凭据**：禁止将凭据写入报告或文件（沿用现状）。

## 验收（Given-When-Then）

### P0

- **P0-1 默认关闭**  
  Given 含一条 query result mismatch 的单文件，argv **无** `--override`  
  When 运行 ggtest  
  Then 该记录 FAILED + diff；文件**未被改写**；无硬错误时退出码 `1`。

- **P0-2 query mismatch 被重写**  
  Given 单文件含一条 query result mismatch（明文期望），argv 含 `--override`  
  When 运行 ggtest  
  Then 该记录 expected 块被 actualView 替换、`----` 头保留；该记录**非** FAILED；文件其余内容字节不变；退出码 `0`。

- **P0-3 重跑幂等**  
  Given P0-2 写回后的文件  
  When 以相同 argv（含或不含 `--override`）再跑  
  Then 全部 PASSED；文件**不再被改写**（无 mismatch）。

- **P0-4 无 mismatch 不写文件**  
  Given 单文件全部 PASSED，argv 含 `--override`  
  When 运行 ggtest  
  Then 文件内容与 mtime **均不变**；退出码 `0`。

- **P0-5 文件其余内容不变**  
  Given 多记录文件（含注释、空白、多条 statement/query），其中**仅一条** query result mismatch，argv 含 `--override`  
  When 运行 ggtest  
  Then 对比改写前后，**仅**该记录 expected 块的行发生变化；注释、空白、SQL、其它记录、header 行均逐字节一致。

- **P0-6 退出码优先级**  
  Given 两文件：A 仅含 in-scope mismatch，B 含一条 scope 外 FAILED；argv 含 `--override`  
  When 运行 ggtest  
  Then A 被写回且非 FAILED；B 仍 FAILED；退出码 `1`。

- **P0-7 选项解析**  
  Given 精确 `--override` → 开启 golden-update。  
  Given `-override` 或 `--over` → usage，退出码 `2`，不连库、不写文件。

### P1

- **P1-1 statement error 消息被重写**  
  Given `statement error <oldmsg>` 且实际错误消息与 `oldmsg` 子串失配，argv 含 `--override`  
  When 运行 ggtest  
  Then header 行的 `expectedErrorMsg` 被实际 error summary 替换；极性仍为 `error`；SQL 与其余内容不变；该记录非 FAILED。

- **P1-2 scope 外不覆盖**  
  Given 文件含 label conflict / query execution failed / 类型签名不匹配 / `statement ok` 失败之一，argv 含 `--override`  
  When 运行 ggtest  
  Then 该记录仍 FAILED；该记录对应的源文件内容**不被改写**（同文件若另有 in-scope mismatch，仅那些被写回）。

- **P1-3 execute-only 不生成 expected 块**  
  Given 无 `----` 的 execute-only query，argv 含 `--override`  
  When 运行 ggtest  
  Then 不比较、不插入任何 expected 块；文件不变。

- **P1-4 只读 FS**  
  Given 文件只读且存在 in-scope mismatch，argv 含 `--override`  
  When 运行 ggtest  
  Then 写回失败 → 该文件硬错误、退出码 `2`、报告含「写回失败」；原文件**未被损坏**（仍可被原样解析）。

- **P1-5 `--override` + `--halt`**  
  Given 单文件：先一条 in-scope query mismatch，后一条 scope 外 FAILED，其后仍有记录；argv 含 `--override --halt`  
  When 运行 ggtest  
  Then 第一条被 override 并写回；第二条 FAILED 触发 `--halt`（其后记录不执行、不以假 FAILED 出现）；退出码 `1`。

- **P1-6 致命中止不写文件**  
  Given 文件开跑后触发 `FatalDatabaseException`（aborted），argv 含 `--override`  
  When 运行 ggtest  
  Then 文件**不被改写**，保留原内容；报告硬错误；退出码 `2`。

- **P1-7 原子性（可观察）**  
  Given 模拟写回过程中发生失败  
  When 运行 ggtest  
  Then 失败后原文件仍完整可被原样解析（无半截/空文件残留）。

- **P1-8 文档**  
  Given README 选项表  
  When 查找 `--override`  
  Then 存在且简述 golden-update 语义。

## 开放问题

> 本节问题均已于 2026-08-10 经用户确认并并入上方合同；保留以备追溯，不再悬置。

1. **overridden 的报告展示**：**已确认（2026-08-10）**：overridden 记录在状态行以 `[OVERRIDDEN]` tag 展示，`TOTAL` 行追加 `overridden=N` 计数，与 PASSED/FAILED 区分；退出码语义不变（`0`/`1`/`2`）。
2. **CI golden-drift 信号**：**已确认（2026-08-10）**：v1 **不**提供 CI golden-drift 信号——不新增独立退出码、不新增 `--override-fail-on-change` 等选项；仅 override 且无剩余 FAILED → 退出码 `0`。
3. **statement error 消息 override 的 golden 粒度**：**已确认（2026-08-10）**：override 时写入**完整的实际 error summary**（与既有「大小写不敏感子串匹配」语义一致，保证重跑通过）。
