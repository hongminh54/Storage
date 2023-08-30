package net.danh.storage.Manager.GameManager;

import net.danh.storage.Manager.UtilsManager.FileManager;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ChatManager {

    public static @NotNull String colorize(String message) {
        return ColorManager.colorize(FileManager.getConfig().getString("prefix") + " " + message);
    }

    public static @NotNull String colorizewp(String message) {
        return ColorManager.colorize(message);
    }

    public static List<String> colorize(String... message) {
        return Arrays.stream(message).map(ChatManager::colorize).collect(Collectors.toList());
    }

    public static List<String> colorize(@NotNull List<String> message) {
        return message.stream().map(ChatManager::colorize).collect(Collectors.toList());
    }
}
