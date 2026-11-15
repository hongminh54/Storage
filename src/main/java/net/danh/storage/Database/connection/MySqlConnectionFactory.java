package net.danh.storage.Database.connection;

import java.util.Map;

public class MySqlConnectionFactory extends HikariConnectionFactory {

    @Override
    public String getImplementationName() {
        return "MySQL";
    }

    @Override
    public SqlDialect getDialect() {
        return SqlDialect.MYSQL;
    }

    @Override
    protected int defaultPort() {
        return 3306;
    }

    @Override
    protected String buildJdbcUrl(String host, String port, String database, boolean useSSL) {
        return "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=" + useSSL + "&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    }

    @Override
    protected String driverClassName() {
        return "com.mysql.cj.jdbc.Driver";
    }

    @Override
    protected void overrideProperties(Map<String, String> properties) {
        // https://github.com/brettwooldridge/HikariCP/wiki/MySQL-Configuration
        properties.putIfAbsent("cachePrepStmts", "true");
        properties.putIfAbsent("prepStmtCacheSize", "250");
        properties.putIfAbsent("prepStmtCacheSqlLimit", "2048");
        properties.putIfAbsent("useServerPrepStmts", "true");
        properties.putIfAbsent("useLocalSessionState", "true");
        properties.putIfAbsent("rewriteBatchedStatements", "true");
        properties.putIfAbsent("cacheResultSetMetadata", "true");
        properties.putIfAbsent("cacheServerConfiguration", "true");
        properties.putIfAbsent("elideSetAutoCommits", "true");
        properties.putIfAbsent("maintainTimeStats", "false");
        properties.putIfAbsent("alwaysSendSetIsolation", "false");
        properties.putIfAbsent("cacheCallableStmts", "true");
        super.overrideProperties(properties);
    }
}