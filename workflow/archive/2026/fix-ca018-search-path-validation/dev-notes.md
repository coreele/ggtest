# Dev Notes: fix-ca018-search-path-validation

## 实现摘要

- 分支：`fix-ca018-search-path-validation` ← `main`。
- 修复 CA-018：`PostgresSchemaIsolation.setSearchPath` / `MySqlSchemaIsolation.setSearchPath` 原先将 `schema` 直接拼入 DDL 未校验，现入口统一调 `SchemaNames.requireSafe(schema)`（与 `teardown` 对齐），补纵深防御。
- 合并 Low（去重）：两类各自的私有 `isSafeIdentifier` 与 `prepare` 中 schema 名生成，抽取为 `com.ggtest.db.SchemaNames`（`generate` / `isSafe` / `requireSafe`）。

## 关键决策：架构守护适配

- 守护测试 `RunnerDependencyIsolationTest.executorAbstractionStaysFreeOfJdbc` 要求 base `com.ggtest.db` 包**不得引用 JDBC**（裸 `body.contains("java.sql")` 扫描，**含注释**）。
- 故 `SchemaNames` 不引用 `java.sql`：`requireSafe` 抛 `IllegalArgumentException`（而非 `SQLException`）。语义上「调用方传入坏标识符」属编程错误，unchecked 更贴切；isolation 类的 `setSearchPath`/`teardown` 仍 `throws SQLException`（覆盖真实 SQL 操作），非法名走 IllegalArgumentException 直传外层。
- 注意：曾因 Javadoc 注释里写了 `{@code java.sql}` 触发守护，已改为「JDBC API」措辞。

## 变更路径

| 任务 | 路径 |
|---|---|
| 新增共享工具 | `src/main/java/com/ggtest/db/SchemaNames.java` |
| 重构 PG | `src/main/java/com/ggtest/db/postgres/PostgresSchemaIsolation.java` |
| 重构 MySQL | `src/main/java/com/ggtest/db/mysql/MySqlSchemaIsolation.java` |
| 新增单测 | `src/test/java/com/ggtest/db/SchemaNamesTest.java` |

两类 isolation 现统一：`prepare = SchemaNames.generate()`；`teardown` / `setSearchPath` 入口 `SchemaNames.requireSafe(schema)`；删除私有 `isSafeIdentifier`。

## 验证

| 命令 | 结果 |
|---|---|
| `mvn -Dtest=SchemaNamesTest test` | Tests=**5** Failures=0 Errors=0 Skipped=0；BUILD SUCCESS |
| `mvn clean test` | Tests=**365** Failures=0 Errors=0 Skipped=34（既有 PG/MySQL/语料门控 skip）；BUILD SUCCESS |

`SchemaNamesTest`（确定性、无 DB）覆盖：`generate` 50 次产出 `ggtest_` 前缀且 `isSafe`；`isSafe` 接受合法名、拒绝 null/空/首字母数字/大写/SQL 元字符/空白/连字符；`requireSafe` 返回合法名、对非法名抛 `IllegalArgumentException`。

既有 DB 门控测试（PostgresSchemaIsolationTest / MySqlSchemaIsolationTest）签名未变，无 DB 时跳过；prepare/teardown 生命周期行为不变。

## 文档影响

| 类别 | 已更新路径或交接说明 |
|---|---|
| 开发文档 | N/A（Javadoc 已在 setSearchPath/teardown 注明 requireSafe 前置条件） |
| 用户文档 | N/A（无 CLI/对外行为变化） |
| 运维文档 | N/A |

## 未解决风险 / 验证缺口

| 项 | 原因 | 风险 | 恢复条件 |
|---|---|---|---|
| N/A | setSearchPath 拒绝路径需真实 Connection（DB 门控）；validation 逻辑已由 SchemaNamesTest 覆盖 | — | — |

## QA 修复回执

| 缺陷 ID | 处理 | 摘要 | 验证 | 建议复测 |
|---|---|---|---|---|
| — | N/A | 本轮无 QA Fail | — | — |
