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

public class AddCommand extends BaseCommand {

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length != 3) {
            sendUsage(sender);
            return;
        }

        String material = args[0];
        if (!MineManager.getPluginBlocks().contains(material)) {
            sendInvalidMaterial(sender, material, new ArrayList<>(MineManager.getPluginBlocks()));
            return;
        }

        Player target = getPlayer(args[1]);
        if (target == null) {
            sendInvalidPlayer(sender, args[1]);
            return;
        }

        int amount = Number.getInteger(args[2]);
        if (amount < 0) {
            if (Number.getInteger(args[2]) == -1) {
                sendInvalidNumber(sender, args[2]);
            } else {
                sendNumberTooLow(sender);
            }
            return;
        }

        if (MineManager.addBlockAmount(target, material, amount)) {
            String[] placeholders = {"#amount#", "#material#", "#player#"};
            String[] replacements = {args[2], material, target.getName()};

            sendMessage(sender, "admin.add_material_amount", placeholders, replacements);

            String[] targetPlaceholders = {"#amount#", "#material#", "#player#"};
            String[] targetReplacements = {args[2], material, sender.getName()};
            sendMessage(target, "user.add_material_amount", targetPlaceholders, targetReplacements);
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
        return "storage.admin.add";
    }

    @Override
    public String getUsage() {
        return "/storage add <material> <player> <amount>";
    }

    @Override
    public String getDescription() {
        return "Add materials to player storage";
    }
}
