package net.danh.storage.Utils;

import net.danh.storage.NMS.NMSAssistant;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ChatUtils {

    // Support multiple hex color formats
    private static final Pattern HEX_PATTERN_1 = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern HEX_PATTERN_2 = Pattern.compile("<#([A-Fa-f0-9]{6})>");
    private static final Pattern HEX_PATTERN_3 = Pattern.compile("\\{#([A-Fa-f0-9]{6})\\}");
    private static final Pattern HEX_PATTERN_4 = Pattern.compile("(?<!&)#([A-Fa-f0-9]{6})(?!\\w)");
    private static final Pattern HEX_PATTERN_AMPERSAND = Pattern.compile("&x(&[A-Fa-f0-9]){6}");
    private static final NMSAssistant nms = new NMSAssistant();

    public static @NotNull String colorize(String message) {
        return translateColors(replacePlaceholders(message));
    }

    public static @NotNull String colorizewp(String message) {
        return translateColors(replacePlaceholders(message));
    }

    public static @NotNull String colorize(@NotNull Player player, String message) {
        String result = PlaceholderUtils.setPlaceholders(player, message);
        return translateColors(replacePlaceholders(result));
    }

    public static @NotNull String colorizewp(@NotNull Player player, String message) {
        String result = PlaceholderUtils.setPlaceholders(player, message);
        return translateColors(replacePlaceholders(result));
    }

    public static List<String> colorize(String... message) {
        return Arrays.stream(message).map(ChatUtils::colorize).collect(Collectors.toList());
    }

    public static List<String> colorize(@NotNull Player player, String... message) {
        return Arrays.stream(message).map(msg -> colorize(player, msg)).collect(Collectors.toList());
    }

    public static List<String> colorize(@NotNull List<String> message) {
        return message.stream().map(ChatUtils::colorize).collect(Collectors.toList());
    }

    public static List<String> colorizewp(@NotNull List<String> message) {
        return message.stream().map(ChatUtils::colorizewp).collect(Collectors.toList());
    }

    private static @NotNull String replacePlaceholders(String message) {
        if (message == null) return "";

        String result = message;
        result = result.replace("#prefix#", File.getConfig().getString("prefix", ""));

        return result;
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

        // Process ampersand format first (Spigot native: &x&R&R&G&G&B&B)
        result = translateAmpersandHex(result);

        // Process bracket formats
        result = translateHexPattern(result, HEX_PATTERN_1);
        result = translateHexPattern(result, HEX_PATTERN_2);
        result = translateHexPattern(result, HEX_PATTERN_3);
        result = translateHexPattern(result, HEX_PATTERN_4);

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

    private static @NotNull String translateAmpersandHex(String message) {
        Matcher matcher = HEX_PATTERN_AMPERSAND.matcher(message);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String match = matcher.group(0);
            // Extract hex digits from &x&R&R&G&G&B&B format
            StringBuilder hexBuilder = new StringBuilder();
            for (int i = 3; i < match.length(); i += 2) {
                hexBuilder.append(match.charAt(i));
            }
            String hexColor = hexBuilder.toString();
            String replacement = getHexColor(hexColor);
            if (replacement.equals("&f")) {
                replacement = match;
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
