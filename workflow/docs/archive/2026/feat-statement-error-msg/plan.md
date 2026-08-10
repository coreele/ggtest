# Plan: feat-statement-error-msg

## 元信息

- 工作项标识: feat-statement-error-msg
- sub-feature-id: feat-statement-error-msg（未拆分）
- 依据 Spec: workflow/docs/features/feat-statement-error-msg/spec.md
- 依据 Design: N/A（Design 门禁 skipped；无模块边界/分层/技术选型决策）
- 依据 UI: N/A（纯逻辑变更，无用户可见界面）
- 路径等级: standard
- Review 门禁: required
- 最低验证层: L2（单元测试覆盖 parser、runner；runner 测试用 FakeDatabaseExecutor 模拟 JDBC 行为）
- 验证命令: `mvn test -pl .`
- 预期证据: 全量测试通过（BUILD SUCCESS），parser 测试覆盖 message token 解析，runner 测试覆盖消息匹配/不匹配/向后兼容

## 适用工程规范

- `workflow/docs/standards/documentation.md`
- `workflow/docs/standards/git.md`（仅 Git 工作区）
- `workflow/docs/standards/quality.md`
- `workflow/docs/standards/security.md`

## 目标摘要

`statement error` 支持可选错误消息匹配：解析 `statement error` header token 后的可选文本作为 `expectedErrorMsg`，执行失败后验证数据库返回的 errorSummary 包含该文本（case-insensitive sub-string）。

## 任务拆解

### T1: 更新 `StatementExpectation` 和 `StatementResult` Javadoc
- 完成条件：
  - `StatementExpectation.java` 移除 `"does not carry error-message matching (out of scope for the first iteration)"`，改为说明 `statement error` 可携带可选消息
  - `StatementResult.java` 移除 `"it must not be matched against expected error messages"` 注释

### T2: 更新 `StatementRecord` model — 新增 `expectedErrorMsg` 字段
- 完成条件：
  - `StatementRecord` 新增 `Optional<String> expectedErrorMsg` 字段（nullable, 默认 null 表示无消息）
  - 更新所有现有构造调用点：parser 中 `StatementRecord(sql, expectation, location)` → 保持兼容（expectedErrorMsg 可接受 null）
  - 所有测试中 `new StatementRecord(…)` 调用点编译通过

### T3: 更新 `SqlLogicTestParser.parseStatement` — 解析可选错误消息
- 完成条件：
  - `statement error` 后 tokens 数量 ≥ 3 时，提取 `tokens[2..]` 为 expectedErrorMsg，传递给 `StatementRecord`
  - `statement error` 仅 2 tokens 时，expectedErrorMsg = null（向后兼容）
  - `statement ok` 后 tokens 数量 ≥ 3 视为错误（与当前一致：`statement requires exactly one expectation token (ok|error)`）
  - parser 中 `#` 在 statement error header 中不被当注释剥离（消息自身可含 `#`）

### T4: 更新 `SqlLogicTestRunner.runStatement` — ERROR 分支消息匹配
- 完成条件：
  - ERROR + expectedErrorMsg 非空：执行失败后，检查 errorSummary（case-insensitive）是否包含 expectedErrorMsg
    - 包含 → PASSED
    - 不包含 → FAILED，failureReason: `"statement error message mismatch: expected ... to contain ..."`
  - ERROR + expectedErrorMsg 为空/null：行为不变（仅验证失败）
  - OK + expectedErrorMsg 非空/null：忽略 expectedErrorMsg（不应出现在合法输入中，但防御性忽略）

### T5: 新增 parser 测试
- 完成条件：
  - `SqlLogicTestParserTest` 新增：
    - `statement error` 带单 token 消息 → expectedErrorMsg 正确
    - `statement error` 带多 token 消息（含空格）→ expectedErrorMsg 正确
    - `statement error` 消息含 `#` → 不被剥离
    - `statement error` 无消息 → expectedErrorMsg 为空（向后兼容）

### T6: 新增 runner 测试
- 完成条件：
  - `SqlLogicTestRunnerTest` 新增：
    - `statement error` 有消息、执行失败且 errorSummary 包含消息 → PASSED
    - `statement error` 有消息、执行失败但 errorSummary 不包含消息 → FAILED（message mismatch）
    - `statement error` 有消息、执行成功 → FAILED（expected to fail but succeeded）
    - `statement error` 无消息、执行失败 → PASSED（向后兼容，与现有测试一致）
    - Case-insensitive 匹配验证

### T7: 新增 test resource fixture
- 完成条件：
  - `src/test/resources/fixtures/runner/p0-4-statement-error-message.test`：包含 statement error 带消息且匹配、不匹配、无消息三场景
  - `RunnerAcceptanceTest` 新增测试方法验证该 fixture 的行为

### T8: 运行全量测试确认无回归
- 完成条件：`mvn test -pl .` → BUILD SUCCESS，所有现有测试通过

## 依赖与顺序

```
T1 ──┬── T2 ──┬── T3 ── T5 (parser tests)
     │        │
     │        └── T4 ── T6 (runner tests) ── T7 (fixture + acceptance)
     │
     └── T8 (全量回归 ── 实施后可随时执行)
```

T1 无依赖；T2 依赖 T1（一致性与 Javadoc）；T3/T4 依赖 T2（model 变更完成）；T5 依赖 T3；T6 依赖 T4；T7 依赖 T6；T8 依赖全部。

## 触碰路径

| 任务 | 文件 |
|---|---|
| T1 | `src/main/java/com/ggtest/model/StatementExpectation.java`, `src/main/java/com/ggtest/db/StatementResult.java` |
| T2 | `src/main/java/com/ggtest/model/StatementRecord.java`, `src/main/java/com/ggtest/parser/SqlLogicTestParser.java`（构造点） |
| T3 | `src/main/java/com/ggtest/parser/SqlLogicTestParser.java` |
| T4 | `src/main/java/com/ggtest/runner/SqlLogicTestRunner.java` |
| T5 | `src/test/java/com/ggtest/parser/SqlLogicTestParserTest.java` |
| T6 | `src/test/java/com/ggtest/runner/SqlLogicTestRunnerTest.java` |
| T7 | `src/test/resources/fixtures/runner/p0-4-statement-error-message.test`, `src/test/java/com/ggtest/runner/RunnerAcceptanceTest.java` |

## 验收与验证

| ID | 要求或命令 | 预期证据 |
|---|---|---|
| V1 | `mvn test -pl . -Dtest=SqlLogicTestParserTest` | 全通过；含 statement error message 解析用例 |
| V2 | `mvn test -pl . -Dtest=SqlLogicTestRunnerTest` | 全通过；含消息匹配/不匹配/向后兼容用例 |
| V3 | `mvn test -pl . -Dtest=RunnerAcceptanceTest` | 全通过；p0-4 新 fixture 验证 |
| V4 | `mvn test -pl .` | BUILD SUCCESS；全量回归通过 |

## 验证缺口

| 项 | 原因 | 风险 | 恢复条件 |
|---|---|---|---|
| PostgreSQL 真实 JDBC errorSummary 格式验证 | 本功能仅做 sub-string 包含匹配，不依赖特定 DB 错误格式；runner 层用 FakeDatabaseExecutor 完整覆盖匹配逻辑 | 低 — 匹配逻辑不依赖真实 DB | N/A |
| 不同 JDBC 驱动 / 语言的错误消息编码 | case-insensitive ASCII sub-string 跨越多数常见格式；Unicode 消息远超出 scope | 低 — 官方 sqllogictest 测试脚本使用 ASCII | N/A |

## 文档影响

| 类别 | 更新路径或 N/A 理由 |
|---|---|
| 开发文档 | N/A — Javadoc 更新已在 T1/T2 中覆盖 |
| 用户文档 | `README.md` 更新 `statement error` 说明（CLI 用户可见行为变更） |
| 运维文档 | N/A — 非运维相关变更 |

## 交接顺序

1. Developer 实施与开发者验证 →
2. Reviewer（Review 门禁 required）→
3. QA 验收 →
4. 用户合并授权 → Manager `done` 一次提交 → 合入

## 修订记录

| 日期 | 摘要 |
|---|---|
| 2026-08-10 | 初始 Plan |
