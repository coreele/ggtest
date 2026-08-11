# 工作项记录: add-statement-query-timeout

工作项标识: add-statement-query-timeout
描述: statement 和 query 支持 `timeout=<ms>` 属性，设置最长执行时间（毫秒），超时判定为失败
目标分支: main
文档影响: demo.slt / demo_zh.slt 添加 timeout 示例

## 切片门禁

| sub-feature-id | 路径等级 | 源分支 | Spec | Spec 门禁 | Design 门禁 | Review 门禁 |
|---|---|---|---|---|---|---|
| add-statement-query-timeout | standard | add-statement-query-timeout | [spec.md](./../features/add-statement-query-timeout/spec.md) | required | required | required |

## 切片状态

| sub-feature-id | 状态 | 后续步骤 |
|---|---|---|
| add-statement-query-timeout | reviewing | Reviewer → QA |
