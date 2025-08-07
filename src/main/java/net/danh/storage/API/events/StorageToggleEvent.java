package net.danh.storage.API.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired when a player toggles their storage auto-pickup on/off
 * This event is cancellable - cancelling it will prevent the toggle
 *
 * @author VoChiDanh, hongminh54
 * @version 2.3.2
 */
public class StorageToggleEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final boolean oldState;
    private boolean newState;
    private boolean cancelled = false;

    /**
     * Create new StorageToggleEvent
     *
     * @param player   The player toggling storage
     * @param newState New storage state (true = enabled, false = disabled)
     */
    public StorageToggleEvent(@NotNull Player player, boolean newState) {
        this.player = player;
        this.newState = newState;
        // Get current state as old state
        this.oldState = net.danh.storage.Manager.MineManager.toggle.getOrDefault(player, false);
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    /**
     * Get the player toggling storage
     *
     * @return Player instance
     */
    @NotNull
    public Player getPlayer() {
        return player;
    }

    /**
     * Get the old storage state
     *
     * @return Old state (true = was enabled, false = was disabled)
     */
    public boolean getOldState() {
        return oldState;
    }

    /**
     * Get the new storage state
     *
     * @return New state (true = will be enabled, false = will be disabled)
     */
    public boolean getNewState() {
        return newState;
    }

    /**
     * Set the new storage state
     * External plugins can modify this to change the final state
     *
     * @param newState New state (true = enabled, false = disabled)
     */
    public void setNewState(boolean newState) {
        this.newState = newState;
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

    /**
     * Check if this is enabling storage (false -> true)
     *
     * @return true if enabling storage
     */
    public boolean isEnabling() {
        return !oldState && newState;
    }

    /**
     * Check if this is disabling storage (true -> false)
     *
     * @return true if disabling storage
     */
    public boolean isDisabling() {
        return oldState && !newState;
    }

    /**
     * Check if state is actually changing
     *
     * @return true if state is changing
     */
    public boolean isStateChanging() {
        return oldState != newState;
    }

    /**
     * Get state change description
     *
     * @return Human readable state change
     */
    @NotNull
    public String getStateChangeDescription() {
        if (isEnabling()) {
            return "enabling";
        } else if (isDisabling()) {
            return "disabling";
        } else {
            return "no change";
        }
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
        return "StorageToggleEvent{" +
                "player=" + player.getName() +
                ", oldState=" + oldState +
                ", newState=" + newState +
                ", stateChange='" + getStateChangeDescription() + '\'' +
                ", cancelled=" + cancelled +
                '}';
    }
}
