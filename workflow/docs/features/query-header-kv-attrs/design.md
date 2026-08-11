# Design: query-header-kv-attrs

## 背景与约束

将 query header 的 `separator <delim>` 空格分隔形式改为 `separator=<delim>` key=value 形式。扩展性：未来可在同一 header 行添加其他属性。

约束：
- 不改变 `QueryRecord` 字段结构（`columnSeparator`、`label` 语义不变）
- 不改变 `ResultComparer` / `ExpectedResultExpander` 的下游语义
- 旧语法须给出可读错误而非静默误解析

## 方案对比与决策

| 方案 | 概要 | 优点 | 缺点 | 比较依据 |
|---|---|---|---|---|
| A: 最小改动 | 仅把 splitTokens 后的两 token 匹配改为单 token `contains("=")` | 改动最小 | 旧语法 `separator |` 会被误解析为 label + 额外 token，错误信息不友好 | 不够 |
| B: KV 解析 + 旧语法检测 | 新增 key=value 循环解析；对不含 `=` 但匹配已知 key 名的 token 抛引导错误 | 扩展性好，错误信息清晰 | 多 ~20 行解析代码 | **选中** |
| C: 全量重构为 Map<String,String> attrs | QueryRecord 新增 `Map<String,String> attributes` 字段 | 最可扩展 | 改动面大，下游需适配新字段 | 过度设计 |

**决策:** 方案 B。新增 `key=value` 循环解析，保持 `columnSeparator` 和 `label` 字段不变。当 token 不含 `=` 但等于已知 key 名时抛引导错误。

## 模块边界与分层

所有改动在 `parser/` 包内：

```
SqlLogicTestParser.parseQuery()
  └─ parseQueryAttributes(tokens, index) → (label, columnSeparator, attrs)
      ├─ token 含 '=' → switch(key) parse 属性
      ├─ token 不含 '=' → 作为 label
      └─ token 不含 '=' 但等于已知 key → 抛引导错误
```

## 模块影响

| 文件 | 变更 |
|---|---|
| `parser/SqlLogicTestParser.java` | 重写 `parseQuery` 中属性解析部分（~30 行） |
| `model/QueryRecord.java` | Javadoc 更新 |
| `normalize/ResultComparer.java` | Javadoc 更新 |
| `normalize/ExpectedResultExpander.java` | Javadoc 更新 |
| `runner/SqlLogicTestRunner.java` | Javadoc 更新 |
| 测试 `SqlLogicTestParserTest.java` | 旧语法用例改为新语法；新增 KV 边界用例 |
| `examples/*.slt` | `separator |` → `separator=|` |
| `src/test/resources/fixtures/runner/*.test` | 同上 |
| 测试 `ResultComparerTest.java` | Javadoc 更新 |
| 测试 `RunnerAcceptanceTest.java` | 引用文件不变（fixture 已更新） |

不碰：`db/`、`cli/`、`normalize/` 核心逻辑

## 风险

| 风险 | 影响 | 缓解 |
|---|---|---|
| 旧语料文件 break | demo.slt / fixture 文件 | 一并更新为 `separator=<delim>` |
| `---- separator` 旧错误路径 | 错误信息不配套 | 同步更新错误信息中的语法模板 |
| 空 separator value 语义 | `separator=` 是否等价 value-per-line | Spec 已明确：`columnSeparator` 为 `Optional.empty()` |
