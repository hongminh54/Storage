package net.danh.storage.Database.connection;

import net.danh.storage.Database.DriverLoader;

public class PostgresConnectionFactory extends HikariConnectionFactory {

    @Override
    public String getImplementationName() {
        return "PostgreSQL";
    }

    @Override
    public SqlDialect getDialect() {
        return SqlDialect.POSTGRESQL;
    }

    @Override
    protected int defaultPort() {
        return 5432;
    }

    @Override
    protected String buildJdbcUrl(String host, String port, String database, boolean useSSL) {
        return "jdbc:postgresql://" + host + ":" + port + "/" + database + (useSSL ? "?ssl=true" : "");
    }

    @Override
    protected String driverClassName() {
        return "org.postgresql.Driver";
    }

    @Override
    protected DriverLoader.Library driverLibrary() {
        return DriverLoader.Library.POSTGRESQL;
    }
}