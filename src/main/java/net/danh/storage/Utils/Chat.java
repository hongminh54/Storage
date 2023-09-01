package net.danh.storage.Utils;

import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Chat {

    public static @NotNull String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', File.getConfig().getString("prefix") + " " + message);
    }

    public static @NotNull String colorizewp(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static List<String> colorize(String... message) {
        return Arrays.stream(message).map(Chat::colorize).collect(Collectors.toList());
    }

    public static List<String> colorizewp(String... message) {
        return Arrays.stream(message).map(Chat::colorizewp).collect(Collectors.toList());
    }

    public static List<String> colorize(@NotNull List<String> message) {
        return message.stream().map(Chat::colorize).collect(Collectors.toList());
    }

    public static List<String> colorizewp(@NotNull List<String> message) {
        return message.stream().map(Chat::colorizewp).collect(Collectors.toList());
    }
}
