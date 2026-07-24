# GGTEST

Java implementation of a sqllogictest-format test tool (Maven, Java 17).

## Prerequisites

- JDK 17+
- Apache Maven 3.8+

## Build and test

```bash
mvn -q clean test
```

Optional compile-only check:

```bash
mvn -q clean compile
```

## Parser usage (current slice)

Parse a file (extension is ignored; UTF-8):

```java
import com.ggtest.parser.SqlLogicTestParser;
import com.ggtest.model.SqlTestRecord;
import java.nio.file.Path;
import java.util.List;

SqlLogicTestParser parser = new SqlLogicTestParser();
List<SqlTestRecord> records = parser.parse(Path.of("sample.test"));
```

Parse an in-memory source (logical name used only for error locations):

```java
List<SqlTestRecord> records = parser.parse("sample.test", content);
```

Malformed input throws `com.ggtest.parser.ParseException` with message
`<sourceName>:<lineNumber>: <reason>`.

Record types live in `com.ggtest.model` (`StatementRecord`, `QueryRecord`,
`SkipIfRecord`, `OnlyIfRecord`, `HashThresholdRecord`, `HaltRecord`).
