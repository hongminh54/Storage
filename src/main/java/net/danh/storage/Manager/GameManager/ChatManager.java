package net.danh.storage.Manager.GameManager;

import net.danh.storage.Manager.UtilsManager.FileManager;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ChatManager {

    public static @NotNull String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', FileManager.getConfig().getString("prefix") + " " + message);
    }

    public static @NotNull String colorizewp(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static List<String> colorize(String... message) {
        return Arrays.stream(message).map(ChatManager::colorize).collect(Collectors.toList());
    }

    public static List<String> colorizewp(String... message) {
        return Arrays.stream(message).map(ChatManager::colorizewp).collect(Collectors.toList());
    }

    public static List<String> colorize(@NotNull List<String> message) {
        return message.stream().map(ChatManager::colorize).collect(Collectors.toList());
    }

    public static List<String> colorizewp(@NotNull List<String> message) {
        return message.stream().map(ChatManager::colorizewp).collect(Collectors.toList());
    }
}
