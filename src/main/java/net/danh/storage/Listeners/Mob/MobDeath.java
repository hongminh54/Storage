package net.danh.storage.Listeners.Mob;

import com.cryptomorin.xseries.messages.ActionBar;
import com.cryptomorin.xseries.messages.Titles;
import net.danh.storage.Manager.Mob.MobStorageManager;
import net.danh.storage.Manager.Mythic.MythicStorageManager;
import net.danh.storage.MythicMobs.MythicMobsHelper;
import net.danh.storage.Storage;
import net.danh.storage.Utils.AutoPickupCache;
import net.danh.storage.Utils.ChatUtils;
import net.danh.storage.Utils.File;
import net.danh.storage.Utils.NotificationQueue;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public class MobDeath implements Listener {

    private static final Map<UUID, Long> lastStorageFullNotification = new HashMap<>();
    private static final AtomicBoolean CMD_WARN_PRINTED = new AtomicBoolean(false);
    private static final AtomicBoolean PDC_WARN_PRINTED = new AtomicBoolean(false);
    private static volatile Method HAS_CUSTOM_MODEL_DATA;
    private static volatile Method GET_PDC;
    private static volatile boolean reflectionInitialized = false;

    public static void cleanupPlayer(Player player) {
        if (player != null) {
            lastStorageFullNotification.remove(player.getUniqueId());
        }
    }

    private static void initReflectionCache() {
        if (reflectionInitialized) {
            return;
        }
        synchronized (MobDeath.class) {
            if (reflectionInitialized) {
                return;
            }
            try {
                HAS_CUSTOM_MODEL_DATA = ItemMeta.class.getMethod("hasCustomModelData");
            } catch (NoSuchMethodException ignored) {
            }
            try {
                GET_PDC = ItemMeta.class.getMethod("getPersistentDataContainer");
            } catch (NoSuchMethodException ignored) {
            }
            reflectionInitialized = true;
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onMobDeath(@NotNull EntityDeathEvent event) {
        if (!MobStorageManager.isSystemEnabled()) {
            return;
        }

        Player player = event.getEntity().getKiller();
        if (player == null || !player.isOnline()) {
            return;
        }

        if (!MobStorageManager.getToggleStatus(player)) {
            return;
        }

        if (AutoPickupCache.isMobWorldBlacklisted(player.getWorld().getName())) {
            return;
        }

        if (!MobStorageManager.isEntityTypeAllowed(event.getEntityType())) {
            return;
        }

        List<ItemStack> dropsToProcess = new ArrayList<>(event.getDrops());
        if (dropsToProcess.isEmpty()) {
            return;
        }

        boolean allowCustomMeta = File.getMobStorageConfig().getBoolean("settings.allow_custom_item_meta", false);
        boolean ignoreMythicItems = File.getMobStorageConfig().getBoolean("settings.ignore_mythic_items", true);
        boolean partialStoreWhenFull = File.getMobStorageConfig().getBoolean("settings.partial_store_when_full", false);

        for (ItemStack drop : dropsToProcess) {
            if (drop == null || drop.getType() == Material.AIR || drop.getAmount() <= 0) {
                continue;
            }

            if (!allowCustomMeta && !isPlainVanillaItem(drop)) {
                continue;
            }

            if (ignoreMythicItems && isMythicItem(drop)) {
                continue;
            }

            String itemName = drop.getType().name();
            if (!MobStorageManager.isConfiguredDropForMob(event.getEntityType(), itemName)) {
                continue;
            }

            if (!MobStorageManager.isAutoPickupEnabledForItem(player, itemName)) {
                continue;
            }

            int amount = drop.getAmount();
            if (partialStoreWhenFull) {
                int space = Math.max(0, MobStorageManager.getMaxStorage(player) - MobStorageManager.getPlayerItem(player, itemName));
                if (space <= 0) {
                    sendStorageFullNotification(player, itemName);
                    continue;
                }
                int toStore = Math.min(amount, space);
                if (MobStorageManager.addItemAmount(player, itemName, toStore)) {
                    if (toStore >= amount) {
                        event.getDrops().remove(drop);
                    } else {
                        drop.setAmount(amount - toStore);
                    }
                    sendNotification(player, itemName, toStore);
                } else {
                    sendStorageFullNotification(player, itemName);
                }
            } else if (MobStorageManager.addItemAmount(player, itemName, amount)) {
                event.getDrops().remove(drop);
                sendNotification(player, itemName, amount);
            } else {
                sendStorageFullNotification(player, itemName);
            }
        }
    }

    private boolean isMythicItem(@NotNull ItemStack itemStack) {
        if (!MythicStorageManager.isSystemEnabled()) {
            return false;
        }
        MythicMobsHelper helper = MythicStorageManager.getMythicMobsHelper();
        return helper != null && helper.getMythicItemInternalName(itemStack) != null;
    }

    private boolean isPlainVanillaItem(@NotNull ItemStack itemStack) {
        if (!itemStack.hasItemMeta()) {
            return true;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return true;
        }

        if (meta.hasDisplayName() || meta.hasLore() || meta.hasEnchants()) {
            return false;
        }

        Set<ItemFlag> flags = meta.getItemFlags();
        if (flags != null && !flags.isEmpty()) {
            return false;
        }

        initReflectionCache();

        if (HAS_CUSTOM_MODEL_DATA != null) {
            try {
                Object result = HAS_CUSTOM_MODEL_DATA.invoke(meta);
                if (result instanceof Boolean && (Boolean) result) {
                    return false;
                }
            } catch (ReflectiveOperationException | RuntimeException e) {
                if (CMD_WARN_PRINTED.compareAndSet(false, true)) {
                    Storage.getStorage().getLogger().log(Level.WARNING, "[MobStorage] Failed to check custom model data.", e);
                }
            }
        }

        if (GET_PDC != null) {
            try {
                Object container = GET_PDC.invoke(meta);
                if (container != null) {
                    Method getKeys = container.getClass().getMethod("getKeys");
                    Object keys = getKeys.invoke(container);
                    if (keys instanceof Set && !((Set<?>) keys).isEmpty()) {
                        return false;
                    }
                }
            } catch (ReflectiveOperationException | RuntimeException e) {
                if (PDC_WARN_PRINTED.compareAndSet(false, true)) {
                    Storage.getStorage().getLogger().log(Level.WARNING, "[MobStorage] Failed to check persistent data container.", e);
                }
            }
        }

        return true;
    }

    private void sendNotification(@NotNull Player player, @NotNull String itemName, int amount) {
        NotificationQueue.add(player, NotificationQueue.TYPE_MOB, itemName,
                MobStorageManager.getItemDisplayName(itemName),
                amount, "",
                MobStorageManager.getPlayerItem(player, itemName),
                MobStorageManager.getMaxStorage(player),
                AutoPickupCache.isMobActionBarEnabled(), AutoPickupCache.isMobTitleEnabled());
    }

    private void sendStorageFullNotification(@NotNull Player player, @NotNull String itemName) {
        if (!File.getMobStorageConfig().getBoolean("storage_full_notification.enabled", true)) {
            return;
        }
        int cooldownSeconds = Math.max(1, File.getMobStorageConfig().getInt("storage_full_notification.cooldown_seconds", 10));
        if (!canShowStorageFullNotification(player, cooldownSeconds)) {
            return;
        }

        String displayName = MobStorageManager.getItemDisplayName(itemName);
        int currentAmount = MobStorageManager.getPlayerItem(player, itemName);
        int maxStorage = MobStorageManager.getMaxStorage(player);

        if (AutoPickupCache.isMobActionBarEnabled()) {
            String template = File.getMobStorageConfig().getString("notification.actionbar.storage_full");
            sendActionBar(player, template, displayName, 0, currentAmount, maxStorage);
        }

        if (AutoPickupCache.isMobTitleEnabled()) {
            String titleTemplate = File.getMobStorageConfig().getString("notification.title.storage_full.title");
            String subtitleTemplate = File.getMobStorageConfig().getString("notification.title.storage_full.subtitle");
            sendTitle(player, titleTemplate, subtitleTemplate, displayName, 0, currentAmount, maxStorage);
        }
    }

    private boolean canShowStorageFullNotification(@NotNull Player player, int cooldownSeconds) {
        long currentTime = System.currentTimeMillis();
        long cooldownMs = cooldownSeconds * 1000L;
        UUID playerId = player.getUniqueId();
        Long lastTime = lastStorageFullNotification.get(playerId);
        if (lastTime == null || currentTime - lastTime >= cooldownMs) {
            lastStorageFullNotification.put(playerId, currentTime);
            return true;
        }
        return false;
    }

    private void sendActionBar(@NotNull Player player, String template, @NotNull String itemName, int amount, int storage, int max) {
        if (template == null || template.isEmpty()) {
            return;
        }
        ActionBar.sendActionBar(Storage.getStorage(), player, ChatUtils.colorizewp(applyPlaceholders(template, itemName, amount, storage, max)));
    }

    private void sendTitle(@NotNull Player player, String titleTemplate, String subtitleTemplate, @NotNull String itemName, int amount, int storage, int max) {
        if (titleTemplate == null || subtitleTemplate == null) {
            return;
        }
        Titles.sendTitle(player,
                ChatUtils.colorizewp(applyPlaceholders(titleTemplate, itemName, amount, storage, max)),
                ChatUtils.colorizewp(applyPlaceholders(subtitleTemplate, itemName, amount, storage, max)));
    }

    private String applyPlaceholders(@NotNull String template, @NotNull String itemName, int amount, int storage, int max) {
        return template
                .replace("#amount#", String.valueOf(amount))
                .replace("#item#", itemName)
                .replace("#storage#", String.valueOf(storage))
                .replace("#max#", String.valueOf(max));
    }
}
