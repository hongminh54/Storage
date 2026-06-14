package net.danh.storage.API;

import net.danh.storage.API.exceptions.StorageException;
import net.danh.storage.API.exceptions.StorageFullException;
import net.danh.storage.Manager.Mob.MobStorageManager;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MobStorageAPI {

    public static boolean isSystemEnabled() {
        return MobStorageManager.isSystemEnabled();
    }

    public static int getMobItemAmount(@NotNull Player player, @NotNull String itemName) {
        if (!isSystemEnabled()) {
            return 0;
        }
        return MobStorageManager.getPlayerItem(player, itemName);
    }

    public static boolean addMobItem(@NotNull Player player, @NotNull String itemName, int amount)
            throws StorageException {
        if (!isSystemEnabled()) {
            throw new IllegalStateException("MobStorage system is not enabled");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (!MobStorageManager.isConfiguredDrop(itemName)) {
            throw new StorageException("Invalid mob item: " + itemName);
        }

        boolean result = MobStorageManager.addItemAmount(player, itemName, amount, true);
        if (!result) {
            throw new StorageFullException("MobStorage is full or invalid item");
        }
        return true;
    }

    public static boolean addMobItemSilent(@NotNull Player player, @NotNull String itemName, int amount) {
        if (!isSystemEnabled() || amount <= 0) {
            return false;
        }
        return MobStorageManager.addItemAmount(player, itemName, amount, false);
    }

    public static boolean removeMobItem(@NotNull Player player, @NotNull String itemName, int amount)
            throws StorageException {
        if (!isSystemEnabled()) {
            throw new IllegalStateException("MobStorage system is not enabled");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (!MobStorageManager.isConfiguredDrop(itemName)) {
            throw new StorageException("Invalid mob item: " + itemName);
        }
        return MobStorageManager.removeItemAmount(player, itemName, amount, true);
    }

    public static boolean removeMobItemSilent(@NotNull Player player, @NotNull String itemName, int amount) {
        if (!isSystemEnabled() || amount <= 0) {
            return false;
        }
        return MobStorageManager.removeItemAmount(player, itemName, amount, false);
    }

    public static void setMobItemAmount(@NotNull Player player, @NotNull String itemName, int amount) {
        if (!isSystemEnabled() || !MobStorageManager.isConfiguredDrop(itemName)) {
            return;
        }
        MobStorageManager.setItemAmount(player, itemName, Math.max(0, amount));
    }

    public static int getMaxMobStorage(@NotNull Player player) {
        if (!isSystemEnabled()) {
            return 0;
        }
        return MobStorageManager.getMaxStorage(player);
    }

    public static boolean isMobAutoPickupEnabled(@NotNull Player player) {
        return isSystemEnabled() && MobStorageManager.getToggleStatus(player);
    }

    public static boolean isMobAutoPickupEnabledForItem(@NotNull Player player, @NotNull String itemName) {
        return isSystemEnabled() && MobStorageManager.isAutoPickupEnabledForItem(player, itemName);
    }

    public static boolean toggleMobAutoPickupForItem(@NotNull Player player, @NotNull String itemName) {
        return isSystemEnabled() && MobStorageManager.toggleItemAutoPickup(player, itemName, true);
    }

    public static void setMobAutoPickup(@NotNull Player player, boolean enabled) {
        if (isSystemEnabled()) {
            MobStorageManager.setToggleStatus(player, enabled, true);
        }
    }

    public static boolean isMobAutoSellEnabledForItem(@NotNull Player player, @NotNull String itemName) {
        return isSystemEnabled() && MobStorageManager.isAutoSellEnabledForItem(player, itemName);
    }

    public static boolean toggleMobAutoSellForItem(@NotNull Player player, @NotNull String itemName) {
        return isSystemEnabled() && MobStorageManager.toggleItemAutoSell(player, itemName);
    }

    public static boolean setMobAutoSellForItem(@NotNull Player player, @NotNull String itemName, boolean enabled) {
        return isSystemEnabled() && MobStorageManager.setItemAutoSell(player, itemName, enabled);
    }

    public static int clearMobAutoSell(@NotNull Player player) {
        if (!isSystemEnabled()) {
            return 0;
        }
        return MobStorageManager.clearAutoSell(player);
    }

    @NotNull
    public static Set<String> getEnabledMobAutoSellItems(@NotNull Player player) {
        if (!isSystemEnabled()) {
            return Collections.emptySet();
        }
        return MobStorageManager.getEnabledAutoSellItems(player);
    }

    @NotNull
    public static String getMobSellableSymbol(@NotNull String itemName) {
        if (!isSystemEnabled()) {
            return "";
        }
        return MobStorageManager.getSellableSymbol(itemName);
    }

    @NotNull
    public static List<String> getConfiguredMobItems() {
        if (!isSystemEnabled()) {
            return Collections.emptyList();
        }
        return MobStorageManager.getConfiguredDrops();
    }

    @NotNull
    public static Map<String, Integer> getPlayerMobItems(@NotNull Player player) {
        if (!isSystemEnabled()) {
            return new HashMap<>();
        }
        return MobStorageManager.getPlayerAllItems(player);
    }
}
