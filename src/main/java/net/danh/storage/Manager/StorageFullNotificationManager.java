package net.danh.storage.Manager;

import net.danh.storage.Utils.ChatUtils;
import net.danh.storage.Utils.File;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StorageFullNotificationManager {

    private static final Map<UUID, Long> lastNotificationTime = new HashMap<>();

    public static void sendStorageFullNotification(Player player) {
        if (!isNotificationEnabled()) {
            return;
        }

        if (player == null) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        long cooldownMs = getCooldownSeconds() * 1000L;

        UUID playerId = player.getUniqueId();

        Long lastTime = lastNotificationTime.get(playerId);
        if (lastTime == null || (currentTime - lastTime) >= cooldownMs) {
            String message = File.getMessage().getString("user.full_storage");
            if (message != null) {
                player.sendMessage(ChatUtils.colorize(message));
            }
            lastNotificationTime.put(playerId, currentTime);
        }
    }

    public static void removePlayer(Player player) {
        if (player == null) {
            return;
        }
        lastNotificationTime.remove(player.getUniqueId());
    }

    private static boolean isNotificationEnabled() {
        return File.getConfig().getBoolean("settings.storage_full_notification.enabled", true);
    }

    private static int getCooldownSeconds() {
        return Math.max(1, File.getConfig().getInt("settings.storage_full_notification.cooldown_seconds", 10));
    }
}
