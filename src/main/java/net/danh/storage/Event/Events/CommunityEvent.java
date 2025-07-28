package net.danh.storage.Event.Events;

import net.danh.storage.Event.BaseEvent;
import net.danh.storage.Event.EventType;
import net.danh.storage.Storage;
import net.danh.storage.Utils.Chat;
import net.danh.storage.Utils.File;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class CommunityEvent extends BaseEvent {
    private BukkitRunnable progressUpdateTask;
    private BukkitRunnable chatProgressTask;

    public CommunityEvent() {
        super(EventType.COMMUNITY_EVENT);
    }

    @Override
    public void start() {
        startEvent();
        eventData.setCommunityProgress(0);
        eventData.clearPlayerData();

        int goal = File.getEventConfig().getInt("events." + eventType.getConfigKey() + ".goal", 10000);
        eventData.setCommunityGoal(goal);

        startProgressUpdates();
        startChatProgressUpdates();
    }

    @Override
    public void end() {
        if (!eventData.isActive()) return;

        stopProgressUpdates();
        stopChatProgressUpdates();

        if (eventData.isCommunityGoalReached()) {
            announceSuccess();
            giveRewardsToParticipants();
        } else {
            announceFailure();
        }

        endEvent();
    }

    @Override
    public void onPlayerMine(Player player, String material, int amount) {
        if (!eventData.isActive()) return;

        boolean isFirstContribution = eventData.getPlayerData(player) == 0;
        double previousPercentage = eventData.getCommunityProgressPercentage();

        eventData.addPlayerData(player, amount);
        eventData.addCommunityProgress(amount);

        // First contribution message
        if (isFirstContribution) {
            sendFirstContributionMessage(player);
        }

        // Check milestone progress
        double currentPercentage = eventData.getCommunityProgressPercentage();
        checkMilestones(previousPercentage, currentPercentage);

        if (eventData.isCommunityGoalReached()) {
            end();
        }
    }

    private void startProgressUpdates() {
        progressUpdateTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!eventData.isActive()) {
                    cancel();
                    return;
                }

                broadcastProgress();
            }
        };

        progressUpdateTask.runTaskTimer(Storage.getStorage(), 200L, 200L);
    }

    private void stopProgressUpdates() {
        if (progressUpdateTask != null && !progressUpdateTask.isCancelled()) {
            progressUpdateTask.cancel();
            progressUpdateTask = null;
        }
    }

    private void startChatProgressUpdates() {
        if (!File.getEventConfig().getBoolean("notifications.chat_progress.enabled", true)) {
            return;
        }

        int interval = File.getEventConfig().getInt("notifications.chat_progress.interval", 300);
        long intervalTicks = interval * 20L;

        chatProgressTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!eventData.isActive()) {
                    cancel();
                    return;
                }
                broadcastChatProgress();
            }
        };

        chatProgressTask.runTaskTimer(Storage.getStorage(), intervalTicks, intervalTicks);
    }

    private void stopChatProgressUpdates() {
        if (chatProgressTask != null && !chatProgressTask.isCancelled()) {
            chatProgressTask.cancel();
            chatProgressTask = null;
        }
    }

    private void broadcastChatProgress() {
        long remainingTime = (eventData.getEndTime() - System.currentTimeMillis()) / 1000;
        int participantCount = getParticipantCount();

        String progressMessage = File.getMessage().getString("events.community_event.chat.progress_summary");

        progressMessage = progressMessage.replace("#current#", String.valueOf(eventData.getCommunityProgress()))
                .replace("#goal#", String.valueOf(eventData.getCommunityGoal()))
                .replace("#percentage#", String.format("%.1f", eventData.getCommunityProgressPercentage()))
                .replace("#participants#", String.valueOf(participantCount))
                .replace("#time#", formatTime(remainingTime));

        boolean participantsOnly = File.getEventConfig().getBoolean("notifications.chat_progress.participants_only", true);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!participantsOnly || eventData.getParticipants().contains(player.getUniqueId())) {
                player.sendMessage(Chat.colorizewp(progressMessage));
            }
        }
    }

    private void broadcastProgress() {
        long remainingTime = (eventData.getEndTime() - System.currentTimeMillis()) / 1000;
        int participantCount = getParticipantCount();

        String progressMessage = File.getMessage().getString("events.community_event.action_bar.progress");

        progressMessage = progressMessage.replace("#current#", String.valueOf(eventData.getCommunityProgress()))
                .replace("#goal#", String.valueOf(eventData.getCommunityGoal()))
                .replace("#percentage#", String.format("%.1f", eventData.getCommunityProgressPercentage()))
                .replace("#participants#", String.valueOf(participantCount))
                .replace("#time#", formatTime(remainingTime));

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (eventData.getParticipants().contains(player.getUniqueId())) {
                player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                        net.md_5.bungee.api.chat.TextComponent.fromLegacyText(Chat.colorizewp(progressMessage)));
            }
        }
    }


    private void announceSuccess() {
        String title = File.getMessage().getString("events.community_event.title.completed");
        String subtitle = File.getMessage().getString("events.community_event.subtitle.completed");

        int fadeIn = File.getEventConfig().getInt("notifications.titles.fade_in", 10);
        int stay = File.getEventConfig().getInt("notifications.titles.stay", 70);
        int fadeOut = File.getEventConfig().getInt("notifications.titles.fade_out", 20);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle(Chat.colorizewp(title), Chat.colorizewp(subtitle), fadeIn, stay, fadeOut);
        }
    }

    private void announceFailure() {
        String title = File.getMessage().getString("events.community_event.title.failed");
        String subtitle = File.getMessage().getString("events.community_event.subtitle.failed");

        subtitle = subtitle.replace("#current#", String.valueOf(eventData.getCommunityProgress()))
                .replace("#goal#", String.valueOf(eventData.getCommunityGoal()));

        int fadeIn = File.getEventConfig().getInt("notifications.titles.fade_in", 10);
        int stay = File.getEventConfig().getInt("notifications.titles.stay", 70);
        int fadeOut = File.getEventConfig().getInt("notifications.titles.fade_out", 20);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle(Chat.colorizewp(title), Chat.colorizewp(subtitle), fadeIn, stay, fadeOut);
        }
    }

    private void giveRewardsToParticipants() {
        for (UUID participantUUID : eventData.getParticipants()) {
            Player participant = Bukkit.getPlayer(participantUUID);

            if (participant != null && participant.isOnline()) {
                giveRewards(participant, "");

                String rewardMessage = File.getMessage().getString("events.common.chat.reward_received");

                participant.sendMessage(Chat.colorizewp(rewardMessage));
            }
        }
    }


    public int getParticipantCount() {
        return eventData.getParticipants().size();
    }

    public double getProgressPercentage() {
        return eventData.getCommunityProgressPercentage();
    }

    public int getCurrentProgress() {
        return eventData.getCommunityProgress();
    }

    public int getGoal() {
        return eventData.getCommunityGoal();
    }

    public int getPlayerContribution(Player player) {
        return eventData.getPlayerData(player);
    }

    public long getRemainingTime() {
        if (!eventData.isActive()) {
            return 0;
        }

        long currentTime = System.currentTimeMillis();
        long endTime = eventData.getEndTime();

        return Math.max(0, (endTime - currentTime) / 1000);
    }

    @Override
    protected void cleanup() {
        stopProgressUpdates();
        stopChatProgressUpdates();
    }

    @Override
    public void forceStop() {
        stopProgressUpdates();
        stopChatProgressUpdates();
        super.forceStop();
    }

    @Override
    protected void broadcastEventStartChat() {
        if (!File.getEventConfig().getBoolean("notifications.chat_messages.enabled", true)) {
            return;
        }

        String message = File.getMessage().getString("events.community_event.chat.event_started");
        if (message == null || message.isEmpty()) return;

        int duration = File.getEventConfig().getInt("events." + eventType.getConfigKey() + ".duration", 1800);
        int goal = File.getEventConfig().getInt("events." + eventType.getConfigKey() + ".goal", 10000);

        message = message.replace("#duration#", String.valueOf(duration / 60))
                        .replace("#goal#", String.valueOf(goal));

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(Chat.colorizewp(message));
        }
    }

    private void sendFirstContributionMessage(Player player) {
        if (!File.getEventConfig().getBoolean("notifications.chat_messages.enabled", true)) {
            return;
        }

        String message = File.getMessage().getString("events.community_event.chat.first_contribution");
        if (message == null || message.isEmpty()) return;

        player.sendMessage(Chat.colorizewp(message));
    }

    private void checkMilestones(double previousPercentage, double currentPercentage) {
        if (!File.getEventConfig().getBoolean("notifications.chat_messages.enabled", true) ||
            !File.getEventConfig().getBoolean("notifications.chat_messages.milestones", true)) {
            return;
        }

        double[] milestones = {25.0, 50.0, 75.0};

        for (double milestone : milestones) {
            if (previousPercentage < milestone && currentPercentage >= milestone) {
                sendMilestoneMessage(milestone);
                break;
            }
        }
    }

    private void sendMilestoneMessage(double percentage) {
        String message = File.getMessage().getString("events.community_event.chat.milestone_reached");
        if (message == null || message.isEmpty()) return;

        message = message.replace("#percentage#", String.format("%.0f", percentage))
                        .replace("#current#", String.valueOf(eventData.getCommunityProgress()))
                        .replace("#goal#", String.valueOf(eventData.getCommunityGoal()));

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(Chat.colorizewp(message));
        }
    }
}
