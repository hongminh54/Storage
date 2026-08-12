package net.danh.storage.API;

import net.danh.storage.API.events.SpecialMaterialDropEvent;
import net.danh.storage.Manager.SpecialMaterial.SpecialMaterialManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * API for special materials system
 * Provides methods for managing rare materials with custom effects
 *
 * @author hongminh54
 * @version 2.3.3
 */
public class SpecialMaterialAPI {

    /**
     * Check if a special material exists
     *
     * @param materialId Special material ID
     * @return true if material exists
     */
    public static boolean hasSpecialMaterial(@NotNull String materialId) {
        return SpecialMaterialManager.hasMaterial(materialId);
    }

    /**
     * Get all loaded special material IDs
     *
     * @return Set of material IDs
     */
    @NotNull
    public static Set<String> getAllSpecialMaterials() {
        return SpecialMaterialManager.getLoadedMaterialIds();
    }

    /**
     * Get number of loaded special materials
     *
     * @return Count of special materials
     */
    public static int getLoadedMaterialsCount() {
        return SpecialMaterialManager.getLoadedMaterialsCount();
    }

    /**
     * Get detailed information about a special material
     *
     * @param materialId Special material ID
     * @return Formatted info string or null if not found
     */
    @Nullable
    public static String getMaterialInfo(@NotNull String materialId) {
        return SpecialMaterialManager.getMaterialInfo(materialId);
    }

    /**
     * Give special material to player
     *
     * @param player     The player
     * @param materialId Special material ID
     * @param amount     Amount to give
     * @return true if successfully given
     */
    public static boolean giveSpecialMaterial(@NotNull Player player, @NotNull String materialId, int amount) {
        if (amount <= 0) {
            return false;
        }

        if (!hasSpecialMaterial(materialId)) {
            return false;
        }

        return SpecialMaterialManager.giveSpecialMaterial(player, materialId, amount);
    }

    /**
     * Check for special material drop from block
     * This will trigger drop chance calculation and effects
     *
     * @param player The player who broke the block
     * @param block  The block that was broken
     */
    public static void checkSpecialMaterialDrop(@NotNull Player player, @NotNull Block block) {
        checkSpecialMaterialDrop(player, block, null);
    }

    /**
     * Check for special material drop from block with enchant modifier
     *
     * @param player      The player who broke the block
     * @param block       The block that was broken
     * @param enchantType Enchant type that may affect drop chance
     */
    public static void checkSpecialMaterialDrop(@NotNull Player player, @NotNull Block block,
                                                @Nullable String enchantType) {
        // Fire event before checking
        SpecialMaterialDropEvent event = new SpecialMaterialDropEvent(player, block, enchantType);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return;
        }

        SpecialMaterialManager.checkSpecialMaterialDrop(player, block, event.getEnchantType());
    }

    /**
     * Check for special material drop from a storage source (crop or mob)
     *
     * @param player     The player who harvested the crop / killed the mob
     * @param sourceType Source type: "crop" or "mob"
     * @param sourceKey  Crop item name or mob name
     * @param location   Location where the drop happens
     * @param stored     Whether the crop/mob drops were stored into the storage
     */
    public static void checkSpecialMaterialDrop(@NotNull Player player, @NotNull String sourceType,
                                                @NotNull String sourceKey, @NotNull Location location,
                                                boolean stored) {
        SpecialMaterialDropEvent event = new SpecialMaterialDropEvent(player, null, null, sourceType, sourceKey);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return;
        }

        SpecialMaterialManager.checkSpecialMaterialDrop(player, sourceType, sourceKey, location, stored);
    }

    /**
     * Reload special materials configuration
     */
    public static void reloadSpecialMaterials() {
        SpecialMaterialManager.loadSpecialMaterials();
    }

    /**
     * Check if special material system is enabled
     *
     * @return true if system is enabled
     */
    public static boolean isSystemEnabled() {
        return getLoadedMaterialsCount() > 0;
    }

    /**
     * Get all special materials as formatted list
     *
     * @return Formatted string with all materials
     */
    @NotNull
    public static String getAllMaterialsInfo() {
        Set<String> materials = getAllSpecialMaterials();
        if (materials.isEmpty()) {
            return "No special materials loaded";
        }

        StringBuilder info = new StringBuilder();
        info.append("Special Materials (").append(materials.size()).append("):\n");

        for (String materialId : materials) {
            String materialInfo = getMaterialInfo(materialId);
            if (materialInfo != null) {
                info.append("- ").append(materialInfo).append("\n");
            }
        }

        return info.toString();
    }
}
