# Review: ggtest-rowwise-expected

## 审阅范围

| 项 | 内容 |
|---|---|
| 工作项 | `ggtest-rowwise-expected`（未拆分） |
| Review 门禁 | **required**（full）；**废止 R3 后复审**（此前含 R3 的 Approve **作废**） |
| 依据 | 冻结 [`spec.md`](./spec.md)（保留 R1/R2，**已废止 R3**）；[`design.md`](./design.md) D1–D4；[`plan.md`](./plan.md) T1–T5；[`dev-notes.md`](./dev-notes.md)；`workflow/docs/standards/{documentation,quality,security,git}.md` |
| 实现版本 | 分支 `ggtest-rowwise-expected`；相对 `HEAD`=`95173b0` 的**工作区未 commit** 变更 |
| 审阅重点 | 无引号层；显式仅 split+trim；目标书写无引号壳；R1/R2；`mvn -q clean test`；禁 demo2/`.env` |
| 未纳入 | 不改实现/测试/Spec/Plan/manager；不作 QA 结论；**不 commit**；无关未跟踪 `architecture-overview` |

## 结论

**Approve** — 无阻塞项。废止 R3 后：显式仅 `splitLiteral`+`strip`；源码无 `splitLiteralRespectingQuotes`/`unquote`；P0-1…P0-9 / P1-4 可证伪；R1/R2 正确。审阅者 `mvn -q clean test` → Tests run: **196**，Failures: **0**，Errors: **0**，Skipped: **17**（BUILD SUCCESS）。可调度 QA（旧 QA Pass 作废，须重跑）。

## 必修项

无。

> `Comment` 不得包含阻塞项；阻塞问题须使用 `Request changes`。本结论为 Approve，无阻塞项。

## 核对清单（要求 → 证据 → 结果）

| 要求 | 证据 | 结果 |
|---|---|---|
| **已废止 R3**：无 `splitLiteralRespectingQuotes` / `unquote` | `src/**/*.java` 无该方法名；`tokenize` 显式仅 `splitLiteral`→`strip` | 通过 |
| 目标书写无引号壳（P0-2） | comparer/parser `p0_2_*`；`rowwise-pipe-separator.test`：`1 \| 1 \| hello world` | 通过 |
| P1-4：期望侧 `'hello world'` 计入原文，不因去引号通过 | `p1_4_*`：expectedView 含 `'`，比对失败 | 通过 |
| R1：本条绑定；下一条恰 `----` 不继承 | `parseQuery`；`QueryRecord`；parser/runner `p0_3_*`；`rowwise-mixed.test` | 通过 |
| R1：无 `SeparatorRecord` / `FileState.columnSeparator`；顶层非法 | 类型已删；permits 同步；`FileState` 无列分隔符；`topLevelSeparatorDirective_*` | 通过 |
| R2：显式才 trim；默认连续空格仍空 token | `tokenize`；`p0_1_consecutiveSpaces*`；`p0_4_*` | 通过 |
| P0-5：含 `S` 须换分隔符或每值一行 | `p0_5_cellContainingSeparator_*` | 通过 |
| 空 delim / 非法 `----…` / `seperator` / 裸 / 三短横 | parser 负例；`src/main` 无 `---separator` 正例 | 通过 |
| 哈希/Diff/每值一行；不改 I/T/R、MD5 | `p0_6`/`p0_7`/`p0_8`；`ValueNormalizer`/`ResultHasher` 无本项 diff | 通过 |
| fixtures；不依赖 demo2/`.env` | 三件 `rowwise-*.test`；无文件顶全局 separator | 通过 |
| README：R1/R2、无引号壳 | 「Expected results」目标书写与 trim 说明 | 通过 |

## 实现正确性

1. **D1**：期望头分流；delim 不用 `splitTokens` 整取。
2. **D2**：删除 `SeparatorRecord`；顶层 `----` 族可读失败。
3. **D3/D4（废止 R3）**：显式仅 split→trim（原文即单元格）；无去引号/`''`/未闭合分支；默认空格旧规则；`compare(..., S, explicit, ...)`。
4. **Runner**：本条 `S`/`explicit`；无文件级覆盖。

合同偏差：无。

## 测试有效性

| 层 | 覆盖 | 可证伪 |
|---|---|---|
| parser | 期望头 `|`；作用域；顶层非法；空 delim；拼写/三短横/裸；无引号目标书写 | 误绑文件级→失败 |
| normalize | P0-1/2/4/5/6/7/8；P1-4；连续空格；混用 | 残留去引号或默认误 trim→失败 |
| runner+fixture | P0-3；三件 e2e（裸文本 + 哈希） | 文件级或引号壳 fixture→失败 |

复验命令与计数见结论。缺口（不阻塞）：无专用行式 `valuesort` 单测（P0-8「及按需」）。

## 文档影响核对

| Plan 声明 | 一致？ | 备注 |
|---|---|---|
| 开发：README、Javadoc、`dev-notes.md` | 是 | 删引号回改与 P0 映射齐全 |
| 用户：README 期望书写 | 是 | R1/R2、无引号壳 |
| 运维：N/A | 是 | 无运维面 |
| 可选 architecture-overview | N/A | Plan 非必做 |

既有 [`qa-report.md`](./qa-report.md) 仍描述含 R3 合同，**不作**本轮依据；QA 须按现行 Spec 重写。

## 安全影响核对

| 检查项 | 结果 | 备注 |
|---|---|---|
| 敏感信息 | 通过 | 无密钥；`.env` 已 ignore 且未跟踪 |
| 认证与授权 | N/A | 未触 |
| 输入与外部访问 | 通过 | 仅本地语料；无新注入/出站/路径穿越面 |
| 依赖变更 | 通过 | 无 pom 依赖变更 |
| 敏感数据 | N/A | 无 |

处置：无安全发现；允许进 QA。合入**禁止**入库 `examples/demo2.slt`、`.env`。

## Git 合规

| 项 | 结果 |
|---|---|
| 源分支 | `ggtest-rowwise-expected` |
| 提交 | 实现未 commit；本报告 **不**由 Reviewer 提交 |
| 禁止项 | 无 `SeparatorRecord`/引号 API/`---separator` 正例 |
| 工作区风险 | 未跟踪 `examples/demo2.slt`（**未** ignore）；本地 `.env`（已 ignore）。提交须排除二者 |

## 非阻塞建议

| ID | 严重度 | 位置 | 说明 |
|---|---|---|---|
| N1 | low | `SqlLogicTestRunner#runQuery` | 展开异常仍前缀「type signature」 |
| N2 | low | `ResultComparerTest` | 可补行式 `valuesort` |
| N3 | low | `examples/demo2.slt` | 未 ignore；合入前勿 `git add` |

## 后续动作与复审范围

1. Manager → 调度 **QA**（现行 Spec P0-1…P0-9 + P1-4 + Plan L3）。
2. QA/合入排除：`demo2.slt`、`.env`、无关 `architecture-overview`。
3. 报告留工作区；`git.md` §1.4：进 QA **不**单独提交本报告。
4. 无实现复审要求。
