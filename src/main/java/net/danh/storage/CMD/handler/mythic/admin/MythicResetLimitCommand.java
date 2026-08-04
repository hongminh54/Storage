package net.danh.storage.CMD.handler.mythic.admin;

import net.danh.storage.CMD.handler.mythic.MythicCommand;
import net.danh.storage.Database.PlayerData;
import net.danh.storage.Manager.Mythic.MythicStorageManager;
import net.danh.storage.Storage;
import net.danh.storage.Utils.File;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

public class MythicResetLimitCommand extends MythicCommand {

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
        int defaultMax = File.getMythicStorageConfig().getInt(
                "settings.default_max_storage",
                100000
        );

        MythicStorageManager.clearMaxStorageOverride(target);

        // Persist removal of the override marker before reloading, otherwise
        // loadPlayerData() will read the old 'mythicmaxoverride:' from DB and
        // re-apply the override immediately.
        MythicStorageManager.savePlayerData(target);

        if (existing != null) {
            Integer parsedOverride = null;
            String rawData = existing.data();
            if (rawData != null && !rawData.isEmpty()) {
                int idx = rawData.indexOf("mythicmaxoverride:");
                if (idx >= 0) {
                    int start = idx + "mythicmaxoverride:".length();
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
                    && existing.max() == parsedOverride) {
                PlayerData cleaned = new PlayerData(
                        existing.player(),
                        existing.data(),
                        Math.max(0, defaultMax),
                        existing.autoPickup()
                );
                Storage.dataStorage.updateTable(cleaned);
            }
        }

        MythicStorageManager.loadPlayerData(target);
        MythicStorageManager.savePlayerData(target);

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
        return "storage.mythicstorage.admin";
    }

    @Override
    public String getUsage() {
        return "/mythicstorage resetlimit <player>";
    }

    @Override
    public String getDescription() {
        return "Reset MythicStorage max override for a player";
    }
}
