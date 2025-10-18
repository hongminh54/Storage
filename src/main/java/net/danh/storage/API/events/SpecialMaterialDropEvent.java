package net.danh.storage.API.events;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Event fired when checking for special material drops
 *
 * @author hongminh54
 * @version 2.3.3
 */
public class SpecialMaterialDropEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final Block block;
    private String enchantType;
    private boolean cancelled;

    public SpecialMaterialDropEvent(@NotNull Player player, @NotNull Block block, @Nullable String enchantType) {
        this.player = player;
        this.block = block;
        this.enchantType = enchantType;
        this.cancelled = false;
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }

    @NotNull
    public Block getBlock() {
        return block;
    }

    @Nullable
    public String getEnchantType() {
        return enchantType;
    }

    public void setEnchantType(@Nullable String enchantType) {
        this.enchantType = enchantType;
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
