# QA Report: fix-pg-teardown-once

## 轮次

| 轮次 | 日期 | 实现版本 | 范围 | 结论 |
|---|---|---|---|---|
| 1 | 2026-07-26 | `393f712` @ `fix-pg-teardown-once` | P0-A…C + L2 回归 | **Pass** |

## 环境与命令

- 工作区：`/Users/zhougangjie/Space/ggtest/.worktrees/fix-pg-teardown-once`
- 门禁：fast；Review **skipped**；Plan **approved**（2026-07-26）
- `GGTEST_PG_URL`：未设置（PG 门控测 skip，非 fail）

| 命令 | 结果 |
|---|---|
| `git rev-parse HEAD` | `393f71289bb269c7a932fc63da45cf12446f58bf` |
| `mvn -q clean test` | BUILD SUCCESS；196 run / 0 fail / 0 error / 17 skip |

## 覆盖（对照 plan 验收 P0-A…C）

| ID | 条目 | 结果 | 证据 |
|---|---|---|---|
| P0-A | prepare 成功后，无论 runner/teardown 成败，`PostgresSchemaIsolation.teardown` 对该 schema **至多一次** | **Pass** | `CliSession#runPostgresFile` 内 `PostgresSchemaIsolation.teardown` 仅 **一处**（`finally`，约 206 行）；try 仅 `prepare` + `runWithExecutor`；`prepare` 失败时 `schema` 仍为 null，`finally` 不 teardown |
| P0-B | teardown 失败仍 `hardFailure`；成功路径 outcome 映射不变 | **Pass** | `teardownException != null` → `FileOutcome.hardFailure`（213–223 行，保留 `outcome.detailLines` 合并语义）；否则 `outcome.hardError()` / 原 outcome（225 行） |
| P0-C | `mvn -q clean test` 通过 | **Pass** | 见上表；surefire 汇总与 dev-notes 一致 |

## 回归

| 范围 | 结果 | 证据 |
|---|---|---|
| 全量单元/集成套件（L2） | **Pass** | 196 run / 0 fail / 0 error / 17 skip |
| PG 门控（`PostgresCliIntegrationTest`、`PostgresSchemaIsolationTest`、`PostgresJdbcExecutorTest`） | **Skip**（预期） | 无 `GGTEST_PG_*`；非 fail |

## 文档与安全

| 项 | 结果 |
|---|---|
| `dev-notes.md` | 存在；验证摘要与 QA 实测一致 |
| `code-audit-register.md` CA-006 | **resolved** |
| 安全（`security.md`） | **N/A** — 内部控制流整理；无认证/授权/凭据/外部访问变更；合并授权前无额外安全风险 |

## 已知未实跑（非阻塞）

| 未验证项 | 原因 | 风险 | 恢复条件 |
|---|---|---|---|
| 实库 teardown 失败路径 | 无 `GGTEST_PG_*` | 低；P0-B 已由代码审查确认 | 提供可达 PG 后重跑门控测 |

## 缺陷

无。

## 结论

- **总体：Pass**
- 恢复条件：N/A
- 合并：**待用户授权**（QA 不 commit/merge/push）
