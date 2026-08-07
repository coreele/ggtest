# 工作项记录: add-jacoco-coverage

工作项标识: add-jacoco-coverage
描述: pom.xml 添加 jacoco-maven-plugin，绑定 test 阶段，设最低覆盖率阈值
路径等级: fast
源分支: add-jacoco-coverage
目标分支: main
文档影响: N/A

> 权威流程见 [workflow/README.md](../../README.md)；活跃状态见 [STATUS.md](STATUS.md)。

## 切片门禁（未拆分）

| sub-feature-id | 路径等级 | 源分支 | Spec | Spec 门禁 | Spec 用户确认 | Design 门禁 | Review 门禁 |
|---|---|---|---|---|---|---|---|
| add-jacoco-coverage | fast | add-jacoco-coverage | 无 | skipped | not-required | skipped | skipped |

## 切片状态

| sub-feature-id | 状态 | 后续步骤 |
|---|---|---|
| add-jacoco-coverage | backlog | Planner |

## 进度笔记

- 2026-08-07 登记。P1：无 JaCoCo，250 测试无覆盖率可见性。`pom.xml` `<build><plugins>` 中添加 `jacoco-maven-plugin`。
