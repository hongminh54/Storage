package net.danh.storage.API.impl;

import net.danh.storage.API.StoragePlayer;
import net.danh.storage.API.exceptions.StorageException;
import net.danh.storage.API.exceptions.StorageFullException;
import net.danh.storage.API.interfaces.IStorageManager;
import net.danh.storage.Manager.MineManager;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class StorageManagerImpl implements IStorageManager {

    private static StorageManagerImpl instance;

    private StorageManagerImpl() {
    }

    public static StorageManagerImpl getInstance() {
        if (instance == null) {
            instance = new StorageManagerImpl();
        }
        return instance;
    }

    @NotNull
    @Override
    public StoragePlayer getStoragePlayer(@NotNull Player player) {
        return new StoragePlayer(player);
    }

    @Override
    public boolean addItem(@NotNull Player player, @NotNull String material, int amount) throws StorageException {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        boolean result = MineManager.addBlockAmount(player, material, amount, true);
        if (!result) {
            throw new StorageFullException("Storage is full or invalid material");
        }
        return true;
    }

    @Override
    public boolean removeItem(@NotNull Player player, @NotNull String material, int amount) throws StorageException {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        return MineManager.removeBlockAmount(player, material, amount, true);
    }

    @Override
    public void setItem(@NotNull Player player, @NotNull String material, int amount) {
        MineManager.setBlock(player, material, Math.max(0, amount));
    }

    @Override
    public int getItemAmount(@NotNull Player player, @NotNull String material) {
        return MineManager.getPlayerBlock(player, material);
    }

    @Override
    public boolean hasItem(@NotNull Player player, @NotNull String material) {
        return MineManager.hasPlayerBlock(player, material) && getItemAmount(player, material) > 0;
    }

    @Override
    public boolean hasEnoughItem(@NotNull Player player, @NotNull String material, int amount) {
        return getItemAmount(player, material) >= amount;
    }

    @NotNull
    @Override
    public Set<String> getStoredMaterials(@NotNull Player player) {
        String prefix = player.getName() + "_";
        return MineManager.playerdata.entrySet().stream()
                .filter(e -> e.getKey().startsWith(prefix) && e.getValue() > 0)
                .map(e -> e.getKey().substring(prefix.length()))
                .collect(Collectors.toSet());
    }

    @NotNull
    @Override
    public Map<String, Integer> getStoredMaterialsWithAmounts(@NotNull Player player) {
        Map<String, Integer> result = new HashMap<>();
        for (String material : getStoredMaterials(player)) {
            int amount = getItemAmount(player, material);
            if (amount > 0) {
                result.put(material, amount);
            }
        }
        return result;
    }

    @Override
    public int getTotalStoredItems(@NotNull Player player) {
        return getStoredMaterials(player).stream()
                .mapToInt(m -> getItemAmount(player, m))
                .sum();
    }

    @Override
    public int getMaxStorage(@NotNull Player player) {
        return MineManager.getMaxBlock(player);
    }

    @Override
    public void setMaxStorage(@NotNull Player player, int amount) {
        MineManager.setMaxStorageOverride(player, amount);
        MineManager.playermaxdata.put(player.getUniqueId(), Math.max(0, amount));
    }

    @Override
    public int getRemainingSpace(@NotNull Player player) {
        return Math.max(0, getMaxStorage(player) - getTotalStoredItems(player));
    }

    @Override
    public boolean isStorageFull(@NotNull Player player) {
        return getTotalStoredItems(player) >= getMaxStorage(player);
    }

    @Override
    public boolean isStorageEmpty(@NotNull Player player) {
        return getTotalStoredItems(player) == 0;
    }

    @Override
    public double getStorageUsagePercentage(@NotNull Player player) {
        int max = getMaxStorage(player);
        return max <= 0 ? 0.0 : (double) getTotalStoredItems(player) / max;
    }

    @Override
    public boolean isStorageEnabled(@NotNull Player player) {
        return MineManager.getToggleStatus(player);
    }

    @Override
    public void setStorageEnabled(@NotNull Player player, boolean enabled) {
        MineManager.setToggleStatus(player, enabled, true);
    }

    @Override
    public void clearStorage(@NotNull Player player) {
        for (String material : new HashSet<>(getStoredMaterials(player))) {
            setItem(player, material, 0);
        }
    }

    @Override
    public void clearMaterial(@NotNull Player player, @NotNull String material) {
        setItem(player, material, 0);
    }

    @NotNull
    @Override
    public List<String> getStorableMaterials() {
        return MineManager.getPluginBlocks();
    }

    @Override
    public boolean isStorableMaterial(@NotNull String material) {
        return MineManager.blocksdata.containsKey(material);
    }

    @Override
    public void savePlayerData(@NotNull Player player) {
        MineManager.savePlayerData(player);
    }

    @Override
    public void loadPlayerData(@NotNull Player player) {
        MineManager.loadPlayerData(player);
    }
}
