# Plan: ggtest-cli-help

## 元信息

- 工作项标识: ggtest-cli-help
- 路径等级: fast
- Review 门禁: skipped

## 任务拆解

1. `CliArgumentParser.parse()`: 添加 `--help` / `-h` case，设置 help flag 到 `ParsedArguments`
2. `ParsedArguments`: 添加 `boolean help` 字段
3. `Main.run()`: 在 resolve 之前检查 `parsed.help()`，打印用法后 return 0
4. `Main`: 新增 `printHelp(PrintStream out)` 方法
5. 测试：更新 `CliArgumentParserTest` + `MainOrchestrationTest`

## 触碰路径

| 文件 | 操作 |
|---|---|
| `cli/CliArgumentParser.java` | 添加 --help/-h |
| `cli/ParsedArguments.java` | 添加 help 字段 |
| `cli/Main.java` | printHelp() + help 检查 |
| 测试 `CliArgumentParserTest.java` | help 解析测试 |
| 测试 `MainOrchestrationTest.java` | help 输出测试 |

## 验收

`mvn test` 全绿 + 手动 `./bin/ggtest --help` 打印用法，exit code 0。
