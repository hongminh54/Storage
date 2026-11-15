package net.danh.storage.Database;

import net.danh.storage.Data.MythicTransferData;
import net.danh.storage.Storage;

import java.util.List;

public class MythicTransferDatabase {

    private final SqlTransferStorage<MythicTransferData> sqlStorage;
    private final YMLMythicTransferStorage ymlStorage;

    public MythicTransferDatabase(Storage plugin) {
        if (Storage.db.isSqlBacked()) {
            sqlStorage = new SqlTransferStorage<>(plugin, "MythicTransferLogs", "item_name",
                    rs -> new MythicTransferData(
                            rs.getInt("id"),
                            rs.getString("sender"),
                            rs.getString("receiver"),
                            rs.getString("item_name"),
                            rs.getInt("amount"),
                            rs.getLong("timestamp"),
                            rs.getString("status")),
                    (ps, data) -> {
                        ps.setString(1, data.getSender());
                        ps.setString(2, data.getReceiver());
                        ps.setString(3, data.getItemName());
                        ps.setInt(4, data.getAmount());
                        ps.setLong(5, data.getTimestamp());
                        ps.setString(6, data.getStatus());
                    });
            ymlStorage = null;
        } else {
            sqlStorage = null;
            ymlStorage = new YMLMythicTransferStorage(plugin);
        }
    }

    public void createTransferTable() {
        // tables are created by the dialect schema during database load
    }

    public void insertTransfer(MythicTransferData transferData) {
        if (sqlStorage != null) {
            sqlStorage.insertTransfer(transferData);
        } else {
            ymlStorage.insertTransfer(transferData);
        }
    }

    public List<MythicTransferData> getTransferHistory(String playerName, int limit) {
        return getTransferHistory(playerName, limit, 0);
    }

    public List<MythicTransferData> getTransferHistory(String playerName, int limit, int offset) {
        if (sqlStorage != null) {
            return sqlStorage.getTransferHistory(playerName, limit, offset);
        }
        return ymlStorage.getTransferHistory(playerName, limit, offset);
    }

    public int getTotalTransferCount(String playerName) {
        if (sqlStorage != null) {
            return sqlStorage.getTotalTransferCount(playerName);
        }
        return ymlStorage.getTotalTransferCount(playerName);
    }
}