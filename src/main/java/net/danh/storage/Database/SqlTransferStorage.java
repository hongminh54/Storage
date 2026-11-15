package net.danh.storage.Database;

import net.danh.storage.Storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Generic SQL transfer log store shared by all SQL backends.
 * Table layout: id (auto), sender, receiver, itemColumn, amount, timestamp, status.
 */
public class SqlTransferStorage<T> {

    private final Storage plugin;
    private final String table;
    private final String itemColumn;
    private final RowMapper<T> mapper;
    private final RowBinder<T> binder;
    public SqlTransferStorage(Storage plugin, String table, String itemColumn,
                              RowMapper<T> mapper, RowBinder<T> binder) {
        this.plugin = plugin;
        this.table = Database.applyPrefix(table);
        this.itemColumn = itemColumn;
        this.mapper = mapper;
        this.binder = binder;
    }

    public void insertTransfer(T data) {
        try (Connection conn = Storage.db.getSQLConnection()) {
            if (conn == null) return;
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO " + table
                    + " (sender, receiver, " + itemColumn + ", amount, timestamp, status) VALUES (?, ?, ?, ?, ?, ?)")) {
                binder.bind(ps, data);
                ps.executeUpdate();
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Unable to insert transfer data into " + table, ex);
        }
    }

    public List<T> getTransferHistory(String playerName, int limit, int offset) {
        return query("SELECT * FROM " + table
                + " WHERE sender = ? OR receiver = ? ORDER BY timestamp DESC LIMIT ? OFFSET ?", ps -> {
            ps.setString(1, playerName);
            ps.setString(2, playerName);
            ps.setInt(3, limit);
            ps.setInt(4, offset);
        });
    }

    public int getTotalTransferCount(String playerName) {
        try (Connection conn = Storage.db.getSQLConnection()) {
            if (conn == null) return 0;
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM " + table
                    + " WHERE sender = ? OR receiver = ?")) {
                ps.setString(1, playerName);
                ps.setString(2, playerName);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Unable to get transfer count from " + table, ex);
        }
        return 0;
    }

    public List<T> getAllTransferHistory(int limit) {
        return query("SELECT * FROM " + table + " ORDER BY timestamp DESC LIMIT ?", ps -> ps.setInt(1, limit));
    }

    public long getTotalSentAmount(String playerName) {
        return getTotalAmount("sender", playerName);
    }

    public long getTotalReceivedAmount(String playerName) {
        return getTotalAmount("receiver", playerName);
    }

    private long getTotalAmount(String column, String playerName) {
        try (Connection conn = Storage.db.getSQLConnection()) {
            if (conn == null) return 0;
            try (PreparedStatement ps = conn.prepareStatement("SELECT COALESCE(SUM(amount), 0) FROM " + table
                    + " WHERE " + column + " = ? AND status LIKE 'SUCCESS%'")) {
                ps.setString(1, playerName);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getLong(1);
                    }
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Unable to get transfer amount from " + table, ex);
        }
        return 0;
    }

    private List<T> query(String sql, StatementBinder statementBinder) {
        List<T> result = new ArrayList<>();
        try (Connection conn = Storage.db.getSQLConnection()) {
            if (conn == null) return result;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                statementBinder.bind(ps);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        result.add(mapper.map(rs));
                    }
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Unable to query transfer history from " + table, ex);
        }
        return result;
    }

    @FunctionalInterface
    public interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    @FunctionalInterface
    public interface RowBinder<T> {
        void bind(PreparedStatement ps, T data) throws SQLException;
    }

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement ps) throws SQLException;
    }
}