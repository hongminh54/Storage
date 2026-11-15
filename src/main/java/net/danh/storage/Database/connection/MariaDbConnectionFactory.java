package net.danh.storage.Database.connection;

import net.danh.storage.Database.DriverLoader;

import java.util.Map;

public class MariaDbConnectionFactory extends HikariConnectionFactory {

    @Override
    public String getImplementationName() {
        return "MariaDB";
    }

    @Override
    public SqlDialect getDialect() {
        return SqlDialect.MARIADB;
    }

    @Override
    protected int defaultPort() {
        return 3306;
    }

    @Override
    protected String buildJdbcUrl(String host, String port, String database, boolean useSSL) {
        return "jdbc:mariadb://" + host + ":" + port + "/" + database + "?useSSL=" + useSSL;
    }

    @Override
    protected String driverClassName() {
        return "org.mariadb.jdbc.Driver";
    }

    @Override
    protected DriverLoader.Library driverLibrary() {
        return DriverLoader.Library.MARIADB;
    }

    @Override
    protected void overrideProperties(Map<String, String> properties) {
        properties.putIfAbsent("prepStmtCacheSize", "250");
        properties.putIfAbsent("prepStmtCacheSqlLimit", "2048");
        properties.putIfAbsent("cachePrepStmts", "true");
        properties.putIfAbsent("useServerPrepStmts", "true");
        super.overrideProperties(properties);
    }
}