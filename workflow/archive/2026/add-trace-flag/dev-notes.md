# Dev Notes: add-trace-flag

## 实现摘要

T1-T4 完成（当天），`--trace` CLI 标志，在执行前将每条 SQL 语句打印到 stderr。

- **T1 CLI 链**：`CliArgumentParser`（`case "--trace" -> trace = true`）、`ParsedArguments`（`boolean trace` 字段）、`CliOptions`（新增 `trace` 字段，提供向后兼容的便捷构造函数）、`RuntimeConfigResolver`（传入 `parsed.trace()`）。
- **T2/T3 runner 注入**：`SqlLogicTestRunner` 获取一个 nullable 的 `traceStream`（通过 `setTraceStream(PrintStream)` 进行 setter）；`runStatement`/`runQuery` 在执行器调用之前调用 `trace(record.sql())`。`FileRunner` 执行 `runner.setTraceStream(options.trace() ? err : null)`。
- **T4 README/回归**：选项表中增加 `--trace` 行，usage 线上增加 `[--trace]`。

## 设计注意事项

- 追踪输出使用 `FileRunner.err`（stderr），与现有的 `schema isolation failed` 等诊断信息来源相同。不影响 stdout 测试报告格式。
- runner 的 `trace()` 方法在 `traceStream` 为 null 时是无操作的（默认状态），因此不带 `--trace` 的情况是零开销的。
- 现有的 SqlLogicTestRunner 构造函数保持不变；追踪 setter 在构造后调用。
- 在并行模式下，每条文件的 `FileRunner` 都拥有同一个 `err` PrintStream（在 `CliSession` 中连接），因此追踪行可以交错——这对于诊断用途来说是可以接受的。

## 开发者验证

- `mvn -q test` → 343 条测试，0 次失败，0 个错误
- `java -jar ... --trace pass.test` → stderr 上显示 `CREATE TABLE t(x int);` / `INSERT INTO t VALUES(1);` / `SELECT x FROM t`；stdout 上显示 `[PASSED]` + TOTAL。
- 不带 `--trace` 的情况下：stdout 和 stderr 与之前相同。
