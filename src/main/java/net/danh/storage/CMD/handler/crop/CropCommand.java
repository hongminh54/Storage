package net.danh.storage.CMD.handler.crop;

import net.danh.storage.CMD.handler.BaseCommand;
import net.danh.storage.Manager.Crop.CropStorageManager;
import net.danh.storage.Utils.File;
import net.danh.storage.Utils.Number;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public abstract class CropCommand extends BaseCommand {

    @Override
    protected void sendMessage(CommandSender sender, String messageKey) {
        String message = File.getMessage().getString("cropstorage." + messageKey);
        if (message != null) {
            super.sendMessage(sender, "cropstorage." + messageKey);
        }
    }

    @Override
    protected void sendMessage(CommandSender sender, String messageKey, String placeholder, String replacement) {
        super.sendMessage(sender, "cropstorage." + messageKey, placeholder, replacement);
    }

    @Override
    protected void sendMessage(CommandSender sender, String messageKey, String[] placeholders, String[] replacements) {
        super.sendMessage(sender, "cropstorage." + messageKey, placeholders, replacements);
    }

    @Override
    protected void sendMessageList(CommandSender sender, String messageKey) {
        super.sendMessageList(sender, "cropstorage." + messageKey);
    }

    protected List<String> getConfiguredDrops() {
        return new ArrayList<>(CropStorageManager.getConfiguredDrops());
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

    protected String getItemDisplayName(String itemName) {
        return CropStorageManager.getItemDisplayName(itemName);
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
