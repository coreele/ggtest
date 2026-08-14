# Review: mysql-engine

## 审阅范围

- 实现版本 / 提交: `134bad3` `feat(mysql): add MySQL engine (--engine mysql)`（已是 `main` 祖先）
- 依据: `plan.md`；`spec.md`；`design.md`
- 说明: 2026-08-14 回顾性审阅。原周期未留存 `review.md`；对照当前 `main` 树与 Spec/Plan。

## 实现正确性

对照 Spec 合同：`--engine mysql`、`jdbc:mysql:` 前缀、`MySqlJdbcExecutor` 继承 `AbstractJdbcExecutor`、`MySqlSchemaIsolation`（CREATE SCHEMA / USE / DROP SCHEMA IF EXISTS）、`FileRunner` 将 MySQL 纳入 schema 隔离、驱动 `mysql-connector-j`。与 postgres/xugu 扩展点一致，未见越界改 parser/normalize。

后续 `fix-ca018-search-path-validation` 已给 `setSearchPath` 补标识符校验并抽取 `SchemaNames`，不否定本项交付。

## 测试有效性

存在 `MySqlJdbcExecutorTest`、`MySqlSchemaIsolationTest`、`MySqlCliIntegrationTest` 与 `fixtures/my/`。无服务 `mvn test`：407/0/0/50 skip。Live 集成门控于 `GGTEST_MY_*`，本轮未复跑。

## 文档影响核对

| Plan 声明 | 实现是否一致 | 备注 |
|---|---|---|
| 开发文档 | 是 | README 含 MySQL |
| 用户文档 | 是 | `--engine mysql` 示例与 help |
| 运维文档 | N/A | 无部署文档要求 |

## 安全影响核对

| 检查项 | 结果 | 处置状态 | 备注 |
|---|---|---|---|
| 敏感信息 | 通过 | 无新硬编码凭据 | 沿用既有脱敏；测试门控密钥不入白名单 |
| 认证与授权 | N/A | — | 无认证模型变更 |
| 输入与外部访问 | 通过 | URL/engine 校验扩展 | JDBC 出站仅用户配置的 URL |
| 依赖变更 | 通过 | Maven Central 公开驱动 | `com.mysql:mysql-connector-j:9.2.0` |

## 必修项

| ID | 位置 | 问题 | 状态 |
|---|---|---|---|
| — | | 无 | |

## 非阻塞建议

Live MySQL 套件依赖环境门控；合入后的回归以无服务 `mvn test` 与门控 skip 为准。

## 结论

Approve

> 回顾性结论：实现已在目标分支，用户确认开发完成。

## 后续动作与复审范围

无。工作项关闭并归档。
