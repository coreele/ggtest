# 工作项记录: chore-maven-compiler-release

工作项标识: chore-maven-compiler-release
描述: 在 pom.xml 显式声明 maven-compiler-plugin（建议 3.13.0），配置 `<release>${maven.compiler.release}</release>`（属性已为 17），使 Maven 3.6.3 默认 toolchain 不再回退到无效的 Source/Target 5，字面 `mvn -q clean test` / `package` 可成功。
路径等级: fast
源分支: chore-maven-compiler-release
目标分支: main
文档影响: `workflow/docs/features/chore-maven-compiler-release/`（plan/dev-notes/qa-report；无 Spec/Design/Review）

> 权威工作流、门禁与状态说明见 [workflow/README.md](../../README.md)。
> 活跃状态见 [STATUS.md](STATUS.md)。
>
> 文档路径：未拆分时 Spec 为 `workflow/docs/features/<feature-id>/spec.md`（无子目录）；已拆分时根目录仅总览 Spec，各切片为 `workflow/docs/features/<feature-id>/<feature-id>-<sub>/spec.md`。
> 归档后本文件迁至 `workflow/docs/archive/YYYY/<feature-id>/manager.md`，相对链接须同步修正；`workflow/docs/manager/` 仅保留活跃工作项记录与 `STATUS.md`。

## 切片（未拆分时仅一行，sub-feature-id = feature-id）

| sub-feature-id | Spec | Spec 门禁 | Spec 用户确认 | Design 门禁 | Review 门禁 | 状态 | 后续步骤 |
|---|---|---|---|---|---|---|---|
| chore-maven-compiler-release | N/A（跳过） | skipped（toolchain 基线修复；无新增产品合同/公开行为） | not-required | skipped（无模块边界/分层/选型决策；仅声明已有 release=17 对应的 compiler 插件） | skipped（fast；变更范围仅 pom.xml 显式 plugin + release 绑定既有属性；无业务代码） | done | 已授权合入 main（不 push）；归档待用户指令 |

阻塞原因:
恢复条件:
恢复后的目标状态:

## Plan 确认

- Plan 路径: [workflow/docs/features/chore-maven-compiler-release/plan.md](../features/chore-maven-compiler-release/plan.md)
- 确认结果: **approved**（2026-08-06）
- 确认依据: 当前用户会话「pom.xml 允许合入」= 对该极简 Plan 的确认与合并授权（完整流程授权一并生效）

## 用户授权记录

- 2026-08-06：用户「pom.xml 允许合入」= **完整流程授权** + **极简 Plan 确认（approved）** + **合并授权**（合入 `main`，本地合入；**不 push**）。勿入库 `sqllogictest/`；勿提交 `.env`。

## 进度笔记

- 2026-08-06：Manager 登记 `chore-maven-compiler-release`；路径 **fast**；Spec/Design/Review **skipped**；源分支 `chore-maven-compiler-release` → 目标 `main`。状态 `backlog` → **`planning`**。调度 **Planner** 编写极简 `plan.md`。
- 2026-08-06：Planner 产出 `plan.md`（compiler 插件 3.13.0 + L2 验证）。用户「pom.xml 允许合入」已持久化为 Plan **approved** + 合并授权。状态 `planning` → **`planned`** → **`developing`**。调度 **Developer**：自 `main` 创建并检出源分支，仅改 `pom.xml` + `dev-notes.md`；勿入库 `sqllogictest/` / `.env`；勿改 `workflow/docs/manager/*`；本轮可不单独 commit（关闭时由 Manager 一次提交）。
- 2026-08-06：Developer 交接。`pom.xml` 已声明 `maven-compiler-plugin` 3.13.0 + `${maven.compiler.release}`；基线改前 `clean package` 失败（option 5）；改后 `clean test` / `package` exit 0、无 option 5。`dev-notes.md` 已写。未 commit。Review 门禁 **skipped** → 状态 `developing` → **`qa`**。调度 **QA** 独立复跑 P0；Pass 后因合并授权已持久化，Manager 置 `done` 并一次提交后调度 Merge Executor 合入 `main`（不 push）。
- 2026-08-06：QA **Pass**（`qa-report.md` 未单独提交）。合并前置核对：Plan approved、Review skipped（fast）、QA Pass、源/目标分支已记录、用户「pom.xml 允许合入」合并授权已持久化。Manager 在源分支将状态置 **`done`**，与 `pom.xml`、Feature 文档（plan/dev-notes/qa-report）及 STATUS/工作项记录 **一次提交**；随后调度 QA 兼任 Merge Executor：`git merge --ff-only chore-maven-compiler-release` 合入 `main`；**不 push**；勿入库 `sqllogictest/`。
