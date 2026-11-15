package net.danh.storage.Database;

import net.danh.storage.Storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

public class FriendDatabase {

    private static final String FRIENDS_TABLE = Database.applyPrefix("StorageFriends");
    private static final String REQUESTS_TABLE = Database.applyPrefix("StorageFriendRequests");
    private static final String LOGS_TABLE = Database.applyPrefix("StorageFriendLogs");
    private static final String ACCESS_SETTINGS_TABLE = Database.applyPrefix("StorageFriendAccessSettings");
    private static final String ACTION_SETTINGS_TABLE = Database.applyPrefix("StorageFriendActionSettings");

    public void createTables() {
        // tables and indexes are created by the dialect schema during SqlDataStorage.load()
    }

    // ==================== Friends ====================

    public void addFriend(UUID playerUuid, UUID friendUuid) {
        long now = System.currentTimeMillis();
        Connection conn = null;
        try {
            conn = Storage.db.getSQLConnection();
            if (conn == null) return;
            conn.setAutoCommit(false);

            // Insert both directions for mutual friendship
            String sql = dialect().insertIgnore(FRIENDS_TABLE,
                    new String[]{"player_uuid", "friend_uuid", "created_at"},
                    new String[]{"player_uuid", "friend_uuid"});

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, playerUuid.toString());
                ps.setString(2, friendUuid.toString());
                ps.setLong(3, now);
                ps.executeUpdate();

                ps.setString(1, friendUuid.toString());
                ps.setString(2, playerUuid.toString());
                ps.setLong(3, now);
                ps.executeUpdate();
            }
            conn.commit();
        } catch (SQLException ex) {
            Storage.getStorage().getLogger().log(Level.SEVERE, "[FriendStorage] Failed to add friend", ex);
            rollback(conn);
        } finally {
            resetAutoCommit(conn);
            closeConnection(conn);
        }
    }

    public void removeFriend(UUID playerUuid, UUID friendUuid) {
        Connection conn = null;
        try {
            conn = Storage.db.getSQLConnection();
            if (conn == null) return;
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM " + FRIENDS_TABLE + " WHERE (player_uuid = ? AND friend_uuid = ?) OR (player_uuid = ? AND friend_uuid = ?)")) {
                ps.setString(1, playerUuid.toString());
                ps.setString(2, friendUuid.toString());
                ps.setString(3, friendUuid.toString());
                ps.setString(4, playerUuid.toString());
                ps.executeUpdate();
            }
            conn.commit();
        } catch (SQLException ex) {
            Storage.getStorage().getLogger().log(Level.SEVERE, "[FriendStorage] Failed to remove friend", ex);
            rollback(conn);
        } finally {
            resetAutoCommit(conn);
            closeConnection(conn);
        }
    }

    public Set<UUID> getFriends(UUID playerUuid) {
        Set<UUID> friends = new LinkedHashSet<>();
        Connection conn = null;
        try {
            conn = Storage.db.getSQLConnection();
            if (conn == null) return friends;

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT friend_uuid FROM " + FRIENDS_TABLE + " WHERE player_uuid = ?")) {
                ps.setString(1, playerUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        try {
                            friends.add(UUID.fromString(rs.getString("friend_uuid")));
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }
            }
        } catch (SQLException ex) {
            Storage.getStorage().getLogger().log(Level.SEVERE, "[FriendStorage] Failed to get friends", ex);
        } finally {
            closeConnection(conn);
        }
        return friends;
    }

    public boolean areFriends(UUID playerUuid, UUID friendUuid) {
        Connection conn = null;
        try {
            conn = Storage.db.getSQLConnection();
            if (conn == null) return false;

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT 1 FROM " + FRIENDS_TABLE + " WHERE player_uuid = ? AND friend_uuid = ? LIMIT 1")) {
                ps.setString(1, playerUuid.toString());
                ps.setString(2, friendUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (SQLException ex) {
            Storage.getStorage().getLogger().log(Level.SEVERE, "[FriendStorage] Failed to check friendship", ex);
        } finally {
            closeConnection(conn);
        }
        return false;
    }

    public int getFriendCount(UUID playerUuid) {
        Connection conn = null;
        try {
            conn = Storage.db.getSQLConnection();
            if (conn == null) return 0;

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM " + FRIENDS_TABLE + " WHERE player_uuid = ?")) {
                ps.setString(1, playerUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        } catch (SQLException ex) {
            Storage.getStorage().getLogger().log(Level.SEVERE, "[FriendStorage] Failed to count friends", ex);
        } finally {
            closeConnection(conn);
        }
        return 0;
    }

    // ==================== Requests ====================

    public void addRequest(UUID senderUuid, UUID receiverUuid, long expiresAt) {
        Connection conn = null;
        try {
            conn = Storage.db.getSQLConnection();
            if (conn == null) return;

            String sql = dialect().upsert(REQUESTS_TABLE,
                    new String[]{"sender_uuid", "receiver_uuid", "created_at", "expires_at"},
                    new String[]{"sender_uuid", "receiver_uuid"});

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, senderUuid.toString());
                ps.setString(2, receiverUuid.toString());
                ps.setLong(3, System.currentTimeMillis());
                ps.setLong(4, expiresAt);
                ps.executeUpdate();
            }
        } catch (SQLException ex) {
            Storage.getStorage().getLogger().log(Level.SEVERE, "[FriendStorage] Failed to add request", ex);
        } finally {
            closeConnection(conn);
        }
    }

    public void removeRequest(UUID senderUuid, UUID receiverUuid) {
        Connection conn = null;
        try {
            conn = Storage.db.getSQLConnection();
            if (conn == null) return;

            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM " + REQUESTS_TABLE + " WHERE sender_uuid = ? AND receiver_uuid = ?")) {
                ps.setString(1, senderUuid.toString());
                ps.setString(2, receiverUuid.toString());
                ps.executeUpdate();
            }
        } catch (SQLException ex) {
            Storage.getStorage().getLogger().log(Level.SEVERE, "[FriendStorage] Failed to remove request", ex);
        } finally {
            closeConnection(conn);
        }
    }

    public boolean hasRequest(UUID senderUuid, UUID receiverUuid) {
        Connection conn = null;
        try {
            conn = Storage.db.getSQLConnection();
            if (conn == null) return false;

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT expires_at FROM " + REQUESTS_TABLE + " WHERE sender_uuid = ? AND receiver_uuid = ? LIMIT 1")) {
                ps.setString(1, senderUuid.toString());
                ps.setString(2, receiverUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return false;
                    long expiresAt = rs.getLong("expires_at");
                    // 0 = never expires
                    return expiresAt == 0 || expiresAt > System.currentTimeMillis();
                }
            }
        } catch (SQLException ex) {
            Storage.getStorage().getLogger().log(Level.SEVERE, "[FriendStorage] Failed to check request", ex);
        } finally {
            closeConnection(conn);
        }
        return false;
    }

    /**
     * Get all pending requests where the player is the receiver (non-expired)
     */
    public Map<UUID, Long> getPendingRequests(UUID receiverUuid) {
        Map<UUID, Long> requests = new LinkedHashMap<>();
        Connection conn = null;
        try {
            conn = Storage.db.getSQLConnection();
            if (conn == null) return requests;

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT sender_uuid, expires_at FROM " + REQUESTS_TABLE + " WHERE receiver_uuid = ?")) {
                ps.setString(1, receiverUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    long now = System.currentTimeMillis();
                    while (rs.next()) {
                        long expiresAt = rs.getLong("expires_at");
                        if (expiresAt != 0 && expiresAt <= now) {
                            continue;
                        }
                        try {
                            UUID senderUuid = UUID.fromString(rs.getString("sender_uuid"));
                            requests.put(senderUuid, expiresAt);
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }
            }
        } catch (SQLException ex) {
            Storage.getStorage().getLogger().log(Level.SEVERE, "[FriendStorage] Failed to get pending requests", ex);
        } finally {
            closeConnection(conn);
        }
        return requests;
    }

    /**
     * Remove all expired requests from the database
     */
    public void cleanExpiredRequests() {
        Connection conn = null;
        try {
            conn = Storage.db.getSQLConnection();
            if (conn == null) return;

            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM " + REQUESTS_TABLE + " WHERE expires_at > 0 AND expires_at <= ?")) {
                ps.setLong(1, System.currentTimeMillis());
                ps.executeUpdate();
            }
        } catch (SQLException ex) {
            Storage.getStorage().getLogger().log(Level.SEVERE, "[FriendStorage] Failed to clean expired requests", ex);
        } finally {
            closeConnection(conn);
        }
    }

    // ==================== Logs ====================

    public void insertLog(UUID ownerUuid, UUID actorUuid, String action, String detail) {
        Connection conn = null;
        try {
            conn = Storage.db.getSQLConnection();
            if (conn == null) return;

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO " + LOGS_TABLE + " (owner_uuid, actor_uuid, action, detail, timestamp) VALUES (?, ?, ?, ?, ?)")) {
                ps.setString(1, ownerUuid.toString());
                ps.setString(2, actorUuid.toString());
                ps.setString(3, action);
                ps.setString(4, detail);
                ps.setLong(5, System.currentTimeMillis());
                ps.executeUpdate();
            }
        } catch (SQLException ex) {
            Storage.getStorage().getLogger().log(Level.SEVERE, "[FriendStorage] Failed to insert log", ex);
        } finally {
            closeConnection(conn);
        }
    }

    /**
     * Alias for insertLog - log friend storage action
     */
    public void logFriendAction(UUID ownerUuid, UUID actorUuid, String action, String details) {
        insertLog(ownerUuid, actorUuid, action, details);
    }

    public List<FriendActionLog> getPlayerActionLogs(UUID ownerUuid, int page, int pageSize) {
        List<FriendActionLog> logs = new ArrayList<>();
        if (page < 1) page = 1;
        if (pageSize <= 0) pageSize = 10;

        int offset = (page - 1) * pageSize;

        Connection conn = null;
        try {
            conn = Storage.db.getSQLConnection();
            if (conn == null) return logs;

            String sql = "SELECT owner_uuid, actor_uuid, action, detail, timestamp FROM " + LOGS_TABLE + " WHERE owner_uuid = ? ORDER BY timestamp DESC LIMIT ? OFFSET ?";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, ownerUuid.toString());
                ps.setInt(2, pageSize);
                ps.setInt(3, offset);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        UUID owner = safeUuid(rs.getString("owner_uuid"));
                        UUID actor = safeUuid(rs.getString("actor_uuid"));
                        if (owner == null || actor == null) continue;
                        logs.add(new FriendActionLog(
                                owner,
                                actor,
                                rs.getString("action"),
                                rs.getString("detail"),
                                rs.getLong("timestamp")
                        ));
                    }
                }
            }
        } catch (SQLException ex) {
            Storage.getStorage().getLogger().log(Level.SEVERE, "[FriendStorage] Failed to get player action logs", ex);
        } finally {
            closeConnection(conn);
        }

        return logs;
    }

    /**
     * Get logs where this player is the actor (actions they performed on others' storage)
     */
    public List<FriendActionLog> getActorActionLogs(UUID actorUuid, int page, int pageSize) {
        List<FriendActionLog> logs = new ArrayList<>();
        if (page < 1) page = 1;
        if (pageSize <= 0) pageSize = 10;
        int offset = (page - 1) * pageSize;

        Connection conn = null;
        try {
            conn = Storage.db.getSQLConnection();
            if (conn == null) return logs;

            String sql = "SELECT owner_uuid, actor_uuid, action, detail, timestamp FROM " + LOGS_TABLE
                    + " WHERE actor_uuid = ? ORDER BY timestamp DESC LIMIT ? OFFSET ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, actorUuid.toString());
                ps.setInt(2, pageSize);
                ps.setInt(3, offset);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        UUID owner = safeUuid(rs.getString("owner_uuid"));
                        UUID actor = safeUuid(rs.getString("actor_uuid"));
                        if (owner == null || actor == null) continue;
                        logs.add(new FriendActionLog(owner, actor,
                                rs.getString("action"),
                                rs.getString("detail"),
                                rs.getLong("timestamp")));
                    }
                }
            }
        } catch (SQLException ex) {
            Storage.getStorage().getLogger().log(Level.SEVERE, "[FriendStorage] Failed to get actor action logs", ex);
        } finally {
            closeConnection(conn);
        }
        return logs;
    }

    public int getActorActionLogCount(UUID actorUuid) {
        Connection conn = null;
        try {
            conn = Storage.db.getSQLConnection();
            if (conn == null) return 0;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM " + LOGS_TABLE + " WHERE actor_uuid = ?")) {
                ps.setString(1, actorUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
        } catch (SQLException ex) {
            Storage.getStorage().getLogger().log(Level.SEVERE, "[FriendStorage] Failed to count actor action logs", ex);
        } finally {
            closeConnection(conn);
        }
        return 0;
    }

    public int getPlayerActionLogCount(UUID ownerUuid) {
        Connection conn = null;
        try {
            conn = Storage.db.getSQLConnection();
            if (conn == null) return 0;

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM " + LOGS_TABLE + " WHERE owner_uuid = ?")) {
                ps.setString(1, ownerUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        } catch (SQLException ex) {
            Storage.getStorage().getLogger().log(Level.SEVERE, "[FriendStorage] Failed to count player action logs", ex);
        } finally {
            closeConnection(conn);
        }
        return 0;
    }

    public boolean getAccessSetting(UUID playerUuid, String storageType, boolean defaultValue) {
        Connection conn = null;
        try {
            conn = Storage.db.getSQLConnection();
            if (conn == null) return defaultValue;

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT allowed FROM " + ACCESS_SETTINGS_TABLE + " WHERE player_uuid = ? AND storage_type = ? LIMIT 1")) {
                ps.setString(1, playerUuid.toString());
                ps.setString(2, storageType.toLowerCase());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getBoolean("allowed");
                    }
                }
            }
        } catch (SQLException ex) {
            Storage.getStorage().getLogger().log(Level.SEVERE, "[FriendStorage] Failed to get access setting", ex);
        } finally {
            closeConnection(conn);
        }
        return defaultValue;
    }

    // ==================== Access Settings ====================

    public void setAccessSetting(UUID playerUuid, String storageType, boolean allowed) {
        Connection conn = null;
        try {
            conn = Storage.db.getSQLConnection();
            if (conn == null) return;

            String sql = dialect().upsert(ACCESS_SETTINGS_TABLE,
                    new String[]{"player_uuid", "storage_type", "allowed"},
                    new String[]{"player_uuid", "storage_type"});

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, playerUuid.toString());
                ps.setString(2, storageType.toLowerCase());
                ps.setBoolean(3, allowed);
                ps.executeUpdate();
            }
        } catch (SQLException ex) {
            Storage.getStorage().getLogger().log(Level.SEVERE, "[FriendStorage] Failed to set access setting", ex);
        } finally {
            closeConnection(conn);
        }
    }

    public Map<String, Boolean> getAllAccessSettings(UUID playerUuid, boolean defaultValue) {
        Map<String, Boolean> settings = new HashMap<>();
        // Initialize with defaults
        settings.put("storage", defaultValue);
        settings.put("mythicstorage", defaultValue);
        settings.put("cropstorage", defaultValue);

        Connection conn = null;
        try {
            conn = Storage.db.getSQLConnection();
            if (conn == null) return settings;

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT storage_type, allowed FROM " + ACCESS_SETTINGS_TABLE + " WHERE player_uuid = ?")) {
                ps.setString(1, playerUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String type = rs.getString("storage_type");
                        boolean allowed = rs.getBoolean("allowed");
                        settings.put(type, allowed);
                    }
                }
            }
        } catch (SQLException ex) {
            Storage.getStorage().getLogger().log(Level.SEVERE, "[FriendStorage] Failed to get all access settings", ex);
        } finally {
            closeConnection(conn);
        }
        return settings;
    }

    public boolean getActionSetting(UUID playerUuid, String storageType, String action, boolean defaultValue) {
        Connection conn = null;
        try {
            conn = Storage.db.getSQLConnection();
            if (conn == null) return defaultValue;

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT allowed FROM " + ACTION_SETTINGS_TABLE + " WHERE player_uuid = ? AND storage_type = ? AND action = ? LIMIT 1")) {
                ps.setString(1, playerUuid.toString());
                ps.setString(2, storageType.toLowerCase());
                ps.setString(3, action.toLowerCase());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getBoolean("allowed");
                    }
                }
            }
        } catch (SQLException ex) {
            Storage.getStorage().getLogger().log(Level.SEVERE, "[FriendStorage] Failed to get action setting", ex);
        } finally {
            closeConnection(conn);
        }
        return defaultValue;
    }

    // ==================== Action Settings ====================

    public void setActionSetting(UUID playerUuid, String storageType, String action, boolean allowed) {
        Connection conn = null;
        try {
            conn = Storage.db.getSQLConnection();
            if (conn == null) return;

            String sql = dialect().upsert(ACTION_SETTINGS_TABLE,
                    new String[]{"player_uuid", "storage_type", "action", "allowed"},
                    new String[]{"player_uuid", "storage_type", "action"});

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, playerUuid.toString());
                ps.setString(2, storageType.toLowerCase());
                ps.setString(3, action.toLowerCase());
                ps.setBoolean(4, allowed);
                ps.executeUpdate();
            }
        } catch (SQLException ex) {
            Storage.getStorage().getLogger().log(Level.SEVERE, "[FriendStorage] Failed to set action setting", ex);
        } finally {
            closeConnection(conn);
        }
    }

    public Map<String, Map<String, Boolean>> getAllActionSettings(UUID playerUuid, boolean defaultDepositAllowed, boolean defaultWithdrawAllowed) {
        Map<String, Map<String, Boolean>> settings = new HashMap<>();
        for (String type : Arrays.asList("storage", "mythicstorage", "cropstorage")) {
            Map<String, Boolean> perType = new HashMap<>();
            perType.put("deposit", defaultDepositAllowed);
            perType.put("withdraw", defaultWithdrawAllowed);
            settings.put(type, perType);
        }

        Connection conn = null;
        try {
            conn = Storage.db.getSQLConnection();
            if (conn == null) return settings;

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT storage_type, action, allowed FROM " + ACTION_SETTINGS_TABLE + " WHERE player_uuid = ?")) {
                ps.setString(1, playerUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String type = rs.getString("storage_type");
                        String action = rs.getString("action");
                        boolean allowed = rs.getBoolean("allowed");

                        Map<String, Boolean> perType = settings.get(type);
                        if (perType == null) {
                            perType = new HashMap<>();
                            perType.put("deposit", defaultDepositAllowed);
                            perType.put("withdraw", defaultWithdrawAllowed);
                            settings.put(type, perType);
                        }
                        perType.put(action, allowed);
                    }
                }
            }
        } catch (SQLException ex) {
            Storage.getStorage().getLogger().log(Level.SEVERE, "[FriendStorage] Failed to get all action settings", ex);
        } finally {
            closeConnection(conn);
        }

        return settings;
    }

    private UUID safeUuid(String raw) {
        if (raw == null) return null;
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    // ==================== Helpers ====================

    private net.danh.storage.Database.connection.SqlDialect dialect() {
        return Storage.db.getDialect();
    }

    private void rollback(Connection conn) {
        if (conn == null) return;
        try {
            conn.rollback();
        } catch (SQLException ignored) {
        }
    }

    private void resetAutoCommit(Connection conn) {
        if (conn == null) return;
        try {
            if (!conn.getAutoCommit()) {
                conn.setAutoCommit(true);
            }
        } catch (SQLException ignored) {
        }
    }

    private void closeConnection(Connection conn) {
        if (conn == null) return;
        try {
            conn.close();
        } catch (SQLException ex) {
            Storage.getStorage().getLogger().log(Level.SEVERE, "[FriendStorage] Failed to close connection", ex);
        }
    }

    public record FriendActionLog(UUID ownerUuid, UUID actorUuid, String action, String detail, long timestamp) {
    }
}
