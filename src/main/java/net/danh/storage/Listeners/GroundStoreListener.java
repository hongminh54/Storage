package net.danh.storage.Listeners;

import com.cryptomorin.xseries.messages.ActionBar;
import com.cryptomorin.xseries.messages.Titles;
import net.danh.storage.Manager.MineManager;
import net.danh.storage.Manager.Mythic.MythicStorageManager;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

public class GroundStoreListener implements Listener {

    private static final String NOTIFY_TYPE_STORAGE = "storage";
    private static final String NOTIFY_TYPE_MYTHIC = "mythic";

    private static final String STORAGE_TITLE_PATH = "mine.title";
    private static final String STORAGE_ACTIONBAR_PATH = "mine.actionbar";

    private static final String MYTHIC_TITLE_PATH = "notification.title";
    private static final String MYTHIC_ACTIONBAR_PATH = "notification.actionbar";

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
                    sendGroundStoreMessage(player, NOTIFY_TYPE_MYTHIC,
                            displayName, amount, currentStorage, maxStorage);
                    return true;
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

            if (!isVanillaStorageCandidate(itemStack)) {
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
                    sendGroundStoreMessage(player, NOTIFY_TYPE_STORAGE, itemName,
                            amount, newStoredAmount, maxStorage);
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isVanillaStorageCandidate(@NotNull ItemStack itemStack) {
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

        if (meta.getItemFlags() != null && !meta.getItemFlags().isEmpty()) {
            return false;
        }

        if (invokeBoolean(meta, "hasCustomModelData")) {
            return false;
        }

        if (invokeBoolean(meta, "isUnbreakable")) {
            return false;
        }

        if (invokeBoolean(meta, "hasAttributeModifiers")) {
            return false;
        }

        return true;
    }

    private boolean invokeBoolean(@NotNull Object target,
                                 @NotNull String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            Object result = method.invoke(target);
            return result instanceof Boolean && (Boolean) result;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private boolean isAnyGroundStoreEnabled(@NotNull Player player) {
        if (MineManager.isGroundStoreEnabled(player)) {
            return true;
        }
        return MythicStorageManager.isGroundStoreEnabled(player);
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
