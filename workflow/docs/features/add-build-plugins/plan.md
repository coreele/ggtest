# Plan: add-build-plugins

## 元信息
- 路径等级: fast
- Review 门禁: required

## 策略
- JaCoCo：绑定 `prepare-agent` + `report`，随 `mvn test` 自动收集覆盖率
- SpotBugs：绑定 `check` 到 `verify` 阶段（不拖慢 `mvn test`）
- Dependency-check：绑定 `check` 到 `verify` 阶段（CVE ≥9 才 fail）

## 任务
1. pom.xml `<properties>` 加版本号
2. pom.xml `<plugins>` 加三个插件
3. `mvn test` 验证 JaCoCo 报告生成
4. `mvn verify` 验证 SpotBugs + dependency-check 执行

## 验收
- `mvn test`: 323 tests, 0 failures, `target/site/jacoco/index.html` 存在
- `mvn verify`: SpotBugs 无 HIGH/CRITICAL bug，dependency-check 无 CVE≥9
