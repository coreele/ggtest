# Design: mysql-engine

## 背景与约束

- MySQL 引擎集成模式与 postgres / xugu 完全同构：executor 极简（extends AbstractJdbcExecutor），隔离在 CLI 层 FileRunner。
- 已实测：`CREATE SCHEMA` + `USE` + `DROP SCHEMA IF EXISTS`；`getString`+`wasNull`；autoCommit=true。
- 驱动公开（Maven Central），SPI 注册，shade 自动合并。无方言特殊处理（无 CASCADE 需求、IF EXISTS 可用）。

## 方案对比与决策

### 决策 1: MySQL 驱动
**选 A:** `com.mysql:mysql-connector-j:9.2.0`（compile scope，Maven Central）。shade 无需改动。

### 决策 2: 执行器
**镜像 `PostgresJdbcExecutor`。** `MySqlJdbcExecutor extends AbstractJdbcExecutor`，`engineName()="mysql"`，展示名 `"MySQL"`，初始致命标记：`"connection closed"`、`"communications link failure"`（MySQL 特有）。

### 决策 3: 隔离
**镜像 `PostgresSchemaIsolation`，用 `USE` 替代 `SET search_path`。**
| 方法 | SQL |
|---|---|
| prepare | `CREATE SCHEMA IF NOT EXISTS ggtest_<uuid>`; `USE ggtest_<uuid>` |
| setSearchPath | `USE <schema>` |
| teardown | `DROP SCHEMA IF EXISTS <schema>`（无需 CASCADE） |

### 决策 4: FileRunner
扩 `needsIsolation = isPostgres || isXugu || isMySql`。prepare/teardown/setSearchPath 按引擎分派到 `MySqlSchemaIsolation`。工厂函数加 `isMySql` 分支。

### 决策 5: RuntimeConfigResolver
`ENGINE_MYSQL="mysql"`；`normalizeEngine` 允许值加 `mysql`；`validateEngineUrlPair` 加 `jdbc:mysql:`。

### 决策 6: 测试策略（门控 `GGTEST_MY_*`）
- `MySqlJdbcExecutorTest`（门控）
- `MySqlSchemaIsolationTest`（门控）
- `MySqlCliIntegrationTest`（门控，fixtures 下 `fixtures/my/`）
- `RuntimeConfigResolverTest` 加 mysql 用例（非门控）
- 非门控不可达 URL 脱敏用例

## 模块影响

新建：`db/mysql/{MySqlJdbcExecutor,MySqlSchemaIsolation}.java`
修改：`cli/{RuntimeConfigResolver,FileRunner,Main}.java`、`pom.xml`、README
新建：测试三件套 + fixtures/my/
不变：同 Xugu 列表
