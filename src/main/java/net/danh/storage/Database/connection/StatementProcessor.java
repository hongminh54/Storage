package net.danh.storage.Database.connection;

public interface StatementProcessor {

    StatementProcessor USE_BACKTICKS = s -> s.replace('\'', '`');

    StatementProcessor USE_DOUBLE_QUOTES = s -> s.replace('\'', '"');

    String process(String statement);
}