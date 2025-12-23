package net.danh.storage.CMD.handler.admin;

import com.cryptomorin.xseries.XEnchantment;
import de.tr7zw.changeme.nbtapi.NBTItem;
import net.danh.storage.CMD.handler.BaseCommand;
import net.danh.storage.GUI.RecipeEditorGUI;
import net.danh.storage.GUI.RecipeEditorListGUI;
import net.danh.storage.Manager.CraftingManager;
import net.danh.storage.Manager.MineManager;
import net.danh.storage.Manager.SoundManager;
import net.danh.storage.Recipe.Recipe;
import net.danh.storage.Storage;
import net.danh.storage.Utils.SoundContext;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class CraftEditorCommand extends BaseCommand {

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, "admin.only_players");
            return;
        }

        Player player = (Player) sender;

        if (args.length > 0 && args[0].equalsIgnoreCase("import")) {
            handleImportCommand(player);
            return;
        }

        SoundManager.setShouldPlayCloseSound(player, false);
        player.openInventory(new RecipeEditorListGUI(player).getInventory(SoundContext.INITIAL_OPEN));
    }

    private void handleImportCommand(Player player) {
        ItemStack heldItem = player.getInventory().getItemInMainHand();

        if (heldItem == null || heldItem.getType() == Material.AIR) {
            sendMessage(player, "crafting.import_empty_hand");
            SoundManager.playSound(player, SoundManager.SoundType.ACTION_ERROR);
            return;
        }

        try {
            String recipeId = CraftingManager.generateUniqueId();
            Recipe recipe = new Recipe(recipeId);

            try {
                ItemStack itemToStore = heldItem.clone();
                itemToStore.setAmount(1);
                NBTItem nbtItem = new NBTItem(itemToStore);
                recipe.setResultItemNbt(nbtItem.toString());
            } catch (Exception ignored) {
            }

            recipe.setResultMaterial(MineManager.normalizeMaterial(heldItem.getType().name()));
            recipe.setResultAmount(Math.max(1, Math.min(64, heldItem.getAmount())));
            recipe.setCategory("imported");

            if (heldItem.hasItemMeta()) {
                ItemMeta meta = heldItem.getItemMeta();

                String recipeName = "&eImported " + heldItem.getType().name();
                if (meta != null && meta.hasDisplayName()) {
                    recipeName = meta.getDisplayName();
                }
                recipe.setName(recipeName);

                // Import display name
                if (meta != null && meta.hasDisplayName()) {
                    recipe.setResultName(meta.getDisplayName());
                } else {
                    recipe.setResultName("&eImported Item");
                }

                // Import lore
                if (meta != null && meta.hasLore()) {
                    recipe.setResultLore(meta.getLore());
                }

                // Import enchantments
                if (meta != null && meta.hasEnchants()) {
                    Map<String, Integer> enchants = new HashMap<>();
                    for (Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
                        String key = null;
                        try {
                            XEnchantment xEnchantment = XEnchantment.matchXEnchantment(entry.getKey());
                            if (xEnchantment != null) {
                                key = xEnchantment.name();
                            }
                        } catch (Exception ignored) {
                        }

                        if (key == null) {
                            key = entry.getKey().getName().toUpperCase();
                        }

                        enchants.put(key, entry.getValue());
                    }
                    recipe.setResultEnchantments(enchants);
                }

                // Import item flags
                if (meta != null) {
                    recipe.setResultFlags(meta.getItemFlags());
                }

                // Import custom model data (1.14+)
                try {
                    if (meta != null && meta.hasCustomModelData()) {
                        recipe.setResultCustomModelData(meta.getCustomModelData());
                    }
                } catch (NoSuchMethodError ignored) {
                }

                // Import unbreakable (1.11+)
                try {
                    if (meta != null) {
                        recipe.setResultUnbreakable(meta.isUnbreakable());
                    }
                } catch (NoSuchMethodError ignored) {
                }
            } else {
                recipe.setName("&eImported " + heldItem.getType().name());
            }

            CraftingManager.addRecipe(recipe);

            sendMessage(player, "crafting.import_success", "#recipe#", recipe.getName());
            SoundManager.playSound(player, SoundManager.SoundType.ACTION_SUCCESS);

            SoundManager.setShouldPlayCloseSound(player, false);
            player.openInventory(new RecipeEditorGUI(player, recipe).getInventory(SoundContext.INITIAL_OPEN));

        } catch (Exception e) {
            sendMessage(player, "crafting.import_failed");
            SoundManager.playSound(player, SoundManager.SoundType.ACTION_ERROR);
            Storage.getStorage().getLogger().log(Level.SEVERE, "Failed to import item as recipe", e);
        }
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Collections.singletonList("import");
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
        return "Open recipe editor or import held item";
    }
}
