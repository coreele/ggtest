# QA Report: ggtest-cli-report

> **工作项**：`ggtest-cli-report`（未拆分）· **路径**：standard · Review **required**（轮次 1+2+3 Approve）· Design skipped  
> **分支**：`ggtest-cli-report` → `main`  
> **Git**：禁止单独 commit 本报告；Pass 后待用户合并授权；Manager 与 STATUS/`done` 一次提交

## 轮次

| 轮次 | 日期 | 实现版本 / 范围 | 环境 | 结论 |
|---|---|---|---|---|
| 1 | 2026-07-25 | 工作区相对 `HEAD`=`00eaf54`（**未 commit**）；T1–T8；Spec P0/P1 + Plan L3 | macOS arm64；OpenJDK 17.0.20；Maven 3.9.16；SQLite 内存库 | **Pass** |
| 2 | 2026-07-25 | 合入前间距小修回归；同分支工作区相对 `00eaf54`（**未 commit**）；列宽 = `max(最长路径, 60)` | 同轮次 1 | **Pass** |
| 3 | 2026-07-25 | 合入前 `[SQL]` 省略小修回归；同分支工作区相对 `00eaf54`（**未 commit**）；`sqlFirstLine` 多行 `<首行> ...` | 同轮次 1 | **Pass** |

---

## 轮次 3（合入前 `[SQL]` 省略 · 回归）

### 入口门禁

| 条件 | 证据 | 结果 |
|---|---|---|
| Plan 已确认 | 台账 Plan approved；本轮增量合同（Spec 增量）由用户拍板 | 满足 |
| Review required + Approve | `review.md` 轮次 3 **Approve**（阻塞 none） | 满足 |
| 可验收实现 | `CliSession#sqlFirstLine`；`CliReportAcceptanceTest` P0-2 / P1-2；`fail.test` 多行夹具 | 满足 |

### 回归范围与结果

合同：`[SQL]` 取首行并去尾随空白（`stripTrailing`）。范围外（须不回归）：diff / 计数 / 彩色 / 退出码 / 路径列宽。

| ID | 要求 | 结果 | 证据 |
|---|---|---|---|
| R3-1 | 多行 SQL → `[SQL] <首行> ...`，且**不得**输出整段 SQL | **Pass** | jar `multiline-fail.test`（`SELECT name` / `FROM items` / `ORDER BY name`）→ `[SQL] SELECT name ...`；无整段；`#p0_2` 断言含 `...` 且否证整段 |
| R3-2 | 单行 SQL → **无**尾随 ` ...` | **Pass** | jar query 单行 → `[SQL] SELECT v FROM t ORDER BY v`；statement 单行 → `[SQL] INSERT INTO definitely_missing_qa_r3_table VALUES (1)`；`#p1_2` 断言 `endsWith(" ...")` 为 false |
| R3-3 | 不回归 diff：`[Diff] (-expected\|+actual)` + `-`/`+`/上下文；`at <file>:<line>` | **Pass** | jar 失败块含 `apple` / `- bananad` / `+ banana` / `cherry` + `at multiline-fail.test:7` |
| R3-4 | 不回归文件级计数与 `Error:` 仅列失败文件 | **Pass** | 混合（fail+pass+skip）→ `TOTAL: passed=1 failed=1 skipped=1`，`Error:` 仅 `multiline-fail.test`；硬错误+pass → `failed=1 passed=1` |
| R3-5 | 不回归彩色与优先级 CLI > `ggtest.color` > `GGTEST_COLOR` > `auto` | **Pass** | ANSI 计数：`--color always`=6；`never`=0；非 TTY `auto`=0；CLI `never` 压过属性/env `always`=0；属性 `never` 压过 env `always`=0；属性 `always` 压过 env `never`=6；env `always`=6。彩色下 `[SQL] SELECT name ...` 仍省略 |
| R3-6 | 不回归退出码 `0`/`1`/`2`（与计数独立） | **Pass** | 全通过=0；query 失败=1；statement 失败=1；混合=1；解析硬错误+pass=**2**；`--color bogus`=2；缺 `--url`=2 |
| R3-7 | 不回归路径列宽 = `max(最长路径, 60)`，路径 ≥60 不截断 | **Pass** | 短路径 → 路径列 60（`.. ` 起于第 62 列）；98 字符长路径 → 列 98 未截断 |
| R3-8 | 无旧式标签 `FILE:` / `PASS in` / `[OK]` / `] after ` / 整段 `reason=` | **Pass** | jar 全部输出检索 → none |
| R3-9 | 凭据不入输出 | **Pass** | `--user qauser --password S3cretQaR3` → 输出中密码出现 0 次 |
| R3-10 | Plan 验证层 L3 | **Pass** | 见命令 |

### 环境与命令

- 分支 `ggtest-cli-report`；相对 `HEAD`=`00eaf54` 工作区（未 commit）
- `mvn -q test` → 0
- 聚焦 7 类（`CliReportAcceptanceTest`、`MainOrchestrationTest`、`EnvConfigIntegrationTest`、`ResultComparerTest`、`CliArgumentParserTest`、`RuntimeConfigResolverTest`、`NormalizeAcceptanceTest`）→ Tests run 63、Failures 0、Errors 0、BUILD SUCCESS
- `mvn -q -DskipTests package` → 0 → `target/ggtest-0.1.0-SNAPSHOT.jar`
- jar E2E 证据 `/tmp/ggtest-qa-r3/`（多行失败 / 单行失败 / statement / 混合 / 硬错误 / 长路径 / 彩色优先级 / 凭据）

### 文档 / 安全（增量）

| 项 | 结果 |
|---|---|
| 文档 | Spec `[SQL]` 呈现节 + 样例、Plan T2/T6、README 失败样例、`dev-notes` 本轮节均与实现一致（`[SQL] SELECT name ...`） |
| 安全 | 仅缩短报告中 SQL 呈现；无凭据/认证/新依赖/出站变化；P0-3 重测通过 |

### 缺陷

无。

### 缺口（不阻塞 Pass）

| 未验证项 | 原因 | 风险 | 恢复条件 | 复测 |
|---|---|---|---|---|
| TTY 下 `auto` 真彩色 | 无交互 `System.console()` | 低 | 本地终端不重定向跑 jar | P1-4 TTY |
| Corpus 硬验收 | `GGTEST_CORPUS_DIR` 未设 | 低 | 设目录后 `mvn test` | `CorpusHardAcceptanceTest` |
| Postgres CLI | `GGTEST_PG_URL` 未设 | 低 | 设 gate 后跑 | `PostgresCliIntegrationTest` |

已闭合：轮次 2 缺口 / 建议 R2-N1「路径 >60 未测」→ R3-7；`dev-notes` 与 `review.md` 轮次 3 缺口「本轮未跑 package/jar」→ 本轮 package + jar E2E。

### 结论（轮次 3）

- **Pass** — `[SQL]` 省略合同符合 Spec 增量；轮次 1+2 合同未回归；缺陷 none
- 合并：待用户授权（**不** commit / **不** merge）
- 后续建议：**merge-auth**

---

## 轮次 2（合入前间距小修 · 回归）

### 入口门禁

| 条件 | 证据 | 结果 |
|---|---|---|
| Plan 已确认 | 台账 Plan approved | 满足 |
| Review required + Approve | `review.md` 轮次 2 **Approve** | 满足 |
| 可验收实现 | `STATUS_PATH_COLUMN_WIDTH=60`；`#statusLinePathColumnUsesMaxOfLongestPathAndSixty` | 满足 |

### 回归范围与结果

| ID | 要求 | 结果 | 证据 |
|---|---|---|---|
| R2-1 | 列宽 = `max(最长路径, 60)`；短路径与 `..` 间距充足（约 60 列） | **Pass** | `pathWidth` 自 60 `Math.max`；jar path_len=41 → col=60、pad=19；列宽测试通过 |
| R2-2 | 标签 `.. [PASSED]` / `.. [FAILED] in` / `.. [SKIPPED]`（无耗时） | **Pass** | jar pass/fail/skip；无 `[OK]`/`after`/`FILE:` |
| R2-3 | 不回归 diff/计数/彩色/退出码；TOTAL·退出码仍合理 | **Pass** | fail 仍有 WHY/SQL/Diff/at；exit 0/1/1/2（pass/fail·mix/hard）；`TOTAL` 文件级；彩色聚焦测试通过 |
| R2-4 | `mvn -q test` + 聚焦 + jar 一眼 | **Pass** | 见命令 |

### 环境与命令

- 分支 `ggtest-cli-report`；相对 `00eaf54` 工作区（未 commit）
- `mvn -q test` → 0
- 聚焦：`CliReportAcceptanceTest`、`MainOrchestrationTest`、`EnvConfigIntegrationTest`、`ResultComparerTest`、`CliArgumentParserTest`、`RuntimeConfigResolverTest` → 0
- `mvn -q -DskipTests package` → 0；jar 证据 `/tmp/ggtest-qa-r2-e2e/`（pass/fail/skip/mix/hard）

### 文档 / 安全（增量）

| 项 | 结果 |
|---|---|
| 文档 | Spec/README 未改；`dev-notes` 已记列宽下限 |
| 安全 | 仅填充宽度；无凭据/认证/新依赖/出站变化 |

### 缺陷

无。

### 结论（轮次 2）

- **Pass** — 间距小修符合台账；轮次 1 合同未回归；缺陷 none
- 合并：待用户授权（**不** commit / **不** merge）
- 后续建议：**merge-auth**

---

## 轮次 1（首轮 · 保留）

### 入口门禁

| 条件 | 证据 | 结果 |
|---|---|---|
| Plan 已用户确认并持久化 | 台账 Plan approved；状态至 `qa` | 满足 |
| Spec 已用户确认 | `spec.md` approved；Q-R1…Q-R8 已决 | 满足 |
| Review required 且 Approve | `review.md` 轮次 1 **Approve** | 满足 |
| 可验收实现与 Plan 验证 | `com.ggtest.cli` + `CliReportAcceptanceTest`；L3 可执行 | 满足 |

### 环境与命令

- 工作区相对 `00eaf54`（未 commit）
- `mvn -q test` → 0；报告相关 9 类复跑 → 0
- `mvn -q -DskipTests package` → 0；jar E2E `/tmp/ggtest-qa-e2e/`（成功/跳过/失败/混合/statement/硬错误/用法；彩色优先级；凭据冒烟）

### Spec 验收（P0 / P1）

| ID | 要求 | 结果 | 证据 |
|---|---|---|---|
| P0-1 | 码 `0`；相对路径；无 `FILE:`；`.. [PASSED] in N ms`；`TOTAL` 文件数 | **Pass** | jar + `#p0_1` |
| P0-1（跳过） | `.. [SKIPPED]` 无耗时；计 `skipped` | **Pass** | jar pass+skip-all |
| P0-2 | 码 `1`；`[FAILED] in` + WHY/SQL/Diff/at；Error；禁 `reason=`/`after` | **Pass** | jar + `#p0_2` |
| P0-3 | 输出无密码明文 | **Pass** | jar + `#p0_3` |
| P0-4 | Q-R5=(B) 旧耦合测试已改且通过 | **Pass** | `mvn test`；禁 `FILE:`/`PASS in` |
| P1-1 | 混合顺序；失败内联；Error 仅失败；文件级 TOTAL；码 `1` | **Pass** | jar + `#p1_1` |
| P1-2 | statement：`[FAILED] in` + WHY/SQL + at；码 `1` | **Pass** | jar + `#p1_2` |
| P1-3 | 硬错误计 `TOTAL.failed`；码 `2`；勿冒充全通过 | **Pass** | jar bad-parse+pass；缺 `--url` → 2 |
| P1-4 | `always` 有 ANSI；`never`/非 TTY `auto` 无 | **Pass** | jar + `#p1_4` |
| P1-5 | CLI > `ggtest.color` > `GGTEST_COLOR` > `auto` | **Pass** | `#p1_5` |

### Plan 验证（L3）

| 项 | 结果 | 证据 |
|---|---|---|
| `mvn -q test` / package / jar E2E | **Pass** | 退出码 0；`/tmp/ggtest-qa-e2e/` |
| 状态行合同与退出码独立 | **Pass** | 标签合同；硬错误→2 / 断言失败→1 / 全过→0 |

### 回归 / 文档 / 安全

| 范围 | 结果 |
|---|---|
| 全量 `mvn -q test` + 报告 9 类 | Pass |
| Corpus / Postgres 门控 | 环境跳过（不阻塞） |
| README / Javadoc / `dev-notes` | Pass |
| 凭据入输出（P0-3）；无真实 `.env`；无新依赖/出站 | 通过 |

### 缺陷

无。

### 缺口（不阻塞 Pass）

| 未验证项 | 原因 | 风险 | 恢复条件 | 复测 |
|---|---|---|---|---|
| TTY 下 `auto` 真彩色 | 无交互 `System.console()` | 低 | 本地终端不重定向跑 jar | P1-4 TTY |
| Corpus 硬验收 | `GGTEST_CORPUS_DIR` 未设 | 低 | 设目录后 `mvn test` | `CorpusHardAcceptanceTest` |
| Postgres CLI | `GGTEST_PG_URL` 未设 | 低 | 设 gate 后跑 | `PostgresCliIntegrationTest` |

### 结论（轮次 1）

- **Pass** — Spec P0/P1 + Plan L3 独立通过；Review Approve；缺陷 none

---

## 最新结论

- **总体：Pass**（最新轮次 = 3）
- 恢复条件：N/A
- 合并：待用户授权（本 Agent **不** commit / **不** merge）
- 质量条件：轮次 1+2+3 均 Pass；Review 轮次 3 Approve 已持久化；无未解决缺陷/安全问题

### 后续建议

- **merge-auth**：用户明确授权后，Manager 置 `done` 并与 `review.md`/`qa-report.md` 一次提交，再合入 `main`
