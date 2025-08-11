package net.danh.storage.Event.Events;

import net.danh.storage.Event.BaseEvent;
import net.danh.storage.Event.EventType;
import net.danh.storage.Storage;
import net.danh.storage.Utils.Chat;
import net.danh.storage.Utils.File;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class DoubleDropEvent extends BaseEvent {

    public DoubleDropEvent() {
        super(EventType.DOUBLE_DROP);
    }

    @Override
    public void start() {
        startEvent();
    }

    @Override
    public void end() {
        if (!eventData.isActive()) return;
        endEvent();
    }

    @Override
    public void onPlayerMine(Player player, String material, int amount) {
        if (!eventData.isActive()) return;
        int previousTotal = eventData.getPlayerData(player);
        boolean isFirstTime = (previousTotal == 0);

        int bonusAmount = calculateBonusAmount(amount);
        long remainingTime = getRemainingTime();

        eventData.addPlayerData(player, amount);

        if (isFirstTime) {
            sendFirstParticipationMessage(player);
        }

        String actionBarMessage = File.getMessage().getString("events.double_drop.action_bar.active");
        actionBarMessage = actionBarMessage.replace("#bonus#", String.valueOf(bonusAmount))
                .replace("#time#", getFormattedRemainingTime())
                .replace("#amount#", String.valueOf(amount));

        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText(Chat.colorizewp(actionBarMessage)));
    }

    public double getMultiplier() {
        if (!eventData.isActive()) {
            return 1.0;
        }

        double multiplier = File.getEventConfig().getDouble("events." + eventType.getConfigKey() + ".multiplier", 2.0);

        if (multiplier <= 0) {
            Storage.getStorage().getLogger().severe("Invalid multiplier value: " + multiplier + " for double_drop event. Must be greater than 0. Using default value 2.0");
            return 2.0;
        }

        if (multiplier != Math.floor(multiplier) && multiplier < 1.0) {
            Storage.getStorage().getLogger().warning("Multiplier value: " + multiplier + " for double_drop event is less than 1.0. This may cause unexpected behavior.");
        }

        return multiplier;
    }

    public int calculateBonusAmount(int originalAmount) {
        if (!eventData.isActive()) {
            return 0;
        }

        double multiplier = getMultiplier();
        int totalAmount = (int) (originalAmount * multiplier);
        return totalAmount - originalAmount;
    }


    @Override
    protected String getEventEndSubtitle() {
        return File.getMessage().getString("events.double_drop.subtitle.ended");
    }

    @Override
    protected void broadcastEventStartChat() {
        if (!File.getEventConfig().getBoolean("notifications.chat_messages.enabled", true)) {
            return;
        }

        String message = File.getMessage().getString("events.double_drop.chat.event_started");
        if (message == null || message.isEmpty()) return;

        int duration = File.getEventConfig().getInt("events." + eventType.getConfigKey() + ".duration", 1800);
        double multiplier = File.getEventConfig().getDouble("events." + eventType.getConfigKey() + ".multiplier", 2.0);

        message = message.replace("#duration#", String.valueOf(duration / 60))
                .replace("#multiplier#", String.valueOf(multiplier));

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(Chat.colorizewp(message));
        }
    }

    @Override
    protected void broadcastEventEndChat() {
        if (!File.getEventConfig().getBoolean("notifications.chat_messages.enabled", true)) {
            return;
        }

        String message = File.getMessage().getString("events.double_drop.chat.event_ended");
        if (message == null || message.isEmpty()) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(Chat.colorizewp(message));
        }
    }

    @Override
    protected String getEventStartSubtitle() {
        String subtitle = File.getMessage().getString("events.double_drop.subtitle.started");
        int duration = File.getEventConfig().getInt("events." + eventType.getConfigKey() + ".duration", 1800);
        double multiplier = File.getEventConfig().getDouble("events." + eventType.getConfigKey() + ".multiplier", 2.0);

        return subtitle.replace("#duration#", String.valueOf(duration / 60))
                .replace("#multiplier#", String.valueOf(multiplier));
    }

    public long getRemainingTime() {
        if (!eventData.isActive()) {
            return 0;
        }

        long currentTime = System.currentTimeMillis();
        long endTime = eventData.getEndTime();

        return Math.max(0, (endTime - currentTime) / 1000);
    }

    public String getFormattedRemainingTime() {
        return formatTime(getRemainingTime());
    }

    @Override
    protected void sendFirstParticipationMessage(Player player) {
        if (!File.getEventConfig().getBoolean("notifications.chat_messages.enabled", true)) {
            return;
        }

        String message = File.getMessage().getString("events.double_drop.chat.first_participation");
        if (message == null || message.isEmpty()) return;

        double multiplier = getMultiplier();
        String timeLeft = getFormattedRemainingTime();

        message = message.replace("#multiplier#", String.valueOf(multiplier))
                .replace("#time#", timeLeft);
        player.sendMessage(Chat.colorizewp(message));
    }
}
