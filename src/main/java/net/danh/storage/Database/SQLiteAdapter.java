package net.danh.storage.Database;

import net.danh.storage.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SQLiteAdapter implements IDataStorage {

    private final SQLite database;

    public SQLiteAdapter(Storage plugin) {
        this.database = new SQLite(plugin);
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
        return "SQLite";
    }

    /**
     * Get the underlying SQLite database for compatibility
     *
     * @return SQLite database instance
     */
    public SQLite getSQLiteDatabase() {
        return database;
    }
}