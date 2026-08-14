# Plan: fix-ca016-stmt-error-conn

## 元信息
- 路径等级: fast
- Review 门禁: required

## Bug
`parseStatement` lines 163-184: `expectedErrorMsg` 通过 `raw.indexOf(" timeout=")` 搜索属性边界。
`conn=` 不被识别 → `statement error msg conn=c1` 的 `expectedErrorMsg` 被污染为 `"msg conn=c1"`。

## 修复
将 lines 163-184 的原始行字符串搜索替换为 token 拼接：
```java
if (expectation == StatementExpectation.ERROR && msgEnd > 2) {
    StringBuilder msg = new StringBuilder();
    for (int i = 2; i < msgEnd; i++) {
        if (!msg.isEmpty()) msg.append(' ');
        msg.append(tokens[i]);
    }
    expectedErrorMsg = msg.toString();
    // errorMsgStartColumn 仍从原始行计算（用于 OverrideWriter）
    int keywordEnd = indexOfToken(headerLine, "error", 0);
    if (keywordEnd >= 0) {
        errorMsgStartColumn = findMsgStartColumn(headerLine, keywordEnd);
    }
}
```

`msgEnd`（= `firstAttrIndex` 或 `tokens.length`）已正确区分消息 token 和属性 token。

## 验收
- 321 tests, 0 failures
- 新增测试：`statement error msg conn=c1` → expectedErrorMsg == "msg"
