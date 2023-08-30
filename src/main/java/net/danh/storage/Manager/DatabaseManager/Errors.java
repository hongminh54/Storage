package net.danh.storage.Manager.DatabaseManager;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class Errors {
    @Contract(pure = true)
    public static @NotNull String sqlConnectionExecute() {
        return "Couldn't execute MySQL statement: ";
    }

    @Contract(pure = true)
    public static @NotNull String sqlConnectionClose() {
        return "Failed to close MySQL connection: ";
    }

    @Contract(pure = true)
    public static @NotNull String noSQLConnection() {
        return "Unable to retrieve MYSQL connection: ";
    }

    @Contract(pure = true)
    public static @NotNull String noTableFound() {
        return "Database Error: No Table Found";
    }
}
