# 工作项: chore-maven-compiler-release

描述: 在 pom.xml 显式声明 maven-compiler-plugin（建议 3.13.0），配置 `<release>${maven.compiler.release}</release>`（属性已为 17），使 Maven 3.6.3 默认 toolchain 不再回退到无效的 Source/Target 5，字面 `mvn -q clean test` / `package` 可成功。
目标分支: main
源分支: chore-maven-compiler-release
基线提交: a6c8719bc48099cf772a6bd1807876dd4577259c
文档影响: `workflow/archive/2026/chore-maven-compiler-release/`（plan/dev-notes/qa-report；无 Spec/Design/Review）

> **本文件须保存为 `workflow/archive/2026/chore-maven-compiler-release/chore-maven-compiler-release.md`**，文件名与目录同名。
> 流程定义见 `workflow/WORKFLOW.md`；看板见 `workflow/STATUS.md`。
> 本工作项的全部产物平铺在 `workflow/archive/2026/chore-maven-compiler-release/`，无子目录、无版本后缀。
> 表内只填枚举、短标签或路径；理由与长说明写进「进度笔记」。

## 门禁

| 路径等级 | Spec | Spec 用户确认 | Design | Review |
|---|---|---|---|---|
| fast | skipped | not-required | skipped | required |

## 状态

| 状态 | 下一步 | 阻塞原因 | 恢复条件 | 恢复后目标 |
|---|---|---|---|---|
| archived | — |  |  |  |

## 子项（仅 tracking 项填写）

| 子项 id | 状态 |
|---|---|
| — | |

## 进度笔记

- 2026-08-06：Manager 登记 `chore-maven-compiler-release`；路径 **fast**；Spec/Design/Review **skipped**；源分支 `chore-maven-compiler-release` → 目标 `main`。状态 `backlog` → **`planning`**。调度 **Planner** 编写极简 `plan.md`。
- 2026-08-06：Planner 产出 `plan.md`（compiler 插件 3.13.0 + L2 验证）。用户「pom.xml 允许合入」已持久化为 Plan **approved** + 合并授权。状态 `planning` → **`planned`** → **`developing`**。调度 **Developer**：自 `main` 创建并检出源分支，仅改 `pom.xml` + `dev-notes.md`；勿入库 `sqllogictest/` / `.env`；勿改 `workflow/docs/manager/*`；本轮可不单独 commit（关闭时由 Manager 一次提交）。
- 2026-08-06：Developer 交接。`pom.xml` 已声明 `maven-compiler-plugin` 3.13.0 + `${maven.compiler.release}`；基线改前 `clean package` 失败（option 5）；改后 `clean test` / `package` exit 0、无 option 5。`dev-notes.md` 已写。未 commit。Review 门禁 **skipped** → 状态 `developing` → **`qa`**。调度 **QA** 独立复跑 P0；Pass 后因合并授权已持久化，Manager 置 `done` 并一次提交后调度 Merge Executor 合入 `main`（不 push）。
- 2026-08-06：QA **Pass**（`qa-report.md` 未单独提交）。合并前置核对：Plan approved、Review skipped（fast）、QA Pass、源/目标分支已记录、用户「pom.xml 允许合入」合并授权已持久化。Manager 在源分支将状态置 **`done`**，与 `pom.xml`、Feature 文档（plan/dev-notes/qa-report）及 STATUS/工作项记录 **一次提交**；随后调度 QA 兼任 Merge Executor：`git merge --ff-only chore-maven-compiler-release` 合入 `main`；**不 push**；勿入库 `sqllogictest/`。
- 2026-08-14：按 ggnote `WORKFLOW.md` 标准迁移工作流目录（记录与产物合并为同一目录；权威文件改为 `workflow/WORKFLOW.md`）。
