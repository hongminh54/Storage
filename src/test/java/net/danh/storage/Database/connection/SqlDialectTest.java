package net.danh.storage.Database.connection;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlDialectTest {

    private static final String[] COLS = {"player", "data", "max", "autopickup"};
    private static final String[] KEYS = {"player"};

    @Test
    void mysqlAndMariadbUseOnDuplicateKey() {
        for (SqlDialect dialect : new SqlDialect[]{SqlDialect.MYSQL, SqlDialect.MARIADB}) {
            String sql = dialect.upsert("PlayerData", COLS, KEYS);
            assertTrue(sql.startsWith("INSERT INTO PlayerData (player, data, max, autopickup) VALUES (?, ?, ?, ?)"), sql);
            assertTrue(sql.contains("ON DUPLICATE KEY UPDATE"), sql);
            assertTrue(sql.contains("data = VALUES(data)"), sql);
            assertTrue(sql.contains("autopickup = VALUES(autopickup)"), sql);
            assertFalse(sql.contains("player = VALUES(player)"), "key column must not be updated: " + sql);
        }
    }

    @Test
    void postgresUsesOnConflictExcluded() {
        String sql = SqlDialect.POSTGRESQL.upsert("PlayerData", COLS, KEYS);
        assertTrue(sql.contains("ON CONFLICT (player) DO UPDATE SET"), sql);
        assertTrue(sql.contains("data = EXCLUDED.data"), sql);
        assertFalse(sql.contains("player = EXCLUDED.player"), sql);
    }

    @Test
    void h2UsesMergeKey() {
        String sql = SqlDialect.H2.upsert("PlayerData", COLS, KEYS);
        assertTrue(sql.startsWith("MERGE INTO PlayerData (player, data, max, autopickup) KEY(player) VALUES (?, ?, ?, ?)"), sql);
    }

    @Test
    void sqliteUsesInsertOrReplace() {
        String sql = SqlDialect.SQLITE.upsert("PlayerData", COLS, KEYS);
        assertTrue(sql.startsWith("INSERT OR REPLACE INTO PlayerData (player, data, max, autopickup) VALUES (?, ?, ?, ?)"), sql);
    }

    @Test
    void insertIgnorePerDialect() {
        assertTrue(SqlDialect.MYSQL.insertIgnore("StorageFriends", COLS, KEYS).startsWith("INSERT IGNORE INTO"));
        assertTrue(SqlDialect.MARIADB.insertIgnore("StorageFriends", COLS, KEYS).startsWith("INSERT IGNORE INTO"));
        assertTrue(SqlDialect.SQLITE.insertIgnore("StorageFriends", COLS, KEYS).startsWith("INSERT OR IGNORE INTO"));
        assertTrue(SqlDialect.POSTGRESQL.insertIgnore("StorageFriends", COLS, KEYS).contains("ON CONFLICT (player) DO NOTHING"));
        assertTrue(SqlDialect.H2.insertIgnore("StorageFriends", COLS, KEYS).startsWith("MERGE INTO"));
    }

    @Test
    void schemaResourcePathsResolve() {
        for (SqlDialect dialect : SqlDialect.values()) {
            String resource = dialect.getSchemaResource();
            assertTrue(resource.startsWith("/schema/") && resource.endsWith(".sql"), resource);
            assertNotNull(getClass().getResourceAsStream(resource), "missing schema resource: " + resource);
        }
    }
}