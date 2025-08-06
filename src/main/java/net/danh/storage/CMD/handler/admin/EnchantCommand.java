package net.danh.storage.CMD.handler.admin;

import net.danh.storage.CMD.handler.BaseCommand;
import net.danh.storage.Manager.EnchantManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EnchantCommand extends BaseCommand {

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!checkPermission(sender, "storage.admin")) {
            sendMessage(sender, "admin.no_permission");
            return;
        }

        if (args.length < 1) {
            sendUsage(sender);
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "give":
                handleGive(sender, args);
                break;
            case "remove":
                handleRemove(sender, args);
                break;
            case "list":
                handleList(sender, args);
                break;
            case "info":
                handleInfo(sender, args);
                break;
            case "setmaxlevel":
                handleSetMaxLevel(sender, args);
                break;
            default:
                sendUsage(sender);
                break;
        }
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sendMessage(sender, "admin.invalid_usage", "#usage#", "/storage enchant give <player> <enchant> <level>");
            return;
        }

        String playerName = args[1];
        String enchantName = args[2];
        String levelStr = args[3];

        Player target = getPlayer(playerName);
        if (target == null) {
            sendInvalidPlayer(sender, playerName);
            return;
        }

        if (!EnchantManager.isValidEnchant(enchantName)) {
            sendMessage(sender, "enchant.invalid_enchant", "#enchant#", enchantName);
            return;
        }

        if (!isValidNumber(levelStr)) {
            sendInvalidNumber(sender, levelStr);
            return;
        }

        int level = Integer.parseInt(levelStr);
        if (!EnchantManager.isValidLevel(enchantName, level)) {
            sendMessage(sender, "enchant.invalid_level", new String[]{"#level#", "#max#"},
                    new String[]{String.valueOf(level), String.valueOf(EnchantManager.getEnchantData(enchantName).maxLevel)});
            return;
        }

        ItemStack item = target.getInventory().getItemInMainHand();
        if (item == null || item.getType().name().equals("AIR")) {
            sendMessage(sender, "enchant.no_item_in_hand");
            return;
        }

        if (!EnchantManager.isApplicableItem(item, enchantName)) {
            sendMessage(sender, "enchant.not_applicable_item");
            return;
        }

        ItemStack enchantedItem = EnchantManager.addEnchant(item, enchantName, level);
        target.getInventory().setItemInMainHand(enchantedItem);

        sendMessage(sender, "enchant.give_success", new String[]{"#player#", "#enchant#", "#level#"},
                new String[]{target.getName(), enchantName, String.valueOf(level)});
        sendMessage(target, "enchant.receive_enchant", new String[]{"#enchant#", "#level#"},
                new String[]{enchantName, String.valueOf(level)});
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendMessage(sender, "admin.invalid_usage", "#usage#", "/storage enchant remove <player> <enchant>");
            return;
        }

        String playerName = args[1];
        String enchantName = args[2];

        Player target = getPlayer(playerName);
        if (target == null) {
            sendInvalidPlayer(sender, playerName);
            return;
        }

        if (!EnchantManager.isValidEnchant(enchantName)) {
            sendMessage(sender, "enchant.invalid_enchant", "#enchant#", enchantName);
            return;
        }

        ItemStack item = target.getInventory().getItemInMainHand();
        if (item == null || item.getType().name().equals("AIR")) {
            sendMessage(sender, "enchant.no_item_in_hand");
            return;
        }

        if (!EnchantManager.hasEnchant(item, enchantName)) {
            sendMessage(sender, "enchant.not_enchanted", "#enchant#", enchantName);
            return;
        }

        ItemStack unenchantedItem = EnchantManager.removeEnchant(item, enchantName);
        target.getInventory().setItemInMainHand(unenchantedItem);

        sendMessage(sender, "enchant.remove_success", new String[]{"#player#", "#enchant#"},
                new String[]{target.getName(), enchantName});
        sendMessage(target, "enchant.enchant_removed", "#enchant#", enchantName);
    }

    private void handleList(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            String playerName = args[1];
            Player target = getPlayer(playerName);
            if (target == null) {
                sendInvalidPlayer(sender, playerName);
                return;
            }

            ItemStack item = target.getInventory().getItemInMainHand();
            if (item == null || item.getType().name().equals("AIR")) {
                sendMessage(sender, "enchant.no_item_in_hand");
                return;
            }

            sendMessage(sender, "enchant.item_enchants_header", "#player#", target.getName());
            boolean hasEnchants = false;
            for (String enchantName : EnchantManager.getAvailableEnchants()) {
                if (EnchantManager.hasEnchant(item, enchantName)) {
                    int level = EnchantManager.getEnchantLevel(item, enchantName);
                    sendMessage(sender, "enchant.item_enchant_entry", new String[]{"#enchant#", "#level#"},
                            new String[]{enchantName, String.valueOf(level)});
                    hasEnchants = true;
                }
            }
            if (!hasEnchants) {
                sendMessage(sender, "enchant.no_enchants_on_item");
            }
        } else {
            sendMessage(sender, "enchant.available_enchants_header");
            for (String enchantName : EnchantManager.getAvailableEnchants()) {
                EnchantManager.EnchantData data = EnchantManager.getEnchantData(enchantName);
                sendMessage(sender, "enchant.available_enchant_entry", new String[]{"#enchant#", "#name#", "#max_level#"},
                        new String[]{enchantName, data.name, String.valueOf(data.maxLevel)});
            }
        }
    }


    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendMessage(sender, "admin.invalid_usage", "#usage#", "/storage enchant info <enchant>");
            return;
        }

        String enchantName = args[1];
        if (!EnchantManager.isValidEnchant(enchantName)) {
            sendMessage(sender, "enchant.invalid_enchant", "#enchant#", enchantName);
            return;
        }

        EnchantManager.EnchantData data = EnchantManager.getEnchantData(enchantName);
        sendMessage(sender, "enchant.info_header", "#enchant#", data.name);
        sendMessage(sender, "enchant.info_description", "#description#", data.description);
        sendMessage(sender, "enchant.info_max_level", "#max_level#", String.valueOf(data.maxLevel));
        sendMessage(sender, "enchant.info_applicable_items", "#items#", String.join(", ", data.applicableItems));
    }

    private void handleSetMaxLevel(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendMessage(sender, "admin.invalid_usage", "#usage#", "/storage enchant setmaxlevel <enchant> <level>");
            return;
        }

        String enchantName = args[1];
        String levelStr = args[2];

        if (!EnchantManager.isValidEnchant(enchantName)) {
            sendMessage(sender, "enchant.invalid_enchant", "#enchant#", enchantName);
            return;
        }

        if (!isValidNumber(levelStr)) {
            sendInvalidNumber(sender, levelStr);
            return;
        }

        int newMaxLevel = Integer.parseInt(levelStr);
        if (newMaxLevel < 1 || newMaxLevel > EnchantManager.getMaxAllowedLevel()) {
            sendMessage(sender, "enchant.invalid_max_level", new String[]{"#level#", "#max#"},
                    new String[]{String.valueOf(newMaxLevel), String.valueOf(EnchantManager.getMaxAllowedLevel())});
            return;
        }

        if (EnchantManager.updateEnchantMaxLevel(enchantName, newMaxLevel)) {
            sendMessage(sender, "enchant.setmaxlevel_success", new String[]{"#enchant#", "#level#"},
                    new String[]{enchantName, String.valueOf(newMaxLevel)});
        } else {
            sendMessage(sender, "enchant.setmaxlevel_failed", "#enchant#", enchantName);
        }
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("give", "remove", "list", "info", "setmaxlevel"));
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("give") || subCommand.equals("remove") || subCommand.equals("list")) {
                completions.addAll(getOnlinePlayerNames());
            } else if (subCommand.equals("info") || subCommand.equals("setmaxlevel")) {
                completions.addAll(EnchantManager.getAvailableEnchants());
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("give") || subCommand.equals("remove")) {
                completions.addAll(EnchantManager.getAvailableEnchants());
            } else if (subCommand.equals("setmaxlevel")) {
                for (int i = 1; i <= EnchantManager.getMaxAllowedLevel(); i++) {
                    completions.add(String.valueOf(i));
                }
            }
        } else if (args.length == 4) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("give")) {
                String enchantName = args[2];
                if (EnchantManager.isValidEnchant(enchantName)) {
                    EnchantManager.EnchantData data = EnchantManager.getEnchantData(enchantName);
                    for (int i = 1; i <= data.maxLevel; i++) {
                        completions.add(String.valueOf(i));
                    }
                }
            }
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "storage.admin";
    }

    @Override
    public String getUsage() {
        return "/storage enchant <give|remove|list|info|setmaxlevel> [args...]";
    }

    @Override
    public String getDescription() {
        return "Manage custom enchantments";
    }
}
