package net.danh.storage.Database;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IDataStorage {

    /**
     * Initialize and load the database
     */
    void load();

    /**
     * Get player data from database
     *
     * @param player Player name
     * @return PlayerData or null if not found
     */
    @Nullable
    PlayerData getData(@NotNull String player);

    /**
     * Create new player data in database
     *
     * @param playerData Player data to create
     */
    void createTable(@NotNull PlayerData playerData);

    /**
     * Update existing player data in database
     *
     * @param playerData Player data to update
     */
    void updateTable(@NotNull PlayerData playerData);

    /**
     * Delete player data from database
     *
     * @param player Player name to delete
     */
    void deleteData(@NotNull String player);

    /**
     * Get database type name
     *
     * @return Database type
     */
    String getType();
}