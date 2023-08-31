package net.danh.storage.CMD;

import net.danh.storage.API.CMDBase;
import net.danh.storage.GUI.PersonalStorage;
import net.danh.storage.Manager.GameManager.ChatManager;
import net.danh.storage.Manager.GameManager.MineManager;
import net.danh.storage.Manager.UtilsManager.FileManager;
import net.danh.storage.Utils.Number;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class StorageCMD extends CMDBase {
    public StorageCMD(String name) {
        super(name);
    }

    @Override
    public void execute(@NotNull CommandSender c, String[] args) {
        if (args.length == 0) {
            if (c instanceof Player) {
                ((Player) c).openInventory(new PersonalStorage((Player) c).getInventory());
            }
        }
        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("help")) {
                if (c.hasPermission("storage.admin")) {
                    FileManager.getMessage().getStringList("admin.help").forEach(s -> c.sendMessage(ChatManager.colorize(s)));
                }
            }
        }
        if (c.hasPermission("storage.admin")) {
            if (args.length == 1) {
                if (args[0].equalsIgnoreCase("reload")) {
                    FileManager.getFileSetting().reload("config.yml", "message.yml");
                    c.sendMessage(ChatManager.colorize(FileManager.getMessage().getString("admin.reload")));
                }
            }
        }
        if (c.hasPermission("storage.admin")) {
            if (args.length == 3) {
                if (args[0].equalsIgnoreCase("max")) {
                    Player p = Bukkit.getPlayer(args[1]);
                    if (p != null) {
                        int amount = Number.getInteger(args[2]);
                        if (amount > 0 && amount >= FileManager.getConfig().getInt("settings.default_max_storage")) {
                            MineManager.playermaxdata.put(p, amount);
                            c.sendMessage(ChatManager.colorize(Objects.requireNonNull(FileManager.getMessage().getString("admin.set_max_storage")).replace("#player#", p.getName()).replace("#amount#", String.valueOf(amount))));
                        }
                    }
                }
            }
        }
        if (c.hasPermission("storage.admin")) {
            if (args.length == 4) {
                if (MineManager.getPluginBlocks().contains(args[1])) {
                    Player p = Bukkit.getPlayer(args[2]);
                    if (p != null) {
                        int number = Number.getInteger(args[3]);
                        if (number > 0) {
                            if (args[0].equalsIgnoreCase("add")) {
                                if (MineManager.addBlockAmount(p, args[1], number)) {
                                    c.sendMessage(ChatManager.colorize(FileManager.getMessage().getString("admin.add_material_amount")).replace("#amount#", args[3]).replace("#material#", args[1]).replace("#player#", p.getName()));
                                    p.sendMessage(ChatManager.colorize(FileManager.getMessage().getString("user.add_material_amount")).replace("#amount#", args[3]).replace("#material#", args[1]).replace("#player#", c.getName()));
                                }
                            }
                            if (args[0].equalsIgnoreCase("remove")) {
                                if (MineManager.removeBlockAmount(p, args[1], number)) {
                                    c.sendMessage(ChatManager.colorize(FileManager.getMessage().getString("admin.remove_material_amount")).replace("#amount#", args[3]).replace("#material#", args[1]).replace("#player#", p.getName()));
                                    p.sendMessage(ChatManager.colorize(FileManager.getMessage().getString("user.remove_material_amount")).replace("#amount#", args[3]).replace("#material#", args[1]).replace("#player#", c.getName()));
                                }
                            }
                            if (args[0].equalsIgnoreCase("set")) {
                                MineManager.setBlock(p, args[1], number);
                                c.sendMessage(ChatManager.colorize(FileManager.getMessage().getString("admin.remove_material_amount")).replace("#amount#", args[3]).replace("#material#", args[1]).replace("#player#", p.getName()));
                                p.sendMessage(ChatManager.colorize(FileManager.getMessage().getString("user.remove_material_amount")).replace("#amount#", args[3]).replace("#material#", args[1]).replace("#player#", c.getName()));
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public List<String> TabComplete(@NotNull CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        List<String> commands = new ArrayList<>();
        if (args.length == 1) {
            commands.add("help");
        }
        if (sender.hasPermission("storage.admin")) {
            if (args.length == 1) {
                commands.add("add");
                commands.add("remove");
                commands.add("set");
                commands.add("reload");
                commands.add("max");
                commands.add("help");
                StringUtil.copyPartialMatches(args[0], commands, completions);
            }
            if (args.length == 2) {
                if (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("set")) {
                    if (commands.addAll(MineManager.getPluginBlocks())) {
                        StringUtil.copyPartialMatches(args[1], commands, completions);
                    }
                }
                if (args[0].equalsIgnoreCase("max")) {
                    Bukkit.getServer().getOnlinePlayers().forEach(player -> commands.add(player.getName()));
                    StringUtil.copyPartialMatches(args[1], commands, completions);
                }
            }
            if (args.length == 3) {
                if (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("set")) {
                    if (MineManager.getPluginBlocks().contains(args[1])) {
                        Bukkit.getServer().getOnlinePlayers().forEach(player -> commands.add(player.getName()));
                        StringUtil.copyPartialMatches(args[2], commands, completions);
                    }
                }
                if (args[0].equalsIgnoreCase("max")) {
                    StringUtil.copyPartialMatches(args[2], Collections.singleton("<number>"), completions);
                }
            }
            if (args.length == 4) {
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
