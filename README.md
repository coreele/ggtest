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

## Parser usage

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

## Normalize / compare usage

Compare expected query results to actual rows (no JDBC). Default
hash-threshold is `ResultComparer.DEFAULT_HASH_THRESHOLD` (8):

```java
import com.ggtest.model.ColumnType;
import com.ggtest.model.SortMode;
import com.ggtest.normalize.ResultComparer;
import java.util.List;

var result = ResultComparer.compare(
        List.of(ColumnType.INTEGER, ColumnType.TEXT),
        SortMode.ROWSORT,
        ResultComparer.DEFAULT_HASH_THRESHOLD,
        "1\n(empty)\n2\nx\n",
        List.of(
                List.of("2", "x"),
                List.of("1", "")));

if (!result.passed()) {
    System.err.println(result.diffSummary());
}
```

Building blocks: `ValueNormalizer`, `ResultSorter`, `ResultHasher`.
