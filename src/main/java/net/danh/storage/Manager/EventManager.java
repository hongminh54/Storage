package net.danh.storage.Manager;

import net.danh.storage.Event.*;
import net.danh.storage.Event.Events.CommunityEvent;
import net.danh.storage.Event.Events.DoubleDropEvent;
import net.danh.storage.Event.Events.MiningContestEvent;
import net.danh.storage.Storage;
import net.danh.storage.Utils.File;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class EventManager {
    private static final Map<EventType, BaseEvent> events = new HashMap<>();
    private static EventScheduler scheduler;
    private static boolean initialized = false;

    public static void initialize() {
        if (initialized) {
            return;
        }

        loadEvents();
        scheduler = new EventScheduler();
        scheduleAllEvents();
        initialized = true;

        Storage.getStorage().getLogger().log(Level.INFO, "Event system initialized successfully");
    }

    public static void shutdown() {
        if (!initialized) {
            return;
        }

        // Use force stop during shutdown for immediate termination
        stopAllEvents(true);
        if (scheduler != null) {
            scheduler.cancelAllScheduledEvents();
        }
        events.clear();
        initialized = false;

        Storage.getStorage().getLogger().log(Level.INFO, "Event system shutdown completed");
    }

    private static void loadEvents() {
        events.put(EventType.MINING_CONTEST, new MiningContestEvent());
        events.put(EventType.DOUBLE_DROP, new DoubleDropEvent());
        events.put(EventType.COMMUNITY_EVENT, new CommunityEvent());
    }

    private static void scheduleAllEvents() {
        for (Map.Entry<EventType, BaseEvent> entry : events.entrySet()) {
            EventType eventType = entry.getKey();
            BaseEvent event = entry.getValue();

            try {
                if (validateEventConfiguration(eventType) &&
                        File.getEventConfig().getBoolean("events." + eventType.getConfigKey() + ".enabled", true)) {
                    scheduler.scheduleEvent(eventType, event);
                }
            } catch (Exception e) {
                Storage.getStorage().getLogger().warning("Failed to schedule event " + eventType.getDisplayName() + ": " + e.getMessage());
            }
        }
    }

    private static boolean validateEventConfiguration(EventType eventType) {
        String basePath = "events." + eventType.getConfigKey();

        // Validate duration
        int duration = File.getEventConfig().getInt(basePath + ".duration", 1800);
        if (duration < 60 || duration > 86400) { // 1 minute to 24 hours
            Storage.getStorage().getLogger().warning("Invalid duration for " + eventType.getDisplayName() + ": " + duration + " seconds. Using default.");
            return false;
        }

        // Validate timing configuration
        String timingType = File.getEventConfig().getString(basePath + ".timing.type", "interval");
        if ("interval".equals(timingType)) {
            int interval = File.getEventConfig().getInt(basePath + ".timing.interval", 3600);
            if (interval < 300) { // Minimum 5 minutes
                Storage.getStorage().getLogger().warning("Invalid interval for " + eventType.getDisplayName() + ": " + interval + " seconds. Minimum is 300 seconds.");
                return false;
            }
        } else if ("schedule".equals(timingType)) {
            String schedule = File.getEventConfig().getString(basePath + ".timing.schedule", "");
            if (schedule.isEmpty()) {
                Storage.getStorage().getLogger().warning("Empty schedule for " + eventType.getDisplayName());
                return false;
            }
        }

        return true;
    }

    public static void reloadEvents() {
        if (!initialized) {
            return;
        }

        stopAllEvents();
        scheduler.cancelAllScheduledEvents();
        scheduleAllEvents();

        Storage.getStorage().getLogger().log(Level.INFO, "Event configurations reloaded");
    }

    public static boolean startEvent(EventType eventType) {
        BaseEvent event = events.get(eventType);
        if (event == null) {
            return false;
        }

        if (event.isActive()) {
            return false;
        }

        try {
            event.start();
            return true;
        } catch (Exception e) {
            Storage.getStorage().getLogger().log(Level.SEVERE,
                    "Failed to start event: " + eventType.getDisplayName(), e);
            return false;
        }
    }

    public static boolean stopEvent(EventType eventType) {
        return stopEvent(eventType, false);
    }

    public static boolean stopEvent(EventType eventType, boolean forceStop) {
        BaseEvent event = events.get(eventType);
        if (event == null) {
            return false;
        }

        if (!event.isActive()) {
            return false;
        }

        try {
            if (forceStop) {
                // Emergency stop - immediate termination
                event.forceStop();
                Storage.getStorage().getLogger().log(Level.INFO,
                        "Force stopped event: " + eventType.getDisplayName());
            } else {
                // Graceful stop - allow proper conclusion
                if (eventType == EventType.MINING_CONTEST) {
                    // For Mining Contest, trigger normal end process with winners and rewards
                    Storage.getStorage().getLogger().log(Level.INFO,
                            "Gracefully ending Mining Contest event with winner announcements");
                    event.end();
                } else {
                    // For other events, use force stop as before
                    event.forceStop();
                    Storage.getStorage().getLogger().log(Level.INFO,
                            "Manually stopped event: " + eventType.getDisplayName());
                }
            }
            return true;
        } catch (Exception e) {
            Storage.getStorage().getLogger().log(Level.SEVERE,
                    "Failed to stop event: " + eventType.getDisplayName(), e);
            return false;
        }
    }

    public static void stopAllEvents() {
        stopAllEvents(true);
    }

    public static void stopAllEvents(boolean forceStop) {
        for (Map.Entry<EventType, BaseEvent> entry : events.entrySet()) {
            BaseEvent event = entry.getValue();
            if (event.isActive()) {
                if (forceStop) {
                    event.forceStop();
                } else {
                    stopEvent(entry.getKey(), false);
                }
            }
        }
    }

    public static boolean isEventActive(EventType eventType) {
        BaseEvent event = events.get(eventType);
        return event != null && event.isActive();
    }

    public static BaseEvent getEvent(EventType eventType) {
        return events.get(eventType);
    }

    public static Map<EventType, BaseEvent> getAllEvents() {
        return new HashMap<>(events);
    }

    public static void onPlayerMine(Player player, String material, int amount) {
        if (!initialized) {
            return;
        }

        for (BaseEvent event : events.values()) {
            if (event.isActive()) {
                try {
                    event.onPlayerMine(player, material, amount);
                } catch (Exception e) {
                    Storage.getStorage().getLogger().log(Level.WARNING,
                            "Error processing mining event for " + event.getEventType().getDisplayName(), e);
                }
            }
        }
    }

    public static int calculateDoubleDropBonus(int originalAmount) {
        DoubleDropEvent doubleDropEvent = (DoubleDropEvent) events.get(EventType.DOUBLE_DROP);
        if (doubleDropEvent != null && doubleDropEvent.isActive()) {
            return doubleDropEvent.calculateBonusAmount(originalAmount);
        }
        return 0;
    }

    public static boolean isDoubleDropActive() {
        return isEventActive(EventType.DOUBLE_DROP);
    }

    public static boolean isMiningContestActive() {
        return isEventActive(EventType.MINING_CONTEST);
    }

    public static boolean isCommunityEventActive() {
        return isEventActive(EventType.COMMUNITY_EVENT);
    }

    public static String getEventStatus(EventType eventType) {
        BaseEvent event = events.get(eventType);
        if (event == null) {
            return File.getMessage().getString("events.status.unknown");
        }

        if (event.isActive()) {
            long remainingTime = (event.getEventData().getEndTime() - System.currentTimeMillis()) / 1000;
            if (remainingTime > 0) {
                return File.getMessage().getString("events.status.active_with_time")
                        .replace("#time#", formatTime(remainingTime));
            } else {
                return File.getMessage().getString("events.status.active");
            }
        }

        if (scheduler != null && scheduler.isEventScheduled(eventType)) {
            return File.getMessage().getString("events.status.scheduled");
        }

        boolean enabled = File.getEventConfig().getBoolean("events." + eventType.getConfigKey() + ".enabled", true);
        return enabled ? File.getMessage().getString("events.status.enabled") :
                File.getMessage().getString("events.status.disabled");
    }

    private static String formatTime(long seconds) {
        if (seconds <= 0) return "0s";
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (hours > 0) {
            return hours + "h " + minutes + "m";
        } else if (minutes > 0) {
            return minutes + "m " + secs + "s";
        } else {
            return secs + "s";
        }
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static boolean canGracefullyStop(EventType eventType) {
        // Currently only Mining Contest supports graceful stopping
        return eventType == EventType.MINING_CONTEST;
    }

    public static String getStopMethodDescription(EventType eventType) {
        if (canGracefullyStop(eventType)) {
            return File.getMessage().getString("events.stop.method.graceful");
        } else {
            return File.getMessage().getString("events.stop.method.immediate");
        }
    }
}
