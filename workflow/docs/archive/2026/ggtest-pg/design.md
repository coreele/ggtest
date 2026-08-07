# Design: ggtest-pg

> 架构决策（Plan 之前完成）。仅处理模块边界、分层与技术选型；需求合同与验收见 [`spec.md`](./spec.md)。
>
> **feature-id**：`ggtest-pg` · **sub-feature-id**：`ggtest-pg`（未拆分）
> **适用对象**：Planner（Plan 输入）、Developer（实现依据）、Reviewer（结构审阅）。
> **前置条件**：已读 [`spec.md`](./spec.md)、工作项 [`workflow/workflow/docs/manager/ggtest-pg.md`](../../manager/ggtest-pg.md)；Spec **approved**（含 PG 与 `.env` 决议）；熟悉归档 `ggtest-core` Design 与既有 `com.ggtest.db` / `db.sqlite` / `runner` / `cli`。
> **阅读顺序**：背景 → 模块边界与分层 → 方案对比与决策 → 模块影响 → 风险 → 对 Plan/Developer 要点。
> **预期结果**：读者掌握 PG 包边界、JDBC/隔离/URL 校验/门控，以及 CLI `.env` 配置解析层边界与合并算法，可据此编写 Plan 与实现。
> **失败处理**：发现 Spec 缺失合同信息时停止并报告 Manager，不在本文件替代 Spec 决策。

## 背景

- `ggtest-core` 已交付：`DatabaseExecutor`、`SqliteJdbcExecutor`、`SqlLogicTestRunner`、CLI（默认 `--engine sqlite`，每文件独立 open→run→close；`--url` 曾强制命令行）。
- 本项两块：① PostgreSQL 第二引擎（规范名 **`postgres`**；每文件 **schema 级**隔离；engine↔URL 硬错误退出码 2；无官方语料硬验收）；② CLI **`.env` 配置加载**（SQLite 与 PG 均适用；优先级 CLI > 进程环境变量 > `.env`）。
- 约束（勿重开）：Java 17、Maven、CLI 优先、hash-threshold 默认 8、halt→skipped、`.test`/`.slt`、SQLite 零豁免硬验收不得回归；执行器不打开/关闭连接；禁止真实 `.env` 入库；运行时键 `GGTEST_*` ≠ 测试门控 `GGTEST_PG_*`。

## 模块边界与分层

依赖单向向内；Runner **不得**依赖具体驱动包（延续 P0-8）。

```text
com.ggtest.cli  ──▶  com.ggtest.db（接口）
        │                 ▲
        │                 │
        ├──▶ runner ──────┤
        │                 │
        ├──▶ db.sqlite ───┤
        └──▶ db.postgres ─┘
                  └──▶ org.postgresql:postgresql
```

| 包 / 层 | 职责 | 禁止 |
|---|---|---|
| `com.ggtest.db` | 既有执行器抽象与结果/致命类型（本项**不改行为合同**） | 驱动依赖、隔离 DDL、读 `.env` |
| `com.ggtest.db.sqlite` | 既有 SQLite 适配（本项只读行为） | — |
| `com.ggtest.db.postgres` | `PostgresJdbcExecutor`；**连接侧** schema 隔离辅助（CREATE / `search_path` / DROP）——**不属于** `DatabaseExecutor` 合同 | 解析、normalize、条件/halt；打开/关闭用户连接；读配置文件 |
| `com.ggtest.runner` | 既有单文件状态机（本项不改对外行为） | `import` `db.sqlite` / `db.postgres` / `java.sql`；读 `.env` |
| `com.ggtest.cli` | argv 解析；**`.env` 加载与三源合并**；`--engine` 允许集；engine↔URL 硬校验；按 engine 选执行器；**编排**每文件连接与 PG schema 隔离；报告/退出码 | 把隔离或配置合并放进 Runner；把密码写入日志/报告/`toString` |

- **连接所有权（继承）**：CLI 创建/关闭连接；执行器只持有已打开连接并执行业务 SQL。
- **隔离所有权**：CLI 编排 schema 生命周期，委托 `db.postgres` 辅助类在同一连接上发管理 SQL；执行器不隐式建 schema。
- **配置所有权**：仅 `com.ggtest.cli` 在启动阶段读 `.env` / 进程环境 / argv，合并为最终运行配置后再连库；`db.*` / `runner` 不感知配置来源。

### CLI 内配置解析子边界

建议在 `com.ggtest.cli` 内拆出（类名可微调，边界固定）：

| 组件 | 职责 |
|---|---|
| `CliArgumentParser` | 只解析 argv（含 `--env-file`）；**不**读文件、**不**读进程环境；输出「显式提供的标志 + 位置参数」；未知选项仍 Usage 错误 |
| `DotEnvLoader`（或等价） | 按路径读单个 `.env`；`KEY=VALUE`；`#` 注释与空行；引号剥离（见决策 10）；产出键值供合并 |
| `RuntimeConfigResolver`（或等价） | 字段级合并三源 → 最终 `CliOptions`；丢弃未知键；校验合并后必有 URL；规范化 engine |

流水线：`argv 解析` → `解析 .env 路径并加载（若适用）` → `读进程环境白名单键` → `按优先级合并` → `engine↔URL 校验` → `CliSession`。

## 方案对比与决策

**决策 1：PG 包放置**

| 方案 | 概要 | 优点 | 缺点 |
|---|---|---|---|
| A（选定） | `com.ggtest.db.postgres`，对称于 `db.sqlite` | 与归档 Design 一致；Runner 依赖检查可扩到禁止 `db.postgres` | 多一子包 |
| B | 实现塞进 `cli` | 改动面小 | 破坏执行器可替换边界；库层测试困难 |

**决策:** 选 A。`PostgresJdbcExecutor`；`engineName()` = `"postgres"`。

**决策 2：JDBC 驱动**

| 方案 | 概要 | 优点 | 缺点 |
|---|---|---|---|
| A（选定） | `org.postgresql:postgresql`，建议钉 **42.7.13**（JDK 17） | 官方驱动；Central 易得；对齐 `jdbc:postgresql://…` | 集成测需可达 PG |
| B | 其他包装/旧大版本线 | — | 偏离事实标准 |

**决策:** 选 A。`pom.xml` property 钉版本。示例 URL：`jdbc:postgresql://host:port/dbname`（文档层补充，不扩展 Spec 合同）。

**决策 3：schema 级隔离机制**

| 方案 | 概要 | 优点 | 缺点 |
|---|---|---|---|
| A（选定） | 每文件：唯一 schema 名 → `CREATE SCHEMA` → `SET search_path TO <schema>, pg_catalog` → Runner → `DROP SCHEMA … CASCADE`（finally）；连接仍由 CLI open/close | 跨文件无用户对象污染；对齐 Spec「schema 级」；fixtures 可验 | 需 `CREATE`/`DROP SCHEMA` 权限；残留 schema 风险（崩溃路径靠 finally + 唯一名） |
| B | 每文件独立 database | 隔离更强 | 需建库权限/运维；超出「schema 级」决议 |
| C | 仅新连接、无 schema | 实现简单 | 不满足 Spec；会再现跨文件污染类缺陷 |

**决策:** 选 A。

- **权限假设**：连接用户对目标库具备 `CREATE SCHEMA` 与 `DROP SCHEMA`（含 CASCADE）权限；工具不负责长期运维该库。
- **命名**：每文件生成唯一、合法标识符（例如前缀 + 短 UUID 去连字符），避免与用户已有 schema 冲突。
- **search_path**：业务 SQL 未限定 schema 时落在隔离 schema；`pg_catalog` 保留以便系统对象解析。
- **失败语义**：隔离管理 SQL 失败视为连接/配置类硬错误（该文件中止，CLI 退出码映射为 2），与「连接不可用」同级，不伪装成业务断言失败。
- **与执行器分工**：`PostgresJdbcExecutor` 只执行语料 SQL；隔离 DDL 不经 Runner 记录模型。

**决策 4：engine↔URL 校验**

| 方案 | 概要 | 优点 | 缺点 |
|---|---|---|---|
| A（选定） | 合并配置后、连库前：规范化 engine 后校验 URL 前缀；`sqlite` ↔ `jdbc:sqlite:`；`postgres` ↔ `jdbc:postgresql:`；不匹配 → `UsageException`/等价，退出码 **2**，不连库、不执行 | 对齐 Spec 硬错误；实现简单可测 | 「明显」以 JDBC 子协议前缀为准，不深挖 host |
| B | 连库后再探测 | — | 已违反「不执行」；浪费连接 |

**决策:** 选 A。未知 `--engine` / 合并后未知 engine 仍退出码 2（允许集 `sqlite`|`postgres`，大小写不敏感后规范化小写）。校验在**合并完成之后**执行，使来自 env / `.env` 的 engine 与 URL 同样受约束。

**决策 5：PG 测试门控**

| 方案 | 概要 | 优点 | 缺点 |
|---|---|---|---|
| A（选定） | 环境变量门控（建议 `GGTEST_PG_URL`，可选 `GGTEST_PG_USER` / `GGTEST_PG_PASSWORD`）；未配置时 JUnit `assumeTrue` 跳过 PG 专属用例；默认 `mvn test` 不因缺 PG 失败 | 对齐 Spec P1-PG-3 与既有 `GGTEST_CORPUS_DIR` 模式 | 无 PG 时 PG 路径无 L3 证据，须在文档/dev-notes 声明 |
| B | Testcontainers 默认拉镜像 | 自包含 | 需 Docker/网络；偏离「门控依赖」；CI 负担大 |

**决策:** 选 A。文档与 README 声明门控变量；强制跑 PG 时由开发者/QA 配置 URL。SQLite 路径与既有语料硬验收**不**依赖 PG。

- **与运行时 `.env` 键严格区分**：`GGTEST_PG_*` **仅**测试门控；运行时合同键为 `GGTEST_URL` / `GGTEST_USER` / `GGTEST_PASSWORD` / `GGTEST_ENGINE` / `GGTEST_HASH_THRESHOLD`。测试不得把 `GGTEST_PG_*` 当作产品配置源；产品代码不得读取 `GGTEST_PG_*` 作为运行时合并输入。

**决策 6：CLI 执行器选择**

| 方案 | 概要 | 优点 | 缺点 |
|---|---|---|---|
| A（选定） | `CliSession` 按规范化 `engine` 分支：`sqlite`→`SqliteJdbcExecutor`；`postgres`→隔离辅助 + `PostgresJdbcExecutor` | 单入口；与 `--engine`/`engineName` 一致 | CLI 略增分支 |
| B | SPI/反射加载 | 可插拔 | 过度设计 |

**决策:** 选 A。`--engine` 与执行器 `engineName()` 必须一致（由同一规范化 token 驱动选择，无需二次猜测）。

**决策 7：`.env` / 配置合并落点**

| 方案 | 概要 | 优点 | 缺点 |
|---|---|---|---|
| A（选定） | 配置加载与合并留在 `com.ggtest.cli`（`DotEnvLoader` + `RuntimeConfigResolver`）；`db`/`runner` 无感知 | 对齐 Spec「配置加载属 CLI」；不破坏执行器边界；SQLite/PG 共用 | CLI 包略增 |
| B | 独立 `com.ggtest.config` 包 | 可复用面大 | 本项无第二消费者；过度分层 |
| C | 塞进 `db.postgres` | — | 违反「SQLite 亦适用」与包边界 |

**决策:** 选 A。不引入第三方 dotenv 库（解析面小；避免传递依赖与许可证噪音）。

**决策 8：`--env-file` 与默认 CWD `.env` 关系**

| 方案 | 概要 | 优点 | 缺点 |
|---|---|---|---|
| A（选定） | **替换**：未传 `--env-file` → 仅尝试 `Paths.get("").toAbsolutePath().resolve(".env")`（CWD）；传了 → **只**读该路径，**不再**读 CWD `.env` | 与 Spec「指定其他文件」一致；单文件、无层内优先级歧义；测例简单 | 不能「叠加」两份 `.env` |
| B | 叠加：先 CWD 再 `--env-file`（或相反）覆盖 | 灵活 | Spec 未要求；同层键冲突需再定义；易踩坑 |

**决策:** 选 A（替换，非叠加）。

- **默认路径缺失**：CWD 无 `.env` 且未传 `--env-file` → **不报错**，该层为空 map。
- **显式 `--env-file` 缺失或不可读**：视为用法/配置错误 → `UsageException`（退出码 **2**），因用户已指定必须使用的路径。
- **同一路径存在但为空/仅注释**：合法；该层无贡献键。

**决策 9：合并算法与键白名单**

| 方案 | 概要 | 优点 | 缺点 |
|---|---|---|---|
| A（选定） | 逐字段：显式 CLI 标志 > 进程环境变量 > `.env` 文件；高层「已提供」则低层不覆盖；未提供则下沉 | 对齐 Spec E1；字段独立合并（例如 CLI 只给 `--url` 时 password 仍可来自 `.env`） | 实现须区分「未出现」与「空串」 |
| B | 整层覆盖（整份 map） | 实现粗 | 违反「字段级」直觉；CLI 部分标志会抹掉 `.env` 其它键 |

**决策:** 选 A（字段级）。

运行时白名单与来源映射：

| 配置字段 | CLI | 进程环境 / `.env` 键 |
|---|---|---|
| url | `--url` | `GGTEST_URL` |
| user | `--user` | `GGTEST_USER` |
| password | `--password` | `GGTEST_PASSWORD` |
| engine | `--engine` | `GGTEST_ENGINE` |
| hash-threshold | `--hash-threshold` | `GGTEST_HASH_THRESHOLD` |

- **「已提供」**：CLI = 该选项出现在 argv；环境 / `.env` = 白名单键存在。空白 URL 视为未提供；合并后仍无 URL → 退出码 2。
- **engine 默认**：三源皆未提供时 **`sqlite`**；`GGTEST_ENGINE` / `.env` 可覆盖默认，CLI `--engine` 优先。
- **hash-threshold 默认**：三源皆未提供时 **8**；非法数值 → 退出码 2。
- **未知键**：忽略、不失败；进程环境只读白名单键（不扫描全部 env）。
- **合并后无 URL**：退出码 **2**，不执行；URL 可来自 CLI / 环境 / `.env` 任一。

**决策 10：`.env` 格式解析边界**

| 方案 | 概要 | 优点 | 缺点 |
|---|---|---|---|
| A（选定） | 最小子集：`KEY=VALUE`；行首或仅空白后 `#` 为注释；空行忽略；键两侧 trim；值：若首尾同为 `"` 或同为 `'` 则剥离一对；不支持 `export ` 前缀、多行值、`\n` 转义、变量插值 | 够用；易测；无第三方 | 非完整 dotenv 方言 |
| B | 完整 dotenv（含插值/多行） | 兼容面大 | 复杂度与歧义高；超出 Spec |

**决策:** 选 A。畸形行（无 `=`）忽略或跳过（等同未知/无效贡献，**不**因此失败），与「未知键忽略」同宽容度；不得因未知键或无关行使 CLI 失败。

**决策 11：凭据安全（结构面）**

| 方案 | 概要 | 优点 | 缺点 |
|---|---|---|---|
| A（选定） | `CliOptions`（及中间 DTO）的 `toString`/`record` 打印不得含 password 明文；报告与日志路径禁止写 password；仓库提供 `.env.example` 占位符；真实 `.env` 依赖既有 `.gitignore`（`.env`、`.env.*`、`!.env.example`） | 对齐 Spec P1-ENV-1/2 与 security 规范 | 测试需断言输出不含密码 |
| B | 依赖调用方自觉 | — | 易泄漏 |

**决策:** 选 A。测试用临时目录构造 `.env`，**禁止**在仓库创建或提交真实 `.env`。

## 模块影响

- `pom.xml`：增加 `org.postgresql:postgresql`（建议 **42.7.13**，compile，随 shade 打入可执行 JAR）。**不**为 dotenv 增加依赖。
- 新增：`com.ggtest.db.postgres`（执行器 + schema 隔离辅助）。
- 修改/新增：`com.ggtest.cli`（`--engine` 允许集、`--env-file`、`.env` 加载、三源合并、URL 校验、按 engine 编排连接/隔离/执行器；放宽「仅 CLI `--url`」）。
- 仓库根：`.env.example`（占位键，无真实凭据）。
- 扩展测试：`RunnerDependencyIsolationTest`（或等价）禁止 `runner` 依赖 `db.postgres`；PG 门控集成测 + 自造 PG fixtures；`.env` 合并/覆盖/缺失/显式路径错误的 CLI 测（临时目录）；既有 SQLite CLI/语料测不得回归。
- **不改行为**：`parser`、`normalize`、`runner` 对外合同；`db` 接口形状。

## 风险

| 风险 | 影响 | 缓解 |
|---|---|---|
| 用户无 CREATE/DROP SCHEMA 权限 | PG 路径全部硬错误 | README 声明权限；fixtures/验收用具备权限的角色 |
| 进程崩溃留下 schema | 目标库残留对象 | 唯一名 + finally DROP；文档说明可手工清理 |
| 致命/业务 SQLState 分类不准 | 误中止或未中止 | 对齐 SQLite：`08*` 等连接类为致命；普通 SQL 错误进结果对象；关连接用例 |
| 无 PG 时跳过过多 | PG 验收证据缺口 | Plan/QA：有 PG 时必跑 P0-PG-*；无 PG 记原因/恢复条件，不得默示 Pass |
| 驱动拉取失败 | 无法构建 | 钉版本；代理 `127.0.0.1:7890`；多次失败则停 |
| SQLite 路径被误改 | Q8 硬验收回归 | 默认 engine 仍 sqlite；无 `.env` + `--url` 兼容测（P0-ENV-4）；回归命令保留 |
| `.env` 与门控键混淆 | 产品误读 `GGTEST_PG_*` 或测试误用运行时键 | Design/测试显式隔离；README 分栏说明 |
| password 经 record/`toString` 泄漏 | P1-ENV-1/P1-PG-2 失败 | 自定义安全字符串表示；输出断言 |
| 显式 `--env-file` 路径错误被静默忽略 | 用户以为已加载 | 决策 8：显式路径缺失 → 退出码 2 |

## 对 Plan 与 Developer 的要点

### Plan

- 顺序：PG 执行器（门控测）→ schema 隔离辅助 → CLI engine/URL 校验 → CLI 编排 → **`.env` 加载/合并/`--env-file`** → fixtures（含 ENV 与跨文件隔离）→ SQLite 回归 + `.env.example` + README。
- 验证：默认 `mvn test`（无 PG 可跳过专属）；有 `GGTEST_PG_URL` 时跑通 P0-PG-*；ENV 用临时 `.env` 覆盖 P0-ENV-* / P1-ENV-*；SQLite `select1` 硬验收不回归且不依赖仓库 `.env`。
- 建议分支：`ggtest-pg` → `main`。
- Review 门禁 required；进入 QA 前须 Approve。
- **禁止**提交真实 `.env`、`examples/` 未跟踪语料。

### Developer

- TDD；`runner` 禁止依赖 `db.postgres` / `db.sqlite` / 直接 `java.sql`。
- `engineName()` / CLI token 仅 **`postgres`**（不以 `postgresql` 为主 token）。
- 隔离 DDL 不进语料断言路径；业务 SQL 原样 JDBC 发送。
- 配置合并在连库与 engine↔URL 校验之前；字段级 CLI > env > `.env`；`--env-file` **替换**默认 CWD `.env`。
- 产品代码只读 `GGTEST_*` 运行时键；`GGTEST_PG_*` 仅测试。
- 不提交官方大语料、`examples/` 未跟踪文件、真实 `.env`；不把官方语料 PG 零失败当作 Pass。
