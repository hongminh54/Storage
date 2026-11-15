package net.danh.storage.Database;

import net.danh.storage.Data.MobTransferData;
import net.danh.storage.Storage;

import java.util.List;

public class MobTransferDatabase {

    private final SqlTransferStorage<MobTransferData> sqlStorage;
    private final YMLMobTransferStorage ymlStorage;

    public MobTransferDatabase(Storage plugin) {
        if (Storage.db.isSqlBacked()) {
            sqlStorage = new SqlTransferStorage<>(plugin, "MobTransferLogs", "item_name",
                    rs -> new MobTransferData(
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
            ymlStorage = new YMLMobTransferStorage(plugin);
        }
    }

    public void createTransferTable() {
        // tables are created by the dialect schema during database load
    }

    public void insertTransfer(MobTransferData transferData) {
        if (sqlStorage != null) {
            sqlStorage.insertTransfer(transferData);
        } else {
            ymlStorage.insertTransfer(transferData);
        }
    }

    public List<MobTransferData> getTransferHistory(String playerName, int limit, int offset) {
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

    public long getTotalSentAmount(String playerName) {
        if (sqlStorage != null) {
            return sqlStorage.getTotalSentAmount(playerName);
        }
        return ymlStorage.getTotalSentAmount(playerName);
    }

    public long getTotalReceivedAmount(String playerName) {
        if (sqlStorage != null) {
            return sqlStorage.getTotalReceivedAmount(playerName);
        }
        return ymlStorage.getTotalReceivedAmount(playerName);
    }
}