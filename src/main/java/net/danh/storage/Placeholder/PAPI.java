package net.danh.storage.Placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.danh.storage.Event.BaseEvent;
import net.danh.storage.Event.EventType;
import net.danh.storage.Event.Events.CommunityEvent;
import net.danh.storage.Event.Events.DoubleDropEvent;
import net.danh.storage.Event.Events.MiningContestEvent;
import net.danh.storage.Manager.EventManager;
import net.danh.storage.Manager.ItemManager;
import net.danh.storage.Manager.MineManager;
import net.danh.storage.Storage;
import net.danh.storage.Utils.File;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class PAPI extends PlaceholderExpansion {
    @Override
    public @NotNull String getIdentifier() {
        return "storage";
    }

    @Override
    public @NotNull String getAuthor() {
        return Storage.getStorage().getDescription().getAuthors().toString();
    }

    @Override
    public @NotNull String getVersion() {
        return Storage.getStorage().getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player p, @NotNull String args) {
        if (p == null) return null;
        if (args.equalsIgnoreCase("status")) {
            return ItemManager.getStatus(p);
        }
        if (args.startsWith("storage_")) {
            String item = args.substring(8);
            return String.valueOf(MineManager.getPlayerBlock(p, item));
        }
        if (args.equalsIgnoreCase("max_storage")) {
            return String.valueOf(MineManager.getMaxBlock(p));
        }
        if (args.startsWith("price_")) {
            String material = args.substring(6);
            ConfigurationSection section = File.getConfig().getConfigurationSection("worth");
            if (section != null) {
                List<String> sell_list = new ArrayList<>(section.getKeys(false));
                if (sell_list.contains(material)) {
                    int worth = section.getInt(material);
                    return String.valueOf(worth);
                }
            }
        }

        // Event System Placeholders
        if (args.startsWith("event_")) {
            return handleEventPlaceholders(p, args);
        }
        if (args.startsWith("mining_contest_")) {
            return handleMiningContestPlaceholders(p, args);
        }
        if (args.startsWith("community_")) {
            return handleCommunityEventPlaceholders(p, args);
        }
        if (args.startsWith("double_drop_")) {
            return handleDoubleDropPlaceholders(p, args);
        }

        return null;
    }

    private String handleEventPlaceholders(Player p, String args) {
        String placeholder = args.substring(6);

        if (placeholder.equals("active")) {
            return hasAnyActiveEvent() ?
                    File.getMessage().getString("events.status.active") :
                    File.getMessage().getString("events.status.disabled");
        }

        if (placeholder.startsWith("active_")) {
            String eventTypeStr = placeholder.substring(7);
            EventType eventType = EventType.fromConfigKey(eventTypeStr);
            if (eventType != null) {
                BaseEvent event = EventManager.getAllEvents().get(eventType);
                return (event != null && event.isActive()) ?
                        File.getMessage().getString("events.status.active") :
                        File.getMessage().getString("events.status.disabled");
            }
            return File.getMessage().getString("events.status.disabled");
        }

        if (placeholder.startsWith("next_")) {
            String nextPlaceholder = placeholder.substring(5);
            return handleNextEventPlaceholders(nextPlaceholder);
        }

        // Handle general next event placeholders
        if (placeholder.equals("next_time")) {
            BaseEvent nextEvent = getNextScheduledEvent();
            if (nextEvent == null) return "N/A";
            return formatTime(getNextEventSeconds(nextEvent));
        }

        if (placeholder.equals("next_seconds")) {
            BaseEvent nextEvent = getNextScheduledEvent();
            if (nextEvent == null) return "0";
            return String.valueOf(getNextEventSeconds(nextEvent));
        }

        BaseEvent activeEvent = getFirstActiveEvent();
        if (activeEvent == null) return "N/A";

        switch (placeholder) {
            case "name":
                return getEventName(activeEvent);
            case "type":
                return activeEvent.getEventType().getDisplayName();
            case "remaining_time":
                return formatTime(getRemainingSeconds(activeEvent));
            case "remaining_seconds":
                return String.valueOf(getRemainingSeconds(activeEvent));
            case "duration":
                return String.valueOf(getEventDuration(activeEvent));
            case "start_time":
                return String.valueOf(activeEvent.getEventData().getStartTime());
            case "start_time_formatted":
                return formatTimestamp(activeEvent.getEventData().getStartTime(), "HH:mm:ss");
            case "start_date":
                return formatTimestamp(activeEvent.getEventData().getStartTime(), "dd/MM/yyyy");
            case "start_datetime":
                return formatTimestamp(activeEvent.getEventData().getStartTime(), "dd/MM/yyyy HH:mm:ss");
        }

        return null;
    }

    private String handleMiningContestPlaceholders(Player p, String args) {
        String placeholder = args.substring(15);

        BaseEvent event = EventManager.getAllEvents().get(EventType.MINING_CONTEST);
        if (event == null || !event.isActive() || !(event instanceof MiningContestEvent)) {
            return getDefaultValue(placeholder);
        }

        MiningContestEvent miningEvent = (MiningContestEvent) event;

        switch (placeholder) {
            case "rank":
                return String.valueOf(miningEvent.getPlayerRank(p));
            case "score":
                return String.valueOf(event.getEventData().getPlayerData(p));
            case "participants":
                return String.valueOf(event.getEventData().getParticipants().size());
        }

        if (placeholder.startsWith("top_") && placeholder.contains("_")) {
            String[] parts = placeholder.split("_");
            if (parts.length >= 3) {
                try {
                    int position = Integer.parseInt(parts[1]);
                    String type = parts[2];

                    List<Map.Entry<UUID, Integer>> leaderboard = event.getEventData().getPlayerData().entrySet()
                            .stream()
                            .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                            .collect(Collectors.toList());

                    if (position > 0 && position <= leaderboard.size()) {
                        Map.Entry<UUID, Integer> entry = leaderboard.get(position - 1);

                        if (type.equals("name")) {
                            Player player = Bukkit.getPlayer(entry.getKey());
                            return player != null ? player.getName() : "Unknown";
                        } else if (type.equals("score")) {
                            return String.valueOf(entry.getValue());
                        }
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return getDefaultValue(placeholder);
    }

    private String handleCommunityEventPlaceholders(Player p, String args) {
        String placeholder = args.substring(10);

        BaseEvent event = EventManager.getAllEvents().get(EventType.COMMUNITY_EVENT);
        if (event == null || !event.isActive() || !(event instanceof CommunityEvent)) {
            return getDefaultValue(placeholder);
        }

        CommunityEvent communityEvent = (CommunityEvent) event;

        switch (placeholder) {
            case "progress":
                return String.valueOf(communityEvent.getCurrentProgress());
            case "goal":
                return String.valueOf(communityEvent.getGoal());
            case "percentage":
                return String.format("%.1f", communityEvent.getProgressPercentage());
            case "participants":
                return String.valueOf(communityEvent.getParticipantCount());
            case "player_contribution":
                return String.valueOf(event.getEventData().getPlayerData(p));
        }

        return getDefaultValue(placeholder);
    }

    private String handleDoubleDropPlaceholders(Player p, String args) {
        String placeholder = args.substring(12);

        BaseEvent event = EventManager.getAllEvents().get(EventType.DOUBLE_DROP);
        if (event == null || !(event instanceof DoubleDropEvent)) {
            return getDefaultValue(placeholder);
        }

        DoubleDropEvent doubleDropEvent = (DoubleDropEvent) event;

        switch (placeholder) {
            case "player_blocks":
                return event.isActive() ? String.valueOf(event.getEventData().getPlayerData(p)) : "0";
            case "multiplier":
                return String.valueOf(doubleDropEvent.getMultiplier());
        }

        return getDefaultValue(placeholder);
    }

    private boolean hasAnyActiveEvent() {
        Map<EventType, BaseEvent> events = EventManager.getAllEvents();
        for (BaseEvent event : events.values()) {
            if (event.isActive()) {
                return true;
            }
        }
        return false;
    }

    private BaseEvent getFirstActiveEvent() {
        Map<EventType, BaseEvent> events = EventManager.getAllEvents();
        for (BaseEvent event : events.values()) {
            if (event.isActive()) {
                return event;
            }
        }
        return null;
    }

    private long getRemainingSeconds(BaseEvent event) {
        if (!event.isActive()) return 0;
        long endTime = event.getEventData().getEndTime();
        long currentTime = System.currentTimeMillis();
        return Math.max(0, (endTime - currentTime) / 1000);
    }

    private long getEventDuration(BaseEvent event) {
        if (!event.isActive()) return 0;
        long startTime = event.getEventData().getStartTime();
        long endTime = event.getEventData().getEndTime();
        return (endTime - startTime) / 1000;
    }

    private String formatTime(long seconds) {
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

    private String getDefaultValue(String placeholder) {
        if (placeholder.contains("active")) return File.getMessage().getString("events.status.disabled");
        if (placeholder.contains("percentage")) return "0.0";
        if (placeholder.contains("time")) return "0s";
        if (placeholder.contains("name")) return "N/A";
        return "0";
    }

    private String getEventName(BaseEvent event) {
        EventType eventType = event.getEventType();
        return File.getEventConfig().getString("events." + eventType.getConfigKey() + ".event_name", eventType.getDisplayName());
    }

    private String handleNextEventPlaceholders(String placeholder) {
        // Handle different placeholder formats
        if (placeholder.endsWith("_time") || placeholder.endsWith("_seconds") ||
                placeholder.endsWith("_datetime") || placeholder.endsWith("_date") ||
                placeholder.endsWith("_schedule_info")) {

            String eventTypeStr = extractEventTypeFromPlaceholder(placeholder);
            EventType eventType = EventType.fromConfigKey(eventTypeStr);

            if (eventType != null) {
                BaseEvent event = EventManager.getAllEvents().get(eventType);
                if (event != null) {
                    return formatEventPlaceholder(event, eventType, placeholder);
                }
            }
        }

        return getDefaultValue(placeholder);
    }

    private String extractEventTypeFromPlaceholder(String placeholder) {
        if (placeholder.endsWith("_time")) {
            return placeholder.substring(0, placeholder.lastIndexOf("_time"));
        } else if (placeholder.endsWith("_seconds")) {
            return placeholder.substring(0, placeholder.lastIndexOf("_seconds"));
        } else if (placeholder.endsWith("_datetime")) {
            return placeholder.substring(0, placeholder.lastIndexOf("_datetime"));
        } else if (placeholder.endsWith("_date")) {
            return placeholder.substring(0, placeholder.lastIndexOf("_date"));
        } else if (placeholder.endsWith("_schedule_info")) {
            return placeholder.substring(0, placeholder.lastIndexOf("_schedule_info"));
        }
        return placeholder;
    }

    private String formatEventPlaceholder(BaseEvent event, EventType eventType, String placeholder) {
        long nextSeconds = getNextEventSeconds(event);
        long nextTime = event.getEventData().getNextScheduledTime();

        // If event is active, return 0 or N/A
        if (event.isActive()) {
            if (placeholder.endsWith("_seconds")) return "0";
            return "N/A";
        }

        // If no next time scheduled, try to calculate from config
        if (nextTime <= 0) {
            String timingType = File.getEventConfig().getString("events." + eventType.getConfigKey() + ".timing.type", "interval");
            if ("schedule".equals(timingType)) {
                // Try to get next time from scheduler
                try {
                    java.time.LocalDateTime nextDateTime = EventManager.getScheduler().getNextScheduleTime(eventType);
                    if (nextDateTime != null) {
                        nextTime = nextDateTime.atZone(java.time.ZoneId.systemDefault()).toEpochSecond() * 1000L;
                        nextSeconds = Math.max(0, (nextTime - System.currentTimeMillis()) / 1000);
                    }
                } catch (Exception e) {
                    // Fallback to default
                }
            }
        }

        if (nextTime <= 0 || nextSeconds <= 0) {
            if (placeholder.endsWith("_seconds")) return "0";
            return "N/A";
        }

        if (placeholder.endsWith("_time")) {
            return formatTime(nextSeconds);
        } else if (placeholder.endsWith("_seconds")) {
            return String.valueOf(nextSeconds);
        } else if (placeholder.endsWith("_datetime")) {
            return formatTimestamp(nextTime, "dd/MM/yyyy HH:mm:ss");
        } else if (placeholder.endsWith("_date")) {
            return formatTimestamp(nextTime, "dd/MM/yyyy");
        } else if (placeholder.endsWith("_schedule_info")) {
            return getScheduleInfo(eventType);
        }

        return getDefaultValue(placeholder);
    }

    private String getScheduleInfo(EventType eventType) {
        String timingType = File.getEventConfig().getString("events." + eventType.getConfigKey() + ".timing.type", "interval");

        if ("schedule".equals(timingType)) {
            String schedule = File.getEventConfig().getString("events." + eventType.getConfigKey() + ".timing.schedule", "N/A");
            return schedule;
        } else if ("interval".equals(timingType)) {
            int interval = File.getEventConfig().getInt("events." + eventType.getConfigKey() + ".timing.interval", 0);
            return "Every " + formatTime(interval);
        }

        return "N/A";
    }

    private BaseEvent getNextScheduledEvent() {
        Map<EventType, BaseEvent> events = EventManager.getAllEvents();
        BaseEvent nextEvent = null;
        long earliestTime = Long.MAX_VALUE;

        for (BaseEvent event : events.values()) {
            if (!event.isActive()) {
                long nextTime = event.getEventData().getNextScheduledTime();
                if (nextTime > 0 && nextTime < earliestTime) {
                    earliestTime = nextTime;
                    nextEvent = event;
                }
            }
        }

        return nextEvent;
    }

    private long getNextEventSeconds(BaseEvent event) {
        if (event.isActive()) return 0;

        long nextTime = event.getEventData().getNextScheduledTime();
        long currentTime = System.currentTimeMillis();

        // If nextTime is valid, calculate seconds
        if (nextTime > 0) {
            return Math.max(0, (nextTime - currentTime) / 1000);
        }

        // If nextTime is not set, try to calculate from scheduler
        EventType eventType = event.getEventType();
        String timingType = File.getEventConfig().getString("events." + eventType.getConfigKey() + ".timing.type", "interval");

        if ("schedule".equals(timingType)) {
            try {
                java.time.LocalDateTime nextDateTime = EventManager.getScheduler().getNextScheduleTime(eventType);
                if (nextDateTime != null) {
                    long calculatedTime = nextDateTime.atZone(java.time.ZoneId.systemDefault()).toEpochSecond() * 1000L;
                    return Math.max(0, (calculatedTime - currentTime) / 1000);
                }
            } catch (Exception e) {
                // Log error if detailed logging is enabled
                if (File.getEventConfig().getBoolean("performance.detailed_logging", false)) {
                    Storage.getStorage().getLogger().warning("Failed to calculate next event time for " + eventType.getDisplayName() + ": " + e.getMessage());
                }
            }
        } else if ("interval".equals(timingType)) {
            // For interval events, return the interval time
            int interval = File.getEventConfig().getInt("events." + eventType.getConfigKey() + ".timing.interval", 0);
            return interval;
        }

        return 0;
    }

    private String formatTimestamp(long timestamp, String pattern) {
        if (timestamp <= 0) return "N/A";
        try {
            LocalDateTime dateTime = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(timestamp),
                    ZoneId.systemDefault()
            );
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            return dateTime.format(formatter);
        } catch (Exception e) {
            return "N/A";
        }
    }
}
