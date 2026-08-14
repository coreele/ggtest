# Dev Notes: mysql-engine

## 实现摘要

MySQL 引擎已合入 `main`：`134bad3` `feat(mysql): add MySQL engine (--engine mysql)`。镜像 postgres/xugu 模式：`MySqlJdbcExecutor`、`MySqlSchemaIsolation`、`FileRunner` 隔离接线、`--engine mysql` + `jdbc:mysql:` 校验、Maven Central 驱动 `mysql-connector-j:9.2.0`。

本文件为 2026-08-14 工作流补档：当时未留存 `dev-notes.md`。

## 变更路径

新建：`db/mysql/{MySqlJdbcExecutor,MySqlSchemaIsolation}.java`、对应测试、`MySqlCliIntegrationTest`、`fixtures/my/*`。
修改：`cli/{RuntimeConfigResolver,FileRunner,Main}.java`、`pom.xml`、`README.md`、`RunnerDependencyIsolationTest`。

## 测试先行记录（TDD）

| Spec ID / 行为项 | 测试 | 先失败 | 后通过 | 说明 |
|---|---|---|---|---|
| P0-1 引擎/URL | `RuntimeConfigResolverTest` | 未单独留红灯记录 | 合入后仍在套件中 | 补档 |
| P0-2/P0-3 E2E 与失败 | `MySqlCliIntegrationTest` + `fixtures/my/{basic,fail}.test` | 未单独留红灯记录 | 门控于 `GGTEST_MY_*` | 补档 |
| P0-4 NULL / skipif / 脱敏 | executor 与 CLI 测试 | 未单独留红灯记录 | 门控 | 补档 |
| P1 隔离/并行/conn/halt | CLI 集成 + `MySqlSchemaIsolationTest` | 未单独留红灯记录 | 门控 | 补档 |

## 验证

| 命令 | 验证层 | 结果摘要 / 证据 |
|---|---|---|
| `mvn test`（2026-08-14，无 `GGTEST_MY_*`） | unit + build | Tests run: 407, Failures: 0, Errors: 0, Skipped: 50；BUILD SUCCESS |
| `git merge-base --is-ancestor 134bad3 main` | — | 是；实现已在目标分支 |

## 目标分支同步（最终 Review 前）

- 目标分支及提交: `main`（当前 HEAD 含 `134bad3`）
- 同步后源分支 HEAD: 实现已直接出现在 `main`；独立源分支 `mysql-engine` 现已不存在
- 同步方式: N/A（历史合入，补档时源分支已删除）
- 冲突及处理: N/A
- 同步后复验: 见上表 `mvn test`

## 文档影响

| 类别 | 已更新路径或交接说明 |
|---|---|
| 开发文档 | README 引擎表 / `--engine mysql` / JDBC 示例 |
| 用户文档 | README、`--help` |
| 运维文档 | N/A（测试门控环境变量，无部署变更） |

## 未解决风险 / 验证缺口

| 项 | 原因 | 风险 | 恢复条件 |
|---|---|---|---|
| 本轮未复跑 live MySQL 集成 | 补档环境未设置 `GGTEST_MY_*` | 门控用例 skip；合入时的 live 证据不在本工作树 | 设置 `GGTEST_MY_*` 后重跑 `MySqlCliIntegrationTest` |
