# 工作项记录: sync-vs-slt-grammar

工作项标识: sync-vs-slt-grammar
描述: vs-slt 插件语法高亮落后于 ggtest 实际支持的 SLT 语法（conn=/timeout=/separator= 属性、statement error 消息、skipif/onlyif 尾部注释、移除 `---- separator` 形式），更新 TextMate grammar
目标分支: main
文档影响: vs-slt/README.md（含过时的 `---- separator` 说明）

> 权威流程见 [workflow/README.md](../../README.md)；活跃状态见 [STATUS.md](STATUS.md)。

## 切片门禁（未拆分）

| sub-feature-id | 路径等级 | 源分支 | Spec | Spec 门禁 | Spec 用户确认 | Design 门禁 | Review 门禁 |
|---|---|---|---|---|---|---|---|
| sync-vs-slt-grammar | standard | sync-vs-slt-grammar | 无 | skipped | not-required | skipped | required |

> Spec skipped 理由：高亮契约已由 `SqlLogicTestParser.java` 与 README 定义，无新增行为合同；本次仅同步现有语法到 grammar。Review required：涉及多语法构造，需要独立审阅正则与 parser 对齐。

## 切片状态

| sub-feature-id | 状态 | 后续步骤 |
|---|---|---|
| sync-vs-slt-grammar | planned | Developer |

## 进度笔记

- 2026-08-11 登记。P1：更新 `vs-slt/syntaxes/sqllogictest.tmLanguage.json` 使其匹配当前 parser 语法，重点缺口：
  1. `statement` 头：`statement ok conn=c1`、`statement error <msg> conn=c2 timeout=2000`（消息 token 到首个 `key=` 为止，`#` 为字面量）
  2. `query` 头：`separator=<delim>` / `timeout=<ms>` / `conn=<name>` 任意顺序，可带 label
  3. `----` 期望头：仅接受精确 `----`，不再接受 `---- separator <delim>`（parser 已报错）
  4. `skipif` / `onlyif`：允许尾部 `#` 注释
  5. SQL body `#` 行：parser 当前按字面量处理（fix-sql-hash-comments 仍 backlog），grammar 不应把 `#` 行当作 sql-body 终止符之外的特殊处理
- 已知：vs-slt/README.md 第 29-30 行提到 `---- separator` 形式，需同步删除。
- 2026-08-11 Plan 已由用户确认（用户会话：ok）；plan.md 见 `workflow/docs/features/sync-vs-slt-grammar/plan.md`。

