package net.danh.storage.Event;

import net.danh.storage.Data.EventData;
import net.danh.storage.Storage;
import net.danh.storage.Utils.Chat;
import net.danh.storage.Utils.File;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseEvent {
    protected final EventType eventType;
    protected final EventData eventData;
    protected final List<BukkitRunnable> reminderTasks;
    protected BukkitRunnable eventTask;

    public BaseEvent(EventType eventType) {
        this.eventType = eventType;
        this.eventData = new EventData(eventType);
        this.reminderTasks = new ArrayList<>();
    }

    public EventType getEventType() {
        return eventType;
    }

    public EventData getEventData() {
        return eventData;
    }

    public boolean isActive() {
        return eventData.isActive();
    }

    public abstract void start();

    public abstract void end();

    public abstract void onPlayerMine(Player player, String material, int amount);

    public boolean isManualStop() {
        if (!eventData.isActive()) return false;
        long currentTime = System.currentTimeMillis();
        long scheduledEndTime = eventData.getEndTime();
        return currentTime < (scheduledEndTime - 5000); // 5 second buffer for manual stops
    }

    protected void startEvent() {
        eventData.setActive(true);
        eventData.setStartTime(System.currentTimeMillis());

        int duration = File.getEventConfig().getInt("events." + eventType.getConfigKey() + ".duration", 1800);
        eventData.setEndTime(System.currentTimeMillis() + (duration * 1000L));

        eventTask = new BukkitRunnable() {
            @Override
            public void run() {
                end();
            }
        };
        eventTask.runTaskLater(Storage.getStorage(), duration * 20L);

        scheduleEndReminders(duration);
        broadcastEventStart();
    }

    protected void endEvent() {
        if (eventTask != null && !eventTask.isCancelled()) {
            eventTask.cancel();
            eventTask = null;
        }

        eventData.setActive(false);
        broadcastEventEnd();
        cleanup();
        eventData.reset();
    }

    protected void cleanup() {
        cancelAllReminders();
    }

    protected String getEventName() {
        return File.getEventConfig().getString("events." + eventType.getConfigKey() + ".event_name", eventType.getDisplayName());
    }

    protected void broadcastEventStart() {
        String title = File.getMessage().getString("events.common.title.started");
        title = title.replace("#event#", getEventName());

        String subtitle = getEventStartSubtitle();

        int fadeIn = File.getEventConfig().getInt("notifications.titles.fade_in", 10);
        int stay = File.getEventConfig().getInt("notifications.titles.stay", 70);
        int fadeOut = File.getEventConfig().getInt("notifications.titles.fade_out", 20);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle(Chat.colorizewp(title), Chat.colorizewp(subtitle), fadeIn, stay, fadeOut);
        }

        broadcastEventStartChat();
    }

    protected String getEventStartSubtitle() {
        String subtitle = File.getMessage().getString("events.common.subtitle.started");
        int duration = File.getEventConfig().getInt("events." + eventType.getConfigKey() + ".duration", 1800);
        return subtitle.replace("#duration#", String.valueOf(duration / 60));
    }

    protected void broadcastEventEnd() {
        String title = File.getMessage().getString("events.common.title.ended");
        title = title.replace("#event#", getEventName());

        String subtitle = getEventEndSubtitle();

        int fadeIn = File.getEventConfig().getInt("notifications.titles.fade_in", 10);
        int stay = File.getEventConfig().getInt("notifications.titles.stay", 70);
        int fadeOut = File.getEventConfig().getInt("notifications.titles.fade_out", 20);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle(Chat.colorizewp(title), Chat.colorizewp(subtitle), fadeIn, stay, fadeOut);
        }

        broadcastEventEndChat();
    }

    protected String getEventEndSubtitle() {
        return File.getMessage().getString("events.common.subtitle.ended");
    }

    protected void broadcastActionBar(String messageKey, Player... players) {
        String message = File.getMessage().getString(messageKey);
        if (message == null || message.isEmpty()) return;

        Player[] targetPlayers = players.length > 0 ? players : Bukkit.getOnlinePlayers().toArray(new Player[0]);

        for (Player player : targetPlayers) {
            if (player != null && player.isOnline()) {
                player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                        net.md_5.bungee.api.chat.TextComponent.fromLegacyText(Chat.colorizewp(message)));
            }
        }
    }

    protected void broadcastChatMessage(String messageKey) {
        String message = File.getMessage().getString(messageKey);
        if (message == null || message.isEmpty()) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(Chat.colorizewp(message));
        }
    }

    protected void broadcastEventStartChat() {
        if (!File.getEventConfig().getBoolean("notifications.chat_messages.enabled", true)) {
            return;
        }

        String message = File.getMessage().getString("events.common.chat.event_started");
        if (message == null || message.isEmpty()) return;

        int duration = File.getEventConfig().getInt("events." + eventType.getConfigKey() + ".duration", 1800);
        message = message.replace("#event#", getEventName())
                        .replace("#duration#", String.valueOf(duration / 60));

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(Chat.colorizewp(message));
        }
    }

    protected void broadcastEventEndChat() {
        if (!File.getEventConfig().getBoolean("notifications.chat_messages.enabled", true)) {
            return;
        }

        String message = File.getMessage().getString("events.common.chat.event_ended");
        if (message == null || message.isEmpty()) return;

        message = message.replace("#event#", getEventName());

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(Chat.colorizewp(message));
        }
    }

    protected void sendFirstParticipationMessage(Player player) {
        if (!File.getEventConfig().getBoolean("notifications.chat_messages.enabled", true) ||
            !File.getEventConfig().getBoolean("notifications.chat_messages.first_participation", true)) {
            return;
        }

        String message = File.getMessage().getString("events.common.chat.first_participation");
        if (message == null || message.isEmpty()) return;

        message = message.replace("#event#", getEventName());
        player.sendMessage(Chat.colorizewp(message));
    }

    protected void giveRewards(Player player, String rewardPath) {
        if (!File.getEventConfig().contains("events." + eventType.getConfigKey() + ".rewards." + rewardPath)) {
            return;
        }

        String basePath = "events." + eventType.getConfigKey() + ".rewards." + rewardPath;

        if (File.getEventConfig().contains(basePath + ".money")) {
            double money = File.getEventConfig().getDouble(basePath + ".money", 0);
            if (money > 0) {
                runMoneyCommand(player, money);
            }
        }

        if (File.getEventConfig().contains(basePath + ".commands")) {
            List<String> commands = File.getEventConfig().getStringList(basePath + ".commands");
            runCommands(player, commands);
        }

        if (File.getEventConfig().contains(basePath + ".broadcast_message")) {
            String message = File.getEventConfig().getString(basePath + ".broadcast_message");
            broadcastRewardMessage(player, message);
        }
    }

    protected void runMoneyCommand(Player player, double money) {
        List<String> sellCommands = File.getConfig().getStringList("sell");
        for (String cmd : sellCommands) {
            String processedCmd = cmd.replace("#money#", String.valueOf(money))
                    .replace("#player#", player.getName());
            new BukkitRunnable() {
                @Override
                public void run() {
                    Storage.getStorage().getServer().dispatchCommand(
                            Storage.getStorage().getServer().getConsoleSender(), processedCmd);
                }
            }.runTask(Storage.getStorage());
        }
    }

    protected void runCommands(Player player, List<String> commands) {
        for (String cmd : commands) {
            String processedCmd = cmd.replace("#player#", player.getName());
            new BukkitRunnable() {
                @Override
                public void run() {
                    Storage.getStorage().getServer().dispatchCommand(
                            Storage.getStorage().getServer().getConsoleSender(), processedCmd);
                }
            }.runTask(Storage.getStorage());
        }
    }

    protected void broadcastRewardMessage(Player player, String message) {
        if (message != null && !message.isEmpty()) {
            String processedMessage = message.replace("#player#", player.getName());
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                onlinePlayer.sendMessage(Chat.colorizewp(processedMessage));
            }
        }
    }

    protected String formatTime(long seconds) {
        if (seconds <= 0) return "0s";
        long minutes = seconds / 60;
        long secs = seconds % 60;
        return minutes > 0 ? minutes + "m " + secs + "s" : secs + "s";
    }

    protected void scheduleEndReminders(int duration) {
        if (!File.getEventConfig().getBoolean("notifications.reminders.enabled", true)) {
            return;
        }

        List<Integer> reminderTimes = File.getEventConfig().getIntegerList("notifications.reminders.before_end");
        if (reminderTimes.isEmpty()) {
            reminderTimes = List.of(300, 60, 10); // Default: 5min, 1min, 10sec
        }

        for (int reminderTime : reminderTimes) {
            if (reminderTime < duration) {
                long delayTicks = (duration - reminderTime) * 20L;

                BukkitRunnable reminderTask = new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (eventData.isActive()) {
                            broadcastEndReminder(reminderTime);
                        }
                    }
                };

                reminderTask.runTaskLater(Storage.getStorage(), delayTicks);
                reminderTasks.add(reminderTask);
            }
        }
    }

    protected void broadcastEndReminder(int secondsLeft) {
        String message = File.getMessage().getString("events.common.chat.reminder_end");
        message = message.replace("#event#", getEventName())
                .replace("#time#", formatTime(secondsLeft));

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(Chat.colorizewp(message));
        }
    }

    protected void cancelAllReminders() {
        for (BukkitRunnable task : reminderTasks) {
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
        }
        reminderTasks.clear();
    }

    public void forceStop() {
        if (eventData.isActive()) {
            try {
                cleanup();
                endEvent();
            } catch (Exception e) {
                Storage.getStorage().getLogger().warning("Error during force stop cleanup for " + eventType.getDisplayName() + ": " + e.getMessage());
                eventData.setActive(false);
                eventData.reset();
            }
        }
    }
}
