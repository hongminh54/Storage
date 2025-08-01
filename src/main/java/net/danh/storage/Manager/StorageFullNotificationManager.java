package net.danh.storage.Manager;

import net.danh.storage.Utils.Chat;
import net.danh.storage.Utils.File;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class StorageFullNotificationManager {

    private static final Map<Player, Long> lastNotificationTime = new HashMap<>();

    public static void sendStorageFullNotification(Player player) {
        if (!isNotificationEnabled()) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        long cooldownMs = getCooldownSeconds() * 1000L;

        Long lastTime = lastNotificationTime.get(player);
        if (lastTime == null || (currentTime - lastTime) >= cooldownMs) {
            String message = File.getMessage().getString("user.full_storage");
            if (message != null) {
                player.sendMessage(Chat.colorize(message));
            }
            lastNotificationTime.put(player, currentTime);
        }
    }

    public static void removePlayer(Player player) {
        lastNotificationTime.remove(player);
    }

    private static boolean isNotificationEnabled() {
        return File.getConfig().getBoolean("settings.storage_full_notification.enabled", true);
    }

    private static int getCooldownSeconds() {
        return Math.max(1, File.getConfig().getInt("settings.storage_full_notification.cooldown_seconds", 10));
    }
}
