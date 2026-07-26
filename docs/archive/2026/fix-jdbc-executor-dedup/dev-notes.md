# dev-notes: fix-jdbc-executor-dedup

> Developer 实施记录（L2）。依据 [plan.md](./plan.md)。

## 摘要

- 新增 `AbstractJdbcExecutor`，上提 `executeStatement` / `executeQuery` / `readRows` / fatal 判定 / `summarize`。
- 两引擎 executor 改为继承；保留 `ENGINE_NAME`、`FATAL_MESSAGE_MARKERS` 与 fatal 展示名（`SQLite` / `PostgreSQL`）。
- `DatabaseExecutor`、`CliSession` 未改。

## 变更路径

| 路径 | 变更 |
|---|---|
| `src/main/java/com/ggtest/db/AbstractJdbcExecutor.java` | 新增（121 行） |
| `src/main/java/com/ggtest/db/sqlite/SqliteJdbcExecutor.java` | 145→44 行 |
| `src/main/java/com/ggtest/db/postgres/PostgresJdbcExecutor.java` | 139→42 行 |
| `src/test/java/com/ggtest/runner/RunnerDependencyIsolationTest.java` | 排除 `AbstractJdbcExecutor.java` 的 JDBC-free 检查 |

Plan 要求 package-private，但子类在 `sqlite`/`postgres` 子包无法访问；改为 `public abstract` + `protected` 构造，仍非用户扩展点。

## 验证

| 阶段 | 命令 | 结果 |
|---|---|---|
| T0 基线 | `mvn -q clean test` | BUILD SUCCESS |
| T4 回归 | `mvn clean test` | 196 run / 0 fail / 17 skip；BUILD SUCCESS |

专项：`SqliteJdbcExecutorTest` 10/0；`RunnerAcceptanceTest` 10/0；`PostgresJdbcExecutorTest` 8 skip（无 `GGTEST_PG_URL`）；`RunnerDependencyIsolationTest` 3/0。

PG 补跑（有门控时）：`GGTEST_PG_URL='jdbc:postgresql://…' mvn -q test -Dtest=PostgresJdbcExecutorTest`

## 验收自检

| 条目 | 结果 |
|---|---|
| A1 共享逻辑仅一处 | 通过 |
| A2 `ENGINE_NAME` 不变 | 通过 |
| A3 引擎专属 markers | 通过 |
| A4 业务 vs fatal 分流 | 通过 |
| A5 全量构建绿 | 通过 |

## 交接

- CA-001 登记：QA Pass 后可标 `resolved`。
- Review 门禁 **required** → 调度 Reviewer。
