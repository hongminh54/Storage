package net.danh.storage.GUI;

import net.danh.storage.GUI.manager.IGUI;
import net.danh.storage.GUI.manager.InteractiveItem;
import net.danh.storage.Manager.ItemManager;
import net.danh.storage.Manager.MineManager;
import net.danh.storage.Utils.Chat;
import net.danh.storage.Utils.File;
import net.danh.storage.Utils.Number;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class PersonalStorage implements IGUI {

    private final Player p;
    private final FileConfiguration config;

    public PersonalStorage(Player p) {
        this.p = p;
        config = File.getGUIStorage();
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        Inventory inventory = Bukkit.createInventory(p, config.getInt("size") * 9, Chat.colorizewp(Objects.requireNonNull(config.getString("title")).replace("#player#", p.getName())));
        for (String item_tag : Objects.requireNonNull(config.getConfigurationSection("items")).getKeys(false)) {
            String slot = Objects.requireNonNull(config.getString("items." + item_tag + ".slot")).replace(" ", "");
            if (item_tag.equalsIgnoreCase("storage_item")) {
                if (slot.contains(",")) {
                    List<String> slot_list = new ArrayList<>(Arrays.asList(slot.split(",")));
                    List<String> item_list = new ArrayList<>(MineManager.getOrderedPluginBlocks());
                    for (int i = 0; i < item_list.size(); i++) {
                        String material = MineManager.getMaterial(item_list.get(i));
                        String name = File.getConfig().getString("items." + item_list.get(i));
                        ItemStack itemStack = ItemManager.getItemConfig(p, material, name != null ? name : item_list.get(i).split(";")[0], config.getConfigurationSection("items.storage_item"));
                        InteractiveItem interactiveItem = new InteractiveItem(itemStack, Number.getInteger(slot_list.get(i))).onClick((player, clickType) -> player.openInventory(new ItemStorage(p, material).getInventory()));
                        inventory.setItem(interactiveItem.getSlot(), interactiveItem);
                    }
                }
            } else if (item_tag.equalsIgnoreCase("toggle_item")) {
                if (slot.contains(",")) {
                    for (String slot_string : slot.split(",")) {
                        InteractiveItem item = new InteractiveItem(ItemManager.getItemConfig(p, Objects.requireNonNull(config.getConfigurationSection("items." + item_tag))), Number.getInteger(slot_string)).onClick((player, clickType) -> {
                            MineManager.toggle.replace(p, !MineManager.toggle.get(p));
                            p.sendMessage(Chat.colorize(Objects.requireNonNull(File.getMessage().getString("user.status.toggle")).replace("#status#", ItemManager.getStatus(p))));
                            p.openInventory(this.getInventory());
                        });
                        inventory.setItem(item.getSlot(), item);
                    }
                } else {
                    InteractiveItem item = new InteractiveItem(ItemManager.getItemConfig(p, Objects.requireNonNull(config.getConfigurationSection("items." + item_tag))), Number.getInteger(slot)).onClick((player, clickType) -> {
                        MineManager.toggle.replace(p, !MineManager.toggle.get(p));
                        p.sendMessage(Chat.colorize(Objects.requireNonNull(File.getMessage().getString("user.status.toggle")).replace("#status#", ItemManager.getStatus(p))));
                        p.openInventory(this.getInventory());
                    });
                    inventory.setItem(item.getSlot(), item);
                }
            } else {
                if (slot.contains(",")) {
                    for (String slot_string : slot.split(",")) {
                        InteractiveItem item = new InteractiveItem(ItemManager.getItemConfig(Objects.requireNonNull(config.getConfigurationSection("items." + item_tag))), Number.getInteger(slot_string));
                        inventory.setItem(item.getSlot(), item);
                    }
                } else {
                    InteractiveItem item = new InteractiveItem(ItemManager.getItemConfig(Objects.requireNonNull(config.getConfigurationSection("items." + item_tag))), Number.getInteger(slot));
                    inventory.setItem(item.getSlot(), item);
                }
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
}
