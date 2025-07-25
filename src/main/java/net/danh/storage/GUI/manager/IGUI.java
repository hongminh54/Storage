package net.danh.storage.GUI.manager;

import net.danh.storage.Utils.SoundContext;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public interface IGUI extends InventoryHolder {

    @NotNull
    Inventory getInventory();

    @NotNull
    default Inventory getInventory(SoundContext context) {
        return getInventory();
    }

}
