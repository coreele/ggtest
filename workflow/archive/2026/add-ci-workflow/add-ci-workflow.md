# 工作项: add-ci-workflow

描述: 添加 GitHub Actions CI 工作流 — Java 17 build + test，SQLite matrix
目标分支: main
源分支: add-ci-workflow
基线提交: a6c8719bc48099cf772a6bd1807876dd4577259c
文档影响: N/A

> **本文件须保存为 `workflow/archive/2026/add-ci-workflow/add-ci-workflow.md`**，文件名与目录同名。
> 流程定义见 `workflow/WORKFLOW.md`；看板见 `workflow/STATUS.md`。
> 本工作项的全部产物平铺在 `workflow/archive/2026/add-ci-workflow/`，无子目录、无版本后缀。
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

- 2026-08-07 登记。P0：项目无自动化构建/测试门禁。添加 `.github/workflows/ci.yml`（30行，`mvn verify` on push/PR，Java 17）。
- 2026-08-09 QA Pass（V1/V2 通过，V3 合入后观察）；已授权合并；状态 `done` 一次提交并合入 main。
- 2026-08-10 归档：源分支 `add-ci-workflow` 已合入 `main`（commits `a9a9cf4`、`f7547ea`），STATUS 归档区记录。
- 2026-08-14：按 ggnote `WORKFLOW.md` 标准迁移工作流目录（记录与产物合并为同一目录；权威文件改为 `workflow/WORKFLOW.md`）。
