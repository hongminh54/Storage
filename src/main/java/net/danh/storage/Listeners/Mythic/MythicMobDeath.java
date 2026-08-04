package net.danh.storage.Listeners.Mythic;

import com.cryptomorin.xseries.messages.ActionBar;
import com.cryptomorin.xseries.messages.Titles;
import net.danh.storage.Manager.Mythic.MythicStorageManager;
import net.danh.storage.MythicMobs.MythicMobsHelper;
import net.danh.storage.Storage;
import net.danh.storage.Utils.ChatUtils;
import net.danh.storage.Utils.File;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.EventExecutor;

import java.lang.reflect.Method;
import java.util.*;

public class MythicMobDeath implements Listener {

    private static final Map<UUID, Long> lastStorageFullNotification = new HashMap<>();

    private static final Map<Class<?>, MythicDeathEventAccessors> eventAccessorsCache =
            new HashMap<>();

    private static boolean canShowStorageFullNotification(Player player, int cooldownSeconds) {
        if (player == null) {
            return false;
        }
        long currentTime = System.currentTimeMillis();
        long cooldownMs = cooldownSeconds * 1000L;

        UUID playerId = player.getUniqueId();

        Long lastTime = lastStorageFullNotification.get(playerId);
        if (lastTime == null || (currentTime - lastTime) >= cooldownMs) {
            lastStorageFullNotification.put(playerId, currentTime);
            return true;
        }

        return false;
    }

    public static void cleanupPlayer(Player player) {
        if (player == null) {
            return;
        }
        lastStorageFullNotification.remove(player.getUniqueId());
    }

    public static void registerListener(org.bukkit.plugin.Plugin plugin) {
        if (!MythicStorageManager.isSystemEnabled()) return;

        MythicMobsHelper helper = MythicStorageManager.getMythicMobsHelper();
        if (helper == null || !helper.isInitialized()) return;

        String packageName = helper.getPackageName();

        String[] deathEventPaths = {
                packageName + ".events.MythicMobDeathEvent",
                "io.lumine.mythic.api.bukkit.events.MythicMobDeathEvent",
                "io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMobDeathEvent",
                "io.lumine.mythic.bukkit.events.MythicMobDeathEvent"
        };

        Class<?> deathEventClass = null;
        for (String eventPath : deathEventPaths) {
            try {
                deathEventClass = Class.forName(eventPath);
                plugin.getLogger().info("[MythicStorage] Found death event class: " + eventPath);
                break;
            } catch (ClassNotFoundException e) {
            }
        }

        if (deathEventClass == null) {
            plugin.getLogger().warning("[MythicStorage] Failed to find MythicMobDeathEvent class");
            return;
        }

        try {
            MythicMobDeath listener = new MythicMobDeath();
            final Class<?> finalDeathEventClass = deathEventClass;

            EventExecutor deathExecutor = (listenerInstance, event) -> {
                if (finalDeathEventClass.isInstance(event)) {
                    listener.handleMythicMobDeath(event);
                }
            };

            @SuppressWarnings("unchecked")
            Class<? extends org.bukkit.event.Event> deathEventType = (Class<? extends org.bukkit.event.Event>) deathEventClass;

            plugin.getServer().getPluginManager().registerEvent(
                    deathEventType,
                    listener,
                    EventPriority.HIGHEST,
                    deathExecutor,
                    plugin,
                    true
            );

            plugin.getLogger().info("[MythicStorage] MythicMobs death listener registered successfully");
        } catch (Exception e) {
            plugin.getLogger().warning("[MythicStorage] Failed to register MythicMobs death listener: " + e.getMessage());
        }
    }

    public void handleMythicMobDeath(org.bukkit.event.Event event) {
        if (!MythicStorageManager.isSystemEnabled()) return;

        String eventClassName = event.getClass().getName();
        if (!eventClassName.contains("MythicMobDeathEvent")) {
            return;
        }

        try {
            Class<?> eventClass = event.getClass();
            MythicDeathEventAccessors accessors = eventAccessorsCache.get(eventClass);
            if (accessors == null) {
                Method getEntity = eventClass.getMethod("getEntity");
                Method getKiller = eventClass.getMethod("getKiller");
                Method getDrops = eventClass.getMethod("getDrops");
                accessors = new MythicDeathEventAccessors(getEntity, getKiller,
                        getDrops);
                eventAccessorsCache.put(eventClass, accessors);
            }

            LivingEntity entity = (LivingEntity) accessors.getEntity.invoke(event);
            Player killer = (Player) accessors.getKiller.invoke(event);

            if (killer == null || !killer.isOnline()) return;

            MythicMobsHelper helper = MythicStorageManager.getMythicMobsHelper();
            if (helper == null || !helper.isInitialized()) return;

            @SuppressWarnings("unchecked")
            Collection<ItemStack> entityDrops =
                    (Collection<ItemStack>) accessors.getDrops.invoke(event);
            if (entityDrops == null) return;

            // Auto-pickup logic - only runs if toggle is enabled
            if (!MythicStorageManager.getToggleStatus(killer)) return;

            boolean actionBarEnabled = File.getMythicStorageConfig().getBoolean(
                    "notification.actionbar.enable", true);
            boolean titleEnabled = File.getMythicStorageConfig().getBoolean(
                    "notification.title.enable", false);
            boolean storageFullEnabled = File.getMythicStorageConfig().getBoolean(
                    "storage_full_notification.enabled", true);
            int storageFullCooldownSeconds = Math.max(1,
                    File.getMythicStorageConfig().getInt(
                            "storage_full_notification.cooldown_seconds", 10));

            String actionBarTemplate = File.getMythicStorageConfig()
                    .getString("notification.actionbar.item_added",
                            "&a+ #amount# #item# &7| &a#storage#&7/&a#max#");
            String titleTemplate = File.getMythicStorageConfig()
                    .getString("notification.title.item_added.title", "&a+ #amount# #item#");
            String subtitleTemplate = File.getMythicStorageConfig()
                    .getString("notification.title.item_added.subtitle", "&7Storage: &a#storage#&7/&a#max#");
            String storageFullActionBarTemplate = File
                    .getMythicStorageConfig().getString(
                            "notification.actionbar.storage_full",
                            "&cStorage Full! &7(&c#storage#&7/&c#max#&7)"
                    );
            String storageFullTitleTemplate = File
                    .getMythicStorageConfig().getString(
                            "notification.title.storage_full.title", "&cStorage Full!"
                    );
            String storageFullSubtitleTemplate = File
                    .getMythicStorageConfig().getString(
                            "notification.title.storage_full.subtitle",
                            "&7(&c#storage#&7/&c#max#&7)"
                    );

            if (File.getMythicStorageConfig().contains("blacklist_world")) {
                if (File.getMythicStorageConfig().getStringList("blacklist_world")
                        .contains(killer.getWorld().getName())) {
                    return;
                }
            }

            if (entityDrops.isEmpty()) return;

            List<ItemStack> dropsToProcess = new ArrayList<>(entityDrops);

            for (ItemStack drop : dropsToProcess) {
                if (drop == null) continue;

                String mythicItemName = helper.getMythicItemInternalName(drop);
                if (mythicItemName == null) continue;

                if (!MythicStorageManager.isConfiguredDrop(mythicItemName)) continue;

                if (!MythicStorageManager.isAutoPickupEnabledForItem(killer,
                        mythicItemName)) {
                    continue;
                }

                int amount = drop.getAmount();
                if (MythicStorageManager.addItemAmount(killer, mythicItemName, amount)) {
                    entityDrops.remove(drop);

                    int currentStorage = MythicStorageManager.getPlayerItem(killer, mythicItemName);
                    int maxStorage = MythicStorageManager.getMaxStorage(killer);

                    // Get display name or fallback to ID
                    String displayName = MythicStorageManager.getItemDisplayNameOrId(mythicItemName, killer);

                    // Send ActionBar notification
                    if (actionBarEnabled && actionBarTemplate != null) {
                        String actionBarMessage = actionBarTemplate
                                .replace("#amount#", String.valueOf(amount))
                                .replace("#item#", displayName)
                                .replace("#storage#",
                                        String.valueOf(currentStorage))
                                .replace("#max#", String.valueOf(maxStorage));
                        ActionBar.sendActionBar(Storage.getStorage(), killer,
                                ChatUtils.colorizewp(actionBarMessage));
                    }

                    // Send Title notification
                    if (titleEnabled && titleTemplate != null && subtitleTemplate != null) {
                        String title = titleTemplate
                                .replace("#amount#", String.valueOf(amount))
                                .replace("#item#", displayName)
                                .replace("#storage#",
                                        String.valueOf(currentStorage))
                                .replace("#max#", String.valueOf(maxStorage));
                        String subtitle = subtitleTemplate
                                .replace("#amount#", String.valueOf(amount))
                                .replace("#item#", displayName)
                                .replace("#storage#",
                                        String.valueOf(currentStorage))
                                .replace("#max#", String.valueOf(maxStorage));
                        Titles.sendTitle(killer, ChatUtils.colorizewp(title),
                                ChatUtils.colorizewp(subtitle));
                    }
                } else {
                    // Storage is full
                    if (storageFullEnabled) {
                        if (canShowStorageFullNotification(killer,
                                storageFullCooldownSeconds)) {
                            int currentStorage = MythicStorageManager.getPlayerItem(killer, mythicItemName);
                            int maxStorage = MythicStorageManager.getMaxStorage(killer);

                            // Get display name for storage full notification
                            String displayNameFull = MythicStorageManager.getItemDisplayNameOrId(mythicItemName, killer);

                            // Send ActionBar for storage full
                            if (actionBarEnabled && storageFullActionBarTemplate != null) {
                                String actionBarMessage = storageFullActionBarTemplate
                                        .replace("#item#", displayNameFull)
                                        .replace("#storage#",
                                                String.valueOf(currentStorage))
                                        .replace("#max#",
                                                String.valueOf(maxStorage));
                                ActionBar.sendActionBar(Storage.getStorage(),
                                        killer, ChatUtils.colorizewp(
                                                actionBarMessage));
                            }

                            // Send Title for storage full
                            if (titleEnabled && storageFullTitleTemplate != null
                                    && storageFullSubtitleTemplate != null) {
                                String title = storageFullTitleTemplate
                                        .replace("#item#", displayNameFull)
                                        .replace("#storage#",
                                                String.valueOf(currentStorage))
                                        .replace("#max#",
                                                String.valueOf(maxStorage));
                                String subtitle = storageFullSubtitleTemplate
                                        .replace("#item#", displayNameFull)
                                        .replace("#storage#",
                                                String.valueOf(currentStorage))
                                        .replace("#max#",
                                                String.valueOf(maxStorage));
                                Titles.sendTitle(killer,
                                        ChatUtils.colorizewp(title),
                                        ChatUtils.colorizewp(subtitle));
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            Storage.getStorage().getLogger().warning("[MythicStorage] Error handling MythicMob death: " + e.getMessage());
        }
    }

    private record MythicDeathEventAccessors(Method getEntity, Method getKiller, Method getDrops) {
    }
}
