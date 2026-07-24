# Spec: ggtest-core / cli-corpus

> 需求与规格（Plan 之前完成）。任务拆解见后续 `plan.md`（或同目录分阶段 Plan）。
>
> **feature-id**：`ggtest-core` · **sub-feature-id**：`cli-corpus`
> **适用对象**：Planner（Plan 输入）、Developer（实现依据）、QA（验收依据）、用户（确认需求范围）。
> **前置条件**：已阅读工作项记录 `docs/manager/ggtest-core.md` 与总览 [`spec.md`](./spec.md)；上游 parser / normalize / runner-sqlite 行为已按其 Spec 可依赖。
> **阅读顺序**：背景与目标 → 非目标 → 范围与可见行为 → 合同 → 验收 → 开放问题。
> **预期结果**：读者能够判定 CLI、报告、退出码与语料硬验收边界，并对开放问题作出决策。
>
> **确认要求（full 路径）**：本 Spec 含合同级范围（CLI、退出码、目录收集、零豁免硬验收）。须由**当前用户会话确认**后方可进入 Plan；确认前状态应为 `awaiting-spec-approval`。
>
> 总览：[spec.md](./spec.md)。上游：[spec-parser.md](./spec-parser.md)、[spec-normalize.md](./spec-normalize.md)、[spec-runner-sqlite.md](./spec-runner-sqlite.md)。

## 背景与目标

- GGTEST 面向用户的唯一入口为命令行工具，并须能跑通官方 sqllogictest 语料作为成熟度证明。
- **本切片目标**：交付 CLI **`ggtest`**、执行统计报告、退出码约定、目录递归收集 `*.test`/`*.slt`，以及对官方语料的**硬验收**（失败数 = 0、退出码 = 0；**零豁免**）。
- 语料由**用户自备**本地路径（已决议 Q4）。

## 非目标

- 解析器、规范化、Runner/SQLite 执行器的内部实现（属上游切片；本切片组装调用）。
- JUnit XML 等结构化报告；图形界面；CI/CD 插件；发布到 Maven Central。
- 仓库内提交官方大语料；语料生成或维护。
- 首期支持非 `sqlite` 的 `--engine` 值。

## 范围与可见行为

### CLI 入口

- 提供可执行命令行入口（命令名 `ggtest`）。
- 支持传入一个或多个测试文件路径，或目录（递归收集 `*.test` 与 `*.slt`，二者同等视为输入）。
- 单文件：不强制扩展名；内容合法即可执行。
- **扩展名不影响语义**：同一内容以 `.test`、`.slt` 或任意后缀呈现时，执行语义相同。

### 结果报告

- 执行结束后向标准输出打印统计：每个文件的通过/失败/跳过记录数，以及总计。
- 每条失败记录打印：文件、行号、记录摘要（SQL 首行）、失败原因（期望 vs 实际的差异摘要，或语句错误信息）。
- 报告为纯文本，无结构化格式要求。

### 官方语料硬验收

- 官方语料在 SQLite（JDBC）上须失败数 = 0、退出码 = 0（P0-1：`select1.test`；P1-5：select1/2/3）。
- **零豁免（Q8）**：不可消除的 JDBC 层偏差须再批豁免——**不可默示豁免**。

## 合同

### API / 接口

**CLI（面向用户的唯一入口，参数名为建议默认值，Planner 可微调但语义不变）：**

```text
ggtest --url <jdbc-url> [--user <user>] [--password <password>]
       [--engine <name>=sqlite] [--hash-threshold <N>]
       <file-or-dir> [<file-or-dir> ...]
```

- `--url`：JDBC 连接串（首期仅接受 SQLite，例如 `jdbc:sqlite::memory:` 或 `jdbc:sqlite:<path>`），必填。
- `--user` / `--password`：数据库凭据，可选（SQLite 通常不需要；可包含于 URL）。
- `--engine`：目标数据库标识，默认 `sqlite`，用于 skipif/onlyif；首期仅允许该值。
- `--hash-threshold`：覆盖初始哈希阈值；文件内 `hash-threshold` 指令仍可在执行中改变阈值。默认值为 **8**（Q5）。
- 位置参数：至少一个测试文件或目录。
  - **单文件**：不强制扩展名。
  - **目录**：递归收集 `*.test` 与 `*.slt`。
- 退出码：`0` = 全部通过；`1` = 存在失败记录；`2` = 用法/配置/解析/连接等错误。

### 数据 / 状态

- 测试文件路径由用户传入；官方大语料不入库。
- 跨文件：每个文件执行完后重置文件作用域状态（hash-threshold 回到 CLI 初值；条件与 label 映射清空），由本切片编排调用 runner 时保证。

### 错误与约束

- 解析错误：报告文件+行号+原因，该文件计为错误并以退出码 `2` 体现；其余文件继续执行。
- 结果不匹配 / 语句断言失败：退出码 `1`。
- 连接失败 / 执行中连接中断：退出码 `2`。
- 凭据不得写入日志或报告输出。
- 硬验收与零豁免见范围「官方语料硬验收」及已决议 Q7/Q8/Q9。
- 继承全局约束见「开放问题 / 已决议」，勿重开。

## 验收（Given-When-Then）

前置环境：本机可使用空白 SQLite（内存库 `jdbc:sqlite::memory:` 或空文件库均可）；已获取官方 sqllogictest 语料（至少含 `select1.test`、`select2.test`、`select3.test`）。

### P0

- **P0-1 官方语料跑通**：
  - Given 一个空白 SQLite（内存或文件）和官方 `select1.test`
  - When 运行 `ggtest --url <sqlite-jdbc-url> select1.test`
  - Then 全部记录执行完毕，输出通过/失败/跳过统计，失败数为 0，退出码为 0

### P1

- **P1-1 目录与多文件**：
  - Given 一个含 `.test` 与 `.slt` 的目录（可含子目录）
  - When 以该目录为参数执行
  - Then 递归收集并执行全部 `*.test` 与 `*.slt`，输出分文件与总计统计

- **P1-5 语料批量跑通**：
  - Given 官方语料 `select1.test`、`select2.test`、`select3.test`
  - When 批量执行
  - Then 输出分文件与总计统计，失败数为 0，退出码为 0

- **P1-6 显式 `.slt` 单文件**：
  - Given 一个内容合法的 `.slt` 文件
  - When 以该文件路径为位置参数执行
  - Then 正常解析并跑通，统计与退出码行为与同等内容的 `.test` 文件一致

## 开放问题

### 已决议（继承自总览，勿重开）

| 编号/议题 | 结论 |
|---|---|
| Q4 | 用户自备语料路径；仓库不提交官方大语料 |
| Q5 | hash-threshold 默认 **8** |
| Q7 | 官方语料硬验收：失败数 = 0、退出码 = 0；P0-1=`select1.test` |
| Q8 | **零豁免硬验收**；不可默示豁免 |
| Q9 | P1-5 硬验收范围 = select1/2/3.test |
| 输入后缀 | `.slt` 与 `.test` 等价 |
| Q1 / Q2 / Q3 | Java 17 / Maven / CLI 优先 |
| 产品名 | GGTEST |

### 待确认

请确认本 Spec 整体后进入 Plan。
