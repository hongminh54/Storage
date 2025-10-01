package net.danh.storage.CMD.handler;

import net.danh.storage.GUI.MythicStorageGUI;
import net.danh.storage.Manager.MythicStorageManager;
import net.danh.storage.Utils.Chat;
import net.danh.storage.Utils.File;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class MythicStorageCmdHandler {

    public void handleCommand(@NotNull CommandSender sender, String[] args) {
        if (!MythicStorageManager.isSystemEnabled()) {
            sender.sendMessage(Chat.colorize(getMessage("system_disabled")));
            return;
        }

        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(Chat.colorize(getMessage("admin.only_players")));
                return;
            }
            Player player = (Player) sender;
            player.openInventory(new MythicStorageGUI(player).getInventory());
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "toggle":
                handleToggle(sender);
                break;
            case "view":
                handleView(sender, args);
                break;
            case "admin":
                handleAdmin(sender, args);
                break;
            case "help":
                handleHelp(sender);
                break;
            default:
                sender.sendMessage(Chat.colorize(getMessage("admin.unknown_command")
                    .replace("#command#", subCommand)));
                break;
        }
    }

    private void handleToggle(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Chat.colorize(getMessage("admin.only_players")));
            return;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("storage.mythicstorage.toggle")) {
            player.sendMessage(Chat.colorize(getMessage("admin.no_permission")));
            return;
        }

        boolean currentStatus = MythicStorageManager.getToggleStatus(player);
        MythicStorageManager.setToggleStatus(player, !currentStatus);

        String message = !currentStatus 
            ? getMessage("enabled")
            : getMessage("disabled");

        if (!message.isEmpty()) {
            player.sendMessage(Chat.colorize(message));
        }
    }

    private void handleView(CommandSender sender, String[] args) {
        if (!sender.hasPermission("storage.mythicstorage.view")) {
            sender.sendMessage(Chat.colorize(getMessage("admin.no_permission")));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(Chat.colorize(getPrefix() + " &cUsage: /mythicstorage view <player>"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null || !target.isOnline()) {
            String message = getMessage("player_not_found")
                .replace("#player#", args[1]);
            sender.sendMessage(Chat.colorize(message));
            return;
        }

        if (sender instanceof Player) {
            ((Player) sender).openInventory(new MythicStorageGUI(target).getInventory());
        } else {
            sender.sendMessage(Chat.colorize(getMessage("admin.only_players")));
        }
    }

    private void handleAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("storage.admin")) {
            sender.sendMessage(Chat.colorize(getMessage("admin.no_permission")));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(Chat.colorize(getPrefix() + " &cUsage: /mythicstorage admin <add|remove|set|reset|reload>"));
            return;
        }

        String adminCmd = args[1].toLowerCase();

        switch (adminCmd) {
            case "add":
                handleAdminAdd(sender, args);
                break;
            case "remove":
                handleAdminRemove(sender, args);
                break;
            case "set":
                handleAdminSet(sender, args);
                break;
            case "reset":
                handleAdminReset(sender, args);
                break;
            case "reload":
                handleAdminReload(sender);
                break;
            default:
                sender.sendMessage(Chat.colorize(getPrefix() + " &cUnknown admin command"));
                break;
        }
    }

    private void handleAdminAdd(CommandSender sender, String[] args) {
        if (args.length < 5) {
            sender.sendMessage(Chat.colorize(getPrefix() + " &cUsage: /mythicstorage admin add <player> <item> <amount>"));
            return;
        }

        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            sender.sendMessage(Chat.colorize(getMessage("player_not_found").replace("#player#", args[2])));
            return;
        }

        String itemName = args[3];
        if (!MythicStorageManager.isConfiguredDrop(itemName)) {
            sender.sendMessage(Chat.colorize(getMessage("invalid_item").replace("#item#", itemName)));
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[4]);
            if (amount <= 0) {
                sender.sendMessage(Chat.colorize(getMessage("admin.number_too_low")));
                return;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(Chat.colorize(getMessage("admin.invalid_number").replace("#number#", args[4])));
            return;
        }

        MythicStorageManager.addItemAmount(target, itemName, amount);
        
        String message = getMessage("admin.add_success")
            .replace("#amount#", String.valueOf(amount))
            .replace("#item#", itemName)
            .replace("#player#", target.getName());
        sender.sendMessage(Chat.colorize(message));
    }

    private void handleAdminRemove(CommandSender sender, String[] args) {
        if (args.length < 5) {
            sender.sendMessage(Chat.colorize(getPrefix() + " &cUsage: /mythicstorage admin remove <player> <item> <amount>"));
            return;
        }

        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            sender.sendMessage(Chat.colorize(getMessage("player_not_found").replace("#player#", args[2])));
            return;
        }

        String itemName = args[3];
        if (!MythicStorageManager.isConfiguredDrop(itemName)) {
            sender.sendMessage(Chat.colorize(getMessage("invalid_item").replace("#item#", itemName)));
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[4]);
            if (amount <= 0) {
                sender.sendMessage(Chat.colorize(getMessage("admin.number_too_low")));
                return;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(Chat.colorize(getMessage("admin.invalid_number").replace("#number#", args[4])));
            return;
        }

        int currentAmount = MythicStorageManager.getPlayerItem(target, itemName);
        if (currentAmount < amount) {
            sender.sendMessage(Chat.colorize(getMessage("admin.not_enough_items")
                .replace("#player#", target.getName())
                .replace("#item#", itemName)));
            return;
        }

        MythicStorageManager.removeItemAmount(target, itemName, amount);
        
        String message = getMessage("admin.remove_success")
            .replace("#amount#", String.valueOf(amount))
            .replace("#item#", itemName)
            .replace("#player#", target.getName());
        sender.sendMessage(Chat.colorize(message));
    }

    private void handleAdminSet(CommandSender sender, String[] args) {
        if (args.length < 5) {
            sender.sendMessage(Chat.colorize(getPrefix() + " &cUsage: /mythicstorage admin set <player> <item> <amount>"));
            return;
        }

        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            sender.sendMessage(Chat.colorize(getMessage("player_not_found").replace("#player#", args[2])));
            return;
        }

        String itemName = args[3];
        if (!MythicStorageManager.isConfiguredDrop(itemName)) {
            sender.sendMessage(Chat.colorize(getMessage("invalid_item").replace("#item#", itemName)));
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[4]);
            if (amount < 0) {
                sender.sendMessage(Chat.colorize(getMessage("admin.invalid_number").replace("#number#", args[4])));
                return;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(Chat.colorize(getMessage("admin.invalid_number").replace("#number#", args[4])));
            return;
        }

        MythicStorageManager.setItemAmount(target, itemName, amount);
        
        String message = getMessage("admin.set_success")
            .replace("#amount#", String.valueOf(amount))
            .replace("#item#", itemName)
            .replace("#player#", target.getName());
        sender.sendMessage(Chat.colorize(message));
    }

    private void handleAdminReset(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Chat.colorize(getPrefix() + " &cUsage: /mythicstorage admin reset <player> [item]"));
            return;
        }

        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            sender.sendMessage(Chat.colorize(getMessage("player_not_found").replace("#player#", args[2])));
            return;
        }

        if (args.length >= 4) {
            String itemName = args[3];
            if (!MythicStorageManager.isConfiguredDrop(itemName)) {
                sender.sendMessage(Chat.colorize(getMessage("invalid_item").replace("#item#", itemName)));
                return;
            }
            
            MythicStorageManager.setItemAmount(target, itemName, 0);
            
            String message = getMessage("admin.reset_item_success")
                .replace("#item#", itemName)
                .replace("#player#", target.getName());
            sender.sendMessage(Chat.colorize(message));
            
            if (target.isOnline()) {
                target.sendMessage(Chat.colorize(getMessage("admin.reset_notify")
                    .replace("#player#", sender.getName())));
            }
        } else {
            for (String itemName : MythicStorageManager.getConfiguredDrops()) {
                MythicStorageManager.setItemAmount(target, itemName, 0);
            }
            
            String message = getMessage("admin.reset_all_success")
                .replace("#player#", target.getName());
            sender.sendMessage(Chat.colorize(message));
            
            if (target.isOnline()) {
                target.sendMessage(Chat.colorize(getMessage("admin.reset_notify")
                    .replace("#player#", sender.getName())));
            }
        }
    }

    private void handleAdminReload(CommandSender sender) {
        File.getFileSetting().reload("mythicstorage.yml", "GUI/mythicstorage.yml", "message.yml");
        MythicStorageManager.reloadConfiguredDrops();
        sender.sendMessage(Chat.colorize(getMessage("admin.reload_success")));
    }

    private void handleHelp(CommandSender sender) {
        sender.sendMessage(Chat.colorize(getMessage("help_header")));
        sender.sendMessage(Chat.colorize(getMessage("help_title")));
        sender.sendMessage(Chat.colorize(getMessage("help_main")));
        sender.sendMessage(Chat.colorize(getMessage("help_toggle")));
        
        if (sender.hasPermission("storage.mythicstorage.view")) {
            sender.sendMessage(Chat.colorize(getMessage("help_view")));
        }
        
        if (sender.hasPermission("storage.admin")) {
            sender.sendMessage(Chat.colorize(getMessage("help_admin_add")));
            sender.sendMessage(Chat.colorize(getMessage("help_admin_remove")));
            sender.sendMessage(Chat.colorize(getMessage("help_admin_set")));
            sender.sendMessage(Chat.colorize(getMessage("help_admin_reset")));
            sender.sendMessage(Chat.colorize(getMessage("help_admin_reload")));
        }
        
        sender.sendMessage(Chat.colorize(getMessage("help_help")));
        sender.sendMessage(Chat.colorize(getMessage("help_footer")));
    }

    public List<String> getTabCompletions(@NotNull CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("toggle");
            completions.add("view");
            completions.add("help");
            if (sender.hasPermission("storage.admin")) {
                completions.add("admin");
            }
            return filterCompletions(completions, args[0]);
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("view")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    completions.add(player.getName());
                }
                return filterCompletions(completions, args[1]);
            }
            
            if (args[0].equalsIgnoreCase("admin") && sender.hasPermission("storage.admin")) {
                completions.add("add");
                completions.add("remove");
                completions.add("set");
                completions.add("reset");
                completions.add("reload");
                return filterCompletions(completions, args[1]);
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("admin")) {
            if (args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("remove") || 
                args[1].equalsIgnoreCase("set") || args[1].equalsIgnoreCase("reset")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    completions.add(player.getName());
                }
                return filterCompletions(completions, args[2]);
            }
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("admin")) {
            if (args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("remove") || 
                args[1].equalsIgnoreCase("set") || args[1].equalsIgnoreCase("reset")) {
                completions.addAll(MythicStorageManager.getConfiguredDrops());
                return filterCompletions(completions, args[3]);
            }
        }

        return completions;
    }

    private List<String> filterCompletions(List<String> completions, String input) {
        List<String> filtered = new ArrayList<>();
        String lowerInput = input.toLowerCase();
        
        for (String completion : completions) {
            if (completion.toLowerCase().startsWith(lowerInput)) {
                filtered.add(completion);
            }
        }
        
        return filtered;
    }

    private String getMessage(String path) {
        String message = File.getMessage().getString("mythicstorage." + path, "");
        return message.replace("#prefix#", getPrefix());
    }

    private String getPrefix() {
        return File.getMessage().getString("prefix", "&7[&6Storage&7]");
    }
}
