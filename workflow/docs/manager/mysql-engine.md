# 工作项记录: mysql-engine

工作项标识: mysql-engine
描述: 为 ggtest 增加 MySQL 数据库引擎：`--engine mysql` 经 `com.mysql:mysql-connector-j` 驱动连接 MySQL 服务；按 schema（database）隔离以支持跨文件/`--parallel` 并行；与现有 `--halt`/`--parallel`/`conn=<name>`/凭据脱敏等正交。
目标分支: main
文档影响: README（引擎表/`--engine` 行/`GGTEST_MY_*` 段）、`--help`、pom（新增 `com.mysql:mysql-connector-j` 依赖）。

## 切片门禁

| sub-feature-id | 路径等级 | 源分支 | Spec | Spec 门禁 | Spec 用户确认 | Design 门禁 | Review 门禁 |
|---|---|---|---|---|---|---|---|
| mysql-engine | full | main | [spec.md](./../features/mysql-engine/spec.md) | required | required | required | required |

## 切片状态

| sub-feature-id | 状态 | 后续步骤 | 阻塞原因 | 恢复条件 | 恢复后目标 |
|---|---|---|---|---|---|
| mysql-engine | awaiting-plan-approval | 用户确认 Plan | | | |

## 进度笔记

### 勘察（2026-08-12）

- MySQL 8.4.10 服务 `localhost:3306`，用户 jason
- JDBC 驱动：`com.mysql:mysql-connector-j:9.2.0`（Maven Central，SPI `com.mysql.cj.jdbc.Driver`，shade `ServicesResourceTransformer` 自动合并）
- URL：`jdbc:mysql://localhost:3306`
- 实测：`CREATE SCHEMA IF EXISTS` + `USE` + `DROP SCHEMA IF EXISTS` 可用；`DROP SCHEMA` 无需 `CASCADE`（MySQL 自动级联删除库内对象）；`getString`+`wasNull` 正常；autoCommit=true
- 隔离镜像 PG/Xugu：prepare = CREATE SCHEMA + USE；setSearchPath = USE；teardown = DROP SCHEMA IF EXISTS
