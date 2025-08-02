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
                } catch (NumberFormatException ignored) {}
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

        if (placeholder.equals("player_blocks")) {
            return event.isActive() ? String.valueOf(event.getEventData().getPlayerData(p)) : "0";
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
}
