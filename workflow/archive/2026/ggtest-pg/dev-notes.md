# Dev Notes: ggtest-pg

## 实现说明

- 分支：`ggtest-pg` → `main`。驱动：`org.postgresql:postgresql` **42.7.13**。
- **T1** `PostgresJdbcExecutor`（`engineName=postgres`）：业务失败→结果对象；连接类→`FatalDatabaseException`；NULL→`null`。
- **T2** `PostgresSchemaIsolation`：CREATE / `search_path` / DROP CASCADE；CLI 编排；管理 SQL 失败→退出码 2。
- **T3–T4** `DotEnvLoader` + `RuntimeConfigResolver`：字段级 CLI > env > `.env`；`--env-file` **替换** CWD；白名单 `GGTEST_URL`/`USER`/`PASSWORD`/`ENGINE`/`HASH_THRESHOLD`；未知键忽略；无 URL / 未知 engine / engine↔URL 错配 → 退出码 2。产品不读 `GGTEST_PG_*`。
- **T5** `CliSession`：`sqlite`→独立连接+`SqliteJdbcExecutor`；`postgres`→隔离+`PostgresJdbcExecutor`。
- **T6** `fixtures/pg/`；门控测；ENV 临时目录；`RunnerDependencyIsolationTest` 禁 `db.postgres`；`.env.example`。
- **T7** README；本文件。密码不进日志/报告/`CliOptions.toString`。

### 变更路径

`pom.xml`；`src/main/java/com/ggtest/db/postgres/`；`src/main/java/com/ggtest/cli/`；对应测试与 `fixtures/pg/`；`.env.example`；`README.md`；`workflow/archive/2026/ggtest-pg/`（含本文件）。

### 首轮交付验证（无 PG 门控）

| 命令 | 结果 |
|---|---|
| `mvn clean test` | **146** run / **0** fail / **16** skip |
| `mvn -q clean package` | SUCCESS |
| select1 SQLite（自备，未入库） | `failed=0` exit **0** |

ENV 仅临时文件；未写仓库真实 `.env`。

### 未解决风险

| 未验证项 | 原因 | 风险 | 恢复条件 | 复测范围 |
|---|---|---|---|---|
| 官方语料 PG 零失败 | P1-PG-4 非硬验收 | — | — | 探索即可 |
| P1-PG-2 非空密码 | 门控密码为空 → 测 skip | 非空密码回显未实跑 | 设非空 `GGTEST_PG_PASSWORD` | `passwordIsNeverPrintedWhenRunningPostgres` |

## QA 修复回执（轮次 2 Fail）

| 缺陷 ID | 处理 | 摘要 | 验证 | 建议复测 |
|---|---|---|---|---|
| **DEF-PG-001** | **已修复** | `fixtures/pg/conditions.test`：`7` 后补空行。增解析回归 `pgConditionsFixture_expectedBlockEndsBeforeFollowingOnlyIf`。 | 解析绿；门控 CLI conditions exit **0**（`skipped=2`） | P0-PG-2、P1-PG-1 |
| **DEF-PG-002** | **已修复** | shade 加 `ServicesResourceTransformer`。增 jar 存在时 SPI 断言。 | SPI=`org.sqlite.JDBC`+`org.postgresql.Driver`；`./bin/ggtest` basic/cross-file exit **0** | P0-PG-3(b)、P0-PG-4 产品 CLI；`package` |

### 修复后验证（有门控）

| 命令 | 结果 |
|---|---|
| `GGTEST_PG_*` + `mvn -q clean test` | **148** / **0** fail / **5** skip |
| `GGTEST_PG_*` + `mvn -q clean package` | SUCCESS |
| `./bin/ggtest --engine postgres\|Postgres` basic / conditions / cross-file | 均 exit **0** |
| select1 SQLite | `passed=1031 failed=0` exit **0** |

门控：localhost:5432 / 库与用户 `postgres`；密码未写入。未提交 `examples/` / 真实 `.env`。

## QA 修复回执（DEF-PG-003）

| 缺陷 ID | 处理 | 摘要 | 验证 | 建议复测 |
|---|---|---|---|---|
| **DEF-PG-003** | **已修复** | 根因：三参 `Main.run` 硬编码 `System::getenv` + 进程 CWD，根 `.env`（`GGTEST_ENGINE=postgres` + PG URL）污染编排测（SQLite URL 硬错配 exit 2；`missingUrl` 被 URL 救活）。方案 A：产品 `main`/三参 `run` **不变**；新增 `Main.run(..., envLookup, workingDirectory)`；上述四类 CLI 测注入 `key -> null` + `@TempDir`。`GGTEST_PG_*` 仅组装 argv（门控保留）。 | 见下表 | 见下 |

### DEF-PG-003 验证（根目录存在本地 `.env`：postgres + PG URL）

| 命令 | 结果 |
|---|---|
| 修复前复现（`MainOrchestrationTest` 两例） | `allPassing…` exit **2**；`missingUrl…` stderr 无 url |
| 无 `GGTEST_PG_*`：`mvn -q clean test` | **148** / **0** fail / **17** skip |
| 有门控 `GGTEST_PG_URL`/`USER`（PASSWORD 空）：`mvn -q clean test` | **148** / **0** fail / **5** skip |
| `mvn -q clean package` | SUCCESS |
| select1 SQLite（自备；临时移开根 `.env` 后产品 CLI） | `passed=1031 failed=0` exit **0** |

未提交 `.env` / `.env.pg` / `examples/`。产品 CLI 读 CWD `.env` 合同未改。

### 建议 QA 复测（关闭 DEF-PG-003）

1. 根 `.env`（postgres）在场：无门控 `mvn -q clean test` 全绿（PG skip）；有 `GGTEST_PG_*` 时 PG 亦绿。
2. `mvn -q clean package` SUCCESS。
3. 产品 CLI：无冲突 `.env` 时 SQLite select1 exit 0；有冲突时 engine↔URL 错配仍 exit 2。
4. ENV：`--env-file` / `@TempDir` 用例绿；不得依赖仓库根 `.env`。
