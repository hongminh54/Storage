package net.danh.storage.API;

import net.danh.storage.Event.BaseEvent;
import net.danh.storage.Event.EventScheduler;
import net.danh.storage.Event.EventType;
import net.danh.storage.Event.Events.CommunityEvent;
import net.danh.storage.Event.Events.DoubleDropEvent;
import net.danh.storage.Event.Events.MiningContestEvent;
import net.danh.storage.Manager.EventManager;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;


/**
 * API for Storage Event System
 * Provides methods for external plugins to interact with server events
 *
 * @author hongminh54
 * @version 2.3.3
 */
public class StorageEventAPI {

    /**
     * Check if event system is initialized
     *
     * @return true if initialized
     */
    public static boolean isInitialized() {
        return EventManager.isInitialized();
    }

    /**
     * Start a specific event
     *
     * @param eventType Type of event to start
     * @return true if successfully started
     */
    public static boolean startEvent(@NotNull EventType eventType) {
        if (!isInitialized()) {
            return false;
        }
        return EventManager.startEvent(eventType);
    }

    /**
     * Stop a specific event
     *
     * @param eventType Type of event to stop
     * @return true if successfully stopped
     */
    public static boolean stopEvent(@NotNull EventType eventType) {
        if (!isInitialized()) {
            return false;
        }
        return EventManager.stopEvent(eventType);
    }

    /**
     * Stop a specific event with force option
     *
     * @param eventType Type of event to stop
     * @param forceStop If true, immediately stop without graceful shutdown
     * @return true if successfully stopped
     */
    public static boolean stopEvent(@NotNull EventType eventType, boolean forceStop) {
        if (!isInitialized()) {
            return false;
        }
        return EventManager.stopEvent(eventType, forceStop);
    }

    /**
     * Check if a specific event is currently active
     *
     * @param eventType Type of event to check
     * @return true if event is active
     */
    public static boolean isEventActive(@NotNull EventType eventType) {
        if (!isInitialized()) {
            return false;
        }
        return EventManager.isEventActive(eventType);
    }

    /**
     * Get the BaseEvent instance for a specific event type
     *
     * @param eventType Type of event
     * @return BaseEvent instance or null if not found
     */
    @Nullable
    public static BaseEvent getEvent(@NotNull EventType eventType) {
        if (!isInitialized()) {
            return null;
        }
        return EventManager.getEvent(eventType);
    }

    /**
     * Get all registered events
     *
     * @return Map of EventType -> BaseEvent
     */
    @NotNull
    public static Map<EventType, BaseEvent> getAllEvents() {
        if (!isInitialized()) {
            return new HashMap<>();
        }
        return EventManager.getAllEvents();
    }

    /**
     * Get event status as formatted string
     *
     * @param eventType Type of event
     * @return Status string
     */
    @NotNull
    public static String getEventStatus(@NotNull EventType eventType) {
        if (!isInitialized()) {
            return "Not initialized";
        }
        return EventManager.getEventStatus(eventType);
    }

    /**
     * Get remaining time for active event in seconds
     *
     * @param eventType Type of event
     * @return Remaining time in seconds, or 0 if not active
     */
    public static long getEventRemainingTime(@NotNull EventType eventType) {
        BaseEvent event = getEvent(eventType);
        if (event == null || !event.isActive()) {
            return 0;
        }
        return (event.getEventData().getEndTime() - System.currentTimeMillis()) / 1000;
    }

    /**
     * Get event start time in milliseconds
     *
     * @param eventType Type of event
     * @return Start time or 0 if not active
     */
    public static long getEventStartTime(@NotNull EventType eventType) {
        BaseEvent event = getEvent(eventType);
        if (event == null || !event.isActive()) {
            return 0;
        }
        return event.getEventData().getStartTime();
    }

    /**
     * Get event end time in milliseconds
     *
     * @param eventType Type of event
     * @return End time or 0 if not active
     */
    public static long getEventEndTime(@NotNull EventType eventType) {
        BaseEvent event = getEvent(eventType);
        if (event == null || !event.isActive()) {
            return 0;
        }
        return event.getEventData().getEndTime();
    }

    /**
     * Check if Mining Contest event is active
     *
     * @return true if active
     */
    public static boolean isMiningContestActive() {
        return EventManager.isMiningContestActive();
    }

    /**
     * Check if Double Drop event is active
     *
     * @return true if active
     */
    public static boolean isDoubleDropActive() {
        return EventManager.isDoubleDropActive();
    }

    /**
     * Check if Community Event is active
     *
     * @return true if active
     */
    public static boolean isCommunityEventActive() {
        return EventManager.isCommunityEventActive();
    }

    /**
     * Get player's score in Mining Contest
     *
     * @param player The player
     * @return Score or 0 if not participating
     */
    public static int getMiningContestScore(@NotNull Player player) {
        MiningContestEvent event = (MiningContestEvent) getEvent(EventType.MINING_CONTEST);
        if (event == null) {
            return 0;
        }
        return event.getEventData().getPlayerData(player);
    }

    /**
     * Get player's rank in Mining Contest
     *
     * @param player The player
     * @return Rank or 0 if not participating
     */
    public static int getMiningContestRank(@NotNull Player player) {
        MiningContestEvent event = (MiningContestEvent) getEvent(EventType.MINING_CONTEST);
        if (event == null) {
            return 0;
        }
        return event.getPlayerRank(player);
    }

    /**
     * Get Mining Contest leaderboard (top players)
     *
     * @param limit Maximum number of players to return
     * @return Map of player name -> score
     */
    @NotNull
    public static Map<String, Integer> getMiningContestLeaderboard(int limit) {
        MiningContestEvent event = (MiningContestEvent) getEvent(EventType.MINING_CONTEST);
        if (event == null) {
            return new HashMap<>();
        }
        return event.getTopPlayers(limit);
    }

    /**
     * Get Mining Contest top 10 leaderboard
     *
     * @return Map of player name -> score
     */
    @NotNull
    public static Map<String, Integer> getMiningContestLeaderboard() {
        return getMiningContestLeaderboard(10);
    }

    /**
     * Get number of participants in Mining Contest
     *
     * @return Number of participants
     */
    public static int getMiningContestParticipants() {
        MiningContestEvent event = (MiningContestEvent) getEvent(EventType.MINING_CONTEST);
        if (event == null) {
            return 0;
        }
        return event.getEventData().getParticipants().size();
    }

    /**
     * Get Double Drop multiplier
     *
     * @return Multiplier value (e.g., 2.0 for double)
     */
    public static double getDoubleDropMultiplier() {
        DoubleDropEvent event = (DoubleDropEvent) getEvent(EventType.DOUBLE_DROP);
        if (event == null || !event.isActive()) {
            return 1.0;
        }
        return event.getMultiplier();
    }

    /**
     * Calculate bonus amount for Double Drop event
     *
     * @param originalAmount Original amount
     * @return Bonus amount to add
     */
    public static int calculateDoubleDropBonus(int originalAmount) {
        return EventManager.calculateDoubleDropBonus(originalAmount);
    }

    /**
     * Get Community Event current progress
     *
     * @return Current progress value
     */
    public static int getCommunityEventProgress() {
        CommunityEvent event = (CommunityEvent) getEvent(EventType.COMMUNITY_EVENT);
        if (event == null) {
            return 0;
        }
        return event.getCurrentProgress();
    }

    /**
     * Get Community Event goal
     *
     * @return Goal value
     */
    public static int getCommunityEventGoal() {
        CommunityEvent event = (CommunityEvent) getEvent(EventType.COMMUNITY_EVENT);
        if (event == null) {
            return 0;
        }
        return event.getGoal();
    }

    /**
     * Get Community Event progress percentage
     *
     * @return Progress percentage (0.0 to 100.0)
     */
    public static double getCommunityEventPercentage() {
        CommunityEvent event = (CommunityEvent) getEvent(EventType.COMMUNITY_EVENT);
        if (event == null) {
            return 0.0;
        }
        return event.getProgressPercentage();
    }

    /**
     * Get player's contribution to Community Event
     *
     * @param player The player
     * @return Contribution amount
     */
    public static int getCommunityEventContribution(@NotNull Player player) {
        CommunityEvent event = (CommunityEvent) getEvent(EventType.COMMUNITY_EVENT);
        if (event == null) {
            return 0;
        }
        return event.getPlayerContribution(player);
    }

    /**
     * Get number of participants in Community Event
     *
     * @return Number of participants
     */
    public static int getCommunityEventParticipants() {
        CommunityEvent event = (CommunityEvent) getEvent(EventType.COMMUNITY_EVENT);
        if (event == null) {
            return 0;
        }
        return event.getParticipantCount();
    }

    /**
     * Get event scheduler instance
     *
     * @return EventScheduler or null if not initialized
     */
    @Nullable
    public static EventScheduler getScheduler() {
        if (!isInitialized()) {
            return null;
        }
        return EventManager.getScheduler();
    }

    /**
     * Reload event configurations
     */
    public static void reloadEvents() {
        if (!isInitialized()) {
            return;
        }
        EventManager.reloadEvents();
    }

    /**
     * Stop all active events
     */
    public static void stopAllEvents() {
        if (!isInitialized()) {
            return;
        }
        EventManager.stopAllEvents();
    }

    /**
     * Stop all active events with force option
     *
     * @param forceStop If true, immediately stop without graceful shutdown
     */
    public static void stopAllEvents(boolean forceStop) {
        if (!isInitialized()) {
            return;
        }
        EventManager.stopAllEvents(forceStop);
    }

    /**
     * Check if event can be gracefully stopped
     *
     * @param eventType Type of event
     * @return true if graceful stop is supported
     */
    public static boolean canGracefullyStop(@NotNull EventType eventType) {
        return EventManager.canGracefullyStop(eventType);
    }

    /**
     * Get available event types
     *
     * @return List of all event types
     */
    @NotNull
    public static List<EventType> getAvailableEventTypes() {
        return new ArrayList<>(Arrays.asList(EventType.values()));
    }
}
