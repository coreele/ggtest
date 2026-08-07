# QA Report: improve-failure-output-format

## 轮次

| 轮次 | 日期 | 范围 | 结论 |
|---|---|---|---|
| 1 | 2026-08-07 | 首测 | Pass |

## 环境与命令

- JDK 17, Maven 3.8+
- 验证命令: `mvn -q clean test`

## 覆盖（对照 plan 最低验证层 + spec 验收）

| ID | 条目 | 结果 | 证据 |
|---|---|---|---|
| P0-1 | 失败报告行格式 `.. [FAILED] in X ms` + 下行 `    at file:line : reason` | Pass | `mvn test` 全量通过；手动验证 fail.test 输出 |
| P0-2 | 不再出现 `[WHY]`、`[SQL]`、`[Diff]` 标签 | Pass | `ReportWriterTest`、`CliReportAcceptanceTest` 中 `assertFalse(contains("[WHY]"))` 等通过 |
| P0-3 | 有 diff 时显示 `        (-expected|+actual)` 及 diff body (8空格缩进) | Pass | `ReportWriterTest.resultMismatchFormatsAtAndDiff` 验证缩进和内容 |
| P0-4 | 无 diff 时不显示 diff 块 | Pass | `ReportWriterTest.hardErrorDetailOmitDiff` 验证仅一行 `at`；手动验证 bad-parse.test |
| P0-5 | `at` 行4空格缩进，非 status 同行 | Pass | `ReportWriterTest.atLineHasFourSpaceIndent` 验证；CliSession 恢复原有打印逻辑 |
| P0-6 | `mvn test` 全量通过 | Pass | 251 测试通过 (18 跳过：PG/语料门控) |

## 缺陷

| ID | 严重度 | 摘要 | 状态 |
|---|---|---|---|
| — | — | — | — |

## 结论

- 总体: Pass
- 恢复条件: N/A
- 合并: 待用户授权
