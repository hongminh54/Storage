package net.danh.storage.API.interfaces;

import net.danh.storage.API.exceptions.InvalidMaterialException;
import net.danh.storage.Manager.EnchantManager.EnchantData;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * Interface for managing custom enchantments
 * Provides methods for enchant operations and queries
 *
 * @author VoChiDanh, hongminh54
 * @version 2.3.2
 */
public interface IEnchantManager {

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
    ItemStack addEnchant(@NotNull ItemStack item, @NotNull String enchantName, int level)
            throws InvalidMaterialException;

    /**
     * Remove custom enchant from item
     *
     * @param item        ItemStack
     * @param enchantName Enchant name to remove
     * @return ItemStack without enchant
     */
    @NotNull
    ItemStack removeEnchant(@NotNull ItemStack item, @NotNull String enchantName);

    /**
     * Remove all custom enchants from item
     *
     * @param item ItemStack
     * @return ItemStack without custom enchants
     */
    @NotNull
    ItemStack removeAllEnchants(@NotNull ItemStack item);

    /**
     * Get enchant level on item
     *
     * @param item        ItemStack
     * @param enchantName Enchant name
     * @return Enchant level (0 if not present)
     */
    int getEnchantLevel(@NotNull ItemStack item, @NotNull String enchantName);

    /**
     * Check if item has specific enchant
     *
     * @param item        ItemStack
     * @param enchantName Enchant name
     * @return true if item has enchant
     */
    boolean hasEnchant(@NotNull ItemStack item, @NotNull String enchantName);

    /**
     * Check if item has any custom enchants
     *
     * @param item ItemStack
     * @return true if item has custom enchants
     */
    boolean hasAnyEnchants(@NotNull ItemStack item);

    /**
     * Get all custom enchants on item
     *
     * @param item ItemStack
     * @return List of enchant names
     */
    @NotNull
    List<String> getEnchants(@NotNull ItemStack item);

    /**
     * Check if enchant name is valid
     *
     * @param enchantName Enchant name
     * @return true if valid
     */
    boolean isValidEnchant(@NotNull String enchantName);

    /**
     * Check if enchant level is valid
     *
     * @param enchantName Enchant name
     * @param level       Enchant level
     * @return true if valid level
     */
    boolean isValidLevel(@NotNull String enchantName, int level);

    /**
     * Check if enchant can be applied to item
     *
     * @param item        ItemStack
     * @param enchantName Enchant name
     * @return true if applicable
     */
    boolean isApplicableItem(@NotNull ItemStack item, @NotNull String enchantName);

    /**
     * Get enchant data
     *
     * @param enchantName Enchant name
     * @return EnchantData or null if not found
     */
    @Nullable
    EnchantData getEnchantData(@NotNull String enchantName);

    /**
     * Get all available custom enchants
     *
     * @return Set of enchant names
     */
    @NotNull
    Set<String> getAvailableEnchants();

    /**
     * Get enchants applicable to specific item type
     *
     * @param item ItemStack
     * @return Set of applicable enchant names
     */
    @NotNull
    Set<String> getApplicableEnchants(@NotNull ItemStack item);

    /**
     * Get maximum level for enchant
     *
     * @param enchantName Enchant name
     * @return Maximum level (0 if enchant not found)
     */
    int getMaxLevel(@NotNull String enchantName);

    /**
     * Get enchant display name
     *
     * @param enchantName Enchant name
     * @return Display name or enchant name if not configured
     */
    @NotNull
    String getEnchantDisplayName(@NotNull String enchantName);

    /**
     * Get enchant description
     *
     * @param enchantName Enchant name
     * @return Description or empty string if not configured
     */
    @NotNull
    String getEnchantDescription(@NotNull String enchantName);

    /**
     * Check if enchant is enabled
     *
     * @param enchantName Enchant name
     * @return true if enabled
     */
    boolean isEnchantEnabled(@NotNull String enchantName);

    /**
     * Get enchant rarity
     *
     * @param enchantName Enchant name
     * @return Rarity string or "common" if not configured
     */
    @NotNull
    String getEnchantRarity(@NotNull String enchantName);

    /**
     * Check if enchant conflicts with another
     *
     * @param enchant1 First enchant name
     * @param enchant2 Second enchant name
     * @return true if they conflict
     */
    boolean hasConflict(@NotNull String enchant1, @NotNull String enchant2);

    /**
     * Get conflicting enchants for specific enchant
     *
     * @param enchantName Enchant name
     * @return Set of conflicting enchant names
     */
    @NotNull
    Set<String> getConflictingEnchants(@NotNull String enchantName);

    /**
     * Reload enchant configuration
     */
    void reloadEnchants();

    /**
     * Get total number of loaded enchants
     *
     * @return Number of enchants
     */
    int getLoadedEnchantsCount();
}
