package com.ggtest.cli;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;

/**
 * Command-line entry point for GGTEST ({@code ggtest}).
 *
 * <p>Parses arguments, collects {@code *.test}/{@code *.slt} inputs, runs them
 * through the parser → SQLite JDBC executor → record runner pipeline, prints a
 * plain-text report, and returns exit code 0 (all passed), 1 (assertion
 * failures), or 2 (usage / parse / connection / fatal errors). Credentials
 * passed via {@code --password} are never written to the report.
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
            CliOptions options = CliArgumentParser.parse(args);
            List<Path> files = TestFileCollector.collect(options.inputs());
            return new CliSession(options, out, err).execute(files);
        } catch (UsageException ex) {
            err.println(ex.getMessage());
            return 2;
        }
    }
}
