package net.danh.storage.GUI.Mob;

import net.danh.storage.Action.MobDeposit;
import net.danh.storage.Action.MobSell;
import net.danh.storage.Action.MobWithdraw;
import net.danh.storage.GUI.manager.IGUI;
import net.danh.storage.GUI.manager.InteractiveItem;
import net.danh.storage.Listeners.ChatListener;
import net.danh.storage.Manager.ItemManager;
import net.danh.storage.Manager.Mob.MobStorageManager;
import net.danh.storage.Manager.SoundManager;
import net.danh.storage.Utils.ChatUtils;
import net.danh.storage.Utils.File;
import net.danh.storage.Utils.Number;
import net.danh.storage.Utils.SoundContext;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class MobItemStorage implements IGUI {

    private final Player player;
    private final String itemName;
    private final FileConfiguration config;
    private final int returnPage;

    public MobItemStorage(Player player, String itemName, int returnPage) {
        this.player = player;
        this.itemName = itemName;
        this.returnPage = Math.max(0, returnPage);
        this.config = File.getMobItemStorageGUIConfig();
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return getInventory(SoundContext.INITIAL_OPEN);
    }

    @NotNull
    @Override
    public Inventory getInventory(SoundContext context) {
        SoundManager.playItemSound(player, config, "gui_open_sound", context);
        String title = ChatUtils.colorizewp(player, Objects.requireNonNull(config.getString("title"))
                .replace("#player#", player.getName())
                .replace("#material#", MobStorageManager.getItemDisplayName(itemName)));
        Inventory inventory = Bukkit.createInventory(this, config.getInt("size") * 9, title);

        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        if (itemsSection == null) {
            return inventory;
        }
        for (String itemTag : itemsSection.getKeys(false)) {
            ConfigurationSection section = itemsSection.getConfigurationSection(itemTag);
            if (section == null || section.getString("slot") == null) {
                continue;
            }
            for (String slot : section.getString("slot").replace(" ", "").split(",")) {
                addItem(inventory, itemTag, section, Number.getInteger(slot.trim()));
            }
        }
        return inventory;
    }

    private void addItem(Inventory inventory, String itemTag, ConfigurationSection section, int slot) {
        ItemStack itemStack = buildItem(section);
        if (itemStack == null) {
            return;
        }
        InteractiveItem item = new InteractiveItem(itemStack, slot);
        String typeLeft = config.getString("items." + itemTag + ".action.left.type");
        String actionLeft = config.getString("items." + itemTag + ".action.left.action");
        String typeRight = config.getString("items." + itemTag + ".action.right.type");
        String actionRight = config.getString("items." + itemTag + ".action.right.action");

        if (typeLeft != null && actionLeft != null) {
            item.onLeftClick(p -> handleAction(itemTag, typeLeft, actionLeft, true));
        }
        if (typeRight != null && actionRight != null) {
            item.onRightClick(p -> handleAction(itemTag, typeRight, actionRight, false));
        }
        if ("sell".equalsIgnoreCase(itemTag)) {
            item.onDropClick(p -> {
                if (!p.hasPermission("storage.mobstorage.autosell")) {
                    p.sendMessage(ChatUtils.colorizewp(File.getMessage().getString("mobstorage.admin.no_permission")));
                    return;
                }
                boolean enabled = MobStorageManager.toggleItemAutoSell(player, itemName);
                String key = enabled ? "mobstorage.autosell.toggle_on" : "mobstorage.autosell.toggle_off";
                player.sendMessage(ChatUtils.colorizewp(File.getMessage().getString(key, "")
                        .replace("#material#", MobStorageManager.getItemDisplayName(itemName))));
                SoundManager.setShouldPlayCloseSound(player, false);
                player.openInventory(new MobItemStorage(player, itemName, returnPage).getInventory(SoundContext.SILENT));
            });
        }
        inventory.setItem(slot, item);
    }

    private ItemStack buildItem(ConfigurationSection section) {
        int amount = MobStorageManager.getPlayerItem(player, itemName);
        int maxStorage = MobStorageManager.getMaxStorage(player);
        boolean autoSellEnabled = MobStorageManager.isAutoSellEnabledForItem(player, itemName);
        String autoSellSymbol = File.getMessage().getString(
                autoSellEnabled ? "mobstorage.autosell.yes" : "mobstorage.autosell.no",
                autoSellEnabled ? "&a✔" : "&c✘");
        return ItemManager.getItemConfigWithPlaceholders(player, section,
                "#item_amount#", String.valueOf(amount),
                "#max_storage#", String.valueOf(maxStorage),
                "#material#", MobStorageManager.getItemDisplayName(itemName),
                "#player#", player.getName(),
                "#autosell#", autoSellSymbol);
    }

    private void handleAction(String itemTag, String type, String action, boolean leftClick) {
        if (player == null || !player.isOnline()) {
            return;
        }

        if ("command".equalsIgnoreCase(type) && "back".equalsIgnoreCase(action)) {
            SoundManager.playActionSound(player, "items." + itemTag + ".action." + (leftClick ? "left" : "right"), config);
            SoundManager.setShouldPlayCloseSound(player, false);
            player.openInventory(new MobStorageGUI(player, returnPage).getInventory(SoundContext.SILENT));
            return;
        }

        if ("withdraw".equalsIgnoreCase(action)) {
            handleWithdraw(itemTag, type, leftClick);
        } else if ("deposit".equalsIgnoreCase(action)) {
            handleDeposit(itemTag, type, leftClick);
        } else if ("sell".equalsIgnoreCase(action)) {
            handleSell(itemTag, type, leftClick);
        }
    }

    private void handleWithdraw(String itemTag, String type, boolean leftClick) {
        SoundManager.playActionSound(player, "items." + itemTag + ".action." + (leftClick ? "left" : "right"), config);
        if ("chat".equalsIgnoreCase(type)) {
            ChatListener.chat_mob_withdraw.put(player.getUniqueId(), itemName);
            ChatListener.chat_return_page.put(player.getUniqueId(), returnPage);
            player.sendMessage(ChatUtils.colorizewp(File.getMessage().getString("mobstorage.action.withdraw.chat_number")));
            SoundManager.setShouldPlayCloseSound(player, false);
            player.closeInventory();
            return;
        }
        if ("all".equalsIgnoreCase(type)) {
            int currentAmount = MobStorageManager.getPlayerItem(player, itemName);
            if (currentAmount <= 0) {
                player.sendMessage(ChatUtils.colorizewp(File.getMessage().getString("mobstorage.action.withdraw.not_enough", "")
                        .replace("#amount#", String.valueOf(currentAmount))));
                SoundManager.playErrorSound(player);
                return;
            }
            new MobWithdraw(player, itemName, currentAmount).doAction();
            SoundManager.playWithdrawSound(player);
            reopenParent();
        }
    }

    private void handleDeposit(String itemTag, String type, boolean leftClick) {
        SoundManager.playActionSound(player, "items." + itemTag + ".action." + (leftClick ? "left" : "right"), config);
        if ("chat".equalsIgnoreCase(type)) {
            ChatListener.chat_mob_deposit.put(player.getUniqueId(), itemName);
            ChatListener.chat_return_page.put(player.getUniqueId(), returnPage);
            player.sendMessage(ChatUtils.colorizewp(File.getMessage().getString("mobstorage.action.deposit.chat_number")));
            SoundManager.setShouldPlayCloseSound(player, false);
            player.closeInventory();
            return;
        }
        if ("all".equalsIgnoreCase(type)) {
            new MobDeposit(player, itemName, Integer.MAX_VALUE).doAction();
            SoundManager.playDepositSound(player);
            reopenParent();
        }
    }

    private void handleSell(String itemTag, String type, boolean leftClick) {
        SoundManager.playActionSound(player, "items." + itemTag + ".action." + (leftClick ? "left" : "right"), config);
        if ("chat".equalsIgnoreCase(type)) {
            ChatListener.chat_mob_sell.put(player.getUniqueId(), itemName);
            ChatListener.chat_return_page.put(player.getUniqueId(), returnPage);
            player.sendMessage(ChatUtils.colorizewp(File.getMessage().getString(
                    "mobstorage.action.sell.chat_number",
                    "#prefix# &aHow many items do you want to sell? &7(Type 'cancel' to exit)")));
            SoundManager.setShouldPlayCloseSound(player, false);
            player.closeInventory();
            return;
        }
        if ("all".equalsIgnoreCase(type)) {
            new MobSell(player, itemName, -1).doAction();
            SoundManager.playSellSound(player);
            reopenParent();
        }
    }

    private void reopenParent() {
        SoundManager.setShouldPlayCloseSound(player, false);
        player.openInventory(new MobStorageGUI(player, returnPage).getInventory(SoundContext.SILENT));
    }
}
