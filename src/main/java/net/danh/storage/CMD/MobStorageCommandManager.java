package net.danh.storage.CMD;

import net.danh.storage.Action.MobDeposit;
import net.danh.storage.Action.MobSell;
import net.danh.storage.Action.MobWithdraw;
import net.danh.storage.CMD.handler.BaseCommand;
import net.danh.storage.GUI.Mob.MobStorageGUI;
import net.danh.storage.Manager.Mob.MobStorageManager;
import net.danh.storage.Utils.ChatUtils;
import net.danh.storage.Utils.File;
import net.danh.storage.Utils.Number;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;

import java.util.*;
import java.util.stream.Collectors;

public class MobStorageCommandManager extends BaseCommand {

    private static final List<String> USER_COMMANDS = Arrays.asList("help", "toggle", "view", "deposit", "withdraw", "sell", "autosell", "itemtoggle", "groundstore");
    private static final List<String> ADMIN_COMMANDS = Arrays.asList("add", "remove", "set", "max", "reload");

    public void handleCommand(CommandSender sender, String[] args) {
        if (!MobStorageManager.isSystemEnabled()) {
            sendMessage(sender, "mobstorage.system_disabled");
            return;
        }

        if (args.length == 0) {
            openGUI(sender);
            return;
        }

        String commandName = args[0].toLowerCase(Locale.ENGLISH);
        if ("admin".equals(commandName)) {
            handleAdmin(sender, Arrays.copyOfRange(args, 1, args.length));
            return;
        }

        if ("help".equals(commandName)) {
            sendMessageList(sender, "mobstorage.user.help");
        } else if ("toggle".equals(commandName)) {
            toggle(sender);
        } else if ("groundstore".equals(commandName)) {
            groundStore(sender);
        } else if ("itemtoggle".equals(commandName)) {
            itemToggle(sender, args);
        } else if ("deposit".equals(commandName)) {
            deposit(sender, args);
        } else if ("withdraw".equals(commandName)) {
            withdraw(sender, args);
        } else if ("sell".equals(commandName)) {
            sell(sender, args);
        } else if ("autosell".equals(commandName)) {
            autoSell(sender, args);
        } else if ("view".equals(commandName)) {
            view(sender, args);
        } else {
            sendMessage(sender, "mobstorage.admin.unknown_command", "#command#", commandName);
        }
    }

    private void openGUI(CommandSender sender) {
        if (!requirePlayer(sender)) {
            return;
        }
        Player player = (Player) sender;
        if (!sender.hasPermission("storage.mobstorage.use")) {
            sendMessage(sender, "mobstorage.admin.no_permission");
            return;
        }
        if (isMobStorageWorldBlacklisted(player)) {
            sendWorldBlacklisted(sender, "MobStorage", player.getWorld().getName());
            return;
        }
        player.openInventory(new MobStorageGUI(player).getInventory());
    }

    private void toggle(CommandSender sender) {
        if (!requirePlayer(sender)) {
            return;
        }
        if (!sender.hasPermission("storage.mobstorage.toggle")) {
            sendMessage(sender, "mobstorage.admin.no_permission");
            return;
        }
        Player player = (Player) sender;
        if (isMobStorageWorldBlacklisted(player)) {
            sendWorldBlacklisted(sender, "MobStorage", player.getWorld().getName());
            return;
        }
        boolean current = MobStorageManager.getToggleStatus(player);
        boolean applied = MobStorageManager.setToggleStatus(player, !current, true);
        if (applied != current) {
            sendMessage(sender, applied ? "mobstorage.toggle_enabled" : "mobstorage.toggle_disabled");
        }
    }

    private void groundStore(CommandSender sender) {
        if (!requirePlayer(sender)) {
            return;
        }
        if (!sender.hasPermission("storage.mobstorage.groundstore")) {
            sendMessage(sender, "mobstorage.ground_store_no_permission");
            return;
        }
        if (!MobStorageManager.isGroundStoreSystemEnabled()) {
            sendMessage(sender, "mobstorage.ground_store_system_disabled");
            return;
        }
        Player player = (Player) sender;
        if (isMobStorageWorldBlacklisted(player)) {
            sendWorldBlacklisted(sender, "MobStorage", player.getWorld().getName());
            return;
        }
        boolean enabled = MobStorageManager.toggleGroundStore(player, true);
        sendMessage(sender, enabled ? "mobstorage.ground_store_toggle_on" : "mobstorage.ground_store_toggle_off");
    }

    private void itemToggle(CommandSender sender, String[] args) {
        if (!requirePlayer(sender)) {
            return;
        }
        if (!sender.hasPermission("storage.mobstorage.toggle")) {
            sendMessage(sender, "mobstorage.admin.no_permission");
            return;
        }
        if (args.length != 2) {
            sendMessage(sender, "mobstorage.admin.invalid_usage", "#usage#", "/mobstorage itemtoggle <item>");
            return;
        }
        String itemName = args[1].toUpperCase(Locale.ENGLISH);
        if (!MobStorageManager.isConfiguredDrop(itemName)) {
            sendMessage(sender, "mobstorage.invalid_item", "#item#", itemName);
            return;
        }
        boolean enabled = MobStorageManager.toggleItemAutoPickup((Player) sender, itemName, true);
        String[] placeholders = {"#item#", "#status#"};
        String[] replacements = {MobStorageManager.getItemDisplayName(itemName), enabled ? File.getMessage().getString("mobstorage.status_enabled", "&aEnabled") : File.getMessage().getString("mobstorage.status_disabled", "&cDisabled")};
        sendMessage(sender, "mobstorage.item_toggle", placeholders, replacements);
    }

    private void deposit(CommandSender sender, String[] args) {
        if (!requirePlayer(sender)) {
            return;
        }
        if (!sender.hasPermission("storage.mobstorage.deposit")) {
            sendMessage(sender, "mobstorage.admin.no_permission");
            return;
        }
        if (args.length < 2 || args.length > 3) {
            sendMessage(sender, "mobstorage.admin.invalid_usage", "#usage#", "/mobstorage deposit <item> [amount|all]");
            return;
        }
        Player player = (Player) sender;
        String itemName = args[1].toUpperCase(Locale.ENGLISH);
        if (!MobStorageManager.isConfiguredDrop(itemName)) {
            sendMessage(sender, "mobstorage.invalid_item", "#item#", itemName);
            return;
        }
        long amount = args.length == 2 || "all".equalsIgnoreCase(args[2]) ? countEligibleItems(player, itemName) : Number.getLong(args[2]);
        if (amount <= 0) {
            sendMessage(sender, "mobstorage.action.deposit.no_items");
            return;
        }
        new MobDeposit(player, itemName, amount).doAction();
    }

    private void withdraw(CommandSender sender, String[] args) {
        if (!requirePlayer(sender)) {
            return;
        }
        if (!sender.hasPermission("storage.mobstorage.withdraw")) {
            sendMessage(sender, "mobstorage.admin.no_permission");
            return;
        }
        if (args.length < 2 || args.length > 3) {
            sendMessage(sender, "mobstorage.admin.invalid_usage", "#usage#", "/mobstorage withdraw <item> [amount|all]");
            return;
        }
        Player player = (Player) sender;
        String itemName = args[1].toUpperCase(Locale.ENGLISH);
        if (!MobStorageManager.isConfiguredDrop(itemName)) {
            sendMessage(sender, "mobstorage.invalid_item", "#item#", itemName);
            return;
        }
        long amount = args.length == 2 || "all".equalsIgnoreCase(args[2]) ? MobStorageManager.getPlayerItem(player, itemName) : Number.getLong(args[2]);
        if (amount <= 0) {
            sendMessage(sender, "mobstorage.no_items");
            return;
        }
        new MobWithdraw(player, itemName, amount).doAction();
    }

    private void sell(CommandSender sender, String[] args) {
        if (!requirePlayer(sender)) {
            return;
        }
        if (!sender.hasPermission("storage.mobstorage.sell")) {
            sendMessage(sender, "mobstorage.admin.no_permission");
            return;
        }
        if (args.length < 2 || args.length > 3) {
            sendMessage(sender, "mobstorage.admin.invalid_usage", "#usage#", "/mobstorage sell <item> [amount|all]");
            return;
        }
        Player player = (Player) sender;
        if (isMobStorageWorldBlacklisted(player)) {
            sendWorldBlacklisted(sender, "MobStorage", player.getWorld().getName());
            return;
        }
        String itemName = args[1].toUpperCase(Locale.ENGLISH);
        if (!MobStorageManager.isConfiguredDrop(itemName)) {
            sendMessage(sender, "mobstorage.invalid_item", "#item#", itemName);
            return;
        }
        long amount = args.length == 2 || "all".equalsIgnoreCase(args[2]) ? -1 : Number.getLong(args[2]);
        if (args.length == 3 && amount <= 0 && !"all".equalsIgnoreCase(args[2])) {
            sendMessage(sender, "mobstorage.admin.invalid_number", "#number#", args[2]);
            return;
        }
        new MobSell(player, itemName, amount).doAction();
    }

    private void autoSell(CommandSender sender, String[] args) {
        if (!requirePlayer(sender)) {
            return;
        }
        if (!sender.hasPermission("storage.mobstorage.autosell")) {
            sendMessage(sender, "mobstorage.admin.no_permission");
            return;
        }

        Player player = (Player) sender;
        if (isMobStorageWorldBlacklisted(player)) {
            sendWorldBlacklisted(sender, "MobStorage", player.getWorld().getName());
            return;
        }
        if (!MobStorageManager.isAutoSellSystemEnabled()) {
            String msg = File.getMessage().getString("mobstorage.autosell.system_disabled", "");
            if (msg != null && !msg.trim().isEmpty()) {
                player.sendMessage(ChatUtils.colorizewp(msg));
            }
            return;
        }
        if (args.length < 2) {
            sendMessage(sender, "mobstorage.admin.invalid_usage", "#usage#", "/mobstorage autosell <toggle|set|list|clear> [item] [on|off]");
            return;
        }

        String sub = args[1].toLowerCase(Locale.ENGLISH);
        if ("clear".equals(sub)) {
            int cleared = MobStorageManager.clearAutoSell(player);
            player.sendMessage(ChatUtils.colorizewp(File.getMessage().getString(
                            "mobstorage.autosell.cleared",
                            "#prefix# &aCleared &e#count# &aAutoSell mob drops.")
                    .replace("#count#", String.valueOf(cleared))));
            return;
        }
        if ("list".equals(sub)) {
            Set<String> enabled = MobStorageManager.getEnabledAutoSellItems(player);
            if (enabled.isEmpty()) {
                player.sendMessage(ChatUtils.colorizewp(File.getMessage().getString("mobstorage.autosell.list_empty", "")));
                return;
            }
            String joined = enabled.stream()
                    .map(MobStorageManager::getItemDisplayName)
                    .map(ChatUtils::colorizewp)
                    .collect(Collectors.joining(ChatUtils.colorizewp("&7, ")));
            player.sendMessage(ChatUtils.colorizewp(File.getMessage().getString(
                            "mobstorage.autosell.list",
                            "#prefix# &aAutoSell mob drops: &f#items#")
                    .replace("#items#", joined)));
            return;
        }
        if (args.length < 3) {
            sendMessage(sender, "mobstorage.admin.invalid_usage", "#usage#", "/mobstorage autosell <toggle|set> <item> [on|off]");
            return;
        }

        String itemName = args[2].toUpperCase(Locale.ENGLISH);
        if (!MobStorageManager.isConfiguredDrop(itemName)) {
            sendMessage(sender, "mobstorage.invalid_item", "#item#", itemName);
            return;
        }
        if ("toggle".equals(sub)) {
            boolean enabled = MobStorageManager.toggleItemAutoSell(player, itemName);
            sendAutoSellToggleMessage(player, itemName, enabled);
            return;
        }
        if ("set".equals(sub)) {
            if (args.length < 4) {
                sendMessage(sender, "mobstorage.admin.invalid_usage", "#usage#", "/mobstorage autosell set <item> <on|off>");
                return;
            }
            Boolean desired = parseOnOff(args[3]);
            if (desired == null) {
                sendMessage(sender, "mobstorage.admin.invalid_usage", "#usage#", "/mobstorage autosell set <item> <on|off>");
                return;
            }
            boolean enabled = MobStorageManager.setItemAutoSell(player, itemName, desired);
            sendAutoSellToggleMessage(player, itemName, enabled);
            return;
        }
        sendMessage(sender, "mobstorage.admin.invalid_usage", "#usage#", "/mobstorage autosell <toggle|set|list|clear> [item] [on|off]");
    }

    private void sendAutoSellToggleMessage(Player player, String itemName, boolean enabled) {
        String key = enabled ? "mobstorage.autosell.toggle_on" : "mobstorage.autosell.toggle_off";
        String msg = File.getMessage().getString(key, "");
        if (msg != null && !msg.trim().isEmpty()) {
            player.sendMessage(ChatUtils.colorizewp(msg.replace("#material#", MobStorageManager.getItemDisplayName(itemName))));
        }
    }

    private Boolean parseOnOff(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim().toLowerCase(Locale.ENGLISH);
        if ("on".equals(value) || "true".equals(value) || "enable".equals(value) || "enabled".equals(value)) {
            return true;
        }
        if ("off".equals(value) || "false".equals(value) || "disable".equals(value) || "disabled".equals(value)) {
            return false;
        }
        return null;
    }

    private void view(CommandSender sender, String[] args) {
        if (!sender.hasPermission("storage.mobstorage.view")) {
            sendMessage(sender, "mobstorage.admin.no_permission");
            return;
        }
        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sendMessage(sender, "mobstorage.player_not_found", "#player#", args[1]);
                return;
            }
        } else {
            if (!requirePlayer(sender)) {
                return;
            }
            target = (Player) sender;
        }
        sendMessage(sender, "mobstorage.viewing_storage", "#player#", target.getName());
        Map<String, Integer> items = MobStorageManager.getPlayerAllItems(target);
        if (items.isEmpty()) {
            sendMessage(sender, "mobstorage.no_items");
            return;
        }
        for (String itemName : MobStorageManager.getConfiguredDrops()) {
            int amount = items.getOrDefault(itemName, 0);
            if (amount > 0) {
                sender.sendMessage(ChatUtils.colorizewp("&7- &e" + MobStorageManager.getItemDisplayName(itemName) + "&7: &a" + amount));
            }
        }
    }

    private void handleAdmin(CommandSender sender, String[] args) {
        if (args.length == 0 || "help".equalsIgnoreCase(args[0])) {
            sendMessageList(sender, "mobstorage.admin.help");
            return;
        }
        String action = args[0].toLowerCase(Locale.ENGLISH);
        if (!sender.hasPermission("storage.mobstorage.admin." + action) && !sender.hasPermission("storage.mobstorage.admin")) {
            sendMessage(sender, "mobstorage.admin.no_permission");
            return;
        }
        if ("reload".equals(action)) {
            File.getFileSetting().reload("mobstorage.yml");
            File.updateMobStorageConfig();
            MobStorageManager.reloadConfiguredDrops();
            reloadOnlinePlayers();
            sendMessage(sender, "mobstorage.admin.reload_success");
            return;
        }
        if ("max".equals(action)) {
            adminMax(sender, args);
            return;
        }
        if ("add".equals(action) || "remove".equals(action) || "set".equals(action)) {
            adminAmount(sender, args, action);
            return;
        }
        sendMessage(sender, "mobstorage.admin.unknown_command", "#command#", action);
    }

    private void adminMax(CommandSender sender, String[] args) {
        if (args.length != 3) {
            sendMessage(sender, "mobstorage.admin.invalid_usage", "#usage#", "/mobstorage admin max <player> <amount>");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sendMessage(sender, "mobstorage.player_not_found", "#player#", args[1]);
            return;
        }
        int amount = Number.getInteger(args[2]);
        if (amount < 0) {
            sendMessage(sender, "mobstorage.admin.invalid_number", "#number#", args[2]);
            return;
        }
        MobStorageManager.setMaxStorageOverride(target, amount);
        MobStorageManager.playermaxdata.put(target.getUniqueId(), amount);
        MobStorageManager.savePlayerData(target);
        sendMessage(sender, "mobstorage.admin.set_max_storage", new String[]{"#player#", "#amount#"}, new String[]{target.getName(), String.valueOf(amount)});
    }

    private void adminAmount(CommandSender sender, String[] args, String action) {
        if (args.length != 4) {
            sendMessage(sender, "mobstorage.admin.invalid_usage", "#usage#", "/mobstorage admin " + action + " <player> <item> <amount>");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sendMessage(sender, "mobstorage.player_not_found", "#player#", args[1]);
            return;
        }
        String itemName = args[2].toUpperCase(Locale.ENGLISH);
        if (!MobStorageManager.isConfiguredDrop(itemName)) {
            sendMessage(sender, "mobstorage.invalid_item", "#item#", itemName);
            return;
        }
        int amount = Number.getInteger(args[3]);
        if (amount <= 0) {
            sendMessage(sender, "mobstorage.admin.invalid_number", "#number#", args[3]);
            return;
        }
        boolean success = true;
        if ("add".equals(action)) {
            success = MobStorageManager.addItemAmount(target, itemName, amount, false);
        } else if ("remove".equals(action)) {
            success = MobStorageManager.removeItemAmount(target, itemName, amount, false);
        } else {
            MobStorageManager.setItemAmount(target, itemName, amount);
        }
        if (!success) {
            String messageKey = "add".equals(action) ? "mobstorage.admin.storage_full" : "mobstorage.admin.not_enough_items";
            sendMessage(sender, messageKey,
                    new String[]{"#player#", "#item#", "#amount#"},
                    new String[]{target.getName(), MobStorageManager.getItemDisplayName(itemName), String.valueOf(amount)});
            return;
        }
        MobStorageManager.savePlayerData(target);
        sendMessage(sender, "mobstorage.admin." + action + "_success",
                new String[]{"#player#", "#item#", "#amount#"},
                new String[]{target.getName(), MobStorageManager.getItemDisplayName(itemName), String.valueOf(amount)});
    }

    private long countEligibleItems(Player player, String itemName) {
        Material material = Material.getMaterial(itemName.toUpperCase(Locale.ENGLISH));
        if (material == null) {
            return 0;
        }
        long count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() != material) {
                continue;
            }
            if (!File.getMobStorageConfig().getBoolean("settings.allow_custom_item_meta", false)
                    && item.hasItemMeta()) {
                continue;
            }
            count += item.getAmount();
        }
        return count;
    }

    private boolean isMobStorageWorldBlacklisted(Player player) {
        return player != null && File.getMobStorageConfig().getStringList("blacklist_world").contains(player.getWorld().getName());
    }

    private void reloadOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            MobStorageManager.savePlayerData(player);
            MobStorageManager.loadPlayerData(player);
        }
    }

    public List<String> getCommandTabCompletions(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            List<String> options = new ArrayList<>(USER_COMMANDS);
            options.add("admin");
            StringUtil.copyPartialMatches(args[0], options, completions);
        } else if (args.length == 2 && "admin".equalsIgnoreCase(args[0])) {
            StringUtil.copyPartialMatches(args[1], ADMIN_COMMANDS, completions);
        } else if (args.length == 2 && ("deposit".equalsIgnoreCase(args[0]) || "withdraw".equalsIgnoreCase(args[0]) || "sell".equalsIgnoreCase(args[0]) || "itemtoggle".equalsIgnoreCase(args[0]))) {
            StringUtil.copyPartialMatches(args[1], MobStorageManager.getConfiguredDrops(), completions);
        } else if (args.length == 2 && "autosell".equalsIgnoreCase(args[0])) {
            StringUtil.copyPartialMatches(args[1], Arrays.asList("toggle", "set", "list", "clear"), completions);
        } else if (args.length == 3 && "autosell".equalsIgnoreCase(args[0]) && !("list".equalsIgnoreCase(args[1]) || "clear".equalsIgnoreCase(args[1]))) {
            StringUtil.copyPartialMatches(args[2], MobStorageManager.getConfiguredDrops(), completions);
        } else if (args.length == 4 && "autosell".equalsIgnoreCase(args[0]) && "set".equalsIgnoreCase(args[1])) {
            StringUtil.copyPartialMatches(args[3], Arrays.asList("on", "off"), completions);
        } else if (args.length == 2 && "view".equalsIgnoreCase(args[0])) {
            StringUtil.copyPartialMatches(args[1], getOnlinePlayerNames(), completions);
        } else if (args.length == 3 && ("deposit".equalsIgnoreCase(args[0]) || "withdraw".equalsIgnoreCase(args[0]) || "sell".equalsIgnoreCase(args[0]))) {
            StringUtil.copyPartialMatches(args[2], Arrays.asList("all", "1", "10", "64", "100"), completions);
        } else if (args.length == 3 && "admin".equalsIgnoreCase(args[0]) && ADMIN_COMMANDS.contains(args[1].toLowerCase(Locale.ENGLISH))) {
            StringUtil.copyPartialMatches(args[2], getOnlinePlayerNames(), completions);
        } else if (args.length == 4 && "admin".equalsIgnoreCase(args[0]) && !"max".equalsIgnoreCase(args[1])) {
            StringUtil.copyPartialMatches(args[3], MobStorageManager.getConfiguredDrops(), completions);
        }
        Collections.sort(completions);
        return completions;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        handleCommand(sender, args);
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        return getCommandTabCompletions(sender, args);
    }

    @Override
    public String getPermission() {
        return null;
    }

    @Override
    public String getUsage() {
        return "/mobstorage [command]";
    }

    @Override
    public String getDescription() {
        return "MobStorage command manager";
    }
}
