package net.danh.storage.Database;

import net.danh.storage.Storage;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

public class SQLite extends Database {

    public String SQLiteCreateTokensTable = "CREATE TABLE IF NOT EXISTS PlayerData (" +
            "`player` VARCHAR(36) NOT NULL," +
            "`data` TEXT DEFAULT '{}'," +
            "`max` BIGINT NOT NULL," +
            "`autopickup` BOOLEAN DEFAULT 0," +
            "PRIMARY KEY (`player`)" +
            ");";
    String dbname;

    public SQLite(Storage instance) {
        super(instance);
        dbname = "PlayerData";
    }

    public Connection getSQLConnection() {
        File dataFolder = new File(Storage.getStorage().getDataFolder(), dbname + ".db");
        if (!dataFolder.exists()) {
            try {
                dataFolder.createNewFile();
            } catch (IOException e) {
                Storage.getStorage().getLogger().log(Level.SEVERE, "File write error: " + dbname + ".db");
            }
        }
        try {
            if (connection != null && !connection.isClosed()) {
                return connection;
            }
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dataFolder);
            return connection;
        } catch (SQLException ex) {
            Storage.getStorage().getLogger().log(Level.SEVERE, "SQLite exception on initialize", ex);
        } catch (ClassNotFoundException ex) {
            Storage.getStorage().getLogger().log(Level.SEVERE, "You need the SQLite JDBC library. Google it. Put it in /lib folder.");
        }
        return null;
    }

    public void load() {
        connection = getSQLConnection();
        try {
            Statement s = connection.createStatement();
            s.executeUpdate(SQLiteCreateTokensTable);
            
            try {
                s.executeUpdate("ALTER TABLE PlayerData ADD COLUMN autopickup BOOLEAN DEFAULT 0");
                Storage.getStorage().getLogger().info("Added autopickup column to existing database");
            } catch (SQLException ignored) {
            }
            
            s.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        initialize();
        Storage.getStorage().getLogger().info("Loaded SQLite Data");
    }
}

