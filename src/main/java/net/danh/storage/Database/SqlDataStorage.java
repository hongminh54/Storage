package net.danh.storage.Database;

import net.danh.storage.Database.connection.ConnectionFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record SqlDataStorage(Database database) implements IDataStorage {

    public SqlDataStorage(ConnectionFactory factory) {
        this(new Database(factory));
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
    @NotNull
    public List<PlayerData> getAllData() {
        return database.getAllData();
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
        return database.getConnectionFactory().getImplementationName();
    }

    @Override
    public void close() {
        database.close();
    }
}