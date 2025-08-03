package net.danh.storage.CMD.handler;

import net.danh.storage.Utils.Chat;
import net.danh.storage.Utils.File;
import net.danh.storage.Utils.Number;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class BaseCommand implements CommandHandler {

    protected boolean checkPermission(CommandSender sender, String permission) {
        return sender.hasPermission(permission);
    }

    protected void sendMessage(CommandSender sender, String messageKey) {
        String message = File.getMessage().getString(messageKey);
        if (message != null) {
            sender.sendMessage(Chat.colorize(message));
        }
    }

    protected void sendMessage(CommandSender sender, String messageKey, String placeholder, String replacement) {
        String message = File.getMessage().getString(messageKey);
        if (message != null) {
            sender.sendMessage(Chat.colorize(message.replace(placeholder, replacement)));
        }
    }

    protected void sendMessage(CommandSender sender, String messageKey, String[] placeholders, String[] replacements) {
        String message = File.getMessage().getString(messageKey);
        if (message != null) {
            for (int i = 0; i < placeholders.length && i < replacements.length; i++) {
                message = message.replace(placeholders[i], replacements[i]);
            }
            sender.sendMessage(Chat.colorize(message));
        }
    }

    protected void sendColorizedMessage(CommandSender sender, String messageKey) {
        String message = File.getMessage().getString(messageKey);
        if (message != null) {
            sender.sendMessage(Chat.colorizewp(message));
        }
    }

    protected void sendColorizedMessage(CommandSender sender, String messageKey, String placeholder, String replacement) {
        String message = File.getMessage().getString(messageKey);
        if (message != null) {
            sender.sendMessage(Chat.colorizewp(message.replace(placeholder, replacement)));
        }
    }

    protected void sendColorizedMessage(CommandSender sender, String messageKey, String[] placeholders, String[] replacements) {
        String message = File.getMessage().getString(messageKey);
        if (message != null) {
            for (int i = 0; i < placeholders.length && i < replacements.length; i++) {
                message = message.replace(placeholders[i], replacements[i]);
            }
            sender.sendMessage(Chat.colorizewp(message));
        }
    }

    protected void sendMessageList(CommandSender sender, String messageKey) {
        List<String> messages = File.getMessage().getStringList(messageKey);
        for (String message : messages) {
            sender.sendMessage(Chat.colorize(message));
        }
    }

    protected void sendColorizedMessageList(CommandSender sender, String messageKey) {
        List<String> messages = File.getMessage().getStringList(messageKey);
        for (String message : messages) {
            sender.sendMessage(Chat.colorizewp(message));
        }
    }

    protected List<String> getOnlinePlayerNames() {
        List<String> playerNames = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            playerNames.add(player.getName());
        }
        return playerNames;
    }

    protected List<String> getOnlinePlayerNamesExcept(String excludeName) {
        List<String> playerNames = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.getName().equals(excludeName)) {
                playerNames.add(player.getName());
            }
        }
        return playerNames;
    }

    protected boolean isValidNumber(String str) {
        return Number.getInteger(str) >= 0;
    }

    protected Player getPlayer(String name) {
        return Bukkit.getPlayer(name);
    }

    protected boolean isPlayer(CommandSender sender) {
        return sender instanceof Player;
    }

    protected boolean requirePlayer(CommandSender sender) {
        if (!isPlayer(sender)) {
            sendMessage(sender, "admin.only_players");
            return false;
        }
        return true;
    }

    protected String getStatusMessage(boolean status) {
        return status ?
                Objects.requireNonNull(File.getMessage().getString("user.status.status_on")) :
                Objects.requireNonNull(File.getMessage().getString("user.status.status_off"));
    }

    protected void sendUsage(CommandSender sender) {
        sendMessage(sender, "admin.invalid_usage", "#usage#", getUsage());
    }

    protected void sendInvalidArguments(CommandSender sender) {
        sendMessage(sender, "admin.invalid_usage", "#usage#", getUsage());
    }

    protected void sendInvalidMaterial(CommandSender sender, String material, List<String> availableMaterials) {
        String materialsStr = String.join(", ", availableMaterials.size() > 10 ?
                availableMaterials.subList(0, 10) : availableMaterials);
        if (availableMaterials.size() > 10) {
            materialsStr += "...";
        }
        String[] placeholders = {"#material#", "#materials#"};
        String[] replacements = {material, materialsStr};
        sendMessage(sender, "admin.invalid_material", placeholders, replacements);
    }

    protected void sendInvalidPlayer(CommandSender sender, String playerName) {
        sendMessage(sender, "admin.invalid_player", "#player#", playerName);
    }

    protected void sendInvalidNumber(CommandSender sender, String number) {
        sendMessage(sender, "admin.invalid_number", "#number#", number);
    }

    protected void sendNumberTooLow(CommandSender sender) {
        sendMessage(sender, "admin.number_too_low");
    }
}
