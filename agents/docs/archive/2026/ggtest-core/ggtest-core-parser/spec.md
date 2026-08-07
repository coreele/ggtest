# Spec: ggtest-core / parser

> 需求与规格（Plan 之前完成）。任务拆解见后续 `plan.md`（或同目录分阶段 Plan）。
>
> **feature-id**：`ggtest-core` · **sub-feature-id**：`parser`
> **适用对象**：Planner（Design/Plan 输入）、Developer（实现依据）、QA（验收依据）、用户（确认需求范围）。
> **前置条件**：已阅读工作项记录 `agents/docs/manager/ggtest-core.md` 与总览 [`spec.md`](../spec.md)；了解 sqllogictest 格式基本概念。
> **阅读顺序**：背景与目标 → 非目标 → 范围与可见行为 → 合同 → 验收 → 开放问题。
> **预期结果**：读者能够判定解析器行为边界，并对开放问题作出决策。
>
> **确认要求（full 路径）**：本 Spec 含合同级范围（记录类型、解析错误定位、扩展名语义）。须由**当前用户会话确认**后方可进入 Design/Plan；确认前状态应为 `awaiting-spec-approval`。
>
> 总览：[spec.md](../spec.md)。

## 背景与目标

- GGTEST 需将 sqllogictest 行式文本解析为可执行的记录模型，供 runner 消费。
- **本切片目标**：实现 parser——解析 `.test` / `.slt`（及不强制扩展名的单文件）UTF-8 输入，产出记录序列；解析失败时报告**文件名与行号**。
- **本切片不连库**；不执行 SQL、不做结果比对、不提供 CLI。

## 非目标

- 连接或执行任何数据库；结果规范化、哈希、排序比对（属 `normalize`）。
- Runner、skipif/onlyif 运行时求值、halt 执行语义（属 `runner-sqlite`；本切片仅解析这些指令为记录）。
- CLI、目录递归收集、统计报告、官方语料硬验收（属 `cli-corpus`）。
- 首期不支持的 sqllogictest-rs 扩展语法（变量替换、record/complete 等）——遇未知记录类型按解析错误处理。

## 范围与可见行为

- 输入为 sqllogictest 格式的 UTF-8 文本。约定扩展名 `.test` 与 `.slt`（二者等价）；**不强制扩展名**：显式传入的单文件路径无论后缀，只要内容为合法 sqllogictest 格式即可解析。
- 以 `#` 开头的行为注释；空行为记录分隔符。
- 支持的记录类型（首期全部解析）：

| 记录 | 语法 | 语义（解析产出） |
|---|---|---|
| statement ok | `statement ok` + 后续行 SQL | 语句成功断言记录 |
| statement error | `statement error` + 后续行 SQL | 语句失败断言记录（不携带错误消息匹配） |
| query | `query <类型签名> [<排序模式>] [<label>]` + SQL + 可选 `----` + 期望结果 | 查询记录；无 `----` 时为「只执行不比对」，解析须保留该标志 |
| skipif | `skipif <db-name>`（可叠加多行） | 条件指令记录 |
| onlyif | `onlyif <db-name>`（可叠加多行） | 条件指令记录 |
| hash-threshold | `hash-threshold <N>` | 阈值指令记录 |
| halt | `halt` | 中止指令记录 |

- 类型签名：由 `I`（整数）、`T`（文本）、`R`（浮点）组成的字符串，每个字符对应一个结果列。
- 排序模式声明：`nosort`（默认）、`rowsort`、`valuesort`（解析识别；比对语义属 normalize）。
- 解析失败（未知记录类型、类型签名非法、格式残缺等）必须报告**文件名和行号**。
- **扩展名不影响语义**：同一内容以 `.test`、`.slt` 或任意后缀呈现时，解析语义完全相同。

## 合同

### API / 接口

- **解析入口（合同级，签名细节属 Design）**：接受「文件路径或等价文本源」与可读内容，产出有序记录模型；失败时抛出/返回可定位的解析错误。
- CLI 退出码由 `cli-corpus` 消费；本切片须保证解析错误信息足以支撑退出码 `2` 的定位报告（文件+行号+原因）。

### 数据 / 状态

- **输入**：sqllogictest 行式 UTF-8 文本（扩展名语义见范围）。
- **输出（记录模型，合同级形状，字段细节属 Design）**：有序记录列表，每条至少可区分上述记录类型，并携带源文件定位（文件名、起始行号）；`query` 须包含类型签名、可选排序模式、可选 label、SQL、是否含期望结果及期望文本（或「仅执行」标志）；`statement` 须含 SQL 与 ok/error 极性；条件/阈值/halt 指令须保留操作数。
- 本切片无跨文件运行时状态。

### 错误与约束

- 解析错误：必须包含**文件名、行号、原因**；不得静默跳过非法输入。
- 不连库；不修改输入文件。
- 继承全局约束见「开放问题 / 已决议」，勿重开。

## 验收（Given-When-Then）

前置环境：可构造本地 UTF-8 测试文件；无需数据库。

### P0

- **P0-7 解析错误定位**：
  - Given 一个含未知记录类型行的文件
  - When 解析（或经 CLI 触发解析）
  - Then 输出包含该文件名与行号的解析错误信息；经 CLI 时退出码为 2

### P1

- **P1-a 记录类型与注释**：
  - Given 一个含注释、空行，以及 `statement ok`、`statement error`、`query`（含 `----`）、`skipif`、`onlyif`、`hash-threshold`、`halt` 的合法文件
  - When 解析
  - Then 产出对应类型的有序记录，注释与空行不作为可执行记录

- **P1-b 无分隔符的 query**：
  - Given 一条 `query` 记录仅有类型签名与 SQL、无 `----` 行
  - When 解析
  - Then 产出「只执行不比对」语义的 query 记录（不含期望结果值）

- **P1-c 扩展名无关**：
  - Given 同一合法内容分别保存为 `.test`、`.slt` 与无标准后缀的文件
  - When 分别解析
  - Then 三份产出的记录序列语义等价

## 开放问题

### 已决议（继承自总览，勿重开）

| 编号/议题 | 结论 |
|---|---|
| Q1 | Java 17 |
| Q2 | Maven |
| Q3 | CLI 优先（本切片不交付 CLI） |
| 产品名 | GGTEST |
| 输入后缀 | `.slt` 与 `.test` 等价；单文件不强制扩展名 |
| Q4 | 用户自备语料（本切片不跑语料） |

### 待确认

请确认本 Spec 整体后进入 Design/Plan。
