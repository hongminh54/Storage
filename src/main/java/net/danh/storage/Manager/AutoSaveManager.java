package net.danh.storage.Manager;

import net.danh.storage.Storage;
import net.danh.storage.Utils.File;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.logging.Level;

public class AutoSaveManager {

    private static BukkitTask autoSaveTask;
    private static boolean isEnabled = false;
    private static int intervalMinutes = 5;
    private static boolean async = true;
    private static boolean logActivity = false;

    public static void initialize() {
        loadConfig();
        if (isEnabled) {
            startAutoSave();
        }
    }

    public static void loadConfig() {
        isEnabled = File.getConfig().getBoolean("auto_save.enabled", true);
        intervalMinutes = Math.max(1, File.getConfig().getInt("auto_save.interval_minutes", 5));
        async = File.getConfig().getBoolean("auto_save.async", true);
        logActivity = File.getConfig().getBoolean("auto_save.log_activity", false);
    }

    public static void startAutoSave() {
        if (autoSaveTask != null) {
            stopAutoSave();
        }

        if (!isEnabled) {
            return;
        }

        long intervalTicks = intervalMinutes * 60L * 20L;

        autoSaveTask = new BukkitRunnable() {
            @Override
            public void run() {
                saveAllPlayerData();
            }
        }.runTaskTimer(Storage.getStorage(), intervalTicks, intervalTicks);

        if (logActivity) {
            String message = File.getMessage().getString("admin.autosave.started", "Auto-save started with interval: #minutes# minutes");
            Storage.getStorage().getLogger().log(Level.INFO,
                    message.replace("#minutes#", String.valueOf(intervalMinutes)));
        }
    }

    public static void stopAutoSave() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
            autoSaveTask = null;

            if (logActivity) {
                String message = File.getMessage().getString("admin.autosave.stopped", "Auto-save stopped");
                Storage.getStorage().getLogger().log(Level.INFO, message);
            }
        }
    }

    public static void restartAutoSave() {
        loadConfig();
        stopAutoSave();
        if (isEnabled) {
            startAutoSave();
        }
    }

    private static void saveAllPlayerData() {
        if (async) {
            Bukkit.getScheduler().runTaskAsynchronously(Storage.getStorage(), () -> {
                performSave();
            });
        } else {
            performSave();
        }
    }

    private static void performSave() {
        try {
            int playerCount = 0;
            for (Player player : Bukkit.getOnlinePlayers()) {
                MineManager.savePlayerData(player);
                playerCount++;
            }

            if (logActivity && playerCount > 0) {
                String message = File.getMessage().getString("admin.autosave.completed", "Auto-save completed for #count# players");
                Storage.getStorage().getLogger().log(Level.INFO,
                        message.replace("#count#", String.valueOf(playerCount)));
            }
        } catch (Exception e) {
            String message = File.getMessage().getString("admin.autosave.error", "Error during auto-save: #error#");
            Storage.getStorage().getLogger().log(Level.SEVERE,
                    message.replace("#error#", e.getMessage()), e);
        }
    }

    public static void forceSave() {
        saveAllPlayerData();
        if (logActivity) {
            String message = File.getMessage().getString("admin.autosave.manual_executed", "Manual save executed");
            Storage.getStorage().getLogger().log(Level.INFO, message);
        }
    }

    public static boolean isEnabled() {
        return isEnabled;
    }

    public static int getIntervalMinutes() {
        return intervalMinutes;
    }

    public static boolean isAsync() {
        return async;
    }

    public static boolean isLogActivity() {
        return logActivity;
    }

    public static boolean isRunning() {
        return autoSaveTask != null && !autoSaveTask.isCancelled();
    }
}
