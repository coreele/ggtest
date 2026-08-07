# 代码审计登记册（Known Issue / Tech Debt / 已接受风险）

持久记录代码审计关注项，供后续审计核对。由人工或审计后整理维护；`code-audit` skill 在文件存在时**必须**读取并与当前代码对照。

**状态枚举：** `open` | `accepted` | `resolved`（审计发现已不存在时建议改为 `resolved`）

## 条目模板

复制下表增行；无条目时保留表头即可。

| ID | 类型 | 状态 | 位置 | 简述 | 影响 / 接受理由 | 建议下一步 | 更新日期 |
|---|---|---|---|---|---|---|---|
| CA-001 | Tech Debt | open | `src/...` | 示例：说明债务 | 阻塞 X 扩展 | 抽取接口 | YYYY-MM-DD |

类型取值：`Known Issue` | `Tech Debt` | `优化` | `Accepted Risk`。

## 登记条目

| ID | 类型 | 状态 | 位置 | 简述 | 影响 / 接受理由 | 建议下一步 | 更新日期 |
|---|---|---|---|---|---|---|---|
| CA-001 | Tech Debt | resolved | `AbstractJdbcExecutor` + sqlite/postgres 子类 | 两引擎执行器同构重复已抽取 | 已合入 `main`（`fix-jdbc-executor-dedup`） | none | 2026-07-26 |
| CA-002 | Known Issue | resolved | `CredentialRedaction` / 诊断输出控制点 | 诊断输出脱敏已落地 | 已合入 `main`（`fix-cli-credential-redaction`；refactor 保留） | none | 2026-07-26 |
| CA-003 | Tech Debt | resolved | `CliSession` → `FileRunner` / `ReportWriter` | 会话过大已拆边界 | 已合入 `main`（`refactor-cli-session-boundaries`） | none | 2026-07-26 |
| CA-004 | Tech Debt | resolved | `ResultComparer.DEFAULT_HASH_THRESHOLD` | hash 阈值单一权威 | 已合入 `main`（`fix-shared-defaults`） | none | 2026-07-26 |
| CA-005 | Tech Debt | resolved | `SqlLogicDefaults` / `ResultComparer` 转发 | column separator 单一权威 | 已合入 `main`（`fix-shared-defaults`） | none | 2026-07-26 |
| CA-006 | Tech Debt | resolved | `FileRunner` / 原 `CliSession.runPostgresFile` | PG teardown 仅 finally 一次 | 已合入 `main`（`fix-pg-teardown-once`；refactor 保留） | none | 2026-07-26 |
| CA-007 | 优化 | accepted | `ResultComparer` LCS | 失败 diff `O(n*m)` | 超大失败集可能陡增；通过路径不受影响；本批不做大改 | backlog / 后续按需 | 2026-07-26 |
| CA-008 | Accepted Risk | accepted | `ValueNormalizer` | 非法 I/R 归一为 0 / 0.000（对齐 sqllogictest） | 非缺陷；Javadoc 已标明语义 | Javadoc done; monitor only | 2026-07-26 |
