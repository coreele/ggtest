# Design: enhance-override

## 背景与约束

`--override` 现状只在「期望与实际不一致」时回写期望体（`RecordResult.overridden` + `OverrideWriter.applyQueryOverride`/`applyStatementOverride`）。类型签名不匹配（`ResultSorter.normalizeAndSort` 抛 `row width != type signature length`）与 SQL 执行失败都直接 `FAILED`。

约束：不改 .slt 解析与测试执行路径；`com.ggtest.normalize` 保持 JDBC-free（`ResultComparer` 已声明「No JDBC dependency」）；保留 `OverrideWriter` 的原子写、EOL 保留、按 startLine 倒序应用。

## 方案对比与决策

**决策 1 —— 类型推断放 `normalize` 包（JDBC-free）**
- 新增 `TypeSignatureInferer.infer(List<List<String>> rows) -> List<ColumnType>`：值驱动，每列非 NULL 值全整数→`I`、否则全实数→`R`、否则→`T`；全 NULL 或零行→`T`。
- 备选放 `cli`：否决——类型语义归属 normalize（`ColumnType`/`ValueNormalizer` 所在）。
- runner 无 ResultSetMetaData（executor 只回 `rows`），故回退直接默认 `T`；JDBC 元数据回退留给 `sql-to-slt`（有原始 JDBC）。

**决策 2 —— override 意图经 `RecordResult` 扩展字段传递**
- `RecordResult` 增 `Optional<String> overrideSignature`（签名改写）与 `boolean overrideAsStatementError`（记录类型转换）；保留 `overrideText`。
- 备选 sealed `OverrideSpec`：否决——破坏既有 `Override` 构造与 `OverrideWriterTest`，改动面大。

**决策 3 —— `OverrideWriter` 增两类操作**
- 签名改写：改写 query 头类型签名（并可注入 `separator=<delim>` 属性）；期望体仍走既有 `applyQueryOverride`。
- 记录转换：`query`/`statement ok` → `statement error <msg>`。query 转换时删除 `----`+期望块；statement ok 转换时把 `ok` 改为 `error <msg>`（保留 timeout/conn 属性）。

**决策 4 —— separator 参数**
- 新 flag `--override-separator <delim>`（`CliArgumentParser`→`ParsedArguments`→`CliOptions`）。
- 指定时：override 的 query 头写 `separator=<delim>`、期望按行式（行内列以 `delim` 连接）；未指定：value-per-line（现有行为）。`--override-separator` 优先于 query 头已声明的 separator。

## 模块边界

| 层 | 变更 |
|---|---|
| `normalize/TypeSignatureInferer`（新） | 值驱动类型推断，无 JDBC |
| `runner/RecordResult` | 增 `overrideSignature`、`overrideAsStatementError` |
| `runner/SqlLogicTestRunner` | `runQuery`：签名不匹配→推断+改写；执行失败→statement error；`runStatement`：ok 失败→statement error；`formatOverrideText` 支持 separator 与新列数 |
| `cli/OverrideCoordinator` | 映射 RecordResult → 富 `Override` |
| `cli/OverrideWriter` | 签名改写、记录转换、separator 属性注入 |
| `cli/CliArgumentParser`、`ParsedArguments`、`CliOptions`、`Main` | `--override-separator` |

## 影响与风险

- 类型推断边界：全 NULL 列/空结果集→`T`（值驱动无法更精）。
- 签名改写需正确计算 query 头行的类型签名 token 位置（`query` 后第一个 token）。
- query→statement error 需删除 `----`+期望块，避免残留；按 startLine 倒序应用不受影响。
- `--override-separator` 仅在 `--override` 下生效，单独使用报 `UsageException`。
