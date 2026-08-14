# Review: ggtest-core / runner-sqlite

> **工作项**：`ggtest-core` · **sub-feature-id**：`runner-sqlite`  
> **路径**：full · **Review 门禁**：required · **Design**：required（`design.md` 已用户 approved）  
> **审阅版本**：`5cf84fc`（实现；前置 docs `e359f05`）；分支 `ggtest-core-runner-sqlite` → `main`；Plan T1–T5  
> **依据**：[`spec.md`](./spec.md)、[`design.md`](./design.md)、[`plan.md`](./plan.md)、[`dev-notes.md`](./dev-notes.md)；`documentation.md` / `quality.md` / `security.md` / `git.md`

## 审阅范围

- 实现：`com.ggtest.db`、`com.ggtest.db.sqlite`（`SqliteJdbcExecutor`）、`com.ggtest.runner`；`pom.xml`（`org.xerial:sqlite-jdbc:3.53.2.0`）
- 测试：`src/test/java/com/ggtest/{db,db/sqlite,runner}/*`；`src/test/resources/fixtures/runner/*`
- 文档：`README.md`（Runner / 扩展库）、公共 Javadoc、`dev-notes.md`
- Git：源分支 `ggtest-core-runner-sqlite`；提交 `e359f05`、`5cf84fc`
- 未改：`parser` / `normalize` 行为源码（相对 `main` diff 为 0）

## 结论

**Approve**

无阻塞项；实现、测试、文档与安全要求满足进入 QA 的条件。建议 Manager 调度 QA。

## 实现正确性

| 要求 | 证据 | 结果 |
|---|---|---|
| P0-3 statement 断言 | fixture `p0-3-statement-assertions.test`：4 条 PASSED；`p0-3-statement-error-made-valid.test`：末条 FAILED | 通过 |
| P0-6 条件控制 | `p0-6-conditions.test`：SKIPPED / PASSED / SKIPPED；被跳过 SQL 指向不存在表，误执行即失败 | 通过 |
| P0-8 扩展点隔离 | `runner` 主源码 import 仅 `com.ggtest.db` + model/normalize；无 `com.ggtest.db.sqlite` / `java.sql`；`RunnerDependencyIsolationTest` + `runnerDrivesAnyExecutorImplementation`（换 `duckdb` 假执行器改变条件判定） | 通过 |
| P1-2 halt | `p1-2-halt.test`：halt 后 2 条 SKIPPED、`halted()` 为真、0 失败 | 通过 |
| P1-4 label | `p1-4-label-conflict.test`：后一条 FAILED，原因含 `label` 与 `samevals`；假执行器覆盖哈希形态一致性 | 通过 |
| Design 分层 | `db` 抽象无 JDBC；JDBC 仅 `db.sqlite`；Runner 编排 skipif/onlyif、hash-threshold、halt、statement/query、label、`FatalDatabaseException` 中止 | 通过 |
| Plan 不越界 | 无 CLI/退出码/目录收集/官方语料硬验收；无 Q8 默示豁免；未改 parser/normalize | 通过 |

独立验证：

```text
JAVA_HOME=OpenJDK 17.0.20 (Homebrew)
PATH=~/tools/apache-maven-3.9.16/bin:$PATH
mvn clean test
→ BUILD SUCCESS
→ Tests run: 84, Failures: 0, Errors: 0, Skipped: 0
  （runner 31 + db/sqlite 17 + parser 10 + normalize 26）
```

## 测试有效性

- 覆盖 P0-3/P0-6/P0-8/P1-2/P1-4 与 Plan T1–T4；假执行器测编排，真 `jdbc:sqlite::memory:` 测 P0-3 与 query→normalize。
- 可失败性：被跳过 SQL 指向缺失表；label 原因含标签名；关连接 → `FatalDatabaseException`；`failingRecordDoesNotStopRemainingRecords`。
- 边界：engine 大小写、halt 受 onlyif 跳过、跳过 hash-threshold 不改阈值、无期望 query、致命中止保留此前结果、SQL NULL→`null`。
- L3 达标（单元/组件 + 内存 SQLite + 构建）；parser/normalize 无回归。
- 非阻塞缺口：无「多条 skipif+onlyif 叠加」真库专项 fixture（假执行器已覆盖）。

## 文档影响核对

| Plan 声明 | 一致 | 备注 |
|---|---|---|
| 开发文档：`README.md` + Javadoc + `dev-notes.md` | 是 | Runner 用法、扩展库、`jdbc:sqlite::memory:` 示例、L3 证据与驱动版本 |
| 用户文档 N/A | 是 | 无 CLI |
| 运维文档 N/A | 是 | 无部署面 |

## 安全影响核对

检查范围：执行调用方/语料 SQL；调用方持有 `Connection`；新增依赖 `sqlite-jdbc`；错误摘要进入 `RecordResult`。无认证授权、无出站网络（测试用内存库）、无写回路径遍历。

| 检查项 | 结果 | 备注 |
|---|---|---|
| 敏感信息 | 无发现 | 代码/测试/fixtures/README/dev-notes/提交无凭据；摘要取驱动消息、不含连接串 |
| 认证与授权 | N/A | — |
| 输入处理 | 可接受 | 有意执行语料 SQL；扩展点契约明确；首期无 message/正则匹配 |
| 文件操作 | 可接受 | 测试 classpath fixtures 只读 |
| 依赖变更 | 可接受 | 钉死 `org.xerial:sqlite-jdbc:3.53.2.0`（compile） |
| 处置状态 | 无需处置 | 允许进入 QA |

## Git 合规

| 检查项 | 结果 |
|---|---|
| 工作分支 | `ggtest-core-runner-sqlite`（非 main） |
| 提交 | `e359f05` docs；`5cf84fc` feat；Conventional Commits |
| 禁止提交项 | 无密钥/`.env`/构建产物；`.gitignore` 含 `target/` |
| 越界 | 未改 parser/normalize 源码 |

## 必修项

| ID | 位置 | 问题 | 状态 |
|---|---|---|---|
| — | — | 无 | — |

## 非阻塞建议

| ID | 位置 | 建议 |
|---|---|---|
| N1 | `SqliteJdbcExecutor` `FATAL_MESSAGE_MARKERS` | 消息启发式（如 `no such database`）可能误判业务失败；cli-corpus 遇误中止时可收紧为 SQLState/`isClosed` 为主 |
| N2 | `SqlLogicTestRunnerTest` | 可选：真库或假执行器专项断言「多条 skipif+onlyif 叠加」 |

## 后续动作

1. Manager：调度 **QA**（`qa-report.md`）。
2. QA：P0-3/P0-6/P0-8/P1-2/P1-4；`mvn -q clean test`（JDK 17）；含 parser/normalize 回归与 runner 依赖检查。
3. Developer：N1–N2 可选，不阻断 QA。
4. 复审：无（Approve；QA Fail 修复后再审）。
