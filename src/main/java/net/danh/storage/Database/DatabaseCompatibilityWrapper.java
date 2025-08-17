package net.danh.storage.Database;

import net.danh.storage.Storage;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DatabaseCompatibilityWrapper extends Database {

    private final IDataStorage dataStorage;

    public DatabaseCompatibilityWrapper(IDataStorage dataStorage) {
        super(Storage.getStorage());
        this.dataStorage = dataStorage;
    }

    @Override
    public Connection getSQLConnection() {
        throw new UnsupportedOperationException("SQL connections not supported for " + dataStorage.getType() + " database");
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
    public void close(PreparedStatement ps, ResultSet rs) {
        // No-op for non-SQL databases
    }
}