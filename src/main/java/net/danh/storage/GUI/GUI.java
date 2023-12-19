package net.danh.storage.GUI;

import net.danh.storage.GUI.listeners.GUIClickListener;
import net.danh.storage.GUI.manager.InteractiveItem;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.UUID;

public class GUI {
    /**
     * The item mapper, which is used to recognize which item was clicked based on NBT tag.
     */
    private static final HashMap<UUID, InteractiveItem> itemMapper = new HashMap<>();

    public static HashMap<UUID, InteractiveItem> getItemMapper() {
        return itemMapper;
    }

    /**
     * Important to call in onEnable to register the GUI listener.
     *
     * @param plugin The plugin instance.
     */
    public static void register(Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(new GUIClickListener(), plugin);
    }

    /**
     * Fills the inventory with items, while also creating a border around it, with an option to disable sides and keep only the top and bottom frame.
     *
     * @param inventory   The inventory to create the border in.
     * @param fillerPanel The material to use for the inner fill, if null, it won't replace the original contents.
     * @param borderPanel The material to use for the border, if null, it won't replace the original contents. If you want the borders to be the same as filler, you have to specify the same item as in fillerPanel.
     * @param full        Whether to create a full border or just a frame.
     */
    public static void fillInventory(Inventory inventory, @Nullable ItemStack fillerPanel, @Nullable ItemStack borderPanel, boolean full) {
        for (int i = 0; i < inventory.getSize(); i++) {
            if ((i % 9 == 0 || (i - 8) % 9 == 0) && borderPanel != null)
                inventory.setItem(i, borderPanel);
            else if (full && (i < 9 || i >= inventory.getSize() - 9) && borderPanel != null)
                inventory.setItem(i, borderPanel);
            else if (fillerPanel != null)
                inventory.setItem(i, fillerPanel);
        }
    }

    /**
     * Fills the inventory with items, while also creating a border around it.
     *
     * @param inventory   The inventory to create the border in.
     * @param fillerPanel The material to use for the inner fill, if null, it won't replace the original contents.
     * @param borderPanel The material to use for the border, if null, it won't replace the original contents. If you want the borders to be the same as filler, you have to specify the same item as in fillerPanel.
     */
    public static void fillInventory(Inventory inventory, @Nullable ItemStack fillerPanel, @Nullable ItemStack borderPanel) {
        fillInventory(inventory, fillerPanel, borderPanel, true);
    }
}
