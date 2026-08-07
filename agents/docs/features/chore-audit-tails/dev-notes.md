# Dev Notes: chore-audit-tails

## 实现说明

- 分支：`chore-audit-tails` → `main`。本轮未 commit / push / merge。
- **T1 done**：`ValueNormalizer` Javadoc — 非法 I/R → `"0"` / `"0.000"`（CA-008）；登记册「建议下一步」→ `Javadoc done; monitor only`。行为未改。
- **T2 done**：`Main.run(..., BooleanSupplier isTty)`；产品默认 `System.console() != null`。测证：`auto`+tty → ANSI；`auto`+非 tty → 无；`always`/`never` 不变。
- **T3 done + §6 部分**：可控非空密码不回显 + 装配测 Pass；DEF-PG-003 无门控全绿 + 产品 CWD `.env` 错配 exit 2。门控真库实连未通（§6）。
- **T4 done**：`.temp/select1..5` sqlite 冒烟 exit 0 / `failed=0`；`CorpusHardAcceptanceTest` 2/0/0。
- **T5 done**：本文件。
- **禁止项遵守**：未改 CA-007 / `ResultComparer`；未改 `agents/docs/manager/*`（Developer）；无凭据入库；未强制入库 `.temp/select*.test`。

### 变更路径

| 任务 | 路径 |
|---|---|
| T1 | `ValueNormalizer.java`；`agents/docs/standards/code-audit-register.md` |
| T2 | `Main.java`；`RuntimeConfigResolverTest`；`CliReportAcceptanceTest` |
| T3 | `PostgresCliIntegrationTest`；`RuntimeConfigResolverTest` |
| T4 | 命令 only |
| T5 | `agents/docs/features/chore-audit-tails/dev-notes.md` |

### 验证

| 命令 | 结果 |
|---|---|
| `mvn -q clean test`（根 `.env` postgres 在场；无 `GGTEST_PG_*`） | Tests=**224** Failures=**0** Errors=**0** Skipped=**18** |
| `mvn -q -DskipTests package` | BUILD SUCCESS |
| `mvn -q test -Dtest=RuntimeConfigResolverTest,CliReportAcceptanceTest` | Tests=**35** / 0 / 0 / 0 |
| `nonEmptyPasswordNeverPrintedWhenPostgresConnectionFails` | Pass（connection failed → exit 2；密码不在 stdout/stderr） |
| `nonEmptyPasswordFromCliIsAssembledForPostgres` / `…FromProcessEnv…` | Pass |
| `./bin/ggtest --url jdbc:sqlite::memory: …/pass.test`（CWD `.env`） | exit **2**（engine postgres ↔ sqlite URL） |
| `GGTEST_PG_*`←本地 `.env` + PG 套件 | Fail/Error：`connection attempt failed` — **非 Pass** |
| `GGTEST_CORPUS_DIR=$PWD/.temp mvn -q test -Dtest=CorpusHardAcceptanceTest` | Tests=**2** / 0 / 0 / 0 |
| `./bin/ggtest --engine sqlite --url jdbc:sqlite::memory: .temp/select{1,2,3}.test` | exit 0；`TOTAL: passed=3 failed=0 skipped=0` |
| 同上 `select{4,5}` | exit 0；`TOTAL: passed=2 failed=0 skipped=0` |
| 同上 `select1..5` | exit 0；`TOTAL: passed=5 failed=0 skipped=0` |

无门控 Skipped=18：PG 门控 14 + Corpus（无 `GGTEST_CORPUS_DIR`）2 + `ExecutableJarManifestTest`（`clean test` 无 jar）2。

### 无法验证（quality.md §6）

| 未验证项 | 原因 | 风险 | 恢复条件 | 复测范围 |
|---|---|---|---|---|
| 门控真库 PG（含 `passwordIsNeverPrintedWhenRunningPostgres`） | `.env`→`GGTEST_PG_*` 后 JDBC `connection attempt failed`（5432 开）；未探查凭据 | 真库非空密码端到端未证（可控合成路径已证不回显） | 可连的非空 `GGTEST_PG_URL`/`USER`/`PASSWORD` | PG CLI/executor/schema/`FileRunner` PG 测；全量 `mvn -q clean test` |
| 有门控全量 `mvn -q clean test` | 同上 | DEF-PG-003「门控开亦绿」未复现 | 同上 | 有门控全量 `mvn -q clean test` |

### 建议复测

1. Reviewer：A1–A6；未改 CA-007；T2 ANSI + T3 可控密码不回显。
2. QA：根 `.env` 无门控 `mvn -q clean test`；`package`；select1–5 / `GGTEST_CORPUS_DIR`；产品错配 exit 2。
3. 可连 PG 时：非空 `GGTEST_PG_PASSWORD` 补跑门控套件。

## QA 修复回执

| 缺陷 ID | 处理 | 摘要 | 验证 | 建议复测 |
|---|---|---|---|---|
| — | N/A | 本轮无 QA Fail | — | — |
