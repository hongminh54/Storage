package net.danh.storage.Event.Events;

import net.danh.storage.Event.BaseEvent;
import net.danh.storage.Event.EventType;
import net.danh.storage.Storage;
import net.danh.storage.Utils.Chat;
import net.danh.storage.Utils.File;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.stream.Collectors;

public class MiningContestEvent extends BaseEvent {
    private BukkitRunnable chatProgressTask;

    public MiningContestEvent() {
        super(EventType.MINING_CONTEST);
    }

    @Override
    public void start() {
        startEvent();
        eventData.clearPlayerData();
        startChatProgressUpdates();
    }

    @Override
    public void end() {
        if (!eventData.isActive()) return;

        try {
            stopChatProgressUpdates();

            // Check if this is a manual stop or natural end
            boolean manualStop = isManualStop();

            if (manualStop) {
                announceManualStop();
            }

            announceWinners();
            giveRewardsToWinners();

            if (manualStop) {
                announceManualStopComplete();
            }
        } catch (Exception e) {
            Storage.getStorage().getLogger().warning("Error during mining contest end: " + e.getMessage());
        } finally {
            endEvent();
        }
    }

    private void announceManualStop() {
        Storage.getStorage().getLogger().info("Mining Contest manually stopped - beginning graceful conclusion process");

        String title = File.getMessage().getString("events.mining_contest.title.manual_stop");
        String subtitle = File.getMessage().getString("events.mining_contest.subtitle.manual_stop");

        int fadeIn = File.getEventConfig().getInt("notifications.titles.fade_in", 10);
        int stay = File.getEventConfig().getInt("notifications.titles.stay", 70);
        int fadeOut = File.getEventConfig().getInt("notifications.titles.fade_out", 20);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle(Chat.colorizewp(title), Chat.colorizewp(subtitle), fadeIn, stay, fadeOut);
        }
    }

    private void announceManualStopComplete() {
        Storage.getStorage().getLogger().info("Mining Contest graceful conclusion completed successfully");

        String message = File.getMessage().getString("events.mining_contest.chat.manual_stop_complete");

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(Chat.colorizewp(message));
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
        if (!eventData.isActive()) return;

        long remainingTime = (eventData.getEndTime() - System.currentTimeMillis()) / 1000;
        Map<String, Integer> topPlayers = getTopPlayers(3);

        StringBuilder top3Builder = new StringBuilder();
        int position = 1;
        for (Map.Entry<String, Integer> entry : topPlayers.entrySet()) {
            if (position > 1) top3Builder.append(", ");
            top3Builder.append("&e").append(position).append(". &a").append(entry.getKey())
                    .append(" &7(").append(entry.getValue()).append(")");
            position++;
        }

        String top3 = top3Builder.length() > 0 ? top3Builder.toString() : "&7No participants yet";

        String progressMessage = File.getMessage().getString("events.mining_contest.chat.progress_summary");
        progressMessage = progressMessage.replace("#top3#", top3)
                .replace("#time#", formatTime(remainingTime));

        boolean participantsOnly = File.getEventConfig().getBoolean("notifications.chat_progress.participants_only", true);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!participantsOnly || eventData.getParticipants().contains(player.getUniqueId())) {
                player.sendMessage(Chat.colorizewp(progressMessage));
            }
        }
    }

    @Override
    protected void cleanup() {
        stopChatProgressUpdates();
        if (eventData != null) {
            eventData.clearPlayerData();
        }
    }

    @Override
    public void onPlayerMine(Player player, String material, int amount) {
        if (!eventData.isActive()) return;

        int previousTotal = eventData.getPlayerData(player);
        int previousRank = getPlayerRank(player);

        eventData.addPlayerData(player, amount);

        int playerTotal = eventData.getPlayerData(player);
        int currentRank = getPlayerRank(player);
        long remainingTime = (eventData.getEndTime() - System.currentTimeMillis()) / 1000;

        // First participation message
        if (previousTotal == 0) {
            sendFirstMineMessage(player);
        }

        // Milestone messages
        checkMilestones(player, previousTotal, playerTotal, currentRank);

        // Rank improvement message
        if (previousRank > 0 && currentRank > 0 && currentRank < previousRank) {
            sendRankImprovedMessage(player, currentRank, playerTotal);
        }

        String actionBarMessage = File.getMessage().getString("events.mining_contest.action_bar.progress");
        actionBarMessage = actionBarMessage.replace("#amount#", String.valueOf(playerTotal))
                .replace("#rank#", currentRank > 0 ? String.valueOf(currentRank) : "N/A")
                .replace("#time#", formatTime(remainingTime));

        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText(Chat.colorizewp(actionBarMessage)));
    }


    private void announceWinners() {
        List<Map.Entry<UUID, Integer>> sortedPlayers = eventData.getPlayerData().entrySet()
                .stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .collect(Collectors.toList());

        if (sortedPlayers.isEmpty()) {
            String noWinnersMessage = File.getMessage().getString("events.mining_contest.chat.no_participants_detailed");
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(Chat.colorizewp(noWinnersMessage));
            }
            return;
        }

        // Show titles for top 3 winners
        String[] positions = {"first_place", "second_place", "third_place"};
        String[] positionNames = {"1st", "2nd", "3rd"};

        for (int i = 0; i < Math.min(sortedPlayers.size(), 3); i++) {
            Map.Entry<UUID, Integer> entry = sortedPlayers.get(i);
            Player player = Bukkit.getPlayer(entry.getKey());

            if (player != null) {
                String winnerTitle = File.getMessage().getString("events.mining_contest.title.winner");
                String winnerSubtitle = File.getMessage().getString("events.mining_contest.subtitle.winner");

                winnerTitle = winnerTitle.replace("#player#", player.getName())
                        .replace("#position#", positionNames[i])
                        .replace("#amount#", String.valueOf(entry.getValue()));

                winnerSubtitle = winnerSubtitle.replace("#player#", player.getName())
                        .replace("#position#", positionNames[i])
                        .replace("#amount#", String.valueOf(entry.getValue()));

                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    onlinePlayer.sendTitle(Chat.colorizewp(winnerTitle), Chat.colorizewp(winnerSubtitle), 10, 70, 20);
                }
            }
        }

        // Display detailed leaderboard
        String detailedLeaderboard = formatDetailedLeaderboard(sortedPlayers);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(detailedLeaderboard);
        }
    }

    private void giveRewardsToWinners() {
        List<Map.Entry<UUID, Integer>> sortedPlayers = eventData.getPlayerData().entrySet()
                .stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .limit(3)
                .collect(Collectors.toList());

        String[] rewardPaths = {"first_place", "second_place", "third_place"};

        for (int i = 0; i < Math.min(sortedPlayers.size(), 3); i++) {
            Map.Entry<UUID, Integer> entry = sortedPlayers.get(i);
            Player player = Bukkit.getPlayer(entry.getKey());

            if (player != null && player.isOnline()) {
                giveRewards(player, rewardPaths[i]);

                String rewardMessage = File.getMessage().getString("events.common.chat.reward_received");

                player.sendMessage(Chat.colorizewp(rewardMessage));
            }
        }
    }

    public Map<String, Integer> getTopPlayers(int limit) {
        // Allow getting top players even when event is not active (for final leaderboard)
        if (eventData.getPlayerData().isEmpty()) {
            return new HashMap<>();
        }

        return eventData.getPlayerData().entrySet()
                .stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toMap(
                        entry -> {
                            Player player = Bukkit.getPlayer(entry.getKey());
                            return player != null ? player.getName() : "Unknown";
                        },
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    private String formatDetailedLeaderboard(List<Map.Entry<UUID, Integer>> sortedPlayers) {
        StringBuilder leaderboard = new StringBuilder();

        // Header
        String header = File.getMessage().getString("events.mining_contest.chat.leaderboard_header");
        leaderboard.append(Chat.colorizewp(header)).append("\n");

        // Separator
        String separator = File.getMessage().getString("events.mining_contest.chat.leaderboard_separator");
        leaderboard.append(Chat.colorizewp(separator)).append("\n");

        // Get display count from config
        int displayCount = File.getEventConfig().getInt("events.mining_contest.leaderboard.display_count", 10);
        int maxDisplay = Math.min(displayCount, 15); // Hard limit to prevent spam

        // Display players
        for (int i = 0; i < Math.min(sortedPlayers.size(), maxDisplay); i++) {
            Map.Entry<UUID, Integer> entry = sortedPlayers.get(i);
            Player player = Bukkit.getPlayer(entry.getKey());

            if (player != null) {
                String entryMessage;
                if (i < 3) {
                    // Top 3 get special formatting
                    entryMessage = File.getMessage().getString("events.mining_contest.chat.leaderboard_entry_winner");
                } else {
                    // Others get normal formatting
                    entryMessage = File.getMessage().getString("events.mining_contest.chat.leaderboard_entry_normal");
                }

                entryMessage = entryMessage.replace("#position#", String.valueOf(i + 1))
                        .replace("#player#", player.getName())
                        .replace("#amount#", String.valueOf(entry.getValue()));

                leaderboard.append(Chat.colorizewp(entryMessage)).append("\n");
            }
        }

        // Show statistics if enabled
        if (File.getEventConfig().getBoolean("events.mining_contest.leaderboard.show_statistics", true)) {
            leaderboard.append(Chat.colorizewp(separator)).append("\n");
            leaderboard.append(getEventStatistics()).append("\n");
        }

        // Footer
        String footer = File.getMessage().getString("events.mining_contest.chat.leaderboard_footer");
        leaderboard.append(Chat.colorizewp(footer));

        return leaderboard.toString();
    }

    private String getEventStatistics() {
        int totalParticipants = eventData.getParticipants().size();
        int totalBlocks = eventData.getPlayerData().values().stream().mapToInt(Integer::intValue).sum();
        long eventDuration = (eventData.getEndTime() - eventData.getStartTime()) / 1000;

        String statistics = File.getMessage().getString("events.mining_contest.chat.event_summary");
        statistics = statistics.replace("#participants#", String.valueOf(totalParticipants))
                .replace("#total_blocks#", String.valueOf(totalBlocks))
                .replace("#duration#", formatTime(eventDuration));

        return Chat.colorizewp(statistics);
    }

    public int getPlayerRank(Player player) {
        if (!eventData.isActive()) {
            return -1;
        }

        List<Map.Entry<UUID, Integer>> sortedPlayers = eventData.getPlayerData().entrySet()
                .stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .collect(Collectors.toList());

        for (int i = 0; i < sortedPlayers.size(); i++) {
            if (sortedPlayers.get(i).getKey().equals(player.getUniqueId())) {
                return i + 1;
            }
        }

        return -1;
    }

    @Override
    protected void broadcastEventStartChat() {
        if (!File.getEventConfig().getBoolean("notifications.chat_messages.enabled", true)) {
            return;
        }

        String message = File.getMessage().getString("events.mining_contest.chat.event_started");
        if (message == null || message.isEmpty()) return;

        int duration = File.getEventConfig().getInt("events." + eventType.getConfigKey() + ".duration", 1800);
        message = message.replace("#duration#", String.valueOf(duration / 60));

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(Chat.colorizewp(message));
        }
    }

    private void sendFirstMineMessage(Player player) {
        if (!File.getEventConfig().getBoolean("notifications.chat_messages.enabled", true)) {
            return;
        }

        String message = File.getMessage().getString("events.mining_contest.chat.first_mine");
        if (message == null || message.isEmpty()) return;

        player.sendMessage(Chat.colorizewp(message));
    }

    private void checkMilestones(Player player, int previousTotal, int currentTotal, int rank) {
        if (!File.getEventConfig().getBoolean("notifications.chat_messages.enabled", true) ||
            !File.getEventConfig().getBoolean("notifications.chat_messages.milestones", true)) {
            return;
        }

        int[] milestones = {100, 500, 1000, 2500, 5000};

        for (int milestone : milestones) {
            if (previousTotal < milestone && currentTotal >= milestone) {
                sendMilestoneMessage(player, milestone, rank);
                break;
            }
        }
    }

    private void sendMilestoneMessage(Player player, int amount, int rank) {
        String message = File.getMessage().getString("events.mining_contest.chat.milestone_reached");
        if (message == null || message.isEmpty()) return;

        message = message.replace("#amount#", String.valueOf(amount))
                        .replace("#rank#", rank > 0 ? String.valueOf(rank) : "N/A");

        player.sendMessage(Chat.colorizewp(message));
    }

    private void sendRankImprovedMessage(Player player, int rank, int amount) {
        String message = File.getMessage().getString("events.mining_contest.chat.rank_improved");
        if (message == null || message.isEmpty()) return;

        message = message.replace("#rank#", String.valueOf(rank))
                        .replace("#amount#", String.valueOf(amount));

        player.sendMessage(Chat.colorizewp(message));
    }
}
