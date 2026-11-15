package net.danh.storage.Database;

import net.danh.storage.Database.connection.SqlDialect;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.util.List;

public class DatabaseCompatibilityWrapper extends Database {

    private final IDataStorage dataStorage;

    public DatabaseCompatibilityWrapper(IDataStorage dataStorage) {
        super(null);
        this.dataStorage = dataStorage;
    }

    @Override
    public Connection getSQLConnection() {
        throw new UnsupportedOperationException("SQL connections not supported for " + dataStorage.getType() + " database");
    }

    @Override
    public SqlDialect getDialect() {
        throw new UnsupportedOperationException("SQL dialect not available for " + dataStorage.getType() + " database");
    }

    @Override
    public boolean isSqlBacked() {
        return false;
    }

    @Override
    public void load() {
        dataStorage.load();
    }

    @Override
    public PlayerData getData(String player) {
        return dataStorage.getData(player);
    }

    @Override
    public @NotNull List<PlayerData> getAllData() {
        return dataStorage.getAllData();
    }

    @Override
    public void createTable(@NotNull PlayerData playerData) {
        dataStorage.createTable(playerData);
    }

    @Override
    public void updateTable(@NotNull PlayerData playerData) {
        dataStorage.updateTable(playerData);
    }

    @Override
    public void deleteData(String player) {
        dataStorage.deleteData(player);
    }

    @Override
    public void close() {
        dataStorage.close();
    }
}