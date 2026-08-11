# 工作项记录: query-header-kv-attrs

工作项标识: query-header-kv-attrs
描述: 将 query header separator 语法从空格分隔 `separator <delim>` 改为 key-value 形式 `separator=<delim>`，为未来添加其他属性（如指定连接等）留好扩展
目标分支: main
文档影响: examples/ 下 .slt 文件需更新语法

## 切片门禁

| sub-feature-id | 路径等级 | 源分支 | Spec | Spec 门禁 | Spec 用户确认 | Design 门禁 | Review 门禁 |
|---|---|---|---|---|---|---|---|
| query-header-kv-attrs | standard | query-header-kv-attrs | [spec.md](./../features/query-header-kv-attrs/spec.md) | required | required | required | required |

## 切片状态

| sub-feature-id | 状态 | 后续步骤 |
|---|---|---|
| query-header-kv-attrs | done | 待合入 main |

## 进度笔记

- 当前语法：`query IIT nosort separator |`（separator 和 delim 是两个独立 token）
- 新语法：`query IIT nosort separator=|`（key=value 单 token）
- 扩展目标：未来可在同一 header 行追加 `connection=<name>` 等属性
- 错误信息中 `separator <delim>` 模板需同步更新
