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

            if (args[0].equalsIgnoreCase("debug")) {
                showEventDebug(sender);
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

    private void showEventDebug(CommandSender sender) {
        sendColorizedMessage(sender, "events.debug.header");

        for (EventType eventType : EventType.values()) {
            String eventName = File.getEventConfig().getString("events." + eventType.getConfigKey() + ".event_name", eventType.getDisplayName());
            String schedule = File.getEventConfig().getString("events." + eventType.getConfigKey() + ".timing.schedule", "N/A");
            String timingType = File.getEventConfig().getString("events." + eventType.getConfigKey() + ".timing.type", "interval");

            // Event info header
            String[] eventPlaceholders = {"#event_name#", "#event_key#"};
            String[] eventReplacements = {eventName, eventType.getConfigKey()};
            sendColorizedMessage(sender, "events.debug.event_info", eventPlaceholders, eventReplacements);

            // Timing type
            sendColorizedMessage(sender, "events.debug.timing_type", "#type#", timingType);

            // Schedule config - show different info for interval vs schedule
            if ("interval".equals(timingType)) {
                int interval = File.getEventConfig().getInt("events." + eventType.getConfigKey() + ".timing.interval", 3600);
                String intervalDisplay = formatIntervalTime(interval);
                sendColorizedMessage(sender, "events.debug.schedule_config", "#schedule#", "Every " + intervalDisplay);
            } else {
                sendColorizedMessage(sender, "events.debug.schedule_config", "#schedule#", schedule);
            }

            // Show next run time for both types
            if ("schedule".equals(timingType)) {
                try {
                    java.time.LocalDateTime nextTime = EventManager.getScheduler().getNextScheduleTime(eventType);
                    if (nextTime != null) {
                        // Next run time
                        sendColorizedMessage(sender, "events.debug.next_run", "#next_time#", nextTime.toString());

                        // Stored time
                        long nextScheduledTime = EventManager.getAllEvents().get(eventType).getEventData().getNextScheduledTime();
                        if (nextScheduledTime > 0) {
                            java.time.LocalDateTime storedTime = java.time.LocalDateTime.ofInstant(
                                    java.time.Instant.ofEpochMilli(nextScheduledTime),
                                    java.time.ZoneId.systemDefault()
                            );
                            sendColorizedMessage(sender, "events.debug.stored_time", "#stored_time#", storedTime.toString());
                        } else {
                            sendColorizedMessage(sender, "events.debug.stored_time_not_set");
                        }
                    } else {
                        sendColorizedMessage(sender, "events.debug.cannot_calculate");
                    }
                } catch (Exception e) {
                    sendColorizedMessage(sender, "events.debug.error_occurred", "#error#", e.getMessage());
                }
            } else if ("interval".equals(timingType)) {
                // Handle interval events
                try {
                    java.time.LocalDateTime nextTime = EventManager.getScheduler().getNextScheduleTime(eventType);
                    if (nextTime != null) {
                        sendColorizedMessage(sender, "events.debug.next_run", "#next_time#", nextTime.toString());

                        long nextScheduledTime = EventManager.getAllEvents().get(eventType).getEventData().getNextScheduledTime();
                        if (nextScheduledTime > 0) {
                            java.time.LocalDateTime storedTime = java.time.LocalDateTime.ofInstant(
                                    java.time.Instant.ofEpochMilli(nextScheduledTime),
                                    java.time.ZoneId.systemDefault()
                            );
                            sendColorizedMessage(sender, "events.debug.stored_time", "#stored_time#", storedTime.toString());
                        } else {
                            sendColorizedMessage(sender, "events.debug.stored_time_not_set");
                        }
                    } else {
                        sendColorizedMessage(sender, "events.debug.cannot_calculate");
                    }
                } catch (Exception e) {
                    sendColorizedMessage(sender, "events.debug.error_occurred", "#error#", e.getMessage());
                }
            }

            // Empty line for spacing
            sender.sendMessage("");
        }

        // Footer with current time
        sendColorizedMessage(sender, "events.debug.footer", "#current_time#", java.time.LocalDateTime.now().toString());
    }

    private String formatIntervalTime(int seconds) {
        if (seconds <= 0) return "0s";

        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, secs);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, secs);
        } else {
            return String.format("%ds", secs);
        }
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        List<String> commands = new ArrayList<>();

        if (args.length == 1) {
            commands.add("help");
            commands.add("list");
            commands.add("debug");
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
