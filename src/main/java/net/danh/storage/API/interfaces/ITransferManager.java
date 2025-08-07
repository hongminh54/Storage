package net.danh.storage.API.interfaces;

import net.danh.storage.API.exceptions.StorageException;
import net.danh.storage.Data.TransferData;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Interface for managing item transfers between players
 * Provides methods for transfer operations and queries
 *
 * @author VoChiDanh, hongminh54
 * @version 2.3.2
 */
public interface ITransferManager {

    /**
     * Transfer item between players
     *
     * @param sender   Sender player
     * @param receiver Receiver player
     * @param material Material to transfer
     * @param amount   Amount to transfer
     * @return true if transfer started successfully
     * @throws StorageException if transfer fails
     */
    boolean transferItem(@NotNull Player sender, @NotNull Player receiver,
                         @NotNull String material, int amount) throws StorageException;

    /**
     * Transfer item to player by name
     *
     * @param sender       Sender player
     * @param receiverName Receiver player name
     * @param material     Material to transfer
     * @param amount       Amount to transfer
     * @return true if transfer started successfully
     * @throws StorageException if transfer fails
     */
    boolean transferItem(@NotNull Player sender, @NotNull String receiverName,
                         @NotNull String material, int amount) throws StorageException;

    /**
     * Check if transfer is possible
     *
     * @param sender   Sender player
     * @param receiver Receiver player
     * @param material Material to transfer
     * @param amount   Amount to transfer
     * @return true if transfer is possible
     */
    boolean canTransfer(@NotNull Player sender, @NotNull Player receiver,
                        @NotNull String material, int amount);

    /**
     * Check if transfer is possible by name
     *
     * @param sender       Sender player
     * @param receiverName Receiver player name
     * @param material     Material to transfer
     * @param amount       Amount to transfer
     * @return true if transfer is possible
     */
    boolean canTransfer(@NotNull Player sender, @NotNull String receiverName,
                        @NotNull String material, int amount);

    /**
     * Get optimal transfer amount (maximum possible)
     *
     * @param sender          Sender player
     * @param receiver        Receiver player
     * @param material        Material to transfer
     * @param requestedAmount Requested amount
     * @return Optimal amount that can be transferred
     */
    int getOptimalTransferAmount(@NotNull Player sender, @NotNull Player receiver,
                                 @NotNull String material, int requestedAmount);

    /**
     * Get optimal transfer amount by name
     *
     * @param sender          Sender player
     * @param receiverName    Receiver player name
     * @param material        Material to transfer
     * @param requestedAmount Requested amount
     * @return Optimal amount that can be transferred
     */
    int getOptimalTransferAmount(@NotNull Player sender, @NotNull String receiverName,
                                 @NotNull String material, int requestedAmount);

    /**
     * Get active transfers for a player
     *
     * @param player The player
     * @return List of active transfers
     */
    @NotNull
    List<TransferData> getActiveTransfers(@NotNull Player player);

    /**
     * Get active transfers where player is sender
     *
     * @param player The player
     * @return List of outgoing transfers
     */
    @NotNull
    List<TransferData> getOutgoingTransfers(@NotNull Player player);

    /**
     * Get active transfers where player is receiver
     *
     * @param player The player
     * @return List of incoming transfers
     */
    @NotNull
    List<TransferData> getIncomingTransfers(@NotNull Player player);

    /**
     * Cancel transfer by ID
     *
     * @param transferId Transfer ID
     * @return true if cancelled successfully
     */
    boolean cancelTransfer(@NotNull String transferId);

    /**
     * Cancel all transfers for a player
     *
     * @param player The player
     * @return Number of transfers cancelled
     */
    int cancelAllTransfers(@NotNull Player player);

    /**
     * Cancel all outgoing transfers for a player
     *
     * @param player The player
     * @return Number of transfers cancelled
     */
    int cancelOutgoingTransfers(@NotNull Player player);

    /**
     * Cancel all incoming transfers for a player
     *
     * @param player The player
     * @return Number of transfers cancelled
     */
    int cancelIncomingTransfers(@NotNull Player player);

    /**
     * Check if player has active transfers
     *
     * @param player The player
     * @return true if player has active transfers
     */
    boolean hasActiveTransfers(@NotNull Player player);

    /**
     * Get transfer cooldown for player
     *
     * @param player The player
     * @return Cooldown in seconds (0 if no cooldown)
     */
    int getTransferCooldown(@NotNull Player player);

    /**
     * Check if player is on transfer cooldown
     *
     * @param player The player
     * @return true if on cooldown
     */
    boolean isOnTransferCooldown(@NotNull Player player);

    /**
     * Get maximum transfer amount per operation
     *
     * @return Maximum amount
     */
    int getMaxTransferAmount();

    /**
     * Get transfer delay in ticks
     *
     * @return Delay in ticks
     */
    int getTransferDelay();

    /**
     * Check if transfers are enabled globally
     *
     * @return true if enabled
     */
    boolean areTransfersEnabled();

    /**
     * Get total number of active transfers
     *
     * @return Number of active transfers
     */
    int getTotalActiveTransfers();
}
