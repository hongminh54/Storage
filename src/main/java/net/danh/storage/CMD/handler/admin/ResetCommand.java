package net.danh.storage.CMD.handler.admin;

import net.danh.storage.CMD.handler.BaseCommand;
import net.danh.storage.Manager.MineManager;
import net.danh.storage.Utils.File;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

public class ResetCommand extends BaseCommand {

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (!requirePlayer(sender)) return;
            Player player = (Player) sender;
            resetAllStorage(player);
            sendMessage(sender, "admin.reset_self_all");
            return;
        }

        if (args.length == 1) {
            String material = args[0];

            if (material.equalsIgnoreCase("all")) {
                if (!requirePlayer(sender)) return;
                Player player = (Player) sender;
                resetAllStorage(player);
                sendMessage(sender, "admin.reset_self_all");
                return;
            }

            if (!MineManager.getPluginBlocks().contains(material)) {
                sendInvalidMaterial(sender, material, new ArrayList<>(MineManager.getPluginBlocks()));
                return;
            }

            if (!requirePlayer(sender)) return;
            Player player = (Player) sender;
            resetMaterialStorage(player, material);
            sendMessage(sender, "admin.reset_self_material", "#material#", material);
            return;
        }

        if (args.length == 2) {
            String material = args[0];
            String targetName = args[1];

            Player target = getPlayer(targetName);
            if (target == null) {
                sendInvalidPlayer(sender, targetName);
                return;
            }

            if (material.equalsIgnoreCase("all")) {
                resetAllStorage(target);
                String[] placeholders = {"#player#"};
                String[] replacements = {target.getName()};
                sendMessage(sender, "admin.reset_target_all", placeholders, replacements);

                if (shouldNotifyTargetPlayer()) {
                    sendMessage(target, "admin.reset_target_all_notify", "#player#", sender.getName());
                }
                return;
            }

            if (!MineManager.getPluginBlocks().contains(material)) {
                sendInvalidMaterial(sender, material, new ArrayList<>(MineManager.getPluginBlocks()));
                return;
            }

            resetMaterialStorage(target, material);
            String[] placeholders = {"#material#", "#player#"};
            String[] replacements = {material, target.getName()};
            sendMessage(sender, "admin.reset_target_material", placeholders, replacements);

            if (shouldNotifyTargetPlayer()) {
                sendMessage(target, "admin.reset_target_material_notify", placeholders, new String[]{material, sender.getName()});
            }
        }
    }

    private void resetAllStorage(Player player) {
        for (String material : MineManager.getPluginBlocks()) {
            MineManager.setBlock(player, material, 0);
        }
    }

    private void resetMaterialStorage(Player player, String material) {
        MineManager.setBlock(player, material, 0);
    }

    private boolean shouldNotifyTargetPlayer() {
        return File.getConfig().getBoolean("settings.reset_notification.notify_target_player", true);
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> materials = new ArrayList<>(MineManager.getPluginBlocks());
            materials.add("all");
            StringUtil.copyPartialMatches(args[0], materials, completions);
        }

        if (args.length == 2) {
            if (MineManager.getPluginBlocks().contains(args[0]) || args[0].equalsIgnoreCase("all")) {
                List<String> playerNames = getOnlinePlayerNames();
                StringUtil.copyPartialMatches(args[1], playerNames, completions);
            }
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "storage.admin.reset";
    }

    @Override
    public String getUsage() {
        return "/storage reset [material|all] [player]";
    }

    @Override
    public String getDescription() {
        return "Reset storage materials for players";
    }
}
