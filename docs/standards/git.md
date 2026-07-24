# Git 协作规范

## 目的与适用范围

本规范规定分支、提交、Pull Request、合并和回滚要求。

**主责角色：** Manager（工作项与分支策略）、Developer（提交与 Pull Request）、Merge Executor（受权合并）。

**生效条件：** 本规范仅在工作区是 Git 仓库时生效。非 Git 工作区须跳过提交与合并操作，但不跳过 Spec、Plan、Review、QA 和归档门禁。

## 1. 工作分支

每个工作项须使用独立工作分支，不得与其他工作项共用同一功能分支。

分支名称须包含工作项标识（`feature-id`），推荐格式：

```text
<feature-id>
<feature-id>-<简短描述>
```

Manager 在工作项记录中声明目标分支；Developer 自目标分支创建并推送工作分支。

## 2. 提交规范

提交须保持原子性：每次提交对应单一逻辑变更，便于审阅与回滚。

提交信息须遵循仓库既有规范；无既有规范时采用 [Conventional Commits](https://www.conventionalcommits.org/)：

```text
<type>(<scope>): <subject>

<body>
```

常见 `type`：`feat`、`fix`、`docs`、`refactor`、`test`、`chore`。

## 3. 禁止提交的内容

禁止将以下内容纳入版本控制或提交记录：

- 密钥、令牌、证书私钥；
- 真实连接字符串（含生产或预发凭据）；
- `.env` 及同类本地环境配置文件（须使用 `.env.example` 等模板）；
- 构建产物（`dist/`、`build/`、`target/` 等，除非仓库明确要求纳入）；
- 本地 IDE 临时文件与个人配置（须在 `.gitignore` 中排除）。

## 4. 合并前置条件

执行合并前须同时满足：

| 条件 | 说明 |
|---|---|
| Plan 确认 | 用户已确认对应 Plan |
| 适用的 Review | Review 门禁为 `required` 时须取得 Approve |
| QA Pass | QA 验收结论为 Pass |
| 分支确认 | 源分支与目标分支已明确并在工作项记录中声明 |
| 用户明确授权 | 当前用户会话已授权合并 |

任一条件未满足时，Merge Executor 不得执行合并。

## 5. 受保护分支

禁止向受保护分支（如 `main`、`master`、`release/*`）执行 force push。

## 6. 无法 fast-forward 或合并策略不明确

出现以下情形时，须停止合并操作并返回 Manager 与用户决策：

- 无法 fast-forward 且仓库未规定允许的合并策略（merge commit、rebase、squash 等）；
- 存在未解决的合并冲突且无法在不破坏 Plan 范围的前提下安全解决；
- 目标分支保护规则与当前合并请求冲突。

不得自行假设合并策略或强制推进。

## 7. 回滚

回滚已共享历史时，优先使用新的 revert 提交或 revert Pull Request，保留完整历史记录。

禁止以破坏性 `reset`（如 `git reset --hard` 后 force push）替代已推送至远程或已被他人基于其工作的提交的回滚。

本地未推送的提交可按仓库规范使用 `reset`；一旦历史已共享，须使用 revert。
