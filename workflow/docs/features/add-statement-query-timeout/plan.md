# Plan: add-statement-query-timeout

## 元信息

- 工作项标识: add-statement-query-timeout
- 路径等级: standard
- Review 门禁: required
- 验证命令: `mvn test`
- 预期证据: 321+ tests, 0 failures

## 任务拆解

### T1: 模型层 — StatementRecord + QueryRecord 加 timeoutMs

- `StatementRecord`: 新增 `int timeoutMs`，构造函数 `timeoutMs` 默认 0
- `QueryRecord`: 新增 `int timeoutMs`，构造函数默认 0

### T2: 解析器 — statement 和 query 头解析 timeout

- `parseStatement()`: 解析 `ok|error` 后的剩余 token 为 key=value 属性
- `parseQuery()`: 在属性循环中新增 `timeout` case
- `timeout` value 验证：正整数，> 0

### T3: DB 层 — DatabaseExecutor + AbstractJdbcExecutor

- `DatabaseExecutor`: 新增默认方法 `executeStatement(sql, timeoutMs)` / `executeQuery(sql, timeoutMs)`，默认委托无 timeout 版本
- `AbstractJdbcExecutor`: 覆写，`setQueryTimeout((timeoutMs + 999) / 1000)`

### T4: Runner — SqlLogicTestRunner 传递 timeout

- `runStatement()`: 调用 `executor.executeStatement(sql, timeoutMs)`
- `runQuery()`: 调用 `executor.executeQuery(sql, timeoutMs)`

### T5: OverrideWriter — statement error 覆写保留 timeout

- `applyStatementOverride()`: 当 `timeoutMs > 0` 时，append ` timeout=<ms>` 到覆写文本后
- 注意：当前 statement override 只替换 error message，timeout 不在替换范围内

### T6: 测试 + 文档

- `SqlLogicTestParserTest`: 新增 timeout 解析测试（正常、非法值、重复 key）
- `SqlLogicTestRunnerTest`: 新增 timeout 传递测试
- `demo.slt` / `demo_zh.slt`: 添加 timeout 示例

## 触碰路径

| 文件 | 操作 |
|---|---|
| `model/StatementRecord.java` | 新增 timeoutMs |
| `model/QueryRecord.java` | 新增 timeoutMs |
| `parser/SqlLogicTestParser.java` | statement/query 解析 timeout |
| `db/DatabaseExecutor.java` | 新增超时方法 |
| `db/AbstractJdbcExecutor.java` | 实现 setQueryTimeout |
| `runner/SqlLogicTestRunner.java` | 传递 timeout |
| `cli/OverrideWriter.java` | 保留 timeout 属性 |

## 交接顺序

Developer → Reviewer (Approve) → QA → 合并授权 → done
