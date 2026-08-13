# Git 协作规范

## 目的与适用范围

本规范规定分支、提交、Pull Request、合并和回滚要求。

**主责角色：** Manager（工作项与分支策略）、Developer（创建工作分支、提交与 Pull Request）、Merge Executor（受权合并）。

**生效条件：** 本规范仅在工作区是 Git 仓库时生效。非 Git 工作区须跳过提交与合并操作，但不跳过 Spec、Plan、Review、QA 和归档门禁。

## 1. 工作分支

### 1.1 必须新建分支

**实现（Developer 开始写代码/测试）之前，必须先有独立工作分支。** 禁止在 `main`、`master` 或 `release/*` 上直接实施功能或缺陷修复。

流程：

1. Manager 在调度 Developer 前，于工作项记录填写 **目标分支**（默认 `main`）与 **源分支**（拟创建的工作分支名）；
2. Developer 自目标分支创建并检出源分支；若源分支已存在则检出并确认基于正确目标；
3. 全部实现、修复、相关文档与**工作流关闭后的 STATUS/`done` 更新**均发生在该源分支上；
4. QA `Pass` 且用户授权合并后，Manager 在源分支置 `done`（与未入库的 `review.md` / `qa-report.md` 一次提交，细则见 §1.4），再由 Merge Executor / PR 合入；合入后不再为 STATUS 或报告单独提交。

每个工作项使用**独立**工作分支，不得与其他工作项共用。

### 1.2 命名

分支名称须包含工作项标识：

```text
<feature-id>
<feature-id>-<简短描述>
```

示例：`fix-ca019-cli-dash-values`、`xugu-engine`。

### 1.3 职责划分

| 角色 | 职责 |
|---|---|
| Manager | 登记时或调度 Developer 前声明目标分支与源分支名；用户授权合并后在**源分支**将状态置 `done`，并与未入库的 `review.md` / `qa-report.md` **一次提交** |
| Developer | 创建/检出工作分支后方可实施；禁止在受保护分支上直接提交实现 |
| Merge Executor | 仅在授权且 STATUS 已为 `done` 后合并源分支 → 目标分支（或用户经 GitHub 合并）；不负责改 STATUS |
| Reviewer / QA | 将 `review.md` / `qa-report.md` 写入 `features/<feature-id>/`；**不负责**对这些报告做 Git 提交（交由 Manager 按时机提交） |

### 1.4 Review / QA 报告的提交时机

| 时机 | 是否提交 `review.md` / `qa-report.md` |
|---|---|
| Reviewer 写出结论、进入 `qa` 等待验收 | **否**（工作区保留即可；Manager 可将 STATUS 推进到 `qa`，但不得把报告一并提交仅为「进 QA」） |
| QA `Pass`、等待人工合并授权 | **否**（父会话审阅工作区报告；禁止为「QA 结束」单独提交报告） |
| 用户授权合并 / 关闭 | **是**：与 STATUS/`done`、工作项记录**一次提交** |
| QA `Fail` / `Blocked`，或 Reviewer `Request changes`（退回修复） | **可以**与状态回退一并提交，使修复链路有持久证据 |

禁止在「QA Pass 待人工审核」窗口内单独提交报告后又为 `done` 再开第二次纯文档提交。

## 2. 提交规范

提交须保持原子性：每次提交对应单一逻辑变更，便于审阅与回滚。授权关闭时「STATUS/`done` + 未入库 review/qa 报告」视为同一逻辑关闭变更，允许同一次提交。

### 2.1 信息格式（Conventional Commits）

```text
<type>(<scope>): <subject>

<body>
```

- **`<type>`**（必须，小写）：`feat` 新功能｜`fix` 缺陷修复｜`docs` 文档｜`refactor` 重构（无行为变化）｜`test` 测试｜`chore` 构建/工具/杂务｜`perf` 性能｜`build` 构建系统/依赖｜`ci` CI 配置。
- **`<scope>`**（可选，小写）：受影响模块，如 `cli`、`db`、`parser`、`normalize`、`runner`、`model`、`workflow`、`audit`、`test`。多模块选主要者；工作流/文档类用 `workflow`/`audit`。
- **`<subject>`**（必须）：祈使语气、小写起首、不加句号、≤72 字符；说「做什么/改什么」，避免泛化（不写 `update code`）。
- **`<body>`**（可选）：解释**为什么**（动机、根因、权衡），不复述 diff；每行 ≤72 字符 wrapping；可用 `-` 列表。

### 2.2 本仓库约定用法

| 场景 | 示例 |
|---|---|
| 实现/缺陷修复 | `fix(cli): allow dash-prefixed values for value options (CA-019)` |
| 新功能 | `feat(db): add XuguDB engine (--engine xugu, alias xugudb)` |
| 工作流状态推进 | `docs(workflow): <feature-id> -> <state> (<context>)`，如 `docs(workflow): fix-ca020-main-fatal-catch -> done (QA Pass r1, review Approve, user authorized)` |
| 审计登记/报告 | `docs(audit): mark CA-023 resolved (...)` / `chore(audit): ...` |
| 纯文档 | `docs: ...`（无明确 scope 时） |

> 工作流状态推进提交统一用 `docs(workflow): <feature-id> -> <state>`；`<state>` 取 `planned` / `qa` / `done` 等，括注关键上下文（review/QA 结论、授权来源）。`developing` 等中间态一般随实现提交，不单独成文。

### 2.3 禁止

- 提交信息含凭据、连接串或可还原密钥；
- `WIP` / `misc` / `fix bug` 等无信息 subject；
- 一个提交混入多个无关 `type`/scope 的变更。

## 3. 禁止提交的内容

禁止将以下内容纳入版本控制或提交记录：

- 密钥、令牌、证书私钥；
- 真实连接字符串（含生产或预发凭据）；
- `.env` 及同类本地环境配置文件（须使用 `.env.example` 等模板）；
- 构建产物（`dist/`、`build/`、`target/` 等，除非仓库明确要求纳入）；
- 本地 IDE 临时文件与个人配置（须在 `.gitignore` 中排除）。

## 4. 合并前置条件

合并门禁（QA Pass、用户授权、分支合规等）以 [`workflow/README.md`](../../README.md#merge) §Merge 为准；本规范仅补充 Git 机制：合入策略见 §6（rebase + FF），受保护分支见 §5，报告提交时机见 §1.4。

任一条件未满足时，Merge Executor 不得执行合并。

## 5. 受保护分支

禁止向受保护分支（如 `main`、`master`、`release/*`）执行 force push。

禁止在受保护分支上直接实施功能或缺陷修复；实现必须经工作分支合并进入。

## 6. 合入目标分支的策略

**强制：rebase 到最新目标分支后 fast-forward 合入**，保持主线线性历史，**禁止 merge commit**（除非用户明确授权保留 merge commit）。

流程（本地或 CI 等价操作）：

1. 在源分支执行 `git fetch`（若使用远程）并 `git rebase <目标分支>`（通常为 `main`）；
2. 解决冲突后，在目标分支上 `git merge --ff-only <源分支>`；
3. 若源分支已包含另一些已合入（或即将合入）分支的提交，只 rebase/合入尚未在目标上的独有提交（例如 `git rebase --onto <目标> <已包含的公共祖先> <源分支>`），避免重复提交与多余 merge commit。

出现以下情形时，须停止合并操作并返回 Manager 与用户决策：

- 无法 fast-forward，且 rebase 不可行或用户未授权改写源分支历史；
- 存在未解决的合并冲突且无法在不破坏工作项范围的前提下安全解决；
- 目标分支保护规则与当前合并请求冲突；
- 用户要求保留 merge commit（偏离默认 rebase+FF，须显式授权）。

不得自行假设允许 merge commit 或 force push；偏离「rebase + FF」须有用户明确授权。

## 7. 回滚

回滚已共享历史时，优先使用新的 revert 提交或 revert Pull Request，保留完整历史记录。

禁止以破坏性 `reset`（如 `git reset --hard` 后 force push）替代已推送至远程或已被他人基于其工作的提交的回滚。

本地未推送的提交可按仓库规范使用 `reset`；一旦历史已共享，须使用 revert。
