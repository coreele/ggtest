# 工作项记录: fix-override-rowwise

工作项标识: fix-override-rowwise
描述: --override 对 row-wise（separator=<delim>）查询写入 flat value-per-line 格式而非 separator-joined rows；同时改善 separator 拼写错误提示
目标分支: main
文档影响: N/A

## 切片门禁

| sub-feature-id | 路径等级 | 源分支 | Spec | Spec 门禁 | Design 门禁 | Review 门禁 |
|---|---|---|---|---|---|---|
| fix-override-rowwise | fast | fix-override-rowwise | N/A | skipped | skipped | skipped |

## 切片状态

| sub-feature-id | 状态 | 后续步骤 |
|---|---|---|
| fix-override-rowwise | planned | Developer 实施（已完成）→ QA → done |

## 进度笔记

- 根因：`SqlLogicTestRunner.runQuery()` 中 override text 直接 `String.join("\n", actualView)` 拼接 flat values，未按 columnSeparator 重整为 row-wise 格式
- 修复：新增 `formatOverrideText()` 按 `typeSignature.size()` 分组，`columnSeparator` 拼接
- hash 保护：`actualView.size() == 1` 时跳过重整（单行 hash）
- misspelling：新增 `editDistance` ≤2 的模糊匹配，`seperator` → "did you mean separator?"
