# Spec: add-report-format

> 需求与规格（Plan 之前完成）。任务拆解见后续同目录 `plan.md`。
>
> **feature-id**：`add-report-format` · **sub-feature-id**：`add-report-format`（未拆分时与 feature-id 相同）

## 背景与目标

ggtest CLI 当前仅输出 human-readable 报告（status 行 + 失败块 + `TOTAL:` 汇总，由 `ReportWriter` + `CliSession.execute()` 生成），CI 工具（GitHub Actions、Jenkins）无法稳定解析测试结果。

**目标：** 新增 `--format` 选项，支持机器可读测试报告输出（`junit-xml`、`json`），便于 CI 工具解析结果、生成可视化、门控流水线。

**来源：** `TODO:12-15`（P2 测试报告输出）。

## 非目标

- **不**引入 HTML 报告（CI 工具消费机器可读格式即可，无浏览器渲染需求）。
- **不**改变默认（不带 `--format`）的 human-readable 输出，保持字节级一致（零回归）。
- **不**替换 `ReportWriter`；机器可读格式与 human-readable 并行存在，按 `--format` 取值选择其一。
- **不**引入 `--out`/`-o` 文件输出选项（本范围仅控格式；CI 通过 shell 重定向 `> report.xml` 写文件）。
- **不**改动测试执行语义、退出码优先级（0/1/2）或任何现有选项的行为。

## 范围与可见行为

### CLI 表面

- 新增 `--format <text|junit-xml|json>` 选项，取值三选一。
- 默认 `text`（等价于不带 `--format` 的现有 human-readable 行为）。
- `--format` 仅控制输出格式；输出目标仍为 stdout（与 human-readable 一致）。
- `--format` 与所有现有选项（`--color`、`--halt`、`--override`、`--engine` 等）正交可组合。
- `--help` 输出新增 `--format` 行。

### 默认行为

- 不带 `--format`，或显式 `--format text`：输出与现状字节一致（status 行、失败块、`TOTAL:` 汇总、空行规则、退出码均不变）。

### 与现有选项的交互

- `--halt`：在 junit-xml/json 报告中，因 halt 提前断出而**未执行**的文件不出现在报告；**已执行**的文件按真实桶（PASSED/FAILED/SKIPPED/OVERRIDDEN）如实报告。退出码优先级不变。
- `--override`（golden-update）：仍输出报告；被 override 的记录计入 OVERRIDDEN，在 json 中以结构化字段、在 junit-xml 中以非 `<failure>` 的方式体现（不计为 failure）。退出码不变。
- `--color`：`--format junit-xml|json` 输出**禁止** ANSI 转义（机器可读格式必须为纯文本）；`--format text` 时 `--color` 行为不变。

## 合同

### API / 接口

- `--format <value>` 取值集合：`text`、`junit-xml`、`json`。值大小写处理与现有选项（`--color`、`--engine`）一致。
- 默认值：`text`。不带 `--format` 等价于 `--format text`。
- `--format text` 与不带 `--format` 行为完全等价。
- 未知值（如 `--format bogus`）→ `UsageException` → 退出码 2 + usage error（`Error: usage` + `[WHY] invalid --format value: bogus`），与现有非法值错误风格一致（对照 `--engine`/`--hash-threshold` 的非法值处理）。
- 缺失值（`--format` 后无 token 或下一 token 以 `-` 开头）→ 同上 usage error。

### 数据 / 状态

junit-xml 与 json 共同覆盖的字段语义：

- **文件级：** 文件显示路径（相对 CWD，与 human-readable status 行一致）、文件桶（PASSED/FAILED/SKIPPED/OVERRIDDEN）、hardError 标志、文件耗时（秒，3 位小数）。
- **record 级（推荐粒度）：** 每条 assertable record（statement/query）一个条目，含：
  - outcome（`PASSED`/`FAILED`/`SKIPPED`/`OVERRIDDEN`）
  - 位置：文件名 + 起始行号（1-based，来自 `SourceLocation.startLine`）
  - record kind（statement/query，含 ok/error 子类）
  - 失败原因（FAILED 时）：首行摘要 + diff 文本（来源同 human-readable 中 `failureReason`，如 `query result mismatch`）
  - 结构化 expected/actual：json 提供（`expected`/`actual` 数组 + `diff` 文本）；junit-xml 受 schema 限制用 `<failure message>` + `<system-out>` diff 文本
  - record 耗时（秒，3 位小数）
- **汇总：** totalPassed/Failed/Skipped/Overridden 计数（与 human-readable `TOTAL:` 一致；override 模式下含 overridden）。
- **多文件聚合：** junit-xml 用 `<testsuites>` 包多个 `<testsuite>`（每文件一个）；json 用顶层对象含 `files` 数组与 `summary`。

**JUnit XML schema 兼容性（推荐）：** 遵循业界事实标准的 surefire-style JUnit XSD：`<testsuites>` / `<testsuite name="" tests="" failures="" skipped="" time="">` / `<testcase classname="" name="" time="">` / `<failure message="" type="">…</failure>` / `<system-out>` / `<system-err>`。该 schema 被 Jenkins JUnit plugin、GitHub Actions `mikepenz/action-junit-report`、CircleCI 等广泛识别。

**JSON 形状（推荐自定义）：** 不套用 junit-xml 的 JSON 化（无业界标准）。自定义顶层对象示例：

```json
{
  "summary": { "passed": 3, "failed": 1, "skipped": 0, "overridden": 0 },
  "files": [
    {
      "file": "pass.test",
      "bucket": "PASSED",
      "hardError": false,
      "durationSeconds": 0.012,
      "records": [
        { "kind": "statement", "outcome": "PASSED", "line": 1, "durationSeconds": 0.001 },
        { "kind": "query", "outcome": "FAILED", "line": 5, "durationSeconds": 0.003,
          "reason": "query result mismatch",
          "expected": ["1", "2"], "actual": ["1", "3"],
          "diff": "    1\n-   2\n+   3\n" }
      ]
    }
  ]
}
```

**凭据脱敏：** junit-xml/json 输出与 human-readable 一致，**禁止**出现连接字符串中的 URL user info 与密码。任何源自异常消息的字段（如 hardError 的 `abortReason`、`failureReason`）必须经过与现有 human-readable 相同的脱敏处理（明文密码 → `***`，`scheme://user:pass@` → `scheme://***@`）。

### 错误与约束

- 未知/缺失 `--format` 值 → usage error，退出码 2（见 API/接口）。
- 凭据泄漏约束（强约束，P0 可验证）：任何格式输出均不得含明文密码或 URL user info。
- ANSI 约束（强约束，P0 可验证）：`--format junit-xml|json` 输出不得含 ANSI 转义序列（`\u001B[`），即便 `--color always`。
- XML 合法性：`--format junit-xml` 输出必须是 well-formed XML，可被标准 XML 解析器解析；失败 record 对应 `<testcase>` 含 `<failure>` 子元素。
- JSON 合法性：`--format json` 输出必须是合法 JSON，可被标准 JSON 解析器解析为对象，含 `summary` 与 `files`。
- 超大语料：record 粒度 junit-xml 在 record 数巨大时可能产生超大 XML；不设硬上限，取舍见开放问题 2。
- 写 stdout 失败（如下游管道关闭）：遵循现有 PrintStream 行为，不额外保证。

## 验收（Given-When-Then）

### P0

- **P0-1 默认零回归：** Given 任意既有 fixture；When 不带 `--format` 运行；Then stdout 与现状字节一致，退出码一致。
- **P0-2 `--format text` 等价默认：** Given 任意 fixture；When `--format text` 运行；Then stdout 与退出码与不带 `--format` 完全一致。
- **P0-3 junit-xml 合法可解析：** Given 含 pass/fail 的 fixture；When `--format junit-xml`；Then stdout 为 well-formed XML，可被标准 XML 解析器解析，根元素为 `<testsuites>`（多文件）或 `<testsuite>`（单文件）；退出码遵循 0/1/2 优先级。
- **P0-4 junit-xml failure 元素：** Given 失败 fixture；When `--format junit-xml`；Then 失败 record 对应的 `<testcase>` 含 `<failure>` 子元素；PASSED record 的 `<testcase>` 不含 `<failure>`/`<error>`。
- **P0-5 json 合法可解析：** Given 含 pass/fail 的 fixture；When `--format json`；Then stdout 为合法 JSON，可被标准 JSON 解析器解析为对象，含 `summary` 与 `files`；退出码遵循 0/1/2 优先级。
- **P0-6 json 结构化失败信息：** Given 失败 fixture；When `--format json`；Then 失败 record 条目含 outcome=`FAILED` 与 `reason` 字段，且含结构化 `expected`/`actual`（或 `diff`）。
- **P0-7 未知格式值 usage error：** Given 任意 fixture；When `--format bogus`；Then 退出码 2，stderr 含 `Error: usage` 与 `--format` 相关说明；stdout 不含 `TOTAL:`、`<testsuites>` 或 JSON。
- **P0-8 凭据脱敏（全格式）：** Given `--password secret` 且某失败/hardError 消息含密码或 URL user info；When 分别 `--format text|junit-xml|json`；Then 三种输出均不含明文 `secret` 与 URL user info。
- **P0-9 无 ANSI（机器可读）：** Given `--color always`；When 分别 `--format junit-xml|json`；Then 输出不含任何 ANSI 转义序列（`\u001B[`）。

### P1

- **P1-1 `--halt` 交互：** Given 多文件、首文件失败、`--halt`；When `--format junit-xml|json`；Then 报告仅含已执行文件（含失败的首文件），未执行文件不出现；退出码遵循 0/1/2。
- **P1-2 `--override` 交互：** Given golden-update 场景、`--override`；When `--format junit-xml|json`；Then OVERRIDDEN 记录在报告中体现（非 FAILED）：json 以结构化字段标注，junit-xml 不计入 `<failure>`；退出码不变。
- **P1-3 testcase 粒度为 record：** Given 含多条 record 的 fixture；When `--format junit-xml`；Then 每条 assertable record 对应一个 `<testcase>`，含 `classname`（文件名）、`name`（含 record kind + 行号）、`time`（秒，3 位小数）。
- **P1-4 多文件 `<testsuites>` 聚合：** Given 多文件；When `--format junit-xml`；Then 根元素为 `<testsuites>`，含多个 `<testsuite>`（每文件一个），各 suite 的 `tests`/`failures`/`skipped` 计数正确。
- **P1-5 json 多文件聚合：** Given 多文件；When `--format json`；Then 顶层 `files` 数组含每个文件对象，`summary` 汇总所有文件计数。
- **P1-6 文件级耗时：** Given 任意 fixture；When `--format junit-xml|json`；Then 每个 suite/file 含耗时字段（秒，3 位小数），值为非负数。

## 决策记录（2026-08-11 用户已确认）

下列各项由用户会话于 2026-08-11 全部确认采纳推荐方案，构成不可变更合同；实现须严格遵循。

1. **JUnit XML schema：** 遵循 surefire-style XSD（`<testsuites>`/`<testsuite>`/`<testcase>`/`<failure>`/`<system-out>`），最大化 CI 工具兼容（Jenkins JUnit plugin、`mikepenz/action-junit-report`、CircleCI）。
2. **testcase 粒度：record 级。** 每条 assertable record（statement/query）对应一个 `<testcase>`。隐含需新增 record 级计时埋点（当前代码仅有文件级计时，见 `CliSession.execute()` 局部变量 `CliSession.java:68,70`）——属 Design/Plan 范围。
3. **JSON 形状：自定义结构化**（顶层 `summary` + `files[]`，每条 record 含 outcome/位置/耗时/结构化 expected/actual/diff）。示例见「数据/状态」。
4. **输出目标：仅 stdout。** `--format` 只控格式，CI 通过 shell 重定向写文件；本切片不引入 `--out`/`-o`。
5. **默认行为字节一致：** 不带 `--format` 与 `--format text` 完全等价于现状（零回归）。
6. **多文件聚合：** junit-xml 用 `<testsuites>`，json 用顶层 `files` + `summary`。
7. **凭据脱敏覆盖全格式：** 与 human-readable 一致。
8. **`--halt` / `--override` 交互：** halt 跳过文件不出现；override 仍输出报告且 OVERRIDDEN 正确体现（非 FAILED）。
9. **耗时精度：** 秒，3 位小数。
10. **错误信息结构化：** json 提供结构化 expected/actual/diff；junit-xml 用 `<failure message>` + `<system-out>` diff 文本。
