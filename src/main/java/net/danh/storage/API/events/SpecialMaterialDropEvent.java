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
    private final String sourceType;
    private final String sourceKey;
    private String enchantType;
    private boolean cancelled;

    public SpecialMaterialDropEvent(@NotNull Player player, @NotNull Block block, @Nullable String enchantType) {
        this(player, block, enchantType, "block", block.getType().name());
    }

    public SpecialMaterialDropEvent(@NotNull Player player, @Nullable Block block, @Nullable String enchantType,
                                    @NotNull String sourceType, @Nullable String sourceKey) {
        this.player = player;
        this.block = block;
        this.enchantType = enchantType;
        this.sourceType = sourceType;
        this.sourceKey = sourceKey;
        this.cancelled = false;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return handlers;
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }

    @Nullable
    public Block getBlock() {
        return block;
    }

    @NotNull
    public String getSourceType() {
        return sourceType;
    }

    @Nullable
    public String getSourceKey() {
        return sourceKey;
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
}
