package net.danh.storage.GUI.listeners;

import de.tr7zw.changeme.nbtapi.NBTItem;
import net.danh.storage.GUI.Crafting.RecipeEditorGUI;
import net.danh.storage.GUI.GUI;
import net.danh.storage.GUI.Mythic.ViewMythicStorageGUI;
import net.danh.storage.GUI.ViewStorageGUI;
import net.danh.storage.GUI.manager.IGUI;
import net.danh.storage.Manager.MineManager;
import net.danh.storage.Manager.Mythic.MythicStorageManager;
import net.danh.storage.Manager.SoundManager;
import net.danh.storage.Storage;
import net.danh.storage.Utils.File;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public class GUIClickListener implements Listener {
    private static final HashMap<UUID, Long> interactTimeout = new HashMap<>();
    private static boolean nbtWarningLogged = false;

    private static void logNBTWarning(Exception e) {
        if (!nbtWarningLogged) {
            Storage.getStorage().getLogger()
                    .warning("NBT-API error detected in GUI interactions. Interactive items may not work properly.");
            Storage.getStorage().getLogger().warning("Your Minecraft version may not be fully supported by NBT-API.");
            Storage.getStorage().getLogger().warning("Error: " + e.getMessage());
            Storage.getStorage().getLogger()
                    .warning("Please consider updating to a newer version of NBT-API or Minecraft.");
            Storage.getStorage().getLogger().warning(
                    "If you are using a custom NBT-API version, please ensure it is compatible with your Minecraft version.");
            Storage.getStorage().getLogger().warning(
                    "If an error occurs, please report it to me at https://github.com/hongminh54/Storage/issues.");
            nbtWarningLogged = true;
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();

        boolean isGUIInv = e.getClickedInventory() != null && e.getClickedInventory().getHolder() != null
                && e.getClickedInventory().getHolder() instanceof IGUI;

        // Check if top inventory is a GUI - prevent shift-click from player inventory
        Inventory topInventory = null;
        Object view = e.getView();
        if (view != null) {
            try {
                Method getTopInventory = view.getClass().getMethod("getTopInventory");
                Object result = getTopInventory.invoke(view);
                if (result instanceof Inventory) {
                    topInventory = (Inventory) result;
                }
            } catch (Exception ignored) {
                topInventory = null;
            }
        }
        boolean isTopInventoryGUI = topInventory != null
                && topInventory.getHolder() != null
                && topInventory.getHolder() instanceof IGUI;

        ItemStack currentItem = e.getCurrentItem();
        boolean isInteractiveItem = false;

        if (currentItem != null && currentItem.getType() != Material.AIR && currentItem.getAmount() > 0) {
            try {
                NBTItem nbtItem = new NBTItem(currentItem);
                isInteractiveItem = nbtItem.hasTag("storage:id");
            } catch (Exception ex) {
                isInteractiveItem = false;
                logNBTWarning(ex);
            }
        }

        boolean shouldCancel = isGUIInv || isInteractiveItem;

        if (!shouldCancel && isTopInventoryGUI && e.getClickedInventory() != null) {
            // Check if clicking from bottom inventory (player inv) with shift click
            boolean isClickingBottomInventory = e.getClickedInventory().equals(e.getWhoClicked().getInventory());
            boolean isShiftClick = e.isShiftClick();

            if (isClickingBottomInventory && isShiftClick) {
                shouldCancel = true;
            }
        }

        if (shouldCancel) {
            e.setCancelled(true);
            // updateInventory() is deprecated since 1.19 and not needed for 1.9+
            if (new net.danh.storage.NMS.NMSAssistant().isVersionLessThan(9)) {
                player.updateInventory();
            }

            if (currentItem != null && currentItem.getType() != Material.AIR && currentItem.getAmount() > 0) {
                try {
                    NBTItem nbtItem = new NBTItem(currentItem);
                    if (nbtItem.hasTag("storage:id")) {
                        UUID uuid = nbtItem.getUUID("storage:id");
                        if (GUI.getItemMapper().containsKey(uuid))
                            GUI.getItemMapper().get(uuid).handleClick(player, e.getClick());
                    }
                } catch (Exception ex) {
                    logNBTWarning(ex);
                }
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        ItemStack item = e.getItem();
        if (item == null || item.getType() == Material.AIR || item.getAmount() <= 0)
            return;

        try {
            NBTItem nbtItem = new NBTItem(item);
            if (!nbtItem.hasTag("storage:id"))
                return;

            UUID uuid = nbtItem.getUUID("storage:id");

            if (GUI.getItemMapper().containsKey(uuid)
                    && System.currentTimeMillis() >= interactTimeout.getOrDefault(e.getPlayer().getUniqueId(), -1L)) {
                GUI.getItemMapper().get(uuid).handleClick(e.getPlayer(), e.getAction());

                interactTimeout.put(e.getPlayer().getUniqueId(), System.currentTimeMillis() + 100L);
            }

            e.setCancelled(true);
        } catch (Exception ex) {
            logNBTWarning(ex);
        }
    }

    @EventHandler
    public void onAnimation(PlayerAnimationEvent e) {
        try {
            if (e.getAnimationType() != PlayerAnimationType.ARM_SWING
                    || e.getPlayer().getTargetBlock(new HashSet<>(), 5).getType() == Material.AIR
                    || e.getPlayer().getGameMode() != GameMode.ADVENTURE)
                return;
        } catch (Exception ex) {
            if (e.getPlayer().getTargetBlock(new HashSet<>(), 5).getType() == Material.AIR
                    || e.getPlayer().getGameMode() != GameMode.ADVENTURE)
                return;
        }

        ItemStack item = e.getPlayer().getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR || item.getAmount() <= 0)
            return;

        try {
            NBTItem nbtItem = new NBTItem(item);
            if (!nbtItem.hasTag("storage:id"))
                return;

            UUID uuid = nbtItem.getUUID("storage:id");

            if (System.currentTimeMillis() >= interactTimeout.getOrDefault(e.getPlayer().getUniqueId(), -1L)
                    && GUI.getItemMapper().containsKey(uuid)) {
                GUI.getItemMapper().get(uuid).handleClick(e.getPlayer(), Action.RIGHT_CLICK_BLOCK);

                interactTimeout.put(e.getPlayer().getUniqueId(), System.currentTimeMillis() + 100L);
            }

            e.setCancelled(true);
        } catch (Exception ex) {
            logNBTWarning(ex);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        ItemStack item = e.getItemDrop().getItemStack();
        if (item.getType() != Material.AIR && item.getAmount() > 0) {
            try {
                NBTItem nbtItem = new NBTItem(item);
                if (nbtItem.hasTag("storage:id"))
                    e.setCancelled(true);
            } catch (Exception ex) {
                logNBTWarning(ex);
            }
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        e.getDrops().removeIf(item -> {
            if (item.getType() != Material.AIR && item.getAmount() > 0) {
                try {
                    NBTItem nbtItem = new NBTItem(item);
                    return nbtItem.hasTag("storage:id");
                } catch (Exception ex) {
                    logNBTWarning(ex);
                    return false;
                }
            }
            return false;
        });
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        Player player = (Player) e.getPlayer();

        if (e.getInventory().getHolder() instanceof IGUI) {
            if (e.getInventory().getHolder() instanceof RecipeEditorGUI) {
                if (RecipeEditorGUI.getShouldRestoreOnClose(player)
                        && RecipeEditorGUI.hasActiveSession(player.getUniqueId())) {
                    String recipeName = RecipeEditorGUI.getBackupRecipeName(player.getUniqueId());

                    if (RecipeEditorGUI.restoreAndSaveBackup(player.getUniqueId())) {
                        player.sendMessage(net.danh.storage.Utils.ChatUtils.colorize(
                                File.getMessage()
                                        .getString("crafting.editor_discarded",
                                                "&7Changes discarded for recipe: &e#recipe#")
                                        .replace("#recipe#", recipeName)));
                    }
                    RecipeEditorGUI.cleanupBackup(player.getUniqueId());
                }
            }

            if (e.getInventory().getHolder() instanceof ViewStorageGUI) {
                ViewStorageGUI gui = (ViewStorageGUI) e.getInventory().getHolder();
                if (gui.getTarget() == null) {
                    MineManager.cleanupOfflinePlayerData(gui.getTargetName());
                }
            }

            if (e.getInventory().getHolder() instanceof ViewMythicStorageGUI) {
                ViewMythicStorageGUI gui = (ViewMythicStorageGUI) e.getInventory().getHolder();
                if (gui.getTarget() == null) {
                    MythicStorageManager.cleanupOfflinePlayerData(gui.getTargetName());
                }
            }

            if (SoundManager.getShouldPlayCloseSound(player)) {
                SoundManager.playCloseSound(player);
            }

            SoundManager.setShouldPlayCloseSound(player, true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();

        if (RecipeEditorGUI.hasActiveSession(player.getUniqueId())) {
            RecipeEditorGUI.restoreAndSaveBackup(player.getUniqueId());
        }

        RecipeEditorGUI.cleanupBackup(player.getUniqueId());
        interactTimeout.remove(player.getUniqueId());
    }
}