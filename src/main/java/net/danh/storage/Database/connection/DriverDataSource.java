package net.danh.storage.Database.connection;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Bridges a {@link Driver} instance (possibly loaded from an isolated classloader)
 * into HikariCP without relying on driver class name resolution.
 */
final class DriverDataSource implements javax.sql.DataSource {

    private final Driver driver;
    private final String jdbcUrl;
    private final Properties properties;

    DriverDataSource(Driver driver, String jdbcUrl, String username, String password) {
        this.driver = driver;
        this.jdbcUrl = jdbcUrl;
        this.properties = new Properties();
        if (username != null) {
            properties.setProperty("user", username);
        }
        if (password != null) {
            properties.setProperty("password", password);
        }
    }

    void addProperty(String key, String value) {
        properties.setProperty(key, value);
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection connection = driver.connect(jdbcUrl, properties);
        if (connection == null) {
            throw new SQLException("Driver " + driver.getClass().getName() + " does not accept URL: " + jdbcUrl);
        }
        return connection;
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Properties copy = new Properties();
        copy.putAll(properties);
        copy.setProperty("user", username);
        copy.setProperty("password", password);
        Connection connection = driver.connect(jdbcUrl, copy);
        if (connection == null) {
            throw new SQLException("Driver " + driver.getClass().getName() + " does not accept URL: " + jdbcUrl);
        }
        return connection;
    }

    @Override
    public PrintWriter getLogWriter() {
        return null;
    }

    @Override
    public void setLogWriter(PrintWriter out) {
    }

    @Override
    public int getLoginTimeout() {
        return 0;
    }

    @Override
    public void setLoginTimeout(int seconds) {
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return iface.cast(this);
        }
        throw new SQLException("Not a wrapper for " + iface.getName());
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) {
        return iface.isInstance(this);
    }
}