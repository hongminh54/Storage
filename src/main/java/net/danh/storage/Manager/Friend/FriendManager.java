package net.danh.storage.Manager.Friend;

import net.danh.storage.API.events.FriendRemoveEvent;
import net.danh.storage.API.events.FriendRequestAcceptEvent;
import net.danh.storage.API.events.FriendRequestDenyEvent;
import net.danh.storage.API.events.FriendRequestSendEvent;
import net.danh.storage.Database.FriendDatabase;
import net.danh.storage.Storage;
import net.danh.storage.Utils.ChatUtils;
import net.danh.storage.Utils.File;
import net.danh.storage.Utils.SchedulerUtil;
import net.danh.storage.Utils.TaskWrapper;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FriendManager {

    private static final String[] STORAGE_TYPES = {"storage", "mythicstorage", "cropstorage"};
    private static final Map<UUID, Set<UUID>> friendCache = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<UUID, Long>> pendingRequestCache = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<String, Boolean>> accessSettingCache = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<String, Boolean>> actionSettingCache = new ConcurrentHashMap<>();
    private static final Set<UUID> loadingPlayerData = ConcurrentHashMap.newKeySet();
    private static FriendDatabase database;
    private static boolean systemEnabled = false;
    private static TaskWrapper autoCleanTask;

    public static void initialize() {
        Storage.getStorage().getLogger().info("[FriendStorage] Initializing Friend Storage feature...");

        friendCache.clear();
        pendingRequestCache.clear();
        accessSettingCache.clear();
        actionSettingCache.clear();
        loadingPlayerData.clear();
        database = null;

        if (autoCleanTask != null) {
            autoCleanTask.cancel();
            autoCleanTask = null;
        }

        systemEnabled = File.getFriendStorageConfig().getBoolean("settings.enabled", true);
        if (!systemEnabled) {
            Storage.getStorage().getLogger().info("[FriendStorage] Feature is disabled in config");
            return;
        }

        database = new FriendDatabase();
        database.createTables();
        database.cleanExpiredRequests();

        scheduleAutoCleanTask();

        Storage.getStorage().getLogger().info("[FriendStorage] Initialization completed!");
    }

    public static boolean isSystemEnabled() {
        return systemEnabled;
    }

    public static FriendDatabase getDatabase() {
        return database;
    }

    public static void loadPlayerData(@NotNull Player player) {
        if (!systemEnabled || database == null) return;

        UUID uuid = player.getUniqueId();
        loadingPlayerData.remove(uuid);
        loadPlayerData(uuid);
    }

    public static void loadPlayerDataAsync(@NotNull Player player, Runnable callback) {
        if (!systemEnabled || database == null) {
            if (callback != null) {
                SchedulerUtil.runTask(Storage.getStorage(), player, callback);
            }
            return;
        }

        UUID uuid = player.getUniqueId();
        FriendDatabase currentDatabase = database;
        loadingPlayerData.add(uuid);
        SchedulerUtil.runTaskAsynchronously(Storage.getStorage(), () -> {
            boolean loaded = loadPlayerData(uuid, currentDatabase, true);
            if (loaded && callback != null) {
                SchedulerUtil.runTask(Storage.getStorage(), player, callback);
            }
        });
    }

    private static void loadPlayerData(@NotNull UUID uuid) {
        loadPlayerData(uuid, database, false);
    }

    private static boolean loadPlayerData(@NotNull UUID uuid, FriendDatabase currentDatabase, boolean requireLoadingToken) {
        if (currentDatabase == null) return false;

        Set<UUID> friends = currentDatabase.getFriends(uuid);
        Map<UUID, Long> pending = currentDatabase.getPendingRequests(uuid);
        boolean defaultValue = File.getFriendStorageConfig().getBoolean("settings.default_access_allowed", false);
        Map<String, Boolean> accessSettings = currentDatabase.getAllAccessSettings(uuid, defaultValue);
        Map<String, Boolean> actionSettings = flattenActionSettings(
                currentDatabase.getAllActionSettings(uuid, defaultValue, defaultValue));

        if (requireLoadingToken && !loadingPlayerData.remove(uuid)) {
            return false;
        }

        friendCache.put(uuid, Collections.synchronizedSet(new LinkedHashSet<>(friends)));
        pendingRequestCache.put(uuid, Collections.synchronizedMap(new LinkedHashMap<>(pending)));
        accessSettingCache.put(uuid, new ConcurrentHashMap<>(accessSettings));
        actionSettingCache.put(uuid, new ConcurrentHashMap<>(actionSettings));
        return true;
    }

    public static void cleanupPlayerData(@NotNull Player player) {
        UUID uuid = player.getUniqueId();
        friendCache.remove(uuid);
        pendingRequestCache.remove(uuid);
        accessSettingCache.remove(uuid);
        actionSettingCache.remove(uuid);
        loadingPlayerData.remove(uuid);
    }

    public static boolean sendRequest(@NotNull Player sender, @NotNull Player target) {
        if (!systemEnabled) return false;

        return sendRequest(sender, target.getUniqueId(), target);
    }

    public static boolean sendRequest(@NotNull Player sender, @NotNull UUID receiverUuid) {
        if (!systemEnabled) return false;

        Player receiverPlayer = Bukkit.getPlayer(receiverUuid);
        return sendRequest(sender, receiverUuid, receiverPlayer);
    }

    private static boolean sendRequest(@NotNull Player sender, @NotNull UUID receiverUuid, Player receiverPlayer) {
        if (!systemEnabled) return false;
        if (database == null) return false;

        UUID senderUuid = sender.getUniqueId();
        UUID targetUuid = receiverUuid;

        if (senderUuid.equals(targetUuid)) return false;
        if (isFriend(senderUuid, targetUuid)) return false;
        if (hasPendingRequest(senderUuid, targetUuid)) return false;

        // Check max friends for sender
        int senderMax = getMaxFriends(sender);
        int senderCount = getFriendCount(senderUuid);
        if (senderCount >= senderMax) return false;

        // Check max friends for target
        int targetMax = receiverPlayer != null ? getMaxFriends(receiverPlayer)
                : File.getFriendStorageConfig().getInt("settings.max_friends_default", 10);
        int targetCount = getFriendCount(targetUuid);
        if (targetCount >= targetMax) return false;

        // Fire event only when receiver is online (event signature requires Player target)
        if (receiverPlayer != null) {
            FriendRequestSendEvent event = new FriendRequestSendEvent(sender, receiverPlayer);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) return false;
        }

        int expireSeconds = File.getFriendStorageConfig().getInt("settings.request_expire_seconds", 300);
        long expiresAt = expireSeconds > 0 ? System.currentTimeMillis() + (expireSeconds * 1000L) : 0;

        database.addRequest(senderUuid, targetUuid, expiresAt);

        // Update target's pending cache if online
        Map<UUID, Long> targetPending = pendingRequestCache.get(targetUuid);
        if (targetPending != null) {
            targetPending.put(senderUuid, expiresAt);
        }

        return true;
    }

    public static boolean acceptRequest(@NotNull Player player, @NotNull UUID senderUuid) {
        if (!systemEnabled) return false;

        UUID playerUuid = player.getUniqueId();

        if (!hasPendingRequest(senderUuid, playerUuid)) return false;

        Player senderPlayer = Bukkit.getPlayer(senderUuid);

        // Fire event
        FriendRequestAcceptEvent event = new FriendRequestAcceptEvent(player, senderUuid);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;

        // Remove request and add friendship
        database.removeRequest(senderUuid, playerUuid);
        database.addFriend(playerUuid, senderUuid);
        applyMutualAccessSettings(playerUuid, senderUuid);

        // Update caches
        Map<UUID, Long> pending = pendingRequestCache.get(playerUuid);
        if (pending != null) {
            pending.remove(senderUuid);
        }

        Set<UUID> playerFriends = friendCache.get(playerUuid);
        if (playerFriends != null) {
            playerFriends.add(senderUuid);
        }

        // Update sender's cache if online
        if (senderPlayer != null) {
            Set<UUID> senderFriends = friendCache.get(senderUuid);
            if (senderFriends != null) {
                senderFriends.add(playerUuid);
            }
        }

        return true;
    }

    public static boolean denyRequest(@NotNull Player player, @NotNull UUID senderUuid) {
        if (!systemEnabled) return false;

        UUID playerUuid = player.getUniqueId();

        if (!hasPendingRequest(senderUuid, playerUuid)) return false;

        // Fire event
        FriendRequestDenyEvent event = new FriendRequestDenyEvent(player, senderUuid);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;

        database.removeRequest(senderUuid, playerUuid);

        Map<UUID, Long> pending = pendingRequestCache.get(playerUuid);
        if (pending != null) {
            pending.remove(senderUuid);
        }

        return true;
    }


    public static boolean removeFriend(@NotNull Player player, @NotNull UUID friendUuid) {
        if (!systemEnabled) return false;

        UUID playerUuid = player.getUniqueId();

        if (!isFriend(playerUuid, friendUuid)) return false;

        // Fire event
        FriendRemoveEvent event = new FriendRemoveEvent(player, friendUuid);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;

        database.removeFriend(playerUuid, friendUuid);

        // Update caches
        Set<UUID> playerFriends = friendCache.get(playerUuid);
        if (playerFriends != null) {
            playerFriends.remove(friendUuid);
        }

        Player friendPlayer = Bukkit.getPlayer(friendUuid);
        if (friendPlayer != null) {
            Set<UUID> friendFriends = friendCache.get(friendUuid);
            if (friendFriends != null) {
                friendFriends.remove(playerUuid);
            }
        }

        return true;
    }

    public static boolean isFriend(@NotNull UUID playerUuid, @NotNull UUID friendUuid) {
        Set<UUID> friends = friendCache.get(playerUuid);
        if (friends != null) {
            return friends.contains(friendUuid);
        }
        if (database == null) return false;
        return database.areFriends(playerUuid, friendUuid);
    }

    public static boolean hasSharedAccess(@NotNull UUID ownerUuid, @NotNull UUID actorUuid) {
        if (!systemEnabled) return false;
        if (ownerUuid.equals(actorUuid)) return true;
        return isFriend(ownerUuid, actorUuid);
    }

    public static boolean isStorageAccessAllowed(@NotNull UUID ownerUuid, @NotNull String storageType) {
        if (!systemEnabled || database == null) return false;
        boolean defaultValue = File.getFriendStorageConfig().getBoolean("settings.default_access_allowed", false);
        String normalizedType = normalizeStorageType(storageType);
        Map<String, Boolean> cached = accessSettingCache.get(ownerUuid);
        if (cached != null && cached.containsKey(normalizedType)) {
            return cached.get(normalizedType);
        }
        boolean allowed = database.getAccessSetting(ownerUuid, normalizedType, defaultValue);
        cacheAccessSetting(ownerUuid, normalizedType, allowed);
        return allowed;
    }

    public static void setStorageAccess(@NotNull UUID ownerUuid, @NotNull String storageType, boolean allowed) {
        if (!systemEnabled || database == null) return;
        String normalizedType = normalizeStorageType(storageType);
        database.setAccessSetting(ownerUuid, normalizedType, allowed);
        cacheAccessSetting(ownerUuid, normalizedType, allowed);
    }

    public static Map<String, Boolean> getAllStorageAccess(@NotNull UUID ownerUuid) {
        boolean defaultValue = File.getFriendStorageConfig().getBoolean("settings.default_access_allowed", false);
        if (!systemEnabled || database == null) {
            return createDefaultAccessSettings(defaultValue);
        }
        Map<String, Boolean> cached = accessSettingCache.get(ownerUuid);
        if (cached != null) {
            Map<String, Boolean> settings = createDefaultAccessSettings(defaultValue);
            settings.putAll(cached);
            return settings;
        }
        Map<String, Boolean> settings = database.getAllAccessSettings(ownerUuid, defaultValue);
        accessSettingCache.put(ownerUuid, new ConcurrentHashMap<>(settings));
        return settings;
    }

    public static Set<UUID> getFriends(@NotNull UUID playerUuid) {
        Set<UUID> cached = friendCache.get(playerUuid);
        if (cached != null) {
            return Collections.unmodifiableSet(new LinkedHashSet<>(cached));
        }
        if (database == null) return Collections.emptySet();
        return database.getFriends(playerUuid);
    }

    public static int getFriendCount(@NotNull UUID playerUuid) {
        Set<UUID> cached = friendCache.get(playerUuid);
        if (cached != null) {
            return cached.size();
        }
        if (database == null) return 0;
        return database.getFriendCount(playerUuid);
    }

    public static Map<UUID, Long> getPendingRequests(@NotNull UUID receiverUuid) {
        Map<UUID, Long> cached = pendingRequestCache.get(receiverUuid);
        if (cached != null) {
            long now = System.currentTimeMillis();
            Map<UUID, Long> valid = new LinkedHashMap<>();
            cached.forEach((sender, expires) -> {
                if (expires == 0 || expires > now) {
                    valid.put(sender, expires);
                }
            });
            return valid;
        }
        if (database == null) return Collections.emptyMap();
        return database.getPendingRequests(receiverUuid);
    }

    public static boolean hasPendingRequest(@NotNull UUID senderUuid, @NotNull UUID receiverUuid) {
        Map<UUID, Long> pending = pendingRequestCache.get(receiverUuid);
        if (pending != null) {
            Long expiresAt = pending.get(senderUuid);
            if (expiresAt == null) return false;
            return expiresAt == 0 || expiresAt > System.currentTimeMillis();
        }
        if (database == null) return false;
        return database.hasRequest(senderUuid, receiverUuid);
    }

    public static int getMaxFriends(@NotNull Player player) {
        int configDefault = File.getFriendStorageConfig().getInt("settings.max_friends_default", 10);

        // Check permission-based max: storage.friends.max.<number>
        int permMax = configDefault;
        for (org.bukkit.permissions.PermissionAttachmentInfo perm : player.getEffectivePermissions()) {
            String name = perm.getPermission();
            if (!perm.getValue()) continue;
            if (!name.startsWith("storage.friends.max.")) continue;
            try {
                int val = Integer.parseInt(name.substring("storage.friends.max.".length()));
                if (val > permMax) {
                    permMax = val;
                }
            } catch (NumberFormatException ignored) {
            }
        }

        return permMax;
    }

    public static void notifyPendingRequests(@NotNull Player player) {
        if (!systemEnabled) return;
        if (!File.getFriendStorageConfig().getBoolean("settings.notify_on_join", true)) return;

        Map<UUID, Long> pending = getPendingRequests(player.getUniqueId());
        if (pending.isEmpty()) return;

        String message = File.getMessage().getString("friends.pending_on_join");
        if (message != null) {
            message = message.replace("#count#", String.valueOf(pending.size()));
            player.sendMessage(ChatUtils.colorize(message));
        }
    }

    public static boolean isDepositAllowed(@NotNull UUID ownerUuid, @NotNull String storageType) {
        if (!systemEnabled || database == null) return false;
        boolean defaultValue = File.getFriendStorageConfig().getBoolean("settings.default_access_allowed", false);
        return getActionSetting(ownerUuid, storageType, "deposit", defaultValue);
    }

    public static boolean isWithdrawAllowed(@NotNull UUID ownerUuid, @NotNull String storageType) {
        if (!systemEnabled || database == null) return false;
        boolean defaultValue = File.getFriendStorageConfig().getBoolean("settings.default_access_allowed", false);
        return getActionSetting(ownerUuid, storageType, "withdraw", defaultValue);
    }

    public static void setDepositAllowed(@NotNull UUID ownerUuid, @NotNull String storageType, boolean allowed) {
        if (!systemEnabled || database == null) return;
        setActionSetting(ownerUuid, storageType, "deposit", allowed);
    }

    public static void setWithdrawAllowed(@NotNull UUID ownerUuid, @NotNull String storageType, boolean allowed) {
        if (!systemEnabled || database == null) return;
        setActionSetting(ownerUuid, storageType, "withdraw", allowed);
    }

    public static void notifyOwnerAction(UUID ownerUuid, UUID actorUuid, String action, String storageType, String material, int amount) {
        if ("deposit".equalsIgnoreCase(action)) {
            notifyOwnerDeposit(ownerUuid, actorUuid, material, amount, storageType);
        } else if ("withdraw".equalsIgnoreCase(action)) {
            notifyOwnerWithdraw(ownerUuid, actorUuid, material, amount, storageType);
        }
    }

    public static void shutdown() {
        if (autoCleanTask != null) {
            autoCleanTask.cancel();
            autoCleanTask = null;
        }
        friendCache.clear();
        pendingRequestCache.clear();
        accessSettingCache.clear();
        actionSettingCache.clear();
        loadingPlayerData.clear();
        if (database != null) {
            database.cleanExpiredRequests();
        }
    }

    private static void scheduleAutoCleanTask() {
        if (!systemEnabled || database == null) {
            return;
        }

        boolean enabled = File.getFriendStorageConfig().getBoolean("settings.auto_clean.enabled", true);
        if (!enabled) {
            return;
        }

        int intervalSeconds = File.getFriendStorageConfig().getInt("settings.auto_clean.interval_seconds", 600);
        if (intervalSeconds <= 0) {
            return;
        }

        long periodTicks = intervalSeconds * 20L;
        autoCleanTask = TaskWrapper.runTaskTimerSafe(Storage.getStorage(), () -> {
            if (!systemEnabled || database == null) {
                if (autoCleanTask != null) {
                    autoCleanTask.cancel();
                    autoCleanTask = null;
                }
                return;
            }
            FriendDatabase currentDatabase = database;
            SchedulerUtil.runTaskAsynchronously(Storage.getStorage(), currentDatabase::cleanExpiredRequests);
            cleanExpiredRequestCache();
        }, periodTicks, periodTicks);
    }

    public static void logAction(UUID ownerUuid, UUID actorUuid, String action, String details) {
        FriendDatabase currentDatabase = database;
        if (currentDatabase == null) return;

        boolean logToConsole = File.getFriendStorageConfig().getBoolean("settings.log_actions", true);
        boolean logToDatabase = File.getFriendStorageConfig().getBoolean("settings.log_to_database", true);
        if (!logToConsole && !logToDatabase) return;

        String ownerName = Bukkit.getOfflinePlayer(ownerUuid).getName();
        String actorName = Bukkit.getOfflinePlayer(actorUuid).getName();
        if (ownerName == null) ownerName = ownerUuid.toString();
        if (actorName == null) actorName = actorUuid.toString();

        if (logToConsole) {
            Storage.getStorage().getLogger().info(String.format(
                    "[FriendStorage] %s - Owner: %s, Actor: %s, Details: %s",
                    action, ownerName, actorName, details
            ));
        }

        if (logToDatabase) {
            SchedulerUtil.runTaskAsynchronously(Storage.getStorage(),
                    () -> currentDatabase.insertLog(ownerUuid, actorUuid, action, details));
        }
    }

    public static void notifyOwnerDeposit(UUID ownerUuid, UUID actorUuid, String material, int amount, String storageType) {
        if (!File.getFriendStorageConfig().getBoolean("settings.notify_on_deposit", true)) {
            return;
        }

        Player owner = Bukkit.getPlayer(ownerUuid);
        if (owner == null || !owner.isOnline()) {
            return;
        }

        Player actor = Bukkit.getPlayer(actorUuid);
        String actorName = actor != null ? actor.getName() : "Unknown";

        String message = File.getMessage().getString("friends.notification.deposit",
                        "#prefix# &e#player# &ađã deposit &e#amount# #material# &avào &e#type# &acủa bạn.")
                .replace("#player#", actorName)
                .replace("#amount#", String.valueOf(amount))
                .replace("#material#", material)
                .replace("#type#", storageType);
        owner.sendMessage(ChatUtils.colorize(message));
    }

    public static void notifyOwnerWithdraw(UUID ownerUuid, UUID actorUuid, String material, int amount, String storageType) {
        if (!File.getFriendStorageConfig().getBoolean("settings.notify_on_withdraw", true)) {
            return;
        }

        Player owner = Bukkit.getPlayer(ownerUuid);
        if (owner == null || !owner.isOnline()) {
            return;
        }

        Player actor = Bukkit.getPlayer(actorUuid);
        String actorName = actor != null ? actor.getName() : "Unknown";

        String message = File.getMessage().getString("friends.notification.withdraw",
                        "#prefix# &e#player# &ađã withdraw &e#amount# #material# &atừ &e#type# &acủa bạn.")
                .replace("#player#", actorName)
                .replace("#amount#", String.valueOf(amount))
                .replace("#material#", material)
                .replace("#type#", storageType);
        owner.sendMessage(ChatUtils.colorize(message));
    }

    private static void applyMutualAccessSettings(UUID playerUuid, UUID senderUuid) {
        if (!File.getFriendStorageConfig().getBoolean("settings.mutual_access_on_accept", true)) {
            return;
        }
        for (String storageType : STORAGE_TYPES) {
            setStorageAccess(playerUuid, storageType, true);
            setStorageAccess(senderUuid, storageType, true);
            setDepositAllowed(playerUuid, storageType, true);
            setDepositAllowed(senderUuid, storageType, true);
            setWithdrawAllowed(playerUuid, storageType, true);
            setWithdrawAllowed(senderUuid, storageType, true);
        }
    }

    private static void cleanExpiredRequestCache() {
        long now = System.currentTimeMillis();
        for (Map<UUID, Long> requests : pendingRequestCache.values()) {
            requests.entrySet().removeIf(entry -> entry.getValue() != 0 && entry.getValue() <= now);
        }
    }

    public static String[] getStorageTypes() {
        return STORAGE_TYPES.clone();
    }

    private static void cachePlayerSettings(UUID uuid) {
        cachePlayerSettings(uuid, database);
    }

    private static void cachePlayerSettings(UUID uuid, FriendDatabase currentDatabase) {
        if (currentDatabase == null) return;
        boolean defaultValue = File.getFriendStorageConfig().getBoolean("settings.default_access_allowed", false);
        accessSettingCache.put(uuid, new ConcurrentHashMap<>(currentDatabase.getAllAccessSettings(uuid, defaultValue)));
        actionSettingCache.put(uuid, new ConcurrentHashMap<>(flattenActionSettings(
                currentDatabase.getAllActionSettings(uuid, defaultValue, defaultValue))));
    }

    private static Map<String, Boolean> flattenActionSettings(Map<String, Map<String, Boolean>> settings) {
        Map<String, Boolean> flattened = new ConcurrentHashMap<>();
        for (Map.Entry<String, Map<String, Boolean>> entry : settings.entrySet()) {
            String storageType = normalizeStorageType(entry.getKey());
            for (Map.Entry<String, Boolean> actionEntry : entry.getValue().entrySet()) {
                flattened.put(actionKey(storageType, actionEntry.getKey()), actionEntry.getValue());
            }
        }
        return flattened;
    }

    private static boolean getActionSetting(UUID ownerUuid, String storageType, String action, boolean defaultValue) {
        String key = actionKey(storageType, action);
        Map<String, Boolean> cached = actionSettingCache.get(ownerUuid);
        if (cached != null && cached.containsKey(key)) {
            return cached.get(key);
        }
        boolean allowed = database.getActionSetting(ownerUuid, normalizeStorageType(storageType), action.toLowerCase(), defaultValue);
        cacheActionSetting(ownerUuid, key, allowed);
        return allowed;
    }

    private static void setActionSetting(UUID ownerUuid, String storageType, String action, boolean allowed) {
        String normalizedType = normalizeStorageType(storageType);
        String normalizedAction = action.toLowerCase();
        database.setActionSetting(ownerUuid, normalizedType, normalizedAction, allowed);
        cacheActionSetting(ownerUuid, actionKey(normalizedType, normalizedAction), allowed);
    }

    private static void cacheAccessSetting(UUID ownerUuid, String storageType, boolean allowed) {
        accessSettingCache.computeIfAbsent(ownerUuid, uuid -> new ConcurrentHashMap<>()).put(storageType, allowed);
    }

    private static void cacheActionSetting(UUID ownerUuid, String key, boolean allowed) {
        actionSettingCache.computeIfAbsent(ownerUuid, uuid -> new ConcurrentHashMap<>()).put(key, allowed);
    }

    private static Map<String, Boolean> createDefaultAccessSettings(boolean defaultValue) {
        Map<String, Boolean> defaults = new HashMap<>();
        for (String storageType : STORAGE_TYPES) {
            defaults.put(storageType, defaultValue);
        }
        return defaults;
    }

    private static String actionKey(String storageType, String action) {
        return normalizeStorageType(storageType) + ":" + action.toLowerCase();
    }

    private static String normalizeStorageType(String storageType) {
        return storageType.toLowerCase();
    }

}
