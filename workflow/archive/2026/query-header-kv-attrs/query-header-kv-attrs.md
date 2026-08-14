# 工作项: query-header-kv-attrs

描述: 将 query header separator 语法从空格分隔 `separator <delim>` 改为 key-value 形式 `separator=<delim>`，为未来添加其他属性（如指定连接等）留好扩展
目标分支: main
源分支: query-header-kv-attrs
基线提交: a6c8719bc48099cf772a6bd1807876dd4577259c
文档影响: examples/ 下 .slt 文件需更新语法

> **本文件须保存为 `workflow/archive/2026/query-header-kv-attrs/query-header-kv-attrs.md`**，文件名与目录同名。
> 流程定义见 `workflow/WORKFLOW.md`；看板见 `workflow/STATUS.md`。
> 本工作项的全部产物平铺在 `workflow/archive/2026/query-header-kv-attrs/`，无子目录、无版本后缀。
> 表内只填枚举、短标签或路径；理由与长说明写进「进度笔记」。

## 门禁

| 路径等级 | Spec | Spec 用户确认 | Design | Review |
|---|---|---|---|---|
| standard | required | required | required | required |

## 状态

| 状态 | 下一步 | 阻塞原因 | 恢复条件 | 恢复后目标 |
|---|---|---|---|---|
| archived | — |  |  |  |

## 子项（仅 tracking 项填写）

| 子项 id | 状态 |
|---|---|
| — | |

## 进度笔记

- 当前语法：`query IIT nosort separator |`（separator 和 delim 是两个独立 token）
- 新语法：`query IIT nosort separator=|`（key=value 单 token）
- 扩展目标：未来可在同一 header 行追加 `connection=<name>` 等属性
- 错误信息中 `separator <delim>` 模板需同步更新
- 2026-08-14：按 ggnote `WORKFLOW.md` 标准迁移工作流目录（记录与产物合并为同一目录；权威文件改为 `workflow/WORKFLOW.md`）。
