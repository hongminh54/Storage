package net.danh.storage.CMD.handler.user;

import net.danh.storage.CMD.handler.BaseCommand;
import net.danh.storage.GUI.RecipeListGUI;
import net.danh.storage.Manager.SoundManager;
import net.danh.storage.Utils.SoundContext;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class CraftCommand extends BaseCommand {

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, "admin.player_only");
            return;
        }

        Player player = (Player) sender;
        
        // Open recipe list GUI
        SoundManager.setShouldPlayCloseSound(player, false);
        player.openInventory(new RecipeListGUI(player).getInventory(SoundContext.INITIAL_OPEN));
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }

    @Override
    public String getPermission() {
        return "storage.craft.use";
    }

    @Override
    public String getUsage() {
        return "/storage craft";
    }

    @Override
    public String getDescription() {
        return "Open custom recipe crafting menu";
    }
}
