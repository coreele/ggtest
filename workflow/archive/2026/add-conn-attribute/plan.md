# Plan: add-conn-attribute

## 元信息

- 工作项标识: add-conn-attribute
- 路径等级: standard
- Review 门禁: required
- 验证: `mvn test` + demo2.slt smoke

## 任务

### T1: Model — StatementRecord + QueryRecord 加 conn

- `StatementRecord`: 新增 `String conn`（null = 默认），更新 3 个构造函数
- `QueryRecord`: 新增 `String conn`（null = 默认），更新 3 个构造函数

### T2: Parser — 解析 `conn=<name>`

- statement 属性循环：新增 `conn` case
- query 属性循环：新增 `conn` case
- 校验：值非空、无空白
- `isKnownAttributeKey` 添加 "conn"

### T3: Runner — 多连接支持

- 新增 `SqlLogicTestRunner(Function<String, DatabaseExecutor> executorFactory, ...)` 构造函数
- `runStatement` / `runQuery`: 通过 `record.conn()` 查找 executor
- 内部缓存 `Map<String, DatabaseExecutor>`

### T4: FileRunner — 惰性连接管理

- `run()` 方法内创建连接工厂 lambda
- 工厂惰性打开 `ConnectionFactory.open(options)` 并创建 executor
- 收集所有连接，finally 关闭

### T5: OverrideWriter — 保留 conn

- `applyStatementOverride`: 若 `stmt.conn() != null`，append ` conn=<name>` 到覆写文本

### T6: 测试 + demo2.slt

- `SqlLogicTestParserTest`: conn 解析测试
- `SqlLogicTestRunnerTest`: 多连接测试
- demo2.slt 格式修正（如果输出格式需要调整）

## 触碰路径

| 文件 | 操作 |
|---|---|
| `model/StatementRecord.java` | 加 conn |
| `model/QueryRecord.java` | 加 conn |
| `parser/SqlLogicTestParser.java` | 解析 conn |
| `runner/SqlLogicTestRunner.java` | 多连接工厂 |
| `cli/FileRunner.java` | 惰性连接生命周期 |
| `cli/OverrideWriter.java` | 保留 conn |
| `examples/demo2.slt` | 可能调整格式 |

## 交接

Developer → Reviewer (Approve) → QA → 合并授权 → done
