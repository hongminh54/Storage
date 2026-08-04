package net.danh.storage.API.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class MobStorageTransferEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player sender;
    private final Player receiver;
    private final String itemName;
    private int amount;
    private boolean cancelled;

    public MobStorageTransferEvent(@NotNull Player sender, @NotNull Player receiver,
                                   @NotNull String itemName, int amount) {
        this.sender = sender;
        this.receiver = receiver;
        this.itemName = itemName;
        this.amount = amount;
        this.cancelled = false;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @NotNull
    public Player getSender() {
        return sender;
    }

    @NotNull
    public Player getReceiver() {
        return receiver;
    }

    @NotNull
    public String getItemName() {
        return itemName;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        this.amount = amount;
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
}
