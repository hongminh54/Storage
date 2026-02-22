package net.danh.storage.Placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.danh.storage.Data.MythicTransferData;
import net.danh.storage.Database.MythicTransferDatabase;
import net.danh.storage.Event.BaseEvent;
import net.danh.storage.Event.EventType;
import net.danh.storage.Event.Events.CommunityEvent;
import net.danh.storage.Event.Events.DoubleDropEvent;
import net.danh.storage.Event.Events.MiningContestEvent;
import net.danh.storage.GUI.Mythic.MythicStorageGUI;
import net.danh.storage.GUI.PersonalStorage;
import net.danh.storage.Manager.Event.EventManager;
import net.danh.storage.Manager.ItemManager;
import net.danh.storage.Manager.MineManager;
import net.danh.storage.Manager.Mythic.MythicStorageManager;
import net.danh.storage.Manager.Mythic.MythicTransferManager;
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
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class PAPI extends PlaceholderExpansion {

    private static final Set<String> LOGGED_KEYS = new HashSet<>();

    private static void logOnce(String key, String message, Exception e) {
        synchronized (LOGGED_KEYS) {
            if (!LOGGED_KEYS.add(key)) {
                return;
            }
        }
        Storage.getStorage().getLogger().log(Level.WARNING, message, e);
    }

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

        if (args.startsWith("sellable")) {
            return handleSellablePlaceholders(args);
        }

        if (args.startsWith("page_")) {
            return handlePagePlaceholders(p, args.substring(5));
        }

        // Storage percentage
        if (args.equalsIgnoreCase("percentage")) {
            return String.valueOf(getStoragePercentage(p));
        }

        // Storage statistics
        if (args.startsWith("total_") || args.startsWith("available_") || args.startsWith("transfer_")) {
            return handleStorageStatistics(p, args);
        }

        if (args.equalsIgnoreCase("status")) {
            return ItemManager.getStatus(p);
        }

        // Storage with formatted/percentage variants
        if (args.startsWith("storage_")) {
            String item = args.substring(8);

            // %storage_<material>_formatted%
            if (item.endsWith("_formatted")) {
                String material = item.substring(0, item.length() - 10);
                return formatNumber(MineManager.getPlayerBlock(p, material));
            }

            // %storage_<material>_percentage%
            if (item.endsWith("_percentage")) {
                String material = item.substring(0, item.length() - 11);
                return String.valueOf(getMaterialPercentage(p, material));
            }

            return String.valueOf(MineManager.getPlayerBlock(p, item));
        }

        if (args.startsWith("crop_sellable")) {
            return handleCropSellablePlaceholders(args.substring("crop_sellable".length()));
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

        // MythicStorage Placeholders
        if (args.startsWith("mythic_")) {
            return handleMythicStoragePlaceholders(p, args);
        }

        // Storage Leaderboard Placeholders
        if (args.startsWith("top_")) {
            return handleStorageLeaderboardPlaceholders(args);
        }

        return null;
    }

    private String handleSellablePlaceholders(@NotNull String args) {
        if (args.equalsIgnoreCase("sellable")) {
            return getSellableSymbol(null, null, false);
        }

        if (args.startsWith("sellable_")) {
            String rawItem = args.substring("sellable_".length());
            String itemKey = normalizeWorthKey(rawItem);
            boolean sellable = isStorageWorthSellable(itemKey);
            return getSellableSymbol(itemKey, rawItem, sellable);
        }

        return null;
    }

    private String handleCropSellablePlaceholders(@NotNull String suffix) {
        if (suffix.isEmpty()) {
            return getCropSellableSymbol(null, null, false);
        }

        if (suffix.startsWith("_")) {
            String rawItem = suffix.substring(1);
            String itemKey = normalizeWorthKey(rawItem);
            boolean sellable = isCropWorthSellable(itemKey);
            return getCropSellableSymbol(itemKey, rawItem, sellable);
        }

        return null;
    }

    private boolean isStorageWorthSellable(@NotNull String itemKey) {
        ConfigurationSection section = File.getConfig().getConfigurationSection("worth");
        if (section == null) {
            return false;
        }
        String worthKey = resolveWorthKey(section, itemKey);
        if (worthKey == null) {
            return false;
        }
        return section.getDouble(worthKey) > 0;
    }

    private boolean isCropWorthSellable(@NotNull String itemKey) {
        ConfigurationSection section = File.getCropStorageConfig().getConfigurationSection("worth");
        if (section == null) {
            return false;
        }
        String worthKey = resolveWorthKey(section, itemKey);
        if (worthKey == null) {
            return false;
        }
        return section.getDouble(worthKey) > 0;
    }

    private String getSellableSymbol(@Nullable String itemKey, @Nullable String rawItem, boolean sellable) {
        String key = sellable ? "user.sellable.yes" : "user.sellable.no";
        String value = File.getMessage().getString(key, sellable ? "&a✔" : "&c✘");
        if (rawItem != null) {
            value = value.replace("#item#", rawItem);
        }
        return value;
    }

    private String getCropSellableSymbol(@Nullable String itemKey, @Nullable String rawItem, boolean sellable) {
        String key = sellable ? "cropstorage.sellable.yes" : "cropstorage.sellable.no";
        String value = File.getMessage().getString(key, sellable ? "&a✔" : "&c✘");
        if (rawItem != null) {
            value = value.replace("#item#", rawItem);
        }
        return value;
    }

    private String normalizeWorthKey(@NotNull String rawItem) {
        return rawItem.replace(":", ";");
    }

    private String resolveWorthKey(@NotNull ConfigurationSection section, @NotNull String materialData) {
        if (section.contains(materialData)) {
            return materialData;
        }

        if (materialData.endsWith(";0")) {
            String noData = materialData.substring(0, materialData.length() - 2);
            if (section.contains(noData)) {
                return noData;
            }
        }

        String withZero = materialData + ";0";
        if (section.contains(withZero)) {
            return withZero;
        }

        return null;
    }

    private String handlePagePlaceholders(Player p, String placeholder) {
        if (placeholder.equalsIgnoreCase("storage_current")) {
            return String.valueOf(PersonalStorage.getPlayerCurrentPage(p) + 1);
        }

        if (placeholder.equalsIgnoreCase("storage_total")) {
            return String.valueOf(getStorageTotalPages());
        }

        if (placeholder.equalsIgnoreCase("mythic_current")) {
            return String.valueOf(MythicStorageGUI.getPlayerCurrentPage(p) + 1);
        }

        if (placeholder.equalsIgnoreCase("mythic_total")) {
            return String.valueOf(getMythicTotalPages());
        }

        return null;
    }

    private int getStorageTotalPages() {
        try {
            String slotConfig = File.getGUIStorage().getString("items.storage_item.slot");
            if (slotConfig == null || slotConfig.trim().isEmpty()) {
                return 1;
            }
            int itemsPerPage = slotConfig.replace(" ", "").split(",").length;
            if (itemsPerPage <= 0) {
                return 1;
            }
            int totalItems = MineManager.getOrderedPluginBlocks().size();
            return Math.max(1, (int) Math.ceil((double) totalItems
                    / (double) itemsPerPage));
        } catch (Exception e) {
            logOnce("papi.storage_total_pages",
                    "[Storage] Failed to calculate storage total pages",
                    e);
            return 1;
        }
    }

    private int getMythicTotalPages() {
        try {
            String slotConfig = File.getMythicStorageGUIConfig()
                    .getString("items.mythic_item.slot");
            if (slotConfig == null || slotConfig.trim().isEmpty()) {
                return 1;
            }
            int itemsPerPage = slotConfig.replace(" ", "").split(",").length;
            if (itemsPerPage <= 0) {
                return 1;
            }
            int totalItems = MythicStorageManager.getConfiguredDrops().size();
            return Math.max(1, (int) Math.ceil((double) totalItems
                    / (double) itemsPerPage));
        } catch (Exception e) {
            logOnce("papi.mythic_total_pages",
                    "[Storage] Failed to calculate mythic storage total pages",
                    e);
            return 1;
        }
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
                    logOnce("papi.event_next_time." + eventType.getConfigKey(),
                            "[Storage] Failed to calculate next schedule time for event: "
                                    + eventType.getDisplayName(),
                            e);
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
            logOnce("papi.format_timestamp." + pattern,
                    "[Storage] Failed to format timestamp with pattern: " + pattern,
                    e);
            return "N/A";
        }
    }

    // MythicStorage Placeholder Handlers
    private String handleMythicStoragePlaceholders(Player p, String args) {
        if (!MythicStorageManager.isSystemEnabled()) {
            return "0";
        }

        String placeholder = args.substring(7); // Remove "mythic_"

        // %storage_mythic_percentage% - MythicStorage percentage
        if (placeholder.equals("percentage")) {
            return String.valueOf(getMythicStoragePercentage(p));
        }

        // %storage_mythic_total_items% - Total unique items
        if (placeholder.equals("total_items")) {
            Map<String, Integer> items = MythicStorageManager.getPlayerAllItems(p);
            return String.valueOf(items.size());
        }

        // %storage_mythic_total_amount% - Total amount of all items
        if (placeholder.equals("total_amount")) {
            Map<String, Integer> items = MythicStorageManager.getPlayerAllItems(p);
            int total = items.values().stream().mapToInt(Integer::intValue).sum();
            return String.valueOf(total);
        }

        // %storage_mythic_total_amount_formatted%
        if (placeholder.equals("total_amount_formatted")) {
            Map<String, Integer> items = MythicStorageManager.getPlayerAllItems(p);
            int total = items.values().stream().mapToInt(Integer::intValue).sum();
            return formatNumber(total);
        }

        // %storage_mythic_max_storage% - Max storage capacity
        if (placeholder.equals("max_storage")) {
            return String.valueOf(MythicStorageManager.getMaxStorage(p));
        }

        // %storage_mythic_available_space%
        if (placeholder.equals("available_space")) {
            Map<String, Integer> items = MythicStorageManager.getPlayerAllItems(p);
            int total = items.values().stream().mapToInt(Integer::intValue).sum();
            int max = MythicStorageManager.getMaxStorage(p);
            return String.valueOf(Math.max(0, max - total));
        }

        // %storage_mythic_autopickup_status% - Auto-pickup status
        if (placeholder.equals("autopickup_status")) {
            boolean status = MythicStorageManager.getToggleStatus(p);
            return status ?
                    File.getMessage().getString("mythicstorage.status_enabled", "Enabled") :
                    File.getMessage().getString("mythicstorage.status_disabled", "Disabled");
        }

        // Transfer statistics placeholders
        if (placeholder.startsWith("transfer_")) {
            return handleMythicTransferPlaceholders(p, placeholder.substring(9));
        }

        // Leaderboard placeholders
        if (placeholder.startsWith("top_")) {
            return handleMythicLeaderboardPlaceholders(placeholder.substring(4));
        }

        // %storage_mythic_<item>_amount_formatted% - Check formatted first (longer suffix)
        if (placeholder.endsWith("_amount_formatted")) {
            String itemName = placeholder.substring(0, placeholder.length() - 17);
            if (MythicStorageManager.isConfiguredDrop(itemName)) {
                return formatNumber(MythicStorageManager.getPlayerItem(p, itemName));
            }
            return "0";
        }

        // %storage_mythic_<item>_amount% - Get specific item amount
        if (placeholder.endsWith("_amount")) {
            String itemName = placeholder.substring(0, placeholder.length() - 7); // Remove "_amount"
            if (MythicStorageManager.isConfiguredDrop(itemName)) {
                return String.valueOf(MythicStorageManager.getPlayerItem(p, itemName));
            }
            return "0";
        }

        // %storage_mythic_<item>% - Get specific item display name
        if (!placeholder.startsWith("total_") && !placeholder.startsWith("autopickup_") &&
                !placeholder.startsWith("max_") && !placeholder.startsWith("transfer_") &&
                !placeholder.startsWith("top_") && !placeholder.startsWith("available_") &&
                !placeholder.equals("percentage")) {
            String itemName = placeholder;
            if (MythicStorageManager.isConfiguredDrop(itemName)) {
                return MythicStorageManager.getItemDisplayNameOrId(itemName, p);
            }
            return itemName;
        }

        return "0";
    }

    private String handleMythicTransferPlaceholders(Player p, String placeholder) {
        MythicTransferDatabase transferDb = MythicTransferManager.getTransferDatabase();
        if (transferDb == null) {
            return "0";
        }

        // %storage_mythic_transfer_sent_total% - Total items sent
        if (placeholder.equals("sent_total")) {
            List<MythicTransferData> transfers = transferDb.getTransferHistory(p.getName(), Integer.MAX_VALUE, 0);
            int total = transfers.stream()
                    .filter(t -> t.getSender().equalsIgnoreCase(p.getName()))
                    .filter(t -> t.getStatus().startsWith("SUCCESS"))
                    .mapToInt(MythicTransferData::getAmount)
                    .sum();
            return String.valueOf(total);
        }

        // %storage_mythic_transfer_received_total% - Total items received
        if (placeholder.equals("received_total")) {
            List<MythicTransferData> transfers = transferDb.getTransferHistory(p.getName(), Integer.MAX_VALUE, 0);
            int total = transfers.stream()
                    .filter(t -> t.getReceiver().equalsIgnoreCase(p.getName()))
                    .filter(t -> t.getStatus().startsWith("SUCCESS"))
                    .mapToInt(MythicTransferData::getAmount)
                    .sum();
            return String.valueOf(total);
        }

        // %storage_mythic_transfer_count% - Total transfer count
        if (placeholder.equals("count")) {
            return String.valueOf(transferDb.getTotalTransferCount(p.getName()));
        }

        return "0";
    }

    private String handleMythicLeaderboardPlaceholders(String placeholder) {
        // %storage_mythic_top_<item>_<position>_name% or %storage_mythic_top_<item>_<position>_amount%
        String[] parts = placeholder.split("_");
        if (parts.length < 3) {
            return "N/A";
        }

        try {
            int position = Integer.parseInt(parts[parts.length - 2]);
            String type = parts[parts.length - 1]; // "name" or "amount"

            // Extract item name (everything between "top_" and "_<position>_<type>")
            StringBuilder itemNameBuilder = new StringBuilder();
            for (int i = 0; i < parts.length - 2; i++) {
                if (i > 0) itemNameBuilder.append("_");
                itemNameBuilder.append(parts[i]);
            }
            String itemName = itemNameBuilder.toString();

            if (!MythicStorageManager.isConfiguredDrop(itemName)) {
                return "N/A";
            }

            // Get all players' data for this item
            Map<String, Integer> leaderboard = new HashMap<>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                int amount = MythicStorageManager.getPlayerItem(online, itemName);
                if (amount > 0) {
                    leaderboard.put(online.getName(), amount);
                }
            }

            // Sort by amount descending
            List<Map.Entry<String, Integer>> sortedList = leaderboard.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .collect(Collectors.toList());

            if (position > 0 && position <= sortedList.size()) {
                Map.Entry<String, Integer> entry = sortedList.get(position - 1);
                if (type.equals("name")) {
                    return entry.getKey();
                } else if (type.equals("amount")) {
                    return String.valueOf(entry.getValue());
                }
            }

        } catch (NumberFormatException ignored) {
        }

        return "N/A";
    }

    // Utility Methods for Formatting and Calculations
    private String formatNumber(int number) {
        if (number >= 1_000_000_000) {
            return String.format("%.1fB", number / 1_000_000_000.0);
        } else if (number >= 1_000_000) {
            return String.format("%.1fM", number / 1_000_000.0);
        } else if (number >= 1_000) {
            return String.format("%.1fK", number / 1_000.0);
        }
        return String.valueOf(number);
    }

    private int getStoragePercentage(Player p) {
        int total = 0;
        List<String> allBlocks = MineManager.getPluginBlocks();
        for (String block : allBlocks) {
            total += MineManager.getPlayerBlock(p, block);
        }
        int max = MineManager.getMaxBlock(p);
        if (max <= 0) return 0;
        return Math.min(100, (int) ((total * 100.0) / max));
    }

    private int getMaterialPercentage(Player p, String material) {
        int amount = MineManager.getPlayerBlock(p, material);
        int max = MineManager.getMaxBlock(p);
        if (max <= 0) return 0;
        return Math.min(100, (int) ((amount * 100.0) / max));
    }

    private int getMythicStoragePercentage(Player p) {
        Map<String, Integer> items = MythicStorageManager.getPlayerAllItems(p);
        int total = items.values().stream().mapToInt(Integer::intValue).sum();
        int max = MythicStorageManager.getMaxStorage(p);
        if (max <= 0) return 0;
        return Math.min(100, (int) ((total * 100.0) / max));
    }

    // Storage Statistics Handler
    private String handleStorageStatistics(Player p, String args) {
        // %storage_total_materials%
        if (args.equals("total_materials")) {
            int count = 0;
            List<String> allBlocks = MineManager.getPluginBlocks();
            for (String block : allBlocks) {
                if (MineManager.getPlayerBlock(p, block) > 0) {
                    count++;
                }
            }
            return String.valueOf(count);
        }

        // %storage_total_blocks%
        if (args.equals("total_blocks")) {
            int total = 0;
            List<String> allBlocks = MineManager.getPluginBlocks();
            for (String block : allBlocks) {
                total += MineManager.getPlayerBlock(p, block);
            }
            return String.valueOf(total);
        }

        // %storage_total_blocks_formatted%
        if (args.equals("total_blocks_formatted")) {
            int total = 0;
            List<String> allBlocks = MineManager.getPluginBlocks();
            for (String block : allBlocks) {
                total += MineManager.getPlayerBlock(p, block);
            }
            return formatNumber(total);
        }

        // %storage_available_space%
        if (args.equals("available_space")) {
            int total = 0;
            List<String> allBlocks = MineManager.getPluginBlocks();
            for (String block : allBlocks) {
                total += MineManager.getPlayerBlock(p, block);
            }
            int max = MineManager.getMaxBlock(p);
            return String.valueOf(Math.max(0, max - total));
        }

        // %storage_available_space_formatted%
        if (args.equals("available_space_formatted")) {
            int total = 0;
            List<String> allBlocks = MineManager.getPluginBlocks();
            for (String block : allBlocks) {
                total += MineManager.getPlayerBlock(p, block);
            }
            int max = MineManager.getMaxBlock(p);
            return formatNumber(Math.max(0, max - total));
        }

        // Transfer statistics (if TransferManager exists)
        if (args.startsWith("transfer_")) {
            return handleTransferStatistics(p, args.substring(9));
        }

        return "0";
    }

    private String handleTransferStatistics(Player p, String placeholder) {
        net.danh.storage.Database.TransferDatabase transferDb = net.danh.storage.Manager.TransferManager.getTransferDatabase();
        if (transferDb == null) {
            return "0";
        }

        // %storage_transfer_sent_total%
        if (placeholder.equals("sent_total")) {
            List<net.danh.storage.Data.TransferData> transfers = transferDb.getTransferHistory(p.getName(), Integer.MAX_VALUE, 0);
            int total = transfers.stream()
                    .filter(t -> t.getSender().equalsIgnoreCase(p.getName()))
                    .filter(t -> t.getStatus().startsWith("SUCCESS"))
                    .mapToInt(net.danh.storage.Data.TransferData::getAmount)
                    .sum();
            return String.valueOf(total);
        }

        // %storage_transfer_received_total%
        if (placeholder.equals("received_total")) {
            List<net.danh.storage.Data.TransferData> transfers = transferDb.getTransferHistory(p.getName(), Integer.MAX_VALUE, 0);
            int total = transfers.stream()
                    .filter(t -> t.getReceiver().equalsIgnoreCase(p.getName()))
                    .filter(t -> t.getStatus().startsWith("SUCCESS"))
                    .mapToInt(net.danh.storage.Data.TransferData::getAmount)
                    .sum();
            return String.valueOf(total);
        }

        // %storage_transfer_count%
        if (placeholder.equals("count")) {
            return String.valueOf(transferDb.getTotalTransferCount(p.getName()));
        }

        return "0";
    }

    // Storage Leaderboard Placeholder Handler
    private String handleStorageLeaderboardPlaceholders(String args) {
        // %storage_top_<material>_<position>_name% or %storage_top_<material>_<position>_amount%
        // %storage_top_all_<position>_name% or %storage_top_all_<position>_amount%
        String placeholder = args.substring(4); // Remove "top_"
        String[] parts = placeholder.split("_");

        if (parts.length < 3) {
            return "N/A";
        }

        try {
            int position = Integer.parseInt(parts[parts.length - 2]);
            String type = parts[parts.length - 1]; // "name" or "amount"

            // Extract material name (everything between "top_" and "_<position>_<type>")
            StringBuilder materialBuilder = new StringBuilder();
            for (int i = 0; i < parts.length - 2; i++) {
                if (i > 0) materialBuilder.append("_");
                materialBuilder.append(parts[i]);
            }
            String material = materialBuilder.toString();

            // Handle "all" - total of all blocks
            if (material.equalsIgnoreCase("all")) {
                return handleAllBlocksLeaderboard(position, type);
            }

            // Handle specific material
            return handleSpecificMaterialLeaderboard(material, position, type);

        } catch (NumberFormatException ignored) {
        }

        return "N/A";
    }

    private String handleAllBlocksLeaderboard(int position, String type) {
        // Calculate total blocks for all online players
        Map<String, Integer> leaderboard = new HashMap<>();

        for (Player online : Bukkit.getOnlinePlayers()) {
            int totalBlocks = 0;
            List<String> allBlocks = MineManager.getPluginBlocks();

            for (String block : allBlocks) {
                totalBlocks += MineManager.getPlayerBlock(online, block);
            }

            if (totalBlocks > 0) {
                leaderboard.put(online.getName(), totalBlocks);
            }
        }

        return getLeaderboardResult(leaderboard, position, type);
    }

    private String handleSpecificMaterialLeaderboard(String material, int position, String type) {
        // Validate material exists in config
        if (!MineManager.getPluginBlocks().contains(material)) {
            return "N/A";
        }

        // Get all online players' data for this material
        Map<String, Integer> leaderboard = new HashMap<>();

        for (Player online : Bukkit.getOnlinePlayers()) {
            int amount = MineManager.getPlayerBlock(online, material);
            if (amount > 0) {
                leaderboard.put(online.getName(), amount);
            }
        }

        return getLeaderboardResult(leaderboard, position, type);
    }

    private String getLeaderboardResult(Map<String, Integer> leaderboard, int position, String type) {
        // Sort by amount descending
        List<Map.Entry<String, Integer>> sortedList = leaderboard.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(Collectors.toList());

        if (position > 0 && position <= sortedList.size()) {
            Map.Entry<String, Integer> entry = sortedList.get(position - 1);
            if (type.equals("name")) {
                return entry.getKey();
            } else if (type.equals("amount")) {
                return String.valueOf(entry.getValue());
            }
        }

        return "N/A";
    }

}
