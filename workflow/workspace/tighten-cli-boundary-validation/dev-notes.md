# Dev Notes: tighten-cli-boundary-validation

## 实现摘要

- `--hash-threshold` CLI / env / `.env` 来源统一拒绝负数；`0` 禁用初始 hash 阈值，文件内 `hash-threshold` 指令仍可重新设置。
- `--separator` 拒绝空字符串与空白字符，避免 `--override` 写出歧义 `separator=`。
- `ConnectionFactory.open` 捕获 JDBC driver 连接阶段逃逸的 `RuntimeException`，包装为 `SQLException`，让 `FileRunner` 统一走 `connection failed` hard-error 路径并继续使用现有脱敏。
- README / README.zh-CN / `--help` 同步 XuguDB 支持矩阵、边界说明与 `hash-threshold` 优先级说明。

## 变更路径

- `src/main/java/com/ggtest/cli/CliArgumentParser.java`
- `src/main/java/com/ggtest/cli/RuntimeConfigResolver.java`
- `src/main/java/com/ggtest/cli/ConnectionFactory.java`
- `src/main/java/com/ggtest/cli/Main.java`
- `src/test/java/com/ggtest/cli/CliArgumentParserTest.java`
- `src/test/java/com/ggtest/cli/RuntimeConfigResolverTest.java`
- `src/test/java/com/ggtest/runner/SqlLogicTestRunnerTest.java`
- `README.md`
- `README.zh-CN.md`

## 测试先行记录（TDD）

| Spec ID / 行为项 | 测试 | 先失败 | 后通过 | 说明 |
|---|---|---|---|---|
| V-1 | `CliArgumentParserTest.negativeHashThresholdYieldsUsageError` | `mvn -q -Dtest=CliArgumentParserTest,RuntimeConfigResolverTest,XuguCliIntegrationTest test`：未抛 `UsageException` | 同命令通过 | CLI 参数来源拒绝负数 |
| V-2 | `RuntimeConfigResolverTest.negativeHashThresholdFromProcessEnvYieldsUsageError`, `negativeHashThresholdFromDotEnvYieldsUsageError` | 同上：env / `.env` 负数未抛 `UsageException` | 同命令通过 | env / `.env` 来源拒绝负数 |
| V-3 | `CliArgumentParserTest.separatorEmptyYieldsUsageError` | 同上：空 separator 未抛 `UsageException` | 同命令通过 | 空字符串参数只能通过程序化 argv 覆盖，测试直接覆盖 parser |
| V-4 | `XuguCliIntegrationTest.nonEmptyPasswordNeverPrintedWhenXuguConnectionFails` | 同上：输出为 `Error: fatal`，未含 `connection failed` | 同命令通过 | 现有不可达 Xugu 用例作为驱动 RuntimeException 归一化红灯 |
| V-5 | 默认回归 | N/A | `mvn -q test` 通过 | 完整默认套件回归 |
| V-6 | Xugu 实库 gated 测试 | N/A | 未通过，见验证缺口 | 本机控制台与 JDBC gated 测试均无法连接到 `127.0.0.1:5138/SYSTEM` |

## 合并前反馈处理

- 2026-08-21：用户用 `--hash-threshold 0` 运行 `sqllogictest/test/select1.test` 后指出仍使用 hash。核对原因：该文件首行 `hash-threshold 8` 会覆盖 CLI 初始阈值；现有语义不是全局强制禁用。已补充 README / README.zh-CN / `--help` 说明，并新增 `SqlLogicTestRunnerTest.initialHashThresholdZeroCanBeReenabledByHashThresholdRecord` 固定该优先级。

## 合并前提交整理

- 用户授权合并时要求压缩源分支提交，并将规则写入 `workflow/agents/standards/git.md`。
- 原源分支多次工作流文档提交已整理为一次工作流关闭提交；实现与用户文档保留语义分界。
- 整理后提交：
  - `50e8c06` `fix(cli): tighten boundary validation`
  - `f92e437` `docs(cli): document boundary validation behavior`
  - 工作流关闭提交：本文件与 `qa-report.md`、STATUS、Git 规范一起提交。

## 验证

| 命令 | 验证层 | 结果摘要 / 证据 |
|---|---|---|
| `mvn -q -Dtest=CliArgumentParserTest,RuntimeConfigResolverTest,XuguCliIntegrationTest test`（实现前） | unit, integration | 预期失败 5 项：负数 hash-threshold CLI/env/.env、空 separator、Xugu 不可达连接 fatal |
| `mvn -q -Dtest=CliArgumentParserTest,RuntimeConfigResolverTest,XuguCliIntegrationTest test`（实现后） | unit, integration | 通过 |
| `mvn -q -Dtest=SqlLogicTestRunnerTest test`（合并前说明修正后） | unit | 通过 |
| `mvn -q -Dtest=SqlLogicTestRunnerTest test`（提交整理后） | unit | 通过 |
| `mvn -q -Dtest=CliArgumentParserTest,RuntimeConfigResolverTest,XuguCliIntegrationTest test`（提交整理后） | unit, integration | 通过 |
| `mvn -q test` | unit, integration, build | 通过 |
| `env GGTEST_XG_URL=jdbc:xugu://127.0.0.1:5138/SYSTEM?char_set=utf8 GGTEST_XG_USER=SYSDBA GGTEST_XG_PASSWORD=*** mvn -q -Dtest=XuguJdbcExecutorTest,XuguSchemaIsolationTest,XuguCliIntegrationTest test` | integration | 未通过：JDBC driver 连接阶段抛 `NullPointerException: socketChannel is null`；CLI 用例已归一化为 `connection failed` hard error |
| `/home/jason/xgspace/XuguDB/Client/xgconsole/xgconsole -s nssl -h 127.0.0.1 -P 5138 -d SYSTEM -u SYSDBA -p *** -e "select 1;"` | manual | 未通过：`Connection information error, connection failed.` |

## 目标分支同步（最终 Review 前）

- 目标分支及提交: `main` `ecc6a8b8d4472b4c765a7465e19bf97d4c123540`
- 同步后源分支 HEAD: `f92e437f92868740ed3aa34f5d07ee17c14d3281`（实现与用户文档整理后；随后追加工作流关闭提交）
- 同步方式: 用户于 2026-08-21 确认目标同步；本地验证 `git merge-base --is-ancestor main HEAD` exit 0，未执行 rebase。
- 冲突及处理: N/A
- 同步后复验: `mvn -q -Dtest=SqlLogicTestRunnerTest test` exit 0；`mvn -q -Dtest=CliArgumentParserTest,RuntimeConfigResolverTest,XuguCliIntegrationTest test` exit 0；`mvn -q test` exit 0。

## 文档影响

| 类别 | 已更新路径或交接说明 |
|---|---|
| 开发文档 | N/A |
| 用户文档 | `README.md`, `README.zh-CN.md` |
| 运维文档 | N/A |

## 未解决风险 / 验证缺口

| 项 | 原因 | 风险 | 恢复条件 |
|---|---|---|---|
| Xugu 实库 gated 测试未通过 | 本机 `xgconsole` 与 JDBC gated 测试均无法连接 `127.0.0.1:5138/SYSTEM` | 真实 Xugu 成功连接路径未在本轮通过；本次修复的不可达连接归一化已自动化覆盖 | Xugu 服务按用户参数可被 `xgconsole -e "select 1;"` 成功连接 |

## QA 修复回执

> QA `Fail` 后按缺陷 ID 追加，不另建文件。

| 缺陷 ID | 处理 | 摘要 | 验证证据 | 建议复测范围 |
|---|---|---|---|---|
| N/A | | | | |
