# 工作项: fix-rowwise-value-per-line-compat

描述: 合同变更 — 移除行式期望推断（纯 `----` 永远每值一行，根修 select4 位点）；行式改为 query 头显式声明 `separator <delim>`；移除 `---- separator` 期望头语法。有意取代归档 `ggtest-rowwise-expected` 合同。
目标分支: main
源分支: fix-rowwise-value-per-line-compat
基线提交: a6c8719bc48099cf772a6bd1807876dd4577259c
文档影响: workflow/archive/2026/fix-rowwise-value-per-line-compat/；README.md / README.zh-CN.md（必要时）；**本轮用户改口**：入库/更新 `examples/demo.slt`、新增 `examples/demo_zh.slt`（公开功能 showcase）；`select*.test` 大语料仍不强制入库（本地保留勿删）；`demo2.slt` 由 Developer 决定并入 demo 或保留

> **本文件须保存为 `workflow/archive/2026/fix-rowwise-value-per-line-compat/fix-rowwise-value-per-line-compat.md`**，文件名与目录同名。
> 流程定义见 `workflow/WORKFLOW.md`；看板见 `workflow/STATUS.md`。
> 本工作项的全部产物平铺在 `workflow/archive/2026/fix-rowwise-value-per-line-compat/`，无子目录、无版本后缀。
> 表内只填枚举、短标签或路径；理由与长说明写进「进度笔记」。

## 门禁

| 路径等级 | Spec | Spec 用户确认 | Design | Review |
|---|---|---|---|---|
| standard | skipped | not-required | skipped | required |

## 状态

| 状态 | 下一步 | 阻塞原因 | 恢复条件 | 恢复后目标 |
|---|---|---|---|---|
| archived | — |  |  |  |

## 子项（仅 tracking 项填写）

| 子项 id | 状态 |
|---|---|
| — | |

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
- 2026-08-14：按 ggnote `WORKFLOW.md` 标准迁移工作流目录（记录与产物合并为同一目录；权威文件改为 `workflow/WORKFLOW.md`）。
