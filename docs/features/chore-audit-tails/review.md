# Review: chore-audit-tails

## 审阅范围

- 工作项：`chore-audit-tails`（未拆分；fast；Review required）
- 源分支：`chore-audit-tails`（工作区未提交）
- 依据：`plan.md`（approved）、`dev-notes.md`、`docs/manager/chore-audit-tails.md`；`quality.md` §6、`security.md`、`git.md`
- Spec / Design：N/A（skipped）
- 实现版本：相对 `HEAD`（`a11fd84`）未提交 diff；Manager 状态文件不计入实现验收
- 审阅改动：`ValueNormalizer.java`；`code-audit-register.md`（CA-008）；`Main.java`（`BooleanSupplier isTty`）；`CliReportAcceptanceTest` / `RuntimeConfigResolverTest` / `PostgresCliIntegrationTest`；`dev-notes.md`
- 排除核对：`ResultComparer` / CA-007 无 diff

## 结论

**Approve**

无阻塞项。A1–A6 满足进入 QA；真库门控 PG 与有门控全量已按 §6 记证，未编造 Pass。

## 必修项

| ID | 位置 | 问题 | 状态 |
|---|---|---|---|
| — | — | 无 | N/A |

## A1–A6 逐项

| ID | 判断 | 证据 |
|---|---|---|
| A1 | Pass | `ValueNormalizer` 仅 Javadoc；方法体未改。CA-008「建议下一步」→ `Javadoc done; monitor only` |
| A2 | Pass | `Main.run(..., BooleanSupplier isTty)`；默认 `System.console() != null`。`p1_4_colorAutoUsesInjectedTty` + `resolveAnsiEnabled*`：auto+tty→ANSI；auto+非 tty→无；always/never 不受 tty。Reviewer 复跑 0 fail |
| A3 | Pass（真库 §6） | 可控：`nonEmptyPasswordNeverPrintedWhenPostgresConnectionFails`（合成 `--password`、不可达 URL、exit 2、无回显）；装配：CLI/`GGTEST_PASSWORD`。门控真库 `passwordIsNeverPrintedWhenRunningPostgres`：notes §6（连接失败、未探凭据、未标 Pass） |
| A4 | Pass（有门控全量 §6） | 无门控：`mvn -q clean test` → 224/0/0/18（根 `.env` 在场）；隔离：`envLookup→null` + `@TempDir`；产品 CWD `.env` 错配 exit 2。有门控全量：§6 |
| A5 | Pass | notes：`CorpusHardAcceptanceTest` 2/0/0；select1–5 exit 0、`failed=0`。Reviewer 抽测 select4+5：exit 0、`TOTAL: passed=2 failed=0` |
| A6 | Pass | 无 `ResultComparer` / CA-007 代码变更；登记册 CA-007 行未改 |

## 实现正确性

T1–T5 路径与 Plan 一致；产品合同未越界（CWD `.env`、默认 TTY、归一化行为）。T2 注入仅测可控、产品默认不变。T3 不可达 URL 证连接路径 + 不回显，真库缺口未靠缩减断言规避。未改 CA-007 / LCS。

## 测试有效性

T2 编排 ANSI + `resolveAnsiEnabled` 可因错误实现失败。T3 合成密码可复现，与 `assumePg` 门控测分离。Reviewer 复跑：`RuntimeConfigResolverTest` 23、`CliReportAcceptanceTest` 12、`PostgresCliIntegrationTest#nonEmptyPasswordNeverPrintedWhenPostgresConnectionFails` 1 → failures=0。全量 224/0/0/18 采信 notes；Skipped=18 = PG 门控 + Corpus 无 env + jar 清单。

## 文档影响核对

| Plan 声明 | 实现是否一致 | 备注 |
|---|---|---|
| 开发文档：Javadoc、CA-008、`dev-notes.md`、`plan.md` | 是 | notes 含验证表与 §6 |
| 用户文档：N/A | 是 | 未改 README/公开合同 |
| 运维文档：N/A | 是 | 无部署/排障变更 |

§6 字段完整：未验证项 → 原因 → 风险 → 恢复条件 → 复测范围。

## 安全影响核对

| 检查项 | 结果 | 备注 |
|---|---|---|
| 敏感信息 | Pass | 合成口令；无真实 `.env`/凭据入库或写入 notes |
| 认证与授权 | N/A | 无新认证模型 |
| 输入与外部访问 | Pass | 可控测 `127.0.0.1:1`；真库失败未探凭据 |
| 依赖变更 | N/A | 无依赖升级 |
| 密码回显 | Pass | stdout/stderr 无密码；`CliOptions.toString` 脱敏测仍在 |

发现项：无。处置：N/A。

## Git 合规

分支 `chore-audit-tails` 与工作项一致；未在 `main` 实施；实现未 commit；无真实 `.env`/凭据/强制入库 `.temp/select*.test`。`docs/manager/*` 属 Manager；本报告未提交。

## 发现项（按严重程度）

| 级别 | 位置 | 说明 | 处置 |
|---|---|---|---|
| 建议（非阻塞） | `Main.java` Javadoc | 七参重载注释写「six-arg overload」 | 可选措辞修正；不阻 QA |
| 残留风险（已 §6） | 真库非空密码 E2E；有门控全量 | 本机 JDBC 连接失败 | QA 可连 PG 时按 notes 复测 |

## 后续动作与复审范围

1. Manager 可调度 QA。
2. QA：无门控 `mvn -q clean test`、`package`、select1–5 / `GGTEST_CORPUS_DIR`、产品 `.env` 错配 exit 2；可连 PG 时补门控与非空密码不回显。
3. 无需 Developer 复审；若 QA Fail 修复后再审。

## 修订记录

| 日期 | 摘要 |
|---|---|
| 2026-07-26 | 初审 Approve；A1–A6；独立复跑 T2/T3 关键测与 select4–5 抽测 |
