package net.danh.storage.CMD.handler.admin;

import net.danh.storage.CMD.handler.BaseCommand;
import net.danh.storage.Database.*;
import net.danh.storage.Manager.MineManager;
import net.danh.storage.Storage;
import net.danh.storage.Utils.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public class MigrateCommand extends BaseCommand {

    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1 || args.length > 2) {
            sendUsage(sender);
            return;
        }

        String typeName = args[0].toLowerCase();
        DatabaseType target = DatabaseType.fromString(typeName);
        // fromString falls back to SQLITE for unknown names; reject anything that
        // does not round-trip so a typo never silently migrates into SQLite.
        if (!target.getName().equals(typeName)) {
            sendMessage(sender, "admin.migrate.invalid_type", "#type#", args[0]);
            return;
        }

        DatabaseType current = DatabaseFactory.getDatabaseType();
        if (target == current) {
            sendMessage(sender, "admin.migrate.same_type", "#type#", target.getName());
            return;
        }

        boolean confirmed = args.length == 2 && args[1].equalsIgnoreCase("confirm");
        if (!confirmed) {
            sendMessage(sender, "admin.migrate.confirm_prompt",
                    new String[]{"#from#", "#to#"},
                    new String[]{current.getName(), target.getName()});
            sendMessage(sender, "admin.migrate.aux_warning");
            return;
        }

        if (!RUNNING.compareAndSet(false, true)) {
            sendMessage(sender, "admin.migrate.already_running");
            return;
        }

        // Flush online players to the source backend on the main thread before
        // reading, so the snapshot includes their latest inventory.
        for (Player player : Bukkit.getOnlinePlayers()) {
            MineManager.savePlayerData(player);
        }

        sendMessage(sender, "admin.migrate.started", "#to#", target.getName());

        Storage plugin = Storage.getStorage();
        SchedulerUtil.runTaskAsynchronously(plugin, () -> {
            try {
                runMigration(sender, plugin, target);
            } catch (Exception ex) {
                plugin.getLogger().log(Level.SEVERE, "Migration to " + target.getName() + " failed", ex);
                notify(sender, "admin.migrate.failed", "#error#",
                        ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
            } finally {
                RUNNING.set(false);
            }
        });
    }

    private void runMigration(CommandSender sender, Storage plugin, DatabaseType target) {
        IDataStorage source = Storage.dataStorage;
        List<PlayerData> records = source.getAllData();
        int total = records.size();
        plugin.getLogger().info("Migration: read " + total + " player records from " + currentName());
        notify(sender, "admin.migrate.read_complete", "#count#", String.valueOf(total));

        IDataStorage destination = DatabaseFactory.createDatabase(plugin, target);
        try {
            destination.load();

            if (destination instanceof SqlDataStorage) {
                // bulkSave upserts in batches of 100 per transaction
                ((SqlDataStorage) destination).database().bulkSave(records);
            } else {
                int done = 0;
                for (PlayerData record : records) {
                    destination.updateTable(record);
                    if (++done % 100 == 0) {
                        plugin.getLogger().info("Migration: " + done + "/" + total + " records written");
                    }
                }
            }

            plugin.getLogger().info("Migration: copied " + total + " records to " + target.getName());
            notify(sender, "admin.migrate.completed",
                    new String[]{"#count#", "#to#"},
                    new String[]{String.valueOf(total), target.getName()});
            notify(sender, "admin.migrate.finish_hint", "#to#", target.getName());
        } finally {
            destination.close();
        }
    }

    private String currentName() {
        return DatabaseFactory.getDatabaseType().getName();
    }

    private void notify(CommandSender sender, String key, String placeholder, String replacement) {
        SchedulerUtil.runTask(Storage.getStorage(), () -> sendMessage(sender, key, placeholder, replacement));
    }

    private void notify(CommandSender sender, String key, String[] placeholders, String[] replacements) {
        SchedulerUtil.runTask(Storage.getStorage(), () -> sendMessage(sender, key, placeholders, replacements));
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            List<String> types = new ArrayList<>();
            for (DatabaseType type : DatabaseType.values()) {
                types.add(type.getName());
            }
            StringUtil.copyPartialMatches(args[0], types, completions);
        } else if (args.length == 2) {
            StringUtil.copyPartialMatches(args[1], List.of("confirm"), completions);
        }
        return completions;
    }

    @Override
    public String getPermission() {
        return "storage.admin.migrate";
    }

    @Override
    public String getUsage() {
        return "/storage migrate <type> [confirm]";
    }

    @Override
    public String getDescription() {
        return "Migrate PlayerData to another database backend";
    }
}