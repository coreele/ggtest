# 工作项记录: add-dependency-check

工作项标识: add-dependency-check
描述: pom.xml 添加 owasp-dependency-check-maven 或 GitHub Dependabot
路径等级: fast
源分支: add-dependency-check
目标分支: main
文档影响: N/A

> 权威工作流、门禁与状态说明见 [workflow/README.md](../../../README.md)。
> 活跃状态见 [STATUS.md](../../manager/STATUS.md)。

## 切片（未拆分，sub-feature-id = add-dependency-check）

| sub-feature-id | Spec 门禁 | Spec 用户确认 | Design 门禁 | Review 门禁 | 状态 | 后续步骤 |
|---|---|---|---|---|---|---|
| add-dependency-check | skipped | not-required | skipped | skipped | **done** | — |

状态: done
后续步骤: none
阻塞原因: none
恢复条件: N/A
恢复后的目标状态: N/A

## 进度笔记

- 2026-08-07 登记。P1：处理 DB 凭证工具，依赖 `sqlite-jdbc 3.53.2.0`、`postgresql 42.7.13`，无 CVE 扫描。添加 OWASP dependency-check 或 Dependabot。
- 2026-08-11：实施前 Manager 将本项与 add-jacoco-coverage / add-spotbugs-analysis 合并为新工作项 `add-build-plugins`（单一 Plan / Review / QA 周期）。功能经 `add-build-plugins` 交付到 `main`（commit `257b7d8`：OWASP dependency-check 10.0.4，`check` 绑定 verify，failBuildOnCVSS=9；Review Approve；QA Pass）。
- 本项无独立 features 产物；按合并交付处置，状态置 `done` 并归档。实际产物与报告见 `workflow/docs/features/add-build-plugins/`。
