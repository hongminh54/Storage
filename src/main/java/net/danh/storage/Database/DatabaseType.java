package net.danh.storage.Database;

public enum DatabaseType {
    SQLITE("sqlite"),
    YML("yml"),
    MYSQL("mysql");

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
}