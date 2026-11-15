package net.danh.storage.Database;

import net.danh.storage.Database.connection.SqlDialect;
import net.danh.storage.Storage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public final class SchemaReader {

    private SchemaReader() {
    }

    public static void applySchema(Storage plugin, Connection connection, SqlDialect dialect, String prefix)
            throws SQLException, IOException {
        List<String> statements = readStatements(plugin, dialect, prefix);
        try (Statement statement = connection.createStatement()) {
            for (String sql : statements) {
                try {
                    statement.execute(sql);
                } catch (SQLException ex) {
                    // indexes may already exist on databases created by older plugin versions
                    if (sql.regionMatches(true, 0, "CREATE INDEX", 0, 12) && isAlreadyExists(ex)) {
                        plugin.getLogger().log(Level.FINE, "Index already exists, skipping", ex);
                        continue;
                    }
                    throw ex;
                }
            }
        }
    }

    public static List<String> readStatements(Storage plugin, SqlDialect dialect, String prefix) throws IOException {
        List<String> statements = new ArrayList<>();
        try (InputStream in = plugin.getResource(dialect.getSchemaResource().substring(1))) {
            if (in == null) {
                throw new IOException("Missing schema resource: " + dialect.getSchemaResource());
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                    continue;
                }
                builder.append(line.replace("{prefix}", prefix)).append('\n');
                if (trimmed.endsWith(";")) {
                    String sql = builder.toString().trim();
                    statements.add(sql.substring(0, sql.length() - 1).trim());
                    builder.setLength(0);
                }
            }
        }
        return statements;
    }

    private static boolean isAlreadyExists(SQLException ex) {
        String message = ex.getMessage();
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase();
        return lower.contains("already exists") || lower.contains("duplicate key name");
    }
}