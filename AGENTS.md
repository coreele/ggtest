# GGTEST 工作流

本项目的工作流定义在 workflow/WORKFLOW.md 中。
你必须严格按照其中定义的步骤执行，不得跳步。

## 强制执行规则
- 每次开始任务前，先复述 workflow 中的当前步骤
- ...

---

# 项目架构与基础知识

以下内容供 Agent 快速索引，理解项目结构后再动手。

## 分层与包职责

```
com.ggtest.model      纯数据类型（sealed SqlTestRecord + record 变体）；无 I/O、无第三方依赖
    ← com.ggtest.parser     sqllogictest 文本 → SqlTestRecord 序列（单遍状态机）
    ← com.ggtest.normalize  I/T/R 归一化、排序、MD5 hash、类型推断、结果比对
    ← com.ggtest.db         DatabaseExecutor 抽象 + AbstractJdbcExecutor + 引擎子包
    ← com.ggtest.runner     SqlLogicTestRunner：串行执行 records、比对、halt/label/skipif
    ← com.ggtest.cli        CLI 入口（Main）、参数解析、运行编排、--override 写回
```

依赖方向严格单向：model ← parser/normalize/db ← runner ← cli。引擎子包
（`db.sqlite` / `db.postgres` / `db.mysql` / `db.xugu`）仅依赖 `com.ggtest.db`
抽象，不互相依赖。架构守护测试 `RunnerDependencyIsolationTest` 确保分层不被破坏。

## 核心数据流

```
CLI args → CliArgumentParser → ParsedArguments
         → RuntimeConfigResolver（merge env/.env）→ CliOptions
         → TestFileCollector.collect(inputs) → List<Path>
         → CliSession.execute(files)
             → per file: SqlLogicTestParser.parse → List<SqlTestRecord>
                         → SqlLogicTestRunner.run(records, executorFactory)
                             → executor.executeStatement/executeQuery
                             → ResultComparer.compare(typeSig, sortMode, hash, expected, actual)
                         → FileRunResult
             → ReportWriter → stdout report
             → exit code 0/1/2
```

## 关键设计决策

### 引擎扩展
实现 `DatabaseExecutor` 接口（或继承 `AbstractJdbcExecutor`），然后在两处接线：
- `RuntimeConfigResolver`：engine allow-list + URL 前缀校验
- `FileRunner`：executor 实例化 + schema 隔离（如有）

### Schema 隔离
PG/MySQL/Xugu 使用每文件唯一 schema（`SchemaNames.generate()`），prepare→run→teardown。
`SchemaNames` 在 `com.ggtest.db`（base 包，JDBC-free），抛 `IllegalArgumentException`（非 `SQLException`）。

### --override golden-update
- `--override`：所有有期望块的 query 一律以实际结果覆盖（含 PASS），并自动推断类型签名。
- 执行失败的 query/statement ok → 改写为 `statement error <msg>`。
- `--separator <delim>`：仅多列 query 注入 `separator=` + 行式回写；覆盖 header 已有分隔符。
- `TypeSignatureInferer`（normalize，JDBC-free）：值驱动推断 I/R/T；全 NULL/空 → T。
- `OverrideWriter`（cli）：行级区间替换，原子写（temp+rename），按 startLine 倒序应用。

### 类型归一化（sqllogictest 对齐）
- `I`（integer）：`Long.parseLong`，非法 → `"0"`（CA-008 accepted）
- `R`（real）：`String.format("%.3f")`，非法 → `"0.000"`
- `T`（text）：非可见 ASCII → `@`；空串 → `(empty)`；NULL → `NULL`
- `ValueNormalizer.normalize(ColumnType, raw)` 是唯一入口

### 结果比对
- `ResultComparer.compare(typeSig, sortMode, hashThreshold, separator, expectedText, actualRows)`
- hash 阈值（默认 8）：值数 > 阈值 → MD5 hash 比对（`ResultHasher`）
- value-per-line（默认）vs row-wise（`separator=<delim>`）
- 行宽 != 签名长度 → `IllegalArgumentException`（被 runner 捕获用于 --override 签名推断）

### XuguDB 驱动（专有）
`com.xugudb:xugu-jdbc` 不在 Maven Central，存于 gitignore 的 `driver/` 目录（m2 布局）。
`pom.xml` 的 `xugu` profile 按驱动 jar 存在与否自动激活——CI 无驱动时跳过，不影响构建。

### 凭据脱敏
- `CredentialRedaction.redactMessage(msg, password)`：URL userinfo → `***@`；替换 password 明文
- `CliOptions.toString()` 密码脱敏
- `AbstractJdbcExecutor.summarize(SQLException)` 返回 `.getMessage().strip()`（业务错误，不含连接串）

## 实用脚本（bin/）

| 脚本 | 作用 |
|---|---|
| `ggtest` | 启动器（查找 `target/ggtest-*.jar` 或 `$GGTEST_JAR`） |
| `sltmd` | .slt → .md（代码块 ↔ 注释转换） |
| `mdslt` | .md → .slt（逆转换） |
| `sltsql` | .slt → 纯 .sql（提取语句，丢弃指令/期望） |
| `sqlslt` | .sql → .slt（statement ok / query T 占位；`--separator <delim>`） |

## 工作流文档索引

- 权威流程：`workflow/WORKFLOW.md`（状态机、门禁、角色）
- STATUS：`workflow/STATUS.md`（当前工作树看板）
- Git 规范：`workflow/agents/standards/git.md`（分支、提交、rebase + FF）
- 审计登记册：`workflow/audit/register.md`
- 审计报告：`workflow/audit/`（工作流外）
