package net.danh.storage.CMD.handler.admin;

import net.danh.storage.CMD.handler.BaseCommand;
import net.danh.storage.Manager.MineManager;
import net.danh.storage.Utils.Number;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SetCommand extends BaseCommand {

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length != 3) {
            sendUsage(sender);
            return;
        }

        Player target = getPlayer(args[1]);
        if (target == null) {
            sendInvalidPlayer(sender, args[1]);
            return;
        }

        int amount = Number.getInteger(args[2]);
        if (amount < 0) {
            sendInvalidNumber(sender, args[2]);
            return;
        }

        List<String> materials = parseMaterials(args[0]);
        if (materials.isEmpty()) {
            sendInvalidMaterial(sender, args[0], new ArrayList<>(MineManager.getPluginBlocks()));
            return;
        }

        for (String material : materials) {
            MineManager.setBlock(target, material, amount);
        }

        String materialDisplay = materials.size() == 1 ? materials.get(0) :
                (materials.size() == MineManager.getPluginBlocks().size() ? "*" : String.join(", ", materials));

        String[] placeholders = {"#amount#", "#material#", "#player#"};
        String[] replacements = {args[2], materialDisplay, target.getName()};
        sendMessage(sender, "admin.set_material_amount", placeholders, replacements);

        String[] targetPlaceholders = {"#amount#", "#material#", "#player#"};
        String[] targetReplacements = {args[2], materialDisplay, sender.getName()};
        sendMessage(target, "user.set_material_amount", targetPlaceholders, targetReplacements);
    }

    private List<String> parseMaterials(String input) {
        List<String> result = new ArrayList<>();
        if (input.equals("*")) {
            result.addAll(MineManager.getPluginBlocks());
        } else if (input.contains(",")) {
            for (String mat : input.split(",")) {
                String trimmed = mat.trim();
                if (MineManager.getPluginBlocks().contains(trimmed)) {
                    result.add(trimmed);
                }
            }
        } else {
            if (MineManager.getPluginBlocks().contains(input)) {
                result.add(input);
            }
        }
        return result;
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            suggestions.add("*");
            suggestions.addAll(MineManager.getPluginBlocks());
            StringUtil.copyPartialMatches(args[0], suggestions, completions);
        }

        if (args.length == 2) {
            List<String> playerNames = getOnlinePlayerNames();
            StringUtil.copyPartialMatches(args[1], playerNames, completions);
        }

        if (args.length == 3) {
            StringUtil.copyPartialMatches(args[2], Arrays.asList("0", "1", "10", "64", "100"), completions);
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "storage.admin.set";
    }

    @Override
    public String getUsage() {
        return "/storage set <material|*|mat1,mat2,...> <player> <amount>";
    }

    @Override
    public String getDescription() {
        return "Set material amount in player storage";
    }
}
