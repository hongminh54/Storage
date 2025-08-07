package net.danh.storage.Event;

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
import java.time.temporal.TemporalAdjusters;
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

        String[] schedules = scheduleString.split(",");
        for (String schedule : schedules) {
            scheduleNextOccurrence(eventType, event, schedule.trim());
        }
    }

    private void scheduleNextOccurrence(EventType eventType, BaseEvent event, String schedule) {
        try {
            String[] parts = schedule.split(":");
            if (parts.length != 2) return;

            String dayPart = parts[0];
            String timePart = parts[1];

            String[] timeComponents = timePart.split(":");
            if (timeComponents.length != 2) return;

            int hour = Integer.parseInt(timeComponents[0]);
            int minute = Integer.parseInt(timeComponents[1]);

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime nextRun = calculateNextRun(dayPart, hour, minute, now);

            long delayTicks = (nextRun.atZone(ZoneId.systemDefault()).toEpochSecond() -
                    now.atZone(ZoneId.systemDefault()).toEpochSecond()) * 20L;

            if (delayTicks > 0) {
                long nextScheduledTime = nextRun.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000L;
                event.getEventData().setNextScheduledTime(nextScheduledTime);

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
                            scheduleNextOccurrence(eventType, event, schedule);
                        }
                    }
                };

                task.runTaskLater(Storage.getStorage(), delayTicks);
                scheduledTasks.put(eventType, task);
            }
        } catch (Exception e) {
            Storage.getStorage().getLogger().warning("Failed to parse schedule: " + schedule + " for event: " + eventType.getConfigKey());
        }
    }

    private LocalDateTime calculateNextRun(String dayPart, int hour, int minute, LocalDateTime now) {
        LocalTime targetTime = LocalTime.of(hour, minute);

        if (dayPart.equalsIgnoreCase("DAILY")) {
            LocalDateTime today = now.with(targetTime);
            if (today.isAfter(now)) {
                return today;
            } else {
                return today.plusDays(1);
            }
        }

        try {
            DayOfWeek targetDay = DayOfWeek.valueOf(dayPart.toUpperCase());
            LocalDateTime nextOccurrence = now.with(TemporalAdjusters.nextOrSame(targetDay)).with(targetTime);

            if (nextOccurrence.isBefore(now) || nextOccurrence.equals(now)) {
                nextOccurrence = now.with(TemporalAdjusters.next(targetDay)).with(targetTime);
            }

            return nextOccurrence;
        } catch (IllegalArgumentException e) {
            return now.plusDays(1).with(targetTime);
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
}
