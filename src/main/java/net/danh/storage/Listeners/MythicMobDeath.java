package net.danh.storage.Listeners;

import com.cryptomorin.xseries.messages.ActionBar;
import net.danh.storage.Manager.MythicStorageManager;
import net.danh.storage.MythicMobs.MythicMobsHelper;
import net.danh.storage.Utils.Chat;
import net.danh.storage.Utils.File;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MythicMobDeath implements Listener {

    private static final Map<Player, Long> lastStorageFullNotification = new HashMap<>();

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMythicMobDeath(org.bukkit.event.Event event) {
        if (!MythicStorageManager.isSystemEnabled()) return;
        
        // Check if this is a MythicMobDeathEvent
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

            @SuppressWarnings("unchecked")
            Collection<ItemStack> drops = new ArrayList<>((Collection<ItemStack>) getDrops.invoke(event));
            if (drops.isEmpty()) return;

            MythicMobsHelper helper = MythicStorageManager.getMythicMobsHelper();
            if (helper == null || !helper.isInitialized()) return;

            Iterator<ItemStack> iterator = drops.iterator();
            boolean addedAny = false;

            while (iterator.hasNext()) {
                ItemStack drop = iterator.next();
                if (drop == null) continue;

                String mythicItemName = helper.getMythicItemInternalName(drop);
                if (mythicItemName == null) continue;

                if (!MythicStorageManager.isConfiguredDrop(mythicItemName)) continue;

                int amount = drop.getAmount();
                if (MythicStorageManager.addItemAmount(killer, mythicItemName, amount)) {
                    iterator.remove();
                    addedAny = true;

                    String message = File.getMessage().getString("mythicstorage.item_added", "")
                        .replace("#amount#", String.valueOf(amount))
                        .replace("#item#", mythicItemName);
                    
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

            if (addedAny) {
                Method setDrops = eventClass.getMethod("setDrops", Collection.class);
                setDrops.invoke(event, drops);
            }

        } catch (Exception e) {
            // Silent fail for compatibility
        }
    }

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
        
        // Try multiple possible event class paths for different MM versions
        String[] possibleEventPaths = {
            packageName + ".events.MythicMobDeathEvent",
            "io.lumine.mythic.api.bukkit.events.MythicMobDeathEvent",
            "io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMobDeathEvent",
            "io.lumine.mythic.bukkit.events.MythicMobDeathEvent"
        };

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
            // Simply register the MythicMobDeath listener directly
            // The onMythicMobDeath method will handle any event type via reflection
            MythicMobDeath listener = new MythicMobDeath();
            plugin.getServer().getPluginManager().registerEvents(listener, plugin);

            plugin.getLogger().info("[MythicStorage] ✓ MythicMobs death listener registered successfully");
        } catch (Exception e) {
            plugin.getLogger().warning("[MythicStorage] Failed to register MythicMobs death listener: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
