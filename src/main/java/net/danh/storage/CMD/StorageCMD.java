package net.danh.storage.CMD;

import net.danh.storage.API.CMDBase;
import net.danh.storage.Event.EventType;
import net.danh.storage.GUI.PersonalStorage;
import net.danh.storage.GUI.TransferGUI;
import net.danh.storage.GUI.TransferMultiGUI;
import net.danh.storage.Manager.*;
import net.danh.storage.Storage;
import net.danh.storage.Utils.Chat;
import net.danh.storage.Utils.File;
import net.danh.storage.Utils.Number;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class StorageCMD extends CMDBase {
    public StorageCMD(String name) {
        super(name);
    }

    @Override
    public void execute(@NotNull CommandSender c, String[] args) {
        if (args.length == 0) {
            if (c instanceof Player) {
                try {
                    Player player = (Player) c;
                    int currentPage = PersonalStorage.getPlayerCurrentPage(player);
                    player.openInventory(new PersonalStorage(player, currentPage).getInventory());
                } catch (IndexOutOfBoundsException e) {
                    c.sendMessage(Chat.colorize(File.getMessage().getString("admin.not_enough_slot")));
                }
            }
        }
        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("help")) {
                if (c.hasPermission("storage.admin")) {
                    File.getMessage().getStringList("admin.help").forEach(s -> c.sendMessage(Chat.colorize(s)));
                }
                File.getMessage().getStringList("user.help").forEach(s -> c.sendMessage(Chat.colorize(s)));
            }
            if (args[0].equalsIgnoreCase("toggle")) {
                if (c instanceof Player) {
                    Player p = (Player) c;
                    if (p.hasPermission("storage.toggle")) {
                        MineManager.toggle.replace(p, !MineManager.toggle.get(p));
                        p.sendMessage(Chat.colorize(Objects.requireNonNull(File.getMessage().getString("user.status.toggle")).replace("#status#", ItemManager.getStatus(p))));
                    }
                }
            }
        }
        if (c.hasPermission("storage.admin") || c.hasPermission("storage.admin.reload")) {
            if (args.length == 1) {
                if (args[0].equalsIgnoreCase("reload")) {
                    File.reloadFiles();
                    MineManager.loadBlocks();
                    AutoSaveManager.restartAutoSave();
                    EventManager.reloadEvents();
                    for (Player p : Storage.getStorage().getServer().getOnlinePlayers()) {
                        MineManager.convertOfflineData(p);
                        MineManager.loadPlayerData(p);
                    }
                    c.sendMessage(Chat.colorize(File.getMessage().getString("admin.reload")));
                }
                if (args[0].equalsIgnoreCase("autosave")) {
                    String statusOn = File.getMessage().getString("user.status.status_on");
                    String statusOff = File.getMessage().getString("user.status.status_off");

                    c.sendMessage(Chat.colorize(File.getMessage().getString("admin.autosave.status_header")));
                    c.sendMessage(Chat.colorize(File.getMessage().getString("admin.autosave.enabled")
                            .replace("#status#", AutoSaveManager.isEnabled() ? statusOn : statusOff)));
                    c.sendMessage(Chat.colorize(File.getMessage().getString("admin.autosave.running")
                            .replace("#status#", AutoSaveManager.isRunning() ? statusOn : statusOff)));
                    c.sendMessage(Chat.colorize(File.getMessage().getString("admin.autosave.interval")
                            .replace("#minutes#", String.valueOf(AutoSaveManager.getIntervalMinutes()))));
                    c.sendMessage(Chat.colorize(File.getMessage().getString("admin.autosave.async")
                            .replace("#status#", AutoSaveManager.isAsync() ? statusOn : statusOff)));
                    c.sendMessage(Chat.colorize(File.getMessage().getString("admin.autosave.log_activity")
                            .replace("#status#", AutoSaveManager.isLogActivity() ? statusOn : statusOff)));
                }
                if (args[0].equalsIgnoreCase("save")) {
                    AutoSaveManager.forceSave();
                    c.sendMessage(Chat.colorize(File.getMessage().getString("admin.autosave.force_save_completed")));
                }
                if (args[0].equalsIgnoreCase("event")) {
                    c.sendMessage(Chat.colorizewp(File.getMessage().getString("events.status.header")));
                    for (EventType eventType : EventType.values()) {
                        String eventName = File.getEventConfig().getString("events." + eventType.getConfigKey() + ".event_name", eventType.getDisplayName());
                        String status = EventManager.getEventStatus(eventType);
                        String statusMessage = File.getMessage().getString("events.status.line")
                                .replace("#event#", eventName)
                                .replace("#status#", status);
                        c.sendMessage(Chat.colorizewp(statusMessage));
                    }
                    c.sendMessage(Chat.colorizewp(File.getMessage().getString("events.status.footer")));
                }
            }
            if (args.length == 2) {
                if (args[0].equalsIgnoreCase("event")) {
                    if (args[1].equalsIgnoreCase("help")) {
                        File.getMessage().getStringList("events.help").forEach(s -> c.sendMessage(Chat.colorizewp(s)));
                    }
                    if (args[1].equalsIgnoreCase("list")) {
                        c.sendMessage(Chat.colorizewp(File.getMessage().getString("events.list.header")));
                        boolean hasActiveEvents = false;
                        for (EventType eventType : EventType.values()) {
                            if (EventManager.isEventActive(eventType)) {
                                String eventName = File.getEventConfig().getString("events." + eventType.getConfigKey() + ".event_name", eventType.getDisplayName());
                                String activeMessage = File.getMessage().getString("events.list.active")
                                        .replace("#event#", eventName);
                                c.sendMessage(Chat.colorizewp(activeMessage));
                                hasActiveEvents = true;
                            }
                        }
                        if (!hasActiveEvents) {
                            c.sendMessage(Chat.colorizewp(File.getMessage().getString("events.list.no_active")));
                        }
                    }

                }
            }
            if (args.length == 3) {
                if (args[0].equalsIgnoreCase("event")) {
                    EventType eventType = EventType.fromConfigKey(args[2]);
                    if (eventType == null) {
                        c.sendMessage(Chat.colorizewp(File.getMessage().getString("events.invalid_type")));
                        return;
                    }

                    if (args[1].equalsIgnoreCase("start")) {
                        String eventName = File.getEventConfig().getString("events." + eventType.getConfigKey() + ".event_name", eventType.getDisplayName());
                        if (EventManager.startEvent(eventType)) {
                            String startMessage = File.getMessage().getString("events.start.success")
                                    .replace("#event#", eventName);
                            c.sendMessage(Chat.colorizewp(startMessage));
                        } else {
                            String failMessage = File.getMessage().getString("events.start.failed")
                                    .replace("#event#", eventName);
                            c.sendMessage(Chat.colorizewp(failMessage));
                        }
                    }
                    if (args[1].equalsIgnoreCase("stop")) {
                        String eventName = File.getEventConfig().getString("events." + eventType.getConfigKey() + ".event_name", eventType.getDisplayName());
                        if (EventManager.stopEvent(eventType, false)) {
                            String stopMessage;
                            if (eventType == EventType.MINING_CONTEST) {
                                stopMessage = File.getMessage().getString("events.stop.success_mining_contest")
                                        .replace("#event#", eventName);
                            } else {
                                stopMessage = File.getMessage().getString("events.stop.success")
                                        .replace("#event#", eventName);
                            }
                            c.sendMessage(Chat.colorizewp(stopMessage));
                        } else {
                            String failMessage = File.getMessage().getString("events.stop.failed")
                                    .replace("#event#", eventName);
                            c.sendMessage(Chat.colorizewp(failMessage));
                        }
                    }
                }
            }
        }
        if (c.hasPermission("storage.admin") || c.hasPermission("storage.admin.max")) {
            if (args.length == 3) {
                if (args[0].equalsIgnoreCase("max")) {
                    Player p = Bukkit.getPlayer(args[1]);
                    if (p != null) {
                        int amount = Number.getInteger(args[2]);
                        if (amount > 0) {
                            MineManager.playermaxdata.put(p, amount);
                            c.sendMessage(Chat.colorize(Objects.requireNonNull(File.getMessage().getString("admin.set_max_storage")).replace("#player#", p.getName()).replace("#amount#", String.valueOf(amount))));
                        }
                    }
                }
            }
        }
        if (c.hasPermission("storage.admin") || c.hasPermission("storage.admin.add") || c.hasPermission("storage.admin.remove") || c.hasPermission("storage.admin.set")) {
            if (args.length == 4) {
                if (MineManager.getPluginBlocks().contains(args[1])) {
                    Player p = Bukkit.getPlayer(args[2]);
                    if (p != null) {
                        int number = Number.getInteger(args[3]);
                        if (number >= 0) {
                            if (args[0].equalsIgnoreCase("add")) {
                                if (c.hasPermission("storage.admin") || c.hasPermission("storage.admin.add")) {
                                    if (MineManager.addBlockAmount(p, args[1], number)) {
                                        c.sendMessage(Chat.colorize(File.getMessage().getString("admin.add_material_amount")).replace("#amount#", args[3]).replace("#material#", args[1]).replace("#player#", p.getName()));
                                        p.sendMessage(Chat.colorize(File.getMessage().getString("user.add_material_amount")).replace("#amount#", args[3]).replace("#material#", args[1]).replace("#player#", c.getName()));
                                    }
                                }
                            }
                            if (args[0].equalsIgnoreCase("remove")) {
                                if (c.hasPermission("storage.admin") || c.hasPermission("storage.admin.remove")) {
                                    if (MineManager.removeBlockAmount(p, args[1], number)) {
                                        c.sendMessage(Chat.colorize(File.getMessage().getString("admin.remove_material_amount")).replace("#amount#", args[3]).replace("#material#", args[1]).replace("#player#", p.getName()));
                                        p.sendMessage(Chat.colorize(File.getMessage().getString("user.remove_material_amount")).replace("#amount#", args[3]).replace("#material#", args[1]).replace("#player#", c.getName()));
                                    }
                                }
                            }
                            if (args[0].equalsIgnoreCase("set")) {
                                if (c.hasPermission("storage.admin") || c.hasPermission("storage.admin.set")) {
                                    MineManager.setBlock(p, args[1], number);
                                    c.sendMessage(Chat.colorize(File.getMessage().getString("admin.set_material_amount")).replace("#amount#", args[3]).replace("#material#", args[1]).replace("#player#", p.getName()));
                                    p.sendMessage(Chat.colorize(File.getMessage().getString("user.set_material_amount")).replace("#amount#", args[3]).replace("#material#", args[1]).replace("#player#", c.getName()));
                                }
                            }
                        }
                    }
                }
            }
        }

        // Transfer commands
        if (args.length >= 2 && args[0].equalsIgnoreCase("transfer")) {
            if (!(c instanceof Player)) {
                c.sendMessage(Chat.colorize(File.getMessage().getString("admin.transfer_commands.only_players")));
                return;
            }

            Player player = (Player) c;

            if (args.length == 2 && args[1].equalsIgnoreCase("log")) {
                // /storage transfer log - view own transfer history
                TransferManager.displayTransferHistory(player, null, 1);
                return;
            }

            if (args.length == 3 && args[1].equalsIgnoreCase("log")) {
                // Check if third argument is a page number or player name
                try {
                    int page = Integer.parseInt(args[2]);
                    if (page > 0) {
                        // /storage transfer log <page> - view own transfer history at specific page
                        TransferManager.displayTransferHistory(player, null, page);
                        return;
                    }
                } catch (NumberFormatException ignored) {
                    // Not a number, treat as player name
                }
                // /storage transfer log <player> - view specific player's transfer history
                TransferManager.displayTransferHistory(player, args[2], 1);
                return;
            }

            if (args.length == 4 && args[1].equalsIgnoreCase("log")) {
                // /storage transfer log <player> <page> - view specific player's transfer history at specific page
                try {
                    int page = Integer.parseInt(args[3]);
                    if (page > 0) {
                        TransferManager.displayTransferHistory(player, args[2], page);
                    } else {
                        player.sendMessage(Chat.colorizewp(File.getMessage().getString("admin.transfer_commands.invalid_page_number")));
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(Chat.colorizewp(File.getMessage().getString("admin.transfer_commands.invalid_page_format")));
                }
                return;
            }

            if (args.length == 3 && args[1].equalsIgnoreCase("multi")) {
                // /storage transfer multi <player> - open multi transfer GUI
                if (!player.hasPermission("storage.transfer.multi")) {
                    player.sendMessage(Chat.colorize(File.getMessage().getString("transfer.failed_permission_multi")));
                    return;
                }

                Player targetPlayer = Bukkit.getPlayer(args[2]);
                if (targetPlayer == null || !targetPlayer.isOnline()) {
                    player.sendMessage(Chat.colorize(File.getMessage().getString("transfer.failed_offline").replace("#player#", args[2])));
                    return;
                }

                if (player.getName().equalsIgnoreCase(targetPlayer.getName())) {
                    player.sendMessage(Chat.colorize(File.getMessage().getString("transfer.failed_same_player")));
                    return;
                }

                try {
                    player.openInventory(new TransferMultiGUI(player, targetPlayer.getName()).getInventory());
                } catch (Exception e) {
                    player.sendMessage(Chat.colorize(File.getMessage().getString("admin.transfer_commands.error_opening_gui")));
                }
                return;
            }

            if (args.length >= 3 && !args[1].equalsIgnoreCase("log") && !args[1].equalsIgnoreCase("multi")) {
                // /storage transfer <player> <material> - open single transfer GUI
                if (!player.hasPermission("storage.transfer.use")) {
                    player.sendMessage(Chat.colorize(File.getMessage().getString("transfer.failed_permission")));
                    return;
                }

                Player targetPlayer = Bukkit.getPlayer(args[1]);
                if (targetPlayer == null || !targetPlayer.isOnline()) {
                    player.sendMessage(Chat.colorize(File.getMessage().getString("transfer.failed_offline").replace("#player#", args[1])));
                    return;
                }

                if (player.getName().equalsIgnoreCase(targetPlayer.getName())) {
                    player.sendMessage(Chat.colorize(File.getMessage().getString("transfer.failed_same_player")));
                    return;
                }

                String material = args.length >= 3 ? args[2] : null;
                if (material == null || !MineManager.getPluginBlocks().contains(material)) {
                    player.sendMessage(Chat.colorize(File.getMessage().getString("transfer.failed_invalid_material")));
                    return;
                }

                int currentAmount = MineManager.getPlayerBlock(player, material);
                if (currentAmount <= 0) {
                    player.sendMessage(Chat.colorize(File.getMessage().getString("transfer.failed_insufficient")
                            .replace("#material#", material)
                            .replace("#current#", "0")));
                    return;
                }

                try {
                    player.openInventory(new TransferGUI(player, targetPlayer.getName(), material).getInventory());
                } catch (Exception e) {
                    player.sendMessage(Chat.colorize(File.getMessage().getString("admin.transfer_commands.error_opening_gui")));
                }
            }
        }
    }

    @Override
    public List<String> TabComplete(@NotNull CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        List<String> commands = new ArrayList<>();
        List<String> adminPerms = Arrays.asList("storage.admin.add", "storage.admin.remove", "storage.admin.set", "storage.admin.reload", "storage.admin.max");
        if (args.length == 1) {
            commands.add("help");
            if (sender.hasPermission("storage.toggle")) {
                commands.add("toggle");
            }
            if (sender.hasPermission("storage.transfer.use") || sender.hasPermission("storage.transfer.multi") || sender.hasPermission("storage.transfer.log")) {
                commands.add("transfer");
            }
            if (sender.hasPermission("storage.admin")) {
                commands.add("add");
                commands.add("remove");
                commands.add("set");
                commands.add("reload");
                commands.add("max");
                commands.add("autosave");
                commands.add("save");
                commands.add("event");
            } else {
                if (sender.hasPermission("storage.admin.add")) commands.add("add");
                if (sender.hasPermission("storage.admin.remove")) commands.add("remove");
                if (sender.hasPermission("storage.admin.set")) commands.add("set");
                if (sender.hasPermission("storage.admin.reload")) {
                    commands.add("reload");
                    commands.add("autosave");
                    commands.add("save");
                    commands.add("event");
                }
                if (sender.hasPermission("storage.admin.max")) commands.add("max");
            }

            StringUtil.copyPartialMatches(args[0], commands, completions);
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("transfer")) {
                if (sender.hasPermission("storage.transfer.log")) {
                    commands.add("log");
                }
                if (sender.hasPermission("storage.transfer.multi")) {
                    commands.add("multi");
                }
                if (sender.hasPermission("storage.transfer.use")) {
                    Bukkit.getServer().getOnlinePlayers().forEach(player -> {
                        if (!player.getName().equals(sender.getName())) {
                            commands.add(player.getName());
                        }
                    });
                }
                StringUtil.copyPartialMatches(args[1], commands, completions);
            }
            if (args[0].equalsIgnoreCase("event")) {
                if (sender.hasPermission("storage.admin") || sender.hasPermission("storage.admin.reload")) {
                    commands.add("help");
                    commands.add("list");
                    commands.add("start");
                    commands.add("stop");
                    StringUtil.copyPartialMatches(args[1], commands, completions);
                }
            }
            if (sender.hasPermission("storage.admin") || sender.hasPermission("storage.admin.add") || sender.hasPermission("storage.admin.remove") || sender.hasPermission("storage.admin.set")) {
                if (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("set")) {
                    if (commands.addAll(MineManager.getPluginBlocks())) {
                        StringUtil.copyPartialMatches(args[1], commands, completions);
                    }
                }
            }
            if (sender.hasPermission("storage.admin") || sender.hasPermission("storage.admin.max")) {
                if (args[0].equalsIgnoreCase("max")) {
                    Bukkit.getServer().getOnlinePlayers().forEach(player -> commands.add(player.getName()));
                    StringUtil.copyPartialMatches(args[1], commands, completions);
                }
            }
        }
        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("event")) {
                if ((args[1].equalsIgnoreCase("start") || args[1].equalsIgnoreCase("stop")) &&
                        (sender.hasPermission("storage.admin") || sender.hasPermission("storage.admin.reload"))) {
                    commands.add("mining_contest");
                    commands.add("double_drop");
                    commands.add("community_event");
                    StringUtil.copyPartialMatches(args[2], commands, completions);
                }
            }
            if (args[0].equalsIgnoreCase("transfer")) {
                if (args[1].equalsIgnoreCase("log")) {
                    if (sender.hasPermission("storage.transfer.log.others")) {
                        // Add player names
                        Bukkit.getServer().getOnlinePlayers().forEach(player -> commands.add(player.getName()));
                    }
                    // Add page numbers
                    commands.add("1");
                    commands.add("2");
                    commands.add("3");
                    StringUtil.copyPartialMatches(args[2], commands, completions);
                } else if (args[1].equalsIgnoreCase("multi") && sender.hasPermission("storage.transfer.multi")) {
                    Bukkit.getServer().getOnlinePlayers().forEach(player -> {
                        if (!player.getName().equals(sender.getName())) {
                            commands.add(player.getName());
                        }
                    });
                    StringUtil.copyPartialMatches(args[2], commands, completions);
                } else if (sender.hasPermission("storage.transfer.use") && !args[1].equalsIgnoreCase("log") && !args[1].equalsIgnoreCase("multi")) {
                    // For /storage transfer <player> <material>
                    if (commands.addAll(MineManager.getPluginBlocks())) {
                        StringUtil.copyPartialMatches(args[2], commands, completions);
                    }
                }
            }
            if (sender.hasPermission("storage.admin") || sender.hasPermission("storage.admin.add") || sender.hasPermission("storage.admin.remove") || sender.hasPermission("storage.admin.set")) {
                if (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("set")) {
                    if (MineManager.getPluginBlocks().contains(args[1])) {
                        Bukkit.getServer().getOnlinePlayers().forEach(player -> commands.add(player.getName()));
                        StringUtil.copyPartialMatches(args[2], commands, completions);
                    }
                }
            }
            if (sender.hasPermission("storage.admin") || sender.hasPermission("storage.admin.max")) {
                if (args[0].equalsIgnoreCase("max")) {
                    StringUtil.copyPartialMatches(args[2], Collections.singleton("<number>"), completions);
                }
            }
        }
        if (args.length == 4) {
            if (args[0].equalsIgnoreCase("transfer")) {
                if (args[1].equalsIgnoreCase("log") && sender.hasPermission("storage.transfer.log.others")) {
                    // Add page numbers for /storage transfer log <player> <page>
                    commands.add("1");
                    commands.add("2");
                    commands.add("3");
                    StringUtil.copyPartialMatches(args[3], commands, completions);
                }
            }
            if (sender.hasPermission("storage.admin") || sender.hasPermission("storage.admin.add") || sender.hasPermission("storage.admin.remove") || sender.hasPermission("storage.admin.set")) {
                if (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("set")) {
                    if (MineManager.getPluginBlocks().contains(args[1])) {
                        StringUtil.copyPartialMatches(args[3], Collections.singleton("<number>"), completions);
                    }
                }
            }
        }

        Collections.sort(completions);
        return completions;
    }
}
