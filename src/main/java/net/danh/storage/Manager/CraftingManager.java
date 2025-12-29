package net.danh.storage.Manager;

import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XMaterial;
import de.tr7zw.changeme.nbtapi.NBTContainer;
import de.tr7zw.changeme.nbtapi.NBTItem;
import net.danh.storage.API.events.RecipeCraftEvent;
import net.danh.storage.API.events.RecipeCreateEvent;
import net.danh.storage.Listeners.ChatListener;
import net.danh.storage.Recipe.Recipe;
import net.danh.storage.Storage;
import net.danh.storage.Utils.ChatUtils;
import net.danh.storage.Utils.File;
import net.danh.storage.Utils.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.io.BukkitObjectInputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class CraftingManager {

    private static final Map<String, Recipe> recipes = new HashMap<>();
    private static final Map<String, List<Recipe>> recipesByCategory = new HashMap<>();
    private static final Map<String, CraftingTask> activeCrafting = new HashMap<>();

    private static final String RECIPE_FOLDER_NAME = "recipes";

    public static void loadRecipes() {
        recipes.clear();
        recipesByCategory.clear();

        FileConfiguration config = File.getCraftingConfig();
        if (config == null) return;

        migrateLegacyRecipesToFolder(config);

        loadRecipesFromFolder();

        ConfigurationSection recipesSection = config.getConfigurationSection("recipes");
        if (recipesSection == null) return;

        for (String recipeId : recipesSection.getKeys(false)) {
            try {
                ConfigurationSection recipeSection = recipesSection.getConfigurationSection(recipeId);
                if (recipeSection != null) {
                    if (!recipes.containsKey(recipeId)) {
                        Recipe recipe = new Recipe(recipeId, recipeSection);
                        addRecipeToMaps(recipe);
                    }
                }
            } catch (Exception e) {
                Storage.getStorage().getLogger().warning("Error loading recipe '" + recipeId + "': " + e.getMessage());
            }
        }
    }

    public static void saveRecipes() {
        for (Recipe recipe : recipes.values()) {
            saveRecipeToFile(recipe);
        }
    }

    private static void migrateLegacyRecipesToFolder(FileConfiguration config) {
        if (config == null || Storage.getStorage() == null) {
            return;
        }

        ConfigurationSection recipesSection = config.getConfigurationSection("recipes");
        if (recipesSection == null) {
            return;
        }

        java.io.File folder = new java.io.File(
                Storage.getStorage().getDataFolder(),
                RECIPE_FOLDER_NAME
        );
        if (!folder.exists() && !folder.mkdirs()) {
            return;
        }

        boolean shouldRemoveLegacySection = false;
        for (String recipeId : recipesSection.getKeys(false)) {
            try {
                if (recipeId == null || recipeId.trim().isEmpty()) {
                    continue;
                }

                java.io.File outFile = new java.io.File(folder, recipeId + ".yml");
                if (outFile.exists()) {
                    shouldRemoveLegacySection = true;
                    continue;
                }

                ConfigurationSection recipeSection =
                        recipesSection.getConfigurationSection(recipeId);
                if (recipeSection == null) {
                    continue;
                }

                org.bukkit.configuration.file.YamlConfiguration yaml =
                        new org.bukkit.configuration.file.YamlConfiguration();
                ConfigurationSection outSection = yaml.createSection("recipe");

                for (String key : recipeSection.getKeys(true)) {
                    outSection.set(key, recipeSection.get(key));
                }

                yaml.save(outFile);
                shouldRemoveLegacySection = true;
            } catch (Exception e) {
                Storage.getStorage().getLogger().warning(
                        "Failed to migrate recipe '" + recipeId + "': "
                                + e.getMessage()
                );
            }
        }

        if (shouldRemoveLegacySection) {
            config.set("recipes", null);
            File.saveCraftingConfig();
        }
    }

    private static void loadRecipesFromFolder() {
        if (Storage.getStorage() == null) {
            return;
        }

        java.io.File folder = new java.io.File(
                Storage.getStorage().getDataFolder(),
                RECIPE_FOLDER_NAME
        );
        if (!folder.exists() && !folder.mkdirs()) {
            return;
        }

        Path dir = folder.toPath();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.yml")) {
            for (Path path : stream) {
                try {
                    String fileName = path.getFileName().toString();
                    if (fileName.length() <= 4) {
                        continue;
                    }

                    String recipeId = fileName.substring(0, fileName.length() - 4);
                    if (recipeId.trim().isEmpty()) {
                        continue;
                    }

                    org.bukkit.configuration.file.YamlConfiguration yaml =
                            org.bukkit.configuration.file.YamlConfiguration
                                    .loadConfiguration(path.toFile());

                    ConfigurationSection section = yaml.getConfigurationSection("recipe");
                    if (section == null) {
                        section = yaml;
                    }

                    Recipe recipe = new Recipe(recipeId, section);
                    addRecipeToMaps(recipe);
                } catch (Exception e) {
                    Storage.getStorage().getLogger().warning(
                            "Error loading recipe file '" + path.getFileName()
                                    + "': " + e.getMessage()
                    );
                }
            }
        } catch (IOException e) {
            Storage.getStorage().getLogger().warning(
                    "Error reading recipe folder: " + e.getMessage()
            );
        }
    }

    private static void saveRecipeToFile(Recipe recipe) {
        if (recipe == null || Storage.getStorage() == null) {
            return;
        }

        java.io.File folder = new java.io.File(
                Storage.getStorage().getDataFolder(),
                RECIPE_FOLDER_NAME
        );
        if (!folder.exists() && !folder.mkdirs()) {
            return;
        }

        java.io.File outFile = new java.io.File(folder, recipe.getId() + ".yml");
        org.bukkit.configuration.file.YamlConfiguration yaml =
                new org.bukkit.configuration.file.YamlConfiguration();

        ConfigurationSection section = yaml.createSection("recipe");
        recipe.saveToConfig(section);

        try {
            yaml.save(outFile);
        } catch (IOException e) {
            Storage.getStorage().getLogger().warning(
                    "Failed to save recipe file '" + outFile.getName() + "': "
                            + e.getMessage()
            );
        }
    }

    private static void deleteRecipeFile(String recipeId) {
        if (recipeId == null || recipeId.trim().isEmpty()
                || Storage.getStorage() == null) {
            return;
        }

        java.io.File folder = new java.io.File(
                Storage.getStorage().getDataFolder(),
                RECIPE_FOLDER_NAME
        );
        if (!folder.exists()) {
            return;
        }

        Path path = Paths.get(folder.getPath(), recipeId + ".yml");
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            Storage.getStorage().getLogger().warning(
                    "Failed to delete recipe file '" + recipeId + ".yml': "
                            + e.getMessage()
            );
        }
    }

    public static Recipe getRecipe(String id) {
        return recipes.get(id);
    }

    public static Collection<Recipe> getAllRecipes() {
        return recipes.values();
    }

    public static List<Recipe> getRecipesByCategory(String category) {
        return recipesByCategory.getOrDefault(category, new ArrayList<>());
    }

    public static Set<String> getCategories() {
        Set<String> allCategories = new LinkedHashSet<>();
        for (String cat : RecipeEditManager.getCategoryNames()) {
            if (!cat.equals("all")) {
                allCategories.add(cat);
            }
        }
        allCategories.addAll(recipesByCategory.keySet());
        return allCategories;
    }

    public static RecipeEditManager.CategoryInfo getCategoryInfo(String category) {
        return RecipeEditManager.getCategoryInfo(category);
    }

    public static String getCategoryDisplayName(String categoryKey) {
        if (categoryKey == null || categoryKey.isEmpty()) return "Unknown";
        if (categoryKey.equalsIgnoreCase("all")) {
            String configName = File.getCraftingConfig().getString("categories.all.name");
            return configName != null ? ChatUtils.colorizewp(configName) : "All";
        }

        String configPath = "categories." + categoryKey.toLowerCase() + ".name";
        String configName = File.getCraftingConfig().getString(configPath);
        if (configName != null) {
            return ChatUtils.colorizewp(configName);
        }
        // Fallback to RecipeEditManager default
        return ChatUtils.colorizewp(RecipeEditManager.getCategoryInfo(categoryKey).displayName);
    }

    public static boolean isValidCategory(String categoryKey) {
        if (categoryKey == null || categoryKey.isEmpty()) return false;
        if (categoryKey.equalsIgnoreCase("all")) return false;

        String configPath = "categories." + categoryKey.toLowerCase();
        if (File.getCraftingConfig().contains(configPath)) {
            return true;
        }
        return RecipeEditManager.getDefaultCategories().containsKey(categoryKey.toLowerCase());
    }

    public static Set<String> getValidCategoryKeys() {
        Set<String> validCategories = new LinkedHashSet<>();
        if (File.getCraftingConfig().contains("categories")) {
            for (String key : File.getCraftingConfig().getConfigurationSection("categories").getKeys(false)) {
                if (!key.equalsIgnoreCase("all")) {
                    validCategories.add(key.toLowerCase());
                }
            }
        }
        for (String key : RecipeEditManager.getCategoryNames()) {
            if (!key.equalsIgnoreCase("all")) {
                validCategories.add(key.toLowerCase());
            }
        }
        return validCategories;
    }

    public static List<Recipe> getAvailableRecipes(Player player) {
        return recipes.values().stream()
                .filter(Recipe::isEnabled)
                .filter(recipe -> hasPermissions(player, recipe))
                .collect(Collectors.toList());
    }

    public static boolean canCraft(Player player, String recipeId) {
        Recipe recipe = recipes.get(recipeId);
        return recipe != null && recipe.isEnabled() &&
                hasPermissions(player, recipe) && hasMaterials(player, recipe);
    }

    public static boolean craftRecipe(Player player, String recipeId) {
        return craftRecipe(player, recipeId, 1);
    }

    public static boolean craftRecipe(Player player, String recipeId, int amount) {
        Recipe recipe = recipes.get(recipeId);
        if (recipe == null || !recipe.isEnabled()) return false;

        if (!hasPermissions(player, recipe)) {
            sendMessage(player, "crafting.no_permission", "#recipe#", recipe.getName());
            SoundManager.playSound(player, SoundManager.SoundType.ACTION_ERROR);
            return false;
        }

        if (isCraftingInProgress(player)) {
            sendMessage(player, "crafting.craft_in_progress");
            SoundManager.playSound(player, SoundManager.SoundType.ACTION_ERROR);
            return false;
        }

        int maxCraftable = getMaxCraftableAmount(player, recipe);
        if (maxCraftable <= 0) {
            sendMessage(player, "crafting.insufficient_materials", "#recipe#", recipe.getName());
            SoundManager.playSound(player, SoundManager.SoundType.ACTION_ERROR);
            return false;
        }

        int actualAmount = Math.min(amount, maxCraftable);

        ItemStack resultItem = createResultItem(recipe);
        if (resultItem == null) {
            sendMessage(player, "crafting.craft_failed", "#recipe#", recipe.getName());
            SoundManager.playSound(player, SoundManager.SoundType.ACTION_ERROR);
            return false;
        }

        RecipeCraftEvent preEvent = new RecipeCraftEvent(player, recipe, actualAmount, resultItem, RecipeCraftEvent.CraftPhase.PRE_CRAFT);
        Bukkit.getPluginManager().callEvent(preEvent);
        if (preEvent.isCancelled()) {
            return false;
        }
        actualAmount = preEvent.getAmount();

        int totalItemsNeeded = recipe.getResultAmount() * actualAmount;
        int availableSpace = calculateInventorySpace(player, resultItem);

        if (availableSpace < totalItemsNeeded) {
            sendMessage(player, "crafting.inventory_full", "#recipe#", recipe.getName());
            SoundManager.playSound(player, SoundManager.SoundType.ACTION_ERROR);
            return false;
        }

        FileConfiguration config = File.getCraftingConfig();
        if (config.getBoolean("settings.delay.enabled", true)) {
            int craftingDelay = config.getInt("settings.delay.seconds", 3);
            if (craftingDelay > 0) {
                startCraftingProcess(player, recipe, actualAmount);
                return true;
            }
        }

        return completeCrafting(player, recipe, actualAmount);
    }

    public static ItemStack createResultItem(Recipe recipe) {
        if (recipe == null) return null;

        ItemStack base64Item = createResultItemFromBase64(recipe);
        if (base64Item != null) {
            return base64Item;
        }

        ItemStack nbtItem = createResultItemFromNbt(recipe);
        if (nbtItem != null) {
            return nbtItem;
        }

        String materialKey = recipe.getResultMaterial();
        String cleanedMaterialKey = materialKey;
        if (cleanedMaterialKey != null) {
            cleanedMaterialKey = cleanedMaterialKey.trim();
            if (cleanedMaterialKey.contains(";")) {
                cleanedMaterialKey = cleanedMaterialKey.split(";", 2)[0];
            }
            if (cleanedMaterialKey.contains(":")) {
                cleanedMaterialKey = cleanedMaterialKey.split(":", 2)[0];
            }
        }

        Optional<XMaterial> xMaterial = XMaterial.matchXMaterial(cleanedMaterialKey);
        if (!xMaterial.isPresent()) return null;

        if (materialKey != null && cleanedMaterialKey != null
                && !materialKey.equals(cleanedMaterialKey)
                && xMaterial.get().name().equalsIgnoreCase(cleanedMaterialKey)) {
            recipe.setResultMaterial(xMaterial.get().name());
        }

        ItemStack item = xMaterial.get().parseItem();
        if (item == null) return null;

        item.setAmount(recipe.getResultAmount());

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatUtils.colorizewp(recipe.getResultName()));

            if (!recipe.getResultLore().isEmpty()) {
                List<String> coloredLore = recipe.getResultLore().stream()
                        .map(ChatUtils::colorizewp)
                        .collect(Collectors.toList());
                meta.setLore(coloredLore);
            }

            for (Map.Entry<String, Integer> entry : recipe.getResultEnchantments().entrySet()) {
                Optional<XEnchantment> xEnchant = XEnchantment.matchXEnchantment(entry.getKey());
                if (xEnchant.isPresent()) {
                    Enchantment enchant = xEnchant.get().getEnchant();
                    if (enchant != null) {
                        meta.addEnchant(enchant, entry.getValue(), true);
                    }
                }
            }

            for (ItemFlag flag : recipe.getResultFlags()) {
                meta.addItemFlags(flag);
            }

            try {
                if (recipe.getResultCustomModelData() > 0) {
                    meta.setCustomModelData(recipe.getResultCustomModelData());
                }
            } catch (NoSuchMethodError ignored) {
            }

            try {
                meta.setUnbreakable(recipe.isResultUnbreakable());
            } catch (NoSuchMethodError ignored) {
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    private static ItemStack createResultItemFromBase64(Recipe recipe) {
        String encoded = recipe.getResultItemBase64();
        if (encoded == null || encoded.trim().isEmpty()) {
            return null;
        }

        try {
            byte[] data = Base64.getDecoder().decode(encoded);
            try (BukkitObjectInputStream in = new BukkitObjectInputStream(new ByteArrayInputStream(data))) {
                Object obj = in.readObject();
                if (!(obj instanceof ItemStack)) {
                    return null;
                }
                ItemStack item = ((ItemStack) obj).clone();
                if (item.getType() == Material.AIR) {
                    return null;
                }

                int amount = recipe.getResultAmount();
                if (amount < 1) {
                    amount = 1;
                } else if (amount > 64) {
                    amount = 64;
                }
                item.setAmount(amount);
                return item;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static ItemStack createResultItemFromNbt(Recipe recipe) {
        String itemNbt = recipe.getResultItemNbt();
        if (itemNbt == null || itemNbt.trim().isEmpty()) {
            return null;
        }

        try {
            NBTContainer container = new NBTContainer(itemNbt);
            ItemStack item = NBTItem.convertNBTtoItem(container);
            if (item == null || item.getType() == Material.AIR) {
                return null;
            }

            try {
                item = new NBTItem(item).getItem();
            } catch (Exception ignored) {
            }

            int amount = recipe.getResultAmount();
            if (amount < 1) {
                amount = 1;
            } else if (amount > 64) {
                amount = 64;
            }
            item.setAmount(amount);
            return item;
        } catch (Exception e) {
            return null;
        }
    }

    public static void addRecipe(Recipe recipe) {
        if (!validateRecipe(recipe)) {
            Storage.getStorage().getLogger().warning("Cannot add invalid recipe: " + recipe.getId());
            return;
        }

        RecipeCreateEvent event = new RecipeCreateEvent(null, recipe, RecipeCreateEvent.Action.CREATE);
        SchedulerUtil.runTask(Storage.getStorage(), () -> {
            Bukkit.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                addRecipeToMaps(recipe);
                saveRecipes();
            }
        });
    }

    public static boolean removeRecipe(String id) {
        Recipe recipe = recipes.get(id);
        if (recipe == null) {
            return false;
        }

        recipes.remove(id);
        removeRecipeFromCategory(recipe);

        RecipeCreateEvent event = new RecipeCreateEvent(null, recipe, RecipeCreateEvent.Action.DELETE);
        SchedulerUtil.runTask(Storage.getStorage(), () -> {
            Bukkit.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                saveRecipes();
                deleteRecipeFile(id);
            } else {
                addRecipeToMaps(recipe);
            }
        });
        return true;
    }

    public static void updateRecipe(Recipe recipe) {
        if (!validateRecipe(recipe)) {
            Storage.getStorage().getLogger().warning("Cannot update invalid recipe: " + recipe.getId());
            return;
        }

        RecipeCreateEvent event = new RecipeCreateEvent(null, recipe, RecipeCreateEvent.Action.UPDATE);
        SchedulerUtil.runTask(Storage.getStorage(), () -> {
            Bukkit.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                removeRecipeFromAllCategories(recipe.getId());
                addRecipeToMaps(recipe);
                saveRecipes();
            }
        });
    }

    public static boolean hasPermissions(Player player, Recipe recipe) {
        String permission = recipe.getPermissionRequirement();
        return permission == null || permission.trim().isEmpty() || player.hasPermission(permission);
    }

    public static boolean hasMaterials(Player player, Recipe recipe) {
        return getMaxCraftableAmount(player, recipe) > 0;
    }

    public static int getMaxCraftableAmount(Player player, Recipe recipe) {
        return recipe.getMaterialRequirements().entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .mapToInt(entry -> {
                    int required = entry.getValue();
                    int available = getAvailableMaterialAmount(player, entry.getKey());
                    return available / required;
                })
                .min()
                .orElse(0);
    }

    private static int getAvailableMaterialAmount(Player player, String requirementKey) {
        String normalized = MineManager.normalizeMaterial(requirementKey);
        String mythicId = getMythicItemId(normalized);
        if (mythicId != null) {
            if (!MythicStorageManager.isSystemEnabled()) {
                return 0;
            }
            return MythicStorageManager.getPlayerItem(player, mythicId);
        }
        return MineManager.getPlayerBlock(player, normalized);
    }

    private static String getMythicItemId(String normalizedKey) {
        if (normalizedKey == null || normalizedKey.isEmpty()) {
            return null;
        }
        if (!normalizedKey.startsWith("mythic;")) {
            return null;
        }
        String[] parts = normalizedKey.split(";", 3);
        if (parts.length < 2) {
            return null;
        }
        String id = parts[1];
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        return id.trim();
    }

    public static void requestCraftAmount(Player player, String recipeId) {
        Recipe recipe = recipes.get(recipeId);
        if (recipe == null || !recipe.isEnabled()) {
            sendMessage(player, "crafting.recipe_not_found", "#recipe#", recipeId);
            SoundManager.playSound(player, SoundManager.SoundType.ACTION_ERROR);
            return;
        }

        if (!hasPermissions(player, recipe)) {
            sendMessage(player, "crafting.no_permission", "#recipe#", recipe.getName());
            SoundManager.playSound(player, SoundManager.SoundType.ACTION_ERROR);
            return;
        }

        int maxCraftable = getMaxCraftableAmount(player, recipe);
        if (maxCraftable <= 0) {
            sendMessage(player, "crafting.insufficient_materials", "#recipe#", recipe.getName());
            return;
        }

        ChatListener.craftingRequests.put(player.getUniqueId(), recipeId);
        player.closeInventory();
        sendMessage(player, "crafting.enter_craft_amount", "#max#", String.valueOf(maxCraftable));
    }

    public static void handleCraftAmountInput(Player player, String recipeId, String input) {
        int amount = net.danh.storage.Utils.Number.getInteger(input.trim());

        if (amount <= 0 || amount > 999) {
            sendMessage(player, "crafting.invalid_craft_amount");
            SoundManager.playSound(player, SoundManager.SoundType.ACTION_ERROR);
            return;
        }

        Recipe recipe = recipes.get(recipeId);
        if (recipe == null) {
            sendMessage(player, "crafting.recipe_not_found", "#recipe#", recipeId);
            SoundManager.playSound(player, SoundManager.SoundType.ACTION_ERROR);
            return;
        }

        SchedulerUtil.runTask(Storage.getStorage(), () -> craftRecipe(player, recipeId, amount));
    }

    public static String generateUniqueId() {
        String baseId = "recipe_" + System.currentTimeMillis();
        int counter = 1;
        String id = baseId;

        while (recipes.containsKey(id)) {
            id = baseId + "_" + counter;
            counter++;
        }

        return id;
    }

    public static boolean recipeExists(String recipeId) {
        return recipes.containsKey(recipeId);
    }

    public static int getTotalRecipes() {
        return recipes.size();
    }

    public static int getEnabledRecipesCount() {
        return (int) recipes.values().stream().filter(Recipe::isEnabled).count();
    }

    public static List<Recipe> getCraftableRecipes(Player player) {
        return recipes.values().stream()
                .filter(Recipe::isEnabled)
                .filter(recipe -> hasPermissions(player, recipe))
                .filter(recipe -> hasMaterials(player, recipe))
                .collect(Collectors.toList());
    }

    public static Recipe duplicateRecipe(String sourceId, String newId) {
        Recipe source = recipes.get(sourceId);
        if (source == null) return null;

        Recipe duplicate = new Recipe(newId);
        duplicate.setName(source.getName() + " (Copy)");
        duplicate.setCategory(source.getCategory());
        duplicate.setEnabled(source.isEnabled());
        duplicate.setResultMaterial(source.getResultMaterial());
        duplicate.setResultName(source.getResultName());
        duplicate.setResultLore(new ArrayList<>(source.getResultLore()));
        duplicate.setResultEnchantments(new HashMap<>(source.getResultEnchantments()));
        duplicate.setResultAmount(source.getResultAmount());
        duplicate.setResultCustomModelData(source.getResultCustomModelData());
        duplicate.setResultUnbreakable(source.isResultUnbreakable());
        duplicate.setResultFlags(new HashSet<>(source.getResultFlags()));
        duplicate.setMaterialRequirements(new HashMap<>(source.getMaterialRequirements()));
        duplicate.setPermissionRequirement(source.getPermissionRequirement());

        return duplicate;
    }

    public static Map<String, Integer> getMissingMaterials(Player player, Recipe recipe) {
        Map<String, Integer> missing = new HashMap<>();
        for (Map.Entry<String, Integer> requirement : recipe.getMaterialRequirements().entrySet()) {
            int needed = requirement.getValue();
            if (needed <= 0) {
                continue;
            }

            int available = getAvailableMaterialAmount(player, requirement.getKey());
            if (available < needed) {
                missing.put(requirement.getKey(), needed - available);
            }
        }
        return missing;
    }

    public static List<Recipe> getRecipesByStatus(boolean enabled) {
        return recipes.values().stream()
                .filter(recipe -> recipe.isEnabled() == enabled)
                .collect(Collectors.toList());
    }

    public static List<Recipe> searchRecipes(String query) {
        String lowerQuery = query.toLowerCase();
        return recipes.values().stream()
                .filter(recipe -> recipe.getId().toLowerCase().contains(lowerQuery) ||
                        recipe.getName().toLowerCase().contains(lowerQuery))
                .collect(Collectors.toList());
    }

    public static Map<String, Integer> getRecipeStats() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("total", recipes.size());
        stats.put("enabled", (int) recipes.values().stream().filter(Recipe::isEnabled).count());
        stats.put("disabled", (int) recipes.values().stream().filter(r -> !r.isEnabled()).count());
        stats.put("categories", recipesByCategory.size());
        return stats;
    }

    public static boolean validateRecipe(Recipe recipe) {
        if (recipe == null) return false;
        if (recipe.getId() == null || recipe.getId().isEmpty()) return false;
        if (recipe.getResultMaterial() == null || recipe.getResultMaterial().isEmpty()) return false;
        if (recipe.getResultAmount() <= 0) return false;
        return recipe.getMaterialRequirements() != null
                && !recipe.getMaterialRequirements().isEmpty();
    }

    public static Map<String, Integer> getTotalMaterialsNeeded(Recipe recipe, int amount) {
        Map<String, Integer> total = new HashMap<>();
        for (Map.Entry<String, Integer> requirement : recipe.getMaterialRequirements().entrySet()) {
            String normalizedMaterial = MineManager.normalizeMaterial(requirement.getKey());
            total.put(normalizedMaterial, requirement.getValue() * amount);
        }
        return total;
    }

    public static boolean isCraftingInProgress(Player player) {
        return activeCrafting.containsKey(player.getUniqueId().toString());
    }

    public static boolean cancelCrafting(Player player) {
        CraftingTask task = activeCrafting.remove(player.getUniqueId().toString());
        if (task != null) {
            task.cancel();
            sendMessage(player, "crafting.craft_cancelled");
            ParticleManager.stopCraftingProcessingAnimation(player);
            return true;
        }
        return false;
    }

    public static void cancelAllCrafting() {
        for (CraftingTask task : activeCrafting.values()) {
            task.cancel();
        }
        activeCrafting.clear();
    }

    private static void startCraftingProcess(Player player, Recipe recipe, int amount) {
        FileConfiguration config = File.getCraftingConfig();
        int craftingDelay = config.getInt("settings.delay.seconds", 3);
        int totalItems = recipe.getResultAmount() * amount;

        sendMessage(player, "crafting.processing",
                new String[]{"#amount#", "#recipe#", "#time#"},
                new String[]{String.valueOf(totalItems), recipe.getName(), String.valueOf(craftingDelay)});

        cancelCrafting(player);

        ParticleManager.playCraftingProcessingAnimation(player, craftingDelay);

        CraftingTask craftingTask = new CraftingTask(player, recipe, amount);
        activeCrafting.put(player.getUniqueId().toString(), craftingTask);

        SchedulerUtil.runTaskLater(Storage.getStorage(), () -> {
            ParticleManager.stopCraftingProcessingAnimation(player);

            if (completeCrafting(player, recipe, amount)) {
                ParticleManager.playCraftingSuccessParticle(player);
                playCraftingSuccessSound(player);
            } else {
                ParticleManager.playCraftingFailedParticle(player);
                playCraftingFailedSound(player);
            }

            activeCrafting.remove(player.getUniqueId().toString());
        }, craftingDelay * 20L);
    }

    private static boolean completeCrafting(Player player, Recipe recipe, int amount) {
        if (!player.isOnline()) {
            return false;
        }

        int maxCraftable = getMaxCraftableAmount(player, recipe);
        if (maxCraftable < amount) {
            sendMessage(player, "crafting.processing_failed", "#recipe#", recipe.getName());
            return false;
        }

        ItemStack resultItem = createResultItem(recipe);
        if (resultItem == null) {
            sendMessage(player, "crafting.craft_failed", "#recipe#", recipe.getName());
            return false;
        }

        int totalItemsNeeded = recipe.getResultAmount() * amount;
        int availableSpace = calculateInventorySpace(player, resultItem);

        if (availableSpace < totalItemsNeeded) {
            sendMessage(player, "crafting.inventory_full", "#recipe#", recipe.getName());
            return false;
        }

        RecipeCraftEvent postEvent = new RecipeCraftEvent(player, recipe, amount, resultItem, RecipeCraftEvent.CraftPhase.POST_CRAFT);
        Bukkit.getPluginManager().callEvent(postEvent);

        if (!removeMaterials(player, recipe, amount)) {
            return false;
        }

        giveResultItems(player, resultItem, recipe.getResultAmount() * amount);

        RecipeCraftEvent completeEvent = new RecipeCraftEvent(player, recipe, amount, resultItem, RecipeCraftEvent.CraftPhase.COMPLETE);
        Bukkit.getPluginManager().callEvent(completeEvent);

        int totalItems = recipe.getResultAmount() * amount;
        sendMessage(player, "crafting.craft_success",
                new String[]{"#recipe#", "#amount#"},
                new String[]{recipe.getName(), String.valueOf(totalItems)});

        return true;
    }

    private static void playCraftingSuccessSound(Player player) {
        FileConfiguration config = File.getCraftingConfig();
        if (config.getBoolean("settings.sounds.enabled", true)) {
            String soundName = config.getString("settings.sounds.success.name");
            if (soundName != null && !soundName.equalsIgnoreCase("none")) {
                float volume = (float) config.getDouble("settings.sounds.success.volume", 0.7);
                float pitch = (float) config.getDouble("settings.sounds.success.pitch", 1.2);
                SoundManager.playSound(player, soundName, volume, pitch);
            }
        }
    }

    private static void playCraftingFailedSound(Player player) {
        FileConfiguration config = File.getCraftingConfig();
        if (config.getBoolean("settings.sounds.enabled", true)) {
            String soundName = config.getString("settings.sounds.failed.name");
            if (soundName != null && !soundName.equalsIgnoreCase("none")) {
                float volume = (float) config.getDouble("settings.sounds.failed.volume", 0.8);
                float pitch = (float) config.getDouble("settings.sounds.failed.pitch", 0.8);
                SoundManager.playSound(player, soundName, volume, pitch);
            }
        }
    }

    private static void addRecipeToMaps(Recipe recipe) {
        recipes.put(recipe.getId(), recipe);
        String category = recipe.getCategory();
        recipesByCategory.computeIfAbsent(category, k -> new ArrayList<>()).add(recipe);
    }

    public static void stageRecipe(Recipe recipe) {
        if (recipe == null || recipe.getId() == null || recipe.getId().isEmpty()) {
            return;
        }
        addRecipeToMaps(recipe);
    }

    private static void removeRecipeFromCategory(Recipe recipe) {
        List<Recipe> categoryRecipes = recipesByCategory.get(recipe.getCategory());
        if (categoryRecipes != null) {
            categoryRecipes.remove(recipe);
            if (categoryRecipes.isEmpty()) {
                recipesByCategory.remove(recipe.getCategory());
            }
        }
    }

    private static void removeRecipeFromAllCategories(String recipeId) {
        for (List<Recipe> categoryRecipes : recipesByCategory.values()) {
            categoryRecipes.removeIf(r -> r.getId().equals(recipeId));
        }
    }

    private static boolean removeMaterials(Player player, Recipe recipe, int amount) {
        for (Map.Entry<String, Integer> requirement : recipe.getMaterialRequirements().entrySet()) {
            String normalizedMaterial = MineManager.normalizeMaterial(requirement.getKey());
            int totalRequired = requirement.getValue() * amount;

            String mythicId = getMythicItemId(normalizedMaterial);
            if (mythicId != null) {
                if (!MythicStorageManager.isSystemEnabled()) {
                    return false;
                }
                if (!MythicStorageManager.removeItemAmount(player, mythicId,
                        totalRequired, true)) {
                    return false;
                }
            } else {
                if (!MineManager.removeBlockAmount(player, normalizedMaterial,
                        totalRequired)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void giveResultItems(Player player, ItemStack item, int amount) {
        int remaining = amount;
        int maxStackSize = item.getMaxStackSize();

        while (remaining > 0) {
            int stackAmount = Math.min(remaining, maxStackSize);
            ItemStack stack = item.clone();
            stack.setAmount(stackAmount);
            player.getInventory().addItem(stack);
            remaining -= stackAmount;
        }
    }

    public static int calculateInventorySpace(Player player, ItemStack itemStack) {
        int availableSpace = 0;
        ItemStack template = itemStack.clone();
        template.setAmount(1);

        for (ItemStack slot : player.getInventory().getStorageContents()) {
            if (slot == null || slot.getType() == Material.AIR) {
                availableSpace += template.getMaxStackSize();
            } else if (slot.isSimilar(template)) {
                int spaceLeft = slot.getMaxStackSize() - slot.getAmount();
                if (spaceLeft > 0) {
                    availableSpace += spaceLeft;
                }
            }
        }

        return availableSpace;
    }

    private static void sendMessage(Player player, String messageKey) {
        String message = File.getMessage().getString(messageKey);
        if (message != null) {
            player.sendMessage(ChatUtils.colorize(message));
        }
    }

    private static void sendMessage(Player player, String messageKey, String placeholder, String replacement) {
        String message = File.getMessage().getString(messageKey);
        if (message != null) {
            player.sendMessage(ChatUtils.colorize(message.replace(placeholder, replacement)));
        }
    }

    private static void sendMessage(Player player, String messageKey, String[] placeholders, String[] replacements) {
        String message = File.getMessage().getString(messageKey);
        if (message != null) {
            for (int i = 0; i < placeholders.length && i < replacements.length; i++) {
                message = message.replace(placeholders[i], replacements[i]);
            }
            player.sendMessage(ChatUtils.colorize(message));
        }
    }

    private static class CraftingTask {
        private final Player player;
        private final Recipe recipe;
        private final int amount;
        private boolean cancelled = false;

        public CraftingTask(Player player, Recipe recipe, int amount) {
            this.player = player;
            this.recipe = recipe;
            this.amount = amount;
        }

        public void cancel() {
            this.cancelled = true;
        }

        public boolean isCancelled() {
            return cancelled;
        }
    }
}
