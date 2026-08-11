# Plan: fix-override-rowwise

## 元信息

- 工作项标识: fix-override-rowwise
- 路径等级: fast
- Review 门禁: skipped
- 验证命令: `mvn test`
- 预期证据: 321 tests, 0 failures

## 目标摘要

修复 `--override` 对 row-wise 查询写入 flat values 的 bug；改善 separator 拼写错误提示。

## 任务拆解

1. `SqlLogicTestRunner`: 新增 `formatOverrideText()` 按 columnSeparator 拼接 row-wise 格式；hash 行（单元素）跳过重整
2. `SqlLogicTestParser`: 新增 `suggestKey()` + `editDistance()`，对拼写接近 known key 的 token 给出建议
3. 手动验证 `--override` 写入 `1|1|hello world` 正确格式
4. 手动验证 `seperator=` `seperator |` 提示 "did you mean separator?"

## 触碰路径

| 文件 | 操作 |
|---|---|
| `runner/SqlLogicTestRunner.java` | 新增 formatOverrideText |
| `parser/SqlLogicTestParser.java` | 新增 suggestKey, editDistance; 更新错误消息 |

## 验收

| ID | 要求 | 证据 |
|---|---|---|
| V1 | `mvn test` | 321 tests, 0 failures |
| V2 | `--override` row-wise 格式正确 | 写入 `1\|1\|hello world` |
| V3 | `seperator=` 拼写提示 | "did you mean \"separator\"?" |
