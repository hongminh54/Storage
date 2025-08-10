package net.danh.storage.Recipe;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class Recipe {
    
    private String id;
    private String name;
    private String category;
    private boolean enabled;
    
    // Result item configuration
    private String resultMaterial;
    private String resultName;
    private List<String> resultLore;
    private Map<String, Integer> resultEnchantments;
    private int resultAmount;
    private int resultCustomModelData;
    private boolean resultUnbreakable;
    private Set<ItemFlag> resultFlags;
    private Map<String, String> customNBTData; // For custom plugin support

    // Requirements
    private Map<String, Integer> materialRequirements; // material;data -> amount
    private List<String> permissionRequirements;
    
    public Recipe(String id) {
        this.id = id;
        this.name = id;
        this.category = "default";
        this.enabled = true;
        this.resultMaterial = "STONE;0";
        this.resultName = "&7Custom Item";
        this.resultLore = new ArrayList<>();
        this.resultEnchantments = new HashMap<>();
        this.resultAmount = 1;
        this.resultCustomModelData = 0;
        this.resultUnbreakable = false;
        this.resultFlags = new HashSet<>();
        this.customNBTData = new HashMap<>();
        this.materialRequirements = new HashMap<>();
        this.permissionRequirements = new ArrayList<>();
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
        
        // Load result item configuration
        ConfigurationSection resultSection = section.getConfigurationSection("result");
        if (resultSection != null) {
            this.resultMaterial = resultSection.getString("material", "STONE;0");
            this.resultName = resultSection.getString("name", "&7Custom Item");
            this.resultLore = resultSection.getStringList("lore");
            this.resultAmount = resultSection.getInt("amount", 1);
            this.resultCustomModelData = resultSection.getInt("custom_model_data", 0);
            this.resultUnbreakable = resultSection.getBoolean("unbreakable", false);
            
            // Load enchantments
            ConfigurationSection enchantSection = resultSection.getConfigurationSection("enchantments");
            if (enchantSection != null) {
                this.resultEnchantments.clear();
                for (String enchant : enchantSection.getKeys(false)) {
                    this.resultEnchantments.put(enchant, enchantSection.getInt(enchant));
                }
            }
            
            // Load flags
            List<String> flagList = resultSection.getStringList("flags");
            this.resultFlags.clear();
            for (String flag : flagList) {
                try {
                    this.resultFlags.add(ItemFlag.valueOf(flag.toUpperCase()));
                } catch (IllegalArgumentException ignored) {}
            }

            // Load custom NBT data
            ConfigurationSection nbtSection = resultSection.getConfigurationSection("custom_nbt");
            if (nbtSection != null) {
                this.customNBTData.clear();
                for (String key : nbtSection.getKeys(false)) {
                    this.customNBTData.put(key, nbtSection.getString(key));
                }
            }
        }
        
        // Load requirements
        ConfigurationSection reqSection = section.getConfigurationSection("requirements");
        if (reqSection != null) {
            ConfigurationSection materialsSection = reqSection.getConfigurationSection("materials");
            if (materialsSection != null) {
                this.materialRequirements.clear();
                for (String material : materialsSection.getKeys(false)) {
                    this.materialRequirements.put(material, materialsSection.getInt(material));
                }
            }

            this.permissionRequirements = reqSection.getStringList("permissions");
        }
    }
    
    public void saveToConfig(ConfigurationSection section) {
        section.set("name", this.name);
        section.set("category", this.category);
        section.set("enabled", this.enabled);
        
        // Save result configuration
        ConfigurationSection resultSection = section.createSection("result");
        resultSection.set("material", this.resultMaterial);
        resultSection.set("name", this.resultName);
        resultSection.set("lore", this.resultLore);
        resultSection.set("amount", this.resultAmount);
        resultSection.set("custom_model_data", this.resultCustomModelData);
        resultSection.set("unbreakable", this.resultUnbreakable);
        
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

        // Save custom NBT data
        if (!this.customNBTData.isEmpty()) {
            ConfigurationSection nbtSection = resultSection.createSection("custom_nbt");
            for (Map.Entry<String, String> entry : this.customNBTData.entrySet()) {
                nbtSection.set(entry.getKey(), entry.getValue());
            }
        }
        
        // Save requirements
        ConfigurationSection reqSection = section.createSection("requirements");
        if (!this.materialRequirements.isEmpty()) {
            ConfigurationSection materialsSection = reqSection.createSection("materials");
            for (Map.Entry<String, Integer> entry : this.materialRequirements.entrySet()) {
                materialsSection.set(entry.getKey(), entry.getValue());
            }
        }
        
        if (!this.permissionRequirements.isEmpty()) {
            reqSection.set("permissions", this.permissionRequirements);
        }
    }
    
    // Getters and setters
    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public String getResultMaterial() { return resultMaterial; }
    public void setResultMaterial(String resultMaterial) { this.resultMaterial = resultMaterial; }
    public String getResultName() { return resultName; }
    public void setResultName(String resultName) { this.resultName = resultName; }
    public List<String> getResultLore() { return resultLore; }
    public void setResultLore(List<String> resultLore) { this.resultLore = resultLore; }
    public Map<String, Integer> getResultEnchantments() { return resultEnchantments; }
    public void setResultEnchantments(Map<String, Integer> resultEnchantments) { this.resultEnchantments = resultEnchantments; }
    public int getResultAmount() { return resultAmount; }
    public void setResultAmount(int resultAmount) { this.resultAmount = resultAmount; }
    public int getResultCustomModelData() { return resultCustomModelData; }
    public void setResultCustomModelData(int resultCustomModelData) { this.resultCustomModelData = resultCustomModelData; }
    public boolean isResultUnbreakable() { return resultUnbreakable; }
    public void setResultUnbreakable(boolean resultUnbreakable) { this.resultUnbreakable = resultUnbreakable; }
    public Set<ItemFlag> getResultFlags() { return resultFlags; }
    public void setResultFlags(Set<ItemFlag> resultFlags) { this.resultFlags = resultFlags; }
    public Map<String, String> getCustomNBTData() { return customNBTData; }
    public void setCustomNBTData(Map<String, String> customNBTData) { this.customNBTData = customNBTData; }

    public Map<String, Integer> getMaterialRequirements() { return materialRequirements; }
    public void setMaterialRequirements(Map<String, Integer> materialRequirements) { this.materialRequirements = materialRequirements; }
    public List<String> getPermissionRequirements() { return permissionRequirements; }
    public void setPermissionRequirements(List<String> permissionRequirements) { this.permissionRequirements = permissionRequirements; }
}
