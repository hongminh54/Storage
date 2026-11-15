package net.danh.storage.Database;

public enum DatabaseType {
    SQLITE("sqlite"),
    YML("yml"),
    MYSQL("mysql"),
    MARIADB("mariadb"),
    POSTGRESQL("postgresql"),
    H2("h2"),
    MONGODB("mongodb");

    private final String name;

    DatabaseType(String name) {
        this.name = name;
    }

    public static DatabaseType fromString(String name) {
        for (DatabaseType type : DatabaseType.values()) {
            if (type.getName().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return SQLITE;
    }

    public String getName() {
        return name;
    }

    public boolean isSql() {
        return this == SQLITE || this == MYSQL || this == MARIADB || this == POSTGRESQL || this == H2;
    }
}