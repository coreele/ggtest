# QA Report: ggtest-pg

## 轮次

| 轮次 | 日期 | 范围 | 结论 |
|---|---|---|---|
| 1 | 2026-07-25 | 首测：Spec P0/P1（PG+ENV）+ Plan L3；`be38ad5`；Review Approve；无 PG 门控 | **Blocked** |
| 2 | 2026-07-25 | 回归：有 PG 门控实跑 P0-PG + ENV/SQLite；`be38ad5` | **Fail** |
| 3 | 2026-07-25 | 回归：修复 `e7e6249`；关闭 DEF-PG-001/002；Reviewer Approve | **Pass** |
| 4 | 2026-07-25 | 回归：`b6ea61f`；关闭 DEF-PG-003（根 `.env` 污染）；Reviewer Approve 轮次 3 | **Pass** |

---

## 轮次 1（首测；无 PG 门控）

### 环境与命令

| 项 | 值 |
|---|---|
| 工作项 | `ggtest-pg`（未拆分；`full`） |
| 分支 / 版本 | `ggtest-pg` → `main`；**`be38ad5`** |
| 入口门禁 | Plan `approved`；Reviewer **`Approve`**（注明 PG P0 未实跑） |
| 构建 | Java 17+；代理 `127.0.0.1:7890` |
| PG 门控 | **未设置** |
| 语料 / ENV | 自备 `examples/select1.test`；ENV 仅临时目录；**未**写仓库 `.env` |

| 命令 | 结果 |
|---|---|
| `mvn clean test`（无 PG） | SUCCESS；**146** run / **0** fail / **16** skip |
| `mvn -q clean package` | EXIT **0** |
| `./bin/ggtest --url jdbc:sqlite::memory: examples/select1.test` | EXIT **0**；`passed=1031 failed=0` |
| 临时目录 ENV CLI | P0-ENV-1…4、`--env-file` 缺失、密码 → 通过 |
| 有门控 `mvn test` / PG CLI | **未执行** |

### 覆盖

| ID | 条目 | 结果 | 证据 |
|---|---|---|---|
| Review | Approve | **通过** | `review.md` |
| P0-PG-1…4；P0-PG-3(b)；P1-PG-1/2(PG) | PG 专属 | **未证** | 无门控 skip |
| P0-PG-3 (a)(c)(d) | SQLite 默认；未知/错配→2 | **通过** | fixture EXIT 0；`mysql`→2；`postgres`+sqlite→2 |
| P0-PG-5 | SQLite select1 | **通过** | `failed=0`、EXIT 0 |
| P0-ENV-1…4；P1-ENV-1/2；`--env-file` 缺失 | ENV 合同 | **通过** | 临时目录 + 自动化 |
| P1-PG-3 | 无 PG 不红 | **通过** | 146/0/16 |
| P1-PG-4 | 官方语料 PG 零失败 | **N/A** | 非 Pass 条件 |
| Plan L3 无 PG | 套件/package | **通过** | |
| Plan L3 有 PG | P0-PG-1…4 | **未证** → **Blocked** | |

### 回归 / 文档 / 安全

| 范围 | 结果 |
|---|---|
| SQLite/CLI/runner；Runner 禁 `db.postgres`；select1 | 通过 |
| 未提交 `examples/` / 真实 `.env` | 遵守 |
| README / `.env.example` / 凭据回显 / 驱动 42.7.13 | 通过 |
| 允许合并？ | **否**（非 Pass；非安全否决） |

### 缺陷

无实现缺陷（环境缺口）。

### 阻塞与恢复

| 未验证项 | 原因 | 风险 | 恢复条件 | 复测范围 |
|---|---|---|---|---|
| P0-PG-1…4；P0-PG-3(b)；P1-PG-1；P1-PG-2(PG) | 无 `GGTEST_PG_*` / 可达 PG | PG 路径未实跑 | 可达 PG（可 CREATE/DROP SCHEMA）+ 门控变量 | T1–T6 PG 测与 fixtures |

### 结论（轮次 1）

- 总体: **Blocked**
- 合并: **不合并**；禁止请求合并授权
- 报告不 git commit

---

## 轮次 2（回归；PG 门控已恢复）

### 入口门禁核验

| 条件 | 证据 | 结果 |
|---|---|---|
| Plan `approved` | `agents/manager/ggtest-pg.md` | 满足 |
| Reviewer **Approve** | `review.md` | 满足 |
| `blocked`→`qa`；PG 可达 | Manager 恢复；localhost:5432 | 满足 |
| 实现 | **`be38ad5`** | 满足 |

### 环境与命令

| 项 | 值 |
|---|---|
| 分支 / 版本 | `ggtest-pg`；**`be38ad5`**；JDK 17.0.20 |
| 代理 | `127.0.0.1:7890` |
| PG 门控 | localhost:5432 / 库 `postgres` / 用户 `postgres`；**密码未写入报告**（空密码；可 CREATE/DROP SCHEMA） |
| ENV / 语料 | 临时目录；自备 `examples/select1.test`；**未**写仓库 `.env` |

| 命令 | 结果 |
|---|---|
| `GGTEST_PG_*` + `mvn -q clean test` | EXIT **1**；**146** run / **1** fail / **0** error / **4** skip |
| `mvn -q clean package` | EXIT **1**（surefire 阻断） |
| `mvn -q clean package -DskipTests` | EXIT **0**（仅辅助 CLI；**不计** Plan Pass） |
| select1 SQLite | EXIT **0**；`passed=1031 failed=0` |
| 临时目录 ENV | P0-ENV-1…4、`--env-file`、P1-ENV-1 → 通过 |
| `./bin/ggtest --engine postgres` + PG fixtures | EXIT **2**：`No suitable driver found` → DEF-PG-002 |
| classpath `Main` + PG fixtures | basic/cross-file EXIT **0**；conditions EXIT **1** → DEF-PG-001 |

Surefire（有门控）：`PostgresJdbcExecutorTest` 8/0；`PostgresSchemaIsolationTest` 1/0；`PostgresCliIntegrationTest` 4 tests / **1** fail / **1** skip（空密码 → `passwordIsNeverPrinted…`）。失败：`skipifAndOnlyIfRespectPostgresEngineCaseInsensitively`（`fixtures/pg/conditions.test`）。

### Spec / Plan 验收

| ID | 要求 | 结果 | 证据 |
|---|---|---|---|
| P0-PG-1 | 执行器合同 | **通过** | `PostgresJdbcExecutorTest` 8/0 |
| P0-PG-2 | skipif/onlyif `postgres` | **Fail** | conditions `failed=1`；期望区吞并后续记录 → DEF-PG-001 |
| P0-PG-3 (a)(c)(d) | SQLite 默认；未知/错配→2 | **通过** | 回归 EXIT 0/2 |
| P0-PG-3 (b) | 产品 CLI + PG | **Fail** | `./bin/ggtest` 无驱动 → DEF-PG-002；classpath `Main` basic EXIT 0 |
| P0-PG-4 | 跨文件 schema 隔离 | **通过**（Maven/classpath） | isolation + cross-file EXIT 0；产品 CLI 被 DEF-PG-002 阻断 |
| P0-PG-5 | SQLite select1 | **通过** | `failed=0`、EXIT 0 |
| P0-ENV-1…4；P1-ENV-1/2 | ENV 合同 | **通过** | 临时目录 |
| P1-PG-1 | `--engine` 大小写 | **Fail（连带）** | 同 conditions / DEF-PG-001 |
| P1-PG-2 | PG 无密码明文 | **部分** | 空密码测 skip；ENV 路径无泄漏；非空 PG 密码未实跑 |
| P1-PG-3 | 无 PG 不红 | **通过** | 轮次 1 |
| P1-PG-4 | 官方语料 PG 零失败 | **N/A** | |
| Plan L3 `mvn test` / `package`（有 PG） | 全绿 | **Fail** | 146/1；package 阻断 |

### 回归

| 范围 | 结果 |
|---|---|
| ENV；select1；未知 engine / URL 错配 | 通过 |
| 全量非失败项 | 146 中仅 conditions 1 fail |
| 未提交 `examples/` / 真实 `.env` | 遵守 |

### 文档与安全

| 项 | 结果 |
|---|---|
| README / `.env.example` | 通过（沿用） |
| 报告无密码明文；未写仓库 `.env` | 通过 |
| shade SPI 仅 sqlite | **缺陷** DEF-PG-002 |
| 允许合并？ | **否** |

### 缺陷

| ID | 严重度 | 摘要 | 状态 | 处理说明 / 验证证据 |
|---|---|---|---|---|
| **DEF-PG-001** | **高** | `fixtures/pg/conditions.test`：`----` 期望区后缺空行，后续记录被当作期望结果 → P0-PG-2 / P1-PG-1 Fail | **open** | expected 含 `7`/`onlyif sqlite`/`statement ok`/`CREATE…`；actual 仅 `7`。对照其它 fixtures 结果后有空行。**修**：`7` 后补空行；复测该 CLI 测 + `mvn test`。 |
| **DEF-PG-002** | **高** | shade uber-JAR 未合并 `META-INF/services/java.sql.Driver`（仅 `org.sqlite.JDBC`）→ `./bin/ggtest` 无法连 PG | **open** | `No suitable driver found`；类在 JAR 内但 SPI 缺 `org.postgresql.Driver`；classpath `Main` 同 fixture EXIT 0。**修**：`ServicesResourceTransformer`（或等价）；复测 `package` 后 `./bin/ggtest` + PG basic/cross-file EXIT 0。 |

### Developer 修复范围

1. 修 DEF-PG-001、DEF-PG-002。
2. Review `required`：须重新 **Approve**。
3. QA 追加轮次：有门控 `mvn -q clean test`（0 fail）、完整 `package`、`./bin/ggtest` PG fixtures、ENV/select1。

### 阻塞

无环境阻塞（轮次 1 门控已解除）。本轮为可修复 Fail。

### 结论（轮次 2）

- 总体: **Fail**
- 通过: P0-PG-1、P0-PG-3(a)(c)(d)、P0-PG-4（Maven/classpath）、P0-PG-5、ENV、文档/安全抽查
- 失败: P0-PG-2、P0-PG-3(b)、连带 P1-PG-1；`mvn test`/`package` 红；DEF-PG-001、DEF-PG-002 **open**
- 恢复条件: N/A
- 合并: **不合并**；禁止请求合并授权
- 报告不 git commit

---

## 轮次 3（回归；修复 `e7e6249`）

### 入口门禁核验

| 条件 | 证据 | 结果 |
|---|---|---|
| Plan `approved` | `agents/manager/ggtest-pg.md` | 满足 |
| Reviewer **Approve**（修复后） | `review.md` 轮次 2；`e7e6249` | 满足 |
| 实现 | `be38ad5` + **`e7e6249`**；分支 `ggtest-pg`；HEAD=`e7e6249` | 满足 |
| 环境 | localhost postgres；JDK 17.0.20 | 满足 |

### 环境与命令

| 项 | 值 |
|---|---|
| 分支 / 版本 | `ggtest-pg`；**`e7e6249`** |
| 代理 | Maven → `127.0.0.1:7890`；JDBC 不走代理 |
| PG 门控 | localhost:5432 / 库 `postgres` / 用户 `postgres`；**密码未写入报告** |
| ENV / 语料 | 临时目录；自备 `examples/select1.test`；**未**写仓库 `.env` |

| 命令 | 结果 |
|---|---|
| `GGTEST_PG_*` + `mvn -q clean test` | EXIT **0**；**148** run / **0** fail / **0** error / **5** skip |
| `GGTEST_PG_*` + `mvn -q clean package` | EXIT **0** |
| 打包后 `ExecutableJarManifestTest` | **3** run / **0** fail / **0** skip |
| `./bin/ggtest` PG basic | EXIT **0**；`passed=3 failed=0` |
| `./bin/ggtest` PG conditions（`postgres` / `Postgres`） | EXIT **0**；`passed=3 failed=0 skipped=2` |
| `./bin/ggtest` PG cross-file | EXIT **0**；`passed=6 failed=0` |
| select1 SQLite | EXIT **0**；`passed=1031 failed=0` |
| 临时目录 ENV | P0-ENV-1…4、`--env-file` 替换/缺失、P1-ENV-1 → 通过 |
| 未知 engine / URL 错配 | EXIT **2** |

Surefire PG：`PostgresJdbcExecutorTest` 8/0；`PostgresSchemaIsolationTest` 1/0；`PostgresCliIntegrationTest` 4/0 fail / **1** skip（空密码 → `passwordIsNeverPrintedWhenRunningPostgres`）。另 skip：`ExecutableJarManifestTest` 2（`clean test` 无 jar）、`CorpusHardAcceptanceTest` 2（官方语料未入库）。

shaded JAR SPI：`org.sqlite.JDBC` + `org.postgresql.Driver`。

### Spec / Plan 验收

| ID | 要求 | 结果 | 证据 |
|---|---|---|---|
| P0-PG-1 | 执行器合同 | **通过** | `PostgresJdbcExecutorTest` 8/0 |
| P0-PG-2 | skipif/onlyif `postgres` | **通过** | conditions CLI EXIT 0；Maven 绿；fixture `7` 后空行 |
| P0-PG-3 (a)(c)(d) | SQLite 默认；未知/错配→2 | **通过** | 抽检 EXIT 2；SQLite 路径绿 |
| P0-PG-3 (b) | 产品 CLI + PG | **通过** | `./bin/ggtest` basic EXIT 0 |
| P0-PG-4 | 跨文件 schema 隔离 | **通过** | Maven + CLI `passed=6 failed=0` EXIT 0 |
| P0-PG-5 | SQLite select1 | **通过** | `failed=0`、EXIT 0 |
| P0-ENV-1…4；P1-ENV-1/2 | ENV 合同 | **通过** | 临时目录；`.env.example` + gitignore |
| P1-PG-1 | `--engine` 大小写 | **通过** | `--engine Postgres` conditions EXIT 0 |
| P1-PG-2 | 无密码明文 | **通过** | CLI/ENV 输出无泄漏；非空门控密码测仍 skip（ENV 等价覆盖） |
| P1-PG-3 | 无 PG 不红 | **通过** | 轮次 1 |
| P1-PG-4 | 官方语料 PG 零失败 | **N/A** | 非 Pass 条件 |
| Plan L3（有 PG） | `mvn test` / `package` | **通过** | 148/0/5；package EXIT 0 |

### 回归

| 范围 | 结果 |
|---|---|
| 轮次 2 失败项（P0-PG-2、P1-PG-1、P0-PG-3(b)、P0-PG-4 产品 CLI、有门控 test/package） | 全部通过 |
| ENV；select1；未知 engine / URL 错配；P0-PG-1 | 通过 |
| 未提交 `examples/` / 真实 `.env` | 遵守（工作区仍有未跟踪 `examples/`） |

### 文档与安全

| 项 | 结果 |
|---|---|
| README / `.env.example` | 通过（修复未改用户合同） |
| 报告无密码明文；未写仓库 `.env` | 通过 |
| shade SPI 含 sqlite + postgresql | **通过** |
| 允许合并？ | **是**（须用户授权；Manager 推进） |

### 缺陷

| ID | 严重度 | 摘要 | 状态 | 处理说明 / 验证证据 |
|---|---|---|---|---|
| **DEF-PG-001** | 高 | conditions fixture 期望区缺空行 | **closed** | `e7e6249` 补空行；CLI conditions EXIT 0（`skipped=2`）；Maven 同测绿 |
| **DEF-PG-002** | 高 | shade 未合并 JDBC SPI | **closed** | `ServicesResourceTransformer`；SPI 双驱动；产品 CLI PG EXIT 0；打包后 SPI 测 3/0 |

开放缺陷：无。

### 阻塞

无。

### 结论（轮次 3）

- 总体: **Pass**
- 通过: 轮次 2 失败项与受影响回归；有门控 L3；ENV；select1；文档/安全；DEF-PG-001/002 **closed**
- 失败: 无
- 合并: 质量条件已满足；**由 Manager 请求用户合并授权**；本报告 **不** git commit

---

## 轮次 4（回归；DEF-PG-003 / `b6ea61f`）

### 入口门禁核验

| 条件 | 证据 | 结果 |
|---|---|---|
| Plan `approved` | `agents/manager/ggtest-pg.md` | 满足 |
| Reviewer **Approve**（轮次 3） | `review.md`；`b6ea61f` | 满足 |
| 实现 | HEAD=`b6ea61f`；分支 `ggtest-pg` | 满足 |
| 环境 | localhost postgres；JDK 17.0.20；根本地 `.env`（`GGTEST_ENGINE=postgres` + `jdbc:postgresql://…`）**在场且未改写** | 满足 |

### 环境与命令

| 项 | 值 |
|---|---|
| 分支 / 版本 | `ggtest-pg`；**`b6ea61f`** |
| 代理 | Maven → `127.0.0.1:7890` |
| 根 `.env` | postgres engine + postgresql URL；报告无密码明文 |
| PG 门控 | localhost:5432 / 库 `postgres` / 用户 `postgres`；PASSWORD 空（未写入报告） |
| 语料 / ENV | 自备 `examples/select1.test`（未跟踪）；ENV 仅临时目录 |

| 命令 | 结果 |
|---|---|
| 根 `.env` + **无** `GGTEST_PG_*` + `mvn -q clean test` | EXIT **0**；**148**/0/0/**17** skip（PG：8+1+4；jar 2；corpus 2） |
| 根 `.env` + `GGTEST_PG_*` + `mvn -q clean test` | EXIT **0**；**148**/0/0/**5** skip；executor 8/0；schema 1/0；cli 4 run / 0 fail / **1** skip（空密码） |
| `GGTEST_PG_*` + `mvn -q clean package` | EXIT **0**；嵌入测 148/0/5 |
| 根 `.env` + `./bin/ggtest --url jdbc:sqlite::memory:`（无 engine 覆盖） | EXIT **2**；`engine 'postgres' requires a jdbc:postgresql: URL` |
| `--engine sqlite --url jdbc:sqlite::memory:` select1（根 `.env` + CLI 覆盖） | EXIT **0**；`passed=1031 failed=0` |
| 干净 CWD JAR + `--url jdbc:sqlite::memory:` select1 | EXIT **0**；`passed=1031 failed=0` |
| 未知 `mysql`；postgres↔sqlite / sqlite↔postgresql 错配 | 均 EXIT **2** |
| `./bin/ggtest --engine postgres\|Postgres` basic / conditions / cross-file | EXIT **0**；basic 3/0；conditions 3/0 skipped=2；cross-file `passed=6 failed=0` |
| 临时目录 ENV P0-ENV-1…4、`--env-file` 替换/缺失、P1 不回显 | 通过；`EnvConfigIntegrationTest` 8/0 |

### DEF-PG-003 关闭证据

| 项 | 内容 |
|---|---|
| 现象 | 根运行时 `.env`（postgres）污染 `Main.run` CLI 测 → `mvn test` 红 |
| 未修复对照 | `e7e6249` + 根 `.env`：**148**/9 fail/**15** skip（SQLite URL↔engine 错配；`missingUrl` 被 `.env` 补足）。未回退复现 |
| 方案 A | 产品仍读 CWD `.env`；测用 `envLookup` + `@TempDir`；SQLite 必跑；`GGTEST_PG_*` 时 PG 仍跑 |
| 关闭证据 | 根 `.env`：门控关 **148/0/17**；门控开 **148/0/5**（PG 实跑）；产品冲突 EXIT **2** |
| 状态 | **closed** |

### Spec / Plan 验收

| ID | 要求 | 结果 | 证据 |
|---|---|---|---|
| DEF-PG-003 | 根 `.env` 不污染测；产品合同不变 | **通过** | 门控关/开 + 冲突 EXIT 2 |
| P0-PG-1…4；P1-PG-1 | PG 执行/条件/CLI/隔离 | **通过** | 门控开 Maven + 产品 fixtures |
| P0-PG-3 (a)(c)(d) | 默认 SQLite；未知/错配→2 | **通过** | 干净 CWD；EXIT 2 抽检 |
| P0-PG-5 | SQLite select1 失败=0 | **通过** | CLI 覆盖与干净 CWD `failed=0` EXIT 0 |
| P0-ENV-1…4；P1-ENV-1/2 | ENV 合同 | **通过** | 临时目录 + Maven 8/0 |
| P1-PG-2 | 无密码明文 | **通过** | 输出无泄漏；非空门控密码测仍 skip |
| P1-PG-3 | 无门控不红 | **通过** | 门控关 148/0/17 |
| P1-PG-4 | 官方语料 PG 零失败 | **N/A** | 非 Pass 条件 |
| Plan L3 | `mvn test` / `package` | **通过** | 关/开全绿；package EXIT 0 |

### 回归 / 文档 / 安全

| 范围 | 结果 |
|---|---|
| 轮次 3 Pass（L3、PG fixtures、ENV、select1、未知/错配）；DEF-PG-001/002 | 通过；保持 **closed** |
| 未提交 `examples/` / 真实 `.env`；未改写用户根 `.env` | 遵守；`.env`/`.env.pg` gitignore；`examples/` 未 stage |
| 产品 CWD `.env` 未削弱；报告无密码明文 | 通过（冲突 EXIT 2） |
| 允许合并？ | **是**（须用户授权）；报告 **不** git commit |

### 缺陷

| ID | 严重度 | 摘要 | 状态 | 验证证据 |
|---|---|---|---|---|
| DEF-PG-001 | 高 | conditions 期望区缺空行 | **closed** | 轮次 3；本轮 conditions EXIT 0 |
| DEF-PG-002 | 高 | shade JDBC SPI | **closed** | 轮次 3；本轮产品 CLI PG EXIT 0 |
| **DEF-PG-003** | **高** | 根 `.env` 污染 CLI 测 | **closed** | `b6ea61f`；148/0/17 与 148/0/5；冲突 EXIT 2 |

开放缺陷：无。阻塞：无。

### 未验证项

| 项 | 原因 |
|---|---|
| 非空 `GGTEST_PG_PASSWORD` → `passwordIsNeverPrintedWhenRunningPostgres` | 本机空密码 skip；P1 由 ENV 临时密码覆盖 |
| 回退 `e7e6249` 复现 9 fail | 未回退；引用合并授权前对照 |

### 结论（轮次 4）

- 总体: **Pass**
- 通过: DEF-PG-003 **closed**；根 `.env` 下门控关/开 L3；PG/ENV/SQLite/CLI 不回归；产品 CWD `.env` 未削弱
- 失败: 无
- 合并: 质量条件已满足；**由 Manager 请求用户合并授权**；`qa-report.md` / `review.md` **不** git commit
