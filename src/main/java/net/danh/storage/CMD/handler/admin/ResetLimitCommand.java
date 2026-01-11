package net.danh.storage.CMD.handler.admin;

import net.danh.storage.CMD.handler.BaseCommand;
import net.danh.storage.Database.PlayerData;
import net.danh.storage.Manager.MineManager;
import net.danh.storage.Storage;
import net.danh.storage.Utils.File;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

public class ResetLimitCommand extends BaseCommand {

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length != 1) {
            sendUsage(sender);
            return;
        }

        Player target = getPlayer(args[0]);
        if (target == null) {
            sendInvalidPlayer(sender, args[0]);
            return;
        }

        PlayerData existing = Storage.db.getData(target.getName());
        int defaultMax = File.getConfig().getInt(
                "settings.default_max_storage",
                100000
        );

        MineManager.clearMaxStorageOverride(target);

        // Persist removal of the override marker before reloading, otherwise
        // loadPlayerData() will read the old ';maxoverride:' from DB and
        // re-apply the override immediately.
        MineManager.savePlayerData(target);

        if (existing != null) {
            Integer parsedOverride = null;
            String rawData = existing.getData();
            if (rawData != null && !rawData.isEmpty()) {
                int idx = rawData.indexOf(";maxoverride:");
                if (idx >= 0) {
                    int start = idx + ";maxoverride:".length();
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
                        existing.isAutoPickup()
                );
                Storage.db.updateTable(cleaned);
            }
        }

        MineManager.loadPlayerData(target);
        MineManager.savePlayerData(target);

        String[] placeholders = {"#player#"};
        String[] replacements = {target.getName()};
        sendMessage(sender, "admin.reset_limit_storage", placeholders,
                replacements);
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> playerNames = getOnlinePlayerNames();
            StringUtil.copyPartialMatches(args[0], playerNames, completions);
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "storage.admin.resetlimit";
    }

    @Override
    public String getUsage() {
        return "/storage resetlimit <player>";
    }

    @Override
    public String getDescription() {
        return "Reset max storage limit for a player";
    }
}
