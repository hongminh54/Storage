package net.danh.storage.API.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Event fired when a player transfers multiple mob drop items to another player via MobStorage.
 * This event is cancellable.
 */
public class MobStorageMultiTransferEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private final Player sender;
    private final Player receiver;
    private final Map<String, Integer> items;
    private boolean cancelled = false;

    /**
     * Creates a new MobStorageMultiTransferEvent.
     *
     * @param sender   The player sending the items
     * @param receiver The player receiving the items
     * @param items    Map of item names to amounts being transferred
     */
    public MobStorageMultiTransferEvent(@NotNull Player sender, @NotNull Player receiver,
                                        @NotNull Map<String, Integer> items) {
        this.sender = sender;
        this.receiver = receiver;
        this.items = items;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    @NotNull
    public HandlerList getHandlers() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    /**
     * Gets the player sending the items.
     *
     * @return The sender
     */
    @NotNull
    public Player getSender() {
        return sender;
    }

    /**
     * Gets the player receiving the items.
     *
     * @return The receiver
     */
    @NotNull
    public Player getReceiver() {
        return receiver;
    }

    /**
     * Gets the map of item names to amounts being transferred.
     *
     * @return The items map
     */
    @NotNull
    public Map<String, Integer> getItems() {
        return items;
    }
}
