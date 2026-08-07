# Plan: ggtest-core / parser

> 实施与验证计划。需求依据见 [`spec.md`](./spec.md)，架构依据见 [`design.md`](./design.md)。
>
> **适用对象**：Developer（实施）、Reviewer（审阅）、QA（验收）。
> **前置条件**：Spec 已确认（approved）；Design（`design.md`）已存在；本地具备 Java 17 与 Maven。
> **阅读顺序**：元信息 → 目标摘要 → 任务拆解 → 依赖与顺序 → 触碰路径 → 验证 → 验收 → 文档影响 → 交接。
> **预期结果**：Developer 可据此以 TDD 实现 parser 并完成本地验证；QA 可据验收项独立复核。
> **失败处理**：验证命令失败时按「验证」节预期证据定位；无法执行时按第「无法执行验证时的处理」记录原因/风险/恢复条件。

## 元信息

- 工作项标识: ggtest-core（sub-feature-id: parser）
- 依据 Spec: [agents/docs/features/ggtest-core/ggtest-core-parser/spec.md](./spec.md)
- 依据 Design: [agents/docs/features/ggtest-core/ggtest-core-parser/design.md](./design.md)
- 路径等级: full
- Review 门禁: required（进入 QA 前须取得 Reviewer `Approve`）
- 最低验证层: L2（单元测试 + 构建）
- 验证命令: `mvn -q clean test`（含编译与单元测试）

## 适用工程规范

- [文档工程](../../standards/documentation.md)
- [Git 协作](../../standards/git.md)（本工作区非 Git：跳过提交与合并，不跳过其他门禁）
- [质量与验证](../../standards/quality.md)
- [安全](../../standards/security.md)

## 目标摘要

- 引导 GGTEST Maven 工程骨架（Java 17 + JUnit 5）。
- 实现记录模型（`com.ggtest.model`）与解析入口（`com.ggtest.parser.SqlLogicTestParser`），将 `.test`/`.slt`/无后缀单文件 UTF-8 文本解析为有序记录序列。
- 解析错误 fail-fast 抛出，携带文件名 + 行号 + 原因。
- 依据：Spec P0-7、P1-a、P1-b、P1-c；Design 决策 1–3 与分层。

## 任务拆解

采用 TDD：先写失败测试，再实现至通过。

1. **T1 — Maven 工程骨架**：创建 `pom.xml`（groupId `com.ggtest`、artifactId `ggtest`、version `0.1.0-SNAPSHOT`、Java 17、JUnit 5 依赖、Surefire）；建立 `src/main/java`、`src/test/java`、`src/test/resources`。
   - 完成条件：`mvn -q clean compile` 成功；`mvn -q test` 可运行（允许空/占位测试）。
2. **T2 — 记录模型（`com.ggtest.model`）**：`SourceLocation`；`sealed interface SqlTestRecord`；`StatementRecord`、`QueryRecord`、`SkipIfRecord`、`OnlyIfRecord`、`HashThresholdRecord`、`HaltRecord`；枚举 `ColumnType`、`SortMode`、`StatementExpectation`。全部不可变。
   - 完成条件：类型编译通过；下游可穷尽 `switch`（默认分支或 permits 完整）。
3. **T3 — 解析错误（`com.ggtest.parser.ParseException`）**：字段 `sourceName`/`lineNumber`/`reason`，消息格式 `"<sourceName>:<lineNumber>: <reason>"`。先写 P0-7 失败测试。
   - 完成条件：未知记录类型输入触发异常，消息含文件名与行号。
4. **T4 — 解析入口与扫描分派（`SqlLogicTestParser`）**：`parse(Path)` 与 `parse(String sourceName, String content)`；单遍状态机（带行号行读取 → 空行切分 → 首行 token 分派 → 构造模型）；处理注释/空行、类型签名、排序模式、label、`----` 期望块（至空行或 EOF）、无 `----` 的仅执行标志、skipif/onlyif/hash-threshold/halt 独立记录。先写 P1-a/b/c 失败测试。
   - 完成条件：P1-a/b/c 测试通过；非法类型签名/残缺格式抛 `ParseException`。
5. **T5 — 验收测试套件与 fixtures**：在 `src/test/resources` 放置 `.test`、`.slt`、无标准后缀三份等价内容及含未知记录类型的错误样例；测试逐项对齐 P0-7、P1-a、P1-b、P1-c（见「验收」）。
   - 完成条件：全部验收测试 Pass。
6. **T6 — 开发文档**：更新根 `README.md`，记录构建命令与解析入口用法；`model`/`parser` 公共类型加必要 Javadoc。
   - 完成条件：README 含可复现构建/测试命令与解析入口示例。

## 依赖与顺序

- T1 → T2 → {T3, T4}；T4 依赖 T2、T3；T5 依赖 T2–T4；T6 依赖 T2–T5。
- T3 与 T4 内部均先测试后实现（TDD）。
- 上游依赖：无（parser 为 `parser ∥ normalize` 起点，不依赖其他切片）。

## 触碰路径

- `pom.xml`（新增）
- `src/main/java/com/ggtest/model/`（新增：记录模型与枚举）
- `src/main/java/com/ggtest/parser/`（新增：`SqlLogicTestParser`、`ParseException`、内部扫描/分派）
- `src/test/java/com/ggtest/parser/`（新增：解析与错误定位测试）
- `src/test/resources/`（新增：`.test`/`.slt`/无后缀 fixture 与错误样例）
- `README.md`（更新：构建与解析入口说明）

## 验证

- **最低验证层**：L2（单元测试 + 构建）。理由：parser 为纯内存逻辑，仅涉及本地文件读取（用临时文件/资源覆盖），不连库、不涉网络或模块间集成，L2 已充分覆盖行为；集成/端到端属下游 `runner-sqlite`、`cli-corpus`。
- **验证命令**：`mvn -q clean test`（Java 17）。可选 `mvn -q clean compile` 单独确认编译。
- **预期证据**：Maven `BUILD SUCCESS`；Surefire 报告全部用例 Pass、0 失败 0 错误；无编译错误。
- **无法执行验证时的处理**：当前无已知障碍（Java 17 + Maven 为本切片唯一依赖）。若本地缺失 JDK 17 或 Maven，记录原因于 `dev-notes.md`，风险为无法确认构建/测试，恢复条件为安装 JDK 17 与 Maven 后重跑上述命令。

## 验收

逐项对齐 [`spec.md`](./spec.md)（要求 → 测试策略 → 预期证据）：

- **P0-7 解析错误定位**：构造含未知记录类型行的 fixture → 解析应抛 `ParseException`，消息含文件名与该行号。（CLI 退出码 2 属 `cli-corpus`，本切片仅保证错误信息足以定位。）
- **P1-a 记录类型与注释**：含注释、空行及全部记录类型（statement ok/error、含 `----` 的 query、skipif、onlyif、hash-threshold、halt）的 fixture → 产出对应类型的有序记录，注释/空行不产生记录。
- **P1-b 无分隔符的 query**：仅含类型签名 + SQL、无 `----` 的 query → `QueryRecord.hasExpectedResults=false` 且 `expectedResults` 为空。
- **P1-c 扩展名无关**：同一内容存为 `.test`、`.slt`、无标准后缀三份 → 三份解析结果记录序列语义等价。

## 文档影响

| 类别 | 更新路径或 N/A 理由 |
|---|---|
| 开发文档 | `README.md`（构建/测试命令、解析入口用法）；`com.ggtest.model` 与 `SqlLogicTestParser` 公共 Javadoc |
| 用户文档 | N/A：本切片不交付 CLI 或用户可见功能（用户可见 CLI 属 `cli-corpus`） |
| 运维文档 | N/A：无部署、监控或运维面变更 |

## 安全影响

- 涉输入处理（解析外部文本文件）：仅读取、不执行 SQL、不连库、不修改输入文件、无反序列化外部对象；无敏感信息写入代码/文档/测试。Reviewer 按 [安全规范](../../standards/security.md) 对输入处理面确认。

## 交接顺序

实施（Developer TDD + `dev-notes.md` 记录验证）→ Reviewer 审阅（测试有效性、文档影响、安全影响）→ 取得 `Approve`（Review 门禁 required，为进入 QA 前置）→ QA 依 Spec/Plan 独立验收并写 `qa-report.md`。非 Git 工作区：跳过提交与合并。

## 修订记录

| 日期 | 摘要 |
|---|---|
| 2026-07-24 | 初稿：任务拆解、验证（L2/Maven）、P0-7/P1-a/b/c 验收策略、文档影响 |
