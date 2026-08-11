# Plan: fix-ca015-dead-adapters

## 元信息
- 路径等级: fast
- Review 门禁: skipped

## 任务
1. 删除 `cli/EngineAdapter.java`、`cli/SqliteAdapter.java`、`cli/PostgresAdapter.java`
2. `mvn compile` + `mvn test` 验证无引用断裂

## 验收
- 321 tests, 0 failures
- 全仓库无 EngineAdapter/SqliteAdapter/PostgresAdapter 引用
