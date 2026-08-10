# Spec: feat-statement-error-msg

> 需求与规格（Plan 之前完成）。任务拆解见后续同目录 `plan.md`。
>
> **feature-id**：`feat-statement-error-msg` · **sub-feature-id**：`feat-statement-error-msg`（未拆分）

## 背景与目标

当前 `statement error` 仅验证 SQL 执行失败（any error），不校验错误消息内容。sqllogictest 规范允许 `statement error` 后跟可选错误消息文本，期望数据库返回的错误信息包含该文本。

本功能为 `statement error` 增加可选的错误消息匹配能力，提升 sqllogictest 验证精度——确保执行不仅失败，而且是因为**预期原因**失败（如特定约束违反、语法错误等）。

## 非目标

- 不实现正则表达式或模式匹配（仅 sub-string 包含匹配，case-insensitive）
- 不匹配完整 error message 精确相等（只要求包含关系，原因：JDBC 驱动错误消息格式因引擎 / 版本有细微差异）
- 不改变 `statement ok` 的行为
- 不影响 `statement error` 无消息时的现有行为（向后兼容）
- 不引入 error code / SQLState 匹配

## 范围与可见行为

| 场景 | 行为 |
|---|---|
| `statement error`（无消息） | 与当前一致：仅验证执行失败 |
| `statement error` 后紧跟空格分隔的错误消息文本 | 解析为可选 expectedErrorMsg；执行失败后验证 errorSummary 包含该文本（case-insensitive sub-string） |
| `statement error` + 预期消息，但执行成功 | 失败报告：`statement expected to fail but succeeded`（与当前一致） |
| `statement error` + 预期消息，执行失败但消息不包含预期文本 | 失败报告（多行 diff 风格）：首行 `statement error message mismatch`，随后 `-   <expectedMsg>`、`+   <actual errorSummary>` |
| 包含 `#` 的预期消息 | `#` 及其后内容视为消息一部分（不按注释剥离；与 `onlyif`/`skipif` 的 `#` 注释行为区分） |

解析器变更：
- `parseStatement` 在 `tokens.length >= 3` 时，将 `tokens[2]` 起的内容重组为 expectedErrorMsg
- 使用 `sqlLines` 类似方式：从 header 行提取 token 后的剩余内容作为消息文本（保原始空白）

## 合同

### API / 接口

`StatementRecord`:
```java
public record StatementRecord(
    String sql,
    StatementExpectation expectation,
    @Nullable String expectedErrorMsg,  // NEW: Optional; null when statement ok or statement error without message
    SourceLocation location
) implements SqlTestRecord {}
```

`StatementExpectation`: 移除 `"out of scope for the first iteration"` Javadoc 说明。

`StatementResult.errorSummary`: 移除 `"must not be matched against expected error messages"` 注释。

### 数据 / 状态

N/A — 不引入新状态或数据存储。

### 错误与约束

| 约束 | 处理 |
|---|---|
| `statement error` tokens 不足（空 SQL，tokens < 2） | 报错：`statement requires at least one expectation token (ok\|error)` |
| `expectedErrorMsg` 为空字符串或仅空格 | 视为无消息（与无消息行为一致） |
| 消息匹配 | errorSummary 为 null/空时视为不匹配 |

## 验收（Given-When-Then）

### P0

1. **statement error 无消息向后兼容**
   - Given 测试文件含 `statement error` 无附加文本、SQL 执行失败
   - When 运行 runner
   - Then 记录 PASSED（与当前一致）

2. **statement error 消息匹配 — 通过**
   - Given `statement error` 后跟消息 `"no such table"`、SQL 执行失败且 errorSummary=`"SQL error: no such table: missing"`
   - When 运行
   - Then case-insensitive sub-string 匹配通过 → PASSED

3. **statement error 消息不匹配 — 失败**
   - Given `statement error` 后跟消息 `"no such table"`、SQL 执行失败且 errorSummary=`"syntax error near INSERT"`
   - When 运行
   - Then 失败报告含 `statement error message mismatch`

4. **statement error 有消息但执行成功 — 失败**
   - Given `statement error` 后跟消息、SQL 执行成功
   - When 运行
   - Then 失败报告：`statement expected to fail but succeeded`

### P1

5. **解析：statement error 含单 token 消息**
   - Given 输入 `statement error no_such_table`
   - When 解析
   - Then `expectedErrorMsg = "no_such_table"`

6. **解析：statement error 含多 token 消息**
   - Given 输入 `statement error no such table: missing`
   - When 解析
   - Then `expectedErrorMsg = "no such table: missing"`

7. **解析：statement error 含 `#` 字面消息**
   - Given 输入 `statement error table#1 not found`
   - When 解析
   - Then `expectedErrorMsg = "table#1 not found"`（`#` 不被当注释剥离）

## 开放问题

- 无
