package net.danh.storage.API.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired when a player transfers crop items to another player via CropStorage.
 * This event is cancellable.
 */
public class CropStorageTransferEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private final Player sender;
    private final Player receiver;
    private final String itemName;
    private boolean cancelled = false;
    private int amount;

    /**
     * Creates a new CropStorageTransferEvent.
     *
     * @param sender   The player sending the items
     * @param receiver The player receiving the items
     * @param itemName The name of the crop item being transferred
     * @param amount   The amount being transferred
     */
    public CropStorageTransferEvent(@NotNull Player sender, @NotNull Player receiver,
                                    @NotNull String itemName, int amount) {
        this.sender = sender;
        this.receiver = receiver;
        this.itemName = itemName;
        this.amount = amount;
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
     * Gets the name of the crop item being transferred.
     *
     * @return The item name
     */
    @NotNull
    public String getItemName() {
        return itemName;
    }

    /**
     * Gets the amount being transferred.
     *
     * @return The amount
     */
    public int getAmount() {
        return amount;
    }

    /**
     * Sets the amount being transferred.
     *
     * @param amount The new amount
     */
    public void setAmount(int amount) {
        this.amount = amount;
    }
}
