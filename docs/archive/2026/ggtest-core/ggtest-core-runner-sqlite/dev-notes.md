# Dev Notes: ggtest-core / runner-sqlite

## 实现说明

- 工作项: `ggtest-core` / `runner-sqlite`；路径 full；Design/Review 门禁 required；源分支 `ggtest-core-runner-sqlite`（目标 `main`）
- Plan T1–T5 已完成:
  - T1 执行器抽象：`DatabaseExecutor`（`engineName` / `executeStatement` / `executeQuery`）、`StatementResult`、`QueryResult`、`FatalDatabaseException`（非受检）
  - T2 Runner 状态机：skipif/onlyif（大小写不敏感、可叠加、作用一条记录后清空）、hash-threshold、halt→skipped、statement ok/error、query 经 `ResultComparer` 比对、label 一致性、致命异常中止；假执行器驱动
  - T3 SQLite 适配：`pom.xml` 增 `org.xerial:sqlite-jdbc` **3.53.2.0**（compile）；`SqliteJdbcExecutor` 持有调用方 `Connection`，`getString` + `wasNull()`→`null`
  - T4 fixtures（6 个 `.test`）+ 验收测试（parser → Runner → SQLite `jdbc:sqlite::memory:`）+ P0-8 源码依赖检查
  - T5 `README.md` 增「Runner usage」「Supporting another database」；公共类型 Javadoc；本文件
- 变更路径:
  - `src/main/java/com/ggtest/db/{DatabaseExecutor,StatementResult,QueryResult,FatalDatabaseException}.java`
  - `src/main/java/com/ggtest/db/sqlite/SqliteJdbcExecutor.java`
  - `src/main/java/com/ggtest/runner/{SqlLogicTestRunner,FileRunResult,RecordResult,RecordOutcome}.java`
  - `src/test/java/com/ggtest/db/ExecutorResultTypesTest.java`、`src/test/java/com/ggtest/db/sqlite/SqliteJdbcExecutorTest.java`
  - `src/test/java/com/ggtest/runner/{SqlLogicTestRunnerTest,FakeDatabaseExecutor,RunnerAcceptanceTest,RunnerDependencyIsolationTest}.java`
  - `src/test/resources/fixtures/runner/*.test`
  - `pom.xml`（sqlite-jdbc + description）、`README.md`、本文件
- 实现取舍（均在 Plan/Spec 范围内，供 Reviewer 核对）:
  - `FileRunResult.recordResults` 只含 statement/query；指令记录只改状态、不产出判定，使 pass/fail/skipped 即测试单元计数
  - 致命异常：触发记录记 `FAILED`，`aborted()`/`abortReason()` 为真值并停止该文件，保留此前结果（退出码映射属 cli-corpus，本切片不实现）
  - label 一致性与结果比对共用 `ResultComparer` 的 `actualView()`（含哈希形态）；只执行型 query 以空期望取视图，仅用于 label 比对
  - 行宽与类型签名不匹配（`ResultComparer` 抛 `IllegalArgumentException`）转为该记录失败，不中止文件
  - 致命判定：SQLState `08*`、连接已关闭、或消息命中连接类关键字；其余 SQL 错误为业务失败
  - Java 17 未启预览，`switch` 模式匹配不可用，记录分派改用 `instanceof` 模式
  - `QueryResult.rows` 需容纳 `null`（SQL NULL），故用可空安全的不可变拷贝而非 `List.copyOf`
- 只读复用 `model` / `parser` / `normalize`，未改其行为合同；未实现 CLI/退出码/目录收集/官方语料硬验收；未引入 Q8 默示豁免
- 文档影响: 开发文档（README + Javadoc + 本文件）已更新；用户文档 N/A（无 CLI 入口）；运维文档 N/A
- 安全: 只执行语料自带 SQL；连接由调用方创建/关闭，执行器不开关连接、不做库初始化与清理；错误摘要仅取驱动消息，不含连接串或凭据；测试用内存库与仓内 fixtures；无敏感信息入仓
- 未解决风险: 无阻塞。JDBC 与原生 sqllogictest 的结果差异留待 cli-corpus 硬验收（Q8，本切片不豁免）。本机 Maven 位于 `~/tools/apache-maven-3.9.16`（不在默认 PATH）；JDK 17.0.20

## 验证

- 最低验证层: L3（单元/组件 + SQLite JDBC 内存库集成 + 构建）
- 命令: `mvn -q clean test`（Java 17；`mvn clean test` 用于取明细）
- 结果: exit 0 / **BUILD SUCCESS**；Surefire **Tests run: 84, Failures: 0, Errors: 0, Skipped: 0**
  - 新增 48：runner 状态机 21 + 验收 7 + 依赖隔离 3 + `com.ggtest.db` 7 + SQLite JDBC 10
  - 既有 36 无回归：parser 10 + normalize 26
- TDD 证据: 每个任务先写测试并观察失败（缺目标类型的编译失败）后实现至通过
- 测试有效性抽查（变异验证）: 临时令条件求值恒返回「不跳过」并停用 label 冲突判定 → 10 项失败，含 `RunnerAcceptanceTest.p0_6_conditionsSkipRecordsPerEngine`（被跳过记录变为 `FAILED`）与 `p1_4_conflictingLabelFailsSecondQuery`；变异已还原，回归全绿
- 依赖获取: `org.xerial:sqlite-jdbc:3.53.2.0` 自 Maven Central 拉取成功，未使用代理 `127.0.0.1:7890`
- 环境: 沙箱内 Maven 无法写 `~/.m2`，验证在沙箱外执行；无验证缺口

## 验收自查（QA 以 Spec 独立复核为准）

| 验收 | 要求 | 证据 | 结果 |
|---|---|---|---|
| P0-3 | `statement ok` 与 `statement error` 均通过；`statement error` 换为合法 SQL 后该条失败 | `p0_3_statementOkAndStatementErrorBothPass`（4 条全 Pass）；`p0_3_statementErrorWithValidSqlFails`（末条 Failed） | 通过 |
| P0-6 | engine=`sqlite` 时第一、三条 skipped，第二条执行 | `p0_6_conditionsSkipRecordsPerEngine`：Skipped/Passed/Skipped；fixture 中被跳过的 SQL 指向不存在的表，误执行即失败 | 通过 |
| P0-8 | runner 仅依赖执行器抽象；SQLite 实现可整体替换而不改 parser/runner 源码 | `RunnerDependencyIsolationTest`（`runner` 主源码不含 `com.ggtest.db.sqlite`/`java.sql`/`org.sqlite`/`org.xerial`；`com.ggtest.db` 顶层无 JDBC；`parser` 不触库）；`runnerDrivesAnyExecutorImplementation`（同一记录序列换 `engineName=duckdb` 的执行器即改变条件判定） | 通过 |
| P1-2 | `halt` 后记录不执行且计 skipped | `p1_2_recordsAfterHaltAreSkipped`：halt 后 2 条 Skipped、`halted()` 为真、0 失败 | 通过 |
| P1-4 | 同 label 结果冲突时后出现记录失败并指明 label 冲突 | `p1_4_conflictingLabelFailsSecondQuery`：后一条 Failed，原因含 `label` 与标签名 `samevals` | 通过 |
| 真库 query 路径 | 经 normalize 比对真实 JDBC 结果 | `queryPathNormalizesSortsAndHashesRealResults`：`IT rowsort` + SQL NULL、只执行型 query、`hash-threshold 1` 触发哈希形态全部 Pass | 通过 |

## QA 修复回执

| 缺陷 ID | 处理 | 摘要 | 验证 | 建议复测 |
|---|---|---|---|---|
| — | — | 尚无 QA 缺陷 | — | — |
