# 代码质量审计报告 — xugu-engine

- 日期：2026-08-12
- 执行：手动对照 `workflow/agents/standards/code-audit.md` 审查实现 `e3a8847`（分支 `xgtest`）
- 基准上一次审计：2026-08-11（`workflow/audit/2026-08-11-src.md`）
- 专项标准：无新增 `code-audit-<topic>.md`
- 登记册：`workflow/audit/register.md`（CA-001~CA-016；无 xugu 相关条目）

## 范围

- 代码：本次变更的所有生产源文件——新建 `db/xugu/{XuguJdbcExecutor,XuguSchemaIsolation}.java`，修改 `cli/{FileRunner,RuntimeConfigResolver,Main}.java`
- 补充：`pom.xml`、`.gitignore`（§7 佐证）；`driver/` gitignore 合规确认
- 测试不纳入本次深度审计范围（仅 §6 抽查抽象守卫更新与测试封装合规性）

## 摘要

- Findings — Critical: 0 | High: 0 | Medium: 0 | Low: 3 | Info: 1
- 关注项 — Known Issue: 1（pre-existing PG/Xugu 资源泄漏）| Tech Debt: 1 | 优化: 1
- 整体质量良好。两个新建类（executor + 隔离）结构与 PG 镜像一致、职责单一、命名清晰。改动处均以镜像 PG 的最小增量方式扩展。无硬编码凭据。测试门变量 `GGTEST_XG_*` 未泄漏到运行时白名单。

## 上次审计跟踪

本次非全量审计，不覆盖全部登记册。与 xugu 引擎代码**无关**的历史 Findings（CA-009~CA-014）维持不变，不在此报告逐条复述。

## Findings（违规）

| 级别 | 标准 | 位置 | 问题 | 建议 | 跟踪 |
|---|---|---|---|---|---|
| Low | §5 | `FileRunner.java:67-91` | 隔离引擎（PG/Xugu）`prepare` 失败时 **连接泄漏**：`connections.put("", first)` 已入 map 后 `prepare` 抛 → 方法 `return hardFailure` 不经 `finally`（`finally` 仅包裹 `runner.run()` 段），连接永不关闭。`openConnection` 失败路径无泄漏（map 未 put）。 | 将 `needsIsolation` 块的 `try` 内嵌到最终 `try-finally` 内，或将早期 return 路径中的连接显式 close。**PR 范围**：同时修复 PG 和 Xugu；或单独登记 1 个 Known Issue 覆盖两引擎。 | CA-017 |
| Low | §2 | `XuguSchemaIsolation.java:66-71` | `setSearchPath(Connection, String schema)` 参数 `schema` 直接拼接 SQL（`"SET SCHEMA " + schema`）未经 `isSafeIdentifier` 校验，与 `teardown` 的防御不一致（`teardown` 在 `DROP SCHEMA` 前调 `isSafeIdentifier`）。`PostgresSchemaIsolation.setSearchPath` 同款。实际调用方 (`FileRunner` 工厂) 传的是 `prepare` 生成的 UUID 名，安全。 | 要么在 `setSearchPath` 内部也加 `isSafeIdentifier` 校验以对称，要么标注前置条件 `@param schema must be a safe SQL identifier`。两引擎同改。 | Low，非阻塞 |
| Low | §4 | `FileRunner.java:72-74` | `isPostgres ? PostgresSchemaIsolation.prepare(first) : XuguSchemaIsolation.prepare(first)`——三元运算符把 `!isPostgres` 隐含==Xugu，若将来加第 4 隔离引擎则静默选错。工厂部分（94-116 行）已用显式 `if (isPostgres)` / `if (isXugu)` 分支，更清晰。 | 把 prepare 处的三元改为与工厂一致的显式 `if/else`。 | Low，非阻塞 |
| Info | §3 | `RuntimeConfigResolver.java`（`validateEngineUrlPair` xugu 分支） | sqlite 与 postgres 分支在 return 后有显式 `return;`，xugu 分支为末尾自然落下——功能一致但风格不统一。 | 补统一 `return;` 或接受现状（与原始 PG 模式一致：原 postgres 分支也末尾落下无 return）。 | Info |

## 关注项（Known Issue / Tech Debt / 优化）

| 类型 | 级别 | 位置 | 描述 | 影响或收益 | 建议下一步 | 登记册 |
|---|---|---|---|---|---|---|
| Known Issue | Low | `FileRunner.java:67-91`（PG 同款） | PG/Xugu `prepare` 失败时默认连接未关闭（连接泄漏）。见 Findings Low §5。 | `prepare` 极少失败（CREATE SCHEMA + SET SCHEMA 在正常 DB 上几乎从不出错）；JVM 退出时连接终被 OS 回收。 | 登记为 CA-017；建议后续专门工作项一次性修复 PG+Xugu 泄漏 | CA-017 |
| Tech Debt | Low | `RuntimeConfigResolver.java`（`normalizeEngine` + `ENGINE_XUGU_ALIAS`） | 引擎别名处理是 one-off 硬编码：单个常量 + 单个 `if`。若未来加更多引擎/别名，需不断追加常量与分支。 | 当前仅三个引擎、一个别名，可接受。 | 与引擎选择 if/else 整体一并重构（spec 已声明非本切片目标） | — |
| 优化 | Info | `FileRunner.java:92` | `String fileSchema = fileSchemaHolder[0];` 使用 `String[]` 单元素数组持有可变引用来穿透 lambda——惯用但间接。 | 代码简洁，无功能影响。 | 无需立即处理；若未来需要更清晰的状态管理可重构为 `AtomicReference` 或持有类 | — |

## 登记册核对

| ID | 原状态 | 审计结论 | 建议 |
|---|---|---|---|
| CA-001~CA-008 | resolved/accepted | 不变 | — |
| CA-009~CA-014 | 参见 2026-08-11 审计 | 不变（均非 xugu 相关） | — |
| CA-015~CA-016 | resolved | 已确认 resolved | — |
| 新增 CA-017 | — | Low §5 PG/Xugu prepare 连接泄漏 | 见 Findings 表 |

### 登记册建议更新

在 `code-audit-register.md` 末尾追加：

| ID | 类型 | 状态 | 位置 | 简述 | 影响 / 接受理由 | 建议下一步 | 更新日期 |
|---|---|---|---|---|---|---|---|
| CA-017 | Known Issue | open | `FileRunner.java:67-91`（PG+Xugu） | 引擎隔离 `prepare` 失败时默认连接不关闭 | `prepare` 极少失败；每次泄漏一连接，JVM 退出回收 | 一次性修复 PG+Xugu：把 `needsIsolation` 块纳入 `try-finally` | 2026-08-12 |

## 覆盖说明

- 已读：`workflow/agents/standards/code-audit.md`；`workflow/audit/register.md`
- 分层（§1）：`db/xugu` 执行器与隔离、`cli` 引擎接线分层正确；`db/xugu` 不在 `parser/runner/normalize` 依赖路径上。
- 正确性/资源（§2/§5）：新建两类的 Statement 全走 try-with-resources；`requireNonNull` 守卫 null；`isSafeIdentifier` 防注入（teardown 时）；已知缺陷：prepare 失败连接泄漏（CA-017，pre-existing PG 模式）。
- 并发（§2）：`XuguSchemaIsolation` 纯静态无状态方法；`XuguJdbcExecutor` 继承基类连接约定。单文件内 factory `connections` map 串行访问安全；`--parallel` 下每文件独立 `FileRunner` 实例无竞态。
- 敏感信息（§7）：源码无硬编码凭据；测试用 env var (`GGTEST_XG_*`)；`driver/` gitignore；`pom.xml` 无泄漏。
- 测试友好（§6）：新建测试只访问 public API（executor/isolation 的公开方法）；未降低封装。
- 重复/复杂度（§4）：Xugu 类与 PG 类同构——这是**故意的架构镜像**（设计决策 2/3/4 明确声明的策略），非有害重复。命名对称统一。
- 命名/可读性（§3）：类名表达意图（`XuguJdbcExecutor`、`XuguSchemaIsolation`）；方法与 PG 同名同型；注释仅说明「为什么」（中文致命标记来源、DROP SCHEMA 无 IF EXISTS 原因、无需 pg_catalog）。
- 范围内无 `TODO` / `FIXME` / `HACK` / `XXX` / `TECHDEBT` 标记。
- 未覆盖：未加载专项 `code-audit-<topic>.md`；未做圈复杂度逐文件计量；未审查 `MainOrchestrationTest` / 与 xugu 无关的测试类。
