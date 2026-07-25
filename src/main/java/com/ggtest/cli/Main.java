package com.ggtest.cli;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;

/**
 * Command-line entry point for GGTEST ({@code ggtest}).
 *
 * <p>Parses arguments, merges optional {@code .env} / process environment,
 * collects {@code *.test}/{@code *.slt} inputs, runs them through the parser →
 * JDBC executor → record runner pipeline, prints a plain-text report, and
 * returns exit code 0 (all passed), 1 (assertion failures), or 2 (usage /
 * parse / connection / fatal errors). Credentials are never written to the
 * report.
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
     * @param args raw argv
     * @param out  report stream (stdout)
     * @param err  usage / connection error stream (stderr)
     * @return exit code 0, 1, or 2
     */
    public static int run(String[] args, PrintStream out, PrintStream err) {
        try {
            ParsedArguments parsed = CliArgumentParser.parse(args);
            CliOptions options = RuntimeConfigResolver.resolve(
                    parsed, System::getenv, Path.of("").toAbsolutePath());
            List<Path> files = TestFileCollector.collect(options.inputs());
            return new CliSession(options, out, err).execute(files);
        } catch (UsageException ex) {
            err.println(ex.getMessage());
            return 2;
        }
    }
}
