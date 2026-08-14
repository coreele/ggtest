# QA Report: fix-ca016-stmt-error-conn

| 项 | 结果 |
|---|---|
| `mvn test` | Pass — 323 tests, 0 failures |
| conn= 不污染 expectedErrorMsg | Pass — "division by zero" != "division by zero conn=c1" |
| timeout= + conn= 组合 | Pass — msg/timeout/conn 各自正确 |
| 无回归 | Pass — 原 statement error 测试全绿 |

## 结论: Pass
