# Spec: ggtest-core / runner-sqlite

> 需求与规格（Plan 之前完成）。任务拆解见后续 `plan.md`（或同目录分阶段 Plan）。
>
> **feature-id**：`ggtest-core` · **sub-feature-id**：`runner-sqlite`
> **适用对象**：Planner（Design/Plan 输入）、Developer（实现依据）、QA（验收依据）、用户（确认需求范围）。
> **前置条件**：已阅读工作项记录 `workflow/archive/2026/ggtest-core/ggtest-core.md` 与总览 [`spec.md`](../spec.md)；了解 parser 记录模型与 normalize 比对合同。
> **阅读顺序**：背景与目标 → 非目标 → 范围与可见行为 → 合同 → 验收 → 开放问题。
> **预期结果**：读者能够判定 Runner 与 SQLite 执行器行为边界，并对开放问题作出决策。
>
> **确认要求（full 路径）**：本 Spec 含合同级范围（执行器抽象、SQLite JDBC、条件控制、statement/query）。须由**当前用户会话确认**后方可进入 Design/Plan；确认前状态应为 `awaiting-spec-approval`。
>
> 总览：[spec.md](../spec.md)。上游：[parser/spec.md](../ggtest-core-parser/spec.md)、[normalize/spec.md](../ggtest-core-normalize/spec.md)。

## 背景与目标

- GGTEST 需按文件内顺序执行已解析记录，对目标库跑 SQL 并比对结果。
- **本切片目标**：实现 Runner + **数据库执行器抽象** + 首期 **SQLite JDBC** 适配；串行单连接执行；支持 skipif/onlyif、halt→skipped、label 一致性、statement ok/error、query 比对（依赖 normalize）。
- **多库扩展点在本切片成型**（见范围「可扩展性」）。

## 非目标

- 其他数据库的生产级实现（PostgreSQL 等）；首期仅交付 SQLite（JDBC）。
- `statement error` 的错误消息/正则匹配（首期只断言「执行失败」这一事实）。
- CLI、目录收集、统计文本格式与退出码编排、官方语料端到端硬验收（属 `cli-corpus`；本切片须能被 CLI 调用并回传逐记录结果）。
- 解析器实现、规范化算法实现（分别属 parser / normalize；本切片消费其产物）。
- 多文件并行、多连接并发。

## 范围与可见行为

### 记录执行

- 通过 JDBC 连接 SQLite 执行记录中的 SQL。
- 首期目标数据库标识为 `sqlite`，用于 `skipif`/`onlyif` 匹配（大小写不敏感）；`engine=sqlite`。
- 记录按文件内顺序**串行**执行，单连接。
- `statement error`：执行失败 → 通过；执行成功 → 失败。`statement ok` 反之。
- 单条记录失败**不中止**该文件后续记录（继续跑并计入统计）；`halt` 与致命错误（如连接断开）除外。
- `query`：执行查询；有期望时经 normalize 比对；无 `----`（只执行）时仅断言查询成功。
- **label 一致性**：同一文件中携带相同 label 的 query 必须产生相同结果（含哈希形态），不一致判为失败。

### 条件控制与 halt

- `skipif <db>` / `onlyif <db>` 可在同一记录前叠加多条，任一 `skipif` 命中或任一 `onlyif` 不命中即跳过下一条记录；被跳过的记录计入 skipped。
- `halt`：中止当前文件后续执行；halt 之后的记录不执行，计为 **skipped**（已决议 Q6）；halt 自身受 skipif/onlyif 控制。

### 可扩展性

- 新增一种数据库时，只需实现数据库执行器/适配器（含：数据库标识名、语句执行、查询执行返回列值），**无需修改 parser 与 runner 核心代码**。
- 首期只交付 SQLite 一个实现，但扩展点契约必须在首期成型并被 runner 实际使用（runner 依赖抽象而非 SQLite 具体类）。

## 合同

### API / 接口

- **Runner（合同级）**：接受已解析记录序列、执行器实例、初始 hash-threshold 等运行参数；按序执行并产出逐记录结果（通过/失败/跳过）及失败原因素材。
- **数据库执行器接口（合同级别，签名细节属 Design）**：报告数据库标识名；执行不返回结果的语句（成功/失败）；执行查询并返回按行列组织的原始值。runner 只通过该接口访问数据库。
- 首期交付 SQLite JDBC 实现，标识名 `sqlite`。

### 数据 / 状态

- 运行时状态（作用域为单个文件）：当前 hash-threshold（初值来自调用方，被文件内 `hash-threshold` 指令更新；跨文件由调用方重置）；待生效的 skipif/onlyif 条件集合（作用于下一条记录后清空）；label → 首次结果的映射。
- 数据库状态：工具不负责测试库初始化与清理；语料自身的 DDL/DML 构造状态。同一文件内记录共享同一连接与会话。验收可用空白 SQLite（内存库或文件库）。

### 错误与约束

- 结果不匹配 / 语句断言失败：记为该记录失败，继续执行。
- 连接失败 / 执行中连接中断：报告错误并中止当前文件（由 CLI 映射为退出码 `2`）。
- 凭据不得写入日志或报告输出。
- 首期单线程串行；对单文件执行时间不设硬性性能指标。
- 继承全局约束见「开放问题 / 已决议」，勿重开。

## 验收（Given-When-Then）

前置环境：本机可使用空白 SQLite（如 `jdbc:sqlite::memory:`）；可构造自造 `.test` 文件；parser 与 normalize 可用（或等价桩，只要行为符合其 Spec）。

### P0

- **P0-3 statement 断言**：
  - Given 一个含 `statement ok`（合法建表语句）与 `statement error`（对不存在表的查询/插入）的测试文件
  - When 执行
  - Then 两条记录均判为通过；将 `statement error` 的 SQL 换成合法语句后重跑，该记录判为失败（经 CLI 时退出码为 1）

- **P0-6 条件控制**：
  - Given 一个含 `skipif sqlite` + 记录、`onlyif sqlite` + 记录、`onlyif postgresql` + 记录的文件
  - When 以 engine=`sqlite` 执行
  - Then 第一、三条被跳过并计入 skipped，第二条正常执行

- **P0-8 扩展点隔离**：
  - Given 首期代码库
  - When 检查 runner 对数据库的依赖
  - Then runner 仅依赖数据库执行器抽象接口，首期交付的 SQLite 实现可整体替换而不修改 parser/runner 源码（QA 以代码审查/依赖检查方式验证，Reviewer 出具佐证）

### P1

- **P1-2 halt**：
  - Given 一个 `halt` 之后仍有记录的文件
  - When 执行
  - Then `halt` 后的记录不执行且计为 skipped，并在报告中体现

- **P1-4 label 一致性**：
  - Given 同一文件内两条相同 label 但结果不同的 query
  - When 执行
  - Then 第二条判为失败并指明 label 冲突

## 开放问题

### 已决议（继承自总览，勿重开）

| 编号/议题 | 结论 |
|---|---|
| 首期目标库 | SQLite（JDBC）；扩展点保留 |
| engine | `sqlite`（skipif/onlyif，大小写不敏感） |
| Q6 | halt 后记录计为 **skipped** |
| Q1 / Q2 | Java 17 / Maven |
| 产品名 | GGTEST |
| Q8 | 零豁免硬验收立场由 cli-corpus 端到端落实；本切片不得引入默示豁免逻辑 |

### 待确认

请确认本 Spec 整体后进入 Design/Plan。
