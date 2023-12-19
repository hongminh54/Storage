package net.danh.storage.GUI.listeners;

import de.tr7zw.changeme.nbtapi.NBTItem;
import net.danh.storage.GUI.GUI;
import net.danh.storage.GUI.manager.IGUI;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
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
        boolean isInteractiveItem = e.getCurrentItem() != null && e.getCurrentItem().getType() != Material.AIR && new NBTItem(e.getCurrentItem()).hasTag("storage:id");

        if (isGUIInv || isInteractiveItem) {
            e.setCancelled(true);
            player.updateInventory();

            UUID uuid = new NBTItem(e.getCurrentItem()).getUUID("storage:id");

            if (GUI.getItemMapper().containsKey(uuid))
                GUI.getItemMapper().get(uuid).handleClick(player, e.getClick());
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getItem() == null || e.getItem().getType() == Material.AIR || !new NBTItem(e.getItem()).hasTag("storage:id"))
            return;

        UUID uuid = new NBTItem(e.getItem()).getUUID("storage:id");

        if (GUI.getItemMapper().containsKey(uuid) && System.currentTimeMillis() >= interactTimeout.getOrDefault(e.getPlayer().getUniqueId(), -1L)) {
            GUI.getItemMapper().get(uuid).handleClick(e.getPlayer(), e.getAction());

            interactTimeout.put(e.getPlayer().getUniqueId(), System.currentTimeMillis() + 100L); // Timeout prevents the GUI opening twice
        }

        e.setCancelled(true);
    }

    @EventHandler
    public void onAnimation(PlayerAnimationEvent e) { // This compensates for the fact that PlayerInteractEvent is not called when the player is in Adventure mode
        if (e.getAnimationType() != PlayerAnimationType.ARM_SWING || e.getPlayer().getTargetBlock(new HashSet<>(), 5).getType() == Material.AIR || e.getPlayer().getGameMode() != GameMode.ADVENTURE)
            return;

        ItemStack item = e.getPlayer().getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR || !new NBTItem(item).hasTag("storage:id")) return;

        UUID uuid = new NBTItem(item).getUUID("storage:id");

        if (System.currentTimeMillis() >= interactTimeout.getOrDefault(e.getPlayer().getUniqueId(), -1L) && GUI.getItemMapper().containsKey(uuid)) {
            GUI.getItemMapper().get(uuid).handleClick(e.getPlayer(), Action.RIGHT_CLICK_BLOCK);

            interactTimeout.put(e.getPlayer().getUniqueId(), System.currentTimeMillis() + 100L); // Timeout prevents the GUI opening twice
        }

        e.setCancelled(true);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        if (e.getItemDrop().getItemStack().getType() != Material.AIR && new NBTItem(e.getItemDrop().getItemStack()).hasTag("storage:id"))
            e.setCancelled(true);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        e.getDrops().removeIf(item -> item.getType() != Material.AIR && new NBTItem(item).hasTag("storage:id"));
    }
}