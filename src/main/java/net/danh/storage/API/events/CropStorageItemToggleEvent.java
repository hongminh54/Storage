package net.danh.storage.API.events;

import net.danh.storage.Manager.Crop.CropStorageManager;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a player toggles auto-pickup for a specific crop item.
 */
public class CropStorageItemToggleEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String itemName;
    private final boolean oldState;
    private boolean newState;
    private boolean cancelled;

    public CropStorageItemToggleEvent(@NotNull Player player, @NotNull String itemName, boolean newState) {
        this.player = player;
        this.itemName = itemName;
        this.oldState = CropStorageManager.isAutoPickupEnabledForItem(player, itemName);
        this.newState = newState;
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

    public boolean getOldState() {
        return oldState;
    }

    public boolean getNewState() {
        return newState;
    }

    public void setNewState(boolean newState) {
        this.newState = newState;
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
