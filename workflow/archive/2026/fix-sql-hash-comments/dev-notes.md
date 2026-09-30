# Dev Notes: fix-sql-hash-comments

## 实现摘要

在 `SqlLogicTestParser` 两处 SQL 体收集循环（`parseQuery()` 的 query 体循环与 `readSqlBody()`）中，于空行判断之后新增「行首为 `#` 则 `lines.next()` 并 `continue`」分支，把 SQL 体内的 `#` 注释行在拼入 SQL 前剥掉。只判行首 `#`（列 0），不处理行内 `#`，对齐官方 C 实现 `nextLine()` 的 `zLine[0]!='#'` 与 sqllogictest-rs 的 `strip_prefix('#')`。

## 变更路径

- `src/main/java/com/ggtest/parser/SqlLogicTestParser.java`
- `src/test/java/com/ggtest/parser/SqlLogicTestParserTest.java`

## 测试先行记录（TDD）

| Spec ID / 行为项 | 测试 | 先失败 | 后通过 | 说明 |
|---|---|---|---|---|
| V-1 statement 体注释行剥离 | `hashCommentLinesInsideSqlBodyAreStripped` | ✅ jshell 复现（修复前 SQL 体含 `# comment inside SQL body`） | ✅ | 单测与实现同会话完成，未严格先写测试；红→绿证据取修复前/后的 jshell 复现输出 |
| V-2 query 体注释行剥离 | `hashCommentLinesInsideSqlBodyAreStripped` | ✅ 同上 | ✅ | 同上 |
| V-3 行内 `#` 与普通行不变 | 既有 `SqlLogicTestParserTest` 用例回归 | N/A（行为未变） | ✅ | `mvn test` 全量无回归 |

> 说明：本工作项此前以 fast-track 方式先实现了代码（用户随后要求补走标准流程）。故单测非严格「先失败」，红绿证据以修复前后的 jshell 复现（`/tmp/repro.slt`：修复前 SQL 体含注释行，修复后只剩 `SELECT 1`）替代，风险低——行为差异由官方规范与参考实现双重锚定。

## 验证

| 命令 | 验证层 | 结果摘要 / 证据 |
|---|---|---|
| `mvn -q -Dtest=SqlLogicTestParserTest test` | unit | exit 0，新增用例通过 |
| `mvn -q test` | unit（全量回归） | exit 0，426 用例 0 失败 0 错误（50 跳过为 live-DB 集成测试） |
| jshell `parse(/tmp/repro.slt)` | manual | 修复后 `QueryRecord.sql()` = `SELECT 1`（注释行已剥除） |

## 目标分支同步（最终 Review 前）

- 目标分支及提交: `main` @ `67647eaa46803b54e764f356f2e7ba18d3ca8d8f`
- 同步后源分支 HEAD: `fix-sql-hash-comments`（自 `67647ea` 创建，未落后）
- 同步方式: N/A（分支直接基于最新目标，无需 rebase）
- 冲突及处理: N/A
- 同步后复验: `mvn -q test` exit 0

## 文档影响

| 类别 | 已更新路径或交接说明 |
|---|---|
| 开发文档 | N/A |
| 用户文档 | N/A |
| 运维文档 | N/A |

## 未解决风险 / 验证缺口

| 项 | 原因 | 风险 | 恢复条件 |
|---|---|---|---|
| N/A | | | |

## QA 修复回执

| 缺陷 ID | 处理 | 摘要 | 验证证据 | 建议复测范围 |
|---|---|---|---|---|
| | | | | |
