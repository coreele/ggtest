# Plan: chore-audit-tails

## 元信息

- 工作项标识: chore-audit-tails（未拆分，sub-feature-id = feature-id）
- 依据 Spec: N/A（Spec 门禁 skipped；范围以工作项记录与用户拍板为准）
- 依据 Design: N/A（Design 门禁 skipped）
- 路径等级: fast
- Review 门禁: required（进入 QA 前须 Reviewer `Approve`）
- 最低验证层: L3（单元/可控集成 + 构建）；语料硬验收为 L4 冒烟（缺环境按 quality.md §6 记证，禁止默示 Pass）
- 验证命令: 见「验证」节
- 源分支: `chore-audit-tails` → 目标 `main`
- 本轮约束: 不要 commit / push / merge（除非父会话另说）

## 适用工程规范

- [文档工程](../../standards/documentation.md)
- [Git 协作](../../standards/git.md)
- [质量与验证](../../standards/quality.md)
- [安全](../../standards/security.md)

## 目标摘要

收口四项尾巴：CA-008 Javadoc + 登记册；CLI `--color auto` 在 TTY/可控条件下证明着色开启；PG 非空密码路径 + DEF-PG-003 回归证据；官方语料 select1–5（或 `GGTEST_CORPUS_DIR`）sqlite 硬验收冒烟并写入 `dev-notes.md`。

### 明确排除

| 项 | 指令 |
|---|---|
| CA-007 ResultComparer LCS | 禁止改代码、禁止列入任务、禁止登记为本项工作 |
| 重开 `architecture-overview` | 禁止 |
| 真实 `.env` / 凭据 / 强制入库 `.temp/select*.test` | 禁止（gitignore 保留） |

## 任务拆解

### T1 — CA-008：ValueNormalizer 非法 I/R Javadoc + 登记册

- 做：在 `ValueNormalizer`（必要时 `normalizeInteger`/`normalizeReal`）Javadoc 标明非法 I/R → `"0"` / `"0.000"`，对齐 sqllogictest，非吞错缺陷。更新 `workflow/audit/register.md` CA-008：状态可仍 `accepted`；「建议下一步」改为文档已补（如 `Javadoc done; monitor only`）；更新日期。
- 不做：改归一化行为或测试期望。
- 完成条件：Javadoc 可读；登记册反映文档已补；`mvn -q test` 无回归。

### T2 — CLI：TTY / `--color auto` 着色开启路径

- 缺口：非 TTY 下 `auto` 无 ANSI 已证；TTY 下 `auto` 开启缺可控证据。`Main` 现用 `System.console() != null`。
- 做：
  1. 为 `resolveAnsiEnabled(AUTO, tty=…)` 与/或 Main 编排补可控测试（优先可注入 `isTty` / BooleanSupplier 或伪 TTY；禁止仅依赖本机交互 console）。
  2. 断言：`auto`+tty → ANSI；`auto`+非 tty → 无；`always`/`never` 不变。
  3. 有缺陷则修（无注入时产品默认仍 `System.console() != null`）。
  4. 可选：未重定向终端跑 jar/`bin/ggtest --color auto` 作补充（写入 notes；不替代可控单测）。
- 完成条件：自动化证明 `auto`+tty 开启；`mvn -q test` 绿；缺口关闭或 §6 记证。

### T3 — PG：非空 `GGTEST_PG_PASSWORD` + DEF-PG-003 回归

- 做：
  1. 非空密码路径：覆盖 `passwordIsNeverPrintedWhenRunningPostgres`（及必要连接装配）。无非空密码时用替身/临时 env/注入；勿把真实密码写入仓库或日志；能连则实跑，不能则 §6 记 skip（禁止编造 Pass）。
  2. DEF-PG-003：根 `.env`（postgres）在场时，隔离测不污染——无门控全绿（PG skip 可接受）；有门控则 PG 亦跑；产品读 CWD `.env` 不变（冲突仍 exit 2）。命令与数值写入 `dev-notes.md`。
- 完成条件：非空密码 Pass 或完整 §6 记录；DEF-PG-003 有可复现证据；无凭据入库。

### T4 — 语料硬验收冒烟（select1–5 / `GGTEST_CORPUS_DIR`）

- 做：对 `.temp/select1.test`…`select5.test` 或 `GGTEST_CORPUS_DIR` 做 sqlite 冒烟（pg 探索可选、非阻断）：
  - `GGTEST_CORPUS_DIR=… mvn -q test -Dtest=CorpusHardAcceptanceTest`（现有 select1、select1–3）；
  - select4/5 及全套 1–5：`./bin/ggtest` 或等价，`--url jdbc:sqlite::memory:`，期望 exit 0 且 `failed=0`（或如实记失败）。
- 完成条件：命令、exit、`TOTAL`/failed 写入本目录 `dev-notes.md`；未知/跳过须原因→风险→恢复条件→复测范围；禁止未跑标 Pass。
- 不做：强制入库 `.temp/select*.test`。

### T5 — 开发产物

- 写本目录 `dev-notes.md`（实现摘要、验证表、§6 缺口、建议复测）。
- 完成条件：Reviewer/QA 可凭 plan + notes 复现；未改 `workflow/docs/manager/*` / `STATUS.md`。

## 依赖与顺序

T1–T4 彼此独立，均可并行；全部完成后做 T5，再 Review → QA。建议落地顺序 T1→T2→T3→T4→T5；最终以全量 `mvn -q clean test` 为准。T3/T4 并行时注意本机 `.env`/门控互不污染。

## 触碰路径

| 任务 | 预期路径 |
|---|---|
| T1 | `src/main/java/com/ggtest/normalize/ValueNormalizer.java`；`workflow/audit/register.md` |
| T2 | `cli/Main.java`（必要时 `RuntimeConfigResolver`）；`src/test/java/com/ggtest/cli/*`（color/TTY） |
| T3 | `PostgresCliIntegrationTest` 等；仅缺陷时触碰 `Main`/`FileRunner`/PG 执行器（产品 `.env` 合同不变） |
| T4 | 通常仅命令 + `dev-notes`；可选扩展 `CorpusHardAcceptanceTest` |
| T5 | `workflow/archive/2026/chore-audit-tails/dev-notes.md` |
| 禁止 | ResultComparer LCS（CA-007）；`workflow/docs/manager/*`；`STATUS.md`；真实 `.env`；入库 `.temp/select*.test` |

## 验收

（fast / 无 Spec）

| ID | 要求 | 证据 |
|---|---|---|
| A1 | CA-008 Javadoc + 登记册已更新；行为未改 | diff 仅注释/登记册；normalize 测绿 |
| A2 | `auto`+tty 着色开启有可控自动化证据 | 新/改测 Pass；或 §6 |
| A3 | 非空 `GGTEST_PG_PASSWORD` 已测或 §6；密码不回显 | notes；无凭据落盘 |
| A4 | DEF-PG-003：根 `.env` 在场隔离测不污染 | `mvn` 数值入 notes |
| A5 | select1–5（或 CORPUS_DIR）sqlite 冒烟已记；跳过有证据 | notes |
| A6 | 未改 CA-007 / ResultComparer LCS | diff |

## 验证

### 命令

```bash
mvn -q clean test
mvn -q -DskipTests package

# T2 定点（类名以实装为准）
mvn -q test -Dtest='*Color*,*Report*,RuntimeConfigResolverTest,CliReportAcceptanceTest'

# T3 有 PG 时（密码本机临时值，勿入库）
GGTEST_PG_URL='jdbc:postgresql://…' GGTEST_PG_USER='…' GGTEST_PG_PASSWORD='…' \
  mvn -q test -Dtest=PostgresCliIntegrationTest,PostgresJdbcExecutorTest,PostgresSchemaIsolationTest

# T3 DEF-PG-003：无门控 / 有门控各一次，记录 run/fail/skip
mvn -q clean test
GGTEST_PG_URL='…' GGTEST_PG_USER='…' GGTEST_PG_PASSWORD='…' mvn -q clean test

# T4（本机 .temp 或 CORPUS_DIR）
GGTEST_CORPUS_DIR="$PWD/.temp" mvn -q test -Dtest=CorpusHardAcceptanceTest
./bin/ggtest --url jdbc:sqlite::memory: \
  .temp/select1.test .temp/select2.test .temp/select3.test .temp/select4.test .temp/select5.test
# 无语料则 §6 记跳过
```

### 最低验证层理由

触及 CLI 着色注入、PG 门控、登记册/注释 → 至少 L3。语料零失败硬验收 → L4 冒烟；缺环境则记缺口，不因语料缺失否定 T1–T3，但 A5 不得标 Pass。

### 预期证据

| 验证 | 通过时 |
|---|---|
| `mvn -q clean test` | Failures=0；Skipped 在 notes 说明 |
| `package` | BUILD SUCCESS |
| T2 | `auto`+tty → ANSI；非 tty → 无 |
| T3 门控 | 非空密码 Pass，或 assume skip + §6 |
| T4 | exit 0 且 failed=0（或如实失败）；跳过有表 |

### 无法验证（quality.md §6）

`dev-notes.md` 表：未验证项 → 原因 → 风险 → 恢复条件 → 复测范围。禁止静默跳过、禁止编造 Pass。

## Review 门禁与进入 QA

- Review：required。
- 进入 QA：T1–T5 完成；L3 绿（或已记不可执行项）；notes 含 T3/T4 证据；Reviewer Approve。
- 虽为 fast，Review 未 skipped → 不得跳过 Review 直接 QA。

## 文档影响

| 类别 | 更新路径或 N/A |
|---|---|
| 开发文档 | `ValueNormalizer` Javadoc；`code-audit-register.md`（CA-008）；本目录 `dev-notes.md`；本 `plan.md` |
| 用户文档 | N/A（无新公开合同；默认不改 README） |
| 运维文档 | N/A（无部署/排障变更） |

## 交接顺序

1. Planner：本 plan → 用户/Manager 确认持久化 → `planned`（Planner 不改状态）。
2. Developer：分支 `chore-audit-tails` 实施 T1–T5；写 notes；本轮不 commit（除非父会话授权）。
3. Reviewer：对照 plan + A1–A6 → Approve / 回退。
4. QA：独立复跑 → `qa-report.md` Pass/Fail/Blocked。
5. 合并：仅用户授权后由 Manager 流程处理。

## 修订记录

| 日期 | 摘要 |
|---|---|
| 2026-07-26 | 初稿并 refine：T1–T5；排除 CA-007；L3+L4 冒烟；Review required |
