# 工作项: xugu-engine

描述: 为 ggtest 增加虚谷数据库（XuguDB）引擎支持：`--engine xugu`（别名 `xugudb`）经 `com.xugudb:xugu-jdbc` 驱动连接虚谷服务；按文件 schema 隔离以支持跨文件/`--parallel` 并行；与现有 `--halt`/`--parallel`/`conn=<name>`/凭据脱敏等正交。
目标分支: xgtest
源分支: xugu-engine
基线提交: a6c8719bc48099cf772a6bd1807876dd4577259c
文档影响: 受影响——README（引擎表/`--engine` 行/`GGTEST_XG_*` 测试门变量段）、`--help`；新增 `com.xugudb:xugu-jdbc` 依赖与本地安装说明。

> **本文件须保存为 `workflow/archive/2026/xugu-engine/xugu-engine.md`**，文件名与目录同名。
> 流程定义见 `workflow/WORKFLOW.md`；看板见 `workflow/STATUS.md`。
> 本工作项的全部产物平铺在 `workflow/archive/2026/xugu-engine/`，无子目录、无版本后缀。
> 表内只填枚举、短标签或路径；理由与长说明写进「进度笔记」。

## 门禁

| 路径等级 | Spec | Spec 用户确认 | Design | Review |
|---|---|---|---|---|
| standard | skipped | not-required | skipped | required |

## 状态

| 状态 | 下一步 | 阻塞原因 | 恢复条件 | 恢复后目标 |
|---|---|---|---|---|
| archived | — |  |  |  |

## 子项（仅 tracking 项填写）

| 子项 id | 状态 |
|---|---|
| — | |

## 进度笔记

### 登记背景（2026-08-12）

用户要求在 `xgtest` 分支按标准工作流（TDD）实现虚谷数据库引擎，engine 名 `xugu` / `xugudb`。已提供：驱动源码 `~/xgspace/cloudjdbc`（产物 `xugu-jdbc-12.3.9-20260710.jar`）、文档 `~/xgspace/docs`、运行中服务 `127.0.0.1:5138/SYSTEM`（SYSDBA/SYSDBA）。

**关键技术勘察（驱动 + 实测连接）：**

| 维度 | 事实 | 来源 |
|---|---|---|
| 驱动 GAV | `com.xugudb:xugu-jdbc:12.3.9-20260710`（本地构建，**未入 m2**，需 `install:install-file`） | cloudjdbc/pom.xml |
| 驱动类 | `com.xugu.cloudjdbc.Driver`；**经 SPI 注册**（`META-INF/services/java.sql.Driver`）→ 现有 shade `ServicesResourceTransformer` 自动合并 | cloudjdbc/src/main/resources |
| 运行时依赖 | `org.checkerframework:checker-qual`（m2 已有；注解，运行时通常可缺） | cloudjdbc/pom.xml |
| URL | `jdbc:xugu://host:port/database?char_set=utf8`；默认端口 5138；协议仅 `jdbc:xugu:` | Driver.parseURL / ReplaceEnum.conStrProName="xugu" |
| 连接属性 | user/password（内部 lowercase）；char_set=utf8 | 实测 |
| 事务 | autoCommit=true 默认（与现有 executor 契约一致：连接拥有权与提交由 caller 决定） | 实测 |
| 隔离原语 | **CREATE SCHEMA / DROP SCHEMA CASCADE / `schema.table`**；`SET SCHEMA <name>` 与 `ALTER SESSION SET CURRENT_SCHEMA = <name>` 均可使未限定 DDL/DML 落入指定 schema；跨 schema 同名表可并存 → **可镜像 `PostgresSchemaIsolation`** | 实测 Probe2 |
| NULL | `getString` 返回 null + `wasNull=true` → `AbstractJdbcExecutor.readRows` 正确 | 实测 Probe2 |
| 标识符大小写 | 不敏感（折大写，Oracle 式）；同文件内一致即可，对 sqllogictest 无碍 | 实测 Probe2 |
| `SELECT 1` 无 FROM | 支持 | 实测 Probe2 |

**架构映射（见 spec.md 详细合同）：** executor 类本身极简（engineName + 致命消息标记 + 展示名），方言编排（连接/隔离）在 CLI 层 `FileRunner`。新增点：`RuntimeConfigResolver`（允许 `xugu`/`xugudb` 别名→`xugu`、URL 前缀校验）、`FileRunner`（`isXugu` 分支 + `XuguSchemaIsolation` prepare/setSearchPath/teardown）、`pom.xml`（驱动依赖）、`Main.printHelp`、README。零下游：`AbstractJdbcExecutor`/`ConnectionFactory`/`SqlLogicTestRunner`/parser/normalize 不变。

**路径判定：** full（新能力、跨模块、范围含驱动依赖与隔离）。Spec required（且需用户确认）；Design required（引擎选择抽象 + 隔离机制）；Review required。源分支 = 目标分支 = `xgtest`（用户指定「在这个分支上实现」；`xgtest`→`main` 由用户自行决定，工作流不强制）。

**切片拆分：** 暂定**未拆分单切片**（引擎需含隔离才可用；与 PG 集成模式一致；一次可审阅）。Spec 用户确认时可应要求拆为 `xugu-engine-core`（full）+ `xugu-isolation`（standard）。
- 2026-08-14：按 ggnote `WORKFLOW.md` 标准迁移工作流目录（记录与产物合并为同一目录；权威文件改为 `workflow/WORKFLOW.md`）。
