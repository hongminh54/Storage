package net.danh.storage.API;

import net.danh.storage.API.exceptions.StorageException;
import net.danh.storage.Manager.MineManager;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Wrapper class for Player with storage-specific functionality
 * Provides convenient methods to interact with player's storage
 *
 * @author VoChiDanh, hongminh54
 * @version 2.3.2
 */
public class StoragePlayer {

    private final Player player;

    /**
     * Create StoragePlayer wrapper
     *
     * @param player The Bukkit player
     */
    public StoragePlayer(@NotNull Player player) {
        this.player = player;
    }

    /**
     * Get the wrapped Bukkit player
     *
     * @return Bukkit Player instance
     */
    @NotNull
    public Player getPlayer() {
        return player;
    }

    /**
     * Get player's name
     *
     * @return Player name
     */
    @NotNull
    public String getName() {
        return player.getName();
    }

    /**
     * Get player's UUID as string
     *
     * @return UUID string
     */
    @NotNull
    public String getUUID() {
        return player.getUniqueId().toString();
    }

    // ==================== STORAGE METHODS ====================

    /**
     * Get amount of specific material in storage
     *
     * @param material Material name (e.g., "STONE;0")
     * @return Amount stored
     */
    public int getStoredAmount(@NotNull String material) {
        return StorageAPI.getItemAmount(player, material);
    }

    /**
     * Check if player has specific material in storage
     *
     * @param material Material name
     * @return true if player has this material
     */
    public boolean hasStoredMaterial(@NotNull String material) {
        return getStoredAmount(material) > 0;
    }

    /**
     * Check if player has at least specified amount of material
     *
     * @param material Material name
     * @param amount   Required amount
     * @return true if player has enough
     */
    public boolean hasEnoughMaterial(@NotNull String material, int amount) {
        return getStoredAmount(material) >= amount;
    }

    /**
     * Add item to storage
     *
     * @param material Material name
     * @param amount   Amount to add
     * @return true if successfully added
     * @throws StorageException if operation fails
     */
    public boolean addItem(@NotNull String material, int amount) throws StorageException {
        return StorageAPI.addItem(player, material, amount);
    }

    /**
     * Remove item from storage
     *
     * @param material Material name
     * @param amount   Amount to remove
     * @return true if successfully removed
     * @throws StorageException if operation fails
     */
    public boolean removeItem(@NotNull String material, int amount) throws StorageException {
        return StorageAPI.removeItem(player, material, amount);
    }

    /**
     * Set exact amount of material in storage
     *
     * @param material Material name
     * @param amount   New amount
     */
    public void setStoredAmount(@NotNull String material, int amount) {
        MineManager.setBlock(player, material, Math.max(0, amount));
    }

    /**
     * Clear all of specific material from storage
     *
     * @param material Material name
     */
    public void clearMaterial(@NotNull String material) {
        setStoredAmount(material, 0);
    }

    /**
     * Get maximum storage capacity
     *
     * @return Maximum capacity
     */
    public int getMaxStorage() {
        return StorageAPI.getMaxStorage(player);
    }

    /**
     * Set maximum storage capacity
     *
     * @param amount New maximum capacity
     */
    public void setMaxStorage(int amount) {
        MineManager.playermaxdata.put(player, Math.max(0, amount));
    }

    /**
     * Get total number of items stored across all materials
     *
     * @return Total items count
     */
    public int getTotalStoredItems() {
        return MineManager.getTotalPlayerItems(player);
    }

    /**
     * Get all materials that player has stored
     *
     * @return Set of material names
     */
    @NotNull
    public Set<String> getStoredMaterials() {
        return MineManager.playerdata.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(player.getName() + "_"))
                .filter(entry -> entry.getValue() > 0)
                .map(entry -> entry.getKey().substring(player.getName().length() + 1))
                .collect(Collectors.toSet());
    }

    /**
     * Get all stored materials with their amounts
     *
     * @return Map of material -> amount
     */
    @NotNull
    public Map<String, Integer> getStoredMaterialsWithAmounts() {
        Map<String, Integer> result = new HashMap<>();
        for (String material : getStoredMaterials()) {
            result.put(material, getStoredAmount(material));
        }
        return result;
    }

    /**
     * Check if storage is empty
     *
     * @return true if no items stored
     */
    public boolean isStorageEmpty() {
        return getTotalStoredItems() == 0;
    }

    /**
     * Check if storage is full
     *
     * @return true if storage is at maximum capacity
     */
    public boolean isStorageFull() {
        return getTotalStoredItems() >= getMaxStorage();
    }

    /**
     * Get remaining storage space
     *
     * @return Available space
     */
    public int getRemainingSpace() {
        return MineManager.getAvailableSpace(player);
    }

    /**
     * Get storage usage percentage
     *
     * @return Usage percentage (0.0 to 1.0)
     */
    public double getStorageUsagePercentage() {
        int max = getMaxStorage();
        if (max <= 0) return 0.0;
        return (double) getTotalStoredItems() / max;
    }

    // ==================== TOGGLE METHODS ====================

    /**
     * Check if auto-pickup is enabled
     *
     * @return true if enabled
     */
    public boolean isStorageEnabled() {
        return StorageAPI.isStorageEnabled(player);
    }

    /**
     * Set auto-pickup state
     *
     * @param enabled New state
     */
    public void setStorageEnabled(boolean enabled) {
        StorageAPI.toggleStorage(player, enabled);
    }

    /**
     * Enable auto-pickup
     */
    public void enableStorage() {
        StorageAPI.toggleStorage(player, true);
    }

    /**
     * Disable auto-pickup
     */
    public void disableStorage() {
        StorageAPI.toggleStorage(player, false);
    }

    /**
     * Toggle auto-pickup on/off
     */
    public void toggleStorage() {
        StorageAPI.toggleStorage(player, !isStorageEnabled());
    }

    // ==================== TRANSFER METHODS ====================

    /**
     * Transfer item to another player
     *
     * @param receiver Target player
     * @param material Material to transfer
     * @param amount   Amount to transfer
     * @return true if transfer started successfully
     * @throws StorageException if transfer fails
     */
    public boolean transferTo(@NotNull Player receiver, @NotNull String material, int amount)
            throws StorageException {
        return StorageAPI.transferItem(player, receiver, material, amount);
    }

    /**
     * Transfer item to another StoragePlayer
     *
     * @param receiver Target StoragePlayer
     * @param material Material to transfer
     * @param amount   Amount to transfer
     * @return true if transfer started successfully
     * @throws StorageException if transfer fails
     */
    public boolean transferTo(@NotNull StoragePlayer receiver, @NotNull String material, int amount)
            throws StorageException {
        return transferTo(receiver.getPlayer(), material, amount);
    }

    /**
     * Check if can transfer to another player
     *
     * @param receiver Target player
     * @param material Material to transfer
     * @param amount   Amount to transfer
     * @return true if transfer is possible
     */
    public boolean canTransferTo(@NotNull Player receiver, @NotNull String material, int amount) {
        return StorageAPI.canTransfer(player, receiver, material, amount);
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Check if player is online
     *
     * @return true if online
     */
    public boolean isOnline() {
        return player.isOnline();
    }

    /**
     * Save player data to database
     */
    public void saveData() {
        MineManager.savePlayerData(player);
    }

    /**
     * Load player data from database
     */
    public void loadData() {
        MineManager.loadPlayerData(player);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        StoragePlayer that = (StoragePlayer) obj;
        return player.getUniqueId().equals(that.player.getUniqueId());
    }

    @Override
    public int hashCode() {
        return player.getUniqueId().hashCode();
    }

    @Override
    public String toString() {
        return "StoragePlayer{" +
                "name=" + getName() +
                ", uuid=" + getUUID() +
                ", totalItems=" + getTotalStoredItems() +
                ", maxStorage=" + getMaxStorage() +
                ", enabled=" + isStorageEnabled() +
                '}';
    }
}
