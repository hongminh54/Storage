package net.danh.storage.Data;

import net.danh.storage.Event.EventType;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class EventData {
    private final EventType eventType;
    private final Map<UUID, Integer> playerData;
    private boolean active;
    private long startTime;
    private long endTime;
    private long nextScheduledTime;
    private int communityProgress;
    private int communityGoal;

    public EventData(EventType eventType) {
        this.eventType = eventType;
        this.active = false;
        this.startTime = 0;
        this.endTime = 0;
        this.nextScheduledTime = 0;
        this.playerData = new HashMap<>();
        this.communityProgress = 0;
        this.communityGoal = 0;
    }

    public EventType getEventType() {
        return eventType;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public long getNextScheduledTime() {
        return nextScheduledTime;
    }

    public void setNextScheduledTime(long nextScheduledTime) {
        this.nextScheduledTime = nextScheduledTime;
    }

    public Map<UUID, Integer> getPlayerData() {
        return playerData;
    }

    public void addPlayerData(Player player, int amount) {
        UUID uuid = player.getUniqueId();
        playerData.put(uuid, playerData.getOrDefault(uuid, 0) + amount);
    }

    public int getPlayerData(Player player) {
        return playerData.getOrDefault(player.getUniqueId(), 0);
    }

    public void clearPlayerData() {
        playerData.clear();
    }

    public Set<UUID> getParticipants() {
        return playerData.keySet();
    }

    public int getCommunityProgress() {
        return communityProgress;
    }

    public void setCommunityProgress(int communityProgress) {
        this.communityProgress = communityProgress;
    }

    public void addCommunityProgress(int amount) {
        this.communityProgress += amount;
    }

    public int getCommunityGoal() {
        return communityGoal;
    }

    public void setCommunityGoal(int communityGoal) {
        this.communityGoal = communityGoal;
    }

    public boolean isCommunityGoalReached() {
        return communityProgress >= communityGoal;
    }

    public double getCommunityProgressPercentage() {
        if (communityGoal <= 0) return 0.0;
        return Math.min(100.0, (double) communityProgress / communityGoal * 100.0);
    }

    public void reset() {
        this.active = false;
        this.startTime = 0;
        this.endTime = 0;
        this.playerData.clear();
        this.communityProgress = 0;
    }
}
