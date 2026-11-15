package net.danh.storage.Database;

import net.danh.storage.Database.connection.SqlDialect;
import net.danh.storage.Storage;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SchemaReaderTest {

    private static final String[] TABLES = {
            "PlayerData", "TransferLogs", "CropTransferLogs", "MobTransferLogs", "MythicTransferLogs",
            "StorageFriends", "StorageFriendRequests", "StorageFriendLogs",
            "StorageFriendAccessSettings", "StorageFriendActionSettings"
    };

    private Storage mockPlugin() {
        Storage plugin = mock(Storage.class);
        when(plugin.getResource(anyString())).thenAnswer(inv ->
                SchemaReaderTest.class.getResourceAsStream("/" + inv.getArgument(0)));
        when(plugin.getLogger()).thenReturn(Logger.getLogger("schema-test"));
        return plugin;
    }

    @Test
    void parsesEveryDialectAndReplacesPrefix() throws Exception {
        Storage plugin = mockPlugin();
        for (SqlDialect dialect : SqlDialect.values()) {
            List<String> statements = SchemaReader.readStatements(plugin, dialect, "test_");
            assertFalse(statements.isEmpty(), dialect + " produced no statements");

            long tableCount = statements.stream()
                    .filter(s -> s.toUpperCase(Locale.ROOT).startsWith("CREATE TABLE"))
                    .count();
            assertEquals(10, tableCount, dialect + " should define 10 tables");

            for (String sql : statements) {
                assertFalse(sql.contains("{prefix}"), "unreplaced prefix: " + sql);
                assertFalse(sql.trim().startsWith("--"), "comment leaked: " + sql);
                assertFalse(sql.endsWith(";"), "trailing semicolon: " + sql);
            }
            assertTrue(statements.stream().anyMatch(s -> s.contains("test_PlayerData")),
                    dialect + " missing prefixed PlayerData");
        }
    }

    @Test
    void sqliteSchemaCreatesAllTables() throws Exception {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            SchemaReader.applySchema(mockPlugin(), conn, SqlDialect.SQLITE, "");
            assertTablesPresent(conn);
        }
    }

    @Test
    void h2SchemaCreatesAllTables() throws Exception {
        try (Connection conn = DriverManager.getConnection("jdbc:h2:mem:schema;DB_CLOSE_DELAY=-1")) {
            SchemaReader.applySchema(mockPlugin(), conn, SqlDialect.H2, "");
            assertTablesPresent(conn);
        }
    }

    @Test
    void h2MergeUpsertRoundTrip() throws Exception {
        try (Connection conn = DriverManager.getConnection("jdbc:h2:mem:roundtrip;DB_CLOSE_DELAY=-1")) {
            SchemaReader.applySchema(mockPlugin(), conn, SqlDialect.H2, "");
            String upsert = SqlDialect.H2.upsert("PlayerData",
                    new String[]{"player", "data", "max", "autopickup"}, new String[]{"player"});

            write(conn, upsert, "Steve", "{}", 5000, true);
            write(conn, upsert, "Steve", "{\"STONE\":64}", 9000, false);

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT data, max, autopickup FROM PlayerData WHERE player = ?")) {
                ps.setString(1, "Steve");
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals("{\"STONE\":64}", rs.getString("data"));
                    assertEquals(9000, rs.getInt("max"));
                    assertFalse(rs.getBoolean("autopickup"));
                    assertFalse(rs.next(), "MERGE must not create a duplicate row");
                }
            }
        }
    }

    private void write(Connection conn, String sql, String player, String data, int max, boolean autoPickup)
            throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, player);
            ps.setString(2, data);
            ps.setInt(3, max);
            ps.setBoolean(4, autoPickup);
            ps.executeUpdate();
        }
    }

    private void assertTablesPresent(Connection conn) throws Exception {
        Set<String> actual = new HashSet<>();
        DatabaseMetaData metaData = conn.getMetaData();
        try (ResultSet rs = metaData.getTables(null, null, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                actual.add(rs.getString("TABLE_NAME").toUpperCase(Locale.ROOT));
            }
        }
        for (String table : TABLES) {
            assertTrue(actual.contains(table.toUpperCase(Locale.ROOT)),
                    "missing table " + table + "; found " + actual);
        }
    }
}