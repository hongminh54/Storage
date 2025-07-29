package net.danh.storage.CMD.handler.admin;

import net.danh.storage.CMD.handler.BaseCommand;
import net.danh.storage.Event.EventType;
import net.danh.storage.Manager.EventManager;
import net.danh.storage.Utils.File;
import org.bukkit.command.CommandSender;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

public class EventCommand extends BaseCommand {

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            showEventStatus(sender);
            return;
        }

        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("help")) {
                sendColorizedMessageList(sender, "events.help");
                return;
            }

            if (args[0].equalsIgnoreCase("list")) {
                showEventList(sender);
                return;
            }
        }

        if (args.length == 2) {
            String action = args[0].toLowerCase();
            String eventTypeStr = args[1].toLowerCase();

            EventType eventType = EventType.fromConfigKey(eventTypeStr);
            if (eventType == null) {
                sendColorizedMessage(sender, "events.invalid_type");
                return;
            }

            String eventName = File.getEventConfig().getString("events." + eventType.getConfigKey() + ".event_name", eventType.getDisplayName());

            if (action.equals("start")) {
                if (EventManager.startEvent(eventType)) {
                    sendColorizedMessage(sender, "events.start.success", "#event#", eventName);
                } else {
                    sendColorizedMessage(sender, "events.start.failed", "#event#", eventName);
                }
            } else if (action.equals("stop")) {
                if (EventManager.stopEvent(eventType, false)) {
                    String messageKey = eventType == EventType.MINING_CONTEST ?
                            "events.stop.success_mining_contest" : "events.stop.success";
                    sendColorizedMessage(sender, messageKey, "#event#", eventName);
                } else {
                    sendColorizedMessage(sender, "events.stop.failed", "#event#", eventName);
                }
            }
        }
    }

    private void showEventStatus(CommandSender sender) {
        sendColorizedMessage(sender, "events.status.header");
        for (EventType eventType : EventType.values()) {
            String eventName = File.getEventConfig().getString("events." + eventType.getConfigKey() + ".event_name", eventType.getDisplayName());
            String status = EventManager.getEventStatus(eventType);
            String[] placeholders = {"#event#", "#status#"};
            String[] replacements = {eventName, status};
            sendColorizedMessage(sender, "events.status.line", placeholders, replacements);
        }
        sendColorizedMessage(sender, "events.status.footer");
    }

    private void showEventList(CommandSender sender) {
        sendColorizedMessage(sender, "events.list.header");
        boolean hasActiveEvents = false;

        for (EventType eventType : EventType.values()) {
            if (EventManager.isEventActive(eventType)) {
                String eventName = File.getEventConfig().getString("events." + eventType.getConfigKey() + ".event_name", eventType.getDisplayName());
                sendColorizedMessage(sender, "events.list.active", "#event#", eventName);
                hasActiveEvents = true;
            }
        }

        if (!hasActiveEvents) {
            sendColorizedMessage(sender, "events.list.no_active");
        }
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        List<String> commands = new ArrayList<>();

        if (args.length == 1) {
            commands.add("help");
            commands.add("list");
            commands.add("start");
            commands.add("stop");
            StringUtil.copyPartialMatches(args[0], commands, completions);
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("start") || args[0].equalsIgnoreCase("stop")) {
                commands.add("mining_contest");
                commands.add("double_drop");
                commands.add("community_event");
                StringUtil.copyPartialMatches(args[1], commands, completions);
            }
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "storage.admin.reload";
    }

    @Override
    public String getUsage() {
        return "/storage event [help|list|start|stop] [event_type]";
    }

    @Override
    public String getDescription() {
        return "Manage server events";
    }
}
