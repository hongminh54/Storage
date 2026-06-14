package net.danh.storage.Listeners;

import com.cryptomorin.xseries.messages.ActionBar;
import com.cryptomorin.xseries.messages.Titles;
import net.danh.storage.Manager.Crop.CropStorageManager;
import net.danh.storage.Manager.MineManager;
import net.danh.storage.Manager.Mob.MobStorageManager;
import net.danh.storage.Manager.Mythic.MythicStorageManager;
import net.danh.storage.Manager.SoundManager;
import net.danh.storage.Storage;
import net.danh.storage.Utils.AutoPickupCache;
import net.danh.storage.Utils.ChatUtils;
import net.danh.storage.Utils.File;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public class GroundStoreListener implements Listener {

    private static final String NOTIFY_TYPE_STORAGE = "storage";
    private static final String NOTIFY_TYPE_MYTHIC = "mythic";
    private static final String NOTIFY_TYPE_CROP = "crop";
    private static final String NOTIFY_TYPE_MOB = "mob";

    private static final String STORAGE_TITLE_PATH = "mine.title";
    private static final String STORAGE_ACTIONBAR_PATH = "mine.actionbar";

    private static final String MYTHIC_TITLE_PATH = "notification.title";
    private static final String MYTHIC_ACTIONBAR_PATH = "notification.actionbar";

    private static final String CROP_TITLE_PATH = "notification.title";
    private static final String CROP_ACTIONBAR_PATH = "notification.actionbar";

    private static final String MOB_TITLE_PATH = "notification.title";
    private static final String MOB_ACTIONBAR_PATH = "notification.actionbar";

    private static final AtomicBoolean CMD_WARN_PRINTED = new AtomicBoolean(false);
    private static final AtomicBoolean PDC_WARN_PRINTED = new AtomicBoolean(false);
    private static volatile Method HAS_CUSTOM_MODEL_DATA;
    private static volatile Method GET_PDC;
    private static volatile boolean reflectionInitialized = false;

    private static void initReflectionCache() {
        if (reflectionInitialized) {
            return;
        }
        synchronized (GroundStoreListener.class) {
            if (reflectionInitialized) {
                return;
            }
            try {
                HAS_CUSTOM_MODEL_DATA = ItemMeta.class.getMethod("hasCustomModelData");
            } catch (NoSuchMethodException ignored) {
                // Not available before 1.14 – leave null
            }
            try {
                GET_PDC = ItemMeta.class.getMethod("getPersistentDataContainer");
            } catch (NoSuchMethodException ignored) {
                // Not available before 1.14 – leave null
            }
            reflectionInitialized = true;
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntityPickup(@NotNull EntityPickupItemEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }

        Player player = (Player) entity;
        if (!isAnyGroundStoreEnabled(player)) {
            return;
        }

        Item itemEntity = event.getItem();
        if (itemEntity == null) {
            return;
        }

        ItemStack itemStack = itemEntity.getItemStack();
        if (itemStack == null) {
            return;
        }

        if (handlePickup(player, itemStack)) {
            itemEntity.remove();
            event.setCancelled(true);
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerPickupLegacy(@NotNull PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        if (!isAnyGroundStoreEnabled(player)) {
            return;
        }

        Item itemEntity = event.getItem();
        if (itemEntity == null) {
            return;
        }

        ItemStack itemStack = itemEntity.getItemStack();
        if (itemStack == null) {
            return;
        }

        if (handlePickup(player, itemStack)) {
            itemEntity.remove();
            event.setCancelled(true);
        }
    }

    private boolean handlePickup(@NotNull Player player,
                                 @NotNull ItemStack itemStack) {
        int amount = itemStack.getAmount();
        if (amount <= 0) {
            return false;
        }

        String mythicItemName = null;
        if (MythicStorageManager.isSystemEnabled()) {
            if (MythicStorageManager.getMythicMobsHelper() != null) {
                mythicItemName = MythicStorageManager.getMythicMobsHelper()
                        .getMythicItemInternalName(itemStack);
            }
        }

        if (MythicStorageManager.isSystemEnabled()
                && !isMythicStorageWorldBlacklisted(player)
                && MythicStorageManager.isGroundStoreEnabled(player)) {
            if (mythicItemName != null
                    && MythicStorageManager.isConfiguredDrop(mythicItemName)
                    && MythicStorageManager.isGroundStoreItemAllowed(mythicItemName)) {
                if (MythicStorageManager.addItemAmount(
                        player,
                        mythicItemName,
                        amount)) {
                    int currentStorage = MythicStorageManager.getPlayerItem(
                            player,
                            mythicItemName);
                    int maxStorage = MythicStorageManager.getMaxStorage(player);
                    String displayName = MythicStorageManager
                            .getItemDisplayNameOrId(mythicItemName, player);
                    SoundManager.playActionSound(player, "ground_store",
                            File.getMythicStorageConfig());
                    sendGroundStoreMessage(player, NOTIFY_TYPE_MYTHIC,
                            displayName, amount, currentStorage, maxStorage);
                    return true;
                }
            }
        }

        if (CropStorageManager.isSystemEnabled()
                && !isCropStorageWorldBlacklisted(player)
                && CropStorageManager.isGroundStoreEnabled(player)) {
            if (mythicItemName == null
                    && isPlainVanillaItem(itemStack)) {
                String cropItemName = itemStack.getType().name();
                if (CropStorageManager.isConfiguredDrop(cropItemName)
                        && CropStorageManager.isGroundStoreItemAllowed(cropItemName)) {
                    if (CropStorageManager.addItemAmount(player, cropItemName, amount)) {
                        int currentStorage = CropStorageManager.getPlayerItem(player, cropItemName);
                        int maxStorage = CropStorageManager.getMaxStorage(player);
                        String displayName = CropStorageManager.getItemDisplayName(cropItemName);
                        SoundManager.playActionSound(player, "ground_store",
                                File.getCropStorageConfig());
                        sendGroundStoreMessage(player, NOTIFY_TYPE_CROP,
                                displayName, amount, currentStorage, maxStorage);
                        return true;
                    }
                }
            }
        }

        if (MobStorageManager.isSystemEnabled()
                && !isMobStorageWorldBlacklisted(player)
                && MobStorageManager.isGroundStoreEnabled(player)) {
            boolean ignoreMythicItems = File.getMobStorageConfig().getBoolean(
                    "settings.ignore_mythic_items",
                    true);
            if ((!ignoreMythicItems || mythicItemName == null)
                    && isMobPlainItemAllowed(itemStack)) {
                String mobItemName = itemStack.getType().name();
                if (MobStorageManager.isConfiguredDrop(mobItemName)
                        && MobStorageManager.isGroundStoreItemAllowed(mobItemName)) {
                    if (MobStorageManager.addItemAmount(player, mobItemName, amount)) {
                        int currentStorage = MobStorageManager.getPlayerItem(player, mobItemName);
                        int maxStorage = MobStorageManager.getMaxStorage(player);
                        String displayName = MobStorageManager.getItemDisplayName(mobItemName);
                        SoundManager.playActionSound(player, "ground_store",
                                File.getMobStorageConfig());
                        sendGroundStoreMessage(player, NOTIFY_TYPE_MOB,
                                displayName, amount, currentStorage, maxStorage);
                        return true;
                    }
                }
            }
        }

        if (MineManager.isGroundStoreEnabled(player)
                && !isStorageWorldBlacklisted(player)) {
            if (mythicItemName != null
                    && MythicStorageManager.isSystemEnabled()
                    && MythicStorageManager.isConfiguredDrop(mythicItemName)) {
                return false;
            }

            if (!isPlainVanillaItem(itemStack)) {
                return false;
            }

            String storageDrop = MineManager.getItemStackDrop(itemStack);
            if (storageDrop != null
                    && MineManager.isGroundStoreItemAllowed(storageDrop)) {
                if (MineManager.addBlockAmount(player, storageDrop, amount)) {
                    int newStoredAmount = MineManager.getPlayerBlock(player,
                            storageDrop);
                    int maxStorage = MineManager.getMaxBlock(player);
                    String name = File.getConfig().getString(
                            "items." + storageDrop);
                    String itemName = name != null ? name
                            : storageDrop
                              .replace("_", " ");
                    SoundManager.playActionSound(player, "ground_store",
                            File.getConfig());
                    sendGroundStoreMessage(player, NOTIFY_TYPE_STORAGE, itemName,
                            amount, newStoredAmount, maxStorage);
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isMobPlainItemAllowed(@NotNull ItemStack itemStack) {
        if (File.getMobStorageConfig().getBoolean(
                "settings.allow_custom_item_meta",
                false)) {
            return true;
        }
        return isPlainVanillaItem(itemStack);
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
                    Storage.getStorage().getLogger().log(Level.WARNING,
                            "Failed to check custom model data.", e);
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
                    Storage.getStorage().getLogger().log(Level.WARNING,
                            "Failed to check persistent data container.", e);
                }
            }
        }

        return true;
    }

    private boolean isAnyGroundStoreEnabled(@NotNull Player player) {
        if (MineManager.isGroundStoreEnabled(player)) {
            return true;
        }
        if (MythicStorageManager.isGroundStoreEnabled(player)) {
            return true;
        }
        if (MobStorageManager.isGroundStoreEnabled(player)) {
            return true;
        }
        return CropStorageManager.isGroundStoreEnabled(player);
    }

    private boolean isStorageWorldBlacklisted(@NotNull Player player) {
        return AutoPickupCache.isStorageWorldBlacklisted(player.getWorld().getName());
    }

    private boolean isMythicStorageWorldBlacklisted(@NotNull Player player) {
        return AutoPickupCache.isMythicWorldBlacklisted(player.getWorld().getName());
    }

    private boolean isCropStorageWorldBlacklisted(@NotNull Player player) {
        return AutoPickupCache.isCropWorldBlacklisted(player.getWorld().getName());
    }

    private boolean isMobStorageWorldBlacklisted(@NotNull Player player) {
        return AutoPickupCache.isMobWorldBlacklisted(player.getWorld().getName());
    }

    private void sendGroundStoreMessage(@NotNull Player player,
                                        @NotNull String type,
                                        @NotNull String itemName,
                                        int amount,
                                        int storage,
                                        int max) {
        String displayAmount = String.valueOf(amount);
        String storageValue = String.valueOf(storage);
        String maxValue = String.valueOf(max);

        if (NOTIFY_TYPE_MYTHIC.equalsIgnoreCase(type)) {
            boolean actionBarEnabled = AutoPickupCache.isMythicActionBarEnabled();
            boolean titleEnabled = AutoPickupCache.isMythicTitleEnabled();

            if (actionBarEnabled) {
                String template = File.getMythicStorageConfig().getString(
                        MYTHIC_ACTIONBAR_PATH + ".item_added",
                        "");
                if (template != null) {
                    String msg = template
                            .replace("#amount#", displayAmount)
                            .replace("#item#", itemName)
                            .replace("#storage#", storageValue)
                            .replace("#max#", maxValue);
                    ActionBar.sendActionBar(Storage.getStorage(), player,
                            ChatUtils.colorizewp(msg));
                }
            }

            if (titleEnabled) {
                String titleTemplate = File.getMythicStorageConfig().getString(
                        MYTHIC_TITLE_PATH + ".item_added.title",
                        "");
                String subtitleTemplate = File.getMythicStorageConfig().getString(
                        MYTHIC_TITLE_PATH + ".item_added.subtitle",
                        "");
                if (titleTemplate != null && subtitleTemplate != null) {
                    String title = titleTemplate
                            .replace("#amount#", displayAmount)
                            .replace("#item#", itemName)
                            .replace("#storage#", storageValue)
                            .replace("#max#", maxValue);
                    String subtitle = subtitleTemplate
                            .replace("#amount#", displayAmount)
                            .replace("#item#", itemName)
                            .replace("#storage#", storageValue)
                            .replace("#max#", maxValue);
                    Titles.sendTitle(player,
                            ChatUtils.colorizewp(title),
                            ChatUtils.colorizewp(subtitle));
                }
            }
            return;
        }

        if (NOTIFY_TYPE_CROP.equalsIgnoreCase(type)) {
            boolean actionBarEnabled = AutoPickupCache.isCropActionBarEnabled();
            boolean titleEnabled = AutoPickupCache.isCropTitleEnabled();

            if (actionBarEnabled) {
                String template = File.getCropStorageConfig().getString(
                        CROP_ACTIONBAR_PATH + ".item_added",
                        "");
                if (template != null) {
                    String msg = template
                            .replace("#amount#", displayAmount)
                            .replace("#item#", itemName)
                            .replace("#storage#", storageValue)
                            .replace("#max#", maxValue);
                    ActionBar.sendActionBar(Storage.getStorage(), player,
                            ChatUtils.colorizewp(msg));
                }
            }

            if (titleEnabled) {
                String titleTemplate = File.getCropStorageConfig().getString(
                        CROP_TITLE_PATH + ".item_added.title",
                        "");
                String subtitleTemplate = File.getCropStorageConfig().getString(
                        CROP_TITLE_PATH + ".item_added.subtitle",
                        "");
                if (titleTemplate != null && subtitleTemplate != null) {
                    String title = titleTemplate
                            .replace("#amount#", displayAmount)
                            .replace("#item#", itemName)
                            .replace("#storage#", storageValue)
                            .replace("#max#", maxValue);
                    String subtitle = subtitleTemplate
                            .replace("#amount#", displayAmount)
                            .replace("#item#", itemName)
                            .replace("#storage#", storageValue)
                            .replace("#max#", maxValue);
                    Titles.sendTitle(player,
                            ChatUtils.colorizewp(title),
                            ChatUtils.colorizewp(subtitle));
                }
            }
            return;
        }

        if (NOTIFY_TYPE_MOB.equalsIgnoreCase(type)) {
            boolean actionBarEnabled = AutoPickupCache.isMobActionBarEnabled();
            boolean titleEnabled = AutoPickupCache.isMobTitleEnabled();

            if (actionBarEnabled) {
                String template = File.getMobStorageConfig().getString(
                        MOB_ACTIONBAR_PATH + ".item_added",
                        "");
                if (template != null) {
                    String msg = template
                            .replace("#amount#", displayAmount)
                            .replace("#item#", itemName)
                            .replace("#storage#", storageValue)
                            .replace("#max#", maxValue);
                    ActionBar.sendActionBar(Storage.getStorage(), player,
                            ChatUtils.colorizewp(msg));
                }
            }

            if (titleEnabled) {
                String titleTemplate = File.getMobStorageConfig().getString(
                        MOB_TITLE_PATH + ".item_added.title",
                        "");
                String subtitleTemplate = File.getMobStorageConfig().getString(
                        MOB_TITLE_PATH + ".item_added.subtitle",
                        "");
                if (titleTemplate != null && subtitleTemplate != null) {
                    String title = titleTemplate
                            .replace("#amount#", displayAmount)
                            .replace("#item#", itemName)
                            .replace("#storage#", storageValue)
                            .replace("#max#", maxValue);
                    String subtitle = subtitleTemplate
                            .replace("#amount#", displayAmount)
                            .replace("#item#", itemName)
                            .replace("#storage#", storageValue)
                            .replace("#max#", maxValue);
                    Titles.sendTitle(player,
                            ChatUtils.colorizewp(title),
                            ChatUtils.colorizewp(subtitle));
                }
            }
            return;
        }

        boolean actionBarEnabled = AutoPickupCache.isStorageGroundStoreActionBarEnabled();
        boolean titleEnabled = AutoPickupCache.isStorageGroundStoreTitleEnabled();

        if (actionBarEnabled) {
            String template = File.getConfig().getString(
                    STORAGE_ACTIONBAR_PATH + ".action",
                    "");
            if (template != null) {
                String msg = template
                        .replace("#amount#", displayAmount)
                        .replace("#item#", itemName)
                        .replace("#storage#", storageValue)
                        .replace("#max#", maxValue);
                ActionBar.sendActionBar(Storage.getStorage(), player,
                        ChatUtils.colorizewp(msg));
            }
        }

        if (titleEnabled) {
            String titleTemplate = File.getConfig().getString(
                    STORAGE_TITLE_PATH + ".title",
                    "");
            String subtitleTemplate = File.getConfig().getString(
                    STORAGE_TITLE_PATH + ".subtitle",
                    "");
            if (titleTemplate != null && subtitleTemplate != null) {
                String title = titleTemplate
                        .replace("#amount#", displayAmount)
                        .replace("#item#", itemName)
                        .replace("#storage#", storageValue)
                        .replace("#max#", maxValue);
                String subtitle = subtitleTemplate
                        .replace("#amount#", displayAmount)
                        .replace("#item#", itemName)
                        .replace("#storage#", storageValue)
                        .replace("#max#", maxValue);
                Titles.sendTitle(player,
                        ChatUtils.colorizewp(title),
                        ChatUtils.colorizewp(subtitle));
            }
        }
    }
}
