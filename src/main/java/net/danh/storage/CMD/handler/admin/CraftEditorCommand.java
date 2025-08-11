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

public class CraftEditorCommand extends BaseCommand {

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, "admin.player_only");
            return;
        }

        Player player = (Player) sender;

        // Handle subcommands
        if (args.length > 0 && args[0].equalsIgnoreCase("import")) {
            handleImportCommand(player);
            return;
        }

        // Default behavior - open recipe editor list GUI
        SoundManager.setShouldPlayCloseSound(player, false);
        player.openInventory(new RecipeEditorListGUI(player).getInventory(SoundContext.INITIAL_OPEN));
    }

    private void handleImportCommand(Player player) {
        ItemStack heldItem = player.getInventory().getItemInMainHand();

        // Check if player is holding an item
        if (heldItem == null || heldItem.getType().name().equals("AIR")) {
            sendMessage(player, "recipe.import_empty_hand");
            return;
        }

        try {
            // Create new recipe with imported item
            String recipeId = CraftingManager.generateUniqueId();
            Recipe recipe = new Recipe(recipeId);

            // Import item data into recipe using improved method
            ItemImportUtil.importItemToRecipe(heldItem, recipe);

            // Add recipe to manager
            CraftingManager.addRecipe(recipe);

            // Detect custom item plugins for detailed message
            String customPlugin = detectCustomItemPlugin(heldItem);
            if (customPlugin != null) {
                sendMessage(player, "recipe.import_custom_item_detected", "#plugin#", customPlugin);
            }

            // Open recipe editor with imported recipe
            SoundManager.setShouldPlayCloseSound(player, false);
            player.openInventory(new RecipeEditorGUI(player, recipe).getInventory(SoundContext.INITIAL_OPEN));

            // Send detailed success message
            String itemName = getItemDisplayName(heldItem);
            String materialString = ItemImportUtil.getMaterialString(heldItem);
            
            String[] placeholders = {"#recipe#", "#item_name#", "#material#"};
            String[] replacements = {recipe.getName(), itemName, materialString};
            sendMessage(player, "recipe.import_success_detailed", placeholders, replacements);

        } catch (Exception e) {
            sendMessage(player, "recipe.import_failed");
            e.printStackTrace();
        }
    }
    
    /**
     * Detects if the item is from a custom item plugin
     */
    private String detectCustomItemPlugin(ItemStack item) {
        try {
            de.tr7zw.changeme.nbtapi.NBTItem nbtItem = new de.tr7zw.changeme.nbtapi.NBTItem(item);
            
            if (nbtItem.hasTag("MMOITEMS_TYPE") || nbtItem.hasTag("MMOITEMS_ID")) {
                return "MMOItems";
            }
            if (nbtItem.hasTag("MyItems")) {
                return "MyItems";
            }
            if (nbtItem.hasTag("itemsadder")) {
                return "ItemsAdder";
            }
            if (nbtItem.hasTag("oraxen")) {
                return "Oraxen";
            }
            if (nbtItem.hasTag("ExecutableItems")) {
                return "ExecutableItems";
            }
            if (nbtItem.hasTag("MythicMobs")) {
                return "MythicMobs";
            }
            if (nbtItem.hasTag("EliteMobs")) {
                return "EliteMobs";
            }
            
        } catch (Exception ignored) {
            // NBT operations might fail, continue without detection
        }
        
        return null;
    }
    
    /**
     * Gets the display name of an item for user-friendly messages
     */
    private String getItemDisplayName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        
        // Generate friendly name from material
        String materialName = item.getType().name().toLowerCase().replace("_", " ");
        String[] words = materialName.split(" ");
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < words.length; i++) {
            if (i > 0) result.append(" ");
            if (!words[i].isEmpty()) {
                result.append(Character.toUpperCase(words[i].charAt(0)));
                if (words[i].length() > 1) {
                    result.append(words[i].substring(1));
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