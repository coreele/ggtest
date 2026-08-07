# QA Report: chore-audit-tails

## 轮次

| 轮次 | 日期 | 范围 | 结论 |
|---|---|---|---|
| 1 | 2026-07-26 | 首测（独立验收 + 回归；Plan A1–A6） | Pass |

## 环境与命令

- 工作区：`/Users/zhougangjie/Space/ggtest`；分支 `chore-audit-tails`（未切换/合并）；HEAD `a11fd84` + 未提交工作树
- 入口：Plan **approved**；Review **required** 且 `review.md` **Approve**
- 根 `.env` 在场；无门控全量时进程未设 `GGTEST_PG_*` / `GGTEST_CORPUS_DIR`；`.temp/select1..5.test` 可用

| 命令 | 结果（QA 实跑） |
|---|---|
| `mvn -q clean test`（无 `GGTEST_PG_*`） | Tests=**224** Failures=**0** Errors=**0** Skipped=**18**；EXIT 0 |
| `mvn -q -DskipTests package` | BUILD SUCCESS；EXIT 0 |
| `mvn -q test -Dtest=RuntimeConfigResolverTest,CliReportAcceptanceTest,PostgresCliIntegrationTest` | Tests=**40** Failures=**0** Errors=**0** Skipped=**4**；EXIT 0（23+12+5；其中 PG 可控非空密码测 OK，门控 4 skip） |
| `GGTEST_CORPUS_DIR=$PWD/.temp mvn -q test -Dtest=CorpusHardAcceptanceTest` | Tests=**2** / 0 / 0 / 0；EXIT 0 |
| `./bin/ggtest --engine sqlite --url jdbc:sqlite::memory: .temp/select{1..5}.test` | EXIT **0**；`TOTAL: passed=5 failed=0 skipped=0` |
| `./bin/ggtest --url jdbc:sqlite::memory: …/cli/pass.test`（CWD `.env`） | EXIT **2**；`engine 'postgres' requires a jdbc:postgresql: URL` |
| `.env`→`GGTEST_PG_*` + postgres `…/pg/basic.test` | EXIT **2**；`connection attempt failed`；无密码回显；**非 Pass**（§6） |

Skipped=18：PG assume **14** + Corpus 无 env **2** + jar 清单（`clean test` 无 jar）**2**。

## 覆盖（对照 plan 最低验证层 + 验收 A1–A6）

| ID | 条目 | 结果 | 证据 |
|---|---|---|---|
| A1 | CA-008 Javadoc + 登记册；行为未改 | Pass | `ValueNormalizer` 仅 Javadoc（非法 I/R → `"0"` / `"0.000"`）；CA-008「建议下一步」→ `Javadoc done; monitor only`；normalize 随全量绿 |
| A2 | `auto`+tty 着色可控证据 | Pass | `isTty` 可注入；默认 `System.console() != null`。`p1_4_colorAutoUsesInjectedTty` + `resolveAnsiEnabled*`：auto+tty→ANSI；auto+非 tty→无；always/never 不变 |
| A3 | 非空密码已测或 §6；永不回显 | Pass（真库 §6） | 可控 `nonEmptyPasswordNeverPrinted…` OK（合成密码、`127.0.0.1:1`、exit 2、无回显）；装配测 OK；门控真库实连失败 → §6 |
| A4 | DEF-PG-003 隔离不污染；产品 `.env` 合同不变 | Pass（有门控全量 §6） | 无门控 224/0/0/18；隔离 `envLookup→null`+`@TempDir`；错配 exit 2；有门控全量 §6 |
| A5 | select1–5 / CORPUS_DIR 冒烟 | Pass | Corpus 2/0/0；select1–5 EXIT 0 / `failed=0` |
| A6 | 未改 CA-007 / ResultComparer | Pass | 无 `ResultComparer` diff；登记册 CA-007 未改 |

L3 满足；L4 语料本机已实跑 Pass。

## 缺陷

| ID | 严重度 | 摘要 | 状态 |
|---|---|---|---|
| — | — | 无 | N/A |

## 无法验证（quality.md §6）

| 未验证项 | 原因 | 风险 | 恢复条件 | 复测范围 |
|---|---|---|---|---|
| 门控真库 PG（含 `passwordIsNeverPrintedWhenRunningPostgres`） | `.env`→`GGTEST_PG_*` 后 JDBC `connection attempt failed`（5432 开）；未探凭据 | 真库非空密码 E2E 未证（可控路径已证不回显） | 可连非空 `GGTEST_PG_URL`/`USER`/`PASSWORD` | PG 门控套件；该不回显测 |
| 有门控全量 `mvn -q clean test` | 同上 | 「门控开亦绿」未复现 | 同上 | 有门控全量 |

## 文档与安全

| 检查 | 结论 |
|---|---|
| 用户/运维文档 | N/A（无公开合同变更） |
| 开发文档 | Javadoc、CA-008、`dev-notes.md` 与 Plan 一致；本报告独立复跑 |
| 敏感信息 / 密码回显 | Pass：合成口令；无真实凭据入库/写入报告；可控与真库失败路径均无回显 |
| 依赖变更 | N/A |

安全发现项：无。质量条件允许请求合并授权；须用户明确授权后由 Manager 处理（本报告未 commit）。

## 结论

- 总体: **Pass**
- 恢复条件: N/A
- 合并: 待用户授权

## 修订记录

| 日期 | 摘要 |
|---|---|
| 2026-07-26 | 轮次 1 Pass；A1–A6；真库/有门控全量 §6；documentation.md §B 自检 |
