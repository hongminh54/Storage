package net.danh.storage.CMD.handler.crop.user;

import net.danh.storage.CMD.handler.crop.CropCommand;
import net.danh.storage.Manager.Crop.CropStorageManager;
import net.danh.storage.Utils.ChatUtils;
import net.danh.storage.Utils.File;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.*;
import java.util.stream.Collectors;

public class CropAutoSellCommand extends CropCommand {

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!requirePlayer(sender)) {
            return;
        }

        Player player = (Player) sender;

        if (isCropStorageWorldBlacklisted(player)) {
            sendWorldBlacklisted(sender, "CropStorage", player.getWorld().getName());
            return;
        }

        if (!CropStorageManager.isSystemEnabled()) {
            sendMessage(sender, "system_disabled");
            return;
        }

        if (!CropStorageManager.isAutoSellSystemEnabled()) {
            String msg = File.getMessage().getString("cropstorage.autosell.system_disabled", "");
            if (msg != null && !msg.trim().isEmpty()) {
                player.sendMessage(ChatUtils.colorizewp(msg));
            }
            return;
        }

        if (args.length < 1) {
            sendUsage(sender);
            return;
        }

        String sub = args[0].toLowerCase();

        if ("clear".equals(sub)) {
            int cleared = CropStorageManager.clearAutoSell(player);
            String msg = File.getMessage().getString(
                    "cropstorage.autosell.cleared",
                    "#prefix# &aCleared &e#count# &aAutoSell crops."
            );
            player.sendMessage(ChatUtils.colorizewp(msg.replace("#count#", String.valueOf(cleared))));
            return;
        }

        if ("list".equals(sub)) {
            Set<String> enabled = CropStorageManager.getEnabledAutoSellItems(player);
            if (enabled.isEmpty()) {
                String msg = File.getMessage().getString("cropstorage.autosell.list_empty", "");
                if (msg != null && !msg.trim().isEmpty()) {
                    player.sendMessage(ChatUtils.colorizewp(msg));
                }
                return;
            }

            String joined = enabled.stream()
                    .map(CropStorageManager::getItemDisplayName)
                    .map(ChatUtils::colorizewp)
                    .collect(Collectors.joining(ChatUtils.colorizewp("&7, ")));
            String msg = File.getMessage().getString("cropstorage.autosell.list", "#prefix# &aAutoSell crops: &f#items#");
            player.sendMessage(ChatUtils.colorizewp(msg.replace("#items#", joined)));
            return;
        }

        if (args.length < 2) {
            sendUsage(sender);
            return;
        }

        String itemName = args[1].toUpperCase();
        if (!CropStorageManager.isConfiguredDrop(itemName)) {
            sendInvalidItem(sender, itemName);
            return;
        }

        String display = CropStorageManager.getItemDisplayName(itemName);

        if ("toggle".equals(sub)) {
            boolean enabled = CropStorageManager.toggleItemAutoSell(player, itemName);
            String key = enabled ? "cropstorage.autosell.toggle_on" : "cropstorage.autosell.toggle_off";
            String msg = File.getMessage().getString(key, "");
            if (msg != null && !msg.trim().isEmpty()) {
                player.sendMessage(ChatUtils.colorizewp(msg.replace("#material#", display)));
            }
            return;
        }

        if ("status".equals(sub)) {
            boolean enabled = CropStorageManager.isAutoSellEnabledForItem(player, itemName);
            String symbol = File.getMessage().getString(enabled ? "cropstorage.autosell.yes" : "cropstorage.autosell.no", enabled ? "&a✔" : "&c✘");
            String msg = File.getMessage().getString("cropstorage.autosell.status", "#prefix# &aAuto Sell for &e#material#&a: #status#");
            player.sendMessage(ChatUtils.colorizewp(msg.replace("#material#", display).replace("#status#", symbol)));
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

            boolean enabled = CropStorageManager.setItemAutoSell(player, itemName, desired);
            String key = enabled ? "cropstorage.autosell.toggle_on" : "cropstorage.autosell.toggle_off";
            String msg = File.getMessage().getString(key, "");
            if (msg != null && !msg.trim().isEmpty()) {
                player.sendMessage(ChatUtils.colorizewp(msg.replace("#material#", display)));
            }
            return;
        }

        sendUsage(sender);
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
            StringUtil.copyPartialMatches(args[1], getConfiguredDrops(), out);
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
        return "storage.cropstorage.autosell";
    }

    @Override
    public String getUsage() {
        return "/cropstorage autosell <toggle|set|status|list|clear> [item] [on|off]";
    }

    @Override
    public String getDescription() {
        return "Manage per-item AutoSell for CropStorage";
    }
}
