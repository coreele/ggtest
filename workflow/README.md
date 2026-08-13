# Agent 工作流与文档索引

本文档是工作流、角色、门禁、状态和文档结构的**唯一权威说明**。STATUS、工作项记录、模板和角色指令只保存执行信息，不得另写或复制完整流程。冲突时以本文为准。

**标识：** `<feature-id>` 为小写短横线。调度主键为 `feature-id`。目录约定见「文档结构」。

## 权威工作流

```text
Manager 登记工作项并判定路径与门禁
→ [Spec=required] Analyst 编写 Spec
→ [需确认 Spec] 用户会话确认
→ [Design=required] Planner 编写 Design
→ Planner 编写 Plan
→ Developer 实施与开发者验证
→ Reviewer 审阅
→ [Review=required] 取得 Approve 后进入 QA
→ QA 验收
  ├─ Fail → Developer 修复 → [需 Review] 复审 → QA 复测
  ├─ Blocked → 记录原因与恢复条件并停止
  └─ Pass → 用户授权合并 → Manager 置 done（源分支一次提交，见 Merge 门禁）
→ 合入目标分支（Merge Executor 或 GitHub PR；合入不再改 STATUS）
→ [用户要求关闭] Manager 归档
```

**`done`：** QA Pass + 用户已授权合入（或非 Git 下授权完成）。表示工作流关闭，**不表示**已出现在目标分支 tip；合入以 git / PR 为准。

**非 Git：** 跳过提交与合并；适用的 Spec / Design / Plan / Review / QA / 归档门禁不得跳过。

### 不得自动越过的用户确认

| 事项 | 何时必须确认 |
|---|---|
| Spec | `full`；或 `standard` 且存在业务歧义 |
| 合并 | 所有路径（QA Pass 后授权，授权后即可标 `done`，不必等合入完成） |

> Design / Plan / Developer / Review / QA 阶段**无需用户确认**。Manager 在 Spec 确认（如需）后连续推进至 QA，仅在 QA Pass 后回到用户请求合并授权。Spec 确认 ≠ 合并授权；二者独立。

## 角色与职责

| 角色 | 职责 | 主要产物 | 不负责 |
|---|---|---|---|
| Manager | 登记、门禁判定、调度、状态、关闭归档 | STATUS、工作项记录 | Spec / Design / Plan、代码、测试报告、合并 |
| Analyst | 需求与行为合同 | `spec.md` | 技术拆分、实现、状态 |
| Planner | 技术设计、按需 UI 设计与实施计划 | `design.md`、`ui-design.md`（按需）、`plan.md` | 需求决策、实现、状态 |
| Developer | TDD、实现、开发者验证、缺陷修复 | 代码、`dev-notes.md` | Spec / Plan、状态、合并 |
| Reviewer | 审阅实现、测试、文档、安全 | `review.md` | 实现、QA、状态、合并 |
| QA | 独立验收与回归 | `qa-report.md` | 业务实现、状态 |
| Merge Executor | STATUS 已为 `done` 后合入 | 合并结果 | 质量验收、代码所有权、状态 |
| DevOps | 本地脚本与部署排障文档 | 脚本、`workflow/docs/deploy/` | CI/CD、Spec / Plan、状态、合并 |

- 默认由 QA 兼任受控 Merge Executor，不因此获得代码所有权；仓库另有 Code Owner / Release Manager / 分支保护等规则时从其规则。
- **仅 Manager** 可改 STATUS 与工作项记录；其他角色只报告结果，由 Manager 在调度下一步前持久化。
- 产出角色在独立上下文结束后**立即返回**待确认 / 待调度事项，不得在子会话内阻塞等用户确认。确认由当前用户会话收集后，再调度 Manager 持久化。

## 路径等级

`fast` / `standard` / `full` 是本工作流的风险等级（非行业标准术语），**按工作项判定并写入工作项记录**。

| 等级 | 适用范围 | Spec | 用户确认 | Review |
|---|---|---|---|---|
| `fast` | 范围明确的单点修复 | 默认跳过 | 合并 | 可在工作项记录标 `skipped` |
| `standard` | 常规功能、重构或接口变更 | 有合同风险时必须 | 合并（Spec 有业务歧义时再确认） | 必须 |
| `full` | 新能力、跨模块或范围未明 | 必须 | Spec、合并 | 必须 |

总览 / tracking 行（只索引、不实施）：路径等级与 Spec / Design / Review 门禁均可为 `N/A`，不调度产出角色。

## 门禁

### Spec

| 约束 | 说明 |
|---|---|
| 必须编写 | `full`；`standard` 在新增行为、公开接口、状态转换、错误约定或跨模块合同时 |
| 默认可跳过 | `fast` |
| 作者 | 仅 Analyst |
| 用户确认 | 见上文「不得自动越过的用户确认」 |

### Design

- **设为 `required`：** 仅当模块边界、分层或技术选型需要决策。
- **不属于 Design：** API 形状、数据约束、错误约定、行为验收（属 Spec）；界面信息架构 / 交互 / 视觉（属按需 `ui-design.md`）。
- **顺序：** Planner 在 Plan 之前编写；`required` 时须先有 `design.md` 再进 Plan。

### UI Design（按需，非门禁）

- **何时：** 范围含用户可见界面、关键流程或交互表面，且存在布局 / 信息架构 / 交互决策时，Planner 产出 `ui-design.md`。
- **跳过：** 纯后端 / API / 文档；或 `fast` 且仅文案 / 小样式、无 IA/布局决策。跳过时 Plan 元信息「依据 UI」标 `N/A`。
- **不替代** Spec 行为合同，也不替代 `design.md` 的结构选型。

### Plan

- Spec 为 `required` 时，对应 `spec.md` 必须已存在。
- Plan **必须包含：** 任务拆分（含完成条件）、依赖与顺序、触碰路径、可复现验证命令、最低验证层、每项验证的预期证据、Review 门禁与进入 QA 条件、文档影响、无法验证时的原因 / 风险 / 恢复条件、实施→Review→QA 交接顺序。
- Plan 写成并持久化后即进入 `planned`，**无需用户确认**。

### Review

| 约束 | 说明 |
|---|---|
| 调用时机 | 实现完成后即可调度 Reviewer |
| 进入 QA | Review 门禁是进 QA 的前置条件，**不是**调用 Reviewer 的前置条件 |
| `standard` / `full` | 进 QA 前必须 `Approve` |
| `fast` | 仅当工作项将 Review 标为 `skipped` 时可直进 QA |
| 总览行 | 可为 `N/A` |
| `Request changes` | 回 Developer 修复并复审 |
| `Comment` | 不得含阻塞项；否则必须改用 `Request changes` |

### QA

- 逐项核对 Spec 验收条件（如有）与 Plan 验证要求。
- 首次与回归均按轮次**追加**到同一 `qa-report.md`。
- 结论仅允许：`Pass` | `Fail` | `Blocked`。
- `Fail` 须登记缺陷；`Blocked` 须登记原因与恢复条件。
- 非 `Pass` 不得请求合并授权。

### Merge

须同时满足：

1. QA 最新结论为 `Pass`
2. 用户会话已明确授权合并
3. Git：工作项源分支、目标分支已记录，且符合 [`workflow/docs/standards/git.md`](workflow/docs/standards/git.md)；合入一律 **rebase + fast-forward**（见 git.md §6，禁止 merge commit 除非用户明确授权）
4. 实现位于该工作项独立工作分支；**禁止**在 `main` / `master` / `release/*` 上直接实施后合并

授权后的关闭提交（Git）：

- Manager 在**该工作项源分支**将状态置 `done`，与未入库的 `review.md` / `qa-report.md` **一次提交**。
- **禁止**在待合并授权期间单独提交上述报告。
- **禁止**合入后再为目标分支单独改 STATUS / 补交报告。
- 合入前 STATUS 须已为 `done`；合入可由 Merge Executor 或 GitHub PR 执行，合入本身不改 STATUS。
- 合入失败：可 `done → blocked`（或保持 `done` 并记阻塞笔记，择一写清）；不得归档。

报告提交时机细则见 [`workflow/docs/standards/git.md`](workflow/docs/standards/git.md) §1.4。

## 状态机与回退

```text
backlog → speccing → awaiting-spec-approval → designing → planning
→ planned → developing → reviewing → qa → done
```

旁支：`blocked`、`cancelled`。`done` 含义见上文。

| 场景 | 转换 |
|---|---|
| 跳过 Spec | `backlog → designing \| planning` |
| Spec 无需确认 | `speccing → designing \| planning` |
| Spec 需确认 | `speccing → awaiting-spec-approval` →（确认后）`designing \| planning` |
| Spec 被拒（rejected） | `awaiting-spec-approval → speccing`（Analyst 修订后重新确认） |
| 跳过 Design | `backlog \| speccing \| awaiting-spec-approval → planning` |
| 开始 Design / Plan | → `designing` / `planning`；Design 通过后 → `planning` |
| Plan 写完 | `planning → planned`（无需用户确认） |
| 开始实施 | `planned → developing` |
| 调度 Reviewer | `developing → reviewing` |
| `fast` 且 Review=`skipped` | `developing → qa` |
| Reviewer `Approve` | `reviewing → qa` |
| Reviewer `Request changes` | `reviewing → developing`（修复后复审） |
| Reviewer `Comment` 含阻塞 | 按 `Request changes` 处理 |
| 用户授权合并 | `qa → done`（源分支一次提交，见 Merge 门禁） |
| 合并授权暂缓 / 拒绝 | `qa` 保持 `qa`（待后续授权；或用户取消 → `cancelled`） |
| 合入失败 | `done → blocked`（或保持 `done` + 笔记） |
| 任意活动态阻塞 | → `blocked`；恢复后进入工作项记录指定目标态 |
| 用户取消 | → `cancelled` |

### QA Fail 闭环

```text
QA Fail → Manager: qa → developing → Developer 修复（更新 dev-notes.md）
→ [Review=required] Reviewer 复审 Approve → Manager: → qa
→ QA 追加回归轮次 → Pass | Fail | Blocked
```

每个缺陷须有唯一标识、严重程度、状态、处理说明与验证证据。Developer 给出建议复测范围；`standard` / `full` 修复须重新 `Approve`；QA 复测失败项与受影响回归范围。循环至 `Pass`、`Blocked` 或用户取消。

## 独立上下文与用户汇报

产出角色在独立上下文中运行，仅通过以下介质交接：工作区变更、Git 提交 / PR（可用时）、`workflow/docs/features/<feature-id>/`、工作项记录与 STATUS。

当前用户会话是唯一用户交互入口。Manager 从持久化文档恢复状态，不得依赖其他角色会话记忆。

**编排：** Manager 默认按状态机连续推进；用户确认门禁见「不得自动越过的用户确认」（仅 Spec 与合并）。

Manager 返回格式：

```text
工作项: <feature-id>
当前状态: <state>
本次操作: <action>
产出文件: <paths>
门禁结果: pass | blocked | awaiting-user
待用户确认: none | spec | merge | question
阻塞信息: none | cause + recovery condition
后续步骤: <role/action>
```

只汇报可验证的操作、文件、状态、门禁、阻塞与待确认事项。

`question` = 非门禁澄清（方案取舍、命名、范围取舍等），由 Manager 视需要提问，不改变状态机；门禁确认仅 Spec 与合并两类。

## 文档结构

```text
workflow/
  README.md
  agents/             # 角色定义
  skills/             # 技能定义
  docs/
    standards/        # documentation | git | quality | security
    manager/
      STATUS.md
      <feature-id>.md   # 仅活跃工作项
    features/<feature-id>/
      spec.md
      design.md        # 按需
      ui-design.md     # 按需
      plan.md
      dev-notes.md
      review.md
      qa-report.md
    _templates/       # 含 ui-design.md 等
    archive/YYYY/<feature-id>/
      manager.md      # 原 manager/<feature-id>.md
      …               # 原 features/<feature-id>/ 内容（若有）
```

**必须 / 禁止：**

- Manager 登记时创建 `workflow/docs/features/<feature-id>/` 与 `workflow/docs/manager/<feature-id>.md`。
- 其他角色**不得**另建不同标识的工作项目录，**不得**使用 `workflow/docs/plans/`、`workflow/docs/qa/`、`workflow/docs/prd/` 等扁平产出根。
- `workflow/docs/manager/` 除 STATUS 外仅保留活跃记录；归档后不得残留。

### feature-id

工作项与归档单位。活跃：`features/<id>/` + `manager/<id>.md`；归档：`archive/YYYY/<id>/`（记录改名为 `manager.md`）。所有产物（`spec` / `design` / `ui-design` / `plan` / `dev-notes` / `review` / `qa-report`）始终在 `features/<feature-id>/` 根目录，**无子目录、不拆分**。

STATUS 活跃表须有 `feature-id` 列。

### 工程规范索引

Docs as Code：与相关代码同仓库、同分支、同审阅演进。工作项须遵循：

| 规范 | 文档 | 内容 |
|---|---|---|
| 文档工程 | [`workflow/docs/standards/documentation.md`](workflow/docs/standards/documentation.md) | 分类主责、文档影响、用户/运维要素；工作流产物整理（§B） |
| Git | [`workflow/docs/standards/git.md`](workflow/docs/standards/git.md) | 分支、提交、PR、合并、回滚 |
| 质量 | [`workflow/docs/standards/quality.md`](workflow/docs/standards/quality.md) | 验证层级、完成定义 |
| 安全 | [`workflow/docs/standards/security.md`](workflow/docs/standards/security.md) | 敏感信息、依赖、认证授权、安全审阅触发 |

Plan 须说明开发 / 用户 / 运维文档影响；不适用标 `N/A` 并写理由。验证无法执行时记录原因、风险与恢复条件。

`code-audit`（`workflow/docs/standards/code-audit*.md`、产物 `workflow/docs/audit/`）在工作流外，不进入本状态机。

## 工作项记录

按模板 [`workflow/docs/_templates/manager-feature.md`](workflow/docs/_templates/manager-feature.md) 创建 `workflow/docs/manager/<feature-id>.md`。

**工作项级：** 工作项标识、描述、目标分支（Git 默认 `main`；非 Git 填「不适用」）、文档影响。

**门禁与状态**（模板拆为「门禁」「状态」两表）：

```text
# 门禁表
路径等级: fast | standard | full | N/A
源分支:                 # 推荐 <feature-id>；总览行可 N/A
Spec 门禁: required | skipped | N/A
Spec 用户确认: required | not-required | approved | rejected | N/A
Design 门禁: required | skipped | N/A
Review 门禁: required | skipped | N/A   # skipped 仅 fast

# 状态表
状态 / 后续步骤 / 阻塞原因 / 恢复条件 / 恢复后的目标状态
```

- 表内只填枚举、短标签或链接；跳过理由、业务歧义、较长阻塞说明写入「进度笔记」（见 [`workflow/docs/standards/documentation.md`](workflow/docs/standards/documentation.md) §B）。
- 阻塞不得只写在脚注。

### Git：文档提交时机

1. 登记时填目标分支；将进入产出的工作项填源分支（宜在调度 Analyst / Planner 前；**最迟**调度 Developer 前）。
2. 源分支已声明后：该工作项的 Spec / Design / Plan / 实现与相关文档均提交到该源分支；代码与文档分提（先代码后文档）见 [`workflow/docs/standards/git.md`](workflow/docs/standards/git.md) §2.4。
3. 源分支未声明前：文件可留在工作区；声明并检出后按 [`workflow/docs/standards/git.md`](workflow/docs/standards/git.md) 提交，禁止在受保护分支直接提交。

## 关闭与归档

**关闭：** QA Pass + 用户授权合并 → Manager 在源分支置 `done` 并一次提交未入库报告 → 允许合入。合入后不再改 STATUS / 补交报告。

**归档**（features 与 manager 记录一并迁入 `workflow/docs/archive/YYYY/<feature-id>/`）仅当：

1. 工作项为 `done`（Review / 实施门禁非 `N/A` 时），且用户明确要求关闭 / 归档（建议核验目标分支已含实现）；或
2. 用户明确取消为 `cancelled`（无 features 产物时仍须归档工作项记录）。

步骤：

1. 从 STATUS 活跃表移除该工作项
2. 若有 `features/<feature-id>/`，**移动**到归档目录；否则创建空归档目录
3. 将 `manager/<feature-id>.md` **移动**为 `archive/YYYY/<feature-id>/manager.md`，并修正相对链接
4. 在 STATUS 归档区记录标识、最终状态与 `manager.md` 链接
5. 仓库可用时提交归档变更（可与功能合入解耦）

完成后，`workflow/docs/manager/<feature-id>.md` 与 `workflow/docs/features/<feature-id>/` 均不得残留。
