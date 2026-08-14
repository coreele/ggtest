# 工作项: add-build-plugins

描述: pom.xml 添加 jacoco（覆盖率）+ spotbugs（静态分析）+ dependency-check（CVE 扫描），合并 backlog 中 add-jacoco-coverage / add-spotbugs-analysis / add-dependency-check
目标分支: main
源分支: add-build-plugins
基线提交: a6c8719bc48099cf772a6bd1807876dd4577259c
文档影响: 

> **本文件须保存为 `workflow/archive/2026/add-build-plugins/add-build-plugins.md`**，文件名与目录同名。
> 流程定义见 `workflow/WORKFLOW.md`；看板见 `workflow/STATUS.md`。
> 本工作项的全部产物平铺在 `workflow/archive/2026/add-build-plugins/`，无子目录、无版本后缀。
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

- 2026-08-07 登记。合并 backlog 中 add-jacoco-coverage / add-spotbugs-analysis / add-dependency-check 三项（均无 features 产物，仅 manager 记录）。
- 2026-08-11：实施于 commit `257b7d8`（pom.xml 添加 JaCoCo 0.8.12 + SpotBugs 4.8.6.4 + dependency-check 10.0.4）。Review Approve、QA Pass（323 tests, 0 failures；SpotBugs 0 bugs）。三项被合并的 backlog 工作项标记 `cancelled` 并归档。
- 2026-08-14：按 ggnote `WORKFLOW.md` 标准迁移工作流目录（记录与产物合并为同一目录；权威文件改为 `workflow/WORKFLOW.md`）。
