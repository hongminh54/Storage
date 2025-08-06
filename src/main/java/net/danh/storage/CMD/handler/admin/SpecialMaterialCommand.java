package net.danh.storage.CMD.handler.admin;

import net.danh.storage.CMD.handler.BaseCommand;
import net.danh.storage.Manager.SpecialMaterialManager;
import org.bukkit.command.CommandSender;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SpecialMaterialCommand extends BaseCommand {

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "list":
                handleList(sender);
                break;
            case "info":
                if (args.length < 2) {
                    sendMessage(sender, "admin.invalid_usage", "#usage#", "/storage specialmaterial info <material_id>");
                    return;
                }
                handleInfo(sender, args[1]);
                break;
            case "give":
                if (args.length < 4) {
                    sendMessage(sender, "admin.invalid_usage", "#usage#", "/storage specialmaterial give <player> <material_id> <amount>");
                    return;
                }
                handleGive(sender, args[1], args[2], args[3]);
                break;
            default:
                sendMessage(sender, "special_material.invalid_subcommand", "#subcommand#", subCommand);
                break;
        }
    }

    private void handleList(CommandSender sender) {
        int count = SpecialMaterialManager.getLoadedMaterialsCount();
        if (count == 0) {
            sendMessage(sender, "special_material.no_materials");
            return;
        }

        sendMessage(sender, "special_material.list.header", "#count#", String.valueOf(count));

        for (String materialId : SpecialMaterialManager.getLoadedMaterialIds()) {
            String info = SpecialMaterialManager.getMaterialInfo(materialId);
            if (info != null) {
                sendMessage(sender, "special_material.list.entry", "#info#", info);
            }
        }
    }

    private void handleInfo(CommandSender sender, String materialId) {
        if (!SpecialMaterialManager.hasMaterial(materialId)) {
            sendMessage(sender, "special_material.material_not_found", "#material#", materialId);
            return;
        }

        String info = SpecialMaterialManager.getMaterialInfo(materialId);
        if (info != null) {
            sendMessage(sender, "special_material.info.header", "#material#", materialId);
            sendMessage(sender, "special_material.info.details", "#info#", info);
        }
    }

    private void handleGive(CommandSender sender, String playerName, String materialId, String amountStr) {
        // Check if material exists
        if (!SpecialMaterialManager.hasMaterial(materialId)) {
            sendMessage(sender, "special_material.material_not_found", "#material#", materialId);
            return;
        }

        // Get target player
        org.bukkit.entity.Player target = getPlayer(playerName);
        if (target == null) {
            sendInvalidPlayer(sender, playerName);
            return;
        }

        // Parse amount
        int amount;
        try {
            amount = Integer.parseInt(amountStr);
            if (amount <= 0) {
                sendNumberTooLow(sender);
                return;
            }
        } catch (NumberFormatException e) {
            sendInvalidNumber(sender, amountStr);
            return;
        }

        // Give special material
        if (SpecialMaterialManager.giveSpecialMaterial(target, materialId, amount)) {
            String[] placeholders = {"#amount#", "#material#", "#player#"};
            String[] replacements = {String.valueOf(amount), materialId, target.getName()};
            sendMessage(sender, "special_material.give_success", placeholders, replacements);

            String[] targetPlaceholders = {"#amount#", "#material#", "#player#"};
            String[] targetReplacements = {String.valueOf(amount), materialId, sender.getName()};
            sendMessage(target, "special_material.give_received", targetPlaceholders, targetReplacements);
        } else {
            sendMessage(sender, "special_material.give_failed");
        }
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("list", "info", "give");
            StringUtil.copyPartialMatches(args[0], subCommands, completions);
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("info")) {
                List<String> materialIds = new ArrayList<>(SpecialMaterialManager.getLoadedMaterialIds());
                StringUtil.copyPartialMatches(args[1], materialIds, completions);
            } else if (args[0].equalsIgnoreCase("give")) {
                List<String> playerNames = getOnlinePlayerNames();
                StringUtil.copyPartialMatches(args[1], playerNames, completions);
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            List<String> materialIds = new ArrayList<>(SpecialMaterialManager.getLoadedMaterialIds());
            StringUtil.copyPartialMatches(args[2], materialIds, completions);
        } else if (args.length == 4 && args[0].equalsIgnoreCase("give")) {
            StringUtil.copyPartialMatches(args[3], Collections.singleton("<amount>"), completions);
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "storage.admin.specialmaterial";
    }

    @Override
    public String getUsage() {
        return "/storage specialmaterial <list|info|give>";
    }

    @Override
    public String getDescription() {
        return "Manage special materials";
    }
}
