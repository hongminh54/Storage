package net.danh.storage.API.events;

import net.danh.storage.Recipe.Recipe;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a player crafts a custom recipe
 *
 * @author hongminh54
 * @version 2.3.4
 */
public class RecipeCraftEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;

    private final Player player;
    private final Recipe recipe;
    private int amount;
    private final ItemStack result;
    private final CraftPhase phase;

    /**
     * Crafting phase enum
     */
    public enum CraftPhase {
        /** Before crafting starts (can cancel) */
        PRE_CRAFT,
        /** After materials removed, before giving result */
        POST_CRAFT,
        /** After crafting completed successfully */
        COMPLETE
    }

    public RecipeCraftEvent(@NotNull Player player, @NotNull Recipe recipe, 
                           int amount, @NotNull ItemStack result, @NotNull CraftPhase phase) {
        this.player = player;
        this.recipe = recipe;
        this.amount = amount;
        this.result = result;
        this.phase = phase;
    }

    /**
     * Get the player crafting
     *
     * @return The player
     */
    @NotNull
    public Player getPlayer() {
        return player;
    }

    /**
     * Get the recipe being crafted
     *
     * @return The recipe
     */
    @NotNull
    public Recipe getRecipe() {
        return recipe;
    }

    /**
     * Get the amount being crafted
     *
     * @return Amount
     */
    public int getAmount() {
        return amount;
    }

    /**
     * Set the amount to craft (only in PRE_CRAFT phase)
     *
     * @param amount New amount
     */
    public void setAmount(int amount) {
        if (phase == CraftPhase.PRE_CRAFT) {
            this.amount = Math.max(1, amount);
        }
    }

    /**
     * Get the result ItemStack
     *
     * @return Result item
     */
    @NotNull
    public ItemStack getResult() {
        return result;
    }

    /**
     * Get the current crafting phase
     *
     * @return Craft phase
     */
    @NotNull
    public CraftPhase getPhase() {
        return phase;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        if (phase == CraftPhase.PRE_CRAFT) {
            this.cancelled = cancel;
        }
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
