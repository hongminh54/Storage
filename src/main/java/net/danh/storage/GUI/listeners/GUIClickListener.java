package net.danh.storage.GUI.listeners;

import de.tr7zw.changeme.nbtapi.NBTItem;
import net.danh.storage.GUI.GUI;
import net.danh.storage.GUI.manager.IGUI;
import net.danh.storage.Manager.SoundManager;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public class GUIClickListener implements Listener {
    private static final HashMap<UUID, Long> interactTimeout = new HashMap<>();

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();

        boolean isGUIInv = e.getClickedInventory() != null && e.getClickedInventory().getHolder() != null && e.getClickedInventory().getHolder() instanceof IGUI;

        ItemStack currentItem = e.getCurrentItem();
        boolean isInteractiveItem = false;

        if (currentItem != null && currentItem.getType() != Material.AIR && currentItem.getAmount() > 0) {
            try {
                NBTItem nbtItem = new NBTItem(currentItem);
                isInteractiveItem = nbtItem.hasTag("storage:id");
            } catch (Exception ex) {
                isInteractiveItem = false;
            }
        }

        if (isGUIInv || isInteractiveItem) {
            e.setCancelled(true);
            player.updateInventory();

            if (currentItem != null && currentItem.getType() != Material.AIR && currentItem.getAmount() > 0) {
                try {
                    NBTItem nbtItem = new NBTItem(currentItem);
                    if (nbtItem.hasTag("storage:id")) {
                        UUID uuid = nbtItem.getUUID("storage:id");
                        if (GUI.getItemMapper().containsKey(uuid))
                            GUI.getItemMapper().get(uuid).handleClick(player, e.getClick());
                    }
                } catch (Exception ex) {
                    // Silent fail to prevent console spam
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

            if (GUI.getItemMapper().containsKey(uuid) && System.currentTimeMillis() >= interactTimeout.getOrDefault(e.getPlayer().getUniqueId(), -1L)) {
                GUI.getItemMapper().get(uuid).handleClick(e.getPlayer(), e.getAction());

                interactTimeout.put(e.getPlayer().getUniqueId(), System.currentTimeMillis() + 100L);
            }

            e.setCancelled(true);
        } catch (Exception ex) {
            // Silent fail to prevent console spam
        }
    }

    @EventHandler
    public void onAnimation(PlayerAnimationEvent e) {
        if (e.getAnimationType() != PlayerAnimationType.ARM_SWING || e.getPlayer().getTargetBlock(new HashSet<>(), 5).getType() == Material.AIR || e.getPlayer().getGameMode() != GameMode.ADVENTURE)
            return;

        ItemStack item = e.getPlayer().getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR || item.getAmount() <= 0) return;

        try {
            NBTItem nbtItem = new NBTItem(item);
            if (!nbtItem.hasTag("storage:id")) return;

            UUID uuid = nbtItem.getUUID("storage:id");

            if (System.currentTimeMillis() >= interactTimeout.getOrDefault(e.getPlayer().getUniqueId(), -1L) && GUI.getItemMapper().containsKey(uuid)) {
                GUI.getItemMapper().get(uuid).handleClick(e.getPlayer(), Action.RIGHT_CLICK_BLOCK);

                interactTimeout.put(e.getPlayer().getUniqueId(), System.currentTimeMillis() + 100L);
            }

            e.setCancelled(true);
        } catch (Exception ex) {
            // Silent fail to prevent console spam
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
                // Silent fail to prevent console spam
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
                    return false;
                }
            }
            return false;
        });
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        Player player = (Player) e.getPlayer();

        // Check if the closed inventory is a GUI
        if (e.getInventory().getHolder() instanceof IGUI) {
            // Check if we should play close sound
            if (SoundManager.getShouldPlayCloseSound(player)) {
                SoundManager.playCloseSound(player);
            }

            // Reset the flag for next time
            SoundManager.setShouldPlayCloseSound(player, true);
        }
    }
}