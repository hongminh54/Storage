package net.danh.storage.Database;

import net.danh.storage.Database.connection.ConnectionFactory;
import net.danh.storage.Database.connection.SqlDialect;
import net.danh.storage.Storage;
import net.danh.storage.Utils.File;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class Database {

    public final String table;
    private final ConnectionFactory connectionFactory;

    public Database(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
        this.table = applyPrefix("PlayerData");
    }

    public static String applyPrefix(String tableName) {
        return File.getConfig().getString("database.table-prefix", "") + tableName;
    }

    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    public Connection getSQLConnection() {
        try {
            return connectionFactory.getConnection();
        } catch (SQLException ex) {
            Storage.getStorage().getLogger().log(Level.SEVERE, Errors.noSQLConnection(), ex);
            return null;
        }
    }

    public SqlDialect getDialect() {
        return connectionFactory.getDialect();
    }

    public boolean isSqlBacked() {
        return true;
    }

    public void load() {
        try {
            connectionFactory.init(Storage.getStorage());
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to initialize database connection", ex);
        }
        try (Connection connection = getSQLConnection()) {
            if (connection == null) {
                throw new IllegalStateException("Unable to open a database connection");
            }
            String prefix = File.getConfig().getString("database.table-prefix", "");
            SchemaReader.applySchema(Storage.getStorage(), connection, getDialect(), prefix);
            migrateAutoPickupColumn(connection);
        } catch (Exception ex) {
            Storage.getStorage().getLogger().log(Level.SEVERE, "Failed to initialize database schema", ex);
        }
    }

    private void migrateAutoPickupColumn(Connection connection) throws SQLException {
        if (columnExists(connection, table, "autopickup")) {
            return;
        }
        try (PreparedStatement ps = connection.prepareStatement(
                "ALTER TABLE " + table + " ADD COLUMN autopickup BOOLEAN DEFAULT FALSE")) {
            ps.executeUpdate();
        }
        Storage.getStorage().getLogger().info("Added autopickup column to existing " + table + " table");
    }

    private boolean columnExists(Connection connection, String tableName, String column) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        // unquoted identifiers fold to lower case on PostgreSQL and upper case on H2
        for (String candidate : new String[]{tableName, tableName.toLowerCase(), tableName.toUpperCase()}) {
            try (ResultSet rs = metaData.getColumns(null, null, candidate, null)) {
                while (rs.next()) {
                    if (column.equalsIgnoreCase(rs.getString("COLUMN_NAME"))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public PlayerData getData(String player) {
        try (Connection conn = getSQLConnection()) {
            if (conn == null) return null;
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM " + table + " WHERE player = ?")) {
                ps.setString(1, player);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return null;
                    }
                    return mapRow(rs);
                }
            }
        } catch (SQLException ex) {
            Storage.getStorage().getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
            return null;
        }
    }

    public List<PlayerData> getAllData() {
        List<PlayerData> result = new ArrayList<>();
        try (Connection conn = getSQLConnection()) {
            if (conn == null) return result;
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM " + table);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        } catch (SQLException ex) {
            Storage.getStorage().getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
        }
        return result;
    }

    private PlayerData mapRow(ResultSet rs) throws SQLException {
        boolean autoPickup = false;
        try {
            autoPickup = rs.getBoolean("autopickup");
        } catch (SQLException ignored) {
        }
        return new PlayerData(rs.getString("player"), rs.getString("data"), rs.getInt("max"), autoPickup);
    }

    public void createTable(@NotNull PlayerData playerData) {
        try (Connection conn = getSQLConnection()) {
            if (conn == null) return;
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO " + table + " (player,data,max,autopickup) VALUES(?,?,?,?)")) {
                ps.setString(1, playerData.player());
                ps.setString(2, playerData.data());
                ps.setInt(3, playerData.max());
                ps.setBoolean(4, playerData.autoPickup());
                ps.executeUpdate();
            }
        } catch (SQLException ex) {
            Storage.getStorage().getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
        }
    }

    public void updateTable(@NotNull PlayerData playerData) {
        try (Connection conn = getSQLConnection()) {
            if (conn == null) return;
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE " + table + " SET data = ?, max = ?, autopickup = ? WHERE player = ?")) {
                ps.setString(1, playerData.data());
                ps.setInt(2, playerData.max());
                ps.setBoolean(3, playerData.autoPickup());
                ps.setString(4, playerData.player());
                ps.executeUpdate();
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            Storage.getStorage().getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
        }
    }

    public void bulkSave(@NotNull List<PlayerData> entries) {
        if (entries.isEmpty()) {
            return;
        }
        String sql = getDialect().upsert(table,
                new String[]{"player", "data", "max", "autopickup"}, new String[]{"player"});
        try (Connection conn = getSQLConnection()) {
            if (conn == null) return;
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                int pending = 0;
                for (PlayerData entry : entries) {
                    ps.setString(1, entry.player());
                    ps.setString(2, entry.data());
                    ps.setInt(3, entry.max());
                    ps.setBoolean(4, entry.autoPickup());
                    ps.addBatch();
                    if (++pending % 100 == 0) {
                        ps.executeBatch();
                        conn.commit();
                    }
                }
                if (pending % 100 != 0) {
                    ps.executeBatch();
                }
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            Storage.getStorage().getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
        }
    }

    public void deleteData(String player) {
        try (Connection conn = getSQLConnection()) {
            if (conn == null) return;
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM " + table + " WHERE player = ?")) {
                ps.setString(1, player);
                ps.executeUpdate();
            }
        } catch (SQLException ex) {
            Storage.getStorage().getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
        }
    }

    public void close() {
        connectionFactory.shutdown();
    }
}