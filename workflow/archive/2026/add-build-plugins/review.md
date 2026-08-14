# Review: add-build-plugins

## 审阅

### pom.xml 变更
- JaCoCo 0.8.12：`prepare-agent` + `report`（test 阶段），`mvn test` 自动生成 `target/site/jacoco/`
- SpotBugs 4.8.6.4：`check` 绑定 verify 阶段，threshold=High，effort=Max
- OWASP dependency-check 10.0.4：`check` 绑定 verify 阶段，failBuildOnCVSS=9

### 验证
- `mvn test`: 323 tests, 0 failures；`target/site/jacoco/index.html` 生成 ✓
- `mvn spotbugs:check`: BugInstance size=0, No errors/warnings found ✓
- SpotBugs/dep-check 绑定 verify 不拖慢 `mvn test`

### 分阶段策略
| 命令 | 执行 |
|---|---|
| `mvn test` | 编译 + 测试 + JaCoCo 报告 |
| `mvn verify` | + SpotBugs + dependency-check |

## 结论: Approve
