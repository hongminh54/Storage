package net.danh.storage.Database.connection;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.danh.storage.Database.DriverLoader;
import net.danh.storage.Storage;
import net.danh.storage.Utils.File;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public abstract class HikariConnectionFactory implements ConnectionFactory {

    private HikariDataSource hikari;

    protected abstract int defaultPort();

    protected abstract String buildJdbcUrl(String host, String port, String database, boolean useSSL);

    /**
     * @return the driver class name, loaded from the bundled jar when
     * {@link #driverLibrary()} is null, otherwise from the runtime-downloaded library
     */
    protected abstract String driverClassName();

    protected DriverLoader.Library driverLibrary() {
        return null;
    }

    protected void overrideProperties(Map<String, String> properties) {
        // https://github.com/brettwooldridge/HikariCP/wiki/Rapid-Recovery
        properties.putIfAbsent("socketTimeout", String.valueOf(TimeUnit.SECONDS.toMillis(30)));
    }

    @Override
    public void init(Storage plugin) throws Exception {
        FileConfiguration config = File.getConfig();
        String section = "database." + getDialect().name().toLowerCase();

        String host = config.getString(section + ".host", "localhost");
        String port = config.getString(section + ".port", String.valueOf(defaultPort()));
        String database = config.getString(section + ".database", "storage");
        String username = config.getString(section + ".username", "root");
        String password = config.getString(section + ".password", "");
        boolean useSSL = config.getBoolean(section + ".useSSL", false);

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setPoolName("storage-hikari");

        Driver driver = loadDriver(plugin);
        DriverDataSource dataSource = new DriverDataSource(driver,
                buildJdbcUrl(host, port, database, useSSL), username, password);

        Map<String, String> properties = new HashMap<>();
        overrideProperties(properties);
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            dataSource.addProperty(entry.getKey(), entry.getValue());
        }
        hikariConfig.setDataSource(dataSource);

        hikariConfig.setMaximumPoolSize(config.getInt("database.pool.max-pool-size", 10));
        hikariConfig.setMinimumIdle(config.getInt("database.pool.min-idle", 2));
        hikariConfig.setMaxLifetime(config.getLong("database.pool.max-lifetime", 1800000));
        hikariConfig.setKeepaliveTime(config.getLong("database.pool.keepalive-time", 0));
        hikariConfig.setConnectionTimeout(config.getLong("database.pool.connection-timeout", 5000));
        // schema setup right after this performs the first real connection
        hikariConfig.setInitializationFailTimeout(-1);

        this.hikari = new HikariDataSource(hikariConfig);
    }

    private Driver loadDriver(Storage plugin) throws Exception {
        ClassLoader classLoader = driverLibrary() == null
                ? getClass().getClassLoader()
                : DriverLoader.obtainClassLoader(plugin, driverLibrary());
        Class<?> driverClass = classLoader.loadClass(driverClassName());
        return (Driver) driverClass.getDeclaredConstructor().newInstance();
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (hikari == null) {
            throw new SQLException("Unable to get a connection from the pool (pool not initialized)");
        }
        Connection connection = hikari.getConnection();
        if (connection == null) {
            throw new SQLException("Unable to get a connection from the pool (getConnection returned null)");
        }
        return connection;
    }

    @Override
    public void shutdown() {
        if (hikari != null) {
            hikari.close();
        }
    }
}