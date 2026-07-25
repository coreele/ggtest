# GGTEST

Java CLI for running [sqllogictest](https://www.sqlite.org/sqllogictest)-format test
files against SQLite via JDBC (Maven, Java 17). Command name: **`ggtest`**.

## Prerequisites

- JDK 17+
- Apache Maven 3.8+

## Build, test, and package

```bash
mvn -q clean test
mvn -q clean package
```

`mvn package` produces an executable uber-JAR under `target/ggtest-*.jar`
(`Main-Class` = `com.ggtest.cli.Main`).

## Run (`ggtest`)

After packaging:

```bash
./bin/ggtest --url jdbc:sqlite::memory: path/to/file.test
# or
java -jar target/ggtest-0.1.0-SNAPSHOT.jar --url jdbc:sqlite::memory: path/to/file.test
```

### CLI

```text
ggtest --url <jdbc-url> [--user <user>] [--password <password>]
       [--engine <name>=sqlite] [--hash-threshold <N>]
       <file-or-dir> [<file-or-dir> ...]
```

| Option | Default | Notes |
|---|---|---|
| `--url` | (required) | SQLite JDBC URL, e.g. `jdbc:sqlite::memory:` or `jdbc:sqlite:/tmp/t.db` |
| `--user` / `--password` | none | Optional DB credentials; **never** written to logs or the report |
| `--engine` | `sqlite` | Used by `skipif` / `onlyif`; only `sqlite` is accepted in this release |
| `--hash-threshold` | `8` | Initial hash threshold per file; file-level `hash-threshold` still applies inside a file |

Positional arguments: at least one file or directory.

- **File**: any extension is accepted; content must be valid sqllogictest.
- **Directory**: recursively collects `*.test` and `*.slt` (stable absolute-path order).

### Exit codes

| Code | Meaning |
|---|---|
| `0` | All assertable records passed |
| `1` | At least one assertion failure |
| `2` | Usage / config / parse / connection / fatal error |

### Report

Stdout is plain text: per-file `passed` / `failed` / `skipped`, a `TOTAL` line,
and `FAILURE` lines with file, line, SQL first-line summary, and reason. Parse
and connection problems are reported as `ERROR` (exit code 2); remaining files
still run after a parse error.

### Official corpus (user-supplied)

Official sqllogictest corpora are **not** shipped in this repository. Point
`ggtest` at your local copies, for example:

```bash
ggtest --url jdbc:sqlite::memory: /path/to/select1.test
ggtest --url jdbc:sqlite::memory: /path/to/select1.test /path/to/select2.test /path/to/select3.test
```

Optional automated hard acceptance: set `GGTEST_CORPUS_DIR` to a directory
containing `select1.test` / `select2.test` / `select3.test`, then `mvn test`.

## Library usage

The CLI assembles the same APIs you can call from Java.

### Parser

```java
import com.ggtest.parser.SqlLogicTestParser;
import com.ggtest.model.SqlTestRecord;
import java.nio.file.Path;
import java.util.List;

SqlLogicTestParser parser = new SqlLogicTestParser();
List<SqlTestRecord> records = parser.parse(Path.of("sample.test"));
```

Malformed input throws `com.ggtest.parser.ParseException` with message
`<sourceName>:<lineNumber>: <reason>`.

### Normalize / compare

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

### Runner

```java
import com.ggtest.db.sqlite.SqliteJdbcExecutor;
import com.ggtest.parser.SqlLogicTestParser;
import com.ggtest.runner.FileRunResult;
import com.ggtest.runner.SqlLogicTestRunner;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;

var records = new SqlLogicTestParser().parse(Path.of("sample.test"));
try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
    FileRunResult result = new SqlLogicTestRunner(new SqliteJdbcExecutor(connection)).run(records);
}
```

Per-file state (hash-threshold, conditions, labels) is scoped to a single
`run` call. Supporting another database: implement `com.ggtest.db.DatabaseExecutor`.
