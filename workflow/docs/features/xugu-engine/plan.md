# Plan: xugu-engine

## 元信息

- 工作项标识: xugu-engine
- sub-feature-id: xugu-engine（未拆分，用户确认）
- 依据 Spec: workflow/docs/features/xugu-engine/spec.md（v1.0 用户已确认）
- 依据 Design: workflow/docs/features/xugu-engine/design.md
- 依据 UI: N/A（无 UI 变更）
- 路径等级: full
- Review 门禁: required
- 最低验证层: L3（单元 + 集成，集成门控于 `GGTEST_XG_*`）
- 验证命令:
  - 无服务：`mvn test`（xugu 集成用例自动 skip，须 0 失败）
  - 有服务：`GGTEST_XG_URL=jdbc:xugu://127.0.0.1:5138/SYSTEM?char_set=utf8 GGTEST_XG_USER=SYSDBA GGTEST_XG_PASSWORD=SYSDBA mvn test`
  - 打包：`mvn package` → 校验 uber-jar 含驱动 + SPI
- 预期证据: 见「验收与验证」；TDD（先红后绿）

## 适用工程规范

- `workflow/docs/standards/documentation.md`
- `workflow/docs/standards/git.md`
- `workflow/docs/standards/quality.md`
- `workflow/docs/standards/security.md`

## 目标摘要

在 ggtest 中以镜像 PostgreSQL 的方式集成虚谷数据库：新增 `XuguJdbcExecutor` + `XuguSchemaIsolation`，CLI 层 `FileRunner`/`RuntimeConfigResolver` 增加 `xugu`（别名 `xugudb`）分支，驱动经本地 `driver/` file repository 解析、shade 入 uber-jar，配套 `GGTEST_XG_*` 门控测试与 README/help 文档；零下游与零引擎回归。

## 任务拆解（TDD）

1. **T1: 驱动 bootstrap + pom 接入**（完成条件：`mvn -q compile` 能解析 `com.xugudb:xugu-jdbc`；`driver/` 已 gitignore）
   - `mvn install:install-file -DlocalRepositoryPath=driver ...`（用 `~/xgspace/cloudjdbc/target/xugu-jdbc-12.3.9-20260710.jar`）
   - `pom.xml`：`<xugu.jdbc.version>`、`<repository>xugu-local file://.../driver</repository>`、compile `<dependency>com.xugudb:xugu-jdbc`
   - `.gitignore`：`/driver/`
   - 新建 `scripts/install-xugu-driver.sh.example`（可复现 bootstrap 文档）
   - 验证：`mvn -q compile`；`mvn package` 后 `jar tf target/ggtest-*.jar | grep -E 'com/xugu/cloudjdbc/Driver.class'` 与 `META-INF/services/java.sql.Driver` 含 xugu 行

2. **T2: `XuguJdbcExecutor`（TDD）**（完成条件：`XuguJdbcExecutorTest` 绿，门控于 `GGTEST_XG_URL`）
   - 先写测试（红）：engineName=="xugu"；建表+插入+`query` 往返比对；NULL → 空行；拒 SQL = 业务失败（`StatementResult.succeeded()==false`，不抛）；`connection.close()` 后 execute* → `FatalDatabaseException`；executor 不关连接
   - 实现 `src/main/java/com/ggtest/db/xugu/XuguJdbcExecutor.java`（extends `AbstractJdbcExecutor`；`ENGINE_NAME="xugu"`；display `"XuguDB"`；致命标记含中文 `"连接已关闭"`、`"与服务器间的连接已经断开"` + 英文连接关闭串）
   - fixtures 随测试内联（建表 SQL 用 Xugu 兼容语法：`CREATE TABLE t(x int)`/`INSERT`/`SELECT`）
   - 验证：有服务时 `XuguJdbcExecutorTest` 绿

3. **T3: `XuguSchemaIsolation`（TDD）**（完成条件：`XuguSchemaIsolationTest` 绿，门控）
   - 先写测试（红）：prepare 返回 `ggtest_<id>`；两轮 schema 生命周期内同名表互不可见；teardown 后 schema 不再存在；`isSafeIdentifier` 拒绝非安全名（防注入）
   - 实现 `XuguJdbcExecutor` 同包 `XuguSchemaIsolation`：`prepare`（CREATE SCHEMA + `SET SCHEMA`）、`setSearchPath`（`SET SCHEMA`）、`teardown`（`DROP SCHEMA IF EXISTS ... CASCADE`）、`isSafeIdentifier`（`[a-z][a-z0-9_]*`）
   - 验证：有服务时 `XuguSchemaIsolationTest` 绿

4. **T4: `RuntimeConfigResolver` 接入**（完成条件：`RuntimeConfigResolverTest` 新增用例绿，**非门控**）
   - `ENGINE_XUGU="xugu"`；`normalizeEngine` 接受 `xugu`/`xugudb` 归一为 `xugu`，错误消息列 `sqlite / postgres / xugu`
   - `validateEngineUrlPair` 加 `xugu`→`jdbc:xugu:` 前缀校验
   - 先写测试（红）：`--engine xugu` 归一化保留；`--engine xugudb` 归一为 `xugu`；`--engine xugu --url jdbc:sqlite:` → usage error；错误消息含 `xugu`；`GGTEST_XG_*` 不被 resolver 读取（镜像 PG 的 gate-key 用例）
   - 实现，绿

5. **T5: `FileRunner` isXugu 分支**（完成条件：`XuguCliIntegrationTest` 基本 run 绿，门控）
   - `isXugu` 分支镜像 `isPostgres`：默认连接 `XuguSchemaIsolation.prepare`；工厂 `connKey→executor`（默认复用、命名连接开新连接 + `setSearchPath` + `XuguJdbcExecutor`）；`finally` `teardown` + 关全部连接
   - 不改 `ConnectionFactory`
   - 集成测试（门控）：基本 run（pass 文件 exit0 + 报告）、断言失败（exit1 + error block）、NULL 正确、`skipif xugu` 生效、跨文件 schema 隔离（两同名表文件互不干扰）、`--parallel 2` 隔离、`conn=<name>` 多连接、`--halt` 正交
   - fixtures：`src/test/resources/fixtures/xg/{basic,conditions,cross-file/schema-a,cross-file/schema-b,...}.test`
   - **非门控**用例：不可达 `jdbc:xugu://10.255.255.1:5138/SYSTEM` + `--password <secret>` → exit2、stdout/stderr 不含 secret（镜像 PG 的脱敏用例）

6. **T6: `Main.printHelp` + README + uber-jar 验证**（完成条件：help/README 含 xugu；`java -jar target/ggtest-*.jar --engine xugu ...` 对 fixture 端到端 PASS）
   - `Main.printHelp` `--engine` 行：`sqlite | postgres | xugu`（备注 `xugudb`）
   - README：`--engine` 行、引擎/隔离表加 XuguDB、`GGTEST_XG_*` 测试门段、驱动 bootstrap 段、校正「加引擎」表述
   - 手工验证 uber-jar：`mvn package` → `java -jar target/ggtest-*.jar --engine xugu --url jdbc:xugu://127.0.0.1:5138/SYSTEM?char_set=utf8 --user SYSDBA --password SYSDBA <fixture>` → PASS

7. **T7: 全量回归 + 零回归确认**（完成条件：`mvn test` 全绿）
   - 无服务环境：`mvn test`（xugu 门控用例 skip，0 失败）
   - 有服务环境：`mvn test`（xugu 用例亦绿）
   - 确认 sqlite/postgres/顺序/`--parallel`/`--halt` 用例零回归
   - uber-jar 含驱动 + SPI 三行

## 依赖与顺序

```
T1 (driver+pom) ─→ T2 (executor, TDD) ─→ T3 (isolation, TDD)
                                            └─→ T4 (resolver) ─→ T5 (FileRunner+集成, TDD)
                                                                   └─→ T6 (help/README/uber-jar) ─→ T7 (回归)
```

## 触碰路径

| 文件 | 新建/修改 | 改动类型 |
|---|---|---|
| `pom.xml` | 修改 | property + repository + dependency（决策 1） |
| `.gitignore` | 修改 | `/driver/` |
| `scripts/install-xugu-driver.sh.example` | 新建 | bootstrap 文档/脚本 |
| `src/main/java/com/ggtest/db/xugu/XuguJdbcExecutor.java` | 新建 | 决策 2 |
| `src/main/java/com/ggtest/db/xugu/XuguSchemaIsolation.java` | 新建 | 决策 3 |
| `src/main/java/com/ggtest/cli/RuntimeConfigResolver.java` | 修改 | 决策 5 |
| `src/main/java/com/ggtest/cli/FileRunner.java` | 修改 | 决策 4 |
| `src/main/java/com/ggtest/cli/Main.java` | 修改 | printHelp |
| `README.md` | 修改 | 决策 6 |
| `src/test/java/com/ggtest/db/xugu/XuguJdbcExecutorTest.java` | 新建 | T2 |
| `src/test/java/com/ggtest/db/xugu/XuguSchemaIsolationTest.java` | 新建 | T3 |
| `src/test/java/com/ggtest/cli/XuguCliIntegrationTest.java` | 新建 | T5 |
| `src/test/java/com/ggtest/cli/RuntimeConfigResolverTest.java` | 修改 | T4 |
| `src/test/resources/fixtures/xg/*.test` | 新建 | T5 |

## 验收与验证

| ID | 要求 | 命令 / 证据 | 结果（实施后填） |
|---|---|---|---|
| V1 | 驱动可解析且入 uber-jar | `mvn package`；`jar tf target/ggtest-*.jar` 含 `com/xugu/cloudjdbc/Driver.class` 与 SPI 三行 | |
| V2 | executor 单元（门控） | `XuguJdbcExecutorTest` 绿（`GGTEST_XG_URL` 可用时） | |
| V3 | 隔离单元（门控） | `XuguSchemaIsolationTest` 绿 | |
| V4 | resolver（非门控） | `RuntimeConfigResolverTest` xugu/xugudb/mismatch 绿 | |
| V5 | CLI 集成（门控） | `XuguCliIntegrationTest` 绿（基本/失败/NULL/skipif/跨文件/并行/多连接/halt） | |
| V6 | 脱敏（非门控） | 不可达 `jdbc:xugu:` URL + secret → exit2、stdout/stderr 无 secret | |
| V7 | 全量零回归 | `mvn test` 无服务：xugu skip、0 失败；有服务：全绿 | |
| V8 | uber-jar 端到端 | `java -jar target/ggtest-*.jar --engine xugu ...` 对 fixture PASS | |
| V9 | Review 门禁 Approve | `review.md` Approve | |
| V10 | QA Pass | `qa-report.md` Pass | |

### Spec 验收项映射

| Spec | 验证 |
|---|---|
| P0-1 引擎解析/归一化 | V4 |
| P0-2 URL 校验 | V4 |
| P0-3 单文件 E2E | V5/V8 |
| P0-4 断言失败报告 | V5 |
| P0-5 NULL | V2/V5 |
| P0-6 skipif xugu | V5 |
| P0-7 凭据脱敏 | V6 |
| P0-8 零回归 | V7 |
| P1-1 跨文件隔离 | V3/V5 |
| P1-2 并行隔离 | V5 |
| P1-3 多连接 | V5 |
| P1-4 halt 正交 | V5 |
| P2-1 加速 | Q-Note |
| P2-2 驱动文档 | T6/README |

## 验证缺口

| 项 | 原因 | 风险 | 恢复条件 |
|---|---|---|---|
| 无服务环境无法验证 V2/V3/V5/V8 与 Spec P0-3..P0-6 / P1-1..P1-4 | xugu 集成测试门控于 `GGTEST_XG_URL` | 中——核心功能依赖真实服务 | 本机服务可用（已提供 `127.0.0.1:5138/SYSTEM`），QA 阶段在服务可用环境复测 |
| 大语料 wall-clock 加速（P2-1） | 无量化阈值 fixture | 低 | 可选准备大语料后 Q-Note |

## 文档影响

| 类别 | 更新路径 |
|---|---|
| 开发文档 | README：驱动 bootstrap、`driver/` 目录约定、引擎/隔离表、加引擎说明 |
| 用户文档 | README：`--engine xugu`（别名 xugudb）、`GGTEST_XG_*` 测试门变量、用法示例 |
| 运维文档 | N/A — 无部署/排障变更（连接由用户 `--url` 提供；服务运维在 ggtest 之外） |

## Review 门禁与进入 QA 条件

- Review 门禁：`required`（full 路径）。Developer 完成 V1-V8 自测后调度 Reviewer；Approve 方可进 QA。
- 进入 QA 条件：Review Approve + `mvn test`（无服务 0 失败）+ uber-jar 含驱动；有服务环境补跑 V2/V3/V5/V8。

## 交接顺序

1. Developer 实施（TDD，T1→T7）与开发者验证（V1-V8） →
2. Reviewer 审阅（required）→ Approve →
3. QA 验收（V1-V10；服务可用环境复测集成项） → Pass →
4. 用户合并授权 → Manager 源分支置 `done` + 未入库 `review.md`/`qa-report.md` 一次提交 →（目标分支 `xgtest`，已在该分支实现；`xgtest`→`main` 由用户自行决定）

## 修订记录

| 日期 | 摘要 |
|---|---|
| 2026-08-12 | 初稿：基于 Spec v1.0（用户确认）+ Design v1.0，TDD 任务 T1-T7 |
