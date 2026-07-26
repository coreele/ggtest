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
| CA-001 | Tech Debt | open | `SqliteJdbcExecutor` / `PostgresJdbcExecutor` | 两引擎执行器近乎同构重复 | 致命判定/行读取易漂移 | 工作项 `fix-jdbc-executor-dedup` | 2026-07-26 |
| CA-002 | Known Issue | open | `CliSession.sanitize` / `CliOptions.toString` | sanitize 仅 strip；url 可能含 userinfo | 诊断输出可能泄露凭据片段 | 工作项 `fix-cli-credential-redaction` | 2026-07-26 |
| CA-003 | Tech Debt | open | `CliSession.java` | 会话过大（编排/连接/PG/报告/脱敏） | 改报告或换引擎牵动面大 | 工作项 `refactor-cli-session-boundaries`（等依赖） | 2026-07-26 |
| CA-004 | Tech Debt | resolved | `ResultComparer` | `DEFAULT_HASH_THRESHOLD=8` 双定义 | 已收敛；唯一字面量在 `ResultComparer`；`RuntimeConfigResolver` 引用 | — | 2026-07-26 |
| CA-005 | Tech Debt | resolved | `SqlLogicDefaults` / `ResultComparer` | `DEFAULT_COLUMN_SEPARATOR` 双定义 | 已收敛；唯一字面量在 `model.SqlLogicDefaults`；parser 与 `ResultComparer` 转发引用 | — | 2026-07-26 |
| CA-006 | Tech Debt | open | `CliSession` PG teardown | try hardFailure 后 finally 可能二次 DROP | 错误日志可能重复 | 工作项 `fix-pg-teardown-once` | 2026-07-26 |
| CA-007 | 优化 | accepted | `ResultComparer` LCS | 失败 diff `O(n*m)` | 超大失败集可能陡增；通过路径不受影响；本批不做大改 | backlog / 后续按需 | 2026-07-26 |
| CA-008 | Accepted Risk | accepted | `ValueNormalizer` | 非法 I/R 归一为 0 / 0.000（对齐 sqllogictest） | 非缺陷；可补 Javadoc | 可选文档改进，本批不强制 | 2026-07-26 |
