# Review: feat-statement-error-msg

- 审阅日期: 2026-08-10
- 审阅人: Reviewer (subagent)
- 路径等级: standard
- 依据: spec.md / plan.md

## 结论

**Request changes**

存在 1 项 major 问题（Plan 声明的用户文档影响未落实）与 2 项 minor 观察。代码逻辑与测试本身正确、全量回归通过；但 declared 交付物（README.md）缺失，须 Developer 补齐后复审。

## 验证结果

| 命令 | 结果 |
|---|---|
| `mvn test -pl . -Dtest=SqlLogicTestParserTest` | BUILD SUCCESS — Tests run: 38, Failures: 0, Errors: 0, Skipped: 0 |
| `mvn test -pl . -Dtest=SqlLogicTestRunnerTest` | BUILD SUCCESS — Tests run: 34, Failures: 0, Errors: 0, Skipped: 0 |
| `mvn test -pl . -Dtest=RunnerAcceptanceTest` | BUILD SUCCESS — Tests run: 11, Failures: 0, Errors: 0, Skipped: 0 |
| `mvn test -pl .`（全量回归） | BUILD SUCCESS — Tests run: 262, Failures: 0, Errors: 0, Skipped: 16（PG 门控） |

测试确实断言行为（非仅编译通过）：parser 测试断言 `expectedErrorMsg` 精确值；runner 测试断言 outcome 与 failureReason 文案；acceptance 测试断言每条记录的 outcome 序列、计数与失败原因。

## 审阅明细

### 1. 行为合同符合性
- Spec「范围与可见行为」表 5 行均被实现/测试覆盖：
  - 无消息向后兼容 → `SqlLogicTestRunner.java:174`（`expectedMsg != null && !expectedMsg.isEmpty()` 判空跳过匹配）；测试 `statementErrorWithoutMessageBackwardCompatible_pass`。
  - 消息匹配通过 → `SqlLogicTestRunner.java:176`（contains）；测试 `statementErrorWithMatchingMessagePasses`、`statementErrorWithMessageContainsExpectedInErrorSummary`。
  - 消息不匹配失败 → `SqlLogicTestRunner.java:177-180`；测试 `statementErrorWithNonMatchingMessageFails`。
  - 有消息但执行成功 → `SqlLogicTestRunner.java:170-171`（先判 succeeded）；测试 `statementErrorWithMessageButExecutionSucceeds`。
  - 消息含 `#` 不剥离 → parser 仅对 `skipif/onlyif` 调 `stripTrailingHashComment`（`SqlLogicTestParser.java:96-98`），`statement` 分支不剥离；测试 `statementErrorWithHashInMessage`。
- P0 (1–4) 与 P1 (5–7) 验收点均有对应测试。OK 分支忽略 `expectedErrorMsg`（防御性，`SqlLogicTestRunner.java:165-168` 未读取该字段）。

### 2. 解析正确性
- `SqlLogicTestParser.java:152-162`：ERROR 且 `tokens.length > 2` 时，用 `indexOfToken(trimmedHeader,"error",0)` 定位 keyword 结束位置，取后续子串并 `trim()`。该方式正确保留消息内部空白与 `#`（`#` 不被当注释）。
- 空/仅空格消息处理：`SqlLogicTestParser.java:157-160`，`raw` 经 `trim()` 为空时 `expectedErrorMsg` 保持 `null`，符合 spec「视为无消息」。
- 边界：`indexOfToken` 找到的是首个 `error` token（keyword 本身），消息内即使再次出现 `error` 子串也不影响（先匹配到 keyword）。逻辑正确。
- `statement ok` 多 token 现显式报错 `statement ok does not take additional operands`（`SqlLogicTestParser.java:141-145`），有测试覆盖。

### 3. runner 匹配逻辑
- case-insensitive：`actual.toLowerCase(Locale.ROOT).contains(expectedMsg.toLowerCase(Locale.ROOT))`（`SqlLogicTestRunner.java:176`），测试 `statementErrorMessageMatchingIsCaseInsensitive` 覆盖。
- 包含语义：使用 `String.contains`，无正则/注入面。
- errorSummary 为 null/空：`actual = result.errorSummary() == null ? "" : ...`（`SqlLogicTestRunner.java:175`），空串不包含非空预期 → FAILED，符合 spec。
- OK 分支不读取 `expectedErrorMsg`（`SqlLogicTestRunner.java:165-168`），向后兼容。

### 4. 向后兼容
- `statement error`（无消息）：`expectedErrorMsg=null` → runner 跳过匹配，仅验证失败。现有 fixture（`p0-3-statement-assertions.test` 等）仍通过。
- `statement ok` 行为不变。

### 5. 测试覆盖
- T5 parser 测试齐备：单 token / 多 token / 含 `#` / 无消息（assertNull）/ `statement ok` 拒绝多 token。见 `SqlLogicTestParserTest.java:575-648`。
- T6 runner 测试齐备：匹配通过 / 不匹配失败 / 大小写不敏感 / 有消息但成功 / 无消息向后兼容 / 长消息包含。见 `SqlLogicTestRunnerTest.java:464-535`。
- T7 fixture `p0-4-statement-error-message.test` 含三场景（无消息 / 匹配 / 不匹配 / 成功误判），`RunnerAcceptanceTest.java:147-167` 逐条断言 outcome 序列、failedCount/passedCount 与 failureReason。

### 6. 代码质量
- 命名清晰（`expectedErrorMsg`、`indexOfToken`、`keywordEnd`），与既有风格一致。
- `indexOfToken(line, token, n)` 的 `n` 参数实际仅以 `0` 调用，略有余量但无害。
- `indexOfToken` 上的 Javadoc（`SqlLogicTestParser.java:432-436`）与本文件既有的 helper 文档风格一致（遵循文件约定，可接受）。

### 7. 安全
- 无敏感信息；消息仅用于 `String.contains` 比较，无正则/命令注入面；`toLowerCase(Locale.ROOT)` 与既有用法一致。检查范围：parser 消息提取、runner 匹配。未触发安全审阅条件。

### 8. 文档影响
- **问题**：Plan「文档影响」声明 用户文档 = `README.md` 更新 `statement error` 说明（CLI 用户可见行为变更），但 `git diff -- README.md` 为空，README 未被修改。README「Expected results」节未提及新的 `statement error <message>` 语法。属 declared 交付物缺失。

### 9. Git 合规
- 实现当前位于 `main` 且全部未提交（`git status` 显示工作区改动）。按 `workflow/docs/standards/git.md` §1.1，实现须位于切片源分支（推荐 `feat-statement-error-msg`），禁止在 `main` 直接实施。此项属 Manager/合并阶段处理范畴，不影响代码审阅结论，见「备注」。

## 问题清单（如有）

| # | 严重程度(blocking/major/minor) | 描述 | 位置 | 建议 |
|---|---|---|---|---|
| 1 | major | Plan 声明的用户文档影响未落实：`README.md` 未更新 `statement error <message>` 可选消息语法说明（用户可见行为变更无文档） | `README.md`（未改动） | 在 README 适当章节（如「Expected results」或新增小节）补充 `statement error <message>` 语法、case-insensitive sub-string 匹配语义与失败原因示例 |
| 2 | minor | 错误消息文案偏离 spec：spec「错误与约束」要求 tokens 不足时保持现有错误 `statement requires exactly one expectation token (ok\|error)`，实现改为 `statement requires at least one expectation token (ok\|error)` | `src/main/java/com/ggtest/parser/SqlLogicTestParser.java:137` | 二选一：恢复原文案以贴合 spec；或更新 spec 该行文案以反映新语义（推荐后者，因新文案对 ERROR 多 token 更准确）。无测试依赖该文案 |
| 3 | minor | 失败原因格式偏离 spec 描述：spec 表述为 `statement error message mismatch: expected ... to contain ...`，实现采用 diff 风格 `statement error message mismatch\n-   <exp>\n+   <actual>` | `src/main/java/com/ggtest/runner/SqlLogicTestRunner.java:177-180` | 验收 P0-3 仅要求「含 `statement error message mismatch`」，已满足；实现风格与 query mismatch 报告一致且更可读。建议反向更新 spec/plan 文案以对齐实现，避免合同与实现长期不一致 |

## 备注

- **Git 分支**：当前在 `main`、改动未提交、未检出切片源分支。按 git.md §1.1，Developer 实施前须先创建并检出独立工作分支（推荐 `feat-statement-error-msg`），全部实现/文档应提交到该源分支。属 Manager 在调度/合并阶段处置，不构成代码审阅的阻塞项。
- **进入 QA 条件**：standard 路径进 QA 前须 Approve。当前因存在 1 项 major（README 缺失），结论为 Request changes，**暂不满足**进入 QA 条件。待 Developer 补齐 README（并处理 minor 项或对齐 spec 文案）后复审，复审 Approve 方可进 QA。
- 工作流产物（spec/plan/manager 记录/STATUS）结构与目录命名符合 workflow/README.md 规范。

## R2 复审（2026-08-10）

- 审阅类型：R1 `Request changes` 后的修复复审
- 审阅人：Reviewer (subagent)
- 当前分支：`feat-statement-error-msg`（R1 Git 合规问题已解决）

### 结论

**Approve**

R1 三项遗留全部解决；README 描述与实现一致；全量回归通过；无新增阻塞项。

### R1 遗留解决状态

| # | R1 项 | 状态 | 核对依据 |
|---|---|---|---|
| 1 | major：README 未更新 `statement error <message>` 说明 | **已解决** | `README.md` 新增 `### Statement expectations` 小节（+27 行，位于「Expected results」与「Exit codes」之间），覆盖语法、case-insensitive 子串包含语义、`#` 不剥离、4 种 outcome 表格；`README.zh-CN.md` 同步新增 `### 语句断言`（+21 行），与英文版一致 |
| 2 | minor：tokens 不足文案 `at least one` vs spec `exactly one` | **已解决** | `spec.md:61` 已更新为 `statement requires at least one expectation token (ok\|error)`，与 `SqlLogicTestParser.java:137` 实现完全一致 |
| 3 | minor：失败原因 diff 风格 vs spec 表述 | **已解决** | `spec.md:28` 已改为多行 diff 风格描述（首行 `statement error message mismatch`，随后 `-   <expectedMsg>`、`+   <actual errorSummary>`），与 `SqlLogicTestRunner.java:177-180` 输出一致；验收 P0-3（`spec.md:82`）未被改动，仍为「含 `statement error message mismatch`」，与 diff 风格兼容 |

### README 与实现一致性核对

- 语法 `statement error <message>`：与 `SqlLogicTestParser.java:152-162`（ERROR 且 `tokens.length > 2` 时取 `error` 后续子串）一致。
- case-insensitive 子串包含：README 表述与 `SqlLogicTestRunner.java:176`（`toLowerCase(Locale.ROOT).contains(...)`）一致。
- `#` 不剥离：README 表述与 parser 实现（statement 分支不调 `stripTrailingHashComment`）一致。
- 空消息处理：README「empty or whitespace-only 视为无消息」与 `SqlLogicTestParser.java:157-160`（`raw.trim()` 为空 → `null`）一致。
- 失败原因文案：README 表格中 `statement expected to fail but succeeded` / `statement error message mismatch`（diff 风格）分别与 `SqlLogicTestRunner.java:171` / `177-180` 逐字一致。
- 中英文 README 行为描述一致，无内部矛盾。

### 本轮验证结果

| 命令 | 结果 |
|---|---|
| `mvn test -pl .`（全量回归） | BUILD SUCCESS — Tests run: 262, Failures: 0, Errors: 0, Skipped: 16（PG 门控） |

与 R1 一致，无回归。

### 本轮新问题

无。未发现 README↔实现不符或 spec 内部矛盾。

### 进入 QA 条件

**满足**。路径等级 standard，本轮结论为 **Approve**，可进入 QA。
