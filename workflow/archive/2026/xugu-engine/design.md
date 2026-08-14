# Design: xugu-engine

## 背景与约束

- 新增虚谷（XuguDB）引擎。架构事实（见 spec.md + manager 进度笔记）：executor 类极简，方言编排（连接/隔离）在 CLI 层 `FileRunner`；引擎选择是 `RuntimeConfigResolver` + `FileRunner` 的 `if/else`（非注册表）。
- 已实测（Probe/Probe2 对 `127.0.0.1:5138/SYSTEM`）：`CREATE SCHEMA` / `DROP SCHEMA CASCADE` / `schema.table` / `SET SCHEMA <name>` 路由未限定名 / 跨 schema 同名表并存 / `getString`+`wasNull` 对 NULL 正确 / 标识符折大写不敏感 / `SELECT 1` 无 FROM 可用 / autoCommit=true 默认。
- 驱动 `com.xugudb:xugu-jdbc:12.3.9-20260710` SPI 注册（`com.xugu.cloudjdbc.Driver`），运行时**自包含**（实测 `checker-qual` 非运行时依赖）。非 Maven Central；用户指定：jar 存于仓库内 `driver/`、gitignore 不入库。
- 约束：零下游回归（`AbstractJdbcExecutor`/`ConnectionFactory`/runner/parser/normalize 不变）；sqlite/postgres/顺序/`--parallel`/`--halt` 行为不变；uber-jar 须含驱动且 SPI 合并。

## 方案对比与决策

### 决策 1: 驱动 Maven 解析机制——本地 file repository

| 方案 | 概要 | 优点 | 缺点 | 比较依据 |
|---|---|---|---|---|
| A | `driver/` 作本地 Maven 仓库：`mvn install:install-file -DlocalRepositoryPath=driver` 生成 m2 布局；pom 增 `<repository>file://${project.basedir}/driver</repository>` + 普通 compile-scope `<dependency>` | 标准 Maven 路径；**shade 一定包含**（与其它 compile 依赖同路径）；`ServicesResourceTransformer` 自动合并 SPI；可解析传递依赖（本例无） | 需一次性 bootstrap（install:install-file 生成 m2 布局，非"裸 jar") | 可靠性最高 |
| B | system-scoped + `<systemPath>${project.basedir}/driver/xugu-jdbc-*.jar</systemPath>`；"裸 jar"直接放入 `driver/` | 用户操作最简（放 jar 即可） | system-scoped 历史与 shade 兼容性不稳；SPI 合并行为需逐一验证；shade 需额外配置才能确保打入 uber-jar | 风险高，需反复验证 |

**决策:** 选 A。`driver/` 为本地 file repository（gitignore）；bootstrap 一次性命令：

```
mvn install:install-file \
  -Dfile=<path>/xugu-jdbc-12.3.9-20260710.jar \
  -DgroupId=com.xugudb -DartifactId=xugu-jdbc -Dversion=12.3.9-20260710 \
  -Dpackaging=jar -DgeneratePom=true -DlocalRepositoryPath=driver
```

pom 变更：
- `<properties>` 加 `<xugu.jdbc.version>12.3.9-20260710</xugu.jdbc.version>`。
- `<repositories>` 加 `<repository><id>xugu-local</id><url>file://${project.basedir}/driver</url></repository>`。
- `<dependencies>` 加 compile-scope `com.xugudb:xugu-jdbc:${xugu.jdbc.version}`。
- shade 插件**无需改动**（`ServicesResourceTransformer` 处理所有 compile 依赖的 SPI）。
- `.gitignore` 加 `/driver/`。
- README + `scripts/install-xugu-driver.sh`（或 `.example`）记录 bootstrap；CI/新克隆须先跑 bootstrap 才能 `mvn package`。

> Developer 须在 TDD 中验证：`mvn package` 产出的 `target/ggtest-*.jar` 含 `com/xugu/cloudjdbc/Driver.class` 且 `META-INF/services/java.sql.Driver` 含三行（sqlite+postgres+xugu）。

### 决策 2: XuguJdbcExecutor——镜像 PostgresJdbcExecutor

| 方案 | 概要 | 比较依据 |
|---|---|---|
| A | `final class XuguJdbcExecutor extends AbstractJdbcExecutor`；构造 `(Connection)` 调 `super(connection, FATAL_MESSAGE_MARKERS, "XuguDB")`；`engineName()="xugu"`；不覆盖 execute* | 与 sqlite/postgres 完全同构；零方言改写 |
| B | 覆盖 execute* 做方言处理 | 无必要（SQL 原样下发） |

**决策:** 选 A。致命消息标记初值（含中文，源自 `ErrorCode.txt`）：

```
"connection is closed", "connection has been closed", "connection closed",
"连接已关闭", "与服务器间的连接已经断开"
```

> `AbstractJdbcExecutor` 的主探测（`SQLState` `08*` + `connection.isClosed()`）已覆盖多数连接级致命情形；标记为补充。Developer 实测核定后可增删。

### 决策 3: XuguSchemaIsolation——镜像 PostgresSchemaIsolation，用 SET SCHEMA

| 方案 | 概要 | 比较依据 |
|---|---|---|
| A | 静态工具类；`prepare`=CREATE SCHEMA + SET SCHEMA；`setSearchPath`=SET SCHEMA；`teardown`=DROP SCHEMA CASCADE；`isSafeIdentifier` 同 PG | 实测有效；与 PG 同构 |
| B | 每文件独立 DATABASE | 更重；CREATE DATABASE 通常不属会话级隔离，且权限/资源开销大 |

**决策:** 选 A。

| 方法 | SQL |
|---|---|
| `String prepare(Connection c)` | `String name = "ggtest_" + uuid(no dashes);` `CREATE SCHEMA <name>;` `SET SCHEMA <name>;` 返回 name |
| `void setSearchPath(Connection c, String schema)` | `SET SCHEMA <schema>`（命名连接指向同 schema） |
| `void teardown(Connection c, String schema)` | `isSafeIdentifier` 校验；`DROP SCHEMA IF EXISTS <name> CASCADE` |
| `boolean isSafeIdentifier(String)` | `[a-z][a-z0-9_]*`（同 PG） |

> 不需 PG 的 `, pg_catalog` 回退：实测 `SET SCHEMA` 后系统目录（`ALL_TABLES`/DUAL/内置函数）仍可达。

### 决策 4: FileRunner——新增 isXugu 分支，镜像 isPostgres

| 方案 | 概要 | 比较依据 |
|---|---|---|
| A | `boolean isXugu = ENGINE_XUGU.equals(options.engine());`；与 `isPostgres` 平行的 prepare/factory/teardown | 最小改动；结构对称 |
| B | 抽象「需要隔离的引擎」基类/策略 | 范围蔓延（spec 非目标） |

**决策:** 选 A。`FileRunner` 关键点（行号据当前 main）：
- `:61` 增 `isXugu`；`isPostgres || isXugu` 触发「默认连接 + prepare」块；按引擎分别用 `PostgresSchemaIsolation` / `XuguSchemaIsolation`。
- 工厂函数（`:88-104`）：`isPostgres && ""==connKey` → 复用默认；`isXugu && ""==connKey` → 复用默认；其余开新连接，按引擎 `setSearchPath` 后返回 `XuguJdbcExecutor`/`PostgresJdbcExecutor`；`else`（sqlite）→ `SqliteJdbcExecutor`。
- `finally`（`:118-132`）：`isPostgres`/`isXugu` 默认连接 `teardown`；统一关闭全部连接。
- `ConnectionFactory.open` 无需改动（`DriverManager` 经 SPI 识别 `jdbc:xugu:`）。

### 决策 5: RuntimeConfigResolver——别名归一化 + URL 校验

- 新增 `public static final String ENGINE_XUGU = "xugu"`。
- `normalizeEngine`：`strip().toLowerCase(ROOT)`；接受 `xugu` 与 `xugudb`，**二者均返回 `"xugu"`**；其余 → `UsageException`（错误消息更新允许值清单为 `sqlite / postgres / xugu`）。
- `validateEngineUrlPair`：`xugu` 分支要求 URL 以 `jdbc:xugu:` 开头，否则 `UsageException`。
- 默认引擎仍 `sqlite`（`CliArgumentParser.DEFAULT_ENGINE`）。

### 决策 6: 用户可见串与文档

- `Main.printHelp` 的 `--engine` 行：`sqlite | postgres | xugu`（备注 `xugudb` 别名）。
- README：`--engine` 表行、引擎/隔离表加 XuguDB 行、`GGTEST_XG_*` 测试门变量段、加引擎章节、校正「实现 `DatabaseExecutor` 即可加引擎」表述（CLI 层亦需改）、驱动 bootstrap 说明。

### 决策 7: 测试策略——镜像 PG，门变量 `GGTEST_XG_*`

| 测试类 | 门 | 内容 |
|---|---|---|
| `XuguJdbcExecutorTest`（db/xugu） | `GGTEST_XG_URL` | 镜像 `PostgresJdbcExecutorTest`：engineName、statement/query 往返、NULL、行序/列数、拒SQL=业务失败、closed conn=FatalDatabaseException、不关连接 |
| `XuguSchemaIsolationTest`（db/xugu） | `GGTEST_XG_URL` | 镜像 `PostgresSchemaIsolationTest`：两 schema 生命周期对象互不可见 |
| `XuguCliIntegrationTest`（cli） | `GGTEST_XG_URL` | 镜像 `PostgresCliIntegrationTest`：基本 run、skipif xugu、跨文件隔离、`--parallel` 隔离、真实脱敏；+ 一条**非门**不可达 `jdbc:xugu:` URL 的脱敏+exit2 用例 |
| `RuntimeConfigResolverTest`（cli） | 无 | `xugu`/`xugudb` 归一化、URL 匹配/失配、错误消息含 `xugu` |

fixtures：`src/test/resources/fixtures/xg/`（basic、conditions、cross-file/schema-a/b 等）。

`assumeXg()` 模式：`assumeTrue(nonBlank(System.getenv("GGTEST_XG_URL")))`；`GGTEST_XG_USER`/`GGTEST_XG_PASSWORD` 可选。**不**入 `DotEnvLoader` 白名单。

## 模块边界与分层

```
CliArgumentParser (不变，--engine 透传)
→ RuntimeConfigResolver (xugu/xugudb 归一化 + URL 校验)
→ CliOptions (engine 字段不变)
→ CliSession → FileRunner
   ├─ isPostgres 分支 (不变)
   ├─ isXugu 分支 (新)
   │    ├─ XuguSchemaIsolation (新, db/xugu)
   │    └─ XuguJdbcExecutor (新, db/xugu) → AbstractJdbcExecutor (不变)
   └─ sqlite 分支 (不变)
pom: xugu-local file repository + com.xugudb:xugu-jdbc (compile)
```

**依赖方向：** `cli → db.xugu`（新）；`db.xugu → db`（AbstractJdbcExecutor）；`db.xugu` 仅依赖 `java.sql` + JDK。`ConnectionFactory`/`runner`/`parser`/`normalize` 零变更。

## 模块影响

| 模块 | 变更 | 说明 |
|---|---|---|
| `db/xugu/XuguJdbcExecutor.java` | 新建 | 决策 2 |
| `db/xugu/XuguSchemaIsolation.java` | 新建 | 决策 3 |
| `cli/RuntimeConfigResolver.java` | 修改 | 决策 5 |
| `cli/FileRunner.java` | 修改 | 决策 4 |
| `cli/Main.java` | 修改 | 决策 6（printHelp） |
| `pom.xml` | 修改 | 决策 1（property + repository + dependency） |
| `.gitignore` | 修改 | `/driver/` |
| `README.md` | 修改 | 决策 6 |
| `scripts/install-xugu-driver.sh`（或 .example） | 新建 | 决策 1 bootstrap |
| `test/db/xugu/XuguJdbcExecutorTest.java` | 新建 | 决策 7 |
| `test/db/xugu/XuguSchemaIsolationTest.java` | 新建 | 决策 7 |
| `test/cli/XuguCliIntegrationTest.java` | 新建 | 决策 7 |
| `test/cli/RuntimeConfigResolverTest.java` | 修改 | 决策 7（xugu/xugudb/mismatch） |
| `test/resources/fixtures/xg/*` | 新建 | 决策 7 |

**明确不变：** `AbstractJdbcExecutor`、`DatabaseExecutor`、`ConnectionFactory`、`SqlLogicTestRunner`、`SqlLogicTestParser`、`StatementResult`/`QueryResult`/`FatalDatabaseException`、`normalize/*`、`model/*`、`CliSession`、`ParallelExecutor`、`DotEnvLoader`、sqlite/postgres 执行器与隔离、`CliOptions`/`ParsedArguments`/`CliArgumentParser`。

## 风险

| 风险 | 影响 | 缓解 |
|---|---|---|
| `driver/` 未 bootstrap → `mvn package` 失败 | 新克隆/CI 构建失败 | README + 脚本明确前置步骤；`driver/` gitignore 但提供 `install-xugu-driver.sh.example`；README「构建」段标注 |
| shade 未正确合并 xugu SPI → uber-jar 找不到驱动 | `java -jar ggtest.jar --engine xugu` 报「无合适驱动」 | Developer TDD 验证：uber-jar 内 `META-INF/services/java.sql.Driver` 三行 + `Driver.class` 存在 |
| Xugu 行为与 sqllogictest 隐含假设不符（类型/排序/NULL 表示） | 部分语料比对失败 | spec 已界定 executor 仅取 `getString`+`wasNull`，I/T/R 归一化由 harness；fixtures 先用最小可控样例，真实大语料作 Q-Note |
| 致命标记集不全 | 连接断开未被识别为 fatal、未中止文件 | `AbstractJdbcExecutor` 主探测（SQLState 08* / isClosed）兜底；标记据 ErrorCode.txt + 实测补充 |
| 标识符折大写与部分大小写敏感语料冲突 | 极少数语料失败 | 非 regression（Xugu 固有）；spec 已声明非目标；Q-Note |
| 服务不可用 → xugu 集成测试全 skip | CI 无覆盖 | 门变量 assumeTrue；`mvn test` 在无服务环境仍 0 失败（自动 skip） |

## 对 Plan 与 Developer 的要点

### Plan

- 任务拆解围绕「先能让构建识别驱动 → executor+隔离 → CLI 接线 → 测试 → 文档」展开；TDD（先红后绿）。
- 触碰路径见模块影响表。
- 最低验证层 L3：单元（executor/隔离/resolver）+ 集成（CLI 端到端，门控）。
- 预期证据：`mvn test`（无服务全 skip、0 失败）；有服务时 `Xugu*Test` 通过；`mvn package` uber-jar 含驱动；`java -jar ... --engine xugu` 对 fixture 端到端 PASS。
- 无服务时无法验证 P0-3..P0-6/P1-1..P1-4 → 登记为验证缺口（恢复条件：`GGTEST_XG_URL` 可用），不阻塞 Review/Plan，QA 复测时在有服务环境跑。

### Developer

- 按 TDD：先写 `XuguJdbcExecutorTest`（红）→ 实现 executor（绿）；隔离、CLI 接线、resolver 同理。
- bootstrap 先行：把驱动 jar 经 `install:install-file -DlocalRepositoryPath=driver` 放入 `driver/`，`mvn -q compile` 通过。
- 引擎选择 `if/else` 扩展，不引入注册表。
- `SET SCHEMA` / `DROP SCHEMA CASCADE` 经实测可用；如某 Xugu 版本语法差异，fallback `ALTER SESSION SET CURRENT_SCHEMA =`。
- 凭据脱敏走既有 `FileOutcome.detailLines()` + `CredentialRedaction`；新增不可达 URL 脱敏用例须**非门**（始终跑）。
- 不改 `ConnectionFactory`；`--engine xugu` 的连接经 `DriverManager` SPI 自动选择驱动。
