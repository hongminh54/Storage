package net.danh.storage.API.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class FriendRequestAcceptEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final UUID senderUuid;
    private boolean cancelled = false;

    public FriendRequestAcceptEvent(@NotNull Player player, @NotNull UUID senderUuid) {
        this.player = player;
        this.senderUuid = senderUuid;
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
    public UUID getSenderUuid() {
        return senderUuid;
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
