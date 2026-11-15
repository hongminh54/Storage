package net.danh.storage.Database.connection;

import net.danh.storage.Storage;

import java.sql.Connection;
import java.sql.SQLException;

public interface ConnectionFactory {

    void init(Storage plugin) throws Exception;

    Connection getConnection() throws SQLException;

    void shutdown();

    String getImplementationName();

    SqlDialect getDialect();
}