# 工作项记录: xugu-engine

工作项标识: xugu-engine
描述: 为 ggtest 增加虚谷数据库（XuguDB）引擎支持：`--engine xugu`（别名 `xugudb`）经 `com.xugudb:xugu-jdbc` 驱动连接虚谷服务；按文件 schema 隔离以支持跨文件/`--parallel` 并行；与现有 `--halt`/`--parallel`/`conn=<name>`/凭据脱敏等正交。
目标分支: xgtest
文档影响: 受影响——README（引擎表/`--engine` 行/`GGTEST_XG_*` 测试门变量段）、`--help`；新增 `com.xugudb:xugu-jdbc` 依赖与本地安装说明。

> 权威流程见 [workflow/README.md](../../README.md)；活跃状态见 [STATUS.md](STATUS.md)。
>
> **切片级：** 路径等级、源分支、门禁、状态、阻塞。**工作项级：** 目标分支。
> 未拆分：产物在 `workflow/docs/features/xugu-engine/`。已拆分：根目录仅总览 Spec；切片在 `<feature-id>-<sub>/`。
> 归档后本文件迁至 `workflow/docs/archive/YYYY/xugu-engine/manager.md`（须修正相对链接）；`workflow/docs/manager/` 仅保留活跃项与 STATUS。
>
> 表内只填枚举、短标签或链接；较长理由写入「进度笔记」（见 `workflow/docs/standards/documentation.md` §B）。

## 切片门禁（未拆分时一行，sub-feature-id = feature-id）

| sub-feature-id | 路径等级 | 源分支 | Spec | Spec 门禁 | Spec 用户确认 | Design 门禁 | Review 门禁 |
|---|---|---|---|---|---|---|---|
| xugu-engine | full | xgtest | [spec.md](./../features/xugu-engine/spec.md) | required | required | required | required |

> 总览行：路径等级与门禁、源分支均可 `N/A`。`Review=skipped` 仅 `fast`；理由写进度笔记。

## 切片状态

| sub-feature-id | 状态 | 后续步骤 | 阻塞原因 | 恢复条件 | 恢复后目标 |
|---|---|---|---|---|---|
| xugu-engine | designing | Planner 编写 design.md | | | |

> 无阻塞则后三列留空。长说明优先进度笔记。

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
