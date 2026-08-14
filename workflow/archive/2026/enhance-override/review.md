# Review: enhance-override

## 审阅范围

- 实现版本 / 提交: `3034c39` → `c0f21ab`（源分支 `enhance-override`，含 3 轮：原始实现 + rename/force-reformat + 单列省略 separator）
- 依据: [spec.md](spec.md)（含 r2 增补）、[design.md](design.md)、[plan.md](plan.md)；standard 路径

## 实现正确性

### r1 核心功能
- **类型签名对齐（P0-1/P0-2）**：`runQuery` 捕获 `ResultComparer.compare` 抛出的 `IllegalArgumentException`（仅 row-width 不匹配路径）→ `TypeSignatureInferer.infer(rows)` + `overrideBodyFor`（复用 normalize/sort/hash）+ `overridden(record, body, signature)`。✓
- **执行失败 → statement error（P0-3/P0-4）**：`runQuery`/`runStatement(OK)` 失败且 override 时 `overriddenAsStatementError`；`convertQueryToStatementError` 改头 + 删 `----`+期望块；`convertStatementToError` 字面 `replace("statement ok", …)`。✓
- **separator（P0-5/P0-6）**：`--separator` 经 `CliArgumentParser`（无空白 + 须 `--override`）→ `CliOptions`；`OverrideWriter` 注入 `separator=<delim>`（`Matcher.quoteReplacement` 防正则元字符）。✓

### r2 增补（`de5ce0a`）
- **rename**：`--override-separator` → `--separator`；CLI/help/README/tests 全链路同步。✓
- **PASS 也重写（P0-7）**：`runQuery` 的 `failures.isEmpty()` 分支增 force-reformat：`overrideEnabled && separator.isPresent() && hasExpected` → `overridden(record, row-wise body)`。原 PASS 的 query 也被行式重写。✓

### r3 优化（`c0f21ab`）
- **单列省略 separator**：runner force-reformat 仅 `typeSignature.size() > 1` 才 override；coordinator 按有效列数（`overrideSignature.length` 或 `typeSignature.size()`）> 1 才传 separator 给 OverrideWriter。单列保持 value-per-line、不注入 `separator=`。✓

### 通用边界
- 签名不匹配仅发生于 rows 非空（row-width 检查），故 `infer` 不会产出空签名。✓
- statement error 消息写原始 errorSummary（业务错误，不含连接串，与既有 Case E 一致）。✓
- 范围守纪律：不改解析/比较/归一化语义；既有 `--override` 不匹配回写与 statement error msg 回写保留。✓

## 测试有效性

- `TypeSignatureInfererTest`（7：I/R/T、NULL 不约束、全 NULL→T、空集、负/大整数）。
- `CliArgumentParserTest`（3：`--separator` 解析、须 override、无空白）。
- `MainOrchestrationTest`（签名对齐、separator 行式、PASS 强制重写）。
- 更新 4 个断言旧行为的测试（执行失败/ok 失败原 FAILED → 现 OVERRIDDEN）——属预期行为变更。
- `mvn test` **406**/0（50 既有 skip）；`mvn spotbugs:check` 通过；端到端自洽全绿。

## 文档影响核对

| Plan 声明 | 实现是否一致 | 备注 |
|---|---|---|
| 开发文档 README | 一致 | `--override` 行为更新 + `--separator` 行、synopsis |
| 用户文档 README | 一致 | 同上 |
| 运维文档 N/A | 一致 | — |

## 安全影响核对

| 检查项 | 结果 | 处置状态 | 备注 |
|---|---|---|---|
| 敏感信息 | 无 | n/a | statement error 消息为业务 SQL 错误 |
| 认证与授权 | 无 | n/a | — |
| 输入与外部访问 | 无 | n/a | `--separator` 校验无空白 |
| 依赖变更 | 无 | n/a | 无新依赖 |

## 必修项

| ID | 位置 | 问题 | 状态 |
|---|---|---|---|
| — | — | 无阻塞项 | — |

> 边角（非阻塞）：statement error 的 msg 若含 `=` 会被 parser 误判为属性起点（既有 Case E 同局限）。

## 结论

Approve（r1 + r2 + r3 覆盖审阅）

## 后续动作与复审范围

- QA r1+r2+r3 通过；后续 QA Fail 修复后须复审，范围限 runner/OverrideWriter/TypeSignatureInferer/Coordinator/CLI。
