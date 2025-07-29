package net.danh.storage.CMD.handler.admin;

import net.danh.storage.CMD.handler.BaseCommand;
import net.danh.storage.Manager.MineManager;
import net.danh.storage.Utils.Number;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RemoveCommand extends BaseCommand {

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length != 3) {
            return;
        }

        String material = args[0];
        if (!MineManager.getPluginBlocks().contains(material)) {
            return;
        }

        Player target = getPlayer(args[1]);
        if (target == null) {
            return;
        }

        int amount = Number.getInteger(args[2]);
        if (amount < 0) {
            return;
        }

        if (MineManager.removeBlockAmount(target, material, amount)) {
            String[] placeholders = {"#amount#", "#material#", "#player#"};
            String[] replacements = {args[2], material, target.getName()};

            sendMessage(sender, "admin.remove_material_amount", placeholders, replacements);

            String[] targetPlaceholders = {"#amount#", "#material#", "#player#"};
            String[] targetReplacements = {args[2], material, sender.getName()};
            sendMessage(target, "user.remove_material_amount", targetPlaceholders, targetReplacements);
        }
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> materials = new ArrayList<>(MineManager.getPluginBlocks());
            StringUtil.copyPartialMatches(args[0], materials, completions);
        }

        if (args.length == 2) {
            if (MineManager.getPluginBlocks().contains(args[0])) {
                List<String> playerNames = getOnlinePlayerNames();
                StringUtil.copyPartialMatches(args[1], playerNames, completions);
            }
        }

        if (args.length == 3) {
            if (MineManager.getPluginBlocks().contains(args[0])) {
                StringUtil.copyPartialMatches(args[2], Collections.singleton("<number>"), completions);
            }
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "storage.admin.remove";
    }

    @Override
    public String getUsage() {
        return "/storage remove <material> <player> <amount>";
    }

    @Override
    public String getDescription() {
        return "Remove materials from player storage";
    }
}
