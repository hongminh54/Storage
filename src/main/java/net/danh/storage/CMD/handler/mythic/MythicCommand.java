package net.danh.storage.CMD.handler.mythic;

import net.danh.storage.CMD.handler.BaseCommand;
import net.danh.storage.Manager.MythicStorageManager;
import net.danh.storage.Utils.File;
import net.danh.storage.Utils.Number;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public abstract class MythicCommand extends BaseCommand {

    @Override
    protected void sendMessage(CommandSender sender, String messageKey) {
        String message = File.getMessage().getString("mythicstorage." + messageKey);
        if (message != null) {
            super.sendMessage(sender, "mythicstorage." + messageKey);
        }
    }

    @Override
    protected void sendMessage(CommandSender sender, String messageKey, String placeholder, String replacement) {
        super.sendMessage(sender, "mythicstorage." + messageKey, placeholder, replacement);
    }

    @Override
    protected void sendMessage(CommandSender sender, String messageKey, String[] placeholders, String[] replacements) {
        super.sendMessage(sender, "mythicstorage." + messageKey, placeholders, replacements);
    }

    @Override
    protected void sendMessageList(CommandSender sender, String messageKey) {
        super.sendMessageList(sender, "mythicstorage." + messageKey);
    }

    protected List<String> getConfiguredDrops() {
        return new ArrayList<>(MythicStorageManager.getConfiguredDrops());
    }

    @Override
    protected boolean isValidNumber(String str) {
        return Number.getInteger(str) >= 0;
    }

    protected int parsePositiveNumber(String str) throws NumberFormatException {
        int num = Number.getInteger(str);
        if (num <= 0) {
            throw new NumberFormatException("Number must be greater than 0");
        }
        return num;
    }

    protected void sendInvalidItem(CommandSender sender, String itemName) {
        sendMessage(sender, "invalid_item", "#item#", itemName);
    }

    protected void sendPlayerNotFound(CommandSender sender, String playerName) {
        sendMessage(sender, "player_not_found", "#player#", playerName);
    }

    @Override
    protected void sendInvalidNumber(CommandSender sender, String number) {
        sendMessage(sender, "admin.invalid_number", "#number#", number);
    }

    @Override
    protected void sendNumberTooLow(CommandSender sender) {
        sendMessage(sender, "admin.number_too_low");
    }

    @Override
    protected void sendInvalidPlayer(CommandSender sender, String playerName) {
        sendMessage(sender, "player_not_found", "#player#", playerName);
    }

    @Override
    protected void sendUsage(CommandSender sender) {
        sendMessage(sender, "admin.invalid_usage", "#usage#", getUsage());
    }
}
