package net.danh.storage.CMD.handler.admin;

import net.danh.storage.CMD.handler.BaseCommand;
import net.danh.storage.GUI.RecipeEditorGUI;
import net.danh.storage.GUI.RecipeEditorListGUI;
import net.danh.storage.Manager.CraftingManager;
import net.danh.storage.Manager.SoundManager;
import net.danh.storage.Recipe.Recipe;
import net.danh.storage.Utils.ItemImportUtil;
import net.danh.storage.Utils.SoundContext;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Optimized command handler for recipe editor with import functionality
 */
public class CraftEditorCommand extends BaseCommand {

    // Cache for custom plugin detection
    private static final String[] PLUGIN_TAGS = {
        "MMOITEMS_TYPE", "MMOITEMS_ID", "MyItems", "itemsadder", 
        "oraxen", "ExecutableItems", "MythicMobs", "EliteMobs"
    };
    
    private static final String[] PLUGIN_NAMES = {
        "MMOItems", "MMOItems", "MyItems", "ItemsAdder", 
        "Oraxen", "ExecutableItems", "MythicMobs", "EliteMobs"
    };

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, "admin.player_only");
            return;
        }

        Player player = (Player) sender;

        if (args.length > 0 && args[0].equalsIgnoreCase("import")) {
            handleImportCommand(player);
            return;
        }

        // Open recipe editor list GUI
        SoundManager.setShouldPlayCloseSound(player, false);
        player.openInventory(new RecipeEditorListGUI(player).getInventory(SoundContext.INITIAL_OPEN));
    }

    private void handleImportCommand(Player player) {
        ItemStack heldItem = player.getInventory().getItemInMainHand();

        if (heldItem == null || heldItem.getType().name().equals("AIR")) {
            sendMessage(player, "recipe.import_empty_hand");
            return;
        }

        try {
            // Create and import recipe
            String recipeId = CraftingManager.generateUniqueId();
            Recipe recipe = new Recipe(recipeId);

            ItemImportUtil.importItemToRecipe(heldItem, recipe);
            CraftingManager.addRecipe(recipe);

            // Detect and notify about custom plugins
            String customPlugin = detectCustomItemPlugin(heldItem);
            if (customPlugin != null) {
                sendMessage(player, "recipe.import_custom_item_detected", "#plugin#", customPlugin);
            }

            // Open recipe editor
            SoundManager.setShouldPlayCloseSound(player, false);
            player.openInventory(new RecipeEditorGUI(player, recipe).getInventory(SoundContext.INITIAL_OPEN));

            // Send success message
            sendSuccessMessage(player, recipe, heldItem);

        } catch (Exception e) {
            sendMessage(player, "recipe.import_failed");
            e.printStackTrace();
        }
    }
    
    /**
     * Optimized custom plugin detection
     */
    private String detectCustomItemPlugin(ItemStack item) {
        try {
            de.tr7zw.changeme.nbtapi.NBTItem nbtItem = new de.tr7zw.changeme.nbtapi.NBTItem(item);
            
            for (int i = 0; i < PLUGIN_TAGS.length; i++) {
                if (nbtItem.hasTag(PLUGIN_TAGS[i])) {
                    return PLUGIN_NAMES[i];
                }
            }
        } catch (Exception ignored) {
            // NBT operations might fail, continue without detection
        }
        
        return null;
    }
    
    /**
     * Send detailed success message efficiently
     */
    private void sendSuccessMessage(Player player, Recipe recipe, ItemStack item) {
        String itemName = getItemDisplayName(item);
        String materialString = ItemImportUtil.getMaterialString(item);
        
        String[] placeholders = {"#recipe#", "#item_name#", "#material#"};
        String[] replacements = {recipe.getName(), itemName, materialString};
        sendMessage(player, "recipe.import_success_detailed", placeholders, replacements);
    }
    
    /**
     * Get display name efficiently
     */
    private String getItemDisplayName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        
        return formatMaterialName(item.getType().name());
    }
    
    /**
     * Format material name to user-friendly format
     */
    private String formatMaterialName(String materialName) {
        String[] words = materialName.toLowerCase().replace("_", " ").split(" ");
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < words.length; i++) {
            if (i > 0) result.append(" ");
            String word = words[i];
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    result.append(word.substring(1));
                }
            }
        }
        
        return result.toString();
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("import");
        }
        return Collections.emptyList();
    }

    @Override
    public String getPermission() {
        return "storage.craft.admin";
    }

    @Override
    public String getUsage() {
        return "/storage crafteditor [import]";
    }

    @Override
    public String getDescription() {
        return "Open recipe editor or import held item (Admin only)";
    }
}