package net.danh.storage.Database;

import net.danh.storage.Database.connection.*;
import net.danh.storage.Storage;
import net.danh.storage.Utils.File;

public class DatabaseFactory {

    /**
     * Create database instance based on configuration
     */
    public static IDataStorage createDatabase(Storage plugin) {
        return createDatabase(plugin, getDatabaseType());
    }

    public static IDataStorage createDatabase(Storage plugin, DatabaseType type) {
        switch (type) {
            case YML:
                return new YMLStorage(plugin);
            case MONGODB:
                return createMongoStorage(plugin);
            case MYSQL:
                return new SqlDataStorage(new MySqlConnectionFactory());
            case MARIADB:
                return new SqlDataStorage(new MariaDbConnectionFactory());
            case POSTGRESQL:
                return new SqlDataStorage(new PostgresConnectionFactory());
            case H2:
                return new SqlDataStorage(new H2ConnectionFactory(
                        plugin.getDataFolder().toPath().resolve("storage-h2")));
            case SQLITE:
            default:
                return new SqlDataStorage(new SqliteConnectionFactory(
                        plugin.getDataFolder().toPath().resolve("PlayerData.db")));
        }
    }

    private static IDataStorage createMongoStorage(Storage plugin) {
        // MongoDataStorage is loaded through the isolated driver classloader so its
        // com.mongodb references resolve against the runtime-downloaded driver jars
        ClassLoader classLoader = DriverLoader.obtainBridgeClassLoader(plugin,
                "net.danh.storage.Database.MongoDataStorage",
                DriverLoader.Library.MONGO_DRIVER_SYNC,
                DriverLoader.Library.MONGO_BSON,
                DriverLoader.Library.MONGO_DRIVER_CORE);
        try {
            Class<?> clazz = classLoader.loadClass("net.danh.storage.Database.MongoDataStorage");
            return (IDataStorage) clazz.getConstructor(Storage.class).newInstance(plugin);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to initialize MongoDB storage", e);
        }
    }

    /**
     * Get current database type from config
     */
    public static DatabaseType getDatabaseType() {
        String databaseType = File.getConfig().getString("database.type", "sqlite").toLowerCase();
        return DatabaseType.fromString(databaseType);
    }
}