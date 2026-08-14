# Plan: fix-ca018-search-path-validation

## 元信息

- 工作项标识: fix-ca018-search-path-validation（未拆分）
- 依据 Spec: N/A（fast，单点安全加固 + 去重）
- 依据 Design: N/A
- 依据 UI: N/A
- 路径等级: fast
- Review 门禁: required
- 来源: `workflow/audit/2026-08-13-src.md` Findings CA-018（Medium §2 + 合并 Low §4）
- 最低验证层: 单元测试 + 构建（架构守护测试 + 新增 SchemaNames 单测）
- 验证命令: `mvn -q -Dtest=SchemaNamesTest test`、`mvn -q clean test`
- 预期证据: SchemaNamesTest 全 Pass；`mvn clean test` BUILD SUCCESS、0 failures（架构守护 `RunnerDependencyIsolationTest` 仍绿）

## 目标摘要

修复 CA-018：`PostgresSchemaIsolation.setSearchPath` / `MySqlSchemaIsolation.setSearchPath` 将 `schema` 直接拼入 DDL（`SET search_path TO <schema>` / `USE <schema>`），未调用标识符校验，与同类 `teardown`（已校验）不一致；纵深防御缺口。同时合并 Low：两类的 `isSafeIdentifier` 与 `prepare` 中 schema 名生成逐字符重复，易单边漂移。

## Bug 定位

- `db/postgres/PostgresSchemaIsolation.java:59-65`（setSearchPath 无校验）
- `db/mysql/MySqlSchemaIsolation.java:42-48`（setSearchPath 无校验）
- 两类各自私有 `isSafeIdentifier`（`[a-z][a-z0-9_]*`）与 `prepare` 中 `"ggtest_" + UUID…toLowerCase()` 重复。

当前调用方（FileRunner）仅传入 `prepare()` 生成的 UUID 名，实践安全；public 入口缺纵深防御。

## 修复方案

1. 新增 `com.ggtest.db.SchemaNames`：`generate()`（`ggtest_` + 小写 hex UUID）、`isSafe(String)`、`requireSafe(String)`。
   - **架构约束**：base `com.ggtest.db` 包须保持 driver-agnostic（守护测试 `RunnerDependencyIsolationTest.executorAbstractionStaysFreeOfJdbc` 扫描文件正文，含注释，禁止出现 `java.sql` 等字面量）。故 `SchemaNames` 不得引用 JDBC；`requireSafe` 抛 `IllegalArgumentException`（语义为「调用方传入坏标识符」），而非 `SQLException`。
2. 两个 isolation 类：`prepare()` 用 `SchemaNames.generate()`；`teardown()` 与 **`setSearchPath()`** 入口调 `SchemaNames.requireSafe(schema)`；删除各自的私有 `isSafeIdentifier`。
3. 新增 `SchemaNamesTest`（确定性、无 DB）：`generate` 产出安全前缀名；`isSafe` 接受/拒绝用例；`requireSafe` 返回合法名 / 对非法名抛 `IllegalArgumentException`。

注：`teardown` 不安全名异常类型由 `SQLException` 变为 `IllegalArgumentException`（防御路径，实践中 prepare 生成名永不触发；无测试断言旧类型）。

## 任务拆解

1. 新增 `SchemaNames` + `SchemaNamesTest`。
2. 重构 `PostgresSchemaIsolation` / `MySqlSchemaIsolation`（含 setSearchPath 校验、去重）。
3. 开发者验证（SchemaNamesTest + `mvn clean test` 含架构守护）。
4. Review / QA。

## 触碰路径

- `src/main/java/com/ggtest/db/SchemaNames.java`（新增）
- `src/main/java/com/ggtest/db/postgres/PostgresSchemaIsolation.java`
- `src/main/java/com/ggtest/db/mysql/MySqlSchemaIsolation.java`
- `src/test/java/com/ggtest/db/SchemaNamesTest.java`（新增）

## 验收与验证

| ID | 要求或命令 | 预期证据 |
|---|---|---|
| V1 | setSearchPath/teardown 对非法 schema 抛异常（不拼入 DDL） | requireSafe 单测 Pass |
| V2 | prepare 生成的名始终安全 | generate 单测 Pass |
| V3 | base db 包不引用 JDBC（架构守护） | RunnerDependencyIsolationTest Pass |
| V4 | `mvn clean test` | BUILD SUCCESS，0 failures |

## 文档影响

| 类别 | 更新路径或 N/A 理由 |
|---|---|
| 开发文档 | N/A（实现内部细节；isolation 类 Javadoc 已说明 requireSafe 前置条件） |
| 用户文档 | N/A（无 CLI/对外行为变化；仅加固 public 入口） |
| 运维文档 | N/A |

## 交接顺序

Developer → Reviewer（required）→ QA → 用户合并授权 → done 一次提交 → 合入 main。

## 修订记录

| 日期 | 摘要 |
|---|---|
| 2026-08-13 | 初版 Plan（来源：2026-08-13 审计 CA-018） |
