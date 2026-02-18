package net.danh.storage.GUI.Crop;

import net.danh.storage.Action.CropDeposit;
import net.danh.storage.Action.CropSell;
import net.danh.storage.Action.CropWithdraw;
import net.danh.storage.GUI.manager.IGUI;
import net.danh.storage.GUI.manager.InteractiveItem;
import net.danh.storage.Listeners.ChatListener;
import net.danh.storage.Manager.Crop.CropStorageManager;
import net.danh.storage.Manager.ItemManager;
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

public class CropItemStorage implements IGUI {

    private final Player player;
    private final String itemName;
    private final FileConfiguration config;
    private final int returnPage;

    public CropItemStorage(Player player, String itemName, int returnPage) {
        this.player = player;
        this.itemName = itemName;
        this.returnPage = Math.max(0, returnPage);
        this.config = File.getCropItemStorageGUIConfig();
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

        String displayName = CropStorageManager.getItemDisplayName(itemName);
        String title = ChatUtils.colorizewp(player, Objects.requireNonNull(config.getString("title"))
                .replace("#player#", player.getName())
                .replace("#material#", displayName));

        Inventory inventory = Bukkit.createInventory(this, config.getInt("size") * 9, title);

        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        if (itemsSection == null) {
            return inventory;
        }

        for (String itemTag : itemsSection.getKeys(false)) {
            ConfigurationSection section = itemsSection.getConfigurationSection(itemTag);
            if (section == null) continue;

            String slotConfig = section.getString("slot");
            if (slotConfig == null) continue;

            String slotString = slotConfig.replace(" ", "");
            if (slotString.contains(",")) {
                for (String s : slotString.split(",")) {
                    addItem(inventory, itemTag, section, Number.getInteger(s.trim()));
                }
            } else {
                addItem(inventory, itemTag, section, Number.getInteger(slotString));
            }
        }

        return inventory;
    }

    private void addItem(Inventory inventory, String itemTag, ConfigurationSection section, int slot) {
        ItemStack itemStack = buildItem(section);
        if (itemStack == null) return;

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

        inventory.setItem(slot, item);
    }

    private ItemStack buildItem(ConfigurationSection section) {
        int amount = CropStorageManager.getPlayerItem(player, itemName);
        int maxStorage = CropStorageManager.getMaxStorage(player);
        String displayName = CropStorageManager.getItemDisplayName(itemName);

        return ItemManager.getItemConfigWithPlaceholders(player, section,
                "#item_amount#", String.valueOf(amount),
                "#max_storage#", String.valueOf(maxStorage),
                "#material#", displayName,
                "#player#", player.getName());
    }

    private void handleAction(String itemTag, String type, String action, boolean isLeftClick) {
        if (player == null || !player.isOnline()) return;

        if ("command".equalsIgnoreCase(type) && "back".equalsIgnoreCase(action)) {
            SoundManager.playActionSound(player, "items." + itemTag + ".action." + (isLeftClick ? "left" : "right"), config);
            SoundManager.setShouldPlayCloseSound(player, false);
            player.openInventory(new CropStorageGUI(player, returnPage).getInventory(SoundContext.SILENT));
            return;
        }

        if ("withdraw".equalsIgnoreCase(action)) {
            SoundManager.playActionSound(player, "items." + itemTag + ".action." + (isLeftClick ? "left" : "right"), config);

            if ("chat".equalsIgnoreCase(type)) {
                ChatListener.chat_crop_withdraw.put(player.getUniqueId(), itemName);
                ChatListener.chat_return_page.put(player.getUniqueId(), returnPage);
                player.sendMessage(ChatUtils.colorizewp(File.getMessage().getString("cropstorage.action.withdraw.chat_number")));
                SoundManager.setShouldPlayCloseSound(player, false);
                player.closeInventory();
                return;
            }

            if ("all".equalsIgnoreCase(type)) {
                int currentAmount = CropStorageManager.getPlayerItem(player, itemName);
                if (currentAmount <= 0) {
                    String msg = File.getMessage().getString("cropstorage.action.withdraw.not_enough", "")
                            .replace("#amount#", String.valueOf(currentAmount));
                    if (!msg.isEmpty()) {
                        player.sendMessage(ChatUtils.colorizewp(msg));
                    }
                    SoundManager.playErrorSound(player);
                    return;
                }

                new CropWithdraw(player, itemName, currentAmount).doAction();
                SoundManager.playWithdrawSound(player);
                SoundManager.setShouldPlayCloseSound(player, false);
                player.openInventory(new CropStorageGUI(player, returnPage).getInventory(SoundContext.SILENT));
                return;
            }
        }

        if ("deposit".equalsIgnoreCase(action)) {
            SoundManager.playActionSound(player, "items." + itemTag + ".action." + (isLeftClick ? "left" : "right"), config);

            if ("chat".equalsIgnoreCase(type)) {
                ChatListener.chat_crop_deposit.put(player.getUniqueId(), itemName);
                ChatListener.chat_return_page.put(player.getUniqueId(), returnPage);
                player.sendMessage(ChatUtils.colorizewp(File.getMessage().getString("cropstorage.action.deposit.chat_number")));
                SoundManager.setShouldPlayCloseSound(player, false);
                player.closeInventory();
                return;
            }

            if ("all".equalsIgnoreCase(type)) {
                new CropDeposit(player, itemName, Integer.MAX_VALUE).doAction();
                SoundManager.playDepositSound(player);
                SoundManager.setShouldPlayCloseSound(player, false);
                player.openInventory(new CropStorageGUI(player, returnPage).getInventory(SoundContext.SILENT));
                return;
            }
        }

        if ("sell".equalsIgnoreCase(action)) {
            SoundManager.playActionSound(player, "items." + itemTag + ".action." + (isLeftClick ? "left" : "right"), config);

            if ("chat".equalsIgnoreCase(type)) {
                ChatListener.chat_crop_sell.put(player.getUniqueId(), itemName);
                ChatListener.chat_return_page.put(player.getUniqueId(), returnPage);
                String msg = File.getMessage().getString(
                        "cropstorage.action.sell.chat_number",
                        "#prefix# &aHow many items do you want to sell? &7(Type 'cancel' to exit)"
                );
                player.sendMessage(ChatUtils.colorizewp(msg));
                SoundManager.setShouldPlayCloseSound(player, false);
                player.closeInventory();
                return;
            }

            if ("all".equalsIgnoreCase(type)) {
                new CropSell(player, itemName, -1).doAction();
                SoundManager.playSellSound(player);
                SoundManager.setShouldPlayCloseSound(player, false);
                player.openInventory(new CropStorageGUI(player, returnPage)
                        .getInventory(SoundContext.SILENT));
            }
        }
    }
}
