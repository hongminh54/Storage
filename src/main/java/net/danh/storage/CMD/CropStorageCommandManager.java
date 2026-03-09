package net.danh.storage.CMD;

import net.danh.storage.CMD.handler.BaseCommand;
import net.danh.storage.CMD.handler.CommandHandler;
import net.danh.storage.CMD.handler.crop.admin.*;
import net.danh.storage.CMD.handler.crop.user.*;
import net.danh.storage.GUI.Crop.CropStorageGUI;
import net.danh.storage.Manager.Crop.CropStorageManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.*;

public class CropStorageCommandManager extends BaseCommand {

    private final Map<String, CommandHandler> commands;

    public CropStorageCommandManager() {
        this.commands = new HashMap<>();
        registerCommands();
    }

    private void registerCommands() {
        // User commands
        registerCommand("help", new CropHelpCommand());
        registerCommand("toggle", new CropToggleCommand());
        registerCommand("groundstore", new CropGroundStoreCommand());
        registerCommand("deposit", new CropDepositCommand());
        registerCommand("withdraw", new CropWithdrawCommand());
        registerCommand("sell", new CropSellCommand());
        registerCommand("autosell", new CropAutoSellCommand());
        registerCommand("view", new CropViewCommand());
        registerCommand("transfer", new CropTransferCommand());

        // Admin commands
        registerCommand("add", new CropAddCommand());
        registerCommand("remove", new CropRemoveCommand());
        registerCommand("max", new CropMaxCommand());
        registerCommand("resetlimit", new CropResetLimitCommand());
        registerCommand("set", new CropSetCommand());
        registerCommand("reset", new CropResetCommand());
        registerCommand("reload", new CropReloadCommand());
    }

    private void registerCommand(String name, CommandHandler handler) {
        commands.put(name.toLowerCase(), handler);
    }

    public void handleCommand(CommandSender sender, String[] args) {
        if (!CropStorageManager.isSystemEnabled()) {
            sendMessage(sender, "cropstorage.system_disabled");
            return;
        }

        if (args.length == 0) {
            handleDefaultCommand(sender);
            return;
        }

        String commandName = args[0].toLowerCase();
        CommandHandler handler = commands.get(commandName);

        if (handler != null) {
            String permission = handler.getPermission();
            if (permission == null || sender.hasPermission(permission)) {
                String[] commandArgs = Arrays.copyOfRange(args, 1, args.length);
                handler.execute(sender, commandArgs);
            } else {
                sendMessage(sender, "cropstorage.admin.no_permission");
            }
        } else {
            sendMessage(sender, "cropstorage.admin.unknown_command", "#command#", commandName);
        }
    }

    private void handleDefaultCommand(CommandSender sender) {
        if (!requirePlayer(sender)) {
            return;
        }

        Player player = (Player) sender;

        if (isCropStorageWorldBlacklisted(player)) {
            sendWorldBlacklisted(sender, "CropStorage", player.getWorld().getName());
            return;
        }

        try {
            player.openInventory(new CropStorageGUI(player).getInventory());
        } catch (IndexOutOfBoundsException e) {
            sendMessage(sender, "cropstorage.admin.not_enough_slot");
        } catch (Exception e) {
            sendMessage(sender, "cropstorage.admin.error_opening_gui");
        }
    }

    public List<String> getCommandTabCompletions(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> availableCommands = new ArrayList<>();

            for (Map.Entry<String, CommandHandler> entry : commands.entrySet()) {
                String commandName = entry.getKey();
                CommandHandler handler = entry.getValue();
                String permission = handler.getPermission();

                if (permission == null || sender.hasPermission(permission)) {
                    availableCommands.add(commandName);
                }
            }

            StringUtil.copyPartialMatches(args[0], availableCommands, completions);
        } else if (args.length > 1) {
            String commandName = args[0].toLowerCase();
            CommandHandler handler = commands.get(commandName);

            if (handler != null) {
                String permission = handler.getPermission();
                if (permission == null || sender.hasPermission(permission)) {
                    String[] commandArgs = Arrays.copyOfRange(args, 1, args.length);
                    List<String> handlerCompletions = handler.getTabCompletions(sender, commandArgs);
                    if (handlerCompletions != null) {
                        completions.addAll(handlerCompletions);
                    }
                }
            }
        }

        Collections.sort(completions);
        return completions;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        handleCommand(sender, args);
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        return getCommandTabCompletions(sender, args);
    }

    @Override
    public String getPermission() {
        return null;
    }

    @Override
    public String getUsage() {
        return "/cropstorage [command]";
    }

    @Override
    public String getDescription() {
        return "CropStorage command manager";
    }
}
