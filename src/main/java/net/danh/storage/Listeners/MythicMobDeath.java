package net.danh.storage.Listeners;

import com.cryptomorin.xseries.messages.ActionBar;
import net.danh.storage.Manager.MythicStorageManager;
import net.danh.storage.MythicMobs.MythicMobsHelper;
import net.danh.storage.Utils.Chat;
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

    private static final Map<Player, Long> lastStorageFullNotification = new HashMap<>();

    private static boolean canShowStorageFullNotification(Player player, int cooldownSeconds) {
        long currentTime = System.currentTimeMillis();
        long cooldownMs = cooldownSeconds * 1000L;

        Long lastTime = lastStorageFullNotification.get(player);
        if (lastTime == null || (currentTime - lastTime) >= cooldownMs) {
            lastStorageFullNotification.put(player, currentTime);
            return true;
        }

        return false;
    }

    public static void cleanupPlayer(Player player) {
        lastStorageFullNotification.remove(player);
    }

    public static void registerListener(org.bukkit.plugin.Plugin plugin) {
        if (!MythicStorageManager.isSystemEnabled()) return;

        MythicMobsHelper helper = MythicStorageManager.getMythicMobsHelper();
        if (helper == null || !helper.isInitialized()) return;

        String packageName = helper.getPackageName();

        String[] possibleEventPaths = {packageName + ".events.MythicMobDeathEvent", "io.lumine.mythic.api.bukkit.events.MythicMobDeathEvent", "io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMobDeathEvent", "io.lumine.mythic.bukkit.events.MythicMobDeathEvent"};

        Class<?> eventClass = null;
        for (String eventPath : possibleEventPaths) {
            try {
                plugin.getLogger().fine("[MythicStorage] Trying event class: " + eventPath);
                eventClass = Class.forName(eventPath);
                plugin.getLogger().info("[MythicStorage] ✓ Found event class: " + eventPath);
                break;
            } catch (ClassNotFoundException e) {
                plugin.getLogger().fine("[MythicStorage] × Event class not found: " + eventPath);
            }
        }

        if (eventClass == null) {
            plugin.getLogger().warning("[MythicStorage] Failed to find MythicMobDeathEvent class");
            return;
        }

        try {
            MythicMobDeath listener = new MythicMobDeath();
            final Class<?> finalEventClass = eventClass;

            EventExecutor executor = (listenerInstance, event) -> {
                if (finalEventClass.isInstance(event)) {
                    listener.handleMythicMobDeath(event);
                }
            };

            @SuppressWarnings("unchecked") Class<? extends org.bukkit.event.Event> eventType = (Class<? extends org.bukkit.event.Event>) eventClass;

            plugin.getServer().getPluginManager().registerEvent(eventType, listener, EventPriority.HIGHEST, executor, plugin, true // ignoreCancelled
            );

            plugin.getLogger().info("[MythicStorage] ✓ MythicMobs death listener registered successfully");
        } catch (Exception e) {
            plugin.getLogger().warning("[MythicStorage] Failed to register MythicMobs death listener: " + e.getMessage());
            e.printStackTrace();
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
            Method getEntity = eventClass.getMethod("getEntity");
            Method getKiller = eventClass.getMethod("getKiller");
            Method getDrops = eventClass.getMethod("getDrops");

            LivingEntity entity = (LivingEntity) getEntity.invoke(event);
            Player killer = (Player) getKiller.invoke(event);

            if (killer == null || !killer.isOnline()) return;
            if (!MythicStorageManager.getToggleStatus(killer)) return;

            MythicMobsHelper helper = MythicStorageManager.getMythicMobsHelper();
            if (helper == null || !helper.isInitialized()) return;

            @SuppressWarnings("unchecked") Collection<ItemStack> entityDrops = (Collection<ItemStack>) getDrops.invoke(event);
            if (entityDrops == null || entityDrops.isEmpty()) return;

            List<ItemStack> dropsToProcess = new ArrayList<>(entityDrops);

            for (ItemStack drop : dropsToProcess) {
                if (drop == null) continue;

                String mythicItemName = helper.getMythicItemInternalName(drop);
                if (mythicItemName == null) continue;

                if (!MythicStorageManager.isConfiguredDrop(mythicItemName)) continue;

                int amount = drop.getAmount();
                if (MythicStorageManager.addItemAmount(killer, mythicItemName, amount)) {
                    entityDrops.remove(drop);

                    String message = File.getMessage().getString("mythicstorage.item_added", "").replace("#amount#", String.valueOf(amount)).replace("#item#", mythicItemName);

                    if (!message.isEmpty()) {
                        ActionBar.sendActionBar(killer, Chat.colorize(message));
                    }
                } else {
                    if (File.getMythicStorageConfig().getBoolean("storage_full_notification.enabled", true)) {
                        int cooldownSeconds = File.getMythicStorageConfig().getInt("storage_full_notification.cooldown_seconds", 10);
                        if (canShowStorageFullNotification(killer, cooldownSeconds)) {
                            String fullMessage = File.getMessage().getString("mythicstorage.storage_full", "");
                            if (!fullMessage.isEmpty()) {
                                ActionBar.sendActionBar(killer, Chat.colorize(fullMessage));
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            // Ignore
        }
    }
}
