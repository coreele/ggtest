# Dev Notes: ggtest-core / cli-corpus

## 实现说明

- 工作项: `ggtest-core` / `cli-corpus`；路径 full；Design skipped；Review required；源分支 `ggtest-core-cli-corpus`（目标 `main`）
- Plan T1–T6 已完成:
  - T1 `CliArgumentParser` / `CliOptions` / `UsageException`：`--url` 必填；可选 `--user`/`--password`/`--engine`（默认 sqlite，仅允许该值）/`--hash-threshold`（默认 8）；≥1 位置参数；用法错误 → 退出码 2；输出不含凭据
  - T2 `TestFileCollector`：目录递归 `*.test`/`*.slt`，绝对路径字典序；单文件不强制扩展名
  - T3 `Main` + `CliSession`：parser → 共享 JDBC 连接 + `SqliteJdbcExecutor` → 每文件新建 `SqlLogicTestRunner(executor, cliHashThreshold).run`；分文件+TOTAL；FAILURE 四要素；退出码 0/1/2；解析错误计硬错误并继续
  - T4 shade/jar `Main-Class=com.ggtest.cli.Main`；`bin/ggtest`
  - T5 fixtures `src/test/resources/fixtures/cli/` + 编排/收集/参数测试；可选 `CorpusHardAcceptanceTest`（`GGTEST_CORPUS_DIR`）
  - T6 README、CLI Javadoc、本文件
- 变更路径:
  - `src/main/java/com/ggtest/cli/{Main,CliSession,CliArgumentParser,CliOptions,UsageException,TestFileCollector}.java`
  - `src/test/java/com/ggtest/cli/{CliArgumentParserTest,TestFileCollectorTest,MainOrchestrationTest,ExecutableJarManifestTest,CorpusHardAcceptanceTest}.java`
  - `src/test/resources/fixtures/cli/**`
  - `pom.xml`、`bin/ggtest`、`README.md`、本文件；并可提交既有未改状态机字段的 `docs/manager/*` 与本切片 `plan.md` 原件
- 取舍: 整次调用共享一条 JDBC 连接；跨文件仅重置 Runner 作用域；手写 argv；凭据只进 `DriverManager`
- 只读组装上游；未改上游合同；无官方大语料入库；无 Q8 默示豁免
- 文档影响: README + Javadoc + 本文件；运维 N/A
- 安全: 用户 SQL + 可选凭据；测试仅内存库/自造 fixtures；无敏感信息入仓
- 阻塞:
  - **未验证项**: P0-1 / P1-5 官方语料硬验收
  - **原因**: 未设置 `GGTEST_CORPUS_DIR`，本机未找到含 `select1.test` 的路径
  - **风险**: L4 硬验收（失败数=0、退出码=0、零豁免）未证实
  - **恢复**: 提供语料目录后执行下方硬验收命令或 `GGTEST_CORPUS_DIR=... mvn test`，补证据到本文件 / QA
  - **复测范围**: P0-1、P1-5（及可选自动化硬验收测试）
  - **禁止**将硬验收标为通过

## 验证

- 最低验证层: L4
- `mvn -q clean test` → **BUILD SUCCESS**；Surefire **Tests run: 110, Failures: 0, Errors: 0, Skipped: 3**
  - 新增 26：参数 8 + 收集 5 + 编排 9 + JAR/脚本 2 + 硬验收可选 2
  - Skipped 3：硬验收×2（无语料）；Manifest Main-Class×1（`clean test` 无 JAR；`package` 后已核验 `Main-Class: com.ggtest.cli.Main`）
  - 既有 84 无回归
- `mvn -q clean package` → **BUILD SUCCESS**
  - `./bin/ggtest --url jdbc:sqlite::memory: src/test/resources/fixtures/cli/pass.test` → 退出码 0、`failed=0`
  - `./bin/ggtest --url jdbc:sqlite::memory: src/test/resources/fixtures/cli/nested` → 退出码 0、分文件+TOTAL
- 硬验收命令（**未执行**）:
  - `ggtest --url jdbc:sqlite::memory: "$SELECT1"` → 期望失败数=0、退出码=0
  - `ggtest --url jdbc:sqlite::memory: "$SELECT1" "$SELECT2" "$SELECT3"` → 期望失败数=0、退出码=0
- TDD: 先失败测试再最小实现
- 环境: JDK 17.0.20；Maven `~/tools/apache-maven-3.9.16`；代理可用但本次未必需

## 验收自查（QA 独立复核为准）

| 验收 | 要求 | 证据 | 结果 |
|---|---|---|---|
| P0-1 | 官方 select1、失败=0、退出码=0 | 无语料 | **未执行（阻塞）** |
| P1-1 | 目录递归 `.test`/`.slt`，分文件+总计 | `directoryRecurses…` + nested fixtures | fixtures 通过 |
| P1-5 | 官方 select1/2/3、失败=0、退出码=0 | 无语料 | **未执行（阻塞）** |
| P1-6 | `.slt` 与同等 `.test` 一致 | `sltFileBehavesLikeEquivalentTestFile` | fixtures 通过 |
| 退出码 0/1/2 | 通过 / 断言失败 / 用法·解析·连接 | `MainOrchestrationTest` | fixtures 通过 |
| 跨文件重置 | 后文件不受前文件 hash-threshold 污染 | `laterFileIsNotPollutedByEarlierHashThreshold` | fixtures 通过 |

## QA 修复回执

| 缺陷 ID | 处理 | 摘要 | 验证 | 建议复测 |
|---|---|---|---|---|
| — | — | 尚无 QA 缺陷 | — | — |
