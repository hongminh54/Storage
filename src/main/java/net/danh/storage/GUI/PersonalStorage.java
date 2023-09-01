package net.danh.storage.GUI;

import dev.digitality.digitalgui.api.IGUI;
import dev.digitality.digitalgui.api.InteractiveItem;
import net.danh.storage.Manager.GameManager.ChatManager;
import net.danh.storage.Manager.GameManager.MineManager;
import net.danh.storage.Manager.UtilsManager.FileManager;
import net.danh.storage.Manager.UtilsManager.ItemManager;
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
        config = FileManager.getGUIStorage();
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        Inventory inventory = Bukkit.createInventory(p, config.getInt("size") * 9, ChatManager.colorizewp(Objects.requireNonNull(config.getString("title")).replace("#player#", p.getName())));
        for (String item_tag : Objects.requireNonNull(config.getConfigurationSection("items")).getKeys(false)) {
            if (item_tag.equalsIgnoreCase("storage_item")) {
                String slot = Objects.requireNonNull(config.getString("items." + item_tag + ".slot")).replace(" ", "");
                if (slot.contains(",")) {
                    List<String> slot_list = new ArrayList<>(Arrays.asList(slot.split(",")));
                    List<String> item_list = new ArrayList<>(MineManager.getPluginBlocks());
                    for (int i = 0; i < slot_list.size(); i++) {
                        String material = MineManager.getMaterial(item_list.get(i));
                        String name = FileManager.getConfig().getString("items." + item_list.get(i));
                        ItemStack itemStack = ItemManager.getItemConfig(p, material, name != null ? name : item_list.get(i).split(";")[0], config.getConfigurationSection("items.storage_item"));
                        InteractiveItem interactiveItem = new InteractiveItem(itemStack, Number.getInteger(slot_list.get(i)));
                        inventory.setItem(interactiveItem.getSlot(), interactiveItem);
                    }
                }
            } else {
                String slot = Objects.requireNonNull(config.getString("items." + item_tag + ".slot")).replace(" ", "");
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

//        InteractiveItem item = new InteractiveItem(Material.DIAMOND, 0, "§aDiamond", "§7This is a diamond.")
//                .onClick((player, clickType) -> { // This will run for any click action
//                    player.sendMessage("You clicked the diamond!");
//                })
//                .onLeftClick(player -> { // This will run on left click, regardless of whether it was InventoryClickEvent or PlayerInteractEvent
//                    player.sendMessage("You left clicked the diamond!");
//                })
//                .onRightClick(player -> { // This will run on right click, regardless of whether it was InventoryClickEvent or PlayerInteractEvent
//                    player.sendMessage("You right clicked the diamond!");
//                });
//        inventory.setItem(item.getSlot(), item);

        return inventory;
    }

    public Player getPlayer() {
        return p;
    }

    public FileConfiguration getConfig() {
        return config;
    }
}
