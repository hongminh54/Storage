package net.danh.storage.GUI;

import dev.digitality.digitalgui.api.IGUI;
import dev.digitality.digitalgui.api.InteractiveItem;
import net.danh.storage.Manager.ItemManager;
import net.danh.storage.Utils.Chat;
import net.danh.storage.Utils.File;
import net.danh.storage.Utils.Number;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ItemStorage implements IGUI {

    private final Player p;

    private final String material;
    private final FileConfiguration config;

    public ItemStorage(Player p, String material) {
        this.p = p;
        this.material = material;
        config = File.getItemStorage();
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        Inventory inventory = Bukkit.createInventory(p, config.getInt("size") * 9, Chat.colorizewp(Objects.requireNonNull(config.getString("title")).replace("#player#", p.getName()).replace("#material#", material.split(";")[0])));
        for (String item_tag : Objects.requireNonNull(config.getConfigurationSection("items")).getKeys(false)) {
            String slot = Objects.requireNonNull(config.getString("items." + item_tag + ".slot")).replace(" ", "");
            if (slot.contains(",")) {
                for (String slot_string : slot.split(",")) {
                    InteractiveItem item = new InteractiveItem(ItemManager.getItemConfig(p, material, Objects.requireNonNull(config.getConfigurationSection("items." + item_tag))), Number.getInteger(slot_string));
                    inventory.setItem(item.getSlot(), item);
                }
            } else {
                InteractiveItem item = new InteractiveItem(ItemManager.getItemConfig(p, material, Objects.requireNonNull(config.getConfigurationSection("items." + item_tag))), Number.getInteger(slot));
                inventory.setItem(item.getSlot(), item);
            }
        }
        return inventory;
    }

    public Player getPlayer() {
        return p;
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public String getMaterial() {
        return material;
    }
}
