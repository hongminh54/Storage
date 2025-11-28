package net.danh.storage.API.events;

import net.danh.storage.Recipe.Recipe;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Called when a recipe is created, updated, deleted, or duplicated
 *
 * @author hongminh54
 * @version 2.3.4
 */
public class RecipeCreateEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private final Player creator;
    private final Recipe recipe;
    private final Action action;
    private boolean cancelled = false;

    public RecipeCreateEvent(@Nullable Player creator, @NotNull Recipe recipe, @NotNull Action action) {
        this.creator = creator;
        this.recipe = recipe;
        this.action = action;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return handlers;
    }

    /**
     * Get the player who performed the action (null if done by plugin/console)
     *
     * @return Player or null
     */
    @Nullable
    public Player getCreator() {
        return creator;
    }

    /**
     * Get the recipe being modified
     *
     * @return The recipe
     */
    @NotNull
    public Recipe getRecipe() {
        return recipe;
    }

    /**
     * Get the action being performed
     *
     * @return Action type
     */
    @NotNull
    public Action getAction() {
        return action;
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

    /**
     * Recipe action enum
     */
    public enum Action {
        /**
         * New recipe created
         */
        CREATE,
        /**
         * Existing recipe updated
         */
        UPDATE,
        /**
         * Recipe deleted
         */
        DELETE,
        /**
         * Recipe duplicated
         */
        DUPLICATE
    }
}
