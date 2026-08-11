# Dev Notes: fix-override-rowwise

## 实施

### SqlLogicTestRunner.formatOverrideText

```java
private static String formatOverrideText(QueryRecord record, List<String> actualView) {
    if (record.columnSeparator().isEmpty() || actualView.size() == 1) {
        return String.join("\n", actualView);  // value-per-line or hash line
    }
    int columns = record.typeSignature().size();
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < actualView.size(); i++) {
        if (i > 0 && i % columns == 0) sb.append('\n');
        else if (i > 0) sb.append(record.columnSeparator().orElseThrow());
        sb.append(actualView.get(i));
    }
    return sb.toString();
}
```

- `columnSeparator` 为空 → 原样 value-per-line
- `actualView.size() == 1` → 单行（可能是 hash），原样
- 否则按 `columns` 分组，`columnSeparator` 拼接

### SqlLogicTestParser.editDistance

- 标准 Levenshtein 距离，O(n*m) 时空优化为单行数组
- 阈值 ≤2 匹配常见拼写错误（seperator, separater, seprator）

### 验证

- `mvn test`: 321 tests, 0 failures
- 手动 `--override`: `1|1|hello world` ✓
