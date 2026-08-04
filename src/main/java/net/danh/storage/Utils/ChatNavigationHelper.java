package net.danh.storage.Utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class ChatNavigationHelper {

    // Send navigation for Storage Transfer log
    public static void sendStorageTransferNavigation(Player player, String targetPlayer, int currentPage, int totalPages,
                                                     int prevPage, int nextPage, boolean hasPrev, boolean hasNext) {
        FileConfiguration messageConfig = File.getMessage();
        String prevCommand = buildStorageNavigationCommand(targetPlayer, player.getName(), prevPage);
        String nextCommand = buildStorageNavigationCommand(targetPlayer, player.getName(), nextPage);

        sendTellrawNavigation(player, prevCommand, nextCommand, hasPrev, hasNext, prevPage, nextPage,
                "transfer.log_nav_previous", "transfer.log_nav_previous_disabled",
                "transfer.log_nav_next", "transfer.log_nav_next_disabled",
                "transfer.log_nav_hover", "transfer.log_nav_spacing",
                "transfer.log_nav_colors", messageConfig);
    }

    // Send navigation for MythicStorage Transfer log
    public static void sendMythicTransferNavigation(Player player, String targetPlayer, int currentPage, int totalPages,
                                                    int prevPage, int nextPage, boolean hasPrev, boolean hasNext) {
        FileConfiguration messageConfig = File.getMessage();
        String prevCommand = buildMythicNavigationCommand(targetPlayer, player.getName(), prevPage);
        String nextCommand = buildMythicNavigationCommand(targetPlayer, player.getName(), nextPage);

        sendTellrawNavigation(player, prevCommand, nextCommand, hasPrev, hasNext, prevPage, nextPage,
                "mythicstorage.transfer.log_nav_previous", "mythicstorage.transfer.log_nav_previous_disabled",
                "mythicstorage.transfer.log_nav_next", "mythicstorage.transfer.log_nav_next_disabled",
                "mythicstorage.transfer.log_nav_hover", "mythicstorage.transfer.log_nav_spacing",
                "mythicstorage.transfer.log_nav_colors", messageConfig);
    }

    // Send navigation for CropStorage Transfer log
    public static void sendCropTransferNavigation(Player player, String targetPlayer, int currentPage, int totalPages,
                                                  int prevPage, int nextPage, boolean hasPrev, boolean hasNext) {
        FileConfiguration messageConfig = File.getMessage();
        String prevCommand = buildCropNavigationCommand(targetPlayer, player.getName(), prevPage);
        String nextCommand = buildCropNavigationCommand(targetPlayer, player.getName(), nextPage);

        sendTellrawNavigation(player, prevCommand, nextCommand, hasPrev, hasNext, prevPage, nextPage,
                "cropstorage.transfer.log_nav_previous", "cropstorage.transfer.log_nav_previous_disabled",
                "cropstorage.transfer.log_nav_next", "cropstorage.transfer.log_nav_next_disabled",
                "cropstorage.transfer.log_nav_hover", "cropstorage.transfer.log_nav_spacing",
                "cropstorage.transfer.log_nav_colors", messageConfig);
    }

    // Send navigation for MobStorage Transfer log
    public static void sendMobTransferNavigation(Player player, String targetPlayer, int currentPage, int totalPages,
                                                 int prevPage, int nextPage, boolean hasPrev, boolean hasNext) {
        FileConfiguration messageConfig = File.getMessage();
        String prevCommand = buildMobNavigationCommand(targetPlayer, player.getName(), prevPage);
        String nextCommand = buildMobNavigationCommand(targetPlayer, player.getName(), nextPage);

        sendTellrawNavigation(player, prevCommand, nextCommand, hasPrev, hasNext, prevPage, nextPage,
                "mobstorage.transfer.log_nav_previous", "mobstorage.transfer.log_nav_previous_disabled",
                "mobstorage.transfer.log_nav_next", "mobstorage.transfer.log_nav_next_disabled",
                "mobstorage.transfer.log_nav_hover", "mobstorage.transfer.log_nav_spacing",
                "mobstorage.transfer.log_nav_colors", messageConfig);
    }

    private static String buildStorageNavigationCommand(String targetPlayer, String viewerName, int page) {
        if (targetPlayer == null || targetPlayer.equals(viewerName)) {
            return "/storage transfer log " + page;
        } else {
            return "/storage transfer log " + targetPlayer + " " + page;
        }
    }

    private static String buildMythicNavigationCommand(String targetPlayer, String viewerName, int page) {
        if (targetPlayer.equals(viewerName)) {
            return "/mythicstorage transfer log " + page;
        } else {
            return "/mythicstorage transfer log " + targetPlayer + " " + page;
        }
    }

    private static String buildCropNavigationCommand(String targetPlayer, String viewerName, int page) {
        if (targetPlayer == null || targetPlayer.equalsIgnoreCase(viewerName)) {
            return "/cropstorage transfer log " + page;
        } else {
            return "/cropstorage transfer log " + targetPlayer + " " + page;
        }
    }

    private static String buildMobNavigationCommand(String targetPlayer, String viewerName, int page) {
        if (targetPlayer == null || targetPlayer.equalsIgnoreCase(viewerName)) {
            return "/mobstorage transfer log " + page;
        } else {
            return "/mobstorage transfer log " + targetPlayer + " " + page;
        }
    }

    private static void sendTellrawNavigation(Player player, String prevCommand, String nextCommand,
                                              boolean hasPrev, boolean hasNext, int prevPage, int nextPage,
                                              String prevKey, String prevDisabledKey, String nextKey, String nextDisabledKey,
                                              String hoverKey, String spacingKey, String colorsKey,
                                              FileConfiguration messageConfig) {
        try {
            String prevText = stripColorCodes(messageConfig.getString(prevKey));
            String prevDisabledText = stripColorCodes(messageConfig.getString(prevDisabledKey));
            String prevHover = stripColorCodes(messageConfig.getString(hoverKey).replace("#page#", String.valueOf(prevPage)));

            String nextText = stripColorCodes(messageConfig.getString(nextKey));
            String nextDisabledText = stripColorCodes(messageConfig.getString(nextDisabledKey));
            String nextHover = stripColorCodes(messageConfig.getString(hoverKey).replace("#page#", String.valueOf(nextPage)));

            String spacing = stripColorCodes(messageConfig.getString(spacingKey));

            String activeColor = convertToJsonColor(messageConfig.getString(colorsKey + ".active"));
            String disabledColor = convertToJsonColor(messageConfig.getString(colorsKey + ".disabled"));
            String spacingColor = convertToJsonColor(messageConfig.getString(colorsKey + ".spacing"));

            StringBuilder json = new StringBuilder();
            json.append("[\"\"");

            if (hasPrev) {
                json.append(",{\"text\":\"").append(escapeJson(prevText)).append("\",\"color\":\"").append(activeColor)
                        .append("\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"").append(escapeJson(prevCommand))
                        .append("\"},\"hoverEvent\":{\"action\":\"show_text\",\"value\":\"").append(escapeJson(prevHover)).append("\"}}");
            } else {
                json.append(",{\"text\":\"").append(escapeJson(prevDisabledText)).append("\",\"color\":\"").append(disabledColor).append("\"}");
            }

            json.append(",{\"text\":\"").append(escapeJson(spacing)).append("\",\"color\":\"").append(spacingColor).append("\"}");

            if (hasNext) {
                json.append(",{\"text\":\"").append(escapeJson(nextText)).append("\",\"color\":\"").append(activeColor)
                        .append("\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"").append(escapeJson(nextCommand))
                        .append("\"},\"hoverEvent\":{\"action\":\"show_text\",\"value\":\"").append(escapeJson(nextHover)).append("\"}}");
            } else {
                json.append(",{\"text\":\"").append(escapeJson(nextDisabledText)).append("\",\"color\":\"").append(disabledColor).append("\"}");
            }

            json.append("]");

            String tellrawCommand = "tellraw " + player.getName() + " " + json;
            player.getServer().dispatchCommand(player.getServer().getConsoleSender(), tellrawCommand);

        } catch (Exception e) {
            useFallbackNavigation(player, prevCommand, nextCommand, hasPrev, hasNext, colorsKey,
                    prevKey, prevDisabledKey, nextKey, nextDisabledKey, spacingKey, messageConfig);
        }
    }

    private static void useFallbackNavigation(Player player, String prevCommand, String nextCommand,
                                              boolean hasPrev, boolean hasNext, String colorsKey,
                                              String prevKey, String prevDisabledKey, String nextKey,
                                              String nextDisabledKey, String spacingKey,
                                              FileConfiguration messageConfig) {
        String activeColorCode = convertToMinecraftColor(messageConfig.getString(colorsKey + ".active"));
        String disabledColorCode = convertToMinecraftColor(messageConfig.getString(colorsKey + ".disabled"));
        String spacingColorCode = convertToMinecraftColor(messageConfig.getString(colorsKey + ".spacing"));

        String prevText = messageConfig.getString(prevKey);
        String prevDisabledText = messageConfig.getString(prevDisabledKey);
        String nextText = messageConfig.getString(nextKey);
        String nextDisabledText = messageConfig.getString(nextDisabledKey);
        String spacing = messageConfig.getString(spacingKey);

        StringBuilder fallback = new StringBuilder();

        if (hasPrev) {
            fallback.append(activeColorCode).append(prevText);
        } else {
            fallback.append(disabledColorCode).append(prevDisabledText);
        }

        fallback.append(spacingColorCode).append(spacing);

        if (hasNext) {
            fallback.append(activeColorCode).append(nextText);
        } else {
            fallback.append(disabledColorCode).append(nextDisabledText);
        }

        player.sendMessage(ChatUtils.colorizewp(fallback.toString()));

        String helpMessage = messageConfig.getString(colorsKey.replace(".log_nav_colors", ".log_nav_help"));
        if (helpMessage != null) {
            player.sendMessage(ChatUtils.colorizewp(helpMessage));
        }

        if (hasPrev) {
            player.sendMessage(ChatUtils.colorizewp("&7Previous: &e" + prevCommand));
        }
        if (hasNext) {
            player.sendMessage(ChatUtils.colorizewp("&7Next: &e" + nextCommand));
        }
    }

    public static String stripColorCodes(String text) {
        if (text == null) return "";

        String result = text.replaceAll("&[0-9a-fA-F]", "");
        result = result.replaceAll("&#[0-9a-fA-F]{6}", "");
        result = result.replaceAll("<#[0-9a-fA-F]{6}>", "");
        result = result.replaceAll("&[lLnNmMoOkKrR]", "");

        return result;
    }

    public static String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public static String convertToJsonColor(String color) {
        if (color == null) return "white";

        if (color.matches("#[0-9a-fA-F]{6}")) {
            return color;
        }

        if (color.matches("&#[0-9a-fA-F]{6}")) {
            return color.substring(1);
        }

        if (color.matches("<#[0-9a-fA-F]{6}>")) {
            return color.substring(1, color.length() - 1);
        }

        switch (color.toLowerCase()) {
            case "&a":
            case "green":
                return "green";
            case "&c":
            case "red":
                return "red";
            case "&e":
            case "yellow":
                return "yellow";
            case "&7":
            case "gray":
                return "gray";
            case "&8":
            case "dark_gray":
                return "dark_gray";
            case "&b":
            case "aqua":
                return "aqua";
            case "&d":
            case "light_purple":
                return "light_purple";
            case "&f":
            case "white":
                return "white";
            case "&0":
            case "black":
                return "black";
            case "&1":
            case "dark_blue":
                return "dark_blue";
            case "&2":
            case "dark_green":
                return "dark_green";
            case "&3":
            case "dark_aqua":
                return "dark_aqua";
            case "&4":
            case "dark_red":
                return "dark_red";
            case "&5":
            case "dark_purple":
                return "dark_purple";
            case "&6":
            case "gold":
                return "gold";
            case "&9":
            case "blue":
                return "blue";
            default:
                return color;
        }
    }

    public static String convertToMinecraftColor(String color) {
        if (color == null) return "&f";

        if (color.matches("&#[0-9a-fA-F]{6}")) {
            return color;
        }

        if (color.matches("#[0-9a-fA-F]{6}")) {
            return "&" + color;
        }

        if (color.matches("<#[0-9a-fA-F]{6}>")) {
            return "&" + color.substring(1, color.length() - 1);
        }

        if (color.startsWith("&")) {
            return color;
        }

        return "&f";
    }
}
