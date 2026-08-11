# dev-notes: sync-vs-slt-grammar

- 工作项标识: `sync-vs-slt-grammar`（未拆分，feature-id = sub-feature-id）
- 实施角色: Developer
- 日期: 2026-08-11
- 源分支: `sync-vs-slt-grammar`
- 约束遵守: 未修改 `SqlLogicTestParser.java`、`examples/*.slt`、`vs-slt/package.json`（版本保持 0.0.3）、`vs-slt/sqllogictest-0.0.1.vsix`

## 变更摘要（T1–T6）

| 任务 | 变更文件 | 说明 |
|---|---|---|
| T1 | `vs-slt/syntaxes/sqllogictest.tmLanguage.json`（`statement-block`） | `begin` 改为两个分支：(a) `statement ok` + 零或多个 `timeout=` / `conn=` 属性（任意顺序、可多个）；(b) `statement error` + 零或多个不含 `=` 的消息 token（负向前瞻 `(?!\S*[=])\S+` 保证消息 token 整词不含 `=`，`#` 按字面量计入消息）+ 零或多个 `timeout=` / `conn=` 属性。`ok` 分支不含消息捕获，因此 `statement ok <token>` 不命中 begin（与 parser `SqlLogicTestParser.java:159-162` 的 "ok does not take additional operands" 对齐）。保留捕获：`1`/`4`=statement → `keyword.control.sqllogictest`，`2`/`5`=ok/error → `constant.language.sqllogictest`；新增 `3`/`7`=属性 → `entity.other.attribute-name.sqllogictest`；错误消息（组 `6`）不捕获，保持普通文本。 |
| T2 | 同上（`query-block`） | `begin` 匹配 `query [ITR]+`（类型改为 `[ITR]+`，与原 `[TIR]+` 等价）+ 可选 `nosort|rowsort|valuesort` + 可选单 label（`(?!\S*[=])\S+`，不含 `=` 的整词 token，避免把 `separator=` 的前缀吞成 label）+ 零或多个 `separator=` / `timeout=` / `conn=` 属性（任意顺序）。保留捕获：`1`=query → `keyword.control.sqllogictest`、`2`=类型 → `entity.name.type.sqllogictest`、`3`=sort → `keyword.other.sort.sqllogictest`、`4`=label → `entity.name.label.sqllogictest`；新增 `5`=属性 → `entity.other.attribute-name.sqllogictest`。 |
| T3 | 同上（`expectation-block`） | `begin` 仅匹配 trim 后精确 `----`：`^\s*(----)\s*$`。删除旧的 `---- separator <delim>` 分支及其 beginCaptures（组 2/4 移除），只保留组 1 `----` → `punctuation.separator.expectation.sqllogictest`。`---- separator |` 不再命中 begin（对照 `requireExactExpectationHeader`，`SqlLogicTestParser.java:404-422`）。 |
| T4 | 同上（`skipif` / `onlyif`） | `match` 增加可选尾部 `# ...` 注释：`^\s*(skipif)\s+(\S+?)(\s*#.*)?\s*$`（`onlyif` 同）。db name 用惰性 `\S+?`，使 `#` 之后部分整段归入注释捕获组 `3` → `comment.line.number-sign.sqllogictest`（对照 `stripTrailingHashComment`，`SqlLogicTestParser.java:460-467`）。保留捕获 `1` → `keyword.control.sqllogictest`、`2` → `variable.other.database.sqllogictest`。 |
| T5 | 同上（`sql-body` + 外层 `statement-block.end` / `query-block.end`） | `sql-body.end` 移除 `(?=^\s*#)` 终止分支，`#` 行在 sql-body 开始后不再终止 body（对照 `readSqlBody`，`SqlLogicTestParser.java:490-506`：`#` 行作为 SQL 字面量读入）。**同步移除** `statement-block.end` / `query-block.end` 中的 `(?=^\s*#)`，决策与理由见下文「T5 `#` 处理决策」。记录之间的全行 `#` 注释仍由顶层 `comment` 规则（`^\s*#.*$`）着色（`skipTrivia`，`SqlLogicTestParser.java:68-77`）。 |
| T6 | `vs-slt/README.md:30` | 删除过时 `---- separator <delim>` 说明，仅保留精确 `----` 期望头文档。`package.json` / VSIX 未改动。 |

## T5 `#` 处理决策

**决策：** 除 `sql-body.end` 外，同时移除了 `statement-block.end` 与 `query-block.end` 中的 `(?=^\s*#)` 终止分支。

**理由：** 依据 TextMate begin/end 作用域语义，嵌套作用域内最内层 scope 的 `end` 优先判定——sql-body 已开始后，其 `end` 不匹配 `#` 行，外层 `end` 不会被触达，`#` 行保持在 sql-body 内（即「内层 scope 优先」，`#` 行为字面 SQL）。但 `#` 紧贴表头、尚无任何前导 SQL 行的边界情形下，sql-body 的 `begin` 含 `(?!\s*#)` 不会开启，此时外层 `end` 的 `(?=^\s*#)` **会**触发、使块结束并把该行交给顶层 comment 规则着色——这与 parser 语义相悖（parser `readSqlBody` 将紧贴表头的 `#` 行也读为 SQL 字面量，`isRecordStart` 对 `#` 行返回 false）。为与外层行为一致（`#` 行不结束任何块），故同步移除外层 `end` 的 `#` 分支。该边界情形不涉及 `examples/demo2.slt`，本决策不影响 demo2.slt 的高亮；`demo2.slt` 中记录之间的 `#` 注释均位于空白行分隔之后，此时块已结束，仍由顶层 comment 规则着色，行为不变。

**未改动 `expectation-block.end`：** 其 `end` 保留 `(?=^\s*#)`。Plan T3 仅要求改 `begin`；期望结果块内 `#` 行属病理输入（`readExpectedResults` 以空白行为界，`#` 行会被读为期望值），不在 demo2.slt / Plan 正向用例范围，保留现状并记录此处潜在的不一致，未扩大变更范围。

## 验证结果（T7）

### V1 — JSON 语法有效

命令：`python3 -m json.tool vs-slt/syntaxes/sqllogictest.tmLanguage.json`

结果：exit 0；`json.tool` 打印缩进后的 JSON（stdout 输出为格式化副本，非校验警告），无错误输出。

### V2 — jq 校验

命令：`jq empty vs-slt/syntaxes/sqllogictest.tmLanguage.json`

结果：exit 0，无输出。

### V3 — 正则样本校验（Plan §验收与验证，132-186 行脚本原样执行）

命令：`python3 - <<'PY' … PY`（Plan 中给定脚本，未改动）

输出：

```
all regex checks passed
```

exit 0。覆盖：demo2.slt 全文件头部命中、10 条正向额外用例、`---- separator |` 负向用例、`sql-body.end` 的 end-neg（`# comment inside sql body` / `SELECT 1` 不命中 end）与 end-pos（`statement ok` / `query I` / `skipif sqlite` / `----` / `   ` 命中 end）。

### V4 — README 无过时形式

命令：`grep -n -- "---- separator" vs-slt/README.md`

结果：无输出，exit 1。

### V5 — 改动范围

命令：`git status --short`

结果：

```
 M vs-slt/README.md
 M vs-slt/syntaxes/sqllogictest.tmLanguage.json
```

`git diff --stat`：仅上述两个文件（+30 / -15）。无 parser / examples / package.json / vsix 改动。

### 补充校验 — 外层 `end` 与 `#` 交互

命令：内联 python3 脚本校验 `statement-block.end` / `query-block.end` / `sql-body.end` 行为。

结果：`outer-end # interplay check: passed`——三个块的 `end` 均不命中 `#` 行 / `SELECT 1`，均命中 record-start / 空白行（`sql-body.end` 另命中 `----`）。该补充校验佐证 T5 决策。

## L4 视觉检查清单（V6，待 QA 执行）

Developer 不运行编辑器 GUI 视觉检查；以下为 QA 在 Cursor / VS Code 安装 vs-slt（源码目录方式，README Option A）后打开 `examples/demo2.slt` 的核对清单，结果记入 `qa-report.md`：

1. `statement ok`（demo2.slt:14/17/55）：`statement` 高亮为 keyword 色，`ok` 高亮为 constant/language 色。
2. `statement ok conn=c1`（demo2.slt:21/24/32/36/39/42）：`conn=c1` 以独立属性色（`entity.other.attribute-name` 主题色）呈现，不吞并后续 SQL 行。
3. `statement error conn=c2 timeout=2000`（demo2.slt:28）：`error` 为 constant 色，`conn=c2` 与 `timeout=2000` 均为属性色。
4. `query II nosort`（demo2.slt:46）：`query` keyword 色、`II` 类型色、`nosort` sort 色。
5. `----` 期望头（demo2.slt:48）：`----` 为期望分隔符色；下方结果行（`1`/`140`/`2`/`200`）不作为 SQL 高亮。
6. 顶层 `#` 注释（demo2.slt:1-12/13/20/27/31/35/45/54）：整行为注释色。
7. SQL body（demo2.slt:15/18/22/25/29/33/37/40/43/47）：`CREATE`/`INSERT`/`BEGIN`/`UPDATE`/`COMMIT`/`SELECT` 等关键字按 `source.sql` 高亮。
8. 可选回归：任一正文行替换为 `#` 开头后 body 不结束（与 T5 决策一致）；`---- separator |` 不显示为期望头高亮。

## 未解决风险

- **`source.sql` 内嵌高亮依赖编辑器内置 SQL grammar**：`meta.embedded.block.sql` 的 `include: source.sql` 行为与编辑器的 SQL TextMate grammar 相关，视觉结果以 V6 实测为准（Plan §验证缺口第 2 项）。
- **`expectation-block.end` 保留 `(?=^\s*#)`**：期望结果块内出现 `#` 行时会使块提前结束，与 parser（`readExpectedResults` 将 `#` 行读为期望值）存在理论不一致；病理输入，不在 demo2.slt / Plan 正向范围，记录备查。
- **statement 头属性值允许含 `=` 时**（如 `conn=a=b`）属性值用 `[^\s]+` 捕获，`begin` 不命中——parser 亦仅校验「不含空白」，可接受此类值但 grammar 不覆盖；非 Plan 用例，记录备查。

## 文档影响

- 用户文档：`vs-slt/README.md` 已更新（T6）。
- 开发 / 运维文档：N/A（语法 JSON 本身即配置；无部署/排障流程变更）。

## 交接

建议后续角色：Reviewer（Review 门禁 `required`）。审阅重点：statement/query 头正则与 parser 对齐（对照 `SqlLogicTestParser.java:133-206`、`216-285`、`404-422`、`460-467`、`490-506`）、T5 外层 `end` 的 `#` 分支移除决策、README 与 Plan 声明一致、安全影响（N/A）。
