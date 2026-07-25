package com.ggtest.runner;

import com.ggtest.db.DatabaseExecutor;
import com.ggtest.db.FatalDatabaseException;
import com.ggtest.db.QueryResult;
import com.ggtest.db.StatementResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * In-memory {@link DatabaseExecutor} used to drive the runner's orchestration
 * without a JDBC driver. Unscripted SQL succeeds (statements) or returns no rows
 * (queries), so tests only declare the behaviour they care about.
 */
final class FakeDatabaseExecutor implements DatabaseExecutor {

    private final String engineName;
    private final List<String> executedSql = new ArrayList<>();
    private final Map<String, StatementResult> statementResults = new HashMap<>();
    private final Map<String, QueryResult> queryResults = new HashMap<>();
    private final Set<String> fatalSql = new HashSet<>();

    FakeDatabaseExecutor() {
        this("sqlite");
    }

    FakeDatabaseExecutor(String engineName) {
        this.engineName = engineName;
    }

    FakeDatabaseExecutor statementFails(String sql, String errorSummary) {
        statementResults.put(sql, StatementResult.failed(errorSummary));
        return this;
    }

    FakeDatabaseExecutor queryReturns(String sql, List<List<String>> rows) {
        queryResults.put(sql, QueryResult.succeeded(rows));
        return this;
    }

    FakeDatabaseExecutor queryFails(String sql, String errorSummary) {
        queryResults.put(sql, QueryResult.failed(errorSummary));
        return this;
    }

    FakeDatabaseExecutor fatalOn(String sql) {
        fatalSql.add(sql);
        return this;
    }

    List<String> executedSql() {
        return List.copyOf(executedSql);
    }

    @Override
    public String engineName() {
        return engineName;
    }

    @Override
    public StatementResult executeStatement(String sql) {
        record(sql);
        return statementResults.getOrDefault(sql, StatementResult.ok());
    }

    @Override
    public QueryResult executeQuery(String sql) {
        record(sql);
        return queryResults.getOrDefault(sql, QueryResult.succeeded(List.of()));
    }

    private void record(String sql) {
        executedSql.add(sql);
        if (fatalSql.contains(sql)) {
            throw new FatalDatabaseException("connection closed while executing");
        }
    }
}
