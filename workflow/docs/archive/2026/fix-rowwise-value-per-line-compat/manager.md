# 工作项记录: fix-rowwise-value-per-line-compat

工作项标识: fix-rowwise-value-per-line-compat
描述: 合同变更 — 移除行式期望推断（纯 `----` 永远每值一行，根修 select4 位点）；行式改为 query 头显式声明 `separator <delim>`；移除 `---- separator` 期望头语法。有意取代归档 `ggtest-rowwise-expected` 合同。
路径等级: standard
源分支: fix-rowwise-value-per-line-compat
目标分支: main
文档影响: workflow/workflow/docs/features/fix-rowwise-value-per-line-compat/；README.md / README.zh-CN.md（必要时）；**本轮用户改口**：入库/更新 `examples/demo.slt`、新增 `examples/demo_zh.slt`（公开功能 showcase）；`select*.test` 大语料仍不强制入库（本地保留勿删）；`demo2.slt` 由 Developer 决定并入 demo 或保留

> 权威工作流、门禁与状态说明见 [workflow/README.md](../../../README.md)。
> 活跃状态见 [STATUS.md](../../manager/STATUS.md)。
>
> 文档路径：未拆分时 Spec 为 `workflow/workflow/docs/features/<feature-id>/spec.md`（无子目录）。

## 切片（未拆分时仅一行，sub-feature-id = feature-id）

| sub-feature-id | Spec | Spec 门禁 | Spec 用户确认 | Design 门禁 | Review 门禁 | 状态 | 后续步骤 |
|---|---|---|---|---|---|---|---|
| fix-rowwise-value-per-line-compat | [spec.md](./spec.md) | required（新增 query 头 `separator` 语法 + 移除既有语法 + 错误约定；有意取代归档合同） | not-required（合同由用户 2026-07-26 逐条拍板口述；OQ-1 随 Plan 确认已拍板） | skipped（parser/model/normalize 既有分层内实现；无模块边界/选型决策） | required（standard；语料兼容 + 语法变更；合入前 demo/README 补充按文档/示例 fast 轻量确认，原 Approve 仍有效） | done | 工作流已关闭（QA Pass×2 + merge-auth）；**已合入 main**（证据：`8a0c236`、`aa90c72`；后续 `9eaa742`/`a92b7a1` 清理 demo2） |

阻塞原因: none（先前「禁止 commit / 未合入」叙述已过时；git 事实显示已在 main）
恢复条件: N/A
恢复后的目标状态: N/A

## Manager 门禁判定（2026-07-26 第二次；取代首次判定）

- **路径**：fast → **standard** — 用户拍板由小修（混形回退）升级为合同变更：新增公开语法、移除既有语法、有意取代归档合同。
- **Spec**：skipped → **required** — 新增行为（query 头 `separator <delim>`）、公开接口（`.test` 文法）、错误约定（token 数 ≠ C、delim 后多余 token、`---- separator` 移除后报错）均变。归档 [`ggtest-rowwise-expected`](../ggtest-rowwise-expected/spec.md) 合同被**有意取代**（默认空格行式 `1 2 3` 不再支持）。
- **Spec 用户确认**：not-required — 合同由用户在当前会话逐条拍板（见「用户决策」节），Spec 为转写；OQ-1 随 Plan 确认拍板。
- **Design**：skipped — 触及 `SqlLogicTestParser` / `QueryRecord` / `ExpectedResultExpander` / `ResultComparer`，但均在既有 parser/model/normalize 分层内，无边界或选型决策。
- **Review**：required — standard 必须。
- **分支**：源 `fix-rowwise-value-per-line-compat` → 目标 `main`。

## 用户决策（2026-07-26，取代原 Plan 方案）

1. 纯 `----` 永远每值一行；彻底移除空格猜行式推断与 `mixed expected line shapes` 抛错。
2. 行式改为 query 头显式声明：`query <类型串> [nosort|rowsort|valuesort] [label] [separator <delim>]`；声明后每行严格恰 C 个 token（否则可读失败）、token trim、空 token → `(empty)`。
3. 消歧：`separator` 后跟一个 token → 分隔符声明；`separator` 为行尾最后 token → 仍按 label（向后兼容；官方语料 8884 条 query 头零冲突）。delim 单 token 不含空白；delim 后再有 token → 解析错误。
4. 移除 `---- separator <delim>` 期望头语法。
5. 哈希单行期望 `N values hashing to …` 优先识别，不受影响。

**破坏面**：归档 `ggtest-rowwise-expected` 合同被取代；fixtures `rowwise-default-space.test` / `rowwise-mixed.test` / `rowwise-pipe-separator.test`、`ResultComparerTest`、parser 单测须迁移；README.md / README.zh-CN.md「Expected results / 期望结果」小节改新语法；用户本地 `examples/`（demo.slt 有默认空格行式、demo2.slt 有 `---- separator |`）**不入库**，在 dev-notes 提示用户自行更新。

## 失败证据（登记时）

- 命令：`./bin/ggtest --engine sqlite --url 'jdbc:sqlite::memory:' examples/select4.test`
- 位点：`examples/select4.test` ~47398（`query ITII` / `rowsort`）；期望含 TEXT `table tn7 row 92`
- 错误：`mixed expected line shapes for 4 column(s); ... line 1 has 1 token(s); line 2 has 4 token(s); ...`

## Plan 确认

- 原 Plan（混形回退小修）：**superseded** — 用户未确认，2026-07-26 拍板新合同取代。
- 新 Plan：**approved**（2026-07-26）— 用户回复「ok」确认 `spec.md` 与 `plan.md`；四项拍板全部按默认批准：
  1. **OQ-1 = 移除** `---- separator <delim>` 期望头；遇之可读解析错误。
  2. token 数 ≠ C → **比对失败**（记录失败、运行继续、摘要含行号/token 数/C），**不**解析中止。
  3. `QueryRecord.columnSeparator` → `Optional<String>`（仅显式声明才有值）；删除 `ResultComparer.DEFAULT_COLUMN_SEPARATOR` 公开常量。
  4. `rowwise-default-space.test` 迁移/更名为「含空格 TEXT 按每值一行」守护 fixture。

## 进度笔记

- 2026-07-26：用户 `/manager` +「解决一下」；登记；Spec/Design skipped；Review required；状态 `planning`；调度 Planner。
- 2026-07-26：Plan 已写 → `awaiting-plan-approval`。
- 2026-07-26：用户拍板新合同（显式 `separator` 声明；移除推断与 `---- separator`）；路径 fast→standard；Spec 门禁 skipped→required；状态回 `speccing`；调度 Analyst。
- 2026-07-26：Analyst 产出 spec.md（OQ-1：`---- separator` 移除随 Plan 拍板）→ `planning`；调度 Planner 重写 plan.md。
- 2026-07-26：Planner 重写 plan.md（覆盖旧小修方案）→ `awaiting-plan-approval`。
- 2026-07-26：用户确认 Plan（「ok」）+ 四项默认批准 → `planned`；调度 Developer。
- 2026-07-26：进入 `developing`；调度 Developer。
- 2026-07-26：Developer 完成（分支 `fix-rowwise-value-per-line-compat`，提交 `8a0c236`；`mvn` 218/0/0；select4 sqlite `failed=0`）→ `reviewing`；调度 Reviewer。
- 2026-07-26：Reviewer **Approve**（`review.md` 未提交）→ `qa`；调度 QA。
- 2026-07-26：QA **Pass**（`qa-report.md` 未提交；`mvn` 218/0/0/18；select4 sqlite `failed=0`；`mixed…` 零命中）→ 停 **merge 授权门禁**。

## 合入授权

- **approved**（2026-07-26）：当前用户会话明确授权将 `fix-rowwise-value-per-line-compat` 合入 `main`。
- **提交纪律覆盖（历史）**：授权当时曾禁止即时 commit；后续已由用户/会话完成入库与合入（见下）。
- **合入结果（已核对 git，2026-07-26）**：**已合入 `main`**。证据：
  - `8a0c236` `fix(rowwise): value-per-line default; query-head separator`（实现）
  - `aa90c72` `docs(fix-rowwise-value-per-line-compat): demos, README, mark done`（demo/demo_zh、README、review/qa、STATUS/done）
  - 后续清理：`9eaa742` / `a92b7a1`（移除冗余 `demo2.slt`）
  - 当前分支 `main`；`git merge-base --is-ancestor` 确认上述 SHA 均在 `main` 上。

## 本轮追加范围（合入前文档/示例；2026-07-26）

用户指令（权威）：在既有工作项上补充后合入 main，**零 commit**。

1. **修订** `examples/demo.slt`：体现目前 ggtest **所有**公开功能（sqllogictest 记录类型与期望形态）。
2. **新增** `examples/demo_zh.slt`：与 demo.slt 同类型中文版（注释/说明中文；SQL 与断言行为对等；sqlite 可跑通）。
3. **修正** `README.md`：与当前 rowwise 合同一致；反映 demo / demo_zh（及必要时 demo2）；报告样例若引用 demo 文件名需同步；必要时一并改 `README.zh-CN.md`。
4. **文档影响改口**：原「examples 不入库」对本轮 **demo / demo_zh** 改为要入库；`select*.test` 仍不强制入库（本地勿删）。
5. 路径判定：QA 已 Pass；本轮为合入前示例/README 同步，**不改 Java** 时按 fast 补充（Review 轻量确认或记录原 Approve 仍有效 → QA 冒烟 demo）。

### demo.slt 验收清单（交 Developer）

- `statement ok` / `statement error`
- `query`：类型串 I/T/R；`nosort` / `rowsort` / `valuesort`；可选 label；可选 `separator <delim>`（行式）
- 纯 `----` = 永远 value-per-line；含空格 TEXT 不拆行
- query 头 `separator |`（及另一种 delim 如 `,`）行式
- NULL / `(empty)` 期望
- execute-only query（无 `----`）
- label 一致性
- `skipif` / `onlyif`（sqlite/postgres）
- `hash-threshold` + `N values hashing to <md5>`
- `halt`（之后记录不得执行）
- 文件头注释给出 sqlite / postgres 运行命令
- 对照 `demo2.slt`：并入主 showcase 或保留；目标「一个主 showcase 覆盖所有功能」；`demo_zh.slt` 对等

### README 修正要点

- Expected results / query-head `separator` 与本分支合同一致；勿写「空格猜行式」或 `---- separator`
- 运行示例指向 `examples/demo.slt` 与 `examples/demo_zh.slt`
- 报告样例 PASSED 列表可含 demo_zh

## 进度笔记（续）

- 2026-07-26：用户授权合并 + 禁止 commit；追加 demo/demo_zh/README 入库范围；状态 `qa` → `developing`；调度 Developer（零 commit）。
- 2026-07-26：Developer 完成合入前补充（demo 并入 demo2 独特用例并保留 demo2；新增 demo_zh；README 中英同步；sqlite `passed=2 failed=0`）→ `reviewing`；调度 Reviewer 轻量确认。
- 2026-07-26：Reviewer 合入前文档/示例补充 **Approve**（原 Approve 仍有效；已追加 review.md）→ `qa`；调度 QA 冒烟。
- 2026-07-26：QA 合入前补充冒烟 **Pass**（demo+demo_zh sqlite `passed=2 failed=0`；已追加 qa-report.md）→ 工作区置 `done`；当时因禁止 commit 暂未入库（历史阻断）。
- 2026-07-26：git 事实核对——实现与 workflow/demo 已在 `main`（`8a0c236`、`aa90c72` 及 demo2 清理提交）。Manager 修正 STATUS/工作项中「未合入 main」漂移；后续步骤改为已合入 main（未归档）。
