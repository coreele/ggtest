# 工作项记录: add-jacoco-coverage

工作项标识: add-jacoco-coverage
描述: pom.xml 添加 jacoco-maven-plugin，绑定 test 阶段，设最低覆盖率阈值
路径等级: fast
源分支: add-jacoco-coverage
目标分支: main
文档影响: N/A

> 权威工作流、门禁与状态说明见 [workflow/README.md](../../../README.md)。
> 活跃状态见 [STATUS.md](../../manager/STATUS.md)。

## 切片（未拆分，sub-feature-id = add-jacoco-coverage）

| sub-feature-id | Spec 门禁 | Spec 用户确认 | Design 门禁 | Review 门禁 | 状态 | 后续步骤 |
|---|---|---|---|---|---|---|
| add-jacoco-coverage | skipped | not-required | skipped | skipped | **done** | — |

状态: done
后续步骤: none
阻塞原因: none
恢复条件: N/A
恢复后的目标状态: N/A

## 进度笔记

- 2026-08-07 登记。P1：无 JaCoCo，250 测试无覆盖率可见性。`pom.xml` `<build><plugins>` 中添加 `jacoco-maven-plugin`。
- 2026-08-11：实施前 Manager 将本项与 add-spotbugs-analysis / add-dependency-check 合并为新工作项 `add-build-plugins`（单一 Plan / Review / QA 周期）。功能经 `add-build-plugins` 交付到 `main`（commit `257b7d8`：JaCoCo 0.8.12，`mvn test` 生成 `target/site/jacoco/`；Review Approve；QA Pass — 323 tests, 0 failures）。
- 本项无独立 features 产物；按合并交付处置，状态置 `done` 并归档。实际产物与报告见 `workflow/docs/features/add-build-plugins/`。
