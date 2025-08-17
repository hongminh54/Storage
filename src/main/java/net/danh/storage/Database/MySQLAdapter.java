package net.danh.storage.Database;

import net.danh.storage.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MySQLAdapter implements IDataStorage {

    private final MySQL database;

    public MySQLAdapter(Storage plugin) {
        this.database = new MySQL(plugin);
    }

    @Override
    public void load() {
        database.load();
    }

    @Override
    @Nullable
    public PlayerData getData(@NotNull String player) {
        return database.getData(player);
    }

    @Override
    public void createTable(@NotNull PlayerData playerData) {
        database.createTable(playerData);
    }

    @Override
    public void updateTable(@NotNull PlayerData playerData) {
        database.updateTable(playerData);
    }

    @Override
    public void deleteData(@NotNull String player) {
        database.deleteData(player);
    }

    @Override
    public String getType() {
        return "MySQL";
    }

    /**
     * Get the underlying MySQL database for compatibility
     *
     * @return MySQL database instance
     */
    public MySQL getMySQLDatabase() {
        return database;
    }
}