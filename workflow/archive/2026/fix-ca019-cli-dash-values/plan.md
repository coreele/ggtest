# Plan: fix-ca019-cli-dash-values

## 元信息

- 工作项标识: fix-ca019-cli-dash-values（未拆分）
- 路径等级: fast | Review 门禁: required
- 来源: `workflow/audit/2026-08-13-src.md` Findings CA-019（Medium §2）
- 验证命令: `mvn -q -Dtest=CliArgumentParserTest test`、`mvn -q clean test`
- 预期证据: CliArgumentParserTest 全 Pass（含新增 3 测试）；`mvn clean test` 0 failures

## 目标摘要

修复 CA-019：`CliArgumentParser.requireValue` 把任何 `startsWith("-")` 的 token 当作「missing value」，导致 `--password -secret`、`--user -name`、`--env-file -path` 等以 `-` 开头的合法值被拒。密码以 `-` 开头属合理输入。

## Bug 定位

`cli/CliArgumentParser.java:96-101`：
```java
if (index >= args.length || args[index].startsWith("-")) {
    throw new UsageException("missing value for " + option);
}
```

## 修复方案

将「缺失值」判定从「token 以 `-` 开头」收紧为「token 是已知选项 flag」。新增 `OPTION_FLAGS` 常量集合（全部 13 个 flag：`--url/--user/--password/--engine/--hash-threshold/--env-file/--color/--parallel/--halt/--override/--trace/--help/-h`）。`requireValue` 仅在越界或下一 token ∈ OPTION_FLAGS 时报缺失值；其余（含 `-secret`、`-1`、`--bogus` 字面值、`-` 开头路径）一律接受为值。

语义：下一 token 是「已知 flag」= 真正缺值；否则视作值。`--url --user`（`--user` 是 flag）仍判缺值；`--parallel -1` 仍由 `n<1` 检查报错（消息含 parallel）。

## 触碰路径

- `src/main/java/com/ggtest/cli/CliArgumentParser.java`
- `src/test/java/com/ggtest/cli/CliArgumentParserTest.java`

## 验收与验证

| ID | 要求 | 预期证据 |
|---|---|---|
| V1 | `--password -secret` 接受为密码 | 新增测试 Pass |
| V2 | `--user -name` / `--env-file -path` 接受 | 新增测试 Pass |
| V3 | 下一 token 是已知 flag 时仍报缺值 | `--password --user` → missing Pass |
| V4 | `--parallel -1` 仍报错（n<1） | 既有 parallelNegativeYieldsUsageError Pass |
| V5 | `mvn clean test` | BUILD SUCCESS，0 failures |

## 文档影响

开发/用户/运维均 N/A（解析器内部行为修正；README 未承诺「拒 `-` 值」，反而属缺陷修复）。

## 交接顺序

Developer → Reviewer（required）→ QA → 合并授权 → done → 合入 main。

## 修订记录

| 日期 | 摘要 |
|---|---|
| 2026-08-13 | 初版 Plan（来源：2026-08-13 审计 CA-019） |
