package net.danh.storage.Database.connection;

import net.danh.storage.Storage;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;

public abstract class FlatfileConnectionFactory implements ConnectionFactory {

    private final Path file;
    private NonClosableConnection connection;

    protected FlatfileConnectionFactory(Path file) {
        this.file = file;
    }

    protected Path getFile() {
        return file;
    }

    protected abstract Connection createConnection(Path file) throws SQLException;

    @Override
    public void init(Storage plugin) throws Exception {
    }

    @Override
    public synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = new NonClosableConnection(createConnection(file));
        }
        return connection;
    }

    @Override
    public synchronized void shutdown() {
        if (connection != null) {
            connection.shutdown();
            connection = null;
        }
    }
}