# Review: query-header-kv-attrs

## 审阅范围

- 依据: spec.md, design.md, plan.md
- 改动: 11 files

## 实现正确性

| Spec P0 | 验证 |
|---|---|
| P0-1 key=value 解析 | `for` 循环: 含 `=` → key=value; 不含 `=` → label; 单 label 限制 ✓; 去重 `seenKeys` ✓ |
| P0-2 separator 语义 | `separator=<delim>` → `Optional.of(value)`; `separator=` → `Optional.empty()` ✓ |
| P0-3 旧语法拒绝 | `isKnownAttributeKey("separator")` → 引导错误 ✓ |
| P0-4 ---- separator 错误 | 消息已更新为 `separator=<delim>` ✓ |
| P0-5 错误模板 | 所有引用点已更新 ✓ |

| Plan T | 验证 |
|---|---|
| T1 parser 重写 | ✓，编译通过 |
| T2 错误消息 | ✓，2 处 `separator <delim>` → `separator=<delim>` |
| T3 examples | ✓，3 个 .slt 文件 7 处更新 |
| T4 fixtures | ✓，2 个 .test 文件更新 |
| T5 测试 | ✓，旧测试适配 + 新增 key=value 边界测试 |
| T6 Javadoc | ✓，3 处更新 |

## 测试有效性

- `mvn test`: 321 tests, 0 failures
- SqlLogicTestParserTest 45 tests 全绿
- RunnerAcceptanceTest 11 tests 全绿（fixture 解析适配）

## 安全

无新增敏感信息、认证变更。

## 结论

**Approve**
