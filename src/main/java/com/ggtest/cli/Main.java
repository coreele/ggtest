package com.ggtest.cli;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

/**
 * Command-line entry point for GGTEST ({@code ggtest}).
 *
 * <p>Parses arguments, merges optional {@code .env} / process environment,
 * collects {@code *.test}/{@code *.slt} inputs, runs them through the parser →
 * JDBC executor → record runner pipeline, prints a human-readable report, and
 * returns exit code 0 (all passed), 1 (assertion failures), or 2 (usage /
 * parse / connection / fatal errors). Credentials are never written to the
 * report.
 *
 * <p>Report color: {@code --color} &gt; {@code -Dggtest.color} &gt;
 * {@code GGTEST_COLOR} &gt; default {@code auto} (TTY only).
 */
public final class Main {

    private Main() {}

    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    /**
     * Programmatic entry used by tests and thin launch scripts.
     *
     * @param args raw argv
     * @return exit code 0, 1, or 2
     */
    public static int run(String[] args) {
        return run(args, System.out, System.err);
    }

    /**
     * Product default: process environment via {@link System#getenv(String)} and
     * the process current working directory for the default {@code .env} path.
     *
     * @param args raw argv
     * @param out  report stream (stdout)
     * @param err  usage / connection error stream (stderr)
     * @return exit code 0, 1, or 2
     */
    public static int run(String[] args, PrintStream out, PrintStream err) {
        return run(args, out, err, System::getenv, Path.of("").toAbsolutePath(), System::getProperty);
    }

    /**
     * Injectable entry for tests that must isolate process {@code GGTEST_*} and
     * a repo-root {@code .env}. Product {@link #main} / the three-arg
     * {@link #run(String[], PrintStream, PrintStream)} keep the default
     * {@code System::getenv} + process CWD contract.
     *
     * @param args             raw argv
     * @param out              report stream (stdout)
     * @param err              usage / connection error stream (stderr)
     * @param envLookup        process environment reader (whitelist keys only)
     * @param workingDirectory directory used for the default {@code .env} path
     * @return exit code 0, 1, or 2
     */
    public static int run(
            String[] args,
            PrintStream out,
            PrintStream err,
            Function<String, String> envLookup,
            Path workingDirectory) {
        return run(args, out, err, envLookup, workingDirectory, System::getProperty);
    }

    /**
     * Fully injectable entry for color priority tests ({@code GGTEST_COLOR} /
     * {@code ggtest.color}). Product TTY detection defaults to
     * {@code System.console() != null}.
     *
     * @param propertyLookup system property reader (key {@link RuntimeConfigResolver#COLOR_PROPERTY})
     */
    public static int run(
            String[] args,
            PrintStream out,
            PrintStream err,
            Function<String, String> envLookup,
            Path workingDirectory,
            Function<String, String> propertyLookup) {
        return run(args, out, err, envLookup, workingDirectory, propertyLookup, () -> System.console() != null);
    }

    /**
     * Same as {@link #run(String[], PrintStream, PrintStream, Function, Path, Function)} with an
     * injectable TTY probe for {@code --color auto} tests. Product callers keep
     * {@code System.console() != null} via the six-arg overload.
     *
     * @param isTty whether stdout is treated as a TTY ({@code auto} enables ANSI when true)
     */
    public static int run(
            String[] args,
            PrintStream out,
            PrintStream err,
            Function<String, String> envLookup,
            Path workingDirectory,
            Function<String, String> propertyLookup,
            BooleanSupplier isTty) {
        try {
            ParsedArguments parsed = CliArgumentParser.parse(args);
            if (parsed.help()) {
                printHelp(out);
                return 0;
            }
            CliOptions options = RuntimeConfigResolver.resolve(parsed, envLookup, workingDirectory, propertyLookup);
            boolean ansi = RuntimeConfigResolver.resolveAnsiEnabled(
                    options.colorMode(), isTty.getAsBoolean());
            List<Path> files = TestFileCollector.collect(options.inputs());
            return new CliSession(options, out, err, ansi).execute(files);
        } catch (UsageException ex) {
            printUsageError(err, ex.getMessage());
            return 2;
        }
    }

    /** Multi-line usage/config hard-error style (no success TOTAL). */
    static void printUsageError(PrintStream err, String message) {
        err.println("Error: usage");
        err.println("    [WHY] " + (message == null ? "" : message.strip()));
    }

    static void printHelp(PrintStream out) {
        out.println("Usage: ggtest [options] <file-or-dir>...");
        out.println();
        out.println("Options:");
        out.println("  --url <jdbc-url>            JDBC connection URL (or GGTEST_URL)");
        out.println("  --user <name>               Database user (or GGTEST_USER)");
        out.println("  --password <pass>           Database password (or GGTEST_PASSWORD)");
        out.println("  --engine <sqlite|postgres>  Target engine (default: sqlite; or GGTEST_ENGINE)");
        out.println("  --hash-threshold <n>        Hash result sets larger than n rows (default: 8)");
        out.println("  --color <auto|always|never> ANSI color mode (or GGTEST_COLOR / -Dggtest.color)");
        out.println("  --halt                      Stop after first failure");
        out.println("  --parallel <N>              Run at most N files concurrently (1 = sequential)");
        out.println("  --override                  Write actual output into expected-result blocks");
        out.println("  --trace                     Print each SQL statement to stderr as it executes");
        out.println("  --env-file <path>           Load config from a .env file (default: ./.env)");
        out.println("  --help, -h                  Print this help and exit");
        out.println();
        out.println("Environment variables: GGTEST_URL, GGTEST_USER, GGTEST_PASSWORD,");
        out.println("  GGTEST_ENGINE, GGTEST_HASH_THRESHOLD, GGTEST_COLOR");
        out.println();
        out.println("See README.md for sqllogictest format details.");
    }
}
