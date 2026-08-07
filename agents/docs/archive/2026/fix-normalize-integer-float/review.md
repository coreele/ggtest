# Review: fix-normalize-integer-float

## 审阅范围

- 工作项：`fix-normalize-integer-float`（未拆分；fast；Review 门禁 required）
- 源分支：`fix-normalize-integer-float`；实现版本：相对 `HEAD`=`e110f5f` 的未提交 diff（本轮无独有 commit）
- 依据：[`plan.md`](./plan.md)（approved；A1–A5）、[`dev-notes.md`](./dev-notes.md)；归档 [`ggtest-core-normalize/spec.md`](../ggtest-core/ggtest-core-normalize/spec.md)（I：`%d`；无法解释 → `0`）；`documentation.md` / `quality.md` / `security.md` / `git.md`
- Spec / Design：N/A（skipped）
- 审阅改动：`ValueNormalizer.java`、`ValueNormalizerTest.java`、本目录 `dev-notes.md`
- 排除：WI-2（~480/484）、WI-3（~491）；整文件 `failed≠0` 不阻塞本项
- 工作区同在、不计入本项实现：`pom.xml`（compiler pin）、`agents/docs/manager/*`、`STATUS.md`、未跟踪 `sqllogictest/`

## 结论

**Approve**

无阻塞项。A1–A5 与 Plan T0–T4 满足进入 QA 条件。

## 必修项

| ID | 位置 | 问题 | 状态 |
|---|---|---|---|
| — | — | 无 | N/A |

## A1–A5 逐项

| ID | 要求 | 证据 | 结果 |
|---|---|---|---|
| A1 | 浮点样 `"1.0"`/`"5.0"` → 对应整数文本 | `integerFloatLikeTruncatesTowardZero`；`ValueNormalizerTest` 11/0/0；直调 `"1.0"→1`、`"5.0"→5` | 通过 |
| A2 | 向零截断（含负值） | `"1.25"→1`、`"-1.9"→"-1"`；`(long) double` 向零 | 通过 |
| A3 | 非数值 → `"0"`；纯整数 / NULL 不回归 | `"abc"→0`；`"42"`/`"-7"`；`null→NULL`；NaN/Infinity→`"0"`（直调） | 通过 |
| A4 | `slt_lang_aggfunc` WI-1 位点消失 | `./bin/ggtest …/slt_lang_aggfunc.test`：仅剩 ~480/484 overflow、~491 精度；无 ~43/~86/~380/~390 的 `0` mismatch / label 冲突 | 通过 |
| A5 | 范围仅本缺陷；WI-2/WI-3 可仍失败 | WI diff 限 normalizer + 测试 + 本目录文档；notes 标明仍失败位点 | 通过 |

## 实现正确性

| 检查 | 结果 |
|---|---|
| `%d` / 向零截断 | `Double.parseDouble` → NaN/Infinity→`"0"` → `(long)` 截断 → `Long.toString`；对齐归档 I 合同 |
| `"1.5"` 期望纠正 | 自 `integerUnparseableBecomesZero` 移除，并入截断用例 → `"1"` |
| TDD | 无 red 提交可复验（禁止 commit）；notes 记 T1 Fail `expected: <1> but was: <0>`；采信 |
| 未越界 | 未改 R/T、runner、JDBC、ResultComparer；未实施 WI-2/WI-3 |
| 分支 | `fix-normalize-integer-float`（非 main） |

## 测试有效性

- T1 覆盖浮点样、向零（含负）、纯整数、非数值、`"1.5"` 纠正；断言可因错误实现失败。
- L2 复跑：`ValueNormalizerTest` 11/0/0；`mvn test` → 228/0/0/skipped=16（notes 记 18，环境门控差异）。
- A4 语料冒烟独立复跑：WI-1 消失。
- 非阻塞缺口：无 NaN/Infinity 单测（行为已实现并直调确认）。

## 文档影响核对

| Plan 声明 | 一致 | 备注 |
|---|---|---|
| 开发文档：`dev-notes.md`；必要时 Javadoc | 是 | red→green、L2、A4、WI-2/WI-3 仍失败；Javadoc 已更新 |
| 用户文档 N/A | 是 | 未改 README / 公开合同 |
| 运维文档 N/A | 是 | 无部署/排障变更 |

## 安全影响核对

检查范围：结果值字符串解析与整数格式化。无认证授权、文件写回、出站网络、本项依赖升级、敏感数据新路径。

| 检查项 | 结果 | 备注 |
|---|---|---|
| 敏感信息 | 无发现 | 代码/测试/notes 无凭据 |
| 认证与授权 | N/A | — |
| 输入与外部访问 | 可接受 | 既有比对路径结果字符串；`parseDouble` 非新攻击面放大 |
| 依赖变更 | N/A | 无本项依赖变更；工作区 `pom.xml` pin 勿纳入合入 |
| 处置状态 | 无需处置 | 允许进入 QA |

## Git 合规

| 检查项 | 结果 |
|---|---|
| 工作分支 | `fix-normalize-integer-float`（非 main） |
| 提交 | 实现尚未 commit（符合本轮约束） |
| 禁止提交项 | WI 文件无密钥/`.env`/构建产物 |
| 合入 stage | 仅 `ValueNormalizer.java`、`ValueNormalizerTest.java`、本目录文档；排除 `pom.xml`、Manager 状态、未跟踪 `sqllogictest/` |

## 发现项（按严重程度）

| 级别 | 位置 | 说明 | 处置 |
|---|---|---|---|
| 建议（非阻塞） | 工作区 `pom.xml` | Plan 禁止的 compiler-plugin pin；notes 称未改；工作区脏文件 | 合入勿 stage |
| 建议（非阻塞） | `ValueNormalizerTest` | 无 NaN/Infinity 断言 | 可选补测 |
| 说明 | notes skipped=18 vs 复跑 16 | 环境门控差异 | QA 以本机复跑为准 |

## 后续动作与复审范围

1. Manager 调度 **QA**。
2. QA：复跑 `ValueNormalizerTest`、`mvn clean test`、`./bin/ggtest …/slt_lang_aggfunc.test`；验收 A1–A5；不得因 ~480/484/~491 判 Fail。
3. 无需 Developer 复审，除非 QA Fail。
4. 本报告不 commit（Manager 按 `git.md` §1.4）。

## 修订记录

| 日期 | 摘要 |
|---|---|
| 2026-08-06 | 初审 Approve；A1–A5；独立复跑单测/全量/aggfunc；refine-docs |
