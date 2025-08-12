package net.danh.storage.API;

import net.danh.storage.API.events.StorageDepositEvent;
import net.danh.storage.API.events.StorageToggleEvent;
import net.danh.storage.API.events.StorageTransferEvent;
import net.danh.storage.API.events.StorageWithdrawEvent;
import net.danh.storage.API.exceptions.InvalidMaterialException;
import net.danh.storage.API.exceptions.StorageException;
import net.danh.storage.API.exceptions.StorageFullException;
import net.danh.storage.Manager.EnchantManager;
import net.danh.storage.Manager.MineManager;
import net.danh.storage.Manager.TransferManager;
import net.danh.storage.Storage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

/**
 * Main API class for Storage plugin
 * Provides static methods for external plugins to interact with Storage system
 *
 * @author VoChiDanh, hongminh54
 * @version 2.3.2
 */
public class StorageAPI {

    private static Storage plugin;
    private static boolean initialized = false;

    /**
     * Initialize the API - Called internally by Storage plugin
     *
     * @param storagePlugin The Storage plugin instance
     */
    public static void initialize(@NotNull Storage storagePlugin) {
        plugin = storagePlugin;
        initialized = true;
    }

    /**
     * Check if API is initialized and ready to use
     *
     * @return true if API is ready
     */
    public static boolean isInitialized() {
        return initialized && plugin != null;
    }

    /**
     * Get Storage plugin instance
     *
     * @return Storage plugin instance
     * @throws IllegalStateException if API not initialized
     */
    @NotNull
    public static Storage getPlugin() {
        if (!isInitialized()) {
            throw new IllegalStateException("StorageAPI not initialized");
        }
        return plugin;
    }

    // ==================== PLAYER STORAGE MANAGEMENT ====================

    /**
     * Get StoragePlayer wrapper for a player
     *
     * @param player The player
     * @return StoragePlayer wrapper
     */
    @NotNull
    public static StoragePlayer getStoragePlayer(@NotNull Player player) {
        return new StoragePlayer(player);
    }

    /**
     * Add item to player's storage
     *
     * @param player   The player
     * @param material Material name (e.g., "STONE;0")
     * @param amount   Amount to add
     * @return true if successfully added
     * @throws StorageException if operation fails
     */
    public static boolean addItem(@NotNull Player player, @NotNull String material, int amount)
            throws StorageException {
        if (!isInitialized()) {
            throw new IllegalStateException("StorageAPI not initialized");
        }

        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        // Fire event
        StorageDepositEvent event = new StorageDepositEvent(player, material, amount);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return false;
        }

        boolean result = MineManager.addBlockAmount(player, material, event.getAmount());
        if (!result) {
            throw new StorageFullException("Storage is full or invalid material");
        }

        return true;
    }

    /**
     * Remove item from player's storage
     *
     * @param player   The player
     * @param material Material name
     * @param amount   Amount to remove
     * @return true if successfully removed
     * @throws StorageException if operation fails
     */
    public static boolean removeItem(@NotNull Player player, @NotNull String material, int amount)
            throws StorageException {
        if (!isInitialized()) {
            throw new IllegalStateException("StorageAPI not initialized");
        }

        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        // Fire event
        StorageWithdrawEvent event = new StorageWithdrawEvent(player, material, amount);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return false;
        }

        return MineManager.removeBlockAmount(player, material, event.getAmount());
    }

    /**
     * Get amount of specific item in player's storage
     *
     * @param player   The player
     * @param material Material name
     * @return Amount of item
     */
    public static int getItemAmount(@NotNull Player player, @NotNull String material) {
        if (!isInitialized()) {
            return 0;
        }
        return MineManager.getPlayerBlock(player, material);
    }

    /**
     * Get player's maximum storage capacity
     *
     * @param player The player
     * @return Maximum storage capacity
     */
    public static int getMaxStorage(@NotNull Player player) {
        if (!isInitialized()) {
            return 0;
        }
        return MineManager.getMaxBlock(player);
    }

    /**
     * Set player's maximum storage capacity
     *
     * @param player The player
     * @param amount New maximum capacity
     */
    public static void setMaxStorage(@NotNull Player player, int amount) {
        if (!isInitialized()) {
            return;
        }
        MineManager.playermaxdata.put(player, Math.max(0, amount));
    }

    /**
     * Check if player has storage enabled (auto-pickup)
     *
     * @param player The player
     * @return true if storage is enabled
     */
    public static boolean isStorageEnabled(@NotNull Player player) {
        if (!isInitialized()) {
            return false;
        }
        return MineManager.getToggleStatus(player);
    }

    /**
     * Toggle player's storage on/off
     *
     * @param player  The player
     * @param enabled New state
     */
    public static void toggleStorage(@NotNull Player player, boolean enabled) {
        if (!isInitialized()) {
            return;
        }

        // Fire event
        StorageToggleEvent event = new StorageToggleEvent(player, enabled);
        Bukkit.getPluginManager().callEvent(event);

        if (!event.isCancelled()) {
            MineManager.toggle.put(player, event.getNewState());
        }
    }

    /**
     * Get all materials stored by player
     *
     * @param player The player
     * @return Set of material names
     */
    @NotNull
    public static Set<String> getStoredMaterials(@NotNull Player player) {
        return getStoragePlayer(player).getStoredMaterials();
    }

    // ==================== TRANSFER SYSTEM ====================

    /**
     * Transfer item between players
     *
     * @param sender   Sender player
     * @param receiver Receiver player
     * @param material Material to transfer
     * @param amount   Amount to transfer
     * @return true if transfer started successfully
     * @throws StorageException if transfer fails
     */
    public static boolean transferItem(@NotNull Player sender, @NotNull Player receiver,
                                       @NotNull String material, int amount) throws StorageException {
        if (!isInitialized()) {
            throw new IllegalStateException("StorageAPI not initialized");
        }

        // Fire event
        StorageTransferEvent event = new StorageTransferEvent(sender, receiver, material, amount);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return false;
        }

        return TransferManager.executeTransfer(sender, receiver.getName(), material, event.getAmount());
    }

    /**
     * Check if transfer is possible
     *
     * @param sender   Sender player
     * @param receiver Receiver player
     * @param material Material to transfer
     * @param amount   Amount to transfer
     * @return true if transfer is possible
     */
    public static boolean canTransfer(@NotNull Player sender, @NotNull Player receiver,
                                      @NotNull String material, int amount) {
        if (!isInitialized()) {
            return false;
        }
        return TransferManager.canTransfer(sender, receiver.getName(), material, amount);
    }

    // ==================== ENCHANT SYSTEM ====================

    /**
     * Add custom enchant to item
     *
     * @param item        ItemStack to enchant
     * @param enchantName Enchant name
     * @param level       Enchant level
     * @return Enchanted ItemStack
     * @throws InvalidMaterialException if enchant cannot be applied
     */
    @NotNull
    public static ItemStack addEnchant(@NotNull ItemStack item, @NotNull String enchantName, int level)
            throws InvalidMaterialException {
        if (!isInitialized()) {
            return item;
        }

        if (!EnchantManager.isValidEnchant(enchantName)) {
            throw new InvalidMaterialException("Invalid enchant: " + enchantName);
        }

        if (!EnchantManager.isApplicableItem(item, enchantName)) {
            throw new InvalidMaterialException("Enchant cannot be applied to this item");
        }

        return EnchantManager.addEnchant(item, enchantName, level);
    }

    /**
     * Remove custom enchant from item
     *
     * @param item        ItemStack
     * @param enchantName Enchant name to remove
     * @return ItemStack without enchant
     */
    @NotNull
    public static ItemStack removeEnchant(@NotNull ItemStack item, @NotNull String enchantName) {
        if (!isInitialized()) {
            return item;
        }
        return EnchantManager.removeEnchant(item, enchantName);
    }

    /**
     * Get enchant level on item
     *
     * @param item        ItemStack
     * @param enchantName Enchant name
     * @return Enchant level (0 if not present)
     */
    public static int getEnchantLevel(@NotNull ItemStack item, @NotNull String enchantName) {
        if (!isInitialized()) {
            return 0;
        }
        return EnchantManager.getEnchantLevel(item, enchantName);
    }

    /**
     * Check if enchant name is valid
     *
     * @param enchantName Enchant name
     * @return true if valid
     */
    public static boolean isValidEnchant(@NotNull String enchantName) {
        if (!isInitialized()) {
            return false;
        }
        return EnchantManager.isValidEnchant(enchantName);
    }

    /**
     * Get all available custom enchants
     *
     * @return Set of enchant names
     */
    @NotNull
    public static Set<String> getAvailableEnchants() {
        if (!isInitialized()) {
            return Set.of();
        }
        return EnchantManager.getAvailableEnchants();
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Get all materials that can be stored
     *
     * @return List of storable materials
     */
    @NotNull
    public static List<String> getStorableMaterials() {
        if (!isInitialized()) {
            return List.of();
        }
        return MineManager.getPluginBlocks();
    }

    /**
     * Check if material can be stored
     *
     * @param material Material name
     * @return true if storable
     */
    public static boolean isStorableMaterial(@NotNull String material) {
        return getStorableMaterials().contains(material);
    }

    /**
     * Reload API data from config files
     * This is called automatically when plugin reloads
     */
    public static void reload() {
        if (!isInitialized()) {
        }
        // API data will be reloaded when managers reload
    }

    /**
     * Shutdown API - Called internally by Storage plugin
     */
    public static void shutdown() {
        initialized = false;
        plugin = null;
    }
}
