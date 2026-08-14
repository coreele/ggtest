# 工作项: mysql-engine

描述: 为 ggtest 增加 MySQL 数据库引擎：`--engine mysql` 经 `com.mysql:mysql-connector-j` 驱动连接 MySQL 服务；按 schema（database）隔离以支持跨文件/`--parallel` 并行；与现有 `--halt`/`--parallel`/`conn=<name>`/凭据脱敏等正交。
目标分支: main
源分支: mysql-engine
基线提交: a6c8719bc48099cf772a6bd1807876dd4577259c
文档影响: README（引擎表/`--engine` 行/`GGTEST_MY_*` 段）、`--help`、pom（新增 `com.mysql:mysql-connector-j` 依赖）。

> **本文件须保存为 `workflow/archive/2026/mysql-engine/mysql-engine.md`**，文件名与目录同名。
> 流程定义见 `workflow/WORKFLOW.md`；看板见 `workflow/STATUS.md`。
> 本工作项的全部产物平铺在 `workflow/archive/2026/mysql-engine/`，无子目录、无版本后缀。
> 表内只填枚举、短标签或路径；理由与长说明写进「进度笔记」。

## 门禁

| 路径等级 | Spec | Spec 用户确认 | Design | Review |
|---|---|---|---|---|
| full | required | approved | required | required |

## 状态

| 状态 | 下一步 | 阻塞原因 | 恢复条件 | 恢复后目标 |
|---|---|---|---|---|
| archived | — |  |  |  |

## 子项（仅 tracking 项填写）

| 子项 id | 状态 |
|---|---|
| — | |

## 进度笔记

### 勘察（2026-08-12）

- MySQL 8.4.10 服务 `localhost:3306`，用户 jason
- JDBC 驱动：`com.mysql:mysql-connector-j:9.2.0`（Maven Central，SPI `com.mysql.cj.jdbc.Driver`，shade `ServicesResourceTransformer` 自动合并）
- URL：`jdbc:mysql://localhost:3306`
- 实测：`CREATE SCHEMA IF EXISTS` + `USE` + `DROP SCHEMA IF EXISTS` 可用；`DROP SCHEMA` 无需 `CASCADE`（MySQL 自动级联删除库内对象）；`getString`+`wasNull` 正常；autoCommit=true
- 隔离镜像 PG：prepare = CREATE SCHEMA + USE；setSearchPath = USE；teardown = DROP SCHEMA IF EXISTS
- 2026-08-14：按 ggnote `WORKFLOW.md` 标准迁移工作流目录（记录与产物合并为同一目录；权威文件改为 `workflow/WORKFLOW.md`）。
- 2026-08-14：用户确认开发已完成。核对 Git：`134bad3` `feat(mysql): add MySQL engine (--engine mysql)` 已是 `main` 祖先；`db/mysql/`、CLI 接线、测试与 `fixtures/my/` 均在目标分支。Plan 已记 Spec 确认于 2026-08-12。原周期未留存 `dev-notes.md` / `review.md` / `qa-report.md`，已补回顾性报告。无服务 `mvn test`：407/0/50 skip。合入已确认 → `done` 并归档。
