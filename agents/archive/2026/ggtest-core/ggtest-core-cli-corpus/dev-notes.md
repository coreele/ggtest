# Dev Notes: ggtest-core / cli-corpus

## 实现说明

- 工作项: `ggtest-core` / `cli-corpus`；路径 full；Design skipped；Review required；源分支 `ggtest-core-cli-corpus` → `main`
- Plan T1–T6 已交付；本轮另修 **DEF-CLI-001**（QA 轮次 2）
- T3 编排（修复后）: 每文件 `SqlLogicTestParser` → **独立** JDBC 连接 → `SqliteJdbcExecutor` → `SqlLogicTestRunner(executor, cliHashThreshold).run` → **关闭连接**；跨文件重置 hash-threshold（Runner 以 CLI 初值新建）与库状态（`:memory:` 每连接空白库）
- 变更路径（含本修复）:
  - `src/main/java/com/ggtest/cli/{Main,CliSession,CliArgumentParser,CliOptions,UsageException,TestFileCollector}.java`
  - `src/test/java/com/ggtest/cli/{CliArgumentParserTest,TestFileCollectorTest,MainOrchestrationTest,ExecutableJarManifestTest,CorpusHardAcceptanceTest}.java`
  - `src/test/resources/fixtures/cli/**`（含 `cross-file/schema-{a,b}.test`）
  - `pom.xml`、`bin/ggtest`、`README.md`、本文件
- 取舍: 手写 argv；凭据只进 `DriverManager`；只读组装上游、未改上游合同；无官方大语料入库；无 Q8 默示豁免
- 文档影响: README + Javadoc + 本文件；运维 N/A
- 安全: 用户 SQL + 可选凭据；测试仅内存库/自造 fixtures；无敏感信息入仓
- 阻塞: 无（硬验收已在本机语料路径执行）

## 验证

- 最低验证层: L4
- `GGTEST_CORPUS_DIR=/Users/zhougangjie/Space/sqllogictest/test`
- `mvn -q clean test` → **BUILD SUCCESS**；Surefire **Tests run: 111, Failures: 0, Errors: 0, Skipped: 1**
  - cli 编排含跨文件库隔离用例；Skipped 1：Manifest Main-Class（`clean test` 无 JAR）
  - 硬验收自动化 2 例均执行并通过（未 skip）
- `mvn -q clean package` → **BUILD SUCCESS**
- P0-1:
  - `./bin/ggtest --url jdbc:sqlite::memory: "$GGTEST_CORPUS_DIR/select1.test"`
  - 退出码 **0**；`TOTAL: passed=1031 failed=0 skipped=0`
- P1-5:
  - `./bin/ggtest --url jdbc:sqlite::memory: select1 select2 select3`（同一次调用三文件）
  - 退出码 **0**；分文件 failed=0；`TOTAL: passed=5413 failed=0 skipped=0`
- 环境: JDK 17.0.20；Maven `~/tools/apache-maven-3.9.16`；代理可用但本次未必需

## 验收自查（QA 独立复核为准）

| 验收 | 要求 | 证据 | 结果 |
|---|---|---|---|
| P0-1 | 官方 select1、失败=0、退出码=0 | 上节 CLI | **Pass（开发者验证）** |
| P1-1 | 目录递归 `.test`/`.slt`，分文件+总计 | `directoryRecurses…` + nested fixtures | fixtures 通过 |
| P1-5 | 官方 select1/2/3、失败=0、退出码=0 | 上节 CLI 批量 | **Pass（开发者验证）** |
| P1-6 | `.slt` 与同等 `.test` 一致 | `sltFileBehavesLikeEquivalentTestFile` | fixtures 通过 |
| 退出码 0/1/2 | 通过 / 断言失败 / 用法·解析·连接 | `MainOrchestrationTest` | fixtures 通过 |
| 跨文件重置 | 后文件不受前文件 hash-threshold / 库 schema 污染 | `laterFileIsNotPollutedByEarlierHashThreshold`；`laterFileIsNotPollutedByEarlierDatabaseSchema` | fixtures 通过 |

## QA 修复回执

| 缺陷 ID | 处理 | 摘要 | 验证 | 建议复测 |
|---|---|---|---|---|
| **DEF-CLI-001** | **已修复** | 根因：`CliSession` 整会话共享 JDBC 连接，跨文件库污染（`table t1 already exists`）。修复：每文件独立 open/run/close；fixtures `schema-a`+`schema-b` 覆盖批量子库隔离 | `mvn -q clean test` 111/0（Skipped 1）；`package` SUCCESS；P0-1 exit 0 failed=0；P1-5 exit 0 failed=0（TOTAL passed=5413） | **必测** P1-5 官方三文件批量、`mvn test`/`package`、`laterFileIsNotPollutedByEarlierDatabaseSchema`；回归 P0-1、P1-1/P1-6/退出码 fixtures。Review required → 重新 Approve 后再 QA 追加轮次 |
