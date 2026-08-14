# 代码审计登记册

持久记录代码审计的关注项，供后续审计核对。`code-audit` skill 在本文件存在时**必须**读取并与当前代码对照。这是数据，不是条款标准——条款见 [../agents/standards/code-audit.md](../agents/standards/code-audit.md)。

**类型：** `Known Issue` | `Tech Debt` | `优化` | `Accepted Risk`
**状态：** `open` | `accepted` | `resolved`（审计发现问题已不存在时改为 `resolved`）

行格式：

```text
| CA-001 | Tech Debt | open | path/to/file | 一句话简述 | 影响或接受理由 | 建议动作 | YYYY-MM-DD |
```

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
| CA-017 | Known Issue | resolved | `cli/OverrideWriter.java` | `writeAtomically` 原子移动回退死代码（catch 错误类型）改 `AtomicMoveNotSupportedException` + `FileMover` 测试缝 | 已合入 `main`（`fix-ca017-override-atomic-move`） | none | 2026-08-13 |
| CA-018 | Known Issue | resolved | `db/postgres/PostgresSchemaIsolation.java` / `db/mysql/MySqlSchemaIsolation.java` | `setSearchPath` 补标识符校验；抽取 `com.ggtest.db.SchemaNames`（generate/isSafe/requireSafe）消除 PG/MySQL 重复 | 已合入 `main`（`fix-ca018-search-path-validation`） | none | 2026-08-13 |
| CA-019 | Known Issue | resolved | `cli/CliArgumentParser.java` | `requireValue` 改为仅在下一 token 是已知 flag 时报缺值，允许 `-` 开头的值（如 `--password -secret`） | 已合入 `main`（`fix-ca019-cli-dash-values`） | none | 2026-08-13 |
| CA-020 | Known Issue | resolved | `cli/Main.java` | 顶层补 `catch (Throwable)` → exit 2 + 脱敏摘要（`printFatalError`） | 已合入 `main`（`fix-ca020-main-fatal-catch`） | none | 2026-08-13 |
| CA-021 | Tech Debt | open | `db/AbstractJdbcExecutor.java:134-138` | `summarize` Javadoc 声称「无凭据」但本层未脱敏（误导） | 层内文档问题；实际脱敏在 CLI 输出层 | 弱化 Javadoc 或本层净化 | 2026-08-13 |
| CA-022 | Tech Debt | open | `model/QueryRecord.java:52-60` / `normalize/ResultComparer.java:86-94` | `columnSeparator` 校验两处逐字符重复 | 少量重复 | 抽取共享校验静态方法 | 2026-08-13 |
| CA-023 | Known Issue | resolved | `cli/FileRunner.java:67-91`（PG/MySQL/Xugu 同款） | 引擎隔离 `prepare` 失败时默认连接泄漏：prepare 失败 catch 内 return 前显式 `first.close()` | 已合入 `main`（`fix-ca023-prepare-conn-leak`；原 2026-08-12 xugu 审计记为 CA-017，因与源码审计 CA-017 撞号改 CA-023） | none | 2026-08-13 |
