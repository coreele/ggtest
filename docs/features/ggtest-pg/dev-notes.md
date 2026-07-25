# Dev Notes: ggtest-pg

## 实现说明

- 分支：`ggtest-pg` → `main`。驱动：`org.postgresql:postgresql` **42.7.13**。
- **T1** `PostgresJdbcExecutor`（`engineName=postgres`）：业务失败→结果对象；连接类→`FatalDatabaseException`；NULL→`null`。
- **T2** `PostgresSchemaIsolation`：CREATE / `search_path` / DROP CASCADE；CLI 编排；管理 SQL 失败→退出码 2。
- **T3–T4** `DotEnvLoader` + `RuntimeConfigResolver`：字段级 CLI > env > `.env`；`--env-file` **替换** CWD；白名单 `GGTEST_URL`/`USER`/`PASSWORD`/`ENGINE`/`HASH_THRESHOLD`；未知键忽略；无 URL / 未知 engine / engine↔URL 错配 → 退出码 2。产品不读 `GGTEST_PG_*`。
- **T5** `CliSession`：`sqlite`→独立连接+`SqliteJdbcExecutor`；`postgres`→隔离+`PostgresJdbcExecutor`。
- **T6** `fixtures/pg/`；门控测；ENV 临时目录；`RunnerDependencyIsolationTest` 禁 `db.postgres`；`.env.example`。
- **T7** README；本文件。密码不进日志/报告/`CliOptions.toString`。

### 验证证据

| 命令 | 结果 |
|---|---|
| `mvn clean test`（无 `GGTEST_PG_URL`） | SUCCESS；Tests run: **146**，Failures/Errors: **0**，Skipped: **16**（PG assume-skip） |
| `mvn -q clean package` | SUCCESS；uber-JAR / `./bin/ggtest` 可用 |
| `./bin/ggtest --url jdbc:sqlite::memory: examples/select1.test`（自备，未入库） | failed=0；exit **0**（P0-PG-5） |

ENV：`RuntimeConfigResolverTest` + `EnvConfigIntegrationTest`（临时文件）；未创建仓库真实 `.env`。

### 变更路径

`pom.xml`；`src/main/java/com/ggtest/db/postgres/`；`src/main/java/com/ggtest/cli/`；对应测试与 `fixtures/pg/`；`.env.example`；`README.md`；`docs/features/ggtest-pg/`（含本文件）。

### 未解决风险

| 未验证项 | 原因 | 风险 | 恢复条件 | 复测范围 |
|---|---|---|---|---|
| PG P0-PG-1…4 | 无 `GGTEST_PG_URL` | PG 路径未证 | 可 CREATE/DROP SCHEMA 的实例 + 门控变量 | T1–T6 PG |
| 官方语料 PG 零失败 | P1-PG-4 非硬验收 | — | — | 探索即可 |

建议后续：**Reviewer**（Review **required**）。复测：默认 `mvn test`/`package`；有 PG 时门控；ENV 临时文件；SQLite select1。

## QA 修复回执

| 缺陷 ID | 处理 | 摘要 | 验证 | 建议复测 |
|---|---|---|---|---|
| | | | | |
