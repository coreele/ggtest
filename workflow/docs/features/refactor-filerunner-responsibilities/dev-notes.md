# Dev Notes: refactor-filerunner-responsibilities

## 实施摘要

将 `FileRunner`（210 行）拆分为 5 个新类，自身缩减到 120 行。

### 新增文件

| 文件 | 行数 | 说明 |
|---|---|---|
| `cli/ConnectionFactory.java` | 20 | JDBC 连接创建 |
| `cli/EngineAdapter.java` | 29 | 引擎生命周期接口（`run` 方法封装完整生命周期） |
| `cli/SqliteAdapter.java` | 22 | SQLite 实现（直接执行，无 prepare/teardown） |
| `cli/PostgresAdapter.java` | 66 | PG 实现（schema 隔离 + teardown 异常处理，精确复制原逻辑） |
| `cli/OverrideCoordinator.java` | 61 | override 收集 + 原子写回 |

### 修改文件

| 文件 | 变更 |
|---|---|
| `cli/FileRunner.java` | 120 行（原 202），移除 runSqliteFile、runPostgresFile、collectOverrides、applyOverrideWriteBack；run() 简化为 parse → connection → adapter.run()；runWithExecutor 改用 OverrideCoordinator |
| `cli/CliSession.java` | 无变更（构造签名不变） |

### 验证

- `mvn compile`: BUILD SUCCESS（51 源文件，0 error）
- `mvn test`: 321 tests, 0 failures, 0 errors, 16 skipped (PG)
- 覆盖：FileRunnerTest(11), PostgresCliIntegrationTest(5), MainOrchestrationTest(28), CliReportAcceptanceTest(14), CorpusHardAcceptanceTest(2) 全绿
