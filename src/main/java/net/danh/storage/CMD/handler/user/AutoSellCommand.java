package net.danh.storage.CMD.handler.user;

import net.danh.storage.CMD.handler.BaseCommand;
import net.danh.storage.Manager.MineManager;
import net.danh.storage.Utils.ChatUtils;
import net.danh.storage.Utils.File;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.*;
import java.util.stream.Collectors;

public class AutoSellCommand extends BaseCommand {

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!requirePlayer(sender)) {
            return;
        }

        Player player = (Player) sender;

        if (isStorageWorldBlacklisted(player)) {
            sendWorldBlacklisted(sender, "Storage", player.getWorld().getName());
            return;
        }

        if (!MineManager.isAutoSellSystemEnabled()) {
            String msg = File.getMessage().getString("user.autosell.system_disabled", "");
            if (msg != null && !msg.trim().isEmpty()) {
                player.sendMessage(ChatUtils.colorize(msg));
            }
            return;
        }

        if (args.length < 1) {
            sendUsage(sender);
            return;
        }

        String sub = args[0].toLowerCase();

        if ("clear".equals(sub)) {
            int cleared = MineManager.clearAutoSell(player);
            String msg = File.getMessage().getString(
                    "user.autosell.cleared",
                    "#prefix# &aCleared &e#count# &aAutoSell items."
            );
            player.sendMessage(ChatUtils.colorize(msg.replace("#count#", String.valueOf(cleared))));
            return;
        }

        if ("list".equals(sub)) {
            Set<String> enabled = MineManager.getEnabledAutoSellItems(player);
            if (enabled.isEmpty()) {
                String msg = File.getMessage().getString("user.autosell.list_empty", "");
                if (msg != null && !msg.trim().isEmpty()) {
                    player.sendMessage(ChatUtils.colorize(msg));
                }
                return;
            }

            String joined = enabled.stream()
                    .map(key -> {
                        String raw = File.getConfig().getString("items." + key, key.split(";")[0]);
                        return ChatUtils.colorize(raw);
                    })
                    .collect(Collectors.joining(ChatUtils.colorize("&7, ")));
            String msg = File.getMessage().getString("user.autosell.list", "#prefix# &aAutoSell items: &f#items#");
            player.sendMessage(ChatUtils.colorize(msg.replace("#items#", joined)));
            return;
        }

        if (args.length < 2) {
            sendUsage(sender);
            return;
        }

        String material = resolveMaterial(args[1]);
        if (!MineManager.getPluginBlocks().contains(material)) {
            sendInvalidMaterial(sender, material, MineManager.getPluginBlocks());
            return;
        }

        String display = File.getConfig().getString("items." + material, material.split(";")[0]);

        if ("toggle".equals(sub)) {
            boolean enabled = MineManager.toggleItemAutoSell(player, material);
            String key = enabled ? "user.autosell.toggle_on" : "user.autosell.toggle_off";
            String msg = File.getMessage().getString(key, "");
            if (msg != null && !msg.trim().isEmpty()) {
                player.sendMessage(ChatUtils.colorize(msg.replace("#material#", display)));
            }
            return;
        }

        if ("status".equals(sub)) {
            boolean enabled = MineManager.isAutoSellEnabledForItem(player, material);
            String symbol = File.getMessage().getString(enabled ? "user.autosell.yes" : "user.autosell.no", enabled ? "&a✔" : "&c✘");
            String msg = File.getMessage().getString("user.autosell.status", "#prefix# &aAuto Sell for &e#material#&a: #status#");
            player.sendMessage(ChatUtils.colorize(msg.replace("#material#", display).replace("#status#", symbol)));
            return;
        }

        if ("set".equals(sub)) {
            if (args.length < 3) {
                sendUsage(sender);
                return;
            }

            Boolean desired = parseOnOff(args[2]);
            if (desired == null) {
                sendUsage(sender);
                return;
            }

            boolean enabled = MineManager.setItemAutoSell(player, material, desired);
            String key = enabled ? "user.autosell.toggle_on" : "user.autosell.toggle_off";
            String msg = File.getMessage().getString(key, "");
            if (msg != null && !msg.trim().isEmpty()) {
                player.sendMessage(ChatUtils.colorize(msg.replace("#material#", display)));
            }
            return;
        }

        sendUsage(sender);
    }

    private String resolveMaterial(String raw) {
        if (raw == null) {
            return ";0";
        }
        String material = raw.replace(":", ";");
        if (!material.contains(";")) {
            return material + ";0";
        }
        return material;
    }

    private Boolean parseOnOff(String raw) {
        if (raw == null) {
            return null;
        }
        String v = raw.trim().toLowerCase();
        if ("on".equals(v) || "true".equals(v) || "enable".equals(v) || "enabled".equals(v)) {
            return true;
        }
        if ("off".equals(v) || "false".equals(v) || "disable".equals(v) || "disabled".equals(v)) {
            return false;
        }
        return null;
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> subs = Arrays.asList("toggle", "set", "status", "list", "clear");
            List<String> out = new ArrayList<>();
            StringUtil.copyPartialMatches(args[0], subs, out);
            Collections.sort(out);
            return out;
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if ("list".equals(sub) || "clear".equals(sub)) {
                return new ArrayList<>();
            }
            List<String> out = new ArrayList<>();
            StringUtil.copyPartialMatches(args[1], MineManager.getPluginBlocks(), out);
            Collections.sort(out);
            return out;
        }

        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if (!"set".equals(sub)) {
                return new ArrayList<>();
            }
            List<String> out = new ArrayList<>();
            StringUtil.copyPartialMatches(args[2], Arrays.asList("on", "off"), out);
            Collections.sort(out);
            return out;
        }

        return new ArrayList<>();
    }

    @Override
    public String getPermission() {
        return "storage.autosell";
    }

    @Override
    public String getUsage() {
        return "/storage autosell <toggle|set|status|list|clear> [item] [on|off]";
    }

    @Override
    public String getDescription() {
        return "Manage per-item AutoSell for Storage";
    }
}
