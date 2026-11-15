package net.danh.storage.Database.connection;

import net.danh.storage.Storage;
import org.sqlite.SQLiteConfig;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class SqliteConnectionFactory extends FlatfileConnectionFactory {

    public SqliteConnectionFactory(Path file) {
        super(file);
    }

    @Override
    public String getImplementationName() {
        return "SQLite";
    }

    @Override
    public SqlDialect getDialect() {
        return SqlDialect.SQLITE;
    }

    @Override
    public void init(Storage plugin) throws Exception {
        Class.forName("org.sqlite.JDBC");
    }

    @Override
    protected Connection createConnection(Path file) throws SQLException {
        SQLiteConfig config = new SQLiteConfig();
        config.setBusyTimeout(5000);
        config.enforceForeignKeys(true);
        config.setJournalMode(SQLiteConfig.JournalMode.WAL);
        config.setSynchronous(SQLiteConfig.SynchronousMode.NORMAL);

        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file.toAbsolutePath(), config.toProperties());
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA temp_store=MEMORY");
            statement.execute("PRAGMA cache_size=-20000");
        }
        return connection;
    }
}