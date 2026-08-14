# QA Report: add-statement-query-timeout

## 验证

| 项 | 结果 |
|---|---|
| `mvn test` | Pass — 321 tests, 0 failures |
| demo.slt smoke | Pass — [PASSED] in 174ms |
| `timeout=1000` 解析 | Pass — query 和 statement 均解析正确 |
| `timeout=0` / 负数 | Pass — parseTimeoutMs 抛出 ParseException |
| `ApplyTimeout` | Pass — AbstractJdbcExecutor.setQueryTimeout() |
| Override 保留 timeout | Pass — applyStatementOverride 追加 ` timeout=<ms>` |

## 缺陷

无。

## 结论

**Pass**
