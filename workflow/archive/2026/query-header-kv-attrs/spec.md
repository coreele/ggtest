# Spec: query-header-kv-attrs

## 背景

当前 query header 中 `separator <delim>` 是两个独立 token，不能自然扩展为其他属性（如未来的 `connection=<name>`）。改为 key=value 形式统一处理 query header 的属性声明。

## P0 — 行为合同（必须满足）

### P0-1: query header 属性解析

query header 剩余 token（type-signature 和 sortmode 之后）解析规则：

| 剩余 token | 解析 |
|---|---|
| 含 `=` 的 token | 解析为 `key=value` 属性对。当前仅支持 `separator` key |
| 不含 `=` 的 token | 解析为 query label（最多一个） |

- 同一个 header 中 label 只能出现一次；多个不含 `=` 的 token 视为错误
- 同一个 key 只能出现一次；重复 key 视为解析错误
- `key` 区分大小写；unknown key 视为解析错误
- `value` 可为空：`separator=` 表示空分隔符（等价于 value-per-line 模式）

### P0-2: separator 属性语义

- `separator=<delim>` — 声明行式分隔符（与旧 `separator <delim>` 等效）
- `separator=` — 空分隔符，等同 value-per-line 模式（`columnSeparator` 为 `Optional.empty()`）
- 不含 `separator` 属性 — 默认 value-per-line 模式

`<delim>` 约束不变：
- 不能含空白字符
- 不能为 null
- `columnSeparator` 字段仍为 `Optional<String>`

### P0-3: 旧语法拒绝

以下旧 query header 格式须产生可读的解析错误（而非静默误解析）：

- `separator <delim>`（两个独立 token）— 错误信息提示使用 `separator=<delim>`
- `lbl separator <delim>` — 同上

### P0-4: 已删除的 `---- separator` 形式

`---- separator ...` 的错误信息也更新，提示应为 query-header 属性形式。

### P0-5: 解析错误信息模板

所有引用旧语法的错误信息更新为 `separator=<delim>` 形式。

## P1 — 边界与异常

### P1-1: 未知属性 key

`query IIT nosort foo=bar` → 解析错误，明确列出支持的 key

### P1-2: 重复 key

`query IIT nosort separator=| separator=,` → 解析错误

### P1-3: 多个非等号 token

`query IIT nosort lbl1 lbl2` → 解析错误（label 只能一个）

### P1-4: separator value 含空白

`query IIT nosort separator=a b` → 解析错误（与旧行为一致）

### P1-5: 无 type signature 的 query

行为不变：`query` 缺 type signature → 解析错误

## P2 — 文档与示例影响

- `examples/demo.slt`、`examples/demo_zh.slt`、`examples/demo_pl.slt` 中 `separator <delim>` 更新为 `separator=<delim>`
- 测试 fixture 文件同更新
- Javadoc 中引用旧语法的注释更新
