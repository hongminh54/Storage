package net.danh.storage.CMD.handler.crop.admin;

import net.danh.storage.CMD.handler.crop.CropCommand;
import net.danh.storage.Database.PlayerData;
import net.danh.storage.Manager.Crop.CropStorageManager;
import net.danh.storage.Storage;
import net.danh.storage.Utils.File;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

public class CropResetLimitCommand extends CropCommand {

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length != 1) {
            sendUsage(sender);
            return;
        }

        Player target = getPlayer(args[0]);
        if (target == null) {
            sendPlayerNotFound(sender, args[0]);
            return;
        }

        PlayerData existing = Storage.dataStorage.getData(target.getName());
        int defaultMax = File.getCropStorageConfig().getInt(
                "settings.default_max_storage",
                100000);

        CropStorageManager.clearMaxStorageOverride(target);

        // Persist removal of the override marker before reloading
        CropStorageManager.savePlayerData(target);

        if (existing != null) {
            Integer parsedOverride = null;
            String rawData = existing.getData();
            if (rawData != null && !rawData.isEmpty()) {
                int idx = rawData.indexOf("cropmaxoverride:");
                if (idx >= 0) {
                    int start = idx + "cropmaxoverride:".length();
                    int end = rawData.indexOf(';', start);
                    String raw = end >= 0
                            ? rawData.substring(start, end)
                            : rawData.substring(start);
                    raw = raw.trim();
                    try {
                        parsedOverride = Integer.parseInt(raw);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            if (parsedOverride != null
                    && parsedOverride >= 0
                    && existing.getMax() == parsedOverride) {
                PlayerData cleaned = new PlayerData(
                        existing.getPlayer(),
                        existing.getData(),
                        Math.max(0, defaultMax),
                        existing.isAutoPickup());
                Storage.dataStorage.updateTable(cleaned);
            }
        }

        CropStorageManager.loadPlayerData(target);
        CropStorageManager.savePlayerData(target);

        sendMessage(sender, "admin.reset_limit_storage", "#player#",
                target.getName());
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], getOnlinePlayerNames(), completions);
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "storage.cropstorage.admin";
    }

    @Override
    public String getUsage() {
        return "/cropstorage resetlimit <player>";
    }

    @Override
    public String getDescription() {
        return "Reset CropStorage max override for a player";
    }
}
