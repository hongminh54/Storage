package net.danh.storage.API;

import net.danh.storage.Manager.Friend.FriendManager;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class FriendStorageAPI {

    public static boolean isSystemEnabled() {
        return FriendManager.isSystemEnabled();
    }

    public static boolean sendFriendRequest(@NotNull Player sender, @NotNull Player target) {
        return FriendManager.sendRequest(sender, target);
    }

    public static boolean acceptFriendRequest(@NotNull Player player, @NotNull UUID senderUuid) {
        return FriendManager.acceptRequest(player, senderUuid);
    }

    public static boolean denyFriendRequest(@NotNull Player player, @NotNull UUID senderUuid) {
        return FriendManager.denyRequest(player, senderUuid);
    }

    public static boolean removeFriend(@NotNull Player player, @NotNull UUID targetUuid) {
        return FriendManager.removeFriend(player, targetUuid);
    }

    public static boolean isFriend(@NotNull UUID playerA, @NotNull UUID playerB) {
        return FriendManager.isFriend(playerA, playerB);
    }

    public static boolean hasSharedAccess(@NotNull UUID ownerUuid, @NotNull UUID actorUuid) {
        return FriendManager.hasSharedAccess(ownerUuid, actorUuid);
    }

    public static Set<UUID> getFriends(@NotNull UUID playerUuid) {
        return FriendManager.getFriends(playerUuid);
    }

    public static Map<UUID, Long> getPendingRequests(@NotNull UUID receiverUuid) {
        return FriendManager.getPendingRequests(receiverUuid);
    }

    public static int getFriendCount(@NotNull UUID playerUuid) {
        return FriendManager.getFriendCount(playerUuid);
    }

    public static int getMaxFriends(@NotNull Player player) {
        return FriendManager.getMaxFriends(player);
    }
}
