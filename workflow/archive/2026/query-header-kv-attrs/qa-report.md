# QA Report: query-header-kv-attrs

## 验证结果

| ID | 要求 | 结果 |
|---|---|---|
| V1 | `mvn compile` | Pass — BUILD SUCCESS |
| V2 | `mvn test` | Pass — 321 tests, 0 failures |
| V3 | 旧语法 `separator \|` → 引导错误 | Pass — P0-3 测试通过 |
| V4 | `separator=` 空值 → value-per-line | Pass — QueryRecord.columnSeparator = Optional.empty() |
| V5 | unknown key → 解析错误 | Pass — 含 supported key 列表 |
| V6 | demo.slt 手动跑 | Pass — fixture 已更新为新语法，RunnerAcceptanceTest 全绿 |

## 缺陷

无。

## 结论

**Pass**
