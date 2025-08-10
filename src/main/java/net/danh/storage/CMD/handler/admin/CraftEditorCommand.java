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

            // Import item data into recipe
            ItemImportUtil.importItemToRecipe(heldItem, recipe);

            // Add recipe to manager
            CraftingManager.addRecipe(recipe);

            // Open recipe editor with imported recipe
            SoundManager.setShouldPlayCloseSound(player, false);
            player.openInventory(new RecipeEditorGUI(player, recipe).getInventory(SoundContext.INITIAL_OPEN));

            // Send success message
            sendMessage(player, "recipe.import_success", "#recipe#", recipe.getName());

        } catch (Exception e) {
            sendMessage(player, "recipe.import_failed");
            e.printStackTrace();
        }
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
