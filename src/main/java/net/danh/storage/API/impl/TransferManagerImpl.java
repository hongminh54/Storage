package net.danh.storage.API.impl;

import net.danh.storage.API.exceptions.StorageException;
import net.danh.storage.API.interfaces.ITransferManager;
import net.danh.storage.Data.TransferData;
import net.danh.storage.Manager.TransferManager;
import net.danh.storage.Utils.File;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TransferManagerImpl implements ITransferManager {

    private static TransferManagerImpl instance;

    private TransferManagerImpl() {
    }

    public static TransferManagerImpl getInstance() {
        if (instance == null) {
            instance = new TransferManagerImpl();
        }
        return instance;
    }

    @Override
    public boolean transferItem(@NotNull Player sender, @NotNull Player receiver,
                                @NotNull String material, int amount) throws StorageException {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        return TransferManager.executeTransfer(sender, receiver.getName(), material, amount, true);
    }

    @Override
    public boolean transferItem(@NotNull Player sender, @NotNull String receiverName,
                                @NotNull String material, int amount) throws StorageException {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        return TransferManager.executeTransfer(sender, receiverName, material, amount, true);
    }

    @Override
    public boolean canTransfer(@NotNull Player sender, @NotNull Player receiver,
                               @NotNull String material, int amount) {
        return TransferManager.canTransfer(sender, receiver.getName(), material, amount);
    }

    @Override
    public boolean canTransfer(@NotNull Player sender, @NotNull String receiverName,
                               @NotNull String material, int amount) {
        return TransferManager.canTransfer(sender, receiverName, material, amount);
    }

    @Override
    public int getOptimalTransferAmount(@NotNull Player sender, @NotNull Player receiver,
                                        @NotNull String material, int requestedAmount) {
        return TransferManager.getOptimalTransferAmount(sender, receiver.getName(), material, requestedAmount);
    }

    @Override
    public int getOptimalTransferAmount(@NotNull Player sender, @NotNull String receiverName,
                                        @NotNull String material, int requestedAmount) {
        return TransferManager.getOptimalTransferAmount(sender, receiverName, material, requestedAmount);
    }

    @NotNull
    @Override
    public List<TransferData> getActiveTransfers(@NotNull Player player) {
        // Active transfers are in-progress, not stored in database
        List<TransferData> result = new ArrayList<>();
        result.addAll(getOutgoingTransfers(player));
        result.addAll(getIncomingTransfers(player));
        return result;
    }

    @NotNull
    @Override
    public List<TransferData> getOutgoingTransfers(@NotNull Player player) {
        if (TransferManager.getTransferDatabase() == null) {
            return Collections.emptyList();
        }
        return TransferManager.getTransferDatabase().getTransferHistory(player.getName(), 50, 0);
    }

    @NotNull
    @Override
    public List<TransferData> getIncomingTransfers(@NotNull Player player) {
        if (TransferManager.getTransferDatabase() == null) {
            return Collections.emptyList();
        }
        return TransferManager.getTransferDatabase().getTransferHistory(player.getName(), 50, 0);
    }

    @Override
    public boolean cancelTransfer(@NotNull String transferId) {
        // Transfer IDs are player names in current implementation
        Player player = Bukkit.getPlayer(transferId);
        if (player != null && TransferManager.isTransferInProgress(player)) {
            TransferManager.cancelTransfer(player);
            return true;
        }
        return false;
    }

    @Override
    public int cancelAllTransfers(@NotNull Player player) {
        int count = 0;
        if (TransferManager.isTransferInProgress(player)) {
            TransferManager.cancelTransfer(player);
            count++;
        }
        return count;
    }

    @Override
    public int cancelOutgoingTransfers(@NotNull Player player) {
        return cancelAllTransfers(player);
    }

    @Override
    public int cancelIncomingTransfers(@NotNull Player player) {
        // Incoming transfers cannot be cancelled by receiver in current implementation
        return 0;
    }

    @Override
    public boolean hasActiveTransfers(@NotNull Player player) {
        return TransferManager.isTransferInProgress(player);
    }

    @Override
    public int getTransferCooldown(@NotNull Player player) {
        // Current implementation doesn't have cooldown tracking
        return 0;
    }

    @Override
    public boolean isOnTransferCooldown(@NotNull Player player) {
        return false;
    }

    @Override
    public int getMaxTransferAmount() {
        return File.getConfig().getInt("transfer.max_amount", Integer.MAX_VALUE);
    }

    @Override
    public int getTransferDelay() {
        return File.getConfig().getInt("transfer.delay", 3) * 20; // Convert to ticks
    }

    @Override
    public boolean areTransfersEnabled() {
        return File.getConfig().getBoolean("transfer.enabled", true);
    }

    @Override
    public int getTotalActiveTransfers() {
        // Count all online players with active transfers
        int count = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (TransferManager.isTransferInProgress(player)) {
                count++;
            }
        }
        return count;
    }
}
