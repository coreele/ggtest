# Review: sync-vs-slt-grammar

## 审阅范围

- 实现版本 / 提交: `6051da7`（`feat(vs-slt): sync grammar to current SLT syntax + workflow -> reviewing`），源分支 `sync-vs-slt-grammar`
- 依据: plan.md（无 spec.md / design.md，门禁均 skipped，理由见工作项记录）
- 对照契约: `src/main/java/com/ggtest/parser/SqlLogicTestParser.java`（`parseStatement` 133-206、`parseQuery` 216-331、`requireExactExpectationHeader` 404-422、`stripTrailingHashComment` 460-467、`readSqlBody` 490-506、`isRecordStart` 549-564、`ColumnType` I/T/R）
- 审阅动作: 重跑 V1–V5；自定义正则探针（多属性、`=` 值、`#` 消息字面量、惰性 db 名、外层 end 行为、全 examples + fixtures 头部扫描）

## 实现正确性

六个缺口（T1–T6）全部修复并与 parser 对齐；逐规则结论：

- **statement-block（T1）**：两分支 begin 验证通过。`ok` 分支：`statement ok` + 零或多个 `timeout=`/`conn=` 属性，不取消息——`statement ok msg` 不命中 begin，与 parser `"ok does not take additional operands"`（159-162）一致。`error` 分支：消息 token 用 `(?!\S*[=])\S+` 负向前瞻，整词不含 `=` 者归消息、首个 `key=` 处停下交给属性组；`#` 在消息中为字面量（`statement error parse error #42 timeout=5000` 命中，消息 = `parse error #42`），与 parser 按首个含 `=` token 分界（153-158）一致。捕获组 1/4→keyword、2/5→constant、3/7→attribute；消息组 6 未捕获（保持普通文本，符合 Plan）。
- **query-block（T2）**：`[ITR]+` 与 `ColumnType` 的 I/T/R 精确一致（原 `[TIR]+` 字符集等价）；sort 模式可选；label 用 `(?!\S*[=])\S+` 单 token，避免吞掉 `separator=` 前缀。label-vs-attr 歧义用例 `query I nosort same_id separator=|`、`query IIT nosort separator=|`、`query III valuesort lbl conn=main timeout=500` 均命中且捕获正确。
- **expectation-block（T3）**：begin 仅 `^\s*(----)\s*$`；`---- separator |`、`---- separator=abc`、`----x` 均不命中，与 `requireExactExpectationHeader`（trim 精确 `----`）一致。
- **skipif/onlyif（T4）**：`(\S+?)(\s*#.*)?` 惰性 db 名验证通过——`skipif sqlite # empty RHS` 归组为 db=`sqlite`、注释=` # empty RHS`；`skipif sqlite#foo` 归组 db=`sqlite`、注释=`#foo`，与 parser 的 `indexOf('#')` 裁剪（461-467）一致。
- **sql-body（T5）**：`sql-body.end` 移除 `(?=^\s*#)` 后对 `#` 行/SQL 行不命中、对 record-start/`----`/空白行命中，与 `readSqlBody`（空白行或 record-start 才终止）一致。Developer 同步移除外层 `statement-block.end`/`query-block.end` 的 `(?=^\s*#)`：经 TextMate begin/end 嵌套语义推演，该决策**成立且更优**——`#` 紧贴表头、sql-body 因 begin 的 `(?!\s*#)` 尚未开启的边界情形下，外层 `#` 分支会提前结束块（与 parser 语义相悖），移除后块保持开启，`#` 行归顶层 comment 规则着色（parser 读为 SQL 字面量，仅着色差异、结构正确）。已开 sql-body 后内层 end 优先，外层 `#` 分支本就不会被触达，移除无回归。
- 未改动 `expectation-block.end` 的 `(?=^\s*#)`：`#` 行出现在期望结果块属病理输入，dev-notes 已记录该理论不一致，未扩大变更范围，可接受。

## 测试有效性

- V1（JSON 有效）、V2（jq）、V3（正则样本校验）由 Reviewer 独立重跑通过，与 dev-notes 记录的 V1–V5 证据一致。
- 额外探针（20+ 条）：多属性任意顺序、`=` 值（`conn=a=b`）、`error` 无消息/纯属性、`skipif`/`onlyif` 多 `#`、外层 statement/query end 对 `#` 行的负向与对 record-start/空白行正向——全部符合 parser 语义。
- 全量扫描 `examples/demo.slt`、`demo_zh.slt`、`demo2.slt` 与 `src/test/resources/fixtures/*.test` 的所有头部行，均命中对应规则（含 `query II rowsort label1`、`query IIT nosort separator=|`、`statement error` 无消息、`skipif postgresql` 等）。
- 局限：无 TextMate scope-stack 自动化（Plan §验证缺口已声明，V6 L4 视觉检查由 QA 执行），属 grammar 配置类变更的已知限制，风险与恢复条件记录完整。

## 文档影响核对

| Plan 声明 | 实现是否一致 | 备注 |
|---|---|---|
| 开发文档 | N/A | 语法 JSON 本身即配置，无代码注释/API/配置文档变更，理由充分 |
| 用户文档 | 一致 | `vs-slt/README.md:30` 已删除 `---- separator <delim>`；grep 确认无残留；手工检查清单其余条目与 demo.slt 实际特性对应 |
| 运维文档 | N/A | 无部署/排障流程变更；VSIX 重新打包为可选验证（Plan §验证缺口），不入文档 |

## 安全影响核对

检查范围与变更影响面一致：纯 TextMate 语法 JSON + README 配置变更，无敏感信息、认证授权、输入处理、外部网络或依赖变更。

| 检查项 | 结果 | 处置状态 | 备注 |
|---|---|---|---|
| 敏感信息 | 无 | 通过 | 提交 diff 扫描无密钥/连接串/.env |
| 认证与授权 | 不涉及 | N/A | 无认证/授权代码路径 |
| 输入与外部访问 | 不涉及 | N/A | grammar 仅匹配文本 token，无代码执行/网络 |
| 依赖变更 | 无 | 通过 | 未新增或升级依赖；`package.json` 无 diff |

## 必修项

| ID | 位置 | 问题 | 状态 |
|---|---|---|---|
| — | — | 无阻塞项 | — |

> `Comment` 不得包含阻塞项；阻塞问题须使用 `Request changes`。本工作项无阻塞项，结论 `Approve`。

## 结论

Approve

## 后续动作与复审范围

- 交由 Manager 调度 QA。QA 复跑 V1–V5 并追加 V6 L4 视觉检查（`examples/demo2.slt`，清单见 dev-notes），结果记入 `qa-report.md`；未过则回 Developer。
- 非阻塞观察（不阻止 QA，供备查，不要求本轮修复）：
  1. dev-notes 风险项「statement 属性值含 `=`（如 `conn=a=b`）begin 不命中」**不准确**——`[^\s]+` 可捕获含 `=` 的值，实测 begin 命中，与 parser 接受语义一致；属风险记录的保守误述，建议修复时一并更正。
  2. `query I separator=| nosort`（属性先于 sort）parser 接受（`nosort` 被当作 label），grammar begin 不命中→整行无高亮。非 Plan 用例、非真实写法，仅记录。
  3. `statement ok conn=c1 # note` 尾部注释 parser 接受（属性循环跳过无 `=` token），grammar begin 不命中。parser 仅对 skipif/onlyif 裁剪 `#` 注释，该形态非 SLT 约定，仅记录。
  4. sql-body.end 的 `(?=^----)` 无 `\s*`：带前导空格的 `----` 行在 sql-body 内不弹出内层，expectation-block 会嵌套在 sql-body scope 内着色。前序遗留、未在本次改动范围，仅记录。
- 复审范围：若 QA 退回，按缺陷定位对应规则复测；本 Review 无需复审。
