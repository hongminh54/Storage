package net.danh.storage.API;

import net.danh.storage.Utils.File;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Wrapper class for ItemStack with storage-specific functionality
 * Provides convenient methods to work with storage items
 *
 * @author VoChiDanh, hongminh54
 * @version 2.3.2
 */
public class StorageItem {

    private final String material;
    private final int amount;
    private final ItemStack itemStack;

    /**
     * Create StorageItem from material string
     *
     * @param material Material string (e.g., "STONE;0")
     * @param amount   Item amount
     */
    public StorageItem(@NotNull String material, int amount) {
        this.material = material;
        this.amount = Math.max(0, amount);
        this.itemStack = createItemStack(material, this.amount);
    }

    /**
     * Create StorageItem from ItemStack
     *
     * @param itemStack The ItemStack
     */
    public StorageItem(@NotNull ItemStack itemStack) {
        this.itemStack = itemStack.clone();
        this.amount = itemStack.getAmount();
        this.material = convertItemStackToMaterial(itemStack);
    }

    /**
     * Create StorageItem from material and amount
     *
     * @param material Bukkit Material
     * @param amount   Item amount
     */
    public StorageItem(@NotNull Material material, int amount) {
        this(material.name() + ";0", amount);
    }

    // ==================== GETTER METHODS ====================

    /**
     * Create StorageItem from material name
     *
     * @param materialName Material name (e.g., "STONE")
     * @param amount       Amount
     * @return StorageItem instance
     */
    @NotNull
    public static StorageItem of(@NotNull String materialName, int amount) {
        return new StorageItem(materialName + ";0", amount);
    }

    /**
     * Create StorageItem from Bukkit Material
     *
     * @param material Bukkit Material
     * @param amount   Amount
     * @return StorageItem instance
     */
    @NotNull
    public static StorageItem of(@NotNull Material material, int amount) {
        return new StorageItem(material, amount);
    }

    /**
     * Create StorageItem from ItemStack
     *
     * @param itemStack ItemStack
     * @return StorageItem instance
     */
    @NotNull
    public static StorageItem from(@NotNull ItemStack itemStack) {
        return new StorageItem(itemStack);
    }

    /**
     * Create empty StorageItem
     *
     * @return Empty StorageItem
     */
    @NotNull
    public static StorageItem empty() {
        return new StorageItem(Material.AIR, 0);
    }

    /**
     * Get material string used by Storage plugin
     *
     * @return Material string (e.g., "STONE;0")
     */
    @NotNull
    public String getMaterial() {
        return material;
    }

    /**
     * Get item amount
     *
     * @return Item amount
     */
    public int getAmount() {
        return amount;
    }

    // ==================== VALIDATION METHODS ====================

    /**
     * Get Bukkit Material type
     *
     * @return Material type
     */
    @NotNull
    public Material getType() {
        return itemStack != null ? itemStack.getType() : Material.AIR;
    }

    /**
     * Get ItemStack representation
     *
     * @return ItemStack or null if invalid
     */
    @Nullable
    public ItemStack getItemStack() {
        return itemStack != null ? itemStack.clone() : null;
    }

    /**
     * Get display name for this item
     *
     * @return Display name from config or material name
     */
    @NotNull
    public String getDisplayName() {
        String configName = File.getConfig().getString("items." + material);
        if (configName != null && !configName.isEmpty()) {
            return configName;
        }

        // Fallback to material name without data value
        String[] parts = material.split(";");
        return parts[0].toLowerCase().replace("_", " ");
    }

    /**
     * Get formatted display name with amount
     *
     * @return Formatted string like "Stone x64"
     */
    @NotNull
    public String getDisplayNameWithAmount() {
        return getDisplayName() + " x" + amount;
    }

    /**
     * Check if this item can be stored in Storage plugin
     *
     * @return true if storable
     */
    public boolean isStorable() {
        return StorageAPI.isStorableMaterial(material);
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Check if item is valid (not null/air)
     *
     * @return true if valid
     */
    public boolean isValid() {
        return itemStack != null && itemStack.getType() != Material.AIR && amount > 0;
    }

    /**
     * Check if item has custom enchants
     *
     * @return true if has custom enchants
     */
    public boolean hasCustomEnchants() {
        if (itemStack == null) return false;

        for (String enchant : StorageAPI.getAvailableEnchants()) {
            if (StorageAPI.getEnchantLevel(itemStack, enchant) > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if item is stackable
     *
     * @return true if stackable
     */
    public boolean isStackable() {
        return itemStack != null && itemStack.getMaxStackSize() > 1;
    }

    /**
     * Get maximum stack size for this item
     *
     * @return Maximum stack size
     */
    public int getMaxStackSize() {
        return itemStack != null ? itemStack.getMaxStackSize() : 1;
    }

    /**
     * Create new StorageItem with different amount
     *
     * @param newAmount New amount
     * @return New StorageItem instance
     */
    @NotNull
    public StorageItem withAmount(int newAmount) {
        return new StorageItem(material, newAmount);
    }

    /**
     * Add amount to current item
     *
     * @param additionalAmount Amount to add
     * @return New StorageItem with increased amount
     */
    @NotNull
    public StorageItem addAmount(int additionalAmount) {
        return withAmount(amount + additionalAmount);
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Subtract amount from current item
     *
     * @param subtractAmount Amount to subtract
     * @return New StorageItem with decreased amount
     */
    @NotNull
    public StorageItem subtractAmount(int subtractAmount) {
        return withAmount(Math.max(0, amount - subtractAmount));
    }

    /**
     * Split item into multiple stacks based on max stack size
     *
     * @return Array of StorageItems representing stacks
     */
    @NotNull
    public StorageItem[] splitIntoStacks() {
        if (!isValid()) {
            return new StorageItem[0];
        }

        int maxStack = getMaxStackSize();
        int stacks = (amount + maxStack - 1) / maxStack; // Ceiling division
        StorageItem[] result = new StorageItem[stacks];

        int remaining = amount;
        for (int i = 0; i < stacks; i++) {
            int stackAmount = Math.min(remaining, maxStack);
            result[i] = new StorageItem(material, stackAmount);
            remaining -= stackAmount;
        }

        return result;
    }

    // ==================== OBJECT METHODS ====================

    /**
     * Check if this item can be combined with another
     *
     * @param other Other StorageItem
     * @return true if materials match
     */
    public boolean canCombineWith(@NotNull StorageItem other) {
        return material.equals(other.material);
    }

    /**
     * Combine this item with another
     *
     * @param other Other StorageItem
     * @return New StorageItem with combined amounts
     */
    @NotNull
    public StorageItem combineWith(@NotNull StorageItem other) {
        if (!canCombineWith(other)) {
            return this;
        }
        return withAmount(amount + other.amount);
    }

    @Nullable
    private ItemStack createItemStack(String material, int amount) {
        try {
            String[] parts = material.split(";");
            Material bukkit_material = Material.valueOf(parts[0]);
            ItemStack item = new ItemStack(bukkit_material, amount);

            // Handle data value for older versions if needed
            if (parts.length > 1) {
                try {
                    short data = Short.parseShort(parts[1]);
                    if (data > 0) {
                        // Use deprecated method for compatibility with older versions
                        @SuppressWarnings("deprecation")
                        ItemStack tempItem = item;
                        tempItem.setDurability(data);
                    }
                } catch (NumberFormatException ignored) {
                    // Ignore invalid data values
                }
            }

            return item;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // ==================== STATIC FACTORY METHODS ====================

    @NotNull
    private String convertItemStackToMaterial(ItemStack item) {
        String materialName = item.getType().name();

        // Use deprecated method for compatibility with older versions
        @SuppressWarnings("deprecation")
        short durability = item.getDurability();

        // For older versions, include durability as data value
        if (durability > 0) {
            return materialName + ";" + durability;
        }

        return materialName + ";0";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        StorageItem that = (StorageItem) obj;
        return amount == that.amount && material.equals(that.material);
    }

    @Override
    public int hashCode() {
        return material.hashCode() * 31 + amount;
    }

    @Override
    public String toString() {
        return "StorageItem{" +
                "material='" + material + '\'' +
                ", amount=" + amount +
                ", displayName='" + getDisplayName() + '\'' +
                ", storable=" + isStorable() +
                ", valid=" + isValid() +
                '}';
    }
}
