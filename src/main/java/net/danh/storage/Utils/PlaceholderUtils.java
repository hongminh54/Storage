package net.danh.storage.Utils;

import me.clip.placeholderapi.PlaceholderAPI;
import net.danh.storage.Storage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaceholderUtils {

    private static final Pattern CUSTOM_PLACEHOLDER_PATTERN = Pattern.compile("#([a-zA-Z0-9_]+)#");
    private static final Set<String> LOGGED_KEYS = new HashSet<>();

    private static Boolean papiAvailable = null;

    public static boolean isPlaceholderAPIAvailable() {
        if (papiAvailable == null) {
            papiAvailable = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        }
        return papiAvailable;
    }

    @NotNull
    public static String setPlaceholders(@Nullable Player player, @NotNull String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        String result = text;

        result = convertCustomPlaceholders(result);

        if (isPlaceholderAPIAvailable() && player != null) {
            try {
                result = PlaceholderAPI.setPlaceholders(player, result);
            } catch (Exception e) {
                synchronized (LOGGED_KEYS) {
                    if (LOGGED_KEYS.add("placeholderapi.set_placeholders")) {
                        Storage.getStorage().getLogger().log(Level.WARNING,
                                "[Storage] Failed to apply PlaceholderAPI placeholders",
                                e);
                    }
                }
            }
        }

        return result;
    }

    @NotNull
    private static String convertCustomPlaceholders(@NotNull String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        Matcher matcher = CUSTOM_PLACEHOLDER_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String placeholder = matcher.group(1);
            if (!isInternalPlaceholder(placeholder)) {
                matcher.appendReplacement(buffer, "%" + placeholder + "%");
            }
        }
        matcher.appendTail(buffer);

        return buffer.toString();
    }

    private static boolean isInternalPlaceholder(@NotNull String placeholder) {
        String[] internalPlaceholders = {
                "prefix",
                "player",
                "item_amount",
                "max_storage",
                "material",
                "status",
                "current_page",
                "total_pages",
                "amount",
                "price",
                "total",
                "balance",
                "item_name",
                "item",
                "sender",
                "receiver",
                "target",
                "time",
                "date",
                "rank",
                "score",
                "goal",
                "progress",
                "percentage",
                "multiplier",
                "participants",
                "contribution",
                "remaining",
                "duration",
                "event_name",
                "event_type",
                "reward",
                "position",
                "value",
                "count",
                "slot",
                "page",
                "input_amount",
                "output_amount",
                "input_material",
                "output_material",
                "ratio",
                "convert_amount",
                "result_amount"
        };

        String lowerPlaceholder = placeholder.toLowerCase();
        for (String internal : internalPlaceholders) {
            if (lowerPlaceholder.equals(internal) || lowerPlaceholder.startsWith(internal + "_")) {
                return true;
            }
        }
        return false;
    }
}
