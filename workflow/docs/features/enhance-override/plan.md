# Plan: enhance-override

## 元信息

- 工作项标识: enhance-override
- 依据 Spec: workflow/docs/features/enhance-override/spec.md
- 依据 Design: workflow/docs/features/enhance-override/design.md
- 依据 UI: N/A
- 路径等级: standard
- Review 门禁: required
- 最低验证层: L3（单元测试 + 全量回归 + 端到端 `--override` 修复验证）
- 验证命令: `mvn -Dtest=... test`、`mvn clean test`

## 任务拆解

1. **T1 类型推断**（完成条件：`TypeSignatureInferer` 存在且单测覆盖 I/R/T、全 NULL、空集）
2. **T2 RecordResult 扩展 + runner 增强**（完成条件：`runQuery` 签名不匹配/执行失败、`runStatement` ok 失败均产出正确 override 意图；`formatOverrideText` 支持 separator 与新列数）
3. **T3 OverrideWriter 增强**（完成条件：签名改写、query/ok→statement error 转换、separator 属性注入；原子写/EOL/倒序保持）
4. **T4 CLI flag**（完成条件：`--override-separator <delim>` 解析、单独使用报 UsageException）
5. **T5 OverrideCoordinator 接线**（完成条件：RecordResult → 富 Override 映射正确）
6. **T6 端到端 + 文档**（完成条件：`--override` 对类型不匹配/执行失败文件自修复后重跑全绿；README/--help 更新）

## 依赖与顺序

1 → 2 → 3 → 4 → 5 → 6（T4 可与 T3 并行，但都依赖 T2 的数据模型）。

## 触碰路径

- `src/main/java/com/ggtest/normalize/TypeSignatureInferer.java`（新）
- `src/main/java/com/ggtest/runner/RecordResult.java`
- `src/main/java/com/ggtest/runner/SqlLogicTestRunner.java`
- `src/main/java/com/ggtest/cli/OverrideCoordinator.java`
- `src/main/java/com/ggtest/cli/OverrideWriter.java`
- `src/main/java/com/ggtest/cli/CliArgumentParser.java`、`ParsedArguments.java`、`CliOptions.java`、`Main.java`
- 测试：`TypeSignatureInfererTest`（新）、`OverrideWriterTest`、`SqlLogicTestRunnerTest`、`CliArgumentParserTest`、`MainOrchestrationTest`

## 验收与验证

| ID | 要求 | 预期证据 |
|---|---|---|
| V1 | `query T` 实际 2 列 → override 后签名 `TT` 且重跑全绿 | 端到端测试 |
| V2 | 执行失败 → `statement error`（脱敏） | 端到端测试 |
| V3 | `--override-separator "|"` → 头含 `separator=|` + 行式 | 单元 + 端到端 |
| V4 | 未指定 separator 默认 value-per-line | 回归 |
| V5 | `mvn clean test` 0 failures | 全量 |

## 文档影响

| 类别 | 更新路径 |
|---|---|
| 开发文档 | README.md（`--override`/`--override-separator`） |
| 用户文档 | README.md 同上；`Main.printHelp` 行 |
| 运维文档 | N/A |

## 交接顺序

Developer → Reviewer（required）→ QA → 用户授权合并。

## 修订记录

| 日期 | 摘要 |
|---|---|
| 2026-08-13 | 初版（Spec/Design 已定） |
