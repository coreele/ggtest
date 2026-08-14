# Review: fix-override-direct-write

## 审阅范围

- 实现版本 / 提交: `f1c9ad9`（源分支 `fix-override-direct-write`）
- 依据: 用户需求（`--override` 直接覆盖 + `--separator` 优先级）；fast 路径

## 实现正确性

- **req1（直接覆盖）**：`runQuery` 中 `overrideEnabled && hasExpectedResults && !labelConflict` → 一律 `OVERRIDDEN`，`formatOverrideText(record, comparison.actualView())`。移除了 `resultMismatch` 与旧 pass-only force-reformat 分支。pass 与 mismatch 都覆盖（内容幂等）。✓
  - label conflict 仍 FAILED（真错误）；execute-only（无 `----`）仍 PASS。✓
- **req2（separator 优先级）**：`effectiveSeparator = overrideSeparator.or(record.columnSeparator())`（body 用 CLI 值）；coordinator 按有效列数 > 1 传 CLI separator；`OverrideWriter.rewriteQueryHeader` 替换已有 `separator=\S+`。端到端验证 `separator=,` → `separator=|`。✓
- 范围守纪律：仅 `runQuery` 的覆盖分支 + 测试；未改解析/归一化/语句覆盖语义。

## 测试有效性

- 更新 4 个断言旧行为的测试（pass → OVERRIDDEN、mtime 断言移除、二次 run OVERRIDDEN、[OVERRIDDEN, OVERRIDDEN]）；新增 `overrideEnabled_separatorOverridesHeaderSeparator`。
- `mvn test` 407/0（50 既有 skip）；`mvn spotbugs:check` 通过；端到端两项需求验证通过。

## 文档影响核对

| 声明 | 实现是否一致 | 备注 |
|---|---|---|
| README `--override` 一律覆盖 | 一致 | — |
| README `--separator` 覆盖 header、单列不生效 | 一致 | — |

## 安全影响核对

| 检查项 | 结果 | 备注 |
|---|---|---|
| 敏感信息 | 无 | 无凭据/依赖变化 |
| 输入与外部访问 | 无 | `--separator` 校验不变 |

## 必修项

| ID | 位置 | 问题 | 状态 |
|---|---|---|---|
| — | — | 无阻塞项 | — |

## 结论

Approve

## 后续动作与复审范围

- 进 QA；QA Fail 修复后复审，范围限 `SqlLogicTestRunner.runQuery` 及测试。
