# Dev Notes: xugu-engine

## 实现摘要（TDD T1-T7 完成）

| 任务 | 产物 | 结果 |
|---|---|---|
| T1 驱动 bootstrap + pom | `driver/`（本地 file repo，gitignore）、`pom.xml`（property + `<repository>xugu-local</repository>` + compile dep）、`.gitignore`、`scripts/install-xugu-driver.sh.example` | `mvn package` uber-jar 含 `com/xugu/cloudjdbc/Driver.class` + SPI 三行 |
| T2 `XuguJdbcExecutor` | `db/xugu/XuguJdbcExecutor.java` + `XuguJdbcExecutorTest`（8 用例，门控） | 8/8 |
| T3 `XuguSchemaIsolation` | `db/xugu/XuguSchemaIsolation.java` + `XuguSchemaIsolationTest`（1 用例，门控） | 1/1 |
| T4 `RuntimeConfigResolver` | `ENGINE_XUGU`/`xugudb` 归一化 + `jdbc:xugu:` 校验 + 5 个 resolver 测试 | 39/39（含新增） |
| T5 `FileRunner` isXugu + CLI 集成 | `FileRunner`（`needsIsolation = isPostgres \|\| isXugu` 统一路径）+ `XuguCliIntegrationTest`（10 用例）+ `fixtures/xg/*` | 10/10 |
| T6 `Main.printHelp` + README | help/README 加 xugu | done |
| T7 全量回归 + 零回归 | `mvn test` | 无服务 367/0/35skip；有服务 367/0/17skip |

## 实测发现的虚谷方言要点（偏离/补充 Design）

1. **`DROP SCHEMA` 不支持 `IF EXISTS`**（与 `DROP TABLE` 不同；`DROP TABLE IF EXISTS` 可用）。`XuguSchemaIsolation.teardown` 用 `DROP SCHEMA <name> CASCADE`（无 `IF EXISTS`）。安全性由 `prepare` 总是先创建保证（生命周期内 schema 必然存在）；`isSafeIdentifier` 防 teardown 注入不变。
2. **`checker-qual` 非运行时依赖**：实测 `java -cp xugu-jdbc.jar Probe`（无 checker-qual）正常加载连接。故 pom 仅声明 `com.xugudb:xugu-jdbc`（compile），不需显式拉 checker-qual（其注解为 CLASS 保留）。
3. **`SET SCHEMA <name>` 与 `ALTER SESSION SET CURRENT_SCHEMA = <name>` 均生效**；取 `SET SCHEMA`（SQL 标准、最简）。`SET SCHEMA` 后系统目录（`ALL_TABLES`/DUAL/内置函数）仍可达，无需 PG 的 `, pg_catalog` 回退。
4. **标识符折大写、大小写不敏感**（Oracle 式）；同文件 create/query 标识符一致即可正确比对。
5. **NULL**：`getString` 返回 null + `wasNull()==true` → `AbstractJdbcExecutor.readRows` 产出 `null` 元素（与 sqlite/pg 一致）。
6. **URL**：`jdbc:xugu://host:port/database[?...]`，默认端口 5138；协议仅 `jdbc:xugu:`（驱动 `ReplaceEnum.conStrProName="xugu"`，不支持 `jdbc:xugudb:`）。
7. **autoCommit=true** 默认（与 executor 契约「连接拥有权/提交由 caller 决定」一致；harness 不显式管理事务）。

## 驱动集成机制（Design 决策 1 落地）

- `driver/` 为本地 Maven file repository（`<repository>xugu-local</repository> file://${project.basedir}/driver`），`/driver/` 入 `.gitignore`。
- bootstrap：`scripts/install-xugu-driver.sh.example <jar>`（封装 `mvn install:install-file -DlocalRepositoryPath=driver`）。
- `com.xugudb:xugu-jdbc:12.3.9-20260710` 为 compile-scope，shade `ServicesResourceTransformer` 自动合并其 SPI。
- **验证（V1/V8）**：`jar tf target/ggtest-*.jar` 含 `com/xugu/cloudjdbc/Driver.class`；`META-INF/services/java.sql.Driver` 三行（`org.sqlite.JDBC`、`org.postgresql.Driver`、`com.xugu.cloudjdbc.Driver`）；`java -jar ... --engine xugu` 对 live 服务端到端 PASS。

## 致命消息标记（决策 2 落地）

`XuguJdbcExecutor.FATAL_MESSAGE_MARKERS`（含中文，源自 `~/xgspace/cloudjdbc/ErrorCode.txt`）：
`"connection closed"`、`"connection is closed"`、`"connection has been closed"`、`"连接已关闭"`(E50020)、`"与服务器间的连接已经断开"`(E50022)。`AbstractJdbcExecutor` 主探测（`SQLState 08*` + `connection.isClosed()`）兜底。

## 引擎选择抽象（决策 4/5 落地）

- `FileRunner`：`needsIsolation = isPostgres || isXugu` 统一「默认连接 + prepare」块；prepare/teardown/setSearchPath 按 `isPostgres` 分派到 `PostgresSchemaIsolation`/`XuguSchemaIsolation`；工厂默认连接复用、命名连接开新连接 + `setSearchPath`。
- `RuntimeConfigResolver`：`ENGINE_XUGU="xugu"`；`normalizeEngine` 把 `xugudb` 归一为 `xugu`；允许值清单 `sqlite / postgres / xugu`；`validateEngineUrlPair` 加 `jdbc:xugu:` 前缀校验。
- 未引入引擎注册表（`if/else` 扩展，符合 spec 非目标）。

## 抽象守卫测试更新

`RunnerDependencyIsolationTest`：`executorAbstractionStaysFreeOfJdbc` 增加 `xugu` 子包豁免（与 sqlite/postgres 同列）；两个测试的禁引清单补 `com.ggtest.db.xugu`、`com.xugu`（driver 包），保持「runner/db-非引擎子包 不直接依赖具体引擎或驱动」不变量。

## 触碰文件

新建：`db/xugu/{XuguJdbcExecutor,XuguSchemaIsolation}.java`、`test/db/xugu/{XuguJdbcExecutorTest,XuguSchemaIsolationTest}.java`、`test/cli/XuguCliIntegrationTest.java`、`test/resources/fixtures/xg/{basic,fail,conditions,multi-conn}.test` + `cross-file/{schema-a,schema-b}.test`、`scripts/install-xugu-driver.sh.example`。
修改：`pom.xml`、`.gitignore`、`cli/{RuntimeConfigResolver,FileRunner,Main}.java`、`test/cli/RuntimeConfigResolverTest.java`、`test/runner/RunnerDependencyIsolationTest.java`、`README.md`。
明确不变：`AbstractJdbcExecutor`/`DatabaseExecutor`/`ConnectionFactory`/`SqlLogicTestRunner`/parser/normalize/model/`CliSession`/`ParallelExecutor`/`DotEnvLoader`/sqlite/postgres 执行器与隔离。

## 开发者验证（V1-V8）

- V1 驱动解析 + uber-jar 含驱动 + SPI 三行：通过
- V2 `XuguJdbcExecutorTest` 8/8（live 服务）：通过
- V3 `XuguSchemaIsolationTest` 1/1：通过
- V4 `RuntimeConfigResolverTest` 39/39（含 5 新增 xugu，非门控）：通过
- V5 `XuguCliIntegrationTest` 10/10（live 服务）：通过
- V6 非门控不可达 URL 脱敏 → exit2：通过
- V7 `mvn test` 无服务 367/0/35skip、有服务 367/0/17skip：通过
- V8 `java -jar target/ggtest-*.jar --engine xugu ... basic.test` → `[PASSED]` exit0：通过
