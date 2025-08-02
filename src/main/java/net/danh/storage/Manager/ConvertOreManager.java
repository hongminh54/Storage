package net.danh.storage.Manager;

import net.danh.storage.Utils.File;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public class ConvertOreManager {

    private static final Map<String, ConvertOption> convertOptions = new HashMap<>();
    private static final Set<String> convertibleMaterials = new HashSet<>();

    public static void loadConvertOptions() {
        convertOptions.clear();
        convertibleMaterials.clear();

        ConfigurationSection itemsSection = File.getConfig().getConfigurationSection("items");
        if (itemsSection == null) return;

        Map<String, String> ingotToBlock = new HashMap<>();

        ConfigurationSection customMappings = File.getConfig().getConfigurationSection("convert.custom_mappings");
        if (customMappings != null) {
            for (String ingotMaterial : customMappings.getKeys(false)) {
                String blockMaterial = customMappings.getString(ingotMaterial);
                if (blockMaterial != null && itemsSection.contains(ingotMaterial) && itemsSection.contains(blockMaterial)) {
                    ingotToBlock.put(ingotMaterial, blockMaterial);
                }
            }
        }

        for (String itemKey : itemsSection.getKeys(false)) {
            String materialName = itemKey.split(";")[0];

            if (!ingotToBlock.containsKey(itemKey) && isIngotMaterial(materialName)) {
                String blockMaterial = getCorrespondingBlock(itemKey, itemsSection);
                if (blockMaterial != null) {
                    ingotToBlock.put(itemKey, blockMaterial);
                }
            }
        }

        for (Map.Entry<String, String> entry : ingotToBlock.entrySet()) {
            String ingotMaterial = entry.getKey();
            String blockMaterial = entry.getValue();

            convertibleMaterials.add(ingotMaterial);
            convertibleMaterials.add(blockMaterial);

            ConversionRatio ratio = getConversionRatio(ingotMaterial, blockMaterial);

            convertOptions.put(ingotMaterial + "_to_block",
                    new ConvertOption(ingotMaterial, blockMaterial, ratio.ingotToBlock, 1, "ingot_to_block"));
            convertOptions.put(blockMaterial + "_to_ingot",
                    new ConvertOption(blockMaterial, ingotMaterial, 1, ratio.blockToIngot, "block_to_ingot"));
        }


    }


    public static List<String> getConvertibleMaterials() {
        List<String> materials = new ArrayList<>(convertibleMaterials);
        Collections.sort(materials);
        return materials;
    }

    public static boolean hasConvertOptions(String material) {
        for (String key : convertOptions.keySet()) {
            if (key.startsWith(material + "_")) {
                return true;
            }
        }
        return false;
    }

    public static List<ConvertOption> getConvertOptions(String material) {
        List<ConvertOption> options = new ArrayList<>();
        for (Map.Entry<String, ConvertOption> entry : convertOptions.entrySet()) {
            if (entry.getKey().startsWith(material + "_")) {
                options.add(entry.getValue());
            }
        }
        return options;
    }

    public static boolean isConvertibleMaterial(String material) {
        return convertibleMaterials.contains(material);
    }

    public static boolean canConvert(String fromMaterial, String toMaterial, int amount) {
        String key = fromMaterial + "_to_" + (toMaterial.contains("BLOCK") ? "block" : "ingot");
        ConvertOption option = convertOptions.get(key);
        return option != null && amount >= option.getFromAmount();
    }

    public static ConvertOption getConvertOption(String fromMaterial, String toMaterial) {
        String key = fromMaterial + "_to_" + (toMaterial.contains("BLOCK") ? "block" : "ingot");
        return convertOptions.get(key);
    }

    public static String getConversionInfo(String material) {
        List<ConvertOption> options = getConvertOptions(material);
        if (options.isEmpty()) {
            return "No conversion options available for " + material;
        }

        StringBuilder info = new StringBuilder();
        info.append("Conversion options for ").append(material).append(":\n");
        for (ConvertOption option : options) {
            info.append("- ").append(option.getDisplayName()).append("\n");
        }
        return info.toString();
    }

    public static void reloadConvertConfig() {
        loadConvertOptions();
    }

    public static int getLoadedConversionsCount() {
        return convertOptions.size() / 2;
    }

    private static boolean isIngotMaterial(String materialName) {
        return materialName.contains("INGOT") ||
                materialName.equals("COAL") ||
                materialName.equals("REDSTONE") ||
                materialName.equals("LAPIS_LAZULI") ||
                materialName.equals("DIAMOND") ||
                materialName.equals("EMERALD") ||
                materialName.equals("QUARTZ") ||
                materialName.equals("NETHERITE_SCRAP");
    }

    private static String getCorrespondingBlock(String ingotMaterial, ConfigurationSection itemsSection) {
        String materialName = ingotMaterial.split(";")[0];
        String dataValue = ingotMaterial.split(";")[1];

        String blockMaterial = null;

        if (materialName.equals("COAL")) {
            blockMaterial = "COAL_BLOCK;" + dataValue;
        } else if (materialName.contains("INGOT")) {
            String baseName = materialName.replace("_INGOT", "");
            blockMaterial = baseName + "_BLOCK;" + dataValue;
        } else if (materialName.equals("REDSTONE")) {
            blockMaterial = "REDSTONE_BLOCK;" + dataValue;
        } else if (materialName.equals("LAPIS_LAZULI")) {
            blockMaterial = "LAPIS_BLOCK;" + dataValue;
        } else if (materialName.equals("DIAMOND")) {
            blockMaterial = "DIAMOND_BLOCK;" + dataValue;
        } else if (materialName.equals("EMERALD")) {
            blockMaterial = "EMERALD_BLOCK;" + dataValue;
        } else if (materialName.equals("QUARTZ")) {
            blockMaterial = "QUARTZ_BLOCK;" + dataValue;
        } else if (materialName.equals("NETHERITE_SCRAP")) {
            blockMaterial = "NETHERITE_BLOCK;" + dataValue;
        }

        if (blockMaterial != null && itemsSection.contains(blockMaterial)) {
            return blockMaterial;
        }

        return null;
    }

    private static ConversionRatio getConversionRatio(String ingotMaterial, String blockMaterial) {
        String pairKey = ingotMaterial + "_" + blockMaterial;

        ConfigurationSection customRatios = File.getConfig().getConfigurationSection("convert.custom_ratios");
        if (customRatios != null && customRatios.contains(pairKey)) {
            int ingotToBlock = customRatios.getInt(pairKey + ".ingot_to_block", 9);
            int blockToIngot = customRatios.getInt(pairKey + ".block_to_ingot", 9);
            return new ConversionRatio(ingotToBlock, blockToIngot);
        }

        int defaultIngotToBlock = File.getConfig().getInt("convert.default_ratios.ingot_to_block", 9);
        int defaultBlockToIngot = File.getConfig().getInt("convert.default_ratios.block_to_ingot", 9);
        return new ConversionRatio(defaultIngotToBlock, defaultBlockToIngot);
    }

    private static class ConversionRatio {
        final int ingotToBlock;
        final int blockToIngot;

        ConversionRatio(int ingotToBlock, int blockToIngot) {
            this.ingotToBlock = ingotToBlock;
            this.blockToIngot = blockToIngot;
        }
    }

    public static class ConvertOption {
        private final String fromMaterial;
        private final String toMaterial;
        private final int fromAmount;
        private final int toAmount;
        private final String convertType;

        public ConvertOption(String fromMaterial, String toMaterial, int fromAmount, int toAmount, String convertType) {
            this.fromMaterial = fromMaterial;
            this.toMaterial = toMaterial;
            this.fromAmount = fromAmount;
            this.toAmount = toAmount;
            this.convertType = convertType;
        }

        public String getFromMaterial() {
            return fromMaterial;
        }

        public String getToMaterial() {
            return toMaterial;
        }

        public int getFromAmount() {
            return fromAmount;
        }

        public int getToAmount() {
            return toAmount;
        }

        public String getConvertType() {
            return convertType;
        }

        public String getDisplayName() {
            String fromName = File.getConfig().getString("items." + fromMaterial, fromMaterial.split(";")[0]);
            String toName = File.getConfig().getString("items." + toMaterial, toMaterial.split(";")[0]);
            return fromAmount + " " + fromName + " → " + toAmount + " " + toName + " (ratio " + fromAmount + ":" + toAmount + ")";
        }

        public int calculateMaxConversions(int availableAmount) {
            return availableAmount / fromAmount;
        }

        public int calculateResultAmount(int conversions) {
            return conversions * toAmount;
        }
    }
}
