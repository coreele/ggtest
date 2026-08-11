# Review: fix-ca016-stmt-error-conn

## 审阅

### Bug 根因
`parseStatement` lines 163-184 用 `raw.indexOf(" timeout=")` 在原始 header 行搜索属性边界，
遗漏 `conn=`。`statement error msg conn=c1` 的 `expectedErrorMsg` 被提取为 `"msg conn=c1"`。

### 修复验证
- 改为 token 拼接：`tokens[2..msgEnd]` 用空格 join
- `msgEnd` = `firstAttrIndex`（首个含 `=` 的 token 索引）或 `tokens.length`
- `errorMsgStartColumn` 仍从原始行计算（OverrideWriter 依赖）

### 测试覆盖
- `statementErrorMsgWithConnAttr_doesNotPolluteMessage`: `conn=c1` → msg == "division by zero" ✓
- `statementErrorMsgWithTimeoutAndConn_extractsMessageOnly`: `timeout=2000 conn=c2` → msg == "lock timeout" ✓
- 原有 statement error 测试全绿（无回归）

### 正确性
- `statement error msg` (无属性): msgEnd == tokens.length, msg == "msg" ✓
- `statement error msg timeout=1000`: msgEnd 指向 timeout token, msg == "msg" ✓
- `statement error` (无 msg): msgEnd == 2, 不进 if 块, expectedErrorMsg == null ✓
- `statement error timeout=1000` (仅属性): msgEnd == 2, 不进 if 块 ✓

## 结论: Approve
