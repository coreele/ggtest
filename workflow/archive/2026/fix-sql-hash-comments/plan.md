# Plan: fix-sql-hash-comments

## 元信息

- 依据 Spec: N/A（fast 路径，Spec skipped）
- 依据 Design: N/A（fast 路径，Design skipped）
- 依据 UI: N/A
- 路径等级: fast
- Review 门禁: skipped（fast 路径，单点修改、无接口变更、无跨模块影响）
- 最低验证层: unit
- 验证命令: `mvn -q -Dtest=SqlLogicTestParserTest test`（定向）+ `mvn -q test`（全量回归）
- 预期证据: 定向与全量均 exit 0；新增用例 `hashCommentLinesInsideSqlBodyAreStripped` 通过；无既有用例回归

## 目标摘要

对齐官方 sqllogictest 规格：SQL 语句/查询体中以 `#` 开头的注释行应在发往数据库前被剥离。当前仅顶层（record 之间）与 skipif/onlyif 头处理了 `#` 注释，`readSqlBody()` 与 query 体循环会将其原样拼入 SQL。

## 任务拆解

1. `SqlLogicTestParser.readSqlBody()`（statement 体）循环内，在空行判断后新增「行首为 `#` 则跳过」分支（完成条件：statement 体中的 `#` 注释行不再进入 SQL）
2. `parseQuery()` 的 query 体循环内，同样新增「行首为 `#` 则跳过」分支（完成条件：query 体中的 `#` 注释行不再进入 SQL）
3. `SqlLogicTestParserTest` 新增用例覆盖 query 体中间注释行与 statement 体开头注释行两种剥离场景（完成条件：断言 `QueryRecord.sql()` / `StatementRecord.sql()` 不含注释行）

## 依赖与顺序

三处均为同一文件内的独立小改，无顺序依赖；任务 3 依赖 1、2 完成后可验证。

## 触碰路径

- `src/main/java/com/ggtest/parser/SqlLogicTestParser.java`
- `src/test/java/com/ggtest/parser/SqlLogicTestParserTest.java`

## 验收与验证

| ID | 要求或命令 | 预期证据 | 结果（实施后填） |
|---|---|---|---|
| V-1 | statement 体中间/开头的行首 `#` 注释行被剥离，不再进入 `StatementRecord.sql()` | 新用例断言通过 | 通过 |
| V-2 | query 体中间的行首 `#` 注释行被剥离，不再进入 `QueryRecord.sql()` | 新用例断言通过 | 通过 |
| V-3 | 行首为 `#` 之外的行（含行内 `#`，如 `SELECT 1; # c`）行为不变 | 既有用例不回归 | 通过 |
| V-4 | `mvn -q test` 全量通过 | exit 0，0 失败 | 通过 |

## 验证缺口

| 项 | 原因 | 风险 | 恢复条件 |
|---|---|---|---|
| N/A | | | |

## 文档影响

| 类别 | 更新路径或 N/A 理由 |
|---|---|
| 开发文档 | N/A（无公开 API 变更，行为对齐官方规格） |
| 用户文档 | N/A（无新增 CLI/用户可见表面） |
| 运维文档 | N/A |

## 交接顺序

1. Developer 实施与自验 →
2. Reviewer（Review 门禁 skipped）→
3. QA 验收 →
4. 用户授权合并 → Manager 第三阶段文档提交（`done`）→ 合入 → 结束

## 修订记录

| 日期 | 摘要 |
|---|---|
| 2026-09-30 | 初稿（fast 路径，Planner） |
