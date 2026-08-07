# 工作项记录: add-dependency-check

工作项标识: add-dependency-check
描述: pom.xml 添加 owasp-dependency-check-maven 或 GitHub Dependabot
路径等级: fast
源分支: add-dependency-check
目标分支: main
文档影响: N/A

> 权威流程见 [workflow/README.md](../../README.md)；活跃状态见 [STATUS.md](STATUS.md)。

## 切片门禁（未拆分）

| sub-feature-id | 路径等级 | 源分支 | Spec | Spec 门禁 | Spec 用户确认 | Design 门禁 | Review 门禁 |
|---|---|---|---|---|---|---|---|
| add-dependency-check | fast | add-dependency-check | 无 | skipped | not-required | skipped | skipped |

## 切片状态

| sub-feature-id | 状态 | 后续步骤 |
|---|---|---|
| add-dependency-check | backlog | Planner |

## 进度笔记

- 2026-08-07 登记。P1：处理 DB 凭证工具，依赖 `sqlite-jdbc 3.53.2.0`、`postgresql 42.7.13`，无 CVE 扫描。添加 OWASP dependency-check 或 Dependabot。
