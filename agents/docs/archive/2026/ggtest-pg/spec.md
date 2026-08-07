# Spec: ggtest-pg

> 需求与规格（Plan 之前完成）。任务拆解见同目录 `plan.md`；架构选型见同目录 `design.md`（Design 门禁 required）。
>
> **feature-id**：`ggtest-pg` · **sub-feature-id**：`ggtest-pg`（未拆分）
> **适用对象**：用户（确认范围）、Planner（Design/Plan）、Developer、QA、Manager。
> **前置条件**：工作项 [`agents/docs/manager/ggtest-pg.md`](../../manager/ggtest-pg.md)；归档 [`agents/docs/archive/2026/ggtest-core/`](../../archive/2026/ggtest-core/)（runner-sqlite / cli-corpus）；`com.ggtest.db.DatabaseExecutor` 与 CLI `--engine`；[`agents/docs/standards/security.md`](../../standards/security.md)。
> **阅读顺序**：背景与目标 → 非目标 → 范围与可见行为 → 合同 → 验收 → 开放问题。
> **预期结果**：PG 第二引擎边界、CLI `.env` 合同、与 SQLite 并存约束及验收口径；PG 与 `.env` 均已决议。
> **失败处理**：合同/验收歧义未决时不得进入 Design/Plan。
>
> **确认要求（full · 增量修订）**：修订稿整体 **已批准（ok，2026-07-25）**。PG 决议（engine=`postgres`、schema 级隔离、engine↔URL 硬错误、PG 官方语料失败=0 不作硬验收）与 `.env` 决议（E1–E5）均有效、勿重开。待用户确认已清空。

## 背景与目标

- `ggtest-core` 已交付 SQLite JDBC、Runner 扩展点与 CLI；「其他库」曾为非目标，扩展点保留。
- **目标（PG）**：交付 **PostgreSQL JDBC 执行器**（`DatabaseExecutor`）；CLI 可选 PG `--engine`；`skipif`/`onlyif` 与 engine 匹配（大小写不敏感）；PG **每文件隔离**；与 SQLite 并存且**不破坏** SQLite 硬验收；P0/P1 验收。
- **目标（CLI `.env`，SQLite 与 PG）**：从 `.env` 读取库配置（至少 JDBC URL、user、password；可含 engine、hash-threshold）；与 CLI / 进程环境变量并存且优先级明确；无真实凭据入库；提供 `.env.example`（占位符）。
- PG engine 规范名：**`postgres`**（CLI `--engine` / `engineName()`）。归档中 `postgresql` 仅为历史语料标识；匹配以 `engineName=postgres` 为准。

## 非目标

- 其他库（MySQL、DuckDB、H2 等）生产级实现。
- SQL 方言改写；`statement error` 消息/正则匹配（只断言失败事实）。
- 多文件并行、多连接并发、连接池产品化。
- 为 PG 改写/维护官方语料；提交官方大语料或 `examples/` 未跟踪演示文件。
- **PG 官方语料硬验收失败数=0**。
- 改变 parser / normalize / runner 核心条件语义（仅扩展执行器、CLI 引擎选择与配置加载）。
- GUI、CI/CD 插件、Maven Central；承诺稳定公共库 API。
- 重开「已决议」项。
- 创建或提交真实 `.env`（仅 `.env.example` 占位符）。
- 将测试门控（如 `GGTEST_PG_*`）等同于运行时 `.env` 合同。

## 范围与可见行为

### PG JDBC 执行器

- 实现 `DatabaseExecutor`：`engineName()`；语句成功/失败；查询返回原始行列（SQL NULL → `null`；不做 I/T/R）。
- 调用方拥有连接：执行器不打开、不初始化、不关闭连接。
- 业务 SQL 失败经结果对象回报；连接不可用 → 致命，中止当前文件（退出码 `2`）。

### engine 与 skipif / onlyif

- `--engine` 允许 `sqlite` 与 **`postgres`**；默认 **`sqlite`**。
- 匹配相对 `engineName()`，大小写不敏感。
- PG：`skipif postgres` 命中跳过；`onlyif postgres` 未命中跳过；与 `sqlite` 对称。
- `--engine` 与执行器必须一致；与 JDBC URL 明显不匹配 → **硬错误**，退出码 `2`，不执行。

### 每文件隔离

- **SQLite（勿破坏）**：每文件独立 open→run→close；内存 URL 等价空白库；文件作用域状态每文件重置。
- **PG（schema 级）**：跨文件不得可观察污染用户对象；实现细节属 Design；验收须可用 fixtures。不采用「无隔离」。同类缺陷曾致 SQLite 硬验收失败（DEF-CLI-001）。

### CLI 接入与配置来源

```text
ggtest [--url <jdbc-url>] [--user <user>] [--password <password>]
       [--engine <name>=sqlite] [--hash-threshold <N>]
       [--env-file <path>]
       <file-or-dir> [<file-or-dir> ...]
```

- **URL**：CLI `--url`、进程环境变量或 `.env` **至少一处**；三处皆无 → 用法错误，退出码 **`2`**。显式 `--url` 须仍可用（cli-corpus / P0-PG-5）。
- `--user` / `--password` 可选；可由 CLI、环境变量或 `.env` 提供。
- `--engine`：默认 `sqlite`；允许 `postgres`（大小写不敏感）；未知 → 退出码 `2`。可由配置源覆盖默认。
- `--hash-threshold` 默认 **8**；文件内指令可覆盖；可由配置源提供。
- 位置参数、目录递归 `*.test`/`*.slt`、报告、退出码 `0`/`1`/`2` 继承 cli-corpus。
- 未选 PG 且无 `.env` 时与归档一致；SQLite 硬验收（失败数=0、退出码=0、零豁免）不得回归。

### CLI `.env` 加载（SQLite 与 PG）

- **必须**从 `.env` 可读：至少 URL、user、password；**允许** engine、hash-threshold。运行时键名：`GGTEST_URL` / `GGTEST_USER` / `GGTEST_PASSWORD` / `GGTEST_ENGINE` / `GGTEST_HASH_THRESHOLD`。
- **优先级**：**命令行 > 进程环境变量 > `.env`**；高层已提供的值不被低层覆盖。
- **路径**：默认当前目录 `.env`；支持 **`--env-file <path>`** 指定其他文件。
- **缺失**：无 `.env`（且未指定 `--env-file`）时**不报错**，仅用 CLI / 环境变量。
- **格式**：`KEY=VALUE`；`#` 注释；允许引号（剥离规则属 Design）；空行忽略。
- **未知键**：**忽略**。
- **安全**：禁止真实 `.env` 入库（已有 `.gitignore`：`.env`、`.env.*`、`!.env.example`）；须提供 `.env.example`（占位符）；stdout/stderr/报告**禁止**密码明文。
- **测试门控 ≠ 运行时合同**：如 `GGTEST_PG_*` 仅用于有无 PG 时跑/跳过测试，与上述运行时键名区分。

### 验收依赖

- SQLite：空白库（如 `jdbc:sqlite::memory:`）；不依赖 PG；不依赖 `.env`（可用 `--url`）。
- PG：可达实例为门控依赖；未配置则跳过 PG 专属用例；默认 `mvn test` 不得因缺 PG 失败（机制属 Design/Plan）。
- 官方语料自备、不入库。硬验收：自造 PG fixtures + schema 隔离 + CLI/执行器/`.env` 合同。

## 合同

### API / 接口

| 面 | 合同 |
|---|---|
| **DatabaseExecutor** | PG 适配器实现 `engineName` / `executeStatement` / `executeQuery`；业务 vs 致命与值语义对齐 SQLite。 |
| **engine 名** | CLI `--engine` 与 `engineName()` PG token 均为 **`postgres`**。 |
| **CLI** | `--engine`：`sqlite` \| `postgres`（大小写不敏感后规范化）；默认 `sqlite`。支持 `--env-file`。URL 可由配置源提供。其余继承 cli-corpus。 |
| **配置合并** | 优先级 **命令行 > 进程环境变量 > `.env`**；合并后无 URL → 退出码 `2`。运行时键：`GGTEST_URL` / `GGTEST_USER` / `GGTEST_PASSWORD` / `GGTEST_ENGINE` / `GGTEST_HASH_THRESHOLD`。 |
| **`.env.example`** | 占位符模板（无真实凭据）；键与运行时约定对齐。 |
| **Runner / Parser / Normalize** | 对外行为不变；换库加执行器；配置加载属 CLI。 |

### 数据 / 状态

- 单文件：记录共享同一连接与会话；文件作用域状态每文件由 CLI 重置。
- 跨文件：**schema 级**隔离；后文件不可观察前文件用户对象。
- 隔离用短暂创建/销毁属本项；不负责用户库长期运维。
- `.env` 仅在 CLI 解析阶段读取；不写入用户库。

### 错误与约束

- 未知 `--engine` → `2`。
- engine↔URL 明显不匹配 → `2`，不执行。
- CLI / 环境变量 / `.env` 皆无 URL → `2`。
- 连接失败/中断 → `2`；断言失败 → `1`。
- 无 `.env` + 显式 `--url` → 与归档一致。
- `.env` 未知键忽略；不因此失败。
- 报告/日志不含凭据；禁止真实 `.env` 入库。
- Java 17、Maven、GGTEST、CLI 优先；禁止默示豁免 SQLite 零豁免硬验收（Q8）；不提交官方大语料与 `examples/` 未跟踪演示。

## 验收（Given-When-Then）

**前置**：JDK 17+、Maven、可运行 `ggtest`；SQLite 空白库；PG 经门控（未配置则跳过 PG 专属）；自造 fixtures；`.env` 用例用临时目录/文件，**禁止**写入仓库真实 `.env`。

### P0

- **P0-PG-1 执行器合同**：Given 已打开 PG 连接与 PG `DatabaseExecutor`；When 合法 `statement ok`、会失败的 `statement error`、已知行列 `query`；Then 断言与原始行列（含 NULL→`null`）符合合同；业务失败非致命；连接关闭后再执行表现为致命/连接错误。

- **P0-PG-2 engine 与条件**：Given 含 `skipif postgres`、`onlyif postgres`、`onlyif sqlite` 的文件；When `--engine postgres` + PG URL；Then 与 runner-sqlite P0-6 对称；匹配大小写不敏感。

- **P0-PG-3 CLI 与默认 SQLite**：Given 已打包 `ggtest`；When (a) 默认/`sqlite` + SQLite URL；(b) `--engine postgres` + PG URL；(c) 未知 `--engine`；(d) engine↔URL 明显不匹配；Then (a) 与归档一致；(b) PG 跑通且失败数符合预期；(c)(d) 退出码 `2` 且不执行。

- **P0-PG-4 跨文件隔离**：Given 两 fixtures 均创建同名用户对象并依赖「开始时不存在」；When 同一次 CLI 批量、**schema 级**隔离；Then 两文件失败数=0、退出码 0，后文件未被污染。

- **P0-PG-5 SQLite 硬验收无回归**：Given 自备 `select1.test` + 空白 SQLite；When `ggtest --url <sqlite-jdbc-url> select1.test`（默认或 `--engine sqlite`，**无** `.env`）；Then 失败数=0、退出码=0。

- **P0-ENV-1 仅 `.env` 提供 URL**：Given 合法 `.env` 含 URL（及所需键），命令行**未**传 `--url`；When `ggtest <fixture>`；Then 使用 `.env` URL 执行，非用法错误（成功退出码 0 或断言失败 1）。

- **P0-ENV-2 CLI 覆盖 `.env`**：Given `.env` 为 URL-A，CLI `--url URL-B`；When `ggtest --url URL-B …`；Then 连接使用 **URL-B**。

- **P0-ENV-3 三处皆无 URL**：Given 无 `--url`、无 URL 环境变量、无可用 `.env` URL；When `ggtest <fixture>`；Then 退出码 **`2`**，不执行语料。

- **P0-ENV-4 无 `.env` + `--url` 兼容**：Given 无 `.env`；When `ggtest --url <sqlite-jdbc-url> <fixture>`；Then 与现网一致，不因缺 `.env` 失败。

### P1

- **P1-PG-1**：Given 合法 PG 参数；When `--engine` 为 `postgres` 不同大小写；Then 解析成功且 skipif/onlyif 正确。

- **P1-PG-2**：Given 带 `--user`/`--password` 的 PG 运行；When 查看 stdout/stderr；Then 无密码明文。

- **P1-PG-3**：Given 无可达 PG；When 默认测试套件（无强制 PG 门控）；Then 不因缺 PG 失败；PG 专属跳过或非强制。

- **P1-PG-4（非硬验收）**：官方 select1/2/3 在 PG 上失败数=0 **不作** Pass 条件。

- **P1-ENV-1 密码不泄露**：Given `.env` 或 CLI 含 password；When 运行；Then stdout/stderr/报告无密码明文。

- **P1-ENV-2 `.env.example`**：Given 仓库交付物；When 检查约定路径 `.env.example`；Then 有占位符键、无真实凭据；真实 `.env` 仍被 gitignore。

## 开放问题

### 已决议（继承 ggtest-core，勿重开）

| 议题 | 结论 |
|---|---|
| Q1 / Q2 / Q3 | Java 17 / Maven / CLI 优先 |
| Q4 | 用户自备语料；不提交官方大语料 |
| Q5 | hash-threshold 默认 **8** |
| Q6 | halt 后 → **skipped** |
| Q8 | SQLite 路径零豁免硬验收；不可默示豁免 |
| 后缀 | `.test` / `.slt` 等价 |
| SQLite engine | `sqlite`；skipif/onlyif 大小写不敏感 |
| 产品名 | GGTEST |

### 已决议（本项 PG，2026-07-25，勿重开）

| 议题 | 结论 |
|---|---|
| engine 名 | **`postgres`**（非 `postgresql` 主 token） |
| 每文件隔离 | **schema 级** |
| engine↔URL 不一致 | **硬错误**（退出码 `2`） |
| PG 官方语料硬验收失败=0 | **否** |

### 已决议（本项 `.env`，2026-07-25，勿重开）

| 议题 | 结论 |
|---|---|
| E1 配置优先级 | **命令行 > 进程环境变量 > `.env`** |
| E2 `.env` 路径 | 当前目录 `.env`；支持 **`--env-file`** |
| E3 键名 | `GGTEST_URL` / `GGTEST_USER` / `GGTEST_PASSWORD` / `GGTEST_ENGINE` / `GGTEST_HASH_THRESHOLD`（与测试门控 `GGTEST_PG_*` 区分） |
| E4 未知键 | **忽略** |
| E5 工作项归属 | **留在 `ggtest-pg`**（不另立工作项） |
| Spec 修订稿整体 | **approved（ok）** |

### 待用户确认

（无）

### 拆分建议（历史备注，非阻塞；不执行）

| 建议 sub-feature-id | 职责 |
|---|---|
| `executor-pg` | PG JDBC 执行器、engine 名、fixtures |
| `cli-engine` | 多 engine CLI、连接/隔离编排、SQLite 回归 |

按 E5，`.env` 留在本项；仍一份未拆分 Spec。
