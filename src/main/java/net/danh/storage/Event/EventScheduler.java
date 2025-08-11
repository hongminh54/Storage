package net.danh.storage.Event;

import net.danh.storage.Manager.EventManager;
import net.danh.storage.Storage;
import net.danh.storage.Utils.Chat;
import net.danh.storage.Utils.File;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventScheduler {
    private final Map<EventType, BukkitRunnable> scheduledTasks;

    public EventScheduler() {
        this.scheduledTasks = new HashMap<>();
    }

    public void scheduleEvent(EventType eventType, BaseEvent event) {
        cancelScheduledEvent(eventType);

        String timingType = File.getEventConfig().getString("events." + eventType.getConfigKey() + ".timing.type", "interval");

        if ("interval".equals(timingType)) {
            scheduleIntervalEvent(eventType, event);
        } else if ("schedule".equals(timingType)) {
            scheduleTimeBasedEvent(eventType, event);
        }
    }

    private void scheduleIntervalEvent(EventType eventType, BaseEvent event) {
        int interval = File.getEventConfig().getInt("events." + eventType.getConfigKey() + ".timing.interval", 3600);

        if (interval < 300) { // Minimum 5 minutes
            Storage.getStorage().getLogger().warning("Interval too short for " + eventType.getDisplayName() + ": " + interval + "s. Using minimum 300s.");
            interval = 300;
        }

        final int finalInterval = interval; // Make it final for inner class
        long intervalTicks = finalInterval * 20L;
        long nextScheduledTime = System.currentTimeMillis() + (finalInterval * 1000L);
        event.getEventData().setNextScheduledTime(nextScheduledTime);

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    if (File.getEventConfig().getBoolean("events." + eventType.getConfigKey() + ".enabled", true) &&
                            !event.isActive()) {
                        event.start();
                        // Update next scheduled time after starting
                        long nextTime = System.currentTimeMillis() + (finalInterval * 1000L);
                        event.getEventData().setNextScheduledTime(nextTime);
                    }
                } catch (Exception e) {
                    Storage.getStorage().getLogger().warning("Error starting scheduled event " + eventType.getDisplayName() + ": " + e.getMessage());
                }
            }
        };

        task.runTaskTimer(Storage.getStorage(), intervalTicks, intervalTicks);
        scheduledTasks.put(eventType, task);
    }

    private void scheduleTimeBasedEvent(EventType eventType, BaseEvent event) {
        String scheduleString = File.getEventConfig().getString("events." + eventType.getConfigKey() + ".timing.schedule", "");

        if (scheduleString.isEmpty()) {
            return;
        }

        // Find the next earliest schedule time from all schedules
        scheduleNextEarliestOccurrence(eventType, event, scheduleString);
    }

    private void scheduleNextEarliestOccurrence(EventType eventType, BaseEvent event, String scheduleString) {
        try {
            LocalDateTime nextRun = findNextScheduleTime(scheduleString);
            if (nextRun == null) {
                Storage.getStorage().getLogger().warning("No valid schedule found for " + eventType.getDisplayName());
                return;
            }

            LocalDateTime now = LocalDateTime.now();
            long delayTicks = (nextRun.atZone(ZoneId.systemDefault()).toEpochSecond() -
                    now.atZone(ZoneId.systemDefault()).toEpochSecond()) * 20L;

            // Always set nextScheduledTime, even if delayTicks <= 0
            long nextScheduledTime = nextRun.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000L;
            event.getEventData().setNextScheduledTime(nextScheduledTime);

            // Log scheduling information
            if (File.getEventConfig().getBoolean("performance.detailed_logging", false)) {
                Storage.getStorage().getLogger().info("Scheduled " + eventType.getDisplayName() +
                        " for " + nextRun + " (in " + (delayTicks / 20) + " seconds)");
            }

            if (delayTicks > 0) {
                scheduleStartReminders(eventType, delayTicks);

                BukkitRunnable task = new BukkitRunnable() {
                    @Override
                    public void run() {
                        try {
                            if (File.getEventConfig().getBoolean("events." + eventType.getConfigKey() + ".enabled", true) &&
                                    !event.isActive()) {
                                event.start();
                            }
                        } catch (Exception e) {
                            Storage.getStorage().getLogger().warning("Error starting scheduled event " + eventType.getDisplayName() + ": " + e.getMessage());
                        } finally {
                            // Schedule the next occurrence after this event starts
                            scheduleNextEarliestOccurrence(eventType, event, scheduleString);
                        }
                    }
                };

                task.runTaskLater(Storage.getStorage(), delayTicks);
                scheduledTasks.put(eventType, task);
            } else {
                // If delay is 0 or negative, schedule for next occurrence immediately
                scheduleNextEarliestOccurrence(eventType, event, scheduleString);
            }
        } catch (Exception e) {
            Storage.getStorage().getLogger().warning("Failed to schedule event " + eventType.getConfigKey() + ": " + e.getMessage());
        }
    }

    private LocalDateTime findNextScheduleTime(String scheduleString) {
        String[] schedules = scheduleString.split(",");
        LocalDateTime earliestTime = null;
        LocalDateTime now = LocalDateTime.now();

        for (String schedule : schedules) {
            schedule = schedule.trim();
            try {
                String[] parts = schedule.split(":");
                if (parts.length != 3) {
                    Storage.getStorage().getLogger().warning("Invalid schedule format: " + schedule + " (expected DAY:HH:MM)");
                    continue;
                }

                String dayPart = parts[0].trim();
                int hour = Integer.parseInt(parts[1].trim());
                int minute = Integer.parseInt(parts[2].trim());

                if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                    Storage.getStorage().getLogger().warning("Invalid time values: " + hour + ":" + minute);
                    continue;
                }

                LocalDateTime nextRun = calculateNextRun(dayPart, hour, minute, now);

                if (earliestTime == null || nextRun.isBefore(earliestTime)) {
                    earliestTime = nextRun;
                }

                // Debug logging
                if (File.getEventConfig().getBoolean("performance.detailed_logging", false)) {
                    Storage.getStorage().getLogger().info("Schedule '" + schedule + "' next run: " + nextRun);
                }
            } catch (NumberFormatException e) {
                Storage.getStorage().getLogger().warning("Invalid number in schedule: " + schedule);
            } catch (Exception e) {
                Storage.getStorage().getLogger().warning("Failed to parse schedule: " + schedule + " - " + e.getMessage());
            }
        }

        return earliestTime;
    }

    private LocalDateTime calculateNextRun(String dayPart, int hour, int minute, LocalDateTime now) {
        LocalTime targetTime = LocalTime.of(hour, minute);

        if (dayPart.equalsIgnoreCase("DAILY")) {
            LocalDateTime today = now.toLocalDate().atTime(targetTime);
            return today.isAfter(now) ? today : today.plusDays(1);
        }

        try {
            DayOfWeek targetDay = DayOfWeek.valueOf(dayPart.toUpperCase());
            LocalDateTime candidate = now.toLocalDate().atTime(targetTime);

            // Find next occurrence of target day that is after current time
            while (candidate.getDayOfWeek() != targetDay || !candidate.isAfter(now)) {
                candidate = candidate.plusDays(1);
            }

            return candidate;
        } catch (IllegalArgumentException e) {
            // Invalid day, default to tomorrow at target time
            return now.plusDays(1).toLocalDate().atTime(targetTime);
        }
    }

    private void scheduleStartReminders(EventType eventType, long eventDelayTicks) {
        if (!File.getEventConfig().getBoolean("notifications.reminders.enabled", true)) {
            return;
        }

        List<Integer> reminderTimes = File.getEventConfig().getIntegerList("notifications.reminders.before_start");
        if (reminderTimes.isEmpty()) {
            reminderTimes = List.of(300, 60, 10); // Default: 5min, 1min, 10sec
        }

        long eventDelaySeconds = eventDelayTicks / 20L;

        for (int reminderTime : reminderTimes) {
            if (reminderTime < eventDelaySeconds) {
                long reminderDelayTicks = eventDelayTicks - (reminderTime * 20L);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        broadcastStartReminder(eventType, reminderTime);
                    }
                }.runTaskLater(Storage.getStorage(), reminderDelayTicks);
            }
        }
    }

    private void broadcastStartReminder(EventType eventType, int secondsLeft) {
        String eventName = File.getEventConfig().getString("events." + eventType.getConfigKey() + ".event_name", eventType.getDisplayName());
        String message = File.getMessage().getString("events.common.chat.reminder_start");
        message = message.replace("#event#", eventName)
                .replace("#time#", formatTime(secondsLeft));

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(Chat.colorizewp(message));
        }
    }

    private String formatTime(long seconds) {
        if (seconds <= 0) return "0s";
        long minutes = seconds / 60;
        long secs = seconds % 60;
        return minutes > 0 ? minutes + "m " + secs + "s" : secs + "s";
    }

    public void cancelScheduledEvent(EventType eventType) {
        BukkitRunnable task = scheduledTasks.remove(eventType);
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }

    public void cancelAllScheduledEvents() {
        for (BukkitRunnable task : scheduledTasks.values()) {
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
        }
        scheduledTasks.clear();
    }

    public boolean isEventScheduled(EventType eventType) {
        BukkitRunnable task = scheduledTasks.get(eventType);
        return task != null && !task.isCancelled();
    }

    // Debug method to get next schedule time for an event
    public LocalDateTime getNextScheduleTime(EventType eventType) {
        String timingType = File.getEventConfig().getString("events." + eventType.getConfigKey() + ".timing.type", "interval");

        if ("schedule".equals(timingType)) {
            String scheduleString = File.getEventConfig().getString("events." + eventType.getConfigKey() + ".timing.schedule", "");
            if (scheduleString.isEmpty()) {
                return null;
            }
            return findNextScheduleTime(scheduleString);
        } else if ("interval".equals(timingType)) {
            // For interval events, calculate next time based on stored nextScheduledTime
            BaseEvent event = EventManager.getAllEvents().get(eventType);
            if (event != null) {
                long nextTime = event.getEventData().getNextScheduledTime();
                if (nextTime > 0) {
                    return LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(nextTime),
                            java.time.ZoneId.systemDefault()
                    );
                }
            }
            return null;
        }

        return null;
    }


}
