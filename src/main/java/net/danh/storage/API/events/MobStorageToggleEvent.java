package net.danh.storage.API.events;

import net.danh.storage.Manager.Mob.MobStorageManager;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class MobStorageToggleEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final boolean oldState;
    private boolean newState;
    private boolean cancelled;

    public MobStorageToggleEvent(@NotNull Player player, boolean newState) {
        this.player = player;
        this.oldState = MobStorageManager.getToggleStatus(player);
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
