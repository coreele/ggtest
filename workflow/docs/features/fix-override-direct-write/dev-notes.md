# Dev Notes: fix-override-direct-write

## 实现摘要

- 分支：`fix-override-direct-write` ← `main`。
- 修复 `--override` 两处行为（fast，Spec/Design skipped）。

## 变更

1. **`--override` 直接覆盖**：`SqlLogicTestRunner.runQuery` 中，`overrideEnabled && hasExpectedResults && !labelConflict` 时**一律**返回 `OVERRIDDEN`（`formatOverrideText(record, comparison.actualView())`），不再区分 pass/mismatch。移除 `resultMismatch` 标志与旧 force-reformat 分支。结果：即便 query 原期望与实际一致（pass），也标 OVERRIDDEN 并重写（内容幂等不变）。
   - label conflict 仍 FAILED（真错误，不覆盖）；execute-only query（无 `----`）仍 PASS。
2. **`--separator` 覆盖 header**：`--separator <delim>` 已优先于 query header 的 `separator=<delim>`（`effectiveSeparator = overrideSeparator.or(record.columnSeparator())`；`OverrideWriter.rewriteQueryHeader` 替换已存在的 `separator=\S+`）。本次确认该路径在「PASS 也覆盖」下同样生效（端到端验证 `separator=,` → `separator=|`）。

## 验证

| 命令 | 结果 |
|---|---|
| `mvn test` | Tests=**407** Failures=0 Errors=0 Skipped=50；BUILD SUCCESS |
| 端到端 | 全绿文件 `--override` → `overridden=1`（原 PASS）；`--override --separator "|"` → `separator=|` + `1 | apple` 行式 |

更新测试：`FileRunnerTest.overrideEnabled_allPassed_fileRewrittenAsOverridden`、`MainOrchestrationTest.overrideEnabled_noMismatch_fileContentUnchanged`（去掉 mtime 断言）、`overrideThenRerun_isIdempotent`（二次 run 为 OVERRIDDEN）、`SqlLogicTestRunnerTest.overrideEnabled_doesNotTriggerHalt`（[OVERRIDDEN, OVERRIDDEN]）；新增 `overrideEnabled_separatorOverridesHeaderSeparator`。

## 文档影响

| 类别 | 已更新 |
|---|---|
| 开发文档 | README `--override`（一律覆盖）/`--separator`（覆盖 header、单列不生效） |
| 用户文档 | 同上 |
| 运维文档 | N/A |

## 未解决风险 / 验证缺口

| 项 | 原因 | 风险 | 恢复条件 |
|---|---|---|---|
| N/A | | | |

## QA 修复回执

| 缺陷 ID | 处理 | 摘要 | 验证 | 建议复测 |
|---|---|---|---|---|
| — | N/A | 本轮无 QA Fail | — | — |
