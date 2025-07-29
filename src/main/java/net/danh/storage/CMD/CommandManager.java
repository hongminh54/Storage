package net.danh.storage.CMD;

import net.danh.storage.CMD.handler.CommandHandler;
import net.danh.storage.CMD.handler.admin.*;
import net.danh.storage.CMD.handler.user.HelpCommand;
import net.danh.storage.CMD.handler.user.ToggleCommand;
import net.danh.storage.CMD.handler.user.TransferCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.util.StringUtil;

import java.util.*;

public class CommandManager {

    private final Map<String, CommandHandler> commands;

    public CommandManager() {
        this.commands = new HashMap<>();
        registerCommands();
    }

    private void registerCommands() {
        registerCommand("help", new HelpCommand());
        registerCommand("toggle", new ToggleCommand());
        registerCommand("transfer", new TransferCommand());

        registerCommand("reload", new ReloadCommand());
        registerCommand("autosave", new AutoSaveCommand());
        registerCommand("save", new SaveCommand());
        registerCommand("event", new EventCommand());
        registerCommand("max", new MaxCommand());
        registerCommand("add", new AddCommand());
        registerCommand("remove", new RemoveCommand());
        registerCommand("set", new SetCommand());
    }

    private void registerCommand(String name, CommandHandler handler) {
        commands.put(name.toLowerCase(), handler);
    }

    public void handleCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            handleDefaultCommand(sender);
            return;
        }

        String commandName = args[0].toLowerCase();
        CommandHandler handler = commands.get(commandName);

        if (handler != null) {
            String permission = handler.getPermission();
            if (permission == null || sender.hasPermission(permission) || hasAnyTransferPermission(sender, commandName)) {
                String[] commandArgs = Arrays.copyOfRange(args, 1, args.length);
                handler.execute(sender, commandArgs);
            }
        }
    }

    private void handleDefaultCommand(CommandSender sender) {
        if (sender instanceof org.bukkit.entity.Player) {
            try {
                org.bukkit.entity.Player player = (org.bukkit.entity.Player) sender;
                int currentPage = net.danh.storage.GUI.PersonalStorage.getPlayerCurrentPage(player);
                player.openInventory(new net.danh.storage.GUI.PersonalStorage(player, currentPage).getInventory());
            } catch (IndexOutOfBoundsException e) {
                sender.sendMessage(net.danh.storage.Utils.Chat.colorize(net.danh.storage.Utils.File.getMessage().getString("admin.not_enough_slot")));
            }
        }
    }

    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> availableCommands = new ArrayList<>();

            for (Map.Entry<String, CommandHandler> entry : commands.entrySet()) {
                String commandName = entry.getKey();
                CommandHandler handler = entry.getValue();
                String permission = handler.getPermission();

                if (permission == null || sender.hasPermission(permission) || hasAnyTransferPermission(sender, commandName)) {
                    availableCommands.add(commandName);
                }
            }

            StringUtil.copyPartialMatches(args[0], availableCommands, completions);
        } else if (args.length > 1) {
            String commandName = args[0].toLowerCase();
            CommandHandler handler = commands.get(commandName);

            if (handler != null) {
                String permission = handler.getPermission();
                if (permission == null || sender.hasPermission(permission) || hasAnyTransferPermission(sender, commandName)) {
                    String[] commandArgs = Arrays.copyOfRange(args, 1, args.length);
                    List<String> commandCompletions = handler.getTabCompletions(sender, commandArgs);
                    if (commandCompletions != null) {
                        completions.addAll(commandCompletions);
                    }
                }
            }
        }

        Collections.sort(completions);
        return completions;
    }

    public Set<String> getRegisteredCommands() {
        return new HashSet<>(commands.keySet());
    }

    private boolean hasAnyTransferPermission(CommandSender sender, String commandName) {
        if (!commandName.equals("transfer")) {
            return false;
        }
        return sender.hasPermission("storage.transfer.use") ||
                sender.hasPermission("storage.transfer.multi") ||
                sender.hasPermission("storage.transfer.log");
    }
}
