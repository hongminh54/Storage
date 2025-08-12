package net.danh.storage.Manager;

import net.danh.storage.Utils.Chat;
import net.danh.storage.Utils.File;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class StorageFullNotificationManager {

    private static final Map<String, Long> lastNotificationTime = new HashMap<>();

    public static void sendStorageFullNotification(Player player) {
        sendStorageFullNotification(player, null);
    }

    public static void sendStorageFullNotification(Player player, String material) {
        if (!isNotificationEnabled()) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        long cooldownMs = getCooldownSeconds() * 1000L;

        String key = material != null ? player.getName() + "_" + material : player.getName() + "_general";
        Long lastTime = lastNotificationTime.get(key);

        if (lastTime == null || (currentTime - lastTime) >= cooldownMs) {
            String message;
            if (material != null) {
                message = File.getMessage().getString("user.material_storage_full");
                if (message != null) {
                    String materialName = File.getConfig().getString("items." + material);
                    if (materialName == null) {
                        materialName = material.replace("_", " ").replace(";", ":");
                    }
                    message = message.replace("#material#", materialName);
                }
            } else {
                message = File.getMessage().getString("user.full_storage");
            }

            if (message != null) {
                player.sendMessage(Chat.colorize(message));
            }
            lastNotificationTime.put(key, currentTime);
        }
    }

    public static void removePlayer(Player player) {
        lastNotificationTime.entrySet().removeIf(entry -> entry.getKey().startsWith(player.getName() + "_"));
    }

    private static boolean isNotificationEnabled() {
        return File.getConfig().getBoolean("settings.storage_full_notification.enabled", true);
    }

    private static int getCooldownSeconds() {
        return Math.max(1, File.getConfig().getInt("settings.storage_full_notification.cooldown_seconds", 10));
    }
}
