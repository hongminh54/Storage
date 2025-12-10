package net.danh.storage.API.impl;

import net.danh.storage.API.exceptions.InvalidMaterialException;
import net.danh.storage.API.interfaces.IEnchantManager;
import net.danh.storage.Manager.EnchantManager;
import net.danh.storage.Manager.EnchantManager.EnchantData;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class EnchantManagerImpl implements IEnchantManager {

    private static EnchantManagerImpl instance;

    private EnchantManagerImpl() {
    }

    public static EnchantManagerImpl getInstance() {
        if (instance == null) {
            instance = new EnchantManagerImpl();
        }
        return instance;
    }

    @NotNull
    @Override
    public ItemStack addEnchant(@NotNull ItemStack item, @NotNull String enchantName, int level)
            throws InvalidMaterialException {
        if (!isValidEnchant(enchantName)) {
            throw new InvalidMaterialException("Invalid enchant: " + enchantName);
        }
        if (!isValidLevel(enchantName, level)) {
            throw new InvalidMaterialException("Invalid level " + level + " for enchant: " + enchantName);
        }
        if (!isApplicableItem(item, enchantName)) {
            throw new InvalidMaterialException("Enchant " + enchantName + " cannot be applied to this item");
        }
        return EnchantManager.addEnchant(item, enchantName, level);
    }

    @NotNull
    @Override
    public ItemStack removeEnchant(@NotNull ItemStack item, @NotNull String enchantName) {
        return EnchantManager.removeEnchant(item, enchantName);
    }

    @NotNull
    @Override
    public ItemStack removeAllEnchants(@NotNull ItemStack item) {
        ItemStack result = item.clone();
        for (String enchant : getEnchants(item)) {
            result = EnchantManager.removeEnchant(result, enchant);
        }
        return result;
    }

    @Override
    public int getEnchantLevel(@NotNull ItemStack item, @NotNull String enchantName) {
        return EnchantManager.getEnchantLevel(item, enchantName);
    }

    @Override
    public boolean hasEnchant(@NotNull ItemStack item, @NotNull String enchantName) {
        return EnchantManager.hasEnchant(item, enchantName);
    }

    @Override
    public boolean hasAnyEnchants(@NotNull ItemStack item) {
        return !getEnchants(item).isEmpty();
    }

    @NotNull
    @Override
    public List<String> getEnchants(@NotNull ItemStack item) {
        return EnchantManager.getAvailableEnchants().stream()
                .filter(e -> EnchantManager.hasEnchant(item, e))
                .collect(Collectors.toList());
    }

    @Override
    public boolean isValidEnchant(@NotNull String enchantName) {
        return EnchantManager.isValidEnchant(enchantName);
    }

    @Override
    public boolean isValidLevel(@NotNull String enchantName, int level) {
        return EnchantManager.isValidLevel(enchantName, level);
    }

    @Override
    public boolean isApplicableItem(@NotNull ItemStack item, @NotNull String enchantName) {
        return EnchantManager.isApplicableItem(item, enchantName);
    }

    @Nullable
    @Override
    public EnchantData getEnchantData(@NotNull String enchantName) {
        return EnchantManager.getEnchantData(enchantName);
    }

    @NotNull
    @Override
    public Set<String> getAvailableEnchants() {
        return EnchantManager.getAvailableEnchants();
    }

    @NotNull
    @Override
    public Set<String> getApplicableEnchants(@NotNull ItemStack item) {
        return EnchantManager.getAvailableEnchants().stream()
                .filter(e -> EnchantManager.isApplicableItem(item, e))
                .collect(Collectors.toSet());
    }

    @Override
    public int getMaxLevel(@NotNull String enchantName) {
        EnchantData data = EnchantManager.getEnchantData(enchantName);
        return data != null ? data.maxLevel : 0;
    }

    @NotNull
    @Override
    public String getEnchantDisplayName(@NotNull String enchantName) {
        String displayName = EnchantManager.getEnchantDisplayName(enchantName);
        return displayName != null ? displayName : enchantName;
    }

    @NotNull
    @Override
    public String getEnchantDescription(@NotNull String enchantName) {
        EnchantData data = EnchantManager.getEnchantData(enchantName);
        return data != null && data.description != null ? data.description : "";
    }

    @Override
    public boolean isEnchantEnabled(@NotNull String enchantName) {
        // All loaded enchants are enabled by default
        return isValidEnchant(enchantName);
    }

    @NotNull
    @Override
    public String getEnchantRarity(@NotNull String enchantName) {
        // Rarity based on max level - higher max level = rarer
        EnchantData data = EnchantManager.getEnchantData(enchantName);
        if (data == null) return "common";

        int maxLevel = data.maxLevel;
        if (maxLevel >= 10) return "legendary";
        if (maxLevel >= 7) return "epic";
        if (maxLevel >= 5) return "rare";
        if (maxLevel >= 3) return "uncommon";
        return "common";
    }

    @Override
    public boolean hasConflict(@NotNull String enchant1, @NotNull String enchant2) {
        // Current implementation: no conflicts defined
        return false;
    }

    @NotNull
    @Override
    public Set<String> getConflictingEnchants(@NotNull String enchantName) {
        // Current implementation: no conflicts defined
        return Collections.emptySet();
    }

    @Override
    public void reloadEnchants() {
        EnchantManager.loadEnchants();
    }

    @Override
    public int getLoadedEnchantsCount() {
        return EnchantManager.getAvailableEnchants().size();
    }
}
