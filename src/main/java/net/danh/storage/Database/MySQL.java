package net.danh.storage.Database;

import net.danh.storage.Storage;
import net.danh.storage.Utils.File;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

public class MySQL extends Database {

    private final String host;
    private final String port;
    private final String database;
    private final String username;
    private final String password;
    private final boolean useSSL;

    public MySQL(Storage instance) {
        super(instance);

        // Load MySQL configuration
        this.host = File.getConfig().getString("database.mysql.host", "localhost");
        this.port = File.getConfig().getString("database.mysql.port", "3306");
        this.database = File.getConfig().getString("database.mysql.database", "storage");
        this.username = File.getConfig().getString("database.mysql.username", "root");
        this.password = File.getConfig().getString("database.mysql.password", "");
        this.useSSL = File.getConfig().getBoolean("database.mysql.useSSL", false);
    }

    @Override
    public Connection getSQLConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                return connection;
            }

            Class.forName("com.mysql.cj.jdbc.Driver");

            String url = String.format("jdbc:mysql://%s:%s/%s?useSSL=%s&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                    host, port, database, useSSL);

            connection = DriverManager.getConnection(url, username, password);
            return connection;

        } catch (SQLException ex) {
            Storage.getStorage().getLogger().log(Level.SEVERE, "MySQL connection failed", ex);
        } catch (ClassNotFoundException ex) {
            Storage.getStorage().getLogger().log(Level.SEVERE, "MySQL JDBC driver not found", ex);
        }
        return null;
    }

    @Override
    public void load() {
        connection = getSQLConnection();
        try {
            Statement s = connection.createStatement();

            // Create PlayerData table
            String createPlayerTable = "CREATE TABLE IF NOT EXISTS " + table + " (" +
                    "`player` VARCHAR(36) NOT NULL," +
                    "`data` TEXT DEFAULT '{}'," +
                    "`max` BIGINT NOT NULL," +
                    "`autopickup` BOOLEAN DEFAULT 0," +
                    "PRIMARY KEY (`player`)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
            s.executeUpdate(createPlayerTable);

            // Add autopickup column if not exists (for migration)
            try {
                s.executeUpdate("ALTER TABLE " + table + " ADD COLUMN autopickup BOOLEAN DEFAULT 0");
                Storage.getStorage().getLogger().info("Added autopickup column to existing MySQL table");
            } catch (SQLException ignored) {
                // Column already exists
            }

            s.close();
        } catch (SQLException e) {
            Storage.getStorage().getLogger().log(Level.SEVERE, "Could not create MySQL tables", e);
        }

        initialize();
        Storage.getStorage().getLogger().info("Loaded MySQL Data");
    }
}