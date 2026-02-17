package net.danh.storage.Listeners;

import com.cryptomorin.xseries.messages.ActionBar;
import com.cryptomorin.xseries.messages.Titles;
import net.danh.storage.Manager.Crop.CropStorageManager;
import net.danh.storage.Manager.MineManager;
import net.danh.storage.Manager.Mythic.MythicStorageManager;
import net.danh.storage.Manager.SoundManager;
import net.danh.storage.Storage;
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
import java.util.logging.Level;

public class GroundStoreListener implements Listener {

    private static final String NOTIFY_TYPE_STORAGE = "storage";
    private static final String NOTIFY_TYPE_MYTHIC = "mythic";
    private static final String NOTIFY_TYPE_CROP = "crop";

    private static final String STORAGE_TITLE_PATH = "mine.title";
    private static final String STORAGE_ACTIONBAR_PATH = "mine.actionbar";

    private static final String MYTHIC_TITLE_PATH = "notification.title";
    private static final String MYTHIC_ACTIONBAR_PATH = "notification.actionbar";

    private static final String CROP_TITLE_PATH = "notification.title";
    private static final String CROP_ACTIONBAR_PATH = "notification.actionbar";

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
                        amount
                )) {
                    int currentStorage = MythicStorageManager.getPlayerItem(
                            player,
                            mythicItemName
                    );
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
                            "items." + storageDrop
                    );
                    String itemName = name != null ? name : storageDrop
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

        try {
            Method hasCustomModelData = meta.getClass().getMethod(
                    "hasCustomModelData"
            );
            Object result = hasCustomModelData.invoke(meta);
            if (result instanceof Boolean && (Boolean) result) {
                return false;
            }
        } catch (NoSuchMethodException e) {
            Storage.getStorage().getLogger().log(
                    Level.WARNING,
                    "Failed to check custom model data (method missing).",
                    e
            );
        } catch (ReflectiveOperationException e) {
            Storage.getStorage().getLogger().log(
                    Level.WARNING,
                    "Failed to check custom model data (reflection error).",
                    e
            );
        } catch (RuntimeException e) {
            Storage.getStorage().getLogger().log(
                    Level.WARNING,
                    "Failed to check custom model data (runtime error).",
                    e
            );
        }

        try {
            Method getPdc = meta.getClass().getMethod(
                    "getPersistentDataContainer"
            );
            Object container = getPdc.invoke(meta);
            if (container != null) {
                Method getKeys = container.getClass().getMethod("getKeys");
                Object keys = getKeys.invoke(container);
                if (keys instanceof Set && !((Set<?>) keys).isEmpty()) {
                    return false;
                }
            }
        } catch (NoSuchMethodException e) {
            Storage.getStorage().getLogger().log(
                    Level.WARNING,
                    "Failed to check persistent data container (method missing).",
                    e
            );
        } catch (ReflectiveOperationException e) {
            Storage.getStorage().getLogger().log(
                    Level.WARNING,
                    "Failed to check persistent data container (reflection error).",
                    e
            );
        } catch (RuntimeException e) {
            Storage.getStorage().getLogger().log(
                    Level.WARNING,
                    "Failed to check persistent data container (runtime error).",
                    e
            );
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
        return CropStorageManager.isGroundStoreEnabled(player);
    }

    private boolean isStorageWorldBlacklisted(@NotNull Player player) {
        return File.getConfig().contains("blacklist_world")
                && File.getConfig().getStringList("blacklist_world")
                .contains(player.getWorld().getName());
    }

    private boolean isMythicStorageWorldBlacklisted(@NotNull Player player) {
        return File.getMythicStorageConfig().contains("blacklist_world")
                && File.getMythicStorageConfig().getStringList("blacklist_world")
                .contains(player.getWorld().getName());
    }

    private boolean isCropStorageWorldBlacklisted(@NotNull Player player) {
        return File.getCropStorageConfig().contains("blacklist_world")
                && File.getCropStorageConfig().getStringList("blacklist_world")
                .contains(player.getWorld().getName());
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
            boolean actionBarEnabled = File.getMythicStorageConfig().getBoolean(
                    MYTHIC_ACTIONBAR_PATH + ".enable",
                    true
            ) && File.getMythicStorageConfig().getBoolean(
                    "ground_store.notification.actionbar.enable",
                    true
            );
            boolean titleEnabled = File.getMythicStorageConfig().getBoolean(
                    MYTHIC_TITLE_PATH + ".enable",
                    false
            ) && File.getMythicStorageConfig().getBoolean(
                    "ground_store.notification.title.enable",
                    true
            );

            if (actionBarEnabled) {
                String template = File.getMythicStorageConfig().getString(
                        MYTHIC_ACTIONBAR_PATH + ".item_added",
                        ""
                );
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
                        ""
                );
                String subtitleTemplate = File.getMythicStorageConfig().getString(
                        MYTHIC_TITLE_PATH + ".item_added.subtitle",
                        ""
                );
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
            boolean actionBarEnabled = File.getCropStorageConfig().getBoolean(
                    CROP_ACTIONBAR_PATH + ".enable",
                    true
            ) && File.getCropStorageConfig().getBoolean(
                    "ground_store.notification.actionbar.enable",
                    true
            );
            boolean titleEnabled = File.getCropStorageConfig().getBoolean(
                    CROP_TITLE_PATH + ".enable",
                    false
            ) && File.getCropStorageConfig().getBoolean(
                    "ground_store.notification.title.enable",
                    true
            );

            if (actionBarEnabled) {
                String template = File.getCropStorageConfig().getString(
                        CROP_ACTIONBAR_PATH + ".item_added",
                        ""
                );
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
                        ""
                );
                String subtitleTemplate = File.getCropStorageConfig().getString(
                        CROP_TITLE_PATH + ".item_added.subtitle",
                        ""
                );
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

        boolean actionBarEnabled = File.getConfig().getBoolean(
                STORAGE_ACTIONBAR_PATH + ".enable",
                true
        ) && File.getConfig().getBoolean(
                "ground_store.notification.actionbar.enable",
                true
        );
        boolean titleEnabled = File.getConfig().getBoolean(
                STORAGE_TITLE_PATH + ".enable",
                true
        ) && File.getConfig().getBoolean(
                "ground_store.notification.title.enable",
                true
        );

        if (actionBarEnabled) {
            String template = File.getConfig().getString(
                    STORAGE_ACTIONBAR_PATH + ".action",
                    ""
            );
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
                    ""
            );
            String subtitleTemplate = File.getConfig().getString(
                    STORAGE_TITLE_PATH + ".subtitle",
                    ""
            );
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
