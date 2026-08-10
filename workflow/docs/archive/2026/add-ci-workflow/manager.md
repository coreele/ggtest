# 工作项记录: add-ci-workflow

工作项标识: add-ci-workflow
描述: 添加 GitHub Actions CI 工作流 — Java 17 build + test，SQLite matrix
路径等级: fast
源分支: add-ci-workflow
目标分支: main
文档影响: N/A

> 权威工作流、门禁与状态说明见 [workflow/README.md](../../../README.md)。
> 活跃状态见 [STATUS.md](../../manager/STATUS.md)。
>
> 文档路径：未拆分时 Spec 为 `workflow/docs/archive/2026/add-ci-workflow/spec.md`（无子目录）；已拆分时根目录仅总览 Spec，各切片为 `workflow/docs/archive/2026/add-ci-workflow/<feature-id>-<sub>/spec.md`。
> 归档后本文件迁至 `workflow/docs/archive/YYYY/<feature-id>/manager.md`，相对链接须同步修正；`workflow/docs/manager/` 仅保留活跃工作项记录与 `STATUS.md`。

## 切片（未拆分时仅一行，sub-feature-id = feature-id）

| sub-feature-id | Spec | Spec 门禁 | Spec 用户确认 | Design 门禁 | Review 门禁 | 状态 | 后续步骤 |
|---|---|---|---|---|---|---|---|
| add-ci-workflow | [plan.md](plan.md) [dev-notes.md](dev-notes.md) [qa-report.md](qa-report.md) | skipped（fast 路径，范围明确：仅新增 CI 配置） | not-required | skipped | skipped | done | 已合入 main；已归档 |

阻塞原因:
恢复条件:
恢复后的目标状态:

## 进度笔记

- 2026-08-07 登记。P0：项目无自动化构建/测试门禁。添加 `.github/workflows/ci.yml`（30行，`mvn verify` on push/PR，Java 17）。
- 2026-08-09 QA Pass（V1/V2 通过，V3 合入后观察）；已授权合并；状态 `done` 一次提交并合入 main。
- 2026-08-10 归档：源分支 `add-ci-workflow` 已合入 `main`（commits `a9a9cf4`、`f7547ea`），STATUS 归档区记录。
