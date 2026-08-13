# 工作项记录: fix-ca017-override-atomic-move

工作项标识: fix-ca017-override-atomic-move
描述: 修复 `OverrideWriter.writeAtomically` 原子移动回退为死代码（CA-017）
目标分支: main

> 权威流程见 [workflow/README.md](../../README.md)；活跃状态见 [STATUS.md](STATUS.md)。

## 切片门禁

| sub-feature-id | 路径等级 | 源分支 | Spec 门禁 | Design 门禁 | Review 门禁 |
|---|---|---|---|---|---|
| fix-ca017-override-atomic-move | fast | fix-ca017-override-atomic-move | skipped | skipped | required |

> 单点正确性修复：`OverrideWriter.writeAtomically` 的 `catch (UnsupportedOperationException)` 永不触发——`Files.move(ATOMIC_MOVE)` 不支持时抛 `AtomicMoveNotSupportedException`（IOException 子类）。范围明确、单文件，故 fast + Spec/Design skipped；与近邻 CA-015/CA-016 修复一致取 Review=required。

## 切片状态

| sub-feature-id | 状态 | 后续步骤 | 阻塞原因 | 恢复条件 | 恢复后目标 |
|---|---|---|---|---|---|
| fix-ca017-override-atomic-move | developing | Reviewer | | | |

## 进度笔记

- 来源：`workflow/docs/audit/2026-08-13-src.md` Findings CA-017。
- 根因与修复方向见 [plan.md](../features/fix-ca017-override-atomic-move/plan.md)。
- 2026-08-13：用户会话确认 Plan（approve）；状态 `awaiting-plan-approval → planned`，源分支 `fix-ca017-override-atomic-move` 已创建并提交 Plan/记录。待调度 Developer。
- 2026-08-13：Developer 实施完成 —— `catch` 改为 `AtomicMoveNotSupportedException` + `FileMover` 注入缝 + 新增回退分支测试；`mvn -Dtest=OverrideWriterTest test`（15/0/0）、`mvn clean test`（360/0/0，34 既有 skip）。状态 `planned → developing`，待调度 Reviewer。
