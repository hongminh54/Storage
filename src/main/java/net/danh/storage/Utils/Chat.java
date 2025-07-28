package net.danh.storage.Utils;

import net.danh.storage.NMS.NMSAssistant;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Chat {

    private static final Pattern HEX_PATTERN_1 = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern HEX_PATTERN_2 = Pattern.compile("<#([A-Fa-f0-9]{6})>");
    private static final NMSAssistant nms = new NMSAssistant();

    public static @NotNull String colorize(String message) {
        return translateColors(File.getConfig().getString("prefix") + " " + message);
    }

    public static @NotNull String colorizewp(String message) {
        return translateColors(message);
    }

    public static List<String> colorize(String... message) {
        return Arrays.stream(message).map(Chat::colorize).collect(Collectors.toList());
    }

    public static List<String> colorize(@NotNull List<String> message) {
        return message.stream().map(Chat::colorize).collect(Collectors.toList());
    }

    public static List<String> colorizewp(@NotNull List<String> message) {
        return message.stream().map(Chat::colorizewp).collect(Collectors.toList());
    }

    private static @NotNull String translateColors(String message) {
        if (message == null) return "";

        String result = message;

        if (nms.isVersionGreaterThanOrEqualTo(16)) {
            result = translateHexColors(result);
        }

        return ChatColor.translateAlternateColorCodes('&', result);
    }

    private static @NotNull String translateHexColors(String message) {
        String result = message;

        result = translateHexPattern(result, HEX_PATTERN_1);
        result = translateHexPattern(result, HEX_PATTERN_2);

        return result;
    }

    private static @NotNull String translateHexPattern(String message, Pattern pattern) {
        Matcher matcher = pattern.matcher(message);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String hexColor = matcher.group(1);
            String replacement = getHexColor(hexColor);
            if (replacement.equals("&f")) {
                replacement = matcher.group(0);
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);

        return buffer.toString();
    }

    private static @NotNull String getHexColor(String hex) {
        try {
            Class<?> chatColorClass = Class.forName("net.md_5.bungee.api.ChatColor");
            Method ofMethod = chatColorClass.getMethod("of", String.class);
            Object chatColor = ofMethod.invoke(null, "#" + hex);
            return chatColor.toString();
        } catch (Exception e) {
            return "&f";
        }
    }
}
