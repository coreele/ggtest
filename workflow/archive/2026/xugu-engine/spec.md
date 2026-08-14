# Spec: xugu-engine

> 状态: v1.0（**用户已确认 2026-08-12**：单切片；驱动 jar 存于仓库内 `driver/` 目录、gitignore 不入库）。路径等级 `full`，Spec 门禁 required + 用户确认 required（已通过）。

## 背景

ggtest 当前支持 `sqlite`（默认）与 `postgres` 两个引擎，二者均经 `AbstractJdbcExecutor` 复用 JDBC 执行逻辑，方言编排（连接/隔离）在 CLI 层 `FileRunner`。本切片新增虚谷数据库（XuguDB）引擎，使 `ggtest --engine xugu` 能对运行中的虚谷服务执行 sqllogictest 文件、按真实结果比对、聚合报告，并与现有 `--halt` / `--parallel` / `conn=<name>` / 凭据脱敏正交。

## 约束

- 驱动：`com.xugudb:xugu-jdbc:12.3.9-20260710`（Java 8 JDBC，类 `com.xugu.cloudjdbc.Driver`，经 `META-INF/services/java.sql.Driver` SPI 注册；非 Maven Central，需本地安装——**机制属 Design 决策**）。运行时依赖 `org.checkerframework:checker-qual`。
- URL：`jdbc:xugu://host:port/database[?...]`，默认端口 5138；协议字符串仅 `jdbc:xugu:`。
- 不改 `AbstractJdbcExecutor` / `ConnectionFactory` / `SqlLogicTestRunner` / 解析器 / normalize（零下游回归）。
- 顺序与 `--parallel`、`--halt`、凭据脱敏、`skipif`/`onlyif`、`conn=<name>` 等既有合同不变。
- 驱动为专有软件；构建须可解析该 artifact（Design 决定 `install:install-file` / system-scoped + 入库 jar / 本地仓库 何者）。

## 合同

### API / 接口

**引擎取值（CLI）：**

- `--engine xugu`（规范值）与 `--engine xugudb`（别名）均接受；二者在 `RuntimeConfigResolver.normalizeEngine` 中**归一化为 `"xugu"`**。
- 解析优先级不变：CLI `--engine` > 进程环境 `GGTEST_ENGINE` > `.env` `GGTEST_ENGINE` > 默认 `sqlite`。
- `RuntimeConfigResolver` 新增常量 `public static final String ENGINE_XUGU = "xugu"`；允许值集合扩为 `{sqlite, postgres, xugu}`（`xugudb` 归一化后并入）；未知值仍 → `UsageException`，退出码 2，stderr 含 `[WHY]`。
- `validateEngineUrlPair`：`xugu` → URL 须以 `jdbc:xugu:` 开头，否则 `UsageException`，退出码 2。
- `Main.printHelp` 的 `--engine` 行与 README 选项表列出 `xugu`（与 `xugudb` 别名说明）。

**执行器（`com.ggtest.db.xugu.XuguJdbcExecutor`）：**

- `public final class XuguJdbcExecutor extends AbstractJdbcExecutor`；构造 `XuguJdbcExecutor(Connection)`。
- `engineName()` 返回 `"xugu"`（用于 `skipif`/`onlyif` 大小写不敏感匹配）。
- 展示名 `"XuguDB"`；致命消息标记集合（连接级错误）——初始集至少含 `"connection is closed"`、`"connection has been closed"`、`"connection closed"`（最终清单由 Developer 据 `~/xgspace/cloudjdbc/ErrorCode.txt` 与实测核定）。
- `executeStatement` / `executeQuery` / 超时重载、`readRows`、`wasNull`→`null`、致命分类全部继承 `AbstractJdbcExecutor`，不覆盖。
- 连接拥有权属 caller（构造只接收 `Connection`，不开/不关）；SQL 经 `statement.execute(sql)` / `executeQuery(sql)` **原样下发**，不做方言改写。

**隔离（`com.ggtest.db.xugu.XuguSchemaIsolation`）：**

镜像 `PostgresSchemaIsolation`（静态工具、私有构造、非 `DatabaseExecutor` 的一部分；CLI 编排 prepare→run→teardown）：

| 方法 | 行为 |
|---|---|
| `String prepare(Connection)` | 生成 `"ggtest_" + UUID（无连字符）`；`CREATE SCHEMA <name>`；`SET SCHEMA <name>`；返回 schema 名 |
| `void setSearchPath(Connection, String schema)` | `SET SCHEMA <schema>`——用于 `conn=<name>` 多连接，使各命名连接指向同一按文件 schema |
| `void teardown(Connection, String schema)` | `isSafeIdentifier` 校验后 `DROP SCHEMA IF EXISTS <name> CASCADE` |
| `boolean isSafeIdentifier(String)` | 同 PG：`[a-z][a-z0-9_]*`，防注入 |

> 必要性：虚谷为共享服务端 DB；无按文件隔离则跨文件/`--parallel` 下同名表（如 `t1`）会冲突。实测虚谷支持 `CREATE SCHEMA` / `DROP SCHEMA CASCADE` / `schema.table` / `SET SCHEMA` 路由未限定名 / 跨 schema 同名表并存。

**CLI 编排（`FileRunner`）：**

新增 `isXugu` 分支，镜像 `isPostgres`：

1. 打开默认（`""`）连接 → `XuguSchemaIsolation.prepare` 得按文件 schema；失败 → `FileOutcome.hardFailure`。
2. 工厂函数 `connKey → executor`：`connKey==""` 复用默认连接的 executor；其余 `connKey` 新开连接、`setSearchPath` 指向同 schema、返回 `XuguJdbcExecutor`。
3. `finally`：`teardown` 默认连接 schema；关闭 connections map 中全部连接。

**`skipif` / `onlyif`：** 默认连接 executor 的 `engineName()`（`"xugu"`）与记录操作数大小写不敏感比较；`skipif xugu` 与 `skipif xugudb` 是否都生效，取决于归一化点——**Decision D1 取「`xugudb` 在 CLI 归一化为 `xugu`，故 fixtures/`skipif` 统一用 `xugu`；`skipif xugudb` 不作额外匹配」**（避免双名歧义）。

### 数据 / 状态

- `NULL`：虚谷 `getString` 对 NULL 返回 `null` 且 `wasNull()==true` → `AbstractJdbcExecutor.readRows` 产出 `null` 元素（与 sqlite/pg 一致）。I/T/R 归一化仍由 harness 侧 `ValueNormalizer` 负责，executor 不参与。
- 标识符：虚谷折大写、大小写不敏感；同文件内 create/query 标识符一致即正确比对，不作大小写改写。
- 类型：executor 仅经 `getString` 取字符串值；类型差异由 normalize 层吸收（与现有一致）。
- 聚合报告格式（status line / error block / Error section / TOTAL）与退出码优先级（hardError→2；failed>0→1；else 0）不变。

### 错误与约束

- 被拒 SQL（业务失败）→ 结果对象返回，runner 继续；连接级致命（`SQLState` `08*` / `connection.isClosed()` / 致命标记）→ 抛 `FatalDatabaseException`，中止当前文件、保留已产结果（沿用 `AbstractJdbcExecutor` 既有分类）。
- `--engine xugu` 与 URL 不以 `jdbc:xugu:` 开头 → usage error，退出码 2。
- `--engine xugudb` 等价 `--engine xugu`（归一化）。
- `--parallel` 与 `--override` 互斥（既有，不变）；`--engine xugu` 与 `--override` 可共存（override 写回仍走 `FileRunner` 现有路径，但**并行+override 互斥**不变）。
- 测试门变量 `GGTEST_XG_URL`（必需）、`GGTEST_XG_USER` / `GGTEST_XG_PASSWORD`（可选）**仅用于测试 `assumeTrue` 门**，**不得**加入 `DotEnvLoader` 运行时白名单，不被 `RuntimeConfigResolver` 读取（镜像 `GGTEST_PG_*` 「门变量、非运行时配置」约定）。

## 验收（Given-When-Then）

### P0

- **P0-1 引擎值解析与归一化**
  - Given `--engine xugu --url jdbc:xugu://...` 或 `--engine xugudb --url jdbc:xugu://...`
  - When 执行
  - Then `RuntimeConfigResolver` 归一化引擎为 `xugu`；`CliOptions.engine()=="xugu"`；不报 usage error

- **P0-2 URL 前缀校验**
  - Given `--engine xugu --url jdbc:sqlite::memory:`
  - When 执行
  - Then usage error，退出码 2，stderr 含 `[WHY]` 与说明

- **P0-3 单文件端到端（需服务可用）**
  - Given 一个简单 pass 文件（建表+插入+`query I`）与运行中的虚谷服务
  - When `ggtest --engine xugu --url jdbc:xugu://127.0.0.1:5138/SYSTEM?char_set=utf8 --user SYSDBA --password SYSDBA <file>`
  - Then 文件报告 `[PASSED]`；退出码 0；TOTAL 正确

- **P0-4 断言失败报告（需服务可用）**
  - Given 一个含错误期望的 `query` 文件
  - When 同 P0-3 执行
  - Then 文件报告 `[FAILED]` + error block（`at <file>:<line> : <why>` + diff）；退出码 1

- **P0-5 NULL 正确传递（需服务可用）**
  - Given `INSERT ... VALUES (NULL)` + `query I` 期望空
  - When 执行
  - Then 正确比对（NULL 行与期望空行一致），`[PASSED]`

- **P0-6 `skipif`/`onlyif` 以 `xugu` 匹配（需服务可用）**
  - Given 含 `skipif xugu` 的文件（其余记录应被跳过）
  - When `--engine xugu` 执行
  - Then 该文件相关记录 `SKIPPED`，文件报告符合 skip 语义

- **P0-7 凭据脱敏（不依赖服务可达）**
  - Given `--engine xugu --password <secret> --url jdbc:xugu://10.255.255.1:5138/SYSTEM`（不可达）与会失败的文件
  - When 执行
  - Then stdout 与 stderr 均不含 `<secret>`；连接失败被包装为 hardError

- **P0-8 零回归**
  - Given 现有 sqlite/postgres/顺序/`--parallel`/`--halt` 用例
  - When `mvn test`
  - Then 全量通过（xugu 相关集成用例无服务时自动 skip）

### P1

- **P1-1 跨文件 schema 隔离（需服务可用）**
  - Given 两文件均 `CREATE TABLE t1(...)` 不同数据
  - When `--engine xugu` 顺序执行两文件
  - Then 两文件互不干扰，各自独立 PASS；无「对象已存在」冲突

- **P1-2 `--parallel` 隔离（需服务可用）**
  - Given 两同名表文件 + `--parallel 2`
  - When 执行
  - Then 并发不冲突；TOTAL 含两文件真实结果；退出码按结果

- **P1-3 `conn=<name>` 多连接（需服务可用）**
  - Given 使用 `conn=a`/`conn=b` 分别建表查询的文件
  - When `--engine xugu` 执行
  - Then 各命名连接独立、指向同 schema，查询正确

- **P1-4 `--halt` 正交（需服务可用）**
  - Given 多文件、首文件失败、`--engine xugu --halt`
  - When 执行
  - Then 后续文件不被分派（既有并行 halt 语义对 xugu 同样生效）

### P2（Nice-to-have，不阻塞 Pass）

- **P2-1 大语料 wall-clock 加速**：≥10 文件、`--parallel <N≥2>` wall-clock < 顺序（Q-Note，无量化阈值）。
- **P2-2 驱动集成方式文档化**：README 说明如何安装 `com.xugudb:xugu-jdbc` 到本地 m2。

## 决策记录

1. **规范引擎名 `xugu`，别名 `xugudb`。** CLI 接受二者并归一化为 `xugu`；URL 仅 `jdbc:xugu:`（驱动 `conStrProName="xugu"` 不支持 `jdbc:xugudb:`）；`skipif`/`onlyif` 统一用 `xugu`，不对 `xugudb` 额外匹配（避免双名歧义，fixture 一致性优先）。
2. **隔离机制用 `SET SCHEMA`。** 实测 `SET SCHEMA <name>` 与 `ALTER SESSION SET CURRENT_SCHEMA = <name>` 均生效；取 `SET SCHEMA`（SQL 标准、最简）。teardown 用 `DROP SCHEMA CASCADE`。
3. **驱动 artifact 存于仓库内 `driver/` 目录，不纳入 git 跟踪（用户确认）。** `driver/` 加入 `.gitignore`；具体 Maven 解析机制（local file repository / system-scoped + systemPath）由 Design 决定，**硬约束**：①构建期可解析；②运行期经 SPI 被 `DriverManager` 发现；③`maven-shade-plugin` 产出 uber-jar 必须含该驱动；④README 记录一次性 bootstrap（把 `~/xgspace/cloudjdbc/target/xugu-jdbc-*.jar` 放入 `driver/`）。`checker-qual` 由 m2 正常解析。
4. **不重写引擎选择为注册表。** 维持 `FileRunner`/`RuntimeConfigResolver` 的 `if/else` 扩展（与 sqlite/postgres 一致；引擎注册表抽象非本切片目标，避免范围蔓延）。README「实现 `DatabaseExecutor` 即可加引擎」的不准确表述在文档影响中校正。
5. **切片策略：未拆分单切片 `xugu-engine`（full）（用户确认）。** 引擎需含隔离才可用；与 PG 集成模式一致；一次可审阅。

## 非目标

- 不做 Xugu 方言改写（SQL 原样下发，与 sqlite/pg 一致）。
- 不引入引擎注册表/插件抽象。
- 不做性能基准量化（P2-1 仅 Q-Note）。
- 不覆盖 `AbstractJdbcExecutor` 既有行为。
- 不改变 sqlite/postgres 任何既有行为。
