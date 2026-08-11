# 工作项记录: add-conn-attribute

工作项标识: add-conn-attribute
描述: statement 和 query 支持 `conn=<name>` 属性，指定命名连接，实现多连接并发测试（如事务锁竞争）
目标分支: main
文档影响: examples/demo2.slt 为并发测试示例

## 切片门禁

| sub-feature-id | 路径等级 | 源分支 | Spec | Spec 门禁 | Design 门禁 | Review 门禁 |
|---|---|---|---|---|---|---|
| add-conn-attribute | standard | add-conn-attribute | [spec.md](./../features/add-conn-attribute/spec.md) | required | required | required |

## 切片状态

| sub-feature-id | 状态 |
|---|---|
| add-conn-attribute | speccing |
