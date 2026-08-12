# Plan: mysql-engine

## 元信息

- 工作项标识: mysql-engine
- 路径等级: full
- Spec 确认: 2026-08-12
- Design: v1.0
- Review: required
- 最低验证层: L3（单元 + 集成，集成门控于 GGTEST_MY_*）
- 验证命令: 无服务 `mvn test`（门控 skip）；有服务 `GGTEST_MY_URL=jdbc:mysql://localhost:3306 GGTEST_MY_USER=jason GGTEST_MY_PASSWORD=... mvn test`

## 任务拆解（TDD，镜像 Xugu）

1. **T1: 驱动 + pom**：加 `com.mysql:mysql-connector-j:9.2.0`；验证 `mvn package` uber-jar SPI 含 mysql 行。
2. **T2: MySqlJdbcExecutor (TDD)**：先写测试（红），实现 executor（绿）。
3. **T3: MySqlSchemaIsolation (TDD)**：先写测试（红），实现隔离（绿）。
4. **T4: RuntimeConfigResolver**：mysql 允许值 + URL 校验 + 测试。
5. **T5: FileRunner isMySql + CLI 集成测试**：扩 needsIsolation；XuguCliIntegrationTest → MySqlCliIntegrationTest + fixtures/my/。
6. **T6: Main.printHelp + README**。
7. **T7: 全量回归**：`mvn test`（无服务 0 失败 + 门控 skip；有服务全绿）。

## 触碰路径

镜像 Xugu：`db/mysql/{MySqlJdbcExecutor,MySqlSchemaIsolation}.java`（新建）、`{MySqlJdbcExecutorTest,MySqlSchemaIsolationTest}`（新建）、`MySqlCliIntegrationTest`（新建）、`fixtures/my/*`（新建）、`cli/{RuntimeConfigResolver,FileRunner,Main}.java`（修改）、`pom.xml`（修改）、README（修改）、`RunnerDependencyIsolationTest`（加 mysql 豁免）。

## 验收

| ID | 要求 | 证据 |
|---|---|---|
| V1 | uber-jar 含驱动 + SPI 四行（sqlite+pg+xugu+mysql）| |
| V2-V6 | executor/隔离/CLI 集成 绿（门控）| |
| V7 | 全量 `mvn test`（无服务 0 失败；有服务全绿）| |
| V8 | Review Approve + QA Pass | |

## 交接顺序

Developer(TDD) → Reviewer(Approve) → QA(Pass) → 用户合并授权 → done 提交 → 合入 main
