# 工作项记录: add-build-plugins

工作项标识: add-build-plugins
描述: pom.xml 添加 jacoco（覆盖率）+ spotbugs（静态分析）+ dependency-check（CVE 扫描），合并 backlog 中 add-jacoco-coverage / add-spotbugs-analysis / add-dependency-check
目标分支: main

## 切片门禁

| sub-feature-id | 路径等级 | Spec 门禁 | Review 门禁 |
|---|---|---|---|
| add-build-plugins | fast | skipped | required |

## 切片状态

| sub-feature-id | 状态 | 后续步骤 |
|---|---|---|
| add-build-plugins | done | — |

## 进度笔记

- 2026-08-07 登记。合并 backlog 中 add-jacoco-coverage / add-spotbugs-analysis / add-dependency-check 三项（均无 features 产物，仅 manager 记录）。
- 2026-08-11：实施于 commit `257b7d8`（pom.xml 添加 JaCoCo 0.8.12 + SpotBugs 4.8.6.4 + dependency-check 10.0.4）。Review Approve、QA Pass（323 tests, 0 failures；SpotBugs 0 bugs）。三项被合并的 backlog 工作项标记 `cancelled` 并归档。
