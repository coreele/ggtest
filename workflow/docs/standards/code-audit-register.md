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
| CA-009 | Known Issue | open | `CredentialRedaction.redactUrlUserInfo` / `FileRunner.sanitize` | URL 脱敏仅覆盖 `://user:pass@host` 格式；查询参数凭据（`?password=secret`）或驱动异常回显中非 URL 形式凭据无法脱敏 | 若用户仅通过 URL 嵌凭据而不单独配置 `--password`/`GGTEST_PASSWORD`，错误输出可能泄露凭据 | 增加通用敏感参数名模式匹配（`password=`, `pwd=`），或文档明确告知凭据须单独提供 | 2026-08-11 |
| CA-010 | Tech Debt | resolved | `FileRunner` | FileRunner 已拆分为 ConnectionFactory + OverrideCoordinator，179 行（原 210） | 已合入 `main`（`refactor-filerunner-responsibilities` + multi-connection rewrite） | none | 2026-08-11 |
| CA-011 | 优化 | open | `RuntimeConfigResolver.resolveHashThreshold` | hash-threshold 未验证非负范围；`<= 0` 语义为「禁用 hash」但负值不明确 | 配置错误时行为不直观 | 在解析处增加 `if (value < 0) throw UsageException` 或文档标明负值等价禁用 | 2026-08-11 |
| CA-012 | Tech Debt | open | `ExpectedResultExpander.splitLiteral` vs `OverrideWriter.splitOnEol` | normalize 和 cli 包中的字符串分割工具功能同族但各自维护 | 少量重复（~10 行），维护成本低 | 低优先级：可抽取到共享工具类 | 2026-08-11 |
| CA-013 | Tech Debt | open | `DotEnvLoader.WHITELIST` | 白名单硬编码 `Set.of(...)`；添加新配置键需修改源码 | 当前 5 个键可接受；新增配置需重新编译 | 按需改为外部配置或注解驱动 | 2026-08-11 |
| CA-014 | Tech Debt | open | `SqlLogicTestParser.LineBuffer.splitPreserveAllLines` | 解析前将整个文件内容拷贝到 `ArrayList<String>` | 大文件（>100MB）可能 OOM；sqllogictest 文件通常很小 | 仅在文件极大时关注；可改为流式读取 | 2026-08-11 |
| CA-015 | Tech Debt | resolved | `cli/EngineAdapter.java` / `cli/SqliteAdapter.java` / `cli/PostgresAdapter.java` | 死代码已删除 | 已合入 `main`（`fix-ca015-dead-adapters`） | none | 2026-08-11 |
| CA-016 | Known Issue | resolved | `parser/SqlLogicTestParser.java:163-184` | statement error 消息提取改用 token 索引，conn= 不再污染 expectedErrorMsg | 已合入 `main`（`fix-ca016-stmt-error-conn`） | none | 2026-08-11 |
