# QA Report: fix-override-rowwise

## 验证

| ID | 要求 | 结果 |
|---|---|---|
| V1 | `mvn test` 321 tests | Pass — 0 failures |
| V2 | `--override` row-wise 格式 | Pass — 写入 `1\|1\|hello world` |
| V3 | `seperator` 拼写提示 | Pass — "did you mean separator?" |

## 缺陷

无。

## 结论

**Pass**
