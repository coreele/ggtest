# Plan: improve-failure-output-format

## 元信息

- 工作项标识: improve-failure-output-format
- 依据 Spec: N/A（fast 且无 Spec 门禁）
- 依据 Design: N/A（Design 门禁为 skipped）
- 路径等级: fast
- Review 门禁: skipped（fast 路径，单点格式调整）
- 最低验证层: L2（单元测试 + 集成测试）
- 验证命令: `mvn -q clean test`

## 适用工程规范

- [文档工程](../../standards/documentation.md)
- [质量与验证](../../standards/quality.md)
- [安全](../../standards/security.md)

## 目标摘要

失败报告输出格式变更：

- 旧: `[FAILED] in X ms` 行 → 缩进 `[WHY]` → `[SQL]` → `[Diff]` → flush `at file:line`
- 新: `[FAILED] in X ms    at file:line : reason`（同行）→ `    (-expected|+actual)`（仅在有 diff 时）→ 无 diff 时不显示 diff 块

## 任务拆解

1. **修改 `ReportWriter.java`**：调整 `detailLines()` 方法——移除 `[WHY]`/`[SQL]`/`[Diff]` 标签，将 `at file:line : reason` 放到不缩进的行（作为 detailLines 的首行），仅在 diffBody 非空时追加 diff 块
2. **更新测试断言**：所有测试中引用 `[WHY]`/`[SQL]`/`[Diff]` 和 flush `at` 行的断言需要更新
3. **`mvn test` 全量通过**

## 依赖与顺序

1 → 2 → 3 顺序执行

## 触碰路径

- `src/main/java/com/ggtest/cli/ReportWriter.java` — `detailLines()` 方法
- `src/test/java/com/ggtest/cli/ReportWriterTest.java` — 直接测试
- `src/test/java/com/ggtest/cli/FileRunnerTest.java` — 引用 `[WHY]`/`[SQL]`/`[Diff]`、flush `at` 断言
- `src/test/java/com/ggtest/cli/CliReportAcceptanceTest.java` — 引用 `[WHY]`/`[SQL]`/`[Diff]`、flush `at` 断言
- `src/test/java/com/ggtest/cli/MainOrchestrationTest.java` — 引用 `[WHY]`/`[SQL]`
- `src/test/java/com/ggtest/cli/EnvConfigIntegrationTest.java` — 引用 `[WHY]`

## 验收

- P0-1: 失败报告行格式为 `.. [FAILED] in X ms    at file:line : reason`
- P0-2: 不再出现 `[WHY]`、`[SQL]`、`[Diff]` 标签
- P0-3: 有 diff 时显示 `    (-expected|+actual)` 及 diff body
- P0-4: 无 diff 时不显示 diff 块（如 hard error）
- P0-5: `at` 行不再 flush 独立成行
- P0-6: `mvn test` 全量通过

## 文档影响

| 类别 | 更新路径或 N/A 理由 |
|---|---|
| 开发文档 | N/A |
| 用户文档 | README.md / README.zh-CN.md（报告示例需同步更新） |
| 运维文档 | N/A |
