# QA Report: tighten-cli-boundary-validation

## 轮次

| 轮次 | 日期 | 实现版本 | 环境 | 范围 | 结论 |
|---|---|---|---|---|---|
| 1 | 2026-08-21 | `ab4cbba5d873611b5bc9a803807a08f4271f554d`（源分支 `tighten-cli-boundary-validation`） | Java 17 / Maven；Xugu driver 本机存在，实库连接不可达 | 首测 | Pass |
| 2 | 2026-08-21 | `b71b6bafdd99e43b50ab2836486167d3280a70e3`（源分支 `tighten-cli-boundary-validation`） | Java 17 / Maven；Xugu driver 本机存在，实库连接不可达 | 合并前说明修正复验 | Pass |
| 3 | 2026-08-21 | `f92e437f92868740ed3aa34f5d07ee17c14d3281`（实现与用户文档整理后；关闭提交仅含工作流记录） | Java 17 / Maven；Xugu driver 本机存在，实库连接不可达 | 合并授权与提交整理复验 | Pass |

## 执行命令

| 命令 | 输出摘要 / 证据位置 |
|---|---|
| `mvn -q -Dtest=SqlLogicTestRunnerTest test` | exit 0 |
| `mvn -q test` | exit 0 |
| `mvn -q -Dtest=CliArgumentParserTest,RuntimeConfigResolverTest,XuguCliIntegrationTest test` | exit 0 |
| `git merge-base --is-ancestor main HEAD` | exit 0；源分支可 fast-forward 合入目标分支 |
| `env GGTEST_XG_URL=jdbc:xugu://127.0.0.1:5138/SYSTEM?char_set=utf8 GGTEST_XG_USER=SYSDBA GGTEST_XG_PASSWORD=*** mvn -q -Dtest=XuguJdbcExecutorTest,XuguSchemaIsolationTest,XuguCliIntegrationTest test` | 未通过；JDBC driver 连接阶段抛 `NullPointerException: socketChannel is null`，CLI 路径已归一化为 `connection failed` |
| `/home/jason/xgspace/XuguDB/Client/xgconsole/xgconsole -s nssl -h 127.0.0.1 -P 5138 -d SYSTEM -u SYSDBA -p *** -e "select 1;"` | 未通过：`Connection information error, connection failed.` |

## 覆盖（对照 Spec 验收与 Plan 验证）

| ID | 条目 | 结果 | 证据 |
|---|---|---|---|
| V-1 | `--hash-threshold -1` | Pass | `CliArgumentParserTest.negativeHashThresholdYieldsUsageError` 覆盖 usage error 与非负约束消息 |
| V-2 | `.env` / env 中 `GGTEST_HASH_THRESHOLD=-1` | Pass | `RuntimeConfigResolverTest.negativeHashThresholdFromProcessEnvYieldsUsageError` 与 `negativeHashThresholdFromDotEnvYieldsUsageError` 覆盖 |
| V-3 | `--override --separator ""` | Pass | `CliArgumentParserTest.separatorEmptyYieldsUsageError` 覆盖 usage error 与非空约束消息 |
| V-4 | Xugu 不可达连接带非空密码 | Pass | `XuguCliIntegrationTest.nonEmptyPasswordNeverPrintedWhenXuguConnectionFails` 覆盖 exit 2、`connection failed` 与密码不回显 |
| V-5 | `mvn -q test` | Pass | 默认测试套件 exit 0 |
| V-6 | 用户提供 Xugu 环境 gated 测试 | Pass with documented gap | gated 测试和 `xgconsole` 均无法连接；环境缺口已写入 `dev-notes.md`，本项自动化覆盖不可达连接归一化 |
| D-1 | `--hash-threshold 0` 的优先级说明 | Pass | `select1.test` 首行 `hash-threshold 8` 可覆盖 CLI 初始 0；README / README.zh-CN / `--help` 已说明，`SqlLogicTestRunnerTest.initialHashThresholdZeroCanBeReenabledByHashThresholdRecord` 覆盖 |
| D-2 | 合并前提交整理规则 | Pass | `workflow/agents/standards/git.md` 已新增合入前提交整理规则；源分支整理为实现、CLI 文档、工作流关闭三类提交 |

## 回归

| 范围 | 结果 | 证据 |
|---|---|---|
| CLI 参数边界 | Pass | parser/resolver 目标测试 exit 0 |
| hash-threshold 指令优先级 | Pass | runner 目标测试 exit 0 |
| Xugu 连接失败错误映射 | Pass | `XuguCliIntegrationTest` exit 0，输出归一化为 `connection failed` 且脱敏 |
| 默认项目回归 | Pass | `mvn -q test` exit 0 |
| 合入前提交整理 | Pass | 用户授权后整理为少量语义提交；整理后重新执行目标测试与默认套件 |

## 文档与安全验收

| 项 | 结果 | 备注 |
|---|---|---|
| 用户可见文档 | Pass | `README.md`、`README.zh-CN.md` 已同步 Xugu 支持、测试门变量与 CLI 边界说明 |
| 运维可执行文档 | N/A | 本项无运维文档变更 |
| 安全验证范围 | Pass | 密码在测试命令、工作流记录与失败输出中脱敏；非空密码不回显由集成测试覆盖 |

## 缺陷

| ID | 严重度 | 摘要 | 状态 | 处理说明 | 验证证据 |
|---|---|---|---|---|---|
| — | — | 无 | — | — | — |

## 阻塞（Blocked 时必填）

- 原因: N/A
- 风险: Xugu 真实成功连接路径未在本轮通过，风险限定为环境可达后的正向实库路径；不可达错误归一化已自动化覆盖。
- 恢复条件: Xugu 服务按 `127.0.0.1:5138/SYSTEM` 可被 `xgconsole` 成功执行 `select 1`。
- 复测范围: `XuguJdbcExecutorTest`、`XuguSchemaIsolationTest`、`XuguCliIntegrationTest` gated 测试。

## 结论

- 本轮结论: Pass
- 合并: 已授权（Manager 置 `done` 后合入）
