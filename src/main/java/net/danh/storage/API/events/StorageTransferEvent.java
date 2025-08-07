package net.danh.storage.API.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired when a player transfers items to another player
 * This event is cancellable - cancelling it will prevent the transfer
 *
 * @author VoChiDanh, hongminh54
 * @version 2.3.2
 */
public class StorageTransferEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player sender;
    private final Player receiver;
    private final String material;
    private int amount;
    private boolean cancelled = false;

    /**
     * Create new StorageTransferEvent
     *
     * @param sender   The player sending items
     * @param receiver The player receiving items
     * @param material Material being transferred (e.g., "STONE;0")
     * @param amount   Amount being transferred
     */
    public StorageTransferEvent(@NotNull Player sender, @NotNull Player receiver,
                                @NotNull String material, int amount) {
        this.sender = sender;
        this.receiver = receiver;
        this.material = material;
        this.amount = amount;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    /**
     * Get the player sending items
     *
     * @return Sender player instance
     */
    @NotNull
    public Player getSender() {
        return sender;
    }

    /**
     * Get the player receiving items
     *
     * @return Receiver player instance
     */
    @NotNull
    public Player getReceiver() {
        return receiver;
    }

    /**
     * Get the material being transferred
     *
     * @return Material string (e.g., "STONE;0")
     */
    @NotNull
    public String getMaterial() {
        return material;
    }

    /**
     * Get the amount being transferred
     *
     * @return Amount of items
     */
    public int getAmount() {
        return amount;
    }

    /**
     * Set the amount to be transferred
     * External plugins can modify this to change the transfer amount
     *
     * @param amount New amount (must be positive)
     */
    public void setAmount(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        this.amount = amount;
    }

    /**
     * Get sender's name for convenience
     *
     * @return Sender name
     */
    @NotNull
    public String getSenderName() {
        return sender.getName();
    }

    /**
     * Get receiver's name for convenience
     *
     * @return Receiver name
     */
    @NotNull
    public String getReceiverName() {
        return receiver.getName();
    }

    /**
     * Get material display name
     *
     * @return Material name without data value
     */
    @NotNull
    public String getMaterialName() {
        return material.split(";")[0];
    }

    /**
     * Check if this is a self-transfer (same player)
     *
     * @return true if sender and receiver are the same
     */
    public boolean isSelfTransfer() {
        return sender.getUniqueId().equals(receiver.getUniqueId());
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
        return "StorageTransferEvent{" +
                "sender=" + sender.getName() +
                ", receiver=" + receiver.getName() +
                ", material='" + material + '\'' +
                ", amount=" + amount +
                ", cancelled=" + cancelled +
                '}';
    }
}
