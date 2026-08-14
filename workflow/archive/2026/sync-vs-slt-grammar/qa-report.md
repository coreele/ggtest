# QA Report: sync-vs-slt-grammar

## 轮次

| 轮次 | 日期 | 实现版本 | 范围 | 结论 |
|---|---|---|---|---|
| 1 | 2026-08-11 | `6051da7` (HEAD `c81a730`) | 首测（自动化层 V1–V5 + 语法 sanity + 文档/安全） | Pass（V6 待用户视觉确认） |
| 2 | 2026-08-11 | HEAD `c81a730` → 仅 V6 回归 | V6 L4 视觉检查（复测清单 1–7 项） | Pass |

## QA 入口门禁核验

| 门禁 | 结果 | 证据 |
|---|---|---|
| Plan 已用户确认且持久化 | Pass | 工作项记录进度笔记：`2026-08-11 Plan 已由用户确认（用户会话：ok）`；plan.md 已入库 |
| Review 门禁 `required` 且有 `Approve` | Pass | review.md 结论 `Approve`，无阻塞项 |
| 存在可验收实现与 Plan 验证要求 | Pass | 源分支 `sync-vs-slt-grammar` 提交 `6051da7`；Plan §验收与验证 V1–V6 齐备 |

## 环境与命令

- 实现版本：`6051da7`（feature 提交）、HEAD `c81a730`（status → qa）
- 分支：`sync-vs-slt-grammar`
- OS：macOS；**headless**（无编辑器 GUI，V6 无法在本环境执行，见下）
- 命令：Plan §验收与验证 V1–V5 全部由 QA 在仓库根独立复跑

## 覆盖（对照 Plan §验收与验证）

| ID | 要求或命令 | 结果 | 证据 |
|---|---|---|---|
| V1 | `python3 -m json.tool vs-slt/syntaxes/sqllogictest.tmLanguage.json` | Pass | exit 0，stdout 为格式化 JSON 副本，无错误 |
| V2 | `jq empty vs-slt/syntaxes/sqllogictest.tmLanguage.json` | Pass | exit 0，无输出（本机装有 jq） |
| V3 | Plan 132-186 行正则样本脚本原样执行 | Pass | 输出 `all regex checks passed`，exit 0 |
| V4 | `grep -n -- "---- separator" vs-slt/README.md` | Pass | 无输出，exit 1 |
| V5 | `git status --short` + `git diff main...HEAD --stat` | Pass | 工作区干净；`main...HEAD` 仅 `vs-slt/`（README + tmLanguage.json）与 workflow 文档变更，**无** parser / examples / package.json / vsix 改动（专项 diff 校验 `src/ examples/ vs-slt/package.json vs-slt/sqllogictest-0.0.1.vsix` 为空） |
| V6 | L4 人工视觉检查（Cursor / VS Code 打开 `examples/demo2.slt`） | Pass（轮次 2） | 用户会话 2026-08-11 安装 vs-slt（源码目录）并打开 demo2.slt，逐项核对清单 1–7 项全部通过。 |

## V6 — L4 视觉检查清单（待用户执行）

安装方式：README Option A（源码目录）——Command Palette → `Extensions: Install from Location…` → 选 `vs-slt` 目录 → 重新打开 `examples/demo2.slt`，语言模式应为 **SQL Logic Test**（如需 CLI 方式可 `cd vs-slt && npx --yes @vscode/vsce package` 后 `cursor --install-extension`，为可选）。

逐项核对：

1. `statement ok`（demo2.slt:14/17/55）：`statement` 为 keyword 色，`ok` 为 constant/language 色。
2. `statement ok conn=c1`（demo2.slt:21/24/32/36/39/42）：`conn=c1` 以独立属性色（`entity.other.attribute-name` 主题色）呈现，不吞并后续 SQL 行。
3. `statement error conn=c2 timeout=2000`（demo2.slt:28）：`error` 为 constant 色，`conn=c2` 与 `timeout=2000` 均为属性色。
4. `query II nosort`（demo2.slt:46）：`query` keyword 色、`II` 类型色、`nosort` sort 色。
5. `----` 期望头（demo2.slt:48）：`----` 为期望分隔符色；下方结果行（`1`/`140`/`2`/`200`）不作为 SQL 高亮。
6. 顶层 `#` 注释（demo2.slt:1-12/13/20/27/31/35/45/54）：整行为注释色。
7. SQL body（demo2.slt:15/18/22/25/29/33/37/40/43/47）：`CREATE`/`INSERT`/`BEGIN`/`UPDATE`/`COMMIT`/`SELECT` 等关键字按 `source.sql` 高亮。
8. 可选回归：任一正文行替换为 `#` 开头后 body 不结束（与 T5 决策一致）；`---- separator |` 不显示为期望头高亮。

以上逐项确认后通知 QA 追加回归轮次与最终结论；任一未过则回 Developer。

## 语法 sanity 检查（QA 独立，不依赖 dev-notes）

对 grammar 实际 regex 与 `examples/demo2.slt` 头部行 + 捕获组做 spot-check：

- statement-block 两分支捕获正确：`statement ok conn=c1` → keyword=`statement`、constant=`ok`、attribute=` conn=c1`；`statement error conn=c2 timeout=2000` → keyword=`statement`、constant=`error`、消息组（未捕获，保持普通文本）、attribute=` conn=c2 timeout=2000`；`statement error msg conn=c1` 消息 `msg` 不并入属性。`ok` 分支不吞消息（`statement ok <token>` 不命中 begin，与 parser `ok does not take additional operands` 一致）。
- query-block：`query II nosort`、`query I nosort same_id`（label=`same_id`）、`query IIT nosort separator=|`、`query III valuesort lbl conn=main timeout=500` 均命中且 keyword/type/sort/label/attribute 捕获正确；`[ITR]+` 与 parser `ColumnType` 的 I/T/R 一致。
- expectation-block：精确 `----` 命中（组 1 → `punctuation.separator.expectation`）；`---- separator |` 不命中（与 `requireExactExpectationHeader` 一致）。
- skipif/onlyif：`skipif sqlite # empty RHS` → db=`sqlite`、注释组=` # empty RHS`（comment scope）；`onlyif postgres # empty RHS` 同。
- 顶层 comment 规则对 demo2.slt 的 `#` / `###` 行命中。
- `sql-body.end` 对 `#` 行 / `SELECT 1` 不命中、对 record-start / `----` / 空白行命中（T5）。

结论：grammar 确实按 Plan 声明的高亮契约着色，sanity 通过。

## 回归测试

| 检查项 | 结果 | 证据 |
|---|---|---|
| demo2.slt 全文件头部行命中（V3 内建） | Pass | V3 脚本枚举 demo2.slt 每一非空行，`statement`/`query`/`skipif`/`onlyif`/`hash-threshold`/`halt`/`----`/`#` 均命中对应规则 |
| 10 条正向额外用例 + `---- separator |` 负向 + `sql-body.end` 五条 | Pass | V3 `extra` / `neg` / `end-neg` / `end-pos` 全部通过 |
| 既有 `statement ok` 纯头（无属性） | Pass | `statement ok` 命中 statement-block begin |
| 既有精确 `----` 期望头行为不退化 | Pass | expectation-block begin 仅精确匹配 `----` |

## 文档与安全验收

| 项 | 结果 | 备注 |
|---|---|---|
| 用户文档 `vs-slt/README.md` | Pass | 第 30 行已删除过时 `---- separator <delim>`，仅保留精确 `----`（V4 佐证）；安装（Option A/B）与手工检查步骤、预期结果齐备（A.3 适用） |
| 开发 / 运维文档 | N/A | 语法 JSON 本身即配置；无部署/排障流程变更，理由充分 |
| 安全验证 | Pass | 纯 TextMate 语法 JSON + README 配置变更；`git diff main...HEAD` 扫描无密钥/连接串/.env/认证/依赖变更；`package.json` 无 diff |

## 缺陷

| ID | 严重度 | 摘要 | 状态 | 处理说明 | 验证证据 |
|---|---|---|---|---|---|
| — | — | 无缺陷 | — | — | — |

Review 非阻塞观察（4 条）逐条复核，均确认**非阻塞**：

1. dev-notes 风险项「statement 属性值含 `=`（`conn=a=b`）begin 不命中」不准确——实测 `statement ok conn=a=b` 命中 begin（`[^\s]+` 可捕获含 `=` 值）。风险记录为保守误述，不影响验收；非缺陷。
2. `query I separator=| nosort`（属性先于 sort）grammar begin 不命中→整行无高亮。parser 接受（`nosort` 视为 label），非 Plan 用例、非真实写法；非缺陷。
3. `statement ok conn=c1 # note` 尾部注释 grammar begin 不命中。parser 仅对 skipif/onlyif 裁剪 `#`，该形态非 SLT 约定；非缺陷。
4. `sql-body.end` 的 `(?=^----)` 无 `\s*`，带前导空格 `----` 在 sql-body 内不弹出内层。前序遗留、未在本次改动范围；非缺陷。

## 阻塞（Blocked 时）

- 原因：N/A（V6 不可执行为 Plan §验证缺口已声明的验证缺口，非环境阻塞导致的验收缺陷）
- 风险：L4 无法在本环境执行，token 边界/嵌套作用域着色依赖人工确认（Plan §验证缺口第 1 项）
- 恢复条件：用户安装 vs-slt 后按上节清单核对 `examples/demo2.slt`，结果补入本报告
- 复测范围：上节清单第 1–7 项（第 8 项为可选回归）

## 结论

- 自动化层（V1–V5 + 语法 sanity + 文档/安全）：**Pass**
- V6（L4 视觉检查）：**Pass**（轮次 2，用户 2026-08-11 视觉确认通过）
- 最终结论：**Pass**
