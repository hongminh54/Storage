package net.danh.storage.Listeners;

import net.danh.storage.Manager.SpecialMaterial.SpecialMaterialManager;
import net.danh.storage.Storage;
import org.bukkit.entity.Firework;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class SpecialMaterialEffects implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFireworkDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Firework)) return;
        if (event.getDamager().getMetadata(SpecialMaterialManager.FIREWORK_METADATA).stream()
                .anyMatch(value -> value.getOwningPlugin() == Storage.getStorage() && value.asBoolean())) {
            event.setCancelled(true);
        }
    }
}
