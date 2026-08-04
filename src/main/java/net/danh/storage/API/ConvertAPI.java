package net.danh.storage.API;

import net.danh.storage.API.events.MaterialConvertEvent;
import net.danh.storage.Manager.ConvertOreManager;
import net.danh.storage.Manager.MineManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API for material conversion system
 * Provides methods for converting materials between different forms (ingots to blocks, etc.)
 *
 * @author hongminh54
 * @version 2.3.3
 */
public class ConvertAPI {

    /**
     * Get all materials that can be converted
     *
     * @return List of convertible material names
     */
    @NotNull
    public static List<String> getConvertibleMaterials() {
        return ConvertOreManager.getConvertibleMaterials();
    }

    /**
     * Check if a material can be converted
     *
     * @param material Material name
     * @return true if material is convertible
     */
    public static boolean isConvertibleMaterial(@NotNull String material) {
        return ConvertOreManager.isConvertibleMaterial(material);
    }

    /**
     * Check if a material has conversion options available
     *
     * @param material Material name
     * @return true if conversion options exist
     */
    public static boolean hasConvertOptions(@NotNull String material) {
        return ConvertOreManager.hasConvertOptions(material);
    }

    /**
     * Get all conversion options for a specific material
     *
     * @param material Material name
     * @return List of conversion options
     */
    @NotNull
    public static List<ConvertOreManager.ConvertOption> getConversionOptions(@NotNull String material) {
        return ConvertOreManager.getConvertOptions(material);
    }

    /**
     * Get specific conversion option between two materials
     *
     * @param fromMaterial Source material
     * @param toMaterial   Target material
     * @return ConvertOption or null if not available
     */
    @Nullable
    public static ConvertOreManager.ConvertOption getConversionOption(@NotNull String fromMaterial,
                                                                      @NotNull String toMaterial) {
        return ConvertOreManager.getConvertOption(fromMaterial, toMaterial);
    }

    /**
     * Check if conversion is possible
     *
     * @param player       The player
     * @param fromMaterial Source material
     * @param toMaterial   Target material
     * @param amount       Amount to convert
     * @return true if conversion is possible
     */
    public static boolean canConvert(@NotNull Player player, @NotNull String fromMaterial,
                                     @NotNull String toMaterial, int amount) {
        if (!ConvertOreManager.canConvert(fromMaterial, toMaterial, amount)) {
            return false;
        }

        int playerAmount = MineManager.getPlayerBlock(player, fromMaterial);
        return playerAmount >= amount;
    }

    /**
     * Convert material for player
     *
     * @param player       The player
     * @param fromMaterial Source material
     * @param toMaterial   Target material
     * @param amount       Amount to convert
     * @return true if conversion successful
     */
    public static boolean convertMaterial(@NotNull Player player, @NotNull String fromMaterial,
                                          @NotNull String toMaterial, int amount) {
        if (amount <= 0) {
            return false;
        }

        ConvertOreManager.ConvertOption option = getConversionOption(fromMaterial, toMaterial);
        if (option == null) {
            return false;
        }

        int playerAmount = MineManager.getPlayerBlock(player, fromMaterial);
        if (playerAmount < amount) {
            return false;
        }

        // Fire event
        MaterialConvertEvent event = new MaterialConvertEvent(player, fromMaterial, toMaterial, amount);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return false;
        }

        // Calculate result amount
        int conversions = amount / option.fromAmount();
        int resultAmount = option.calculateResultAmount(conversions);

        // Remove source material
        if (!MineManager.removeBlockAmount(player, fromMaterial, amount, false)) {
            return false;
        }

        // Add target material
        MineManager.addBlockAmount(player, toMaterial, resultAmount, false);

        return true;
    }

    /**
     * Get maximum number of conversions possible for player
     *
     * @param player       The player
     * @param fromMaterial Source material
     * @param toMaterial   Target material
     * @return Maximum conversions possible
     */
    public static int getMaxConversions(@NotNull Player player, @NotNull String fromMaterial,
                                        @NotNull String toMaterial) {
        ConvertOreManager.ConvertOption option = getConversionOption(fromMaterial, toMaterial);
        if (option == null) {
            return 0;
        }

        int playerAmount = MineManager.getPlayerBlock(player, fromMaterial);
        return option.calculateMaxConversions(playerAmount);
    }

    /**
     * Calculate result amount from conversion
     *
     * @param fromMaterial Source material
     * @param toMaterial   Target material
     * @param amount       Amount to convert
     * @return Result amount or 0 if conversion not possible
     */
    public static int calculateResultAmount(@NotNull String fromMaterial, @NotNull String toMaterial, int amount) {
        ConvertOreManager.ConvertOption option = getConversionOption(fromMaterial, toMaterial);
        if (option == null) {
            return 0;
        }

        int conversions = amount / option.fromAmount();
        return option.calculateResultAmount(conversions);
    }

    /**
     * Get conversion info as formatted string
     *
     * @param material Material name
     * @return Formatted conversion info
     */
    @NotNull
    public static String getConversionInfo(@NotNull String material) {
        return ConvertOreManager.getConversionInfo(material);
    }

    /**
     * Get conversion ratio between two materials
     *
     * @param fromMaterial Source material
     * @param toMaterial   Target material
     * @return Map with "from" and "to" amounts, or empty if not available
     */
    @NotNull
    public static Map<String, Integer> getConversionRatio(@NotNull String fromMaterial, @NotNull String toMaterial) {
        ConvertOreManager.ConvertOption option = getConversionOption(fromMaterial, toMaterial);
        if (option == null) {
            return new HashMap<>();
        }

        Map<String, Integer> ratio = new HashMap<>();
        ratio.put("from", option.fromAmount());
        ratio.put("to", option.toAmount());
        return ratio;
    }

    /**
     * Batch convert multiple materials for player
     *
     * @param player      The player
     * @param conversions Map of fromMaterial -> (toMaterial, amount)
     * @return Map of fromMaterial -> success status
     */
    @NotNull
    public static Map<String, Boolean> batchConvert(@NotNull Player player,
                                                    @NotNull Map<String, ConversionRequest> conversions) {
        Map<String, Boolean> results = new HashMap<>();

        for (Map.Entry<String, ConversionRequest> entry : conversions.entrySet()) {
            String fromMaterial = entry.getKey();
            ConversionRequest request = entry.getValue();

            boolean success = convertMaterial(player, fromMaterial, request.toMaterial, request.amount);
            results.put(fromMaterial, success);
        }

        return results;
    }

    /**
     * Reload conversion configurations
     */
    public static void reloadConversions() {
        ConvertOreManager.reloadConvertConfig();
    }

    /**
     * Get number of loaded conversions
     *
     * @return Number of conversion pairs loaded
     */
    public static int getLoadedConversionsCount() {
        return ConvertOreManager.getLoadedConversionsCount();
    }

    /**
     * Conversion request helper class
     */
    public record ConversionRequest(String toMaterial, int amount) {
        public ConversionRequest(@NotNull String toMaterial, int amount) {
            this.toMaterial = toMaterial;
            this.amount = amount;
        }
    }
}
