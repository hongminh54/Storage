package net.danh.storage.Recipe;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemFlag;

import java.util.*;

public class Recipe {

    private final String id;
    private String name;
    private String category;
    private boolean enabled;

    private String resultMaterial;
    private String resultName;
    private List<String> resultLore;
    private Map<String, Integer> resultEnchantments;
    private int resultAmount;
    private int resultCustomModelData;
    private boolean resultUnbreakable;
    private Set<ItemFlag> resultFlags;
    private String resultItemNbt;
    private String resultItemBase64;

    private Map<String, Integer> materialRequirements;
    private String permissionRequirement;

    public Recipe(String id) {
        this.id = id;
        this.name = id;
        this.category = "default";
        this.enabled = true;
        this.resultMaterial = "STONE";
        this.resultName = "&7Custom Item";
        this.resultLore = new ArrayList<>();
        this.resultEnchantments = new HashMap<>();
        this.resultAmount = 1;
        this.resultCustomModelData = 0;
        this.resultUnbreakable = false;
        this.resultFlags = new HashSet<>();
        this.resultItemNbt = null;
        this.resultItemBase64 = null;
        this.materialRequirements = new HashMap<>();
        this.permissionRequirement = null;
    }

    public Recipe(String id, ConfigurationSection section) {
        this(id);
        loadFromConfig(section);
    }

    public void loadFromConfig(ConfigurationSection section) {
        if (section == null) return;

        this.name = section.getString("name", this.id);
        this.category = section.getString("category", "default");
        this.enabled = section.getBoolean("enabled", true);

        ConfigurationSection resultSection = section.getConfigurationSection("result");
        if (resultSection != null) {
            this.resultMaterial = resultSection.getString("material", "STONE");
            this.resultName = resultSection.getString("name", "&7Custom Item");
            this.resultLore = resultSection.getStringList("lore");
            this.resultAmount = resultSection.getInt("amount", 1);
            this.resultCustomModelData = resultSection.getInt("custom_model_data", 0);
            this.resultUnbreakable = resultSection.getBoolean("unbreakable", false);
            this.resultItemNbt = resultSection.getString("item_nbt", null);
            this.resultItemBase64 = resultSection.getString("item_base64", null);

            ConfigurationSection enchantSection = resultSection.getConfigurationSection("enchantments");
            if (enchantSection != null) {
                this.resultEnchantments.clear();
                for (String enchant : enchantSection.getKeys(false)) {
                    this.resultEnchantments.put(enchant, enchantSection.getInt(enchant));
                }
            }

            List<String> flagList = resultSection.getStringList("flags");
            this.resultFlags.clear();
            for (String flag : flagList) {
                try {
                    this.resultFlags.add(ItemFlag.valueOf(flag.toUpperCase()));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        ConfigurationSection reqSection = section.getConfigurationSection("requirements");
        if (reqSection != null) {
            ConfigurationSection materialsSection = reqSection.getConfigurationSection("materials");
            if (materialsSection != null) {
                this.materialRequirements.clear();
                for (String material : materialsSection.getKeys(false)) {
                    this.materialRequirements.put(material, materialsSection.getInt(material));
                }
            }

            if (reqSection.contains("permissions")) {
                List<String> oldPermissions = reqSection.getStringList("permissions");
                this.permissionRequirement = oldPermissions.isEmpty() ? null : oldPermissions.get(0);
            } else if (reqSection.contains("permission")) {
                this.permissionRequirement = reqSection.getString("permission");
            } else {
                this.permissionRequirement = null;
            }
        }
    }

    public void saveToConfig(ConfigurationSection section) {
        section.set("name", this.name);
        section.set("category", this.category);
        section.set("enabled", this.enabled);

        ConfigurationSection resultSection = section.createSection("result");
        resultSection.set("material", this.resultMaterial);
        resultSection.set("name", this.resultName);
        resultSection.set("lore", this.resultLore);
        resultSection.set("amount", this.resultAmount);
        resultSection.set("custom_model_data", this.resultCustomModelData);
        resultSection.set("unbreakable", this.resultUnbreakable);

        if (this.resultItemNbt != null && !this.resultItemNbt.trim().isEmpty()) {
            resultSection.set("item_nbt", this.resultItemNbt);
        }

        if (this.resultItemBase64 != null && !this.resultItemBase64.trim().isEmpty()) {
            resultSection.set("item_base64", this.resultItemBase64);
        }

        if (!this.resultEnchantments.isEmpty()) {
            ConfigurationSection enchantSection = resultSection.createSection("enchantments");
            for (Map.Entry<String, Integer> entry : this.resultEnchantments.entrySet()) {
                enchantSection.set(entry.getKey(), entry.getValue());
            }
        }

        if (!this.resultFlags.isEmpty()) {
            List<String> flagList = new ArrayList<>();
            for (ItemFlag flag : this.resultFlags) {
                flagList.add(flag.name());
            }
            resultSection.set("flags", flagList);
        }

        ConfigurationSection reqSection = section.createSection("requirements");
        if (!this.materialRequirements.isEmpty()) {
            ConfigurationSection materialsSection = reqSection.createSection("materials");
            for (Map.Entry<String, Integer> entry : this.materialRequirements.entrySet()) {
                materialsSection.set(entry.getKey(), entry.getValue());
            }
        }

        if (this.permissionRequirement != null && !this.permissionRequirement.trim().isEmpty()) {
            reqSection.set("permission", this.permissionRequirement);
        }
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getResultMaterial() {
        return resultMaterial;
    }

    public void setResultMaterial(String resultMaterial) {
        this.resultMaterial = resultMaterial;
    }

    public String getResultName() {
        return resultName;
    }

    public void setResultName(String resultName) {
        this.resultName = resultName;
    }

    public List<String> getResultLore() {
        return resultLore;
    }

    public void setResultLore(List<String> resultLore) {
        this.resultLore = resultLore;
    }

    public Map<String, Integer> getResultEnchantments() {
        return resultEnchantments;
    }

    public void setResultEnchantments(Map<String, Integer> resultEnchantments) {
        this.resultEnchantments = resultEnchantments;
    }

    public int getResultAmount() {
        return resultAmount;
    }

    public void setResultAmount(int resultAmount) {
        this.resultAmount = resultAmount;
    }

    public int getResultCustomModelData() {
        return resultCustomModelData;
    }

    public void setResultCustomModelData(int resultCustomModelData) {
        this.resultCustomModelData = resultCustomModelData;
    }

    public boolean isResultUnbreakable() {
        return resultUnbreakable;
    }

    public void setResultUnbreakable(boolean resultUnbreakable) {
        this.resultUnbreakable = resultUnbreakable;
    }

    public Set<ItemFlag> getResultFlags() {
        return resultFlags;
    }

    public void setResultFlags(Set<ItemFlag> resultFlags) {
        this.resultFlags = resultFlags;
    }

    public String getResultItemNbt() {
        return resultItemNbt;
    }

    public void setResultItemNbt(String resultItemNbt) {
        this.resultItemNbt = resultItemNbt;
    }

    public String getResultItemBase64() {
        return resultItemBase64;
    }

    public void setResultItemBase64(String resultItemBase64) {
        this.resultItemBase64 = resultItemBase64;
    }

    public Map<String, Integer> getMaterialRequirements() {
        return materialRequirements;
    }

    public void setMaterialRequirements(Map<String, Integer> materialRequirements) {
        this.materialRequirements = materialRequirements;
    }

    public String getPermissionRequirement() {
        return permissionRequirement;
    }

    public void setPermissionRequirement(String permissionRequirement) {
        this.permissionRequirement = permissionRequirement;
    }
}
