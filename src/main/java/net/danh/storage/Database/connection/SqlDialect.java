package net.danh.storage.Database.connection;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum SqlDialect {
    MYSQL("mysql", StatementProcessor.USE_BACKTICKS),
    MARIADB("mariadb", StatementProcessor.USE_BACKTICKS),
    POSTGRESQL("postgresql", StatementProcessor.USE_DOUBLE_QUOTES),
    SQLITE("sqlite", StatementProcessor.USE_BACKTICKS),
    H2("h2", StatementProcessor.USE_BACKTICKS);

    private final String schemaResource;
    private final StatementProcessor statementProcessor;

    SqlDialect(String schemaName, StatementProcessor statementProcessor) {
        this.schemaResource = "/schema/" + schemaName + ".sql";
        this.statementProcessor = statementProcessor;
    }

    public String getSchemaResource() {
        return schemaResource;
    }

    public StatementProcessor getStatementProcessor() {
        return statementProcessor;
    }

    public String upsert(String table, String[] columns, String[] keys) {
        String cols = String.join(", ", columns);
        String placeholders = Arrays.stream(columns).map(c -> "?").collect(Collectors.joining(", "));
        String keyList = String.join(", ", keys);
        String updates = Arrays.stream(columns)
                .filter(c -> !Arrays.asList(keys).contains(c))
                .map(c -> c + " = VALUES(" + c + ")")
                .collect(Collectors.joining(", "));

        switch (this) {
            case MYSQL:
            case MARIADB:
                return "INSERT INTO " + table + " (" + cols + ") VALUES (" + placeholders + ")"
                        + " ON DUPLICATE KEY UPDATE " + updates;
            case POSTGRESQL:
                String pgUpdates = Arrays.stream(columns)
                        .filter(c -> !Arrays.asList(keys).contains(c))
                        .map(c -> c + " = EXCLUDED." + c)
                        .collect(Collectors.joining(", "));
                return "INSERT INTO " + table + " (" + cols + ") VALUES (" + placeholders + ")"
                        + " ON CONFLICT (" + keyList + ") DO UPDATE SET " + pgUpdates;
            case H2:
                return "MERGE INTO " + table + " (" + cols + ") KEY(" + keyList + ") VALUES (" + placeholders + ")";
            case SQLITE:
            default:
                return "INSERT OR REPLACE INTO " + table + " (" + cols + ") VALUES (" + placeholders + ")";
        }
    }

    public String insertIgnore(String table, String[] columns, String[] keys) {
        String cols = String.join(", ", columns);
        String placeholders = Arrays.stream(columns).map(c -> "?").collect(Collectors.joining(", "));
        String keyList = String.join(", ", keys);

        switch (this) {
            case MYSQL:
            case MARIADB:
                return "INSERT IGNORE INTO " + table + " (" + cols + ") VALUES (" + placeholders + ")";
            case POSTGRESQL:
                return "INSERT INTO " + table + " (" + cols + ") VALUES (" + placeholders + ")"
                        + " ON CONFLICT (" + keyList + ") DO NOTHING";
            case H2:
                return "MERGE INTO " + table + " (" + cols + ") KEY(" + keyList + ") VALUES (" + placeholders + ")";
            case SQLITE:
            default:
                return "INSERT OR IGNORE INTO " + table + " (" + cols + ") VALUES (" + placeholders + ")";
        }
    }
}