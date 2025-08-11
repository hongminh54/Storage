package net.danh.storage.API.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired when a player's storage permissions are refreshed
 * This event is cancellable - cancelling it will prevent the refresh
 *
 * @author VoChiDanh, hongminh54
 * @version 2.3.2
 */
public class StoragePermissionRefreshEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final int oldLimit;
    private int newLimit;
    private boolean cancelled = false;

    /**
     * Create new StoragePermissionRefreshEvent
     *
     * @param player   The player whose permissions are being refreshed
     * @param oldLimit The old storage limit
     * @param newLimit The new storage limit
     */
    public StoragePermissionRefreshEvent(@NotNull Player player, int oldLimit, int newLimit) {
        this.player = player;
        this.oldLimit = oldLimit;
        this.newLimit = newLimit;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    /**
     * Get the player whose permissions are being refreshed
     *
     * @return Player instance
     */
    @NotNull
    public Player getPlayer() {
        return player;
    }

    /**
     * Get the old storage limit
     *
     * @return Old limit
     */
    public int getOldLimit() {
        return oldLimit;
    }

    /**
     * Get the new storage limit
     *
     * @return New limit
     */
    public int getNewLimit() {
        return newLimit;
    }

    /**
     * Set the new storage limit
     * External plugins can modify this to override the calculated limit
     *
     * @param newLimit New limit (must be positive)
     */
    public void setNewLimit(int newLimit) {
        if (newLimit < 0) {
            throw new IllegalArgumentException("New limit cannot be negative");
        }
        this.newLimit = newLimit;
    }

    /**
     * Check if the limit has changed
     *
     * @return true if old and new limits are different
     */
    public boolean hasLimitChanged() {
        return oldLimit != newLimit;
    }

    /**
     * Get the difference between old and new limits
     *
     * @return Difference (positive = increase, negative = decrease)
     */
    public int getLimitDifference() {
        return newLimit - oldLimit;
    }

    /**
     * Check if this is a limit increase
     *
     * @return true if new limit is higher than old limit
     */
    public boolean isLimitIncrease() {
        return newLimit > oldLimit;
    }

    /**
     * Check if this is a limit decrease
     *
     * @return true if new limit is lower than old limit
     */
    public boolean isLimitDecrease() {
        return newLimit < oldLimit;
    }

    /**
     * Get player's name for convenience
     *
     * @return Player name
     */
    @NotNull
    public String getPlayerName() {
        return player.getName();
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @Override
    public String toString() {
        return "StoragePermissionRefreshEvent{" +
                "player=" + player.getName() +
                ", oldLimit=" + oldLimit +
                ", newLimit=" + newLimit +
                ", cancelled=" + cancelled +
                '}';
    }
}
