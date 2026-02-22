package net.danh.storage.API.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class CropStorageSellEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String itemName;
    private double worthPerItem;
    private int amount;
    private boolean cancelled;

    public CropStorageSellEvent(@NotNull Player player, @NotNull String itemName, int amount, double worthPerItem) {
        this.player = player;
        this.itemName = itemName;
        this.amount = Math.max(0, amount);
        this.worthPerItem = Math.max(0D, worthPerItem);
        this.cancelled = false;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @NotNull
    public Player getPlayer() {
        return player;
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

    public double getWorthPerItem() {
        return worthPerItem;
    }

    public void setWorthPerItem(double worthPerItem) {
        if (worthPerItem < 0) {
            throw new IllegalArgumentException("Worth cannot be negative");
        }
        this.worthPerItem = worthPerItem;
    }

    public double getTotalMoney() {
        return worthPerItem * amount;
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
