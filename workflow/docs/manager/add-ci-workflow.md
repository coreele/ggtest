# 工作项记录: add-ci-workflow

工作项标识: add-ci-workflow
描述: 添加 GitHub Actions CI 工作流 — Java 17 build + test，SQLite matrix
路径等级: fast
源分支: add-ci-workflow
目标分支: main
文档影响: N/A

> 权威流程见 [workflow/README.md](../../README.md)；活跃状态见 [STATUS.md](STATUS.md)。

## 切片门禁（未拆分）

| sub-feature-id | 路径等级 | 源分支 | Spec | Spec 门禁 | Spec 用户确认 | Design 门禁 | Review 门禁 |
|---|---|---|---|---|---|---|---|
| add-ci-workflow | fast | add-ci-workflow | 无 | skipped（范围明确：.github/workflows/ci.yml） | not-required | skipped | skipped |

## 切片状态

| sub-feature-id | 状态 | 后续步骤 |
|---|---|---|
| add-ci-workflow | backlog | Planner |

## 进度笔记

- 2026-08-07 登记。P0：项目无自动化构建/测试门禁。添加 `.github/workflows/ci.yml`（30行，`mvn verify` on push/PR，Java 17）。
