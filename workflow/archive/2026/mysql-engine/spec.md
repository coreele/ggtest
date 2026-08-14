# Spec: mysql-engine

> 状态: v1.0（待用户确认）。路径等级 `full`。引擎集成模式已由 `postgres` → `xugu` 两次实践建立——MySQL 镜像该模式，简化部分（驱动公开、语法更标准）。

## 合同

**引擎取值：** `--engine mysql`。`RuntimeConfigResolver` 新增 `ENGINE_MYSQL="mysql"`；`normalizeEngine` 允许值扩为 `sqlite / postgres / xugu / mysql`；`validateEngineUrlPair`：`mysql` → URL 须以 `jdbc:mysql:` 开头。

**执行器：** `MySqlJdbcExecutor extends AbstractJdbcExecutor`。`engineName()="mysql"`、展示名 `"MySQL"`。

**隔离：** `MySqlSchemaIsolation`（镜像 `PostgresSchemaIsolation`/`XuguSchemaIsolation`）。`prepare`：`CREATE SCHEMA IF NOT EXISTS <uuid>` + `USE <uuid>`；`setSearchPath`：`USE <schema>`；`teardown`：`DROP SCHEMA IF EXISTS <schema>`（无需 `CASCADE`）。

**CLI 接线：** `FileRunner` 扩展 `needsIsolation = isPostgres || isXugu || isMySql`。

**测试门控：** `GGTEST_MY_URL` / `GGTEST_MY_USER` / `GGTEST_MY_PASSWORD`（不入 `DotEnvLoader` 白名单）。

**驱动：** `com.mysql:mysql-connector-j:9.2.0`（compile scope，Maven Central，SPI 注册，shade 自动合并）。

## 验收 P0

- P0-1 `--engine mysql` 解析通过，`jdbc:mysql:` URL 校验
- P0-2 单文件端到端（live 服务）：`[PASSED]`，退出码 0
- P0-3 断言失败报告：`[FAILED]` + error block，退出码 1
- P0-4 NULL / skipif mysql / 凭据脱敏 / 零回归
- P1 跨文件隔离、`--parallel` 隔离、`conn=<name>` 多连接、`--halt` 正交
