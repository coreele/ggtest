# QA Report: fix-sql-hash-comments

> 首测与所有回归写在本文件，按轮次追加；禁止 `qa-report-v2.md`。

## 轮次

| 轮次 | 日期 | 实现版本 | 环境 | 范围 | 结论 |
|---|---|---|---|---|---|
| 1 | 2026-09-30 | `ff97c29` | JDK 17 + Maven（本机） | 首测 | Pass |

## 执行命令

| 命令 | 输出摘要 / 证据位置 |
|---|---|
| `mvn -q test` | exit 0；427 用例 0 失败 0 错误（50 跳过为 live-DB 集成测试，与本次改动无关） |
| `mvn -q -Dtest=SqlLogicTestParserTest test` | exit 0；新增 `hashCommentLinesInsideSqlBodyAreStripped` 通过 |
| jshell `parse(/tmp/repro.slt)` | 修复后 `QueryRecord.sql()` = `SELECT 1`（`#` 注释行已剥除） |

## 覆盖（对照 Spec 验收与 Plan 验证）

> ID 沿用 Plan 的 `V-n`，与 Plan 的验收表逐条对应。

| ID | 条目 | 结果 | 证据 |
|---|---|---|---|
| V-1 | statement 体中间/开头的行首 `#` 注释行被剥离，不再进入 `StatementRecord.sql()` | Pass | 新用例断言 `CREATE TABLE t(a INT)`（`# comment at start...` 已剥除） |
| V-2 | query 体中间的行首 `#` 注释行被剥离，不再进入 `QueryRecord.sql()` | Pass | 新用例断言 `SELECT 1\nSELECT 2`（`# comment inside query body` 已剥除） |
| V-3 | 行首为 `#` 之外的行（含行内 `#`）行为不变 | Pass | 既有 `SqlLogicTestParserTest` 全部通过，无回归 |
| V-4 | `mvn -q test` 全量通过 | Pass | exit 0，427 用例 0 失败 |

## 回归

| 范围 | 结果 | 证据 |
|---|---|---|
| 全量单元/集成测试套件 | Pass | `mvn -q test` exit 0；无既有用例失败 |

## 文档与安全验收

| 项 | 结果 | 备注 |
|---|---|---|
| 用户可见文档 | N/A | Plan 声明 N/A；无用户可见表面变更 |
| 运维可执行文档 | N/A | 无 |
| 安全验证范围 | 通过 | 无敏感信息/认证/依赖变更；仅解析器行内判断，不引入输入执行面 |

## 缺陷

| ID | 严重度 | 摘要 | 状态 | 处理说明 | 验证证据 |
|---|---|---|---|---|---|
| — | | | | | |

## 阻塞（Blocked 时必填）

- 原因:
- 风险:
- 恢复条件:
- 复测范围:

## 结论

- 本轮结论: Pass
- 合并: 待用户授权
