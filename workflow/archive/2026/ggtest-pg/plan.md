# Plan: ggtest-pg

> 实施与验证计划。需求依据见 [`spec.md`](./spec.md)；架构选型见 [`design.md`](./design.md)。
>
> **适用对象**：Developer（实施）、Reviewer（审阅）、QA（验收）、Manager（门禁流转）。
> **前置条件**：Spec 用户确认 `approved`（含 PG 与 `.env`）；Design 已定稿；Java 17 + Maven；既有 `ggtest-core`（SQLite CLI）在目标分支可用。
> **阅读顺序**：元信息 → 目标摘要 → 任务拆解 → 依赖与顺序 → 触碰路径 → 验证 → 验收 → 文档影响 → 交接。
> **预期结果**：Developer 按 TDD 交付 PG 执行器、schema 隔离、CLI `--engine postgres`、`.env` 三源合并与门控/ENV 测试；Reviewer Approve 后 QA 独立验收。
> **失败处理**：按「验证」节定位；无法执行时按「无法执行验证时的处理」记录原因/风险/恢复条件。
>
> **确认要求**：本 Plan 须由**当前用户会话确认**后，Manager 方可持久化确认并将状态设为 `planned` 再调度 Developer。Planner **不得**自行批准。

## 元信息

- 工作项标识: ggtest-pg（sub-feature-id: ggtest-pg，未拆分）
- 依据 Spec: [workflow/archive/2026/ggtest-pg/spec.md](./spec.md)
- 依据 Design: [workflow/archive/2026/ggtest-pg/design.md](./design.md)
- 路径等级: full
- Review 门禁: **required**（进入 QA 前须 Reviewer `Approve`）
- 最低验证层: **L3**（单元 + 构建 + CLI/执行器集成；PG 门控；ENV 用临时目录；SQLite 硬验收回归必达，语料自备）
- 验证命令:
  - `mvn -q clean test`（无 `GGTEST_PG_URL` 时 PG 专属须 skip，不得失败）
  - `mvn -q clean package`
  - 有可达 PG 时（门控 `GGTEST_PG_*`，**非**运行时 `GGTEST_*`）:
    - `GGTEST_PG_URL='jdbc:postgresql://…' GGTEST_PG_USER=… GGTEST_PG_PASSWORD=… mvn -q test`
    - `./bin/ggtest --url "$GGTEST_PG_URL" --user … --password … --engine postgres <pg-fixtures…>`
  - ENV（临时目录 `.env`，**禁止**仓库真实 `.env`）：覆盖 P0-ENV-1…4；`--env-file` 替换与显式缺失→2（见「预期证据」）
  - SQLite 回归（自备 `$SELECT1`；**无**仓库 `.env`）：`./bin/ggtest --url jdbc:sqlite::memory: "$SELECT1"` → 失败数=0、退出码=0
  - 代理：`export MAVEN_OPTS="-Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7890"`；多次失败则停止
- 建议源分支: `ggtest-pg`（目标分支 `main`）

## 适用工程规范

- [文档工程](../../standards/documentation.md)
- [Git 协作](../../standards/git.md)
- [质量与验证](../../standards/quality.md)
- [安全](../../standards/security.md)

## 目标摘要

- PG：`PostgresJdbcExecutor`（`engineName=postgres`）、schema 级隔离、CLI `--engine postgres`、engine↔URL 硬错误（退出码 2）。
- `.env`：字段级 **CLI > 环境变量 > `.env`**；CWD 默认；`--env-file` **替换**；键 `GGTEST_URL`/`USER`/`PASSWORD`/`ENGINE`/`HASH_THRESHOLD`；未知键忽略；无 URL → 退出码 2；`.env.example`。
- 与 SQLite 并存；不破坏零豁免硬验收；官方语料 PG 零失败非 Pass（P1-PG-4）。
- 依据：Spec P0/P1（PG+ENV）；Design 决策 1–11。

## 任务拆解

TDD：先写失败测试，再实现至通过。行为以 Spec 为准；结构以 Design 为准。

1. **T1 — PG JDBC 执行器**  
   - `pom.xml` 增加 `org.postgresql:postgresql`（建议 **42.7.13**）。  
   - 实现 `com.ggtest.db.postgres.PostgresJdbcExecutor`：`engineName()`=`postgres`；语句/查询合同对齐 SQLite（业务失败→结果对象；连接类→`FatalDatabaseException`；NULL→`null`，不做 I/T/R）。  
   - 测试：门控下 P0-PG-1；无门控时 assume 跳过。  
   - **完成条件**：有 `GGTEST_PG_URL` 时执行器测通过；无则 skip 且 `mvn test` 仍绿。

2. **T2 — schema 级隔离辅助**  
   - `db.postgres` 辅助：唯一 schema → `CREATE SCHEMA` → `SET search_path TO <schema>, pg_catalog` →（调用方跑 Runner）→ `DROP SCHEMA … CASCADE`（finally）。  
   - 隔离管理 SQL 失败 → 硬错误语义（CLI 映射退出码 2）。  
   - **完成条件**：门控下证明两段业务 DDL 在不同 schema 生命周期互不可见。

3. **T3 — `.env` 解析、三源合并与 `--env-file`**  
   - `com.ggtest.cli`：`DotEnvLoader` + `RuntimeConfigResolver`（名可微调）；**不**引入第三方 dotenv。  
   - 字段级合并：CLI > 进程环境 > `.env`；白名单键见 Design；未知键忽略。  
   - 路径：默认 CWD `.env`；`--env-file` **替换**（不叠加）；默认缺失不报错；显式路径缺失/不可读 → 退出码 2。  
   - 格式：`KEY=VALUE`、`#` 注释、空行、引号剥离（Design 决策 10）。  
   - 合并后无 URL → 退出码 2；放宽「仅 CLI `--url`」。  
   - **完成条件**：自动化覆盖 P0-ENV-1…4；临时目录测 `--env-file` 替换与显式缺失；**禁止**创建/提交仓库真实 `.env`。

4. **T4 — CLI：engine 允许集与 URL 硬校验**  
   - 合并后配置上：`--engine` / `GGTEST_ENGINE` 允许 `sqlite`|`postgres`（大小写不敏感→小写）；未知 → 退出码 2。  
   - 连库前：`sqlite`↔`jdbc:sqlite:`；`postgres`↔`jdbc:postgresql:`；不匹配 → 退出码 2、不连库、不执行（P0-PG-3 c/d；P1-PG-1）。  
   - **完成条件**：合法大小写、未知 engine、engine↔URL 错配（含来自 `.env` 的组合）有测。

5. **T5 — CLI：按 engine 编排连接 / 隔离 / 执行器**  
   - `CliSession`：`sqlite`→每文件 open→`SqliteJdbcExecutor`→run→close；`postgres`→open→隔离 prepare→`PostgresJdbcExecutor`→run→隔离 teardown→close。  
   - engine 与执行器同一规范化 token。  
   - **完成条件**：P0-PG-3 (a)(b)；默认/`sqlite` 与归档一致；报告/退出码继承 cli-corpus。

6. **T6 — 自造 fixtures 与验收测试（PG + ENV）**  
   - 仓库内小 fixtures（`src/test/resources/fixtures/…`，**禁止**提交 `examples/` / 官方大语料）：  
     - skipif/onlyif `postgres`/`sqlite`（P0-PG-2）  
     - 跨文件同名对象隔离（P0-PG-4）  
     - 基础 statement/query  
   - 扩展 `RunnerDependencyIsolationTest`：`runner` 不得依赖 `db.postgres`。  
   - P1-PG-2 / P1-ENV-1：stdout/stderr/报告无密码明文（含 `.env` 密码路径）。  
   - P1-PG-3：无 `GGTEST_PG_URL` 时默认套件不失败。  
   - P1-ENV-2：交付 `.env.example`（占位键）；确认 `.gitignore` 仍忽略真实 `.env`。  
   - 产品代码不得把 `GGTEST_PG_*` 当运行时合并输入。  
   - **完成条件**：门控开时 P0-PG-1…4 有证据；门控关时 P1-PG-3；ENV P0/P1 有自动化证据。

7. **T7 — SQLite 回归与文档**  
   - 既有 CLI/SQLite 测试全绿；P0-PG-5：自备 `select1.test` + `jdbc:sqlite::memory:`、**无** `.env` → 失败数=0、退出码=0（写入 `dev-notes.md`）。  
   - 更新 `README.md`：`--engine postgres`、PG URL/权限/隔离、门控 `GGTEST_PG_*` vs 运行时 `GGTEST_*`、`.env` / `--env-file` 优先级、凭据勿入日志；官方语料 PG 零失败非硬验收。  
   - `dev-notes.md`：命令、退出码、门控跳过/执行、ENV 测法、驱动版本。  
   - **完成条件**：README 可指导 PG/SQLite/`.env`；回归与文档证据齐全。

## 依赖与顺序

```text
T1 → T2 → T3 → T4 → T5 → T6 → T7
```

- T3 须在 T4/T5 之前合入主路径（合并结果驱动校验与编排）。  
- T4 测试可与 T3 后期并行起草，但连库编排以 T5 为准。  
- 各任务内部 TDD。  
- 只读/不改行为合同：`parser`、`normalize`、`runner`（除依赖隔离检查）、`db` 接口、`db.sqlite`。  
- Git：自 `main` 创建并检出 **`ggtest-pg`**。  
- 网络：依赖拉取失败用代理 `127.0.0.1:7890`；多次未成功则停止并记入 `dev-notes.md`。  
- **禁止**：提交真实 `.env`；提交 `examples/` 未跟踪语料/demo；把跳过的 PG 路径默示为 Pass。

## 触碰路径

| 路径 | 动作 |
|---|---|
| `pom.xml` | 增加 postgresql JDBC（钉版本）；无 dotenv 依赖 |
| `src/main/java/com/ggtest/db/postgres/` | 新增执行器 + 隔离辅助 |
| `src/main/java/com/ggtest/cli/` | argv（含 `--env-file`）、`.env` 加载/合并、URL 校验、`CliSession` 编排；安全 `toString` |
| `src/test/java/com/ggtest/db/postgres/` | 门控执行器/隔离测 |
| `src/test/java/com/ggtest/cli/` | engine/URL/编排/`.env`/凭据测（临时目录） |
| `src/test/java/com/ggtest/runner/RunnerDependencyIsolationTest.java` | 禁止依赖 `db.postgres` |
| `src/test/resources/fixtures/` | PG/CLI 自造小文件 |
| `.env.example` | 新增占位符模板（无真实凭据） |
| `README.md` | CLI/PG/`.env`/门控说明 |
| `workflow/archive/2026/ggtest-pg/dev-notes.md` | Developer 实施后 |

**禁止**：改 Spec/本 Plan 外的 Design 决议；改 `workflow/docs/manager/*`；提交官方大语料、`examples/` 未跟踪文件、真实 `.env`；无关重构；日志/报告/toString 输出凭据。

## 验证

- **最低验证层：L3**。理由：执行器单元测 + CLI 进程级集成（自造 fixtures + 临时 `.env`）可证明合同；PG 为门控外部依赖，无 PG 时 skip 保证不红（P1-PG-3）。L4 官方语料仅用于 **SQLite** 回归（P0-PG-5），不作 PG 零失败硬验收（P1-PG-4）。
- **验证命令**：见「元信息」。
- **预期证据**：

  | 命令/场景 | 预期 |
  |---|---|
  | `mvn -q clean test`（无 PG） | `BUILD SUCCESS`；PG assume-skip；SQLite/ENV（临时）/既有用例 Pass |
  | `mvn -q clean test`（有 `GGTEST_PG_URL`） | PG 专属 Pass；P0-PG-1…4 证据 |
  | `mvn -q clean package` | 可执行 JAR / `./bin/ggtest` 可用 |
  | 仅临时 `.env` 提供 URL | 非退出码 2（用法）；可执行（0 或 1） |
  | CLI `--url` 覆盖 `.env` | 实际连接 URL = CLI |
  | 三处皆无 URL | 退出码 2，无执行 |
  | 无 `.env` + `--url` | 与归档一致 |
  | 显式 `--env-file` 缺失 | 退出码 2 |
  | engine 未知或 URL 错配 | 退出码 2，无执行 |
  | PG 跨文件隔离 fixtures | 失败数=0、退出码 0 |
  | `./bin/ggtest --url jdbc:sqlite::memory: "$SELECT1"` | 失败数=0、退出码=0 |
  | 含 password（CLI 或 `.env`）的输出 | 无密码明文 |
  | `.env.example` + gitignore | 占位键存在；真实 `.env` 被忽略 |

### 无法执行验证时的处理

| 未验证项 | 原因 | 风险 | 恢复条件 | 复测范围 |
|---|---|---|---|---|
| PG 专属 P0 | 无可达 PG / 未设 `GGTEST_PG_URL` | PG 路径未证 | 提供可 CREATE/DROP SCHEMA 的实例与门控变量 | T1–T6 PG 相关 |
| P0-PG-5 | 无自备 `select1.test` | SQLite 硬验收未证 | 设置语料路径后重跑 | T7 / Corpus 回归 |
| 依赖下载 | 网络失败且代理多次失败 | 无法构建 | 网络恢复或可用 Central | `mvn test`/`package` |

禁止将跳过的 PG 硬路径默示为 Pass；须在 `dev-notes.md` / QA 报告记录。ENV 验收不依赖真实 PG（可用 sqlite URL + 临时 `.env`）。

## 验收

对齐 [`spec.md`](./spec.md)（引用，不抄全文）：

| ID | 要点 |
|---|---|
| P0-PG-1 | PG 执行器 statement/query/致命分裂与 NULL 语义 |
| P0-PG-2 | skipif/onlyif 相对 `postgres` 对称、大小写不敏感 |
| P0-PG-3 | CLI 默认 SQLite；(b) PG fixtures；(c)(d) 未知 engine / URL 错配 → 退出码 2 |
| P0-PG-4 | 同批两文件 schema 级隔离，失败数=0 |
| P0-PG-5 | SQLite `select1` 失败数=0、退出码=0（零豁免） |
| P0-ENV-1 | 仅 `.env` 提供 URL 可运行（非用法错误） |
| P0-ENV-2 | CLI `--url` 覆盖 `.env` |
| P0-ENV-3 | 三处皆无 URL → 退出码 2 |
| P0-ENV-4 | 无 `.env` + `--url` 兼容现网 |
| P1-PG-1 | `--engine` 大小写变体 |
| P1-PG-2 | 无密码明文 |
| P1-PG-3 | 无 PG 时默认 `mvn test` 不失败 |
| P1-PG-4 | 官方语料 PG 零失败 **非** Pass 条件 |
| P1-ENV-1 | `.env`/CLI 密码不出现在 stdout/stderr/报告 |
| P1-ENV-2 | `.env.example` 占位符；真实 `.env` 仍被 gitignore |

## 文档影响

| 类别 | 更新路径或 N/A 理由 |
|---|---|
| 开发文档 | `README.md`；`workflow/archive/2026/ggtest-pg/dev-notes.md`；相关 Javadoc；`.env.example` |
| 用户文档 | `README.md`：`--engine postgres`、URL/凭据、schema 隔离与权限、`.env`/`--env-file`/优先级、运行时 `GGTEST_*` vs 门控 `GGTEST_PG_*`；官方语料 PG 零失败非硬验收 |
| 运维文档 | N/A——无独立部署拓扑；门控/权限/代理约定在 README 与 `dev-notes` 覆盖即可 |

## Review 门禁与进入 QA

- Review 门禁：**required**。  
- **进入 QA 条件**：Plan 任务完成；声明的验证已执行或按上表记录缺口；文档影响已落地；**Reviewer 结论为 `Approve`**。  
- standard/full：**禁止**在无 Approve 时进入 QA。  
- Review 门禁是进入 QA 的前置，不是调用 Reviewer 的前置（实现完成后即可请求 Review）。

## 交接顺序

1. **Plan 确认**：用户确认本 Plan → Manager 持久化到工作项 → 状态 `planned` → 调度 Developer（Planner **不得**自行批准）。  
2. **实施**：Developer 在分支 `ggtest-pg` 按 T1→T7 交付，写 `dev-notes.md`。  
3. **Review**：Reviewer 审实现/测试/文档/安全（含凭据与 `.env`）；须 `Approve`。  
4. **QA**：独立按 Spec P0/P1（含 ENV）+ 本 Plan 验证层出 `qa-report.md`。  
5. **完成**：QA Pass 且用户授权后由 Manager 推进 `done`/合并流程（见 git 规范）。

## 修订记录

| 日期 | 摘要 |
|---|---|
| 2026-07-25 | 初稿：Design 后 Plan（仅 PG）；待确认 |
| 2026-07-25 | 更新：纳入 `.env`（T3）；原 T3–T6 顺延为 T4–T7；验收增 P0-ENV/P1-ENV；待用户确认（awaiting-plan-approval） |
