# Plan: refactor-filerunner-responsibilities

## 元信息

- 工作项标识: refactor-filerunner-responsibilities
- sub-feature-id: refactor-filerunner-responsibilities（未拆分）
- 依据 Spec: N/A（纯内部重构，跳过）
- 依据 Design: workflow/archive/2026/refactor-filerunner-responsibilities/design.md
- 依据 UI: N/A
- 路径等级: standard
- Review 门禁: required
- 最低验证层: L3（单元测试 + 集成测试）
- 验证命令: `mvn test`
- 预期证据: 所有测试通过，无新增编译警告

## 适用工程规范

- `workflow/agents/standards/documentation.md`
- `workflow/agents/standards/git.md`
- `workflow/agents/standards/quality.md`
- `workflow/agents/standards/security.md`

## 目标摘要

将 `FileRunner`（210 行，多职责）拆分为：
- `ConnectionFactory` — JDBC 连接创建
- `EngineAdapter` — 引擎生命周期接口
- `SqliteAdapter` — SQLite 实现
- `PostgresAdapter` — PG 实现（schema 隔离 + teardown）
- `OverrideCoordinator` — override 收集与写回
- `FileRunner` — 退化为纯编排器（~60 行）

## 任务拆解

### T1: 实现 ConnectionFactory（完成条件：独立编译通过，Extract Method 不改变行为）

从 `FileRunner.openConnection()`（L197-205）迁出：
```java
static Connection open(CliOptions options) throws SQLException
```
- `openConnection()` 替换为 `ConnectionFactory.open(options)` 调用
- 编译通过即为完成

### T2: 实现 EngineAdapter + SqliteAdapter（完成条件：编译通过；FileRunner.runSqliteFile 适配调用）

`EngineAdapter`（package-private 接口）：
```java
String engineName();
DatabaseExecutor createExecutor(Connection connection);
default void prepare(Connection connection) throws SQLException {} // no-op default
default void teardown(Connection connection) throws SQLException {} // no-op default
```

`SqliteAdapter`（package-private）：
- `engineName()` → `SqliteJdbcExecutor.ENGINE_NAME`
- `createExecutor()` → `new SqliteJdbcExecutor(connection)`
- prepare/teardown 使用接口默认 no-op

重构 `FileRunner.runSqliteFile(L73-75)` 为通过 adapter 调用。

### T3: 实现 PostgresAdapter（完成条件：编译通过；runPostgresFile 行为精确复制到 PostgresAdapter）

将 `FileRunner.runPostgresFile(L77-114)` 完整逻辑迁入 `PostgresAdapter.run()`：

```java
FileOutcome run(Connection connection, List<SqlTestRecord> records,
                String display, Path file, Function<...> runWithExecutor,
                PrintStream err, ReportWriter reportWriter)
```

或更简洁地，`PostgresAdapter` 实现 `prepare()`（创建 schema + SET search_path）和 `teardown()`（DROP SCHEMA CASCADE），运行循环留在 FileRunner。

**关键：** teardown 的 finally 语义、schema null 检查、teardown 异常覆盖 outcome 的逻辑必须精确复制。原实现中 `err.println` 的 schema isolation 失败消息保留。

### T4: 实现 OverrideCoordinator（完成条件：编译通过；collectOverrides + writeBack 行为不变）

从 `FileRunner.L162-194` 迁出：
```java
static List<OverrideWriter.Override> collectOverrides(FileRunResult result)
FileOutcome applyOverrideWriteBack(Path file, List<OverrideWriter.Override> overrides, 
                                    String display, ReportWriter reportWriter)
```
- `applyOverrideWriteBack` 返回 `null` 表示无 write-back 错误
- `OverrideWriter.writeAtomically` 的异常处理保留
- sanitize 调用保留（通过传入 FileRunner.sanitize 方法引用或直接使用 `CredentialRedaction.redactMessage`）

### T5: 重构 FileRunner（完成条件：编译通过；所有现有 FileRunnerTest 通过）

重构后的 `FileRunner`：
- 成员：`CliOptions`, `PrintStream err`, `ReportWriter`, `ConnectionFactory`(static), `OverrideCoordinator`(static), `EngineAdapter`
- `run(parser, file, display)`:
  1. parse (L40-55)
  2. `ConnectionFactory.open(options)` (L57)
  3. try-with-resources connection
  4. adapter = selectEngineAdapter(options.engine()) → L58-59
  5. adapter.prepare(connection)
  6. runWithExecutor(adapter.createExecutor(connection), records, display, file)
  7. adapter.teardown(connection) in finally
  8. override write-back check
  9. return outcome
- `sanitize(String)` 保留（L207-209）
- `runWithExecutor(DatabaseExecutor, records, display, file)` 保留（L116-160）

### T6: 更新 CliSession（完成条件：编译通过；构造参数适配）

`CliSession(L34-41)` 中 `new FileRunner(options, err, reportWriter)` 不变（因为新拆分出的类是 static 方法或通过 FileRunner 内部使用，FileRunner 公共构造签名可能不变）。

### T7: 全量测试验证（完成条件：mvn test 全部通过）

运行 `mvn test`，确保：
- `FileRunnerTest` 全部通过
- `PostgresCliIntegrationTest` 通过
- `CliReportAcceptanceTest` 通过
- `CorpusHardAcceptanceTest` 通过
- 无新增 regression

## 依赖与顺序

```
T1 (ConnectionFactory)
 ├─ T2 (EngineAdapter + SqliteAdapter)
 │    └─ T3 (PostgresAdapter)
 │         └─ T5 (FileRunner 重构)
 ├─ T4 (OverrideCoordinator)
 │    └─ T5 (FileRunner 重构)
 └─ T5 → T6 (CliSession) → T7 (全量测试)
```

T1 和 T2 可并行；T4 可与 T2 并行。T3 依赖 T2（需要 EngineAdapter 接口）。T5 依赖 T1+T3+T4。T6 依赖 T5。T7 串行末端。

## 触碰路径

| 文件 | 操作 | 说明 |
|---|---|---|
| `cli/ConnectionFactory.java` | 新增 | T1 |
| `cli/EngineAdapter.java` | 新增 | T2 |
| `cli/SqliteAdapter.java` | 新增 | T2 |
| `cli/PostgresAdapter.java` | 新增 | T3 |
| `cli/OverrideCoordinator.java` | 新增 | T4 |
| `cli/FileRunner.java` | 重写 | T5，从 210 行缩减到 ~110 行 |
| `cli/CliSession.java` | 小改 | T6，构造适配（若 FileRunner 签名变化） |
| `cli/FileRunnerTest.java` | 可能调整 | T7，若 mock 依赖变化 |
| `workflow/archive/2026/refactor-filerunner-responsibilities/dev-notes.md` | 新增 | Developer 记录 |

不碰：`Model/`, `Parser/`, `Normalize/`, `Db/`, `Runner/`, `OverrideWriter`, `ReportWriter`, `SqlLogicTestRunner`, `Main`

## 验收与验证

| ID | 要求或命令 | 预期证据 | 结果 |
|---|---|---|---|
| V1 | `mvn compile` 无错误 | BUILD SUCCESS | |
| V2 | `mvn test -pl .` 全部通过 | Tests run: XX, Failures: 0, Errors: 0 | |
| V3 | `PostgresCliIntegrationTest` 通过（若 PG 可用） | PG 集成测试通过或 skipped | |
| V4 | `CorpusHardAcceptanceTest` 通过 | 语料验收全绿 | |
| V5 | `FileRunnerTest` 覆盖所有正常/异常路径 | 通过；无退化 | |
| V6 | 手动抽查 `--engine postgres` + `--engine sqlite` 输出格式不变 | TOTAL 行格式一致 | |
| V7 | 手动抽查 `--override` 写回行为不变 | 文件内容正确改写 | |
| V8 | 手动抽查 credential redaction 有效 | 错误输出无明文密码/URL 凭据 | |

## 验证缺口

| 项 | 原因 | 风险 | 恢复条件 |
|---|---|---|---|
| N/A | — | — | — |

## 文档影响

| 类别 | 更新路径或 N/A 理由 |
|---|---|
| 开发文档 | `workflow/archive/2026/refactor-filerunner-responsibilities/dev-notes.md`；注册册 CA-010 更新为 resolved |
| 用户文档 | N/A（无用户可见行为变化） |
| 运维文档 | N/A |

## 交接顺序

1. Developer 实施与开发者验证（T1-T7）→
2. Reviewer（Review 门禁 required）→ 取得 Approve →
3. QA 验收（逐项核对 V1-V8）→
4. 用户合并授权 → Manager 置 `done` 一次提交 → 合入 main

## 修订记录

| 日期 | 摘要 |
|---|---|
| 2026-08-11 | 初始 Plan |
