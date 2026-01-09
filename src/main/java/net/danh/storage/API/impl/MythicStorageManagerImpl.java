package net.danh.storage.API.impl;

import net.danh.storage.API.exceptions.StorageException;
import net.danh.storage.API.exceptions.StorageFullException;
import net.danh.storage.API.interfaces.IMythicStorageManager;
import net.danh.storage.Manager.Mythic.MythicStorageManager;
import net.danh.storage.MythicMobs.MythicMobsHelper;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MythicStorageManagerImpl implements IMythicStorageManager {

    private static MythicStorageManagerImpl instance;

    private MythicStorageManagerImpl() {
    }

    public static MythicStorageManagerImpl getInstance() {
        if (instance == null) {
            instance = new MythicStorageManagerImpl();
        }
        return instance;
    }

    @Override
    public boolean isSystemEnabled() {
        return MythicStorageManager.isSystemEnabled();
    }

    @Override
    public boolean addItem(@NotNull Player player, @NotNull String itemName, int amount) throws StorageException {
        if (!isSystemEnabled()) {
            throw new IllegalStateException("MythicStorage system is not enabled");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (!isValidItem(itemName)) {
            throw new StorageException("Invalid MythicMobs item: " + itemName);
        }

        boolean result = MythicStorageManager.addItemAmount(player, itemName, amount, true);
        if (!result) {
            throw new StorageFullException("MythicStorage is full or item not configured");
        }
        return true;
    }

    @Override
    public boolean removeItem(@NotNull Player player, @NotNull String itemName, int amount) throws StorageException {
        if (!isSystemEnabled()) {
            throw new IllegalStateException("MythicStorage system is not enabled");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        return MythicStorageManager.removeItemAmount(player, itemName, amount, true);
    }

    @Override
    public void setItem(@NotNull Player player, @NotNull String itemName, int amount) {
        if (!isSystemEnabled()) return;
        MythicStorageManager.setItemAmount(player, itemName, Math.max(0, amount));
    }

    @Override
    public int getItemAmount(@NotNull Player player, @NotNull String itemName) {
        if (!isSystemEnabled()) return 0;
        return MythicStorageManager.getPlayerItem(player, itemName);
    }

    @Override
    public boolean hasItem(@NotNull Player player, @NotNull String itemName) {
        return getItemAmount(player, itemName) > 0;
    }

    @Override
    public boolean hasEnoughItem(@NotNull Player player, @NotNull String itemName, int amount) {
        return getItemAmount(player, itemName) >= amount;
    }

    @NotNull
    @Override
    public Map<String, Integer> getAllItems(@NotNull Player player) {
        return MythicStorageManager.getPlayerAllItems(player);
    }

    @Override
    public int getTotalItems(@NotNull Player player) {
        return getAllItems(player).values().stream().mapToInt(Integer::intValue).sum();
    }

    @Override
    public int getMaxStorage(@NotNull Player player) {
        if (!isSystemEnabled()) return 0;
        return MythicStorageManager.getMaxStorage(player);
    }

    @Override
    public void setMaxStorage(@NotNull Player player, int amount) {
        if (!isSystemEnabled()) return;
        MythicStorageManager.playermaxdata.put(player.getUniqueId(), Math.max(0, amount));
    }

    @Override
    public int getRemainingSpace(@NotNull Player player) {
        return Math.max(0, getMaxStorage(player) - getTotalItems(player));
    }

    @Override
    public boolean isStorageFull(@NotNull Player player) {
        return getTotalItems(player) >= getMaxStorage(player);
    }

    @Override
    public boolean isAutoPickupEnabled(@NotNull Player player) {
        if (!isSystemEnabled()) return false;
        return MythicStorageManager.getToggleStatus(player);
    }

    @Override
    public void setAutoPickup(@NotNull Player player, boolean enabled) {
        if (!isSystemEnabled()) return;
        MythicStorageManager.setToggleStatus(player, enabled);
    }

    @Override
    public void clearStorage(@NotNull Player player) {
        if (!isSystemEnabled()) return;
        for (String item : getConfiguredItems()) {
            setItem(player, item, 0);
        }
    }

    @Override
    public void clearItem(@NotNull Player player, @NotNull String itemName) {
        setItem(player, itemName, 0);
    }

    @NotNull
    @Override
    public List<String> getConfiguredItems() {
        if (!isSystemEnabled()) return new ArrayList<>();
        return MythicStorageManager.getConfiguredDrops();
    }

    @Override
    public boolean isValidItem(@NotNull String itemName) {
        if (!isSystemEnabled()) return false;
        MythicMobsHelper helper = MythicStorageManager.getMythicMobsHelper();
        return helper != null && helper.isValidMythicItem(itemName);
    }

    @Override
    public boolean isConfiguredItem(@NotNull String itemName) {
        if (!isSystemEnabled()) return false;
        return MythicStorageManager.isConfiguredDrop(itemName);
    }

    @NotNull
    @Override
    public String getItemDisplayName(@NotNull Player player, @NotNull String itemName) {
        if (!isSystemEnabled()) return itemName;
        return MythicStorageManager.getItemDisplayNameOrId(itemName, player);
    }

    @NotNull
    @Override
    public List<String> getInvalidItems() {
        if (!isSystemEnabled()) return new ArrayList<>();
        return MythicStorageManager.getInvalidItems();
    }

    @Override
    public boolean hasInvalidItems() {
        return MythicStorageManager.hasInvalidItems();
    }

    @Override
    public void savePlayerData(@NotNull Player player) {
        if (!isSystemEnabled()) return;
        MythicStorageManager.savePlayerData(player);
    }

    @Override
    public void loadPlayerData(@NotNull Player player) {
        if (!isSystemEnabled()) return;
        MythicStorageManager.loadPlayerData(player);
    }

    @Override
    public void reload() {
        MythicStorageManager.reloadConfiguredDrops();
    }
}
