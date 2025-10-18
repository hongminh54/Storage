package net.danh.storage.API.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired when a player converts materials
 *
 * @author hongminh54
 * @version 2.3.3
 */
public class MaterialConvertEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final String fromMaterial;
    private final String toMaterial;
    private int amount;
    private boolean cancelled;

    public MaterialConvertEvent(@NotNull Player player, @NotNull String fromMaterial,
                                @NotNull String toMaterial, int amount) {
        this.player = player;
        this.fromMaterial = fromMaterial;
        this.toMaterial = toMaterial;
        this.amount = amount;
        this.cancelled = false;
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }

    @NotNull
    public String getFromMaterial() {
        return fromMaterial;
    }

    @NotNull
    public String getToMaterial() {
        return toMaterial;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = Math.max(0, amount);
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return handlers;
    }
}
