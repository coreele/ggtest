# 工作项: sync-vs-slt-grammar

描述: vs-slt 插件语法高亮落后于 ggtest 实际支持的 SLT 语法（conn=/timeout=/separator= 属性、statement error 消息、skipif/onlyif 尾部注释、移除 `---- separator` 形式），更新 TextMate grammar
目标分支: main
源分支: sync-vs-slt-grammar
基线提交: a6c8719bc48099cf772a6bd1807876dd4577259c
文档影响: vs-slt/README.md（含过时的 `---- separator` 说明）

> **本文件须保存为 `workflow/archive/2026/sync-vs-slt-grammar/sync-vs-slt-grammar.md`**，文件名与目录同名。
> 流程定义见 `workflow/WORKFLOW.md`；看板见 `workflow/STATUS.md`。
> 本工作项的全部产物平铺在 `workflow/archive/2026/sync-vs-slt-grammar/`，无子目录、无版本后缀。
> 表内只填枚举、短标签或路径；理由与长说明写进「进度笔记」。

## 门禁

| 路径等级 | Spec | Spec 用户确认 | Design | Review |
|---|---|---|---|---|
| standard | skipped | not-required | skipped | required |

## 状态

| 状态 | 下一步 | 阻塞原因 | 恢复条件 | 恢复后目标 |
|---|---|---|---|---|
| archived | — |  |  |  |

## 子项（仅 tracking 项填写）

| 子项 id | 状态 |
|---|---|
| — | |

## 进度笔记

- 2026-08-11 登记。P1：更新 `vs-slt/syntaxes/sqllogictest.tmLanguage.json` 使其匹配当前 parser 语法，重点缺口：
  1. `statement` 头：`statement ok conn=c1`、`statement error <msg> conn=c2 timeout=2000`（消息 token 到首个 `key=` 为止，`#` 为字面量）
  2. `query` 头：`separator=<delim>` / `timeout=<ms>` / `conn=<name>` 任意顺序，可带 label
  3. `----` 期望头：仅接受精确 `----`，不再接受 `---- separator <delim>`（parser 已报错）
  4. `skipif` / `onlyif`：允许尾部 `#` 注释
  5. SQL body `#` 行：parser 当前按字面量处理（fix-sql-hash-comments 仍 backlog），grammar 不应把 `#` 行当作 sql-body 终止符之外的特殊处理
- 已知：vs-slt/README.md 第 29-30 行提到 `---- separator` 形式，需同步删除。
- 2026-08-11 Plan 已由用户确认（用户会话：ok）；plan.md 见 `workflow/archive/2026/sync-vs-slt-grammar/plan.md`。
- 2026-08-11 Developer 完成 T1–T7；V1–V5 通过（V3 输出 `all regex checks passed`），dev-notes.md 已产出。V6（L4 视觉检查）待 QA。
- 2026-08-11 Reviewer Approve（review.md：实现正确性、安全 N/A、无阻塞项；4 条非阻塞观察备查）。
- 2026-08-11 QA 轮次 2（仅 V6 回归）：L4 视觉检查通过，用户确认；最终结论 Pass，用户授权合并。
- 2026-08-14：按 ggnote `WORKFLOW.md` 标准迁移工作流目录（记录与产物合并为同一目录；权威文件改为 `workflow/WORKFLOW.md`）。
