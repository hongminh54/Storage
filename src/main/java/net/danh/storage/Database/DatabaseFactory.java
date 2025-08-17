package net.danh.storage.Database;

import net.danh.storage.Storage;
import net.danh.storage.Utils.File;

public class DatabaseFactory {

    /**
     * Create database instance based on configuration
     *
     * @param plugin Plugin instance
     * @return Database implementation
     */
    public static IDataStorage createDatabase(Storage plugin) {
        String databaseType = File.getConfig().getString("database.type", "sqlite").toLowerCase();
        DatabaseType type = DatabaseType.fromString(databaseType);

        switch (type) {
            case YML:
                return new YMLStorage(plugin);
            case MYSQL:
                return new MySQLAdapter(plugin);
            case SQLITE:
            default:
                return new SQLiteAdapter(plugin);
        }
    }

    /**
     * Get current database type from config
     *
     * @return Database type
     */
    public static DatabaseType getDatabaseType() {
        String databaseType = File.getConfig().getString("database.type", "sqlite").toLowerCase();
        return DatabaseType.fromString(databaseType);
    }
}