# Dev Notes: fix-pg-teardown-once

## 实现说明

- 分支：`fix-pg-teardown-once` → `main`。
- **T1**：`CliSession#runPostgresFile` 去掉 try 内 `PostgresSchemaIsolation.teardown`；`schema != null` 时仅在 `finally` 调用一次。用 `outcome` / `teardownException` 在 try-finally 之后决定返回值（teardown 失败 → `hardFailure`；否则沿用 hardError / pass / skip 映射）。`prepare` 失败时 `schema` 仍为 null，finally 不 teardown。
- **T2**：全量回归；登记册 CA-006 → `resolved`。

### 变更路径

| 路径 | 变更 |
|---|---|
| `src/main/java/com/ggtest/cli/CliSession.java` | 仅 `runPostgresFile` 控制流 |
| `workflow/audit/register.md` | 新建；CA-006 → `resolved` |
| `workflow/archive/2026/fix-pg-teardown-once/dev-notes.md` | 本文件 |

未触碰：`sanitize`、`CliOptions`、其它方法。

### TDD 说明

内部控制流整理，无新对外 CLI 合同；`CliSession` 无独立单元测。以全量 `mvn -q clean test` 作 L2 回归证据。

### 验证

| 命令 | 结果 |
|---|---|
| `mvn -q clean test` | BUILD SUCCESS；**196** run / **0** fail / **0** error / **17** skip |

PG 门控测在无 `GGTEST_PG_*` 时 skip（非 fail），与既有行为一致。

### 未解决风险

| 未验证项 | 原因 | 风险 | 恢复条件 | 复测范围 |
|---|---|---|---|---|
| 实库 teardown 失败路径 | 无 `GGTEST_PG_*` | 控制流由代码审查确认；实库 DROP 失败未实跑 | 提供可达 PG | 既有 `Postgres*` 门控测 |

## 交接

- Review：skipped（fast）。
- 建议后续：**QA**（P0-A…C）。
