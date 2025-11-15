package net.danh.storage.API;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * API for plugin integration hooks
 * Allows external plugins to hook into storage operations
 *
 * @author hongminh54
 * @version 2.3.3
 */
public class StorageHookAPI {

    private static final Map<HookPriority, List<DepositHook>> depositHooks = new ConcurrentHashMap<>();
    private static final Map<HookPriority, List<WithdrawHook>> withdrawHooks = new ConcurrentHashMap<>();
    private static final Map<HookPriority, List<TransferHook>> transferHooks = new ConcurrentHashMap<>();
    private static final Map<HookPriority, List<ToggleHook>> toggleHooks = new ConcurrentHashMap<>();

    static {
        for (HookPriority priority : HookPriority.values()) {
            depositHooks.put(priority, new ArrayList<>());
            withdrawHooks.put(priority, new ArrayList<>());
            transferHooks.put(priority, new ArrayList<>());
            toggleHooks.put(priority, new ArrayList<>());
        }
    }

    /**
     * Register a deposit hook
     *
     * @param hook     The hook to register
     * @param priority Hook priority
     */
    public static void registerDepositHook(@NotNull DepositHook hook, @NotNull HookPriority priority) {
        depositHooks.get(priority).add(hook);
    }

    /**
     * Register a deposit hook with normal priority
     *
     * @param hook The hook to register
     */
    public static void registerDepositHook(@NotNull DepositHook hook) {
        registerDepositHook(hook, HookPriority.NORMAL);
    }

    /**
     * Register a withdraw hook
     *
     * @param hook     The hook to register
     * @param priority Hook priority
     */
    public static void registerWithdrawHook(@NotNull WithdrawHook hook, @NotNull HookPriority priority) {
        withdrawHooks.get(priority).add(hook);
    }

    /**
     * Register a withdraw hook with normal priority
     *
     * @param hook The hook to register
     */
    public static void registerWithdrawHook(@NotNull WithdrawHook hook) {
        registerWithdrawHook(hook, HookPriority.NORMAL);
    }

    /**
     * Register a transfer hook
     *
     * @param hook     The hook to register
     * @param priority Hook priority
     */
    public static void registerTransferHook(@NotNull TransferHook hook, @NotNull HookPriority priority) {
        transferHooks.get(priority).add(hook);
    }

    /**
     * Register a transfer hook with normal priority
     *
     * @param hook The hook to register
     */
    public static void registerTransferHook(@NotNull TransferHook hook) {
        registerTransferHook(hook, HookPriority.NORMAL);
    }

    /**
     * Register a toggle hook
     *
     * @param hook     The hook to register
     * @param priority Hook priority
     */
    public static void registerToggleHook(@NotNull ToggleHook hook, @NotNull HookPriority priority) {
        toggleHooks.get(priority).add(hook);
    }

    /**
     * Register a toggle hook with normal priority
     *
     * @param hook The hook to register
     */
    public static void registerToggleHook(@NotNull ToggleHook hook) {
        registerToggleHook(hook, HookPriority.NORMAL);
    }

    /**
     * Unregister a deposit hook
     *
     * @param hook The hook to unregister
     */
    public static void unregisterDepositHook(@NotNull DepositHook hook) {
        for (List<DepositHook> hooks : depositHooks.values()) {
            hooks.remove(hook);
        }
    }

    /**
     * Unregister a withdraw hook
     *
     * @param hook The hook to unregister
     */
    public static void unregisterWithdrawHook(@NotNull WithdrawHook hook) {
        for (List<WithdrawHook> hooks : withdrawHooks.values()) {
            hooks.remove(hook);
        }
    }

    /**
     * Unregister a transfer hook
     *
     * @param hook The hook to unregister
     */
    public static void unregisterTransferHook(@NotNull TransferHook hook) {
        for (List<TransferHook> hooks : transferHooks.values()) {
            hooks.remove(hook);
        }
    }

    /**
     * Unregister a toggle hook
     *
     * @param hook The hook to unregister
     */
    public static void unregisterToggleHook(@NotNull ToggleHook hook) {
        for (List<ToggleHook> hooks : toggleHooks.values()) {
            hooks.remove(hook);
        }
    }

    /**
     * Call before deposit hooks
     *
     * @param player   The player
     * @param material Material being deposited
     * @param amount   Amount being deposited
     * @return true if operation should continue, false to cancel
     */
    public static boolean callBeforeDeposit(@NotNull Player player, @NotNull String material, int amount) {
        for (HookPriority priority : HookPriority.values()) {
            for (DepositHook hook : depositHooks.get(priority)) {
                if (!hook.onBeforeDeposit(player, material, amount)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Call after deposit hooks
     *
     * @param player   The player
     * @param material Material deposited
     * @param amount   Amount deposited
     */
    public static void callAfterDeposit(@NotNull Player player, @NotNull String material, int amount) {
        for (HookPriority priority : HookPriority.values()) {
            for (DepositHook hook : depositHooks.get(priority)) {
                hook.onAfterDeposit(player, material, amount);
            }
        }
    }

    /**
     * Call before withdraw hooks
     *
     * @param player   The player
     * @param material Material being withdrawn
     * @param amount   Amount being withdrawn
     * @return true if operation should continue, false to cancel
     */
    public static boolean callBeforeWithdraw(@NotNull Player player, @NotNull String material, int amount) {
        for (HookPriority priority : HookPriority.values()) {
            for (WithdrawHook hook : withdrawHooks.get(priority)) {
                if (!hook.onBeforeWithdraw(player, material, amount)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Call after withdraw hooks
     *
     * @param player   The player
     * @param material Material withdrawn
     * @param amount   Amount withdrawn
     */
    public static void callAfterWithdraw(@NotNull Player player, @NotNull String material, int amount) {
        for (HookPriority priority : HookPriority.values()) {
            for (WithdrawHook hook : withdrawHooks.get(priority)) {
                hook.onAfterWithdraw(player, material, amount);
            }
        }
    }

    /**
     * Call before transfer hooks
     *
     * @param sender   Sender player
     * @param receiver Receiver player
     * @param material Material being transferred
     * @param amount   Amount being transferred
     * @return true if operation should continue, false to cancel
     */
    public static boolean callBeforeTransfer(@NotNull Player sender, @NotNull Player receiver,
                                             @NotNull String material, int amount) {
        for (HookPriority priority : HookPriority.values()) {
            for (TransferHook hook : transferHooks.get(priority)) {
                if (!hook.onBeforeTransfer(sender, receiver, material, amount)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Call after transfer hooks
     *
     * @param sender   Sender player
     * @param receiver Receiver player
     * @param material Material transferred
     * @param amount   Amount transferred
     */
    public static void callAfterTransfer(@NotNull Player sender, @NotNull Player receiver,
                                         @NotNull String material, int amount) {
        for (HookPriority priority : HookPriority.values()) {
            for (TransferHook hook : transferHooks.get(priority)) {
                hook.onAfterTransfer(sender, receiver, material, amount);
            }
        }
    }

    /**
     * Call before toggle hooks
     *
     * @param player   The player
     * @param newState New toggle state
     * @return true if operation should continue, false to cancel
     */
    public static boolean callBeforeToggle(@NotNull Player player, boolean newState) {
        for (HookPriority priority : HookPriority.values()) {
            for (ToggleHook hook : toggleHooks.get(priority)) {
                if (!hook.onBeforeToggle(player, newState)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Call after toggle hooks
     *
     * @param player   The player
     * @param newState New toggle state
     */
    public static void callAfterToggle(@NotNull Player player, boolean newState) {
        for (HookPriority priority : HookPriority.values()) {
            for (ToggleHook hook : toggleHooks.get(priority)) {
                hook.onAfterToggle(player, newState);
            }
        }
    }

    /**
     * Clear all registered hooks
     */
    public static void clearAllHooks() {
        for (List<DepositHook> hooks : depositHooks.values()) {
            hooks.clear();
        }
        for (List<WithdrawHook> hooks : withdrawHooks.values()) {
            hooks.clear();
        }
        for (List<TransferHook> hooks : transferHooks.values()) {
            hooks.clear();
        }
        for (List<ToggleHook> hooks : toggleHooks.values()) {
            hooks.clear();
        }
    }

    /**
     * Hook priority levels
     */
    public enum HookPriority {
        LOWEST,
        LOW,
        NORMAL,
        HIGH,
        HIGHEST
    }

    /**
     * Hook interface for deposit operations
     */
    public interface DepositHook {
        /**
         * Called before item is deposited
         *
         * @param player   The player
         * @param material Material being deposited
         * @param amount   Amount being deposited
         * @return true to allow, false to cancel
         */
        boolean onBeforeDeposit(@NotNull Player player, @NotNull String material, int amount);

        /**
         * Called after item is deposited
         *
         * @param player   The player
         * @param material Material deposited
         * @param amount   Amount deposited
         */
        void onAfterDeposit(@NotNull Player player, @NotNull String material, int amount);
    }

    /**
     * Hook interface for withdraw operations
     */
    public interface WithdrawHook {
        /**
         * Called before item is withdrawn
         *
         * @param player   The player
         * @param material Material being withdrawn
         * @param amount   Amount being withdrawn
         * @return true to allow, false to cancel
         */
        boolean onBeforeWithdraw(@NotNull Player player, @NotNull String material, int amount);

        /**
         * Called after item is withdrawn
         *
         * @param player   The player
         * @param material Material withdrawn
         * @param amount   Amount withdrawn
         */
        void onAfterWithdraw(@NotNull Player player, @NotNull String material, int amount);
    }

    /**
     * Hook interface for transfer operations
     */
    public interface TransferHook {
        /**
         * Called before item is transferred
         *
         * @param sender   Sender player
         * @param receiver Receiver player
         * @param material Material being transferred
         * @param amount   Amount being transferred
         * @return true to allow, false to cancel
         */
        boolean onBeforeTransfer(@NotNull Player sender, @NotNull Player receiver,
                                 @NotNull String material, int amount);

        /**
         * Called after item is transferred
         *
         * @param sender   Sender player
         * @param receiver Receiver player
         * @param material Material transferred
         * @param amount   Amount transferred
         */
        void onAfterTransfer(@NotNull Player sender, @NotNull Player receiver,
                             @NotNull String material, int amount);
    }

    /**
     * Hook interface for toggle operations
     */
    public interface ToggleHook {
        /**
         * Called before storage is toggled
         *
         * @param player   The player
         * @param newState New toggle state
         * @return true to allow, false to cancel
         */
        boolean onBeforeToggle(@NotNull Player player, boolean newState);

        /**
         * Called after storage is toggled
         *
         * @param player   The player
         * @param newState New toggle state
         */
        void onAfterToggle(@NotNull Player player, boolean newState);
    }
}
