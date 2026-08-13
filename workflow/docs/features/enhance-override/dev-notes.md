# Dev Notes: enhance-override

## 实现摘要

- 分支：`enhance-override` ← `main`。
- 强化 `--override` 三类自动修复 + separator 参数（standard，Spec/Design/Plan 已确认）。

## 变更

1. **类型推断**：新增 `com.ggtest.normalize.TypeSignatureInferer.infer(rows)`（值驱动、JDBC-free）：每列非 NULL 值全整数→I、否则全实数→R、否则→T；全 NULL/空集→T。
2. **runner**（`SqlLogicTestRunner`）：
   - `runQuery` 执行失败 → `RecordResult.overriddenAsStatementError(record, errorSummary)`；
   - `runQuery` 签名不匹配（`IllegalArgumentException`）→ 推断签名 + `overrideBodyFor`（经 `ResultComparer.compare` 空期望重算 actualView，复用 hash/sort）+ `overridden(record, body, signature)`；
   - `runStatement` `ok` 失败 → `overriddenAsStatementError`；
   - `formatOverrideText` 重构为「(actualView, columns, separator)」，`effectiveSeparator = overrideSeparator.or(record.columnSeparator())`。
3. **RecordResult**：增 `overrideSignature`、`overrideAsStatementError`（保留旧构造/factory 向后兼容）。
4. **OverrideWriter**：`Override` 增 `newSignature`/`separator`/`toStatementError`；新增 `rewriteQueryHeader`（签名 + separator 属性，`Matcher.quoteReplacement` 防分隔符正则元字符）、`convertQueryToStatementError`（改头 + 删 `----`+期望块）、`convertStatementToError`（`statement ok`→`statement error`）。
5. **CLI**：`--override-separator <delim>`（`CliArgumentParser`→`ParsedArguments`→`CliOptions`），校验无空白、且须 `--override`；`Main.printHelp` 补行。
6. **OverrideCoordinator**：`collectOverrides(result, overrideSeparator)` 映射 RecordResult → 富 Override。

## 验证

| 命令 | 结果 |
|---|---|
| `mvn test` | Tests=**405** Failures=0 Errors=0 Skipped=50；BUILD SUCCESS |
| 端到端（SQLite） | `query T` 2 列 → `query IT` + 值逐行；执行失败 → `statement error`（`----` 删）；生成后重跑 PASSED |
| separator | `--override-separator "|"` → `query IT separator=|` + 行式 `1 | apple` |

新增测试：`TypeSignatureInfererTest`（7）、`CliArgumentParserTest`（3）、`MainOrchestrationTest`（签名对齐、separator）；并更新 4 个断言旧行为的测试（执行失败/ok 失败原 FAILED → 现 OVERRIDDEN）。

## 决策与边界

- 类型推断回退默认 `T`：runner 仅有 rows、无 ResultSetMetaData；JDBC 元数据回退留给 `sql-to-slt`。
- statement error 消息写原始 errorSummary（业务 SQL 错误，不含连接串；与既有 `statement error` override 一致，未新增脱敏链路）。
- `statement error <msg>` 的 msg 若含 `=` 会被 parser 误判为属性起点（既有 Case E 同局限，边角场景）。
- `--override-separator` 仅对 query 生效；statement 记录不受影响。

## 文档影响

| 类别 | 已更新 |
|---|---|
| 开发文档 | README.md 选项表 `--override`/新增 `--override-separator`、synopsis |
| 用户文档 | 同上 |
| 运维文档 | N/A |

## 未解决风险 / 验证缺口

| 项 | 原因 | 风险 | 恢复条件 |
|---|---|---|---|
| statement error msg 含 `=` | parser 以 `=` 定位属性边界 | 生成 golden 回读误解析 | 用户手动调整该 msg |

## QA 修复回执

| 缺陷 ID | 处理 | 摘要 | 验证 | 建议复测 |
|---|---|---|---|---|
| — | N/A | 本轮无 QA Fail | — | — |
