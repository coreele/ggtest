# Plan: query-header-kv-attrs

## 元信息

- 工作项标识: query-header-kv-attrs
- 依据 Spec: workflow/archive/2026/query-header-kv-attrs/spec.md
- 依据 Design: workflow/archive/2026/query-header-kv-attrs/design.md
- 路径等级: standard
- Review 门禁: required
- 最低验证层: L3（单元测试 + 集成测试）
- 验证命令: `mvn test`
- 预期证据: 321 tests, 0 failures

## 目标摘要

将 query header 中 `separator <delim>`（两个独立 token）改为 `separator=<delim>`（单个 key=value token），并建立可扩展的属性解析框架。

## 任务拆解

### T1: 重写 parser 属性解析（SqlLogicTestParser.parseQuery）

替换 `parseQuery` 中 L189-217 的 label/separator 解析逻辑为新的 key=value 循环：

```
for 每个 remaining token:
  含 '=' → 解析 key=value：
    key=separator → 设置 columnSeparator，校验空白
    key=未知    → ParseException "unknown attribute key"
  不含 '=' → 作为 label（最多一个）
  不含 '=' 但等于已知 key 名 → ParseException "use key=value form"
```

- 空值 `separator=` → `columnSeparator = Optional.empty()`
- 重复 key → ParseException
- 多个 label → ParseException

### T2: 更新错误信息模板

更新 `SqlLogicTestParser` 中所有引用旧语法的错误消息：
- `separator <delim>` → `separator=<delim>`
- `---- separator was removed` → 更新为新语法指引
- `unexpected tokens in query header after separator <delim>` → 更新措辞
- `requireValue` 附近无关（仅 CLI 用）

### T3: 更新 examples/*.slt

| 文件 | 旧 | 新 |
|---|---|---|
| `demo.slt:105` | `query IIT nosort separator \|` | `query IIT nosort separator=\|` |
| `demo.slt:111` | `query IIT nosort separator ,` | `query IIT nosort separator=,` |
| `demo.slt:117` | `query IIT nosort separator \|` | `query IIT nosort separator=\|` |
| `demo_zh.slt:104` | `query IIT nosort separator \|` | `query IIT nosort separator=\|` |
| `demo_zh.slt:110` | `query IIT nosort separator ,` | `query IIT nosort separator=,` |
| `demo_zh.slt:116` | `query IIT nosort separator \|` | `query IIT nosort separator=\|` |
| `demo_pl.slt:203` | `query TT nosort separator \|` | `query TT nosort separator=\|` |

### T4: 更新测试 fixture 文件

| 文件 | 行 | 旧 | 新 |
|---|---|---|---|
| `rowwise-pipe-separator.test:8` | `query IIT nosort separator \|` | `query IIT nosort separator=\|` |
| `rowwise-mixed.test:17` | `query III nosort separator \|` | `query III nosort separator=\|` |

### T5: 更新 SqlLogicTestParserTest

- `queryHead_separatorPipe_noLabel_bindsDelim` → 改为 `separator=|`
- `p0_3_queryHead_separatorNonPipe_noLabel_bindsDelim` → 改为 `separator=::`
- `p0_4_queryHead_separatorPipe_withLabel_bindsDelim` → 改为 `lbl separator=|`
- `p0_5_queryHead_separatorWithoutDelimiter_labelConsumed` → 改为 `lbl separator`（无 `=`）→ 应抛引导错误
- `p1_1_queryHead_separatorThenExtraToken` → 适配新错误格式
- 新增测试: `separator=` 空值、`separator=a b` 空白报错、重复 key、unknown key、旧语法引导错误
- 删除与旧 `---- separator` 形式相关的过时测试（或更新为新的错误格式）

### T6: 更新 Javadoc

- `QueryRecord.java:17` — `separator <delim>` → `separator=<delim>`
- `ResultComparer.java:15` — 同上
- `ExpectedResultExpander.java:22` — 同上
- `SqlLogicTestRunner.java:33` — 同上

### T7: 全量测试

`mvn test` — 预期 321 tests, 0 failures

## 依赖与顺序

```
T1 → T2 → T3, T4, T5, T6 可并行 → T7
```

## 触碰路径

| 文件 | 操作 |
|---|---|
| `parser/SqlLogicTestParser.java` | 重写属性解析 + 错误信息 |
| `model/QueryRecord.java` | Javadoc |
| `normalize/ResultComparer.java` | Javadoc |
| `normalize/ExpectedResultExpander.java` | Javadoc |
| `runner/SqlLogicTestRunner.java` | Javadoc |
| `parser/SqlLogicTestParserTest.java` | 测试适配 + 新增 |
| `examples/demo.slt` | 语法更新 |
| `examples/demo_zh.slt` | 语法更新 |
| `examples/demo_pl.slt` | 语法更新 |
| `fixtures/runner/rowwise-pipe-separator.test` | 语法更新 |
| `fixtures/runner/rowwise-mixed.test` | 语法更新 |

## 验收与验证

| ID | 要求 | 预期证据 |
|---|---|---|
| V1 | `mvn compile` | BUILD SUCCESS |
| V2 | `mvn test` | 321 tests, 0 failures |
| V3 | 旧语法 `separator \|` 抛出引导错误 | 包含 "separator=<delim>" 的 ParseException |
| V4 | `separator=` 空值解析为 value-per-line | 对应测试通过 |
| V5 | unknown key 抛出 ParseException | 测试通过 |
| V6 | `./bin/ggtest` 跑 demo.slt 正常 | [PASSED] / [FAILED] 行为不变 |

## 文档影响

| 类别 | 更新路径或 N/A 理由 |
|---|---|
| 开发文档 | N/A（Javadoc 即为开发文档） |
| 用户文档 | N/A（demo.slt 即用户示例） |
| 运维文档 | N/A |

## 交接顺序

1. Developer → 2. Reviewer (Approve) → 3. QA → 4. 合并授权 → done
