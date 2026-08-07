# Review: ggtest-pg

## 轮次

| 轮次 | 范围 | 版本 | 结论 |
|---|---|---|---|
| 1 | 首审 T1–T7 全量实现 | `be38ad5` | **Approve**（PG P0 未实跑） |
| 2 | 复审 QA 轮次 2 缺陷修复（DEF-PG-001 / DEF-PG-002） | `e7e6249` | **Approve** |
| 3 | 复审 DEF-PG-003（方案 A：测试隔离；产品 `.env` 合同不变） | `b6ea61f` | **Approve** |

---

## 轮次 1（首审；`be38ad5`）

### 审阅范围

| 项 | 内容 |
|---|---|
| 工作项 | `ggtest-pg`（未拆分；`full`；Review **required**） |
| 依据 | [`spec.md`](./spec.md)、[`design.md`](./design.md)、[`plan.md`](./plan.md)、[`dev-notes.md`](./dev-notes.md)；`agents/manager/ggtest-pg.md`；`agents/standards/{documentation,quality,security,git}.md` |
| 实现版本 | 分支 `ggtest-pg`；commit **`be38ad5`**（相对 `main` `39caef8`）；目标 `main` |
| 审阅内容 | T1–T7 实现/测试；README / `.env.example` / `dev-notes`；安全与 Git 禁止项；独立 `mvn clean test` / `package`、自备 SQLite `select1` |
| 未纳入 | 不改业务代码；不进 QA；不合并；不 commit 本报告 |

### 结论

**Approve**

无阻塞项。T1–T7 对齐 Spec/Design/Plan；无 PG 门控时套件绿；文档与安全满足进入 QA。本环境未实跑 PG 专属 P0（无 `GGTEST_PG_URL`），缺口已记，**不得**默示 Pass；QA 须在可达 PG 上补证或记 Blocked。

### 实现正确性

| 合同要点 | 结果 |
|---|---|
| T1 `PostgresJdbcExecutor`，`engineName=postgres`；业务失败→结果对象；连接类→致命；NULL→`null` | 通过（门控测有 PG 时覆盖） |
| T2 schema：CREATE / `search_path` / DROP CASCADE；CLI 编排；管理失败→退出码 2 | 通过 |
| T3 `.env`：CLI > env > `.env`；CWD 默认；`--env-file` **替换**；白名单键；未知键忽略；无 URL / 显式路径缺失→2 | 通过 |
| T4 `sqlite`\|`postgres`；engine↔URL（`jdbc:sqlite:` / `jdbc:postgresql:`）硬错误→2、不执行 | 通过 |
| T5 `CliSession` 按 engine 选执行器；PG 每文件隔离 | 通过 |
| T6 fixtures；Runner 禁 `db.postgres`/`java.sql`；产品不读 `GGTEST_PG_*` | 通过 |
| T7 SQLite 硬验收无回归；README / `.env.example` / `dev-notes` | 通过（select1 exit 0） |
| 驱动 postgresql **42.7.13** | 通过（`pom.xml`） |

未见改 parser/normalize/runner 行为合同；未见提交 `examples/` 或真实 `.env`。

### 测试有效性

| 要求 | 证据 | 结果 |
|---|---|---|
| P0-ENV-1…4；`--env-file` 替换/缺失；未知 engine；URL 错配；未知键；门控键隔离 | `RuntimeConfigResolverTest`、`EnvConfigIntegrationTest`、`DotEnvLoaderTest` | 通过（可证伪） |
| P1-ENV-1/2 密码不入输出；`.env.example` | `EnvConfigIntegrationTest`、`MainOrchestrationTest`、`CliOptions.toString` 脱敏 | 通过 |
| P0-PG-1…4、P1-PG-1/2 | `PostgresJdbcExecutorTest`、`PostgresSchemaIsolationTest`、`PostgresCliIntegrationTest` + `fixtures/pg/` | **本环境 skip**（无 `GGTEST_PG_URL`）；结构可证伪 |
| P1-PG-3 无 PG 不红 | 146 run / 0 fail / **16 skip** | 通过 |
| P0-PG-5 select1 | `./bin/ggtest --url jdbc:sqlite::memory: examples/select1.test` → failed=0、exit **0**（语料未入库） | 通过 |
| Runner 隔离 | `RunnerDependencyIsolationTest` | 通过 |
| L3 | `mvn clean test`、`mvn -q clean package` → **SUCCESS** | 通过 |

### 必修项

| ID | 位置 | 问题 | 状态 |
|---|---|---|---|
| — | — | 无 | — |

> `Comment` 不得包含阻塞项；阻塞问题须使用 `Request changes`。

### 非阻塞建议

| ID | 严重度 | 位置 | 说明 |
|---|---|---|---|
| N1 | low | `CliSession.sanitize` | 注释称防凭据回显，实现仅 `strip()`；报告路径本不写 password 且有输出断言。可真脱敏或改注释。 |
| N2 | low | `RuntimeConfigResolver` 测 | `GGTEST_HASH_THRESHOLD` 三源合并缺专用断言；实现已有。 |

### 文档影响核对

| Plan 声明 | 实现是否一致 | 备注 |
|---|---|---|
| 开发文档 | 是 | `README.md`；`dev-notes.md`；Javadoc；`.env.example` |
| 用户文档 | 是 | README：`--engine postgres`、隔离/权限、`.env`/`--env-file`/优先级、`GGTEST_*` vs `GGTEST_PG_*`、PG 官方语料非硬验收 |
| 运维文档 | N/A | 与 Plan 一致；门控/代理在 README + `dev-notes` |

### 安全影响核对

| 检查项 | 结果 | 备注 |
|---|---|---|
| 敏感信息 | 通过 | 无真实 `.env`；`.gitignore`：`.env`/`.env.*`/`!.env.example`；`.env.example` 占位；commit 无 `examples/` |
| 认证与授权 | N/A | 无应用级认证；DB 凭据经 JDBC Properties |
| 输入与外部访问 | 通过 | `.env` 白名单；schema UUID + teardown 标识符校验 |
| 依赖变更 | 通过 | postgresql **42.7.13**；无第三方 dotenv |
| 凭据入日志 | 通过 | `CliOptions.toString` 脱敏；stdout/stderr/报告断言无密码；产品不读 `GGTEST_PG_*` |

无未解决安全问题；允许进入 QA。

### Git 合规

| 检查 | 结果 |
|---|---|
| 工作分支 | `ggtest-pg` |
| 提交 | `be38ad5` 含实现/测试/文档/README/`.env.example`；**无**真实 `.env`、**无** `examples/` |
| 工作区 | 本地未跟踪 `examples/`（符合勿提交） |

### 验证证据摘要（审阅者独立）

| 命令 | 结果 |
|---|---|
| `mvn clean test`（代理 `127.0.0.1:7890`；无 `GGTEST_PG_URL`） | SUCCESS；**146** run / 0 fail / **16** skip |
| `mvn -q clean package` | EXIT 0 |
| `./bin/ggtest --url jdbc:sqlite::memory: examples/select1.test` | failed=0；exit **0** |

#### 未验证缺口

| 未验证项 | 原因 | 风险 | 恢复条件 | 复测范围 |
|---|---|---|---|---|
| P0-PG-1…4（及门控下 P1-PG-1/2） | 无 `GGTEST_PG_URL` | PG 路径未在本 Review 实跑 | 可 CREATE/DROP SCHEMA 的实例 + `GGTEST_PG_*` | T1–T6 PG 测与 fixtures |

### 后续动作

1. Manager：Review 门禁满足 → 可调度 **QA**（本报告 ≠ QA Pass）。
2. QA：按 Spec P0/P1 独立验收；有 PG 时必跑门控；无 PG 不得默示 PG Pass，须记原因/风险/恢复条件。
3. N1–N2 不阻塞；若修则复审限相关文件。
4. 本 `review.md` 不由 Reviewer 提交（待合并授权前留工作区）。

---

## 轮次 2（复审 QA 缺陷修复；`e7e6249`）

### 审阅范围

| 项 | 内容 |
|---|---|
| 工作项 | `ggtest-pg`（未拆分；`full`；Review **required**，须重新 Approve 才能进 QA） |
| 实现版本 | 分支 `ggtest-pg`；commit **`e7e6249`**（相对 `be38ad5`） |
| 依据 | [`qa-report.md`](./qa-report.md) 轮次 2（DEF-PG-001 / DEF-PG-002）；[`dev-notes.md`](./dev-notes.md) 修复回执；[`spec.md`](./spec.md)、[`plan.md`](./plan.md)；`agents/standards/{documentation,quality,security,git}.md` |
| 差异 | `pom.xml`（shade `ServicesResourceTransformer`）；`src/test/resources/fixtures/pg/conditions.test`（+1 空行）；`ExecutableJarManifestTest`（SPI 断言）；`SqlLogicTestParserTest`（解析回归）；`dev-notes.md` |
| 独立验证 | 有 PG 门控（localhost postgres）；命令与结果见「验证证据摘要」 |
| 未纳入 | 不改业务代码/测试；不进 QA；不合并；不 commit 本报告 |

### 结论

**Approve**

无阻塞项。DEF-PG-001、DEF-PG-002 已实质修复并经独立复跑证实；产品 CLI 在 PG 上 exit 0；SQLite 硬验收无回归；无缩减测试或改变合同的规避。修复未触碰产品源码，仅打包配置 + fixture + 测试。

### 缺陷修复核对

| 缺陷 | 修复 | 独立验证 | 结论 |
|---|---|---|---|
| **DEF-PG-001**（conditions fixture 期望区吞并后续记录） | `fixtures/pg/conditions.test` 在 `7` 后补空行，使期望区在 `onlyif sqlite` 前结束 | 产品 CLI conditions `skipped=2`、exit 0；`PostgresCliIntegrationTest.skipifAndOnlyIfRespectPostgresEngineCaseInsensitively` 绿 | **已修复**（由 QA 关闭） |
| **DEF-PG-002**（uber-JAR 仅含 `org.sqlite.JDBC` SPI） | `pom.xml` shade 增 `ServicesResourceTransformer` 合并 `META-INF/services/java.sql.Driver`；`filters` 未排除 `META-INF/services/*` | 打包后 SPI 断言实跑通过；产品 CLI PG fixtures exit 0（`No suitable driver found` 消失） | **已修复**（由 QA 关闭） |

修复方式无合同变更：fixture 修正**扩大**了覆盖（`onlyif sqlite` 此前被吞入期望区，从未作为记录执行）；未删改断言或放宽期望。

### 实现正确性

| 检查 | 结果 |
|---|---|
| 修复限于缺陷根因，无越界改动 | 通过（`git show --stat e7e6249`：仅 `pom.xml` + 1 fixture + 2 测试 + `dev-notes.md`；**无** `src/main/`） |
| 同类 fixture 是否残留同一缺陷 | 通过（全量扫描 `src/test/resources/fixtures/`：`----` 期望区后紧跟记录关键字的实例 **0** 处） |
| DEF-PG-001 未以改 parser 合同规避 | 通过（parser 未改；空行终止期望区符合既有行为与 `select1` 语料） |
| shade 变更副作用 | 通过（`ManifestResourceTransformer` 保留 `Main-Class`；`package` exit 0；SQLite 路径不受影响） |
| 回归：SQLite 硬验收 | 通过（`select1` `passed=1031 failed=0`、exit **0**） |

### 测试有效性

| 要求 | 证据 | 结果 |
|---|---|---|
| DEF-PG-001 有回归防护且可证伪 | `SqlLogicTestParserTest.pgConditionsFixture_expectedBlockEndsBeforeFollowingOnlyIf` 断言首个 `query` 的 `expectedResults()` 恰为 `["7"]` 且存在 `onlyif sqlite` 记录；删空行即回到 QA 轮次 2 观察到的吞并态 → 失败 | 通过 |
| DEF-PG-002 有回归防护 | `ExecutableJarManifestTest.packagedJarMergesJdbcDriverSpiForSqliteAndPostgres` 断言 SPI 同时含 `org.sqlite.JDBC` 与 `org.postgresql.Driver`；本审阅在有 jar 时实跑 **未 skip** 且通过 | 通过（门控时机见 N3） |
| Plan L3（有 PG 门控） | `mvn clean test` 148 run / 0 fail / 5 skip；`mvn -q clean package` exit 0（详见验证证据摘要） | 通过 |
| 5 处 skip 合理 | `ExecutableJarManifestTest` 2（test 阶段无 jar）、`CorpusHardAcceptanceTest` 2（官方语料未入库）、`PostgresCliIntegrationTest` 1（门控密码为空 → 非空密码回显未覆盖，同轮次 1 风险） | 通过（缺口见下） |
| PG 专属 P0 本轮实跑 | `PostgresJdbcExecutorTest` 8/0、`PostgresSchemaIsolationTest` 1/0、`PostgresCliIntegrationTest` 4 run / 0 fail；产品 CLI cross-file `passed=6 failed=0` | 通过（轮次 1 缺口已消除） |

### 文档影响核对

| 项 | 结果 |
|---|---|
| `dev-notes.md` 修复回执 | 通过（缺陷 ID、处理、验证、建议复测齐备；数值与本审阅复跑一致：148/0/5、CLI exit 0） |
| README / `.env.example` | 无需更新（修复不改用户可见 CLI 合同） |
| 运维文档 | N/A（与 Plan 一致） |

### 安全影响核对

| 检查项 | 结果 | 备注 |
|---|---|---|
| 敏感信息 | 通过 | 提交无真实 `.env` / `examples/`；`.gitignore` 保持 `.env`、`.env.*`、`!.env.example`；本报告不含密码明文（门控：localhost postgres） |
| 依赖变更 | 通过 | 无新增依赖；仅 shade 打包配置 |
| 打包面变化 | 通过 | `ServicesResourceTransformer` 合并的 SPI 来自既有依赖（sqlite-jdbc、postgresql），未引入新代码；签名文件仍由 `filters` 排除 |
| 凭据回显 | 通过 | 复跑 CLI 输出仅 `passed/failed/skipped`，无 URL 凭据；`CliOptions.toString` 脱敏未改 |

无未解决安全问题。

### Git 合规

| 检查 | 结果 |
|---|---|
| 工作分支 | `ggtest-pg`（未在 `main` 实施） |
| 提交 `e7e6249` | 内容与缺陷范围一致；message 为 `fix(ggtest-pg): …` 并说明 DEF-PG-001/002 |
| 禁止提交项 | 通过（无 `examples/`、无真实 `.env`、无构建产物） |
| 工作区 | `agents/manager/*`、`review.md`、`qa-report.md`、`examples/` 未提交（符合 §1.4 与运维约定） |

### 必修项

| ID | 位置 | 问题 | 状态 |
|---|---|---|---|
| — | — | 无 | — |

### 非阻塞建议

| ID | 严重度 | 位置 | 说明 |
|---|---|---|---|
| N1 | low | `CliSession.sanitize` | 轮次 1 遗留：注释称防凭据回显，实现仅 `strip()`。 |
| N2 | low | `RuntimeConfigResolver` 测 | 轮次 1 遗留：`GGTEST_HASH_THRESHOLD` 三源合并缺专用断言。 |
| N3 | medium | `ExecutableJarManifestTest` + `pom.xml` | SPI 断言为 `@EnabledIf(jarExists)`，surefire 在 `package` 之前运行，故 `mvn clean test` 与 `mvn clean package` 单趟均 **skip**；仅在已存在 jar 的二次构建才生效，且可能读到陈旧 jar。DEF-PG-002 的自动化门控因此偏弱，建议改为 `verify` 阶段校验（failsafe / 打包后校验），使其在单趟 CI 中必跑。不阻塞：本轮已人工在打包后实跑该断言并通过。 |

### 验证证据摘要（审阅者独立）

| 命令 | 结果 |
|---|---|
| `mvn clean test`（PG 门控：localhost postgres；代理 `127.0.0.1:7890`） | BUILD SUCCESS；**148** run / **0** fail / **0** error / **5** skip |
| `mvn -q clean package`（同门控） | exit **0** |
| 打包后 `mvn test -Dtest=ExecutableJarManifestTest` | **3** run / **0** fail / **0** skip（SPI 断言实跑） |
| `./bin/ggtest --engine postgres` basic / conditions | `passed=3 failed=0 skipped=0` / `passed=3 failed=0 skipped=2`；均 exit **0** |
| `./bin/ggtest --engine Postgres` cross-file schema-a + schema-b | `passed=6 failed=0`；exit **0** |
| `./bin/ggtest --url jdbc:sqlite::memory: examples/select1.test` | `passed=1031 failed=0`；exit **0** |
| fixtures 全量扫描（期望区后缺空行） | 0 处 |

#### 未验证缺口

| 未验证项 | 原因 | 风险 | 恢复条件 | 复测范围 |
|---|---|---|---|---|
| P1-PG-2 非空密码不回显 | 门控 PG 密码为空 → `passwordIsNeverPrintedWhenRunningPostgres` skip | 非空密码回显路径未实跑（ENV 路径已有等价断言） | 设非空 `GGTEST_PG_PASSWORD` 的实例 | 该用例 + PG CLI 输出抽查 |
| P1-PG-4 官方语料 PG 零失败 | Spec 定为非硬验收 | — | — | 探索性 |

### 后续动作

1. Manager：Review 门禁满足（`Approve`）→ 可调度 **QA 轮次 3**（本报告 ≠ QA Pass）。
2. QA 轮次 3 建议范围：有门控 `mvn -q clean test`（0 fail）+ 完整 `package`、`./bin/ggtest` PG basic/conditions/cross-file、ENV P0/P1、SQLite `select1`；关闭 DEF-PG-001 / DEF-PG-002。
3. N1–N3 不阻塞；若修则复审限相关文件。
4. 本 `review.md` 不由 Reviewer 提交（待合并授权前留工作区）。

---

## 轮次 3（复审 DEF-PG-003；`b6ea61f`）

### 审阅范围

| 项 | 内容 |
|---|---|
| 工作项 | `ggtest-pg`（未拆分；`full`；Review **required**） |
| 实现版本 | 分支 `ggtest-pg`；commit **`b6ea61f`**（基于 `e7e6249`；未 amend） |
| 依据 | `agents/manager/ggtest-pg.md`（DEF-PG-003 / 方案 A）；[`qa-report.md`](./qa-report.md)；[`dev-notes.md`](./dev-notes.md)；既有 Spec/Design/Plan（**无**合同变更）；`agents/standards/{documentation,quality,security,git}.md` |
| 差异 | `git show b6ea61f`：`Main.java`；`MainOrchestrationTest` / `EnvConfigIntegrationTest` / `CorpusHardAcceptanceTest` / `PostgresCliIntegrationTest`；`dev-notes.md` |
| 焦点 | 方案 A 是否修复 DEF-PG-003；产品读 CWD `.env` 不变；SQLite 必跑；`GGTEST_PG_*` 门控保留；安全/提交边界 |
| 未纳入 | 不重审 `be38ad5`/`e7e6249` 全量；不改代码；不进 QA；不合并；不 commit 本报告 |

### 结论

**Approve**

无阻塞项。DEF-PG-003 按方案 A 修复：产品 `main`/三参 `run` 仍 `System::getenv` + 进程 CWD；四类 CLI 测注入 `key -> null` + `@TempDir`；根目录 postgres `.env` 下独立复跑全绿，门控开时 PG 仍跑。未削弱验收；提交无 `.env`/`examples/`；无未解决安全问题。

### 缺陷修复核对

| 缺陷 | 方案 A 要求 | 独立验证 | 结论 |
|---|---|---|---|
| **DEF-PG-003** | 产品读 CWD `.env` **不改**；测注入 `envLookup` + `workingDirectory`；SQLite 必跑；`GGTEST_PG_*` 时 PG 仍跑 | 根 `.env`（`GGTEST_ENGINE=postgres` + `jdbc:postgresql://…`）：无门控 **148/0/17**；有门控 **148/0/5**（PG CLI/executor/schema 实跑）；产品 CLI sqlite URL + 根 `.env` engine → exit **2** | **已修复**（待 QA 关闭） |

未以缩减测试或改 Spec 规避：PG skip 仍 `assumeTrue(GGTEST_PG_URL)`；无 `@Disabled`；门控仍组装 argv。

### 实现正确性

| 检查 | 结果 |
|---|---|
| 三参 `run` → `System::getenv` + `Path.of("").toAbsolutePath()`；`main` → 三参 | 通过 |
| 五参重载注入；`RuntimeConfigResolver.resolve(parsed, envLookup, workingDirectory)` | 通过 |
| 四类 CLI 测：`key -> null` + `@TempDir` | 通过 |
| `EnvConfigIntegrationTest`：`--env-file`/临时文件精确控制，不依赖仓库根 `.env` | 通过 |
| `PostgresCliIntegrationTest`：`GGTEST_PG_*` 仅组 argv；`Main.run` 空 env | 通过 |
| 产品合同保留 | 通过（根 `.env` postgres + `--url jdbc:sqlite::memory:` → exit **2**，stderr 含 hard-mismatch） |

### 测试有效性

| 要求 | 证据 | 结果 |
|---|---|---|
| 根 `.env` 污染下编排测不失败 | 无门控 **148**/0/**17**；`MainOrchestrationTest` 10/0/0 | 通过 |
| SQLite 基线必跑 | 无门控下编排/ENV 全跑（非假 skip） | 通过 |
| 门控开 PG 执行 | `GGTEST_PG_URL`/`USER`（PASSWORD 空）：**148**/0/**5**；executor 8、schema 1、CLI 3 run / 1 skip（非空密码） | 通过 |
| 未永久禁用 / 未静默改 skip | 门控关 skip、开 run；原因仍为门控/语料/jar | 通过 |
| L3 package | `mvn -q -DskipTests package` exit **0** | 通过 |

### 文档影响核对

| 项 | 结果 |
|---|---|
| `dev-notes.md` DEF-PG-003 回执 | 通过（根因、方案 A、验证、建议复测；数值一致） |
| Spec / Design / README / `.env.example` | N/A（方案 A 无合同变更；本提交未改） |

### 安全影响核对

| 检查项 | 结果 | 备注 |
|---|---|---|
| 提交边界 | 通过 | `b6ea61f` 6 文件；无 `.env`/`.env.pg`/`examples/`；`.env` 仍 ignore |
| 凭据 | 通过 | 报告无密码明文；门控 PASSWORD 空；产品不读 `GGTEST_PG_*` |
| 注入面 | 通过 | `Function`/`Path` 仅测试注入；默认入口不变 |

无未解决安全问题。

### Git 合规

| 检查 | 结果 |
|---|---|
| 分支 / 提交 | `ggtest-pg`；`b6ea61f` 相对 `e7e6249`；message `fix(ggtest-pg): isolate runtime .env…` |
| 禁止项 | 通过（无真实 `.env`、无 `examples/`、无构建产物） |
| 本报告 | **不**由 Reviewer 提交 |

### 必修项

| ID | 位置 | 问题 | 状态 |
|---|---|---|---|
| — | — | 无 | — |

### 非阻塞建议

| ID | 严重度 | 位置 | 说明 |
|---|---|---|---|
| N1–N3 | — | — | 轮次 1–2 遗留；本轮未改，仍不阻塞 |
| N4 | low | `EnvConfigIntegrationTest` | Main 级 CWD `.env`（无 `--env-file`）可补；现由 `RuntimeConfigResolverTest` 覆盖 |

### 验证证据摘要（审阅者独立）

| 命令 / 条件 | 结果 |
|---|---|
| 根 `.env` 含 postgres engine + PG URL（本地已有；未改写/未提交） | 污染条件满足 |
| 无 `GGTEST_PG_*`：`mvn -q clean test`（代理 `127.0.0.1:7890`） | **148** / **0** fail / **17** skip |
| 有门控 `GGTEST_PG_URL`/`USER`（PASSWORD 空）：`mvn -q clean test` | **148** / **0** / **5**；PG 实跑 |
| `mvn -q -DskipTests package` | exit **0** |
| `./bin/ggtest --url jdbc:sqlite::memory:` + fixture（根 `.env` 在场） | exit **2**；engine/URL 硬错配 |
| `git show b6ea61f` 文件集 | 无 `.env` / `examples/` |

#### 未验证缺口

| 未验证项 | 原因 | 风险 | 恢复条件 | 复测范围 |
|---|---|---|---|---|
| P1-PG-2 非空密码不回显 | 门控 PASSWORD 空 → skip | 同轮次 2 | 非空 `GGTEST_PG_PASSWORD` | 该用例 |
| 未临时新建后再删 `.env` | 根目录**已有**等价污染文件；未覆盖/删除用户本地 `.env` | 低（条件已满足） | — | — |

### 后续动作

1. Manager：`Approve` → 可调度 **QA 回归**（关闭 DEF-PG-003；本报告 ≠ QA Pass）。
2. QA：根 `.env`（postgres）在场时无/有门控 `mvn -q clean test`；`package`；产品 CLI（冲突→2；无冲突 select1）；ENV `--env-file`。
3. N1–N4 不阻塞。
4. 本 `review.md` 不由 Reviewer 提交。
