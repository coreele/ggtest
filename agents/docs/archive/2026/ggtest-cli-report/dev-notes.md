# Dev Notes: ggtest-cli-report

> 适用对象：Reviewer、QA、Manager  
> 前置条件：JDK 17+、Maven；工作分支 `ggtest-cli-report`  
> 操作步骤：按下方验证命令复现；对照 Spec P0/P1（含 `[SQL]` 首行/` ...`）  
> 预期结果：L3 全过；报告布局与彩色优先级符合 Spec  
> 失败处理：见「缺口」表；恢复后复测对应范围

工作项: ggtest-cli-report（未拆分）· standard · Review required  
分支: `ggtest-cli-report` → `main` · 2026-07-25  
**本阶段未 commit / merge / push**（用户指令）

## 合入前小修（`[SQL]` 多行省略 · 2026-07-25）

失败块 `[SQL]`：首行（去尾随空白）；去除首行后仍有非空白 → `<首行> ...`；纯单行不加。范围外：diff / 计数 / 彩色 / 退出码 / 路径列宽。

| 项 | 内容 |
|---|---|
| 代码 | `CliSession#sqlFirstLine`：首行 + 非空余量时追加 ` ...` |
| 夹具 | `fail.test` → 多行 `SELECT name`/`FROM items` |
| 测试 | P0-2：`[SQL] SELECT name ...`；P1-2：单行 INSERT **无**尾随 ` ...`（`CliReportAcceptanceTest`） |
| 文档 | README 失败样例 `[SQL] SELECT name ...` |
| 验证 | `mvn -q test` → **通过** |
| 合同偏差 | none |

## 合入前小修（路径列宽下限 · 2026-07-25）

列宽 = `max(本次最长路径, 60)`（`STATUS_PATH_COLUMN_WIDTH`）；路径 ≥60 不截断。范围外：diff / 计数 / 彩色 / 退出码。

| 项 | 内容 |
|---|---|
| 代码 | `CliSession`：`pathWidth` 自 60 起取 `Math.max` |
| 测试 | `CliReportAcceptanceTest#statusLinePathColumnUsesMaxOfLongestPathAndSixty` |
| 验证 | `mvn -q test` → **通过** |
| 合同偏差 | none |

## 实现摘要

人类可读 CLI 报告（Spec）。T1–T8 完成；合入前增量：列宽下限 + `[SQL]` 省略。

| 任务 | 要点 |
|---|---|
| T1 | 相对 CWD；`[PASSED]`/`[FAILED] in`/`[SKIPPED]`；文件级 TOTAL；退出码独立；列宽 `max(最长, 60)` |
| T2 | `[WHY]`/`[SQL]`/`[Diff]` + `at`；`[SQL]` 首行/` ...`；`ResultComparer` 只改呈现 |
| T3 | 混合顺序；Error 仅失败；成功行间无空块 |
| T4 | `--color` > `ggtest.color` > `GGTEST_COLOR` > `auto` |
| T5 | 硬错误同视觉、计 `failed`、码 `2`；用法 `Error: usage` + `[WHY]` |
| T6 | Q-R5=(B) + `CliReportAcceptanceTest`（含多行/单行 `[SQL]`） |
| T7 | README（`[SQL] SELECT name ...`）+ Javadoc |
| T8 | 本文件 |

不变量：比较语义；退出码 `0`/`1`/`2`；凭据不进输出。

## 变更路径

- 主代码：`cli/{CliSession,Main,CliArgumentParser,ParsedArguments,CliOptions,RuntimeConfigResolver,ColorMode,ReportStyle}.java`；`normalize/ResultComparer.java`
- 测试：`CliReportAcceptanceTest`；夹具 `fail.test`（多行）、`skip-all.test`、`statement-fail.test`；及 Plan 所列受影响测试
- 文档：`README.md`；本文件
- **禁止触碰已遵守**：`agents/docs/manager/*`、`examples/` 未跟踪样例、`.env*`（Spec/Plan 由 Analyst/Planner 回写，本轮未改）

## 验证证据（L3 · quality.md §1）

| 验证 | 结果 |
|---|---|
| `mvn -q test` | **通过**（含 `[SQL]` 省略与列宽；本轮必证） |
| `mvn -q -DskipTests package` | **通过**（主线；本轮未重跑）→ `target/ggtest-0.1.0-SNAPSHOT.jar` |
| jar 成功 / 失败 / 混合 / 硬错误 | 主线已证（exit 0/1/1/2；计数与标签符合 Spec） |
| 彩色优先级 | always 有 ANSI；never / 非 TTY auto / 属性优于 env / CLI 覆盖 — 主线已证 |

P0/P1（含 P0-2 多行 ` ...`、P1-2 单行不加、P1-1、P1-4、P1-5）与列宽由 `CliReportAcceptanceTest` 自动化。

### 缺口（quality.md §6）

| 未验证项 | 原因 | 风险 | 恢复条件 | 复测 |
|---|---|---|---|---|
| TTY 下 `auto` 真彩色 | 无交互 `System.console()` | 低 | 本地终端不重定向跑 jar | P1-4 TTY |
| Corpus 硬验收 | `GGTEST_CORPUS_DIR` 未设 | 低 | 设目录后 `mvn test` | `CorpusHardAcceptanceTest` |
| Postgres CLI | `GGTEST_PG_URL` 未设 | 低 | 设 gate 后跑 | `PostgresCliIntegrationTest` |
| 本轮 jar 冒烟 | Plan 可选；`mvn test` 已覆盖 `[SQL]` | 低 | 需要时 `package` + jar | P0-2/P1-2 |

## 文档影响

README：成功/失败/混合、彩色优先级、文件计数与退出码独立；失败样例 `[SQL]` 已对齐多行省略。Javadoc 同步。

## 风险

1. `TOTAL` 为**文件数**；依赖旧 `FILE:`/query 计数的脚本需改。
2. CWD `.env` 含 `GGTEST_ENGINE=postgres` 时，jar 对照须 `--engine sqlite`（或匹配 URL）。
3. `examples/demo*.slt` 为故意失败样例，勿作成功基线。

## 合同偏差

none

## 建议后续

Reviewer（`[SQL]` 省略短审 → Approve → QA 回归 → 合并授权）
