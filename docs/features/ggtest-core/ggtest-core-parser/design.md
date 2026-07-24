# Design: ggtest-core / parser

> 架构决策（Plan 之前完成）。仅处理模块边界、分层与记录模型的技术选型；需求合同与验收见 [`spec.md`](./spec.md)。
>
> **feature-id**：`ggtest-core` · **sub-feature-id**：`parser`
> **适用对象**：Planner（Plan 输入）、Developer（实现依据）、Reviewer（结构审阅）。
> **前置条件**：已读 [`spec.md`](./spec.md)、总览 [`spec.md`](../spec.md)、工作项记录 [`ggtest-core.md`](../../../manager/ggtest-core.md)。
> **阅读顺序**：背景 → 模块边界与分层 → 记录模型 → 解析入口 → 错误定位 → 扩展名无关 → 方案对比 → 模块影响 → 风险 → 对 Plan 与 Developer 的要点。
> **预期结果**：读者掌握 parser 的模块划分、记录模型形状与解析入口契约，可据此编写 Plan 与实现。
> **失败处理**：发现 Spec 缺失合同信息（API 数据约束、错误约定、验收）时停止并报告 Manager，不在本文件替代 Spec 决策。

## 背景

- 本切片为 GGTEST 首个实现切片，工作区为空（无 `pom.xml`、无 `src/`），故 parser 需同时引导 Maven 工程骨架。
- 约束（继承自 Spec/总览，勿重开）：Java 17、Maven、SQLite 首期目标但**本切片不连库**、`.slt` 与 `.test` 等价且单文件不强制扩展名、CLI 优先但**本切片不交付 CLI**。
- 下游 `normalize`（消费记录模型比对）与 `runner-sqlite`（消费记录模型执行）依赖 parser 产出的记录模型；模型形状是本 Design 的核心边界决策。

## 模块边界与分层

分层方向（依赖单向向内，`model` 为最内层，不依赖其他包）：

```text
com.ggtest.parser  ──依赖──▶  com.ggtest.model
        │
        └─ 内部子职责：行读取(定位) → 记录切分(空行分隔) → 类型识别与模型构建
（下游切片）
com.ggtest.normalize / com.ggtest.runner  ──依赖──▶  com.ggtest.model
```

- **`com.ggtest.model`（记录模型，共享）**：纯数据类型，无解析逻辑、无 I/O、无第三方依赖。下游切片仅依赖此包即可消费记录，不接触 parser 内部。
- **`com.ggtest.parser`（解析）**：对外暴露解析入口，内部为单遍状态机（行读取带行号 → 按空行切分记录块 → 逐块按首行 token 分派构造模型）。
- **决策依据**：将 `model` 与 `parser` 拆为两个包，使 `normalize`/`runner-sqlite` 依赖稳定的数据契约而非解析实现，降低跨切片耦合。

## 记录模型

采用 Java 17 `sealed interface` + `record` 表达封闭的记录类型集合，便于下游用穷尽 `switch` 消费。

- **`SourceLocation`**（record）：`String sourceName`、`int startLine`（1 基）。每条记录携带，支撑错误与报告定位。
- **`SqlTestRecord`**（sealed interface）：所有记录的公共父类型，暴露 `SourceLocation location()`。已知实现（permits）：

| 记录类型 | 建议类型 | 关键字段 |
|---|---|---|
| statement | `StatementRecord` | `String sql`、`StatementExpectation expectation`（枚举 `OK` / `ERROR`）、`SourceLocation` |
| query | `QueryRecord` | `List<ColumnType> typeSignature`、`SortMode sortMode`、`Optional<String> label`、`String sql`、`boolean hasExpectedResults`、`List<String> expectedResults`、`SourceLocation` |
| skipif | `SkipIfRecord` | `String dbName`、`SourceLocation` |
| onlyif | `OnlyIfRecord` | `String dbName`、`SourceLocation` |
| hash-threshold | `HashThresholdRecord` | `int threshold`、`SourceLocation` |
| halt | `HaltRecord` | `SourceLocation` |

- **枚举**：
  - `ColumnType { INTEGER('I'), TEXT('T'), REAL('R') }`——类型签名逐字符映射为 `List<ColumnType>`；非法字符触发解析错误。
  - `SortMode { NOSORT, ROWSORT, VALUESORT }`——默认 `NOSORT`；仅识别声明，比对语义属 `normalize`。
  - `StatementExpectation { OK, ERROR }`——`statement error` 不携带错误消息匹配（首期非目标）。
- **query「仅执行不比对」**：无 `----` 时 `hasExpectedResults=false`、`expectedResults` 为空列表（P1-b）。含 `----` 时 `hasExpectedResults=true`，`----` 之后至记录结束的原始行进入 `expectedResults`（原样保留，规范化/哈希属 `normalize`）。
- **skipif/onlyif 作为独立有序记录**：见「方案对比与决策」决策 1。条件与 `hash-threshold`/`halt` 均按出现顺序作为独立记录发出并保留操作数；它们与后续 statement/query 的运行时关联由 `runner-sqlite` 负责，本切片不做归属。

## 解析入口 API 形状

`com.ggtest.parser.SqlLogicTestParser`（形状级，最终签名以实现为准）：

- `List<SqlTestRecord> parse(Path file) throws IOException`——读取 UTF-8 文件，`sourceName` 取文件路径字符串。
- `List<SqlTestRecord> parse(String sourceName, String content)`——从文本源解析，`sourceName` 仅用于错误定位（可为逻辑名）。

产出为**有序** `List<SqlTestRecord>`（保留文件出现顺序）；注释行（`#` 开头）与空行不产生记录（P1-a）。UTF-8 编码固定。

## 错误定位机制

- 专用异常 `com.ggtest.parser.ParseException extends RuntimeException`，字段：`String sourceName`、`int lineNumber`、`String reason`；`getMessage()` 格式化为 `"<sourceName>:<lineNumber>: <reason>"`。
- **失败即抛（fail-fast）**：遇首个非法输入（未知记录类型、非法类型签名、残缺格式等）立即抛出，携带**文件名 + 行号 + 原因**（P0-7）；不静默跳过。
- `IOException`（文件不可读）与 `ParseException`（内容非法）分离，便于 `cli-corpus` 分别映射退出码（解析错误 → 退出码 2）。异常为非受检，避免强制沿调用链传播 `throws`，`cli-corpus` 顶层捕获即可。

## 扩展名无关处理

parser 不检查文件后缀；`parse(Path)` 与 `parse(String, String)` 对相同内容产出语义等价的记录序列（P1-c）。目录递归与 `*.test`/`*.slt` 过滤属 `cli-corpus`，不在本切片。

## 方案对比与决策

**决策 1：skipif/onlyif 的建模方式**

| 方案 | 概要 | 优点 | 缺点 |
|---|---|---|---|
| A（选定）| 作为独立有序记录发出，保留操作数 | parser 保持纯切分职责；归属逻辑集中于 runner | 需 runner 做前缀关联 |
| B | 作为修饰符附加到后续 statement/query | 单条记录自带条件，消费简单 | parser 承担运行时语义；跨切片职责越界 |

**决策:** 选 A。Spec 明确将 skipif/onlyif/hash-threshold/halt 列为记录类型并要求「仅解析为记录、保留操作数、产出有序列表」，运行时求值属 `runner-sqlite`。

**决策 2：记录模型类型表达**

| 方案 | 概要 | 优点 | 缺点 |
|---|---|---|---|
| A（选定）| `sealed interface` + `record` | 类型封闭、下游穷尽 `switch`、不可变 | 需 Java 16+（已满足 17）|
| B | 单一类 + `type` 枚举字段 | 结构简单 | 字段松散、类型不安全、易漏判 |

**决策:** 选 A，契合 Java 17 且为下游提供强类型契约。

**决策 3：错误处理策略**

| 方案 | 概要 | 优点 | 缺点 |
|---|---|---|---|
| A（选定）| fail-fast 抛 `ParseException`（首个错误）| 满足 P0-7 定位；实现简单 | 一次只报一个错误 |
| B | 收集全部错误后返回 | 一次暴露多处问题 | 复杂度高；首期无需求 |

**决策:** 选 A；Spec 仅要求定位非法输入，fail-fast 足够，B 留作后续扩展点。

## 模块影响

- 新增 Maven 工程骨架：`pom.xml`（groupId `com.ggtest`、artifactId `ggtest`、Java 17、JUnit 5 测试依赖）；`src/main/java`、`src/test/java`、`src/test/resources` 目录。
- 新增包 `com.ggtest.model`（数据类型）与 `com.ggtest.parser`（入口 + 内部扫描/分派 + `ParseException`）。
- 为下游确立稳定契约：`normalize`、`runner-sqlite`、`cli-corpus` 后续仅依赖 `com.ggtest.model` 与 `SqlLogicTestParser`。

## 风险

| 风险 | 影响 | 缓解 |
|---|---|---|
| 记录模型字段不满足下游 runner 需求 | 后续切片返工 | 模型置于共享 `model` 包并明确契约；skipif/onlyif 保留操作数与顺序 |
| `----` 后期望结果块的边界（空行/EOF 终止）处理有误 | 解析结果偏差 | 明确「`----` 至空行或 EOF 为期望块，原样保留」并以 P1-a/P1-b 测试锁定 |
| Maven/JUnit 版本或本地环境差异 | 无法构建/测试 | pom 固定 Java 17 与 JUnit 5 版本；验证命令统一为 Maven |
| 类型签名非法字符判定遗漏 | 漏报解析错误 | 以 `ColumnType` 枚举穷举合法字符，非法即抛 `ParseException`（P0-7）|

## 对 Plan 与 Developer 的要点

### Plan

- 任务须引导 Maven 骨架，再按 `model → parser 入口/异常 → 扫描分派` 分层落地，最后覆盖 P0-7、P1-a/b/c 的测试。
- 验证限定为 Maven（Java 17）本地可执行命令；无数据库、无网络依赖。
- 文档影响：开发文档更新项目 README（构建/解析入口说明）；用户文档、运维文档本切片 N/A（不交付 CLI/无部署面）。

### Developer

- 采用 TDD：先按验收项写失败测试（含 `.test`/`.slt`/无后缀 fixture），再实现。
- 记录模型放 `com.ggtest.model` 且不可变；解析入口与 `ParseException` 放 `com.ggtest.parser`。
- 解析错误消息须含文件名 + 行号 + 原因；非法输入不得静默跳过。
- 不连库、不实现排序/哈希比对、不实现 skipif/onlyif 运行时求值（仅解析为记录）。
