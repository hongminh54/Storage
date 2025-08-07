package net.danh.storage.API.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired when a player deposits items into their storage
 * This event is cancellable - cancelling it will prevent the deposit
 *
 * @author VoChiDanh, hongminh54
 * @version 2.3.2
 */
public class StorageDepositEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String material;
    private int amount;
    private boolean cancelled = false;

    /**
     * Create new StorageDepositEvent
     *
     * @param player   The player depositing items
     * @param material Material being deposited (e.g., "STONE;0")
     * @param amount   Amount being deposited
     */
    public StorageDepositEvent(@NotNull Player player, @NotNull String material, int amount) {
        this.player = player;
        this.material = material;
        this.amount = amount;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    /**
     * Get the player depositing items
     *
     * @return Player instance
     */
    @NotNull
    public Player getPlayer() {
        return player;
    }

    /**
     * Get the material being deposited
     *
     * @return Material string (e.g., "STONE;0")
     */
    @NotNull
    public String getMaterial() {
        return material;
    }

    /**
     * Get the amount being deposited
     *
     * @return Amount of items
     */
    public int getAmount() {
        return amount;
    }

    /**
     * Set the amount to be deposited
     * External plugins can modify this to change the deposit amount
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
     * Get player's name for convenience
     *
     * @return Player name
     */
    @NotNull
    public String getPlayerName() {
        return player.getName();
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
        return "StorageDepositEvent{" +
                "player=" + player.getName() +
                ", material='" + material + '\'' +
                ", amount=" + amount +
                ", cancelled=" + cancelled +
                '}';
    }
}
