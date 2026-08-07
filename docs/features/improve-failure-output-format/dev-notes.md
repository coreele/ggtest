# Dev Notes: improve-failure-output-format

## 实现说明

### 变更摘要

调整 CLI 失败用例报告输出形式：

| 维度 | 旧格式 | 新格式 |
|------|--------|--------|
| 位置行 | `    [WHY] reason` (缩进) + flush `at file:line` | `    at file:line : reason` (4空格缩进) |
| SQL 行 | `    [SQL] sql` | 移除 |
| Diff 标签 | `    [Diff] (-expected|+actual)` | `        (-expected|+actual)` (8空格缩进，无标签) |
| Diff 体 | `    ` (4空格缩进) | `        ` (8空格缩进) |
| 多失败块间 | 空行分隔 | 无空行，直接堆叠 |
| 无 diff 时 | 仍显示 `[Diff]` 标签 | 不显示 diff 区域 |

### 变更路径

- `src/main/java/com/ggtest/cli/ReportWriter.java` — `detailLines()` 签名移除 `sql` 参数；`at` 行加4空格前缀；diff 加8空格前缀，移除 `[WHY]`/`[SQL]`/`[Diff]` 标签；移除 `sqlFirstLine()`；恢复简单 `printStatusLine()`
- `src/main/java/com/ggtest/cli/CliSession.java` — 恢复原本 FAILED 打印逻辑（`at` 行不再拼到 status 行上）
- `src/main/java/com/ggtest/cli/FileRunner.java` — 移除 `detailLines()` 调用的多余 `sql` 参数；移除失败块间空行分隔

### 测试变更

- `ReportWriterTest.java` — 适配新格式缩进、移除标签断言，新增 `assertBodyIndent` 和 `statusLineWithExtraAppendsAfterTiming` 未使用但保留
- `FileRunnerTest.java` — 移除 `[WHY]`/`[SQL]`/`[Diff]` 断言；`assertAtIndent4` 验证4空格缩进；移除空行分隔断言
- `CliReportAcceptanceTest.java` — 移除标签断言，更新 `at` 行计数逻辑（trim 匹配缩进的 `at`），移除空行分隔验证
- `MainOrchestrationTest.java` — 移除 `[WHY]`/`[SQL]` 断言，使用 `trim().startsWith("at ")` 匹配缩进 `at` 行

### 验证

- 命令: `mvn -q clean test`
- 结果: 251 测试通过，18 跳过（PG 和语料门控）
- 手动验证: fail.test 输出格式对齐预期；multi-fail.test 输出格式对齐预期；bad-parse.test (硬错误) 无 diff 块

## QA 修复回执

| 缺陷 ID | 处理 | 摘要 | 验证 | 建议复测 |
|---|---|---|---|---|
| — | — | — | — | — |
