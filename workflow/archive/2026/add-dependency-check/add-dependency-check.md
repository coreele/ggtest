# 工作项: add-dependency-check

描述: pom.xml 添加 owasp-dependency-check-maven 或 GitHub Dependabot
目标分支: main
源分支: add-dependency-check
基线提交: a6c8719bc48099cf772a6bd1807876dd4577259c
文档影响: N/A

> **本文件须保存为 `workflow/archive/2026/add-dependency-check/add-dependency-check.md`**，文件名与目录同名。
> 流程定义见 `workflow/WORKFLOW.md`；看板见 `workflow/STATUS.md`。
> 本工作项的全部产物平铺在 `workflow/archive/2026/add-dependency-check/`，无子目录、无版本后缀。
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

- 2026-08-07 登记。P1：处理 DB 凭证工具，依赖 `sqlite-jdbc 3.53.2.0`、`postgresql 42.7.13`，无 CVE 扫描。添加 OWASP dependency-check 或 Dependabot。
- 2026-08-11：实施前 Manager 将本项与 add-jacoco-coverage / add-spotbugs-analysis 合并为新工作项 `add-build-plugins`（单一 Plan / Review / QA 周期）。功能经 `add-build-plugins` 交付到 `main`（commit `257b7d8`：OWASP dependency-check 10.0.4，`check` 绑定 verify，failBuildOnCVSS=9；Review Approve；QA Pass）。
- 本项无独立 features 产物；按合并交付处置，状态置 `done` 并归档。实际产物与报告见 `workflow/archive/2026/add-build-plugins/`。
- 2026-08-14：按 ggnote `WORKFLOW.md` 标准迁移工作流目录（记录与产物合并为同一目录；权威文件改为 `workflow/WORKFLOW.md`）。
