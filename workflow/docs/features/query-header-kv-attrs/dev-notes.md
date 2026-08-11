# Dev Notes: query-header-kv-attrs

## 实施摘要

将 query header 中 `separator <delim>` 改为 `separator=<delim>` key=value 形式。

### 核心改动

**`parser/SqlLogicTestParser.parseQuery()`** — 属性解析重写：
- 旧：基于 `remaining` token 数的 if-else 链（4 种组合）
- 新：`for` 循环遍历剩余 token，含 `=` → 解析 key=value，不含 → label 或抛引导错误
- 新增 `isKnownAttributeKey()` 辅助方法，对未加 `=` 的已知 key 名抛引导错误
- 新增 key 去重检测（`Set<String> seenKeys`）
- `separator=` 空值 → `columnSeparator = Optional.empty()`
- unknown key → 解析错误

### 语法变更

| 场景 | 旧 | 新 |
|---|---|---|
| 分隔符 | `separator \|` | `separator=\|` |
| 分隔符 + label | `lbl separator \|` | `lbl separator=\|` |
| 仅 label | `lbl` | `lbl`（不变） |
| 无参数 | — | —（不变） |
| 旧语法错误 | "unexpected tokens" | "use key=value form … (e.g. separator=<value>)" |

### 验证

- `mvn test`: 321 tests, 0 failures, 0 errors
- SqlLogicTestParserTest 45 tests 全绿（含新增 key=value 边界测试）
- demo.slt / demo_zh.slt / demo_pl.slt 已更新为新语法
