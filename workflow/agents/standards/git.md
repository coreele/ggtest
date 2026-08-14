# Git 规范

仅在仓库是 Git 仓库时生效。非 Git 仓库跳过分支、提交与合并，其余门禁一律不跳过。

流程门禁以 [WORKFLOW.md](../../WORKFLOW.md) 为准，本文件只补 Git 机制。

## 1. 工作分支

**任何工作项文件产生之前必须已有独立源分支。** 禁止先在目标分支创建记录、Spec、Plan 或实现，再等 Developer 建分支。

1. Manager 在内存中分配 `<id>`，确定**目标分支**（默认 `main`）与**源分支**；源分支名默认即 `<id>`，必要时可为 `<id>-<简短描述>`。
2. 创建前确认：目标分支存在；源分支名未被其他工作项占用；工作树没有无关修改；远程目标需要更新时已 fetch。
3. Manager 从明确的目标分支提交创建并检出源分支，记录该提交为**基线提交**。不得用含未提交修改的 `git switch -c` 把其他工作带进新分支；无法获得干净工作树时停止，或使用独立 `git worktree`。
4. 分支就绪后，Manager 才创建 `workspace/<id>/<id>.md`、更新 STATUS、提交登记记录并调度 Analyst / Planner。
5. Developer 不创建或改选源分支；开工前只验证当前分支、目标分支、源分支与基线记录一致，不满足就停止并报告 Manager。
6. 每个实施类工作项独占一个源分支。一个 Git worktree 同时只推进一个实施类工作项；并行工作使用不同 worktree。

`tracking` 是治理项，不实施代码，目标分支、源分支与基线可填 `N/A`。其记录按仓库政策作为纯文档提交处理；目标分支受保护时使用 docs 分支 / PR，不得借治理项绕过分支保护。

## 2. 提交责任

| 内容 | 谁提交 |
|---|---|
| 实现代码与测试 | Developer |
| `<id>.md`、`STATUS.md`、`spec.md`、`design.md`、`ui-design.md`、`plan.md`、`dev-notes.md`、`review.md`、`qa-report.md` | **Manager** |
| 审计报告与登记册 | 执行 `code-audit` 的会话 |

产出角色一律把文档留在**当前源分支的工作树**并报告，不执行 `git add` / `commit` / `push`。Manager 提交前须确认当前分支与 `<id>.md` 一致，且提交不含无关修改。

## 3. 提交时机

| 时机 | 提交什么 |
|---|---|
| 登记完成 | Manager 在新建源分支提交 `<id>.md` 与 STATUS |
| Spec / Design / Plan 完成 | Manager 随状态推进提交对应阶段文档 |
| 实施完成 | Developer 先提交代码与测试，Manager 随后提交 `dev-notes.md` 与状态 |
| Reviewer 给出结论、进入 `qa` | **不提交** `review.md` |
| QA `Pass`、进入 `merge-approval` | **不提交** `qa-report.md` |
| 用户授权合并 | Manager 把 `done` 状态与未入库的 `review.md`、`qa-report.md` **一次提交** |
| QA `Fail` / `Blocked`，或 `Request changes` 退回 | 可与状态回退一并提交，让修复链路有持久证据 |

禁止在「QA Pass 待授权」窗口内先单独提交报告、随后再为 `done` 开第二次纯文档提交。

## 4. 提交信息

```text
<type>(<scope>): <subject>

<body>
```

- **type**（必须，小写）：`feat` `fix` `docs` `refactor` `test` `chore` `perf` `build` `ci`。
- **scope**（可选，小写）：受影响模块名，取自本仓库实际模块划分（如 `cli`、`db`、`parser`、`normalize`、`runner`、`model`）；工作流与流程文档统一用 `workflow`，审计用 `audit`。
- **subject**（必须）：祈使语气、小写开头、无句号、≤72 字符，说清改了什么，不写 `update code` 这类空话。
- **body**（可选）：解释**为什么**（动机、根因、取舍），不复述 diff。

工作流状态推进统一写：`docs(workflow): <id> -> <state> (<上下文>)`，上下文括注 Review / QA 结论或授权来源。`developing` 等中间态一般随实现提交，不单独成文。

**禁止**：提交信息含凭据或可还原密钥；`WIP` / `misc` / `fix bug` 类无信息 subject；一个提交混入多个无关 type 或 scope。

## 5. 代码与文档分提

一次变更同时含实现代码与工作流文档时，**分两次提交，先代码后文档**：代码提交只含实现、测试与紧耦合资源（fixtures、构建配置）；文档提交用 `docs(workflow): ...`。

例外：关闭提交（`done` + 未入库的 `review.md` / `qa-report.md`）是同一逻辑关闭变更，一次提交不拆分。纯文档变更直接单次 `docs(...)`。

## 6. 禁止入库

密钥、令牌、证书私钥；真实连接字符串；`.env` 及同类本地环境配置（用 `.env.example` 模板）；构建产物；IDE 临时文件与个人配置（须在 `.gitignore` 排除）。

## 7. 同步、验收与合入

禁止向 `main`、`master`、`release/*` force push，禁止在其上直接实施。

### 7.1 最终验收前同步

Developer 完成实现后、进入最终 Review 前：

1. Developer 先提交代码与测试并报告“待同步”；Manager 提交此时待入库的 `dev-notes.md` 与状态，确保工作树干净；
2. Developer fetch 远程更新（如有），把源分支 rebase 到最新目标分支；
3. 发生冲突时按文件所有权处理：Developer 只解决代码、测试及紧耦合资源；`<id>.md`、STATUS 与其他工作流文档由 Manager 解决。任一方无法确认语义时停止并交用户决策，不得用 `ours` / `theirs` 整体覆盖；
4. Developer 在全部冲突按职责解决后继续 rebase，重新执行 Plan 要求的验证，在 `dev-notes.md` 追加目标分支提交、同步后源分支 HEAD、冲突处理与验证证据；
5. Manager 提交同步证据，确认工作树干净后才把状态推进到 `reviewing`。

禁止依赖未声明的 stash、autostash 或把未提交文档带过 rebase；同步前后每一份工作流证据都须由 Manager 明确入库。

Reviewer 与 QA 必须以同步后的提交为对象，并分别在报告中记录完整或足以唯一识别的提交 SHA。

### 7.2 QA 后目标分支移动

- QA Pass 后目标分支虽有新提交，但待合入源分支仍可直接 fast-forward：无需改写源分支，可直接合入。
- 若必须再次 rebase，且旧 QA 提交与新 HEAD 的文件树完全一致（仅 ancestry / SHA 变化）：重新运行最低必要验证，QA 在报告追加同步轮次并记录新 HEAD 后，才可请求合入。
- 若 rebase 发生冲突、改变提交内容或文件树：状态回到 `developing`，重新自验、Review、QA 与合并授权。禁止把未验收版本直接合入。

### 7.3 合入策略

默认保持线性历史：源分支已基于最新目标且验证通过后，fast-forward 合入；禁止 merge commit（除非用户明确授权保留）。

源分支若已包含其他已合入分支的提交，只 rebase 尚未在目标上的独有提交（如 `git rebase --onto <目标> <公共祖先> <源分支>`），避免重复提交。

出现以下情形停止合并并交回 Manager 与用户决策：无法 fast-forward 且 rebase 不可行或未获授权改写历史；冲突无法在不破坏工作项范围下安全解决；分支保护规则冲突；用户要求保留 merge commit。

源分支已推送或已被他人使用时，rebase 会改写共享历史：须先取得仓库政策或用户授权，更新远程只能使用 `--force-with-lease`；禁止普通 `--force`，永远禁止强推目标分支。

## 8. 合入后清理

确认目标分支已含实现后删除源分支（本地 `git branch -d <源分支>`；远程分支存在时再 `git push origin --delete <源分支>`）。归档工作项前应完成清理，避免已合入分支长期堆积。

归档发生在目标分支已含实现之后，属于治理文档变更：仓库允许时可提交到目标分支；目标分支受保护时使用独立 docs 分支 / PR。

## 9. 回滚

已共享的历史用新的 revert 提交或 revert PR 回滚，保留完整记录。禁止用破坏性 `reset` + force push 回滚已推送或已被他人基于其工作的提交。本地未推送的提交可用 `reset`。
