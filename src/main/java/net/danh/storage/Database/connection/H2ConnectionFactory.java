package net.danh.storage.Database.connection;

import net.danh.storage.Database.DriverLoader;
import net.danh.storage.Storage;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;

public class H2ConnectionFactory extends FlatfileConnectionFactory {

    private Driver driver;

    public H2ConnectionFactory(Path file) {
        super(file);
    }

    @Override
    public String getImplementationName() {
        return "H2";
    }

    @Override
    public SqlDialect getDialect() {
        return SqlDialect.H2;
    }

    @Override
    public void init(Storage plugin) throws Exception {
        ClassLoader classLoader = DriverLoader.obtainClassLoader(plugin, DriverLoader.Library.H2);
        Class<?> driverClass = classLoader.loadClass("org.h2.Driver");
        driver = (Driver) driverClass.getDeclaredConstructor().newInstance();
    }

    @Override
    protected Connection createConnection(Path file) throws SQLException {
        // H2 appends '.mv.db' to the given path itself
        String path = file.toAbsolutePath().toString().replace('\\', '/');
        Connection connection = driver.connect("jdbc:h2:" + path, new Properties());
        if (connection == null) {
            throw new SQLException("H2 driver did not accept the database path: " + path);
        }
        return connection;
    }
}