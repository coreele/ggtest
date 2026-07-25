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

## Runner usage

`SqlLogicTestRunner` executes one file's records in order against a
`com.ggtest.db.DatabaseExecutor`. The first shipped executor is
`SqliteJdbcExecutor` (`org.xerial:sqlite-jdbc`); the connection is created and
closed by the caller:

```java
import com.ggtest.db.sqlite.SqliteJdbcExecutor;
import com.ggtest.model.SqlTestRecord;
import com.ggtest.parser.SqlLogicTestParser;
import com.ggtest.runner.FileRunResult;
import com.ggtest.runner.RecordOutcome;
import com.ggtest.runner.SqlLogicTestRunner;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

List<SqlTestRecord> records = new SqlLogicTestParser().parse(Path.of("sample.test"));

try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
    FileRunResult result = new SqlLogicTestRunner(new SqliteJdbcExecutor(connection)).run(records);

    System.out.printf(
            "passed=%d failed=%d skipped=%d%n",
            result.passedCount(), result.failedCount(), result.skippedCount());
    result.recordResults().stream()
            .filter(r -> r.outcome() == RecordOutcome.FAILED)
            .forEach(r -> System.err.println(r.location() + ": " + r.failureReason()));
}
```

Behavior of one run:

- `statement ok` / `statement error` assert success / failure only; error messages
  are not matched.
- `query` compares through `ResultComparer` when an expected block is present,
  and asserts execution only when the `----` separator is absent.
- `skipif <db>` / `onlyif <db>` guard the next record and are matched
  case-insensitively against `DatabaseExecutor.engineName()` (`sqlite`).
- `hash-threshold <N>` updates the threshold for the rest of the run; pass a
  different initial value to the two-argument constructor.
- `halt` stops the file; remaining records are reported as skipped.
- A failing record does not stop the file. A `FatalDatabaseException` (closed or
  broken connection) aborts it, and `FileRunResult.aborted()` reports that.
- Per-file state is scoped to a single `run` call, so the next file starts clean.

## Supporting another database

Implement `com.ggtest.db.DatabaseExecutor` in its own package and pass it to the
runner: report `engineName()`, execute statements and queries, return raw column
values (`null` for SQL NULL), and throw `FatalDatabaseException` for
connection-level failures. Parser, normalize, and runner code stays unchanged.
