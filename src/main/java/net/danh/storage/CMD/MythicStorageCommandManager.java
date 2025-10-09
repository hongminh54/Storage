package net.danh.storage.CMD;

import net.danh.storage.CMD.handler.BaseCommand;
import net.danh.storage.CMD.handler.CommandHandler;
import net.danh.storage.CMD.handler.mythic.admin.*;
import net.danh.storage.CMD.handler.mythic.user.MythicHelpCommand;
import net.danh.storage.CMD.handler.mythic.user.MythicToggleCommand;
import net.danh.storage.CMD.handler.mythic.user.MythicViewCommand;
import net.danh.storage.Manager.MythicStorageManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.*;

public class MythicStorageCommandManager extends BaseCommand {

    private final Map<String, CommandHandler> commands;

    public MythicStorageCommandManager() {
        this.commands = new HashMap<>();
        registerCommands();
    }

    private void registerCommands() {
        // User commands
        registerCommand("toggle", new MythicToggleCommand());
        registerCommand("view", new MythicViewCommand());
        registerCommand("help", new MythicHelpCommand());

        // Admin commands
        registerCommand("add", new MythicAddCommand());
        registerCommand("remove", new MythicRemoveCommand());
        registerCommand("set", new MythicSetCommand());
        registerCommand("reset", new MythicResetCommand());
        registerCommand("reload", new MythicReloadCommand());
    }

    private void registerCommand(String name, CommandHandler handler) {
        commands.put(name.toLowerCase(), handler);
    }

    public void handleCommand(CommandSender sender, String[] args) {
        if (!MythicStorageManager.isSystemEnabled()) {
            sendMessage(sender, "mythicstorage.system_disabled");
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
                sendMessage(sender, "mythicstorage.admin.no_permission");
            }
        } else {
            sendMessage(sender, "mythicstorage.admin.unknown_command", "#command#", commandName);
        }
    }

    private void handleDefaultCommand(CommandSender sender) {
        if (!requirePlayer(sender)) {
            return;
        }

        Player player = (Player) sender;

        if (isMythicStorageWorldBlacklisted(player)) {
            sendWorldBlacklisted(sender, "MythicStorage", player.getWorld().getName());
            return;
        }

        try {
            player.openInventory(new net.danh.storage.GUI.MythicStorageGUI(player).getInventory());
        } catch (IndexOutOfBoundsException e) {
            sendMessage(sender, "mythicstorage.admin.not_enough_slot");
        } catch (Exception e) {
            sendMessage(sender, "mythicstorage.admin.error_opening_gui");
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
        return "/mythicstorage [command]";
    }

    @Override
    public String getDescription() {
        return "MythicStorage command manager";
    }
}
