# Plan: sync-vs-slt-grammar

## 元信息

- 工作项标识: sync-vs-slt-grammar
- sub-feature-id: sync-vs-slt-grammar（未拆分，与 feature-id 相同）
- 依据 Spec: N/A（Spec 门禁 skipped：高亮契约已由 `SqlLogicTestParser.java` 与 README 定义，无新增行为合同，见工作项记录进度笔记）
- 依据 Design: N/A（Design 门禁 skipped：纯语法配置文件的 regex 更新，无模块边界 / 分层 / 选型决策）
- 依据 UI: N/A（无用户可见界面布局 / 交互决策；语法高亮 token 着色属文本渲染，非本工作流 UI 范畴）
- 路径等级: standard
- Review 门禁: required
- 最低验证层: L1（JSON 语法有效 + 正则样本匹配）+ L4（编辑器内人工视觉检查）
- 验证命令:
  - `python3 -m json.tool vs-slt/syntaxes/sqllogictest.tmLanguage.json`
  - `jq empty vs-slt/syntaxes/sqllogictest.tmLanguage.json`（可选，未装 jq 时以 V1 为准）
  - `python3 - <<'PY' … PY`（正则样本校验脚本，见 §验收与验证 V3）
  - `grep -n -- "---- separator" vs-slt/README.md`（预期无匹配）
  - `git status --short`（确认改动范围）
- 预期证据: JSON 解析 exit 0 无输出；正则脚本输出 `all regex checks passed` 且 exit 0；grep 无匹配（exit 1）；`git status` 仅显示 `vs-slt/` 与工作流文档变更，无 parser / examples 改动

## 适用工程规范

> 使用仓库根路径，避免未拆分/已拆分目录深度不同导致相对链接断裂。

- `workflow/docs/standards/documentation.md`
- `workflow/docs/standards/git.md`（仅 Git 工作区）
- `workflow/docs/standards/quality.md`
- `workflow/docs/standards/security.md`（N/A：纯语法高亮配置文件，无敏感信息、依赖、认证授权或数据流变更）

## 目标摘要

同步 `vs-slt` 插件的 TextMate grammar（`vs-slt/syntaxes/sqllogictest.tmLanguage.json`）到 ggtest parser 当前支持的 SLT 语法，使 `examples/demo2.slt` 正确高亮。修正六处缺口：

1. `statement` 头：`ok` 支持 `timeout=` / `conn=` 属性；`error` 支持消息 token（到首个 `key=` 为止，`#` 为字面量）+ 属性。
2. `query` 头：`[ITR]+` 类型 + 可选 sort mode + 可选单 label + `separator=` / `timeout=` / `conn=` 任意顺序。
3. 期望头：仅精确 `----`；`---- separator <delim>` 不再高亮为有效期望头。
4. `skipif` / `onlyif`：允许尾部 `# ...` 注释。
5. SQL body 的 `#` 行：不再作为 sql-body 终止符（与 parser 一致，`#` 为字面量）；记录之间的全行 `#` 注释仍由顶层 comment 规则着色。
6. `vs-slt/README.md` 删除过时的 `---- separator <delim>` 说明。

约束：不修改 parser 代码（`SqlLogicTestParser.java`）、不修改 `examples/*.slt`、不 bump `vs-slt/package.json` 版本（保持 0.0.3）。

## 任务拆解

### T1: `statement` 头 grammar

修改 `statement-block.begin` 使其匹配（对照 parser `SqlLogicTestParser.java:133-206`）：

- `statement ok`（无操作数）或 `statement ok` + `timeout=<ms>` / `conn=<name>` 属性（任意顺序、可多个）；`ok` 不取消息。
- `statement error` + 消息 token（零个或多个，不含 `=` 的 token 序列，到首个 `key=` 为止；`#` 为字面量）+ 可选 `timeout=<ms>` / `conn=<name>` 属性。
- 保留 `keyword.control.sqllogictest`（`statement`）与 `constant.language.sqllogictest`（`ok`/`error`）捕获；属性建议用 `entity.other.attribute-name` 之类独立捕获，`ok`/`error` 后的消息保持普通文本即可。

完成条件：`statement ok conn=c1`、`statement error conn=c2 timeout=2000`、`statement error msg conn=c1` 均命中 begin（V3 正向用例通过）；`ok` 不误吞消息 token。

### T2: `query` 头 grammar

修改 `query-block.begin` 使其匹配（对照 parser `SqlLogicTestParser.java:216-285`）：

- `query [ITR]+` + 可选 `nosort|rowsort|valuesort` + 可选单 label（不含 `=` 的单个 token）+ `separator=<delim>` / `timeout=<ms>` / `conn=<name>` 属性（任意顺序、可多个）。
- 保留 `entity.name.type`（类型签名）、`keyword.other.sort`（sort mode）、label 与属性捕获。

完成条件：`query II nosort`、`query I nosort same_id`、`query IIT nosort separator=|`、`query III valuesort lbl conn=main timeout=500` 均命中 begin（V3 正向用例通过）。

### T3: 期望头仅接受精确 `----`

修改 `expectation-block.begin` 仅匹配 trim 后精确的 `----`（对照 parser `requireExactExpectationHeader`，`SqlLogicTestParser.java:404-422`）；删除旧的 `separator` 分支与对应 beginCaptures。`---- separator <delim>` 不得再被高亮为有效期望头；可选：用 `invalid.illegal` 之类 scope 标红该移除形式，不要求强制。

完成条件：`----` 命中 expectation begin；`---- separator |` 不命中（V3 负向用例通过）。

### T4: `skipif` / `onlyif` 尾部注释

修改 `skipif` / `onlyif` 的 `match` 允许可选尾部 `# ...` 注释（对照 parser `stripTrailingHashComment`，`SqlLogicTestParser.java:460-467`），注释部分以 comment scope 捕获。

完成条件：`skipif sqlite # empty RHS`、`onlyif postgres # empty RHS` 命中且 `#` 尾部按注释着色（V3 正向用例通过）。

### T5: SQL body `#` 行语义对齐

移除 `sql-body.end` 中的 `(?=^\s*#)` 终止分支，使 `#` 行在 sql-body 开始后不再终止 body（对照 parser `readSqlBody`，`SqlLogicTestParser.java:490-506`：`#` 行作为 SQL 字面量读入）。记录之间的全行 `#` 注释仍由顶层 `comment` 规则（`^\\s*#.*$`）着色。

注意边界：`statement-block.end` / `query-block.end` 若保留 `(?=^\\s*#)`，需确认与「sql-body 已开始后遇 `#` 行」不冲突（sql-body 内层 scope 优先，内层不终止即不触发外层 end）；`#` 紧贴表头（无前导 SQL 行）的边界情形不涉及 demo2.slt，Developer 可按 parser 语义取舍并在 dev-notes 说明。

完成条件：`sql-body.end` 对 `#` 行不匹配、对 record-start / `----` / 空行匹配（V3 end 用例通过）；demo2.slt 中 body 内无 `#` 场景回归无异常。

### T6: README 与 package.json

- `vs-slt/README.md:30`：`----` / `---- separator <delim>` → 仅保留精确 `----` 说明。
- `vs-slt/package.json`：**不改**（版本保持 0.0.3，本次不 bump）。
- 过时构建产物 `vs-slt/sqllogictest-0.0.1.vsix`：**不修改、不提交**；VSIX 重新打包仅作可选人工验证（见 §验证缺口），不进入本次变更。

完成条件：README 不再出现 `---- separator`（V4 通过）；package.json 无 diff。

### T7: 验证并记录

运行 §验收与验证全部命令，结果记录到 `workflow/docs/features/sync-vs-slt-grammar/dev-notes.md`（未拆分：feature 根目录）。

完成条件：V1–V5 全部通过；L4 视觉检查清单（V6）在 dev-notes 记录执行步骤与结论。

## 依赖与顺序

```
T1–T5（同一 JSON 文件编辑，Developer 一次性完成，内部顺序自由）
T6（独立文件，可与 T1–T5 并行）
T7（依赖 T1–T6 全部完成）
```

单文件 JSON 改动建议一次完成并即时重验 JSON 有效性，避免中途破坏语法文件；T7 前须再次整体校验。

## 触碰路径

| 文件 | 操作 | 说明 |
|---|---|---|
| `vs-slt/syntaxes/sqllogictest.tmLanguage.json` | 修改 | `statement-block` / `query-block` / `expectation-block` / `skipif` / `onlyif` / `sql-body` 规则 |
| `vs-slt/README.md` | 修改 | 删除过时 `---- separator <delim>` 说明（第 30 行） |
| `vs-slt/package.json` | 不改 | 版本保持 0.0.3；本次不 bump |
| `vs-slt/sqllogictest-0.0.1.vsix` | 不改 | 过时构建产物，非源文件；重新打包仅可选验证 |
| `src/main/java/com/ggtest/parser/SqlLogicTestParser.java` | 不改 | 仅作对齐参照（约束） |
| `examples/*.slt` | 不改 | 仅作样本输入（约束） |

## 验收与验证

| ID | 要求或命令 | 预期证据 | 结果（实施后填） |
|---|---|---|---|
| V1 | `python3 -m json.tool vs-slt/syntaxes/sqllogictest.tmLanguage.json`（仓库根目录执行） | exit 0，无输出（JSON 语法有效） | |
| V2 | `jq empty vs-slt/syntaxes/sqllogictest.tmLanguage.json` | exit 0，无输出（若本机未装 jq，以 V1 为准并记入 dev-notes） | |
| V3 | `python3 - <<'PY' … PY`（脚本见下） | 输出 `all regex checks passed`，exit 0 | |
| V4 | `grep -n -- "---- separator" vs-slt/README.md` | 无输出（exit 1），README 不再含过时形式 | |
| V5 | `git status --short` | 仅 `vs-slt/` 与工作流文档变更；无 parser / examples / package.json / vsix 改动 | |
| V6 | L4 人工视觉检查：Cursor / VS Code 安装 vs-slt 后打开 `examples/demo2.slt`，逐项核对高亮清单 | 清单全项通过并记录于 `qa-report.md`（§验证缺口） | |

V3 使用的脚本（从 grammar 读取实际 regex 校验，正向/负向/`sql-body.end` 行为三组）：

```bash
python3 - <<'PY'
import json, re
g = json.load(open('vs-slt/syntaxes/sqllogictest.tmLanguage.json'))
r = g['repository']
def rgx(k):
    return r[k].get('begin') or r[k].get('match')
def hit(k, line):
    return re.search(rgx(k), line) is not None
fails = []
rules = {'statement': 'statement-block', 'query': 'query-block',
         'skipif': 'skipif', 'onlyif': 'onlyif',
         'hash-threshold': 'hash-threshold', 'halt': 'halt'}
for i, raw in enumerate(open('examples/demo2.slt', encoding='utf-8'), 1):
    s = raw.strip()
    if not s:
        continue
    if s.startswith('#'):
        k = 'comment'
    elif s.startswith('----'):
        k = 'expectation-block' if s == '----' else None
    else:
        k = rules.get(s.split()[0])
    if k and not hit(k, raw):
        fails.append(('demo2.slt', i, k, s))
extra = [
    ('statement ok conn=c1', 'statement-block'),
    ('statement error conn=c2 timeout=2000', 'statement-block'),
    ('statement error msg conn=c1', 'statement-block'),
    ('statement error parse error #42 timeout=5000', 'statement-block'),
    ('query II nosort', 'query-block'),
    ('query I nosort same_id', 'query-block'),
    ('query IIT nosort separator=|', 'query-block'),
    ('query III valuesort lbl conn=main timeout=500', 'query-block'),
    ('skipif sqlite # empty RHS', 'skipif'),
    ('onlyif postgres # empty RHS', 'onlyif'),
]
for line, k in extra:
    if not hit(k, line):
        fails.append(('extra', '-', k, line))
if hit('expectation-block', '---- separator |'):
    fails.append(('neg', '-', 'expectation-block', '---- separator |'))
end = r['sql-body']['end']
for line in ['# comment inside sql body', 'SELECT 1']:
    if re.search(end, line):
        fails.append(('end-neg', '-', 'sql-body', line))
for line in ['statement ok', 'query I', 'skipif sqlite', '----', '   ']:
    if not re.search(end, line):
        fails.append(('end-pos', '-', 'sql-body', line))
if fails:
    for f_ in fails:
        print('FAIL', f_)
    raise SystemExit(1)
print('all regex checks passed')
PY
```

## 验证缺口

| 项 | 原因 | 风险 | 恢复条件 |
|---|---|---|---|
| L4 编辑器视觉检查（V6） | TextMate grammar 无内置自动化测试工具；`vs-slt/` 下无 test 基础设施（已 glob 确认无测试文件），token 着色只能在 Cursor / VS Code 中人工观察 | 自动化无法发现 token 边界 / 嵌套作用域错误着色（如 sql-body 与期望块交替、块结束边界、`#` 行归属），可能合入后才发现高亮不准 | QA 安装 vs-slt 后打开 `examples/demo2.slt`，按清单核对：statement 头属性、query 头属性与 label、期望块 `----` 与结果行、skipif/onlyif 尾注释、SQL 关键字；结果记入 `qa-report.md`；未过则回 Developer |
| 自动化仅覆盖头部行匹配 + `sql-body.end` 守卫，不复现 TextMate 多行 begin/end 作用域推进 | Python 单行正则测试无法模拟编辑器 tokenizer 的多行 begin/end 嵌套语义 | begin/end 配对的嵌套与回退边界（尤其 sql-body 内嵌 `source.sql`）可能不精确 | 依赖 L4 视觉检查 + Reviewer 对正则的静态审阅（对照 parser 行号）；Review 结论须明确正则与 parser 对齐结论 |
| VSIX 重新打包为可选人工验证 | VSIX 是构建产物，`sqllogictest-0.0.1.vsix` 为过时产物；本次为源文件 + README 变更，不要求重新打包 | 若不重新打包，用户需用源码目录方式安装才能看到新高亮（README Option A） | 可选：`cd vs-slt && npx --yes @vscode/vsce package` 后手工安装核对；不作为进入 QA 的前置条件 |

## 文档影响

| 类别 | 更新路径或 N/A 理由 |
|---|---|
| 开发文档 | N/A（无代码注释 / API / 配置文档变更；语法 JSON 本身即配置） |
| 用户文档 | `vs-slt/README.md` — 删除过时 `---- separator <delim>` 说明，保留安装与手工检查指引（A.3 适用：安装步骤与预期结果已具备，仅改一处过期示例） |
| 运维文档 | N/A（无部署、监控、排障、备份恢复流程；VSIX 重新打包仅可选，不入文档） |

## 交接顺序

1. Developer 实施并运行 V1–V5，记录到 `dev-notes.md`，完成 L4 视觉检查初检（T1–T7） →
2. Reviewer（Review 门禁 **required**）：审阅正则与 parser 对齐（对照 `SqlLogicTestParser.java` 相关行号）、文档影响（README 与 Plan 声明一致）、安全影响（N/A）；`Approve` 是进入 QA 的前置条件，`Request changes` 回 Developer 修复并复审 →
3. QA 验收：独立复跑 V1–V5 并追加 V6 L4 视觉检查，逐项对照 Plan 验收表写入 `qa-report.md`；结论 Pass / Fail / Blocked →
4. 用户合并授权 → Manager 在源分支置 `done` 一次提交 → 合入 `main`

## 修订记录

| 日期 | 摘要 |
|---|---|
| 2026-08-11 | 初始编写（Planner） |
