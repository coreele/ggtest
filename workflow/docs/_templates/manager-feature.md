# 工作项记录: <feature-id>

工作项标识: <feature-id>
描述:
目标分支: main
文档影响:

> 权威流程见 [workflow/README.md](../../README.md)；活跃状态见 [STATUS.md](STATUS.md)。
>
> **切片级：** 路径等级、源分支、门禁、状态、阻塞。**工作项级：** 目标分支。
> 未拆分：产物在 `workflow/docs/features/<feature-id>/`。已拆分：根目录仅总览 Spec；切片在 `<feature-id>-<sub>/`。
> 归档后本文件迁至 `workflow/docs/archive/YYYY/<feature-id>/manager.md`（须修正相对链接）；`workflow/docs/manager/` 仅保留活跃项与 STATUS。
>
> 表内只填枚举、短标签或链接；较长理由写入「进度笔记」（见 `workflow/docs/standards/documentation.md` §B）。

## 切片门禁（未拆分时一行，sub-feature-id = feature-id）

| sub-feature-id | 路径等级 | 源分支 | Spec | Spec 门禁 | Spec 用户确认 | Design 门禁 | Review 门禁 |
|---|---|---|---|---|---|---|---|
| <feature-id> | fast \| standard \| full | <feature-id> | [spec.md](./../features/%3Cfeature-id%3E/spec.md) | required \| skipped | required \| not-required \| approved \| rejected | required \| skipped | required \| skipped |

> 总览行：路径等级与门禁、源分支均可 `N/A`。`Review=skipped` 仅 `fast`；理由写进度笔记。

## 切片状态

| sub-feature-id | 状态 | 后续步骤 | 阻塞原因 | 恢复条件 | 恢复后目标 |
|---|---|---|---|---|---|
| <feature-id> | backlog | | | | |

> 无阻塞则后三列留空。长说明优先进度笔记。

## 进度笔记

-
