package net.danh.storage.Database;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class Errors {
    @Contract(pure = true)
    public static @NotNull String sqlConnectionExecute() {
        return "Couldn't execute SQL statement: ";
    }

    @Contract(pure = true)
    public static @NotNull String noSQLConnection() {
        return "Unable to retrieve SQL connection: ";
    }
}