package net.danh.storage.GUI.Mob;

import net.danh.storage.GUI.manager.IGUI;
import net.danh.storage.GUI.manager.InteractiveItem;
import net.danh.storage.Manager.ItemManager;
import net.danh.storage.Manager.Mob.MobStorageManager;
import net.danh.storage.Manager.SoundManager;
import net.danh.storage.Utils.ChatUtils;
import net.danh.storage.Utils.File;
import net.danh.storage.Utils.Number;
import net.danh.storage.Utils.SoundContext;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MobStorageGUI implements IGUI {

    public static HashMap<UUID, Integer> playerCurrentPage = new HashMap<>();
    private final Player player;
    private final FileConfiguration config;
    private final int currentPage;

    public MobStorageGUI(Player player) {
        this(player, 0);
    }

    public MobStorageGUI(Player player, int page) {
        this.player = player;
        this.currentPage = Math.max(0, page);
        this.config = File.getMobStorageGUIConfig();
        playerCurrentPage.put(player.getUniqueId(), this.currentPage);
    }

    public static int getPlayerCurrentPage(Player player) {
        if (player == null) {
            return 0;
        }
        return playerCurrentPage.getOrDefault(player.getUniqueId(), 0);
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
                .replace("#player#", player.getName()));
        Inventory inventory = Bukkit.createInventory(this, config.getInt("size") * 9, title);

        List<String> configuredDrops = MobStorageManager.getConfiguredDrops();
        String slotConfig = Objects.requireNonNull(config.getString("items.mob_item.slot")).replace(" ", "");
        int itemsPerPage = slotConfig.split(",").length;
        int totalPages = Math.max(1, (int) Math.ceil((double) configuredDrops.size() / itemsPerPage));
        boolean hasMultiplePages = totalPages > 1;
        Set<Integer> navigationSlots = getNavigationSlots(hasMultiplePages);

        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        if (itemsSection == null) {
            return inventory;
        }

        for (String itemTag : itemsSection.getKeys(false)) {
            String slot = Objects.requireNonNull(config.getString("items." + itemTag + ".slot")).replace(" ", "");
            if (itemTag.equalsIgnoreCase("mob_item")) {
                addMobItems(inventory, configuredDrops, slot, itemsPerPage);
            } else if (itemTag.equalsIgnoreCase("previous_page")) {
                addPageItem(inventory, itemTag, slot, hasMultiplePages && currentPage > 0, currentPage - 1, totalPages);
            } else if (itemTag.equalsIgnoreCase("next_page")) {
                addPageItem(inventory, itemTag, slot, hasMultiplePages && currentPage < totalPages - 1, currentPage + 1, totalPages);
            } else {
                addConfiguredItem(inventory, itemTag, slot, navigationSlots, hasMultiplePages);
            }
        }
        return inventory;
    }

    private Set<Integer> getNavigationSlots(boolean hasMultiplePages) {
        Set<Integer> slots = new HashSet<>();
        if (!hasMultiplePages) {
            return slots;
        }
        addSlots(slots, config.getString("items.previous_page.slot"));
        addSlots(slots, config.getString("items.next_page.slot"));
        return slots;
    }

    private void addSlots(Set<Integer> slots, String raw) {
        if (raw == null) {
            return;
        }
        for (String slot : raw.split(",")) {
            slots.add(Number.getInteger(slot.trim()));
        }
    }

    private void addMobItems(Inventory inventory, List<String> configuredDrops, String slot, int itemsPerPage) {
        if (!slot.contains(",")) {
            return;
        }
        List<String> slotList = new ArrayList<>(Arrays.asList(slot.split(",")));
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, configuredDrops.size());
        for (int i = startIndex; i < endIndex; i++) {
            int slotIndex = i - startIndex;
            if (slotIndex >= slotList.size()) {
                continue;
            }
            String itemName = configuredDrops.get(i);
            Material material = Material.getMaterial(itemName);
            if (material == null || material == Material.AIR) {
                continue;
            }
            ItemStack displayItem = new ItemStack(material, 1);
            ItemMeta meta = displayItem.getItemMeta();
            if (meta != null) {
                int amount = MobStorageManager.getPlayerItem(player, itemName);
                int maxStorage = MobStorageManager.getMaxStorage(player);
                boolean autopickupEnabled = MobStorageManager.isAutoPickupEnabledForItem(player, itemName);
                String autopickupStatus = File.getMessage().getString(
                        autopickupEnabled ? "mobstorage.status_enabled" : "mobstorage.status_disabled",
                        autopickupEnabled ? "&aEnabled" : "&cDisabled");
                meta.setDisplayName(ChatUtils.colorizewp(MobStorageManager.getItemDisplayName(itemName)));
                List<String> lore = new ArrayList<>();
                for (String line : config.getStringList("items.mob_item.lore")) {
                    lore.add(ChatUtils.colorizewp(line
                            .replace("#item_amount#", String.valueOf(amount))
                            .replace("#max_storage#", String.valueOf(maxStorage))
                            .replace("#autopickup_status#", ChatUtils.colorizewp(autopickupStatus))
                            .replace("#sellable#", MobStorageManager.getSellableSymbol(itemName))));
                }
                meta.setLore(lore);
                displayItem.setItemMeta(meta);
            }
            InteractiveItem item = new InteractiveItem(displayItem, Number.getInteger(slotList.get(slotIndex)))
                    .onClick((p, clickType) -> handleItemClick(p, itemName, clickType));
            inventory.setItem(item.getSlot(), item);
        }
    }

    private void addPageItem(Inventory inventory, String itemTag, String slot, boolean shouldShow, int targetPage, int totalPages) {
        if (!shouldShow) {
            return;
        }
        ItemStack pageItem = getNavigationItem(itemTag, totalPages);
        if (pageItem == null) {
            return;
        }
        for (String slotString : slot.split(",")) {
            InteractiveItem item = new InteractiveItem(pageItem.clone(), Number.getInteger(slotString.trim()))
                    .onClick((p, clickType) -> {
                        SoundManager.playItemSound(p, config, "items." + itemTag, SoundContext.INITIAL_OPEN);
                        SoundManager.setShouldPlayCloseSound(p, false);
                        p.openInventory(new MobStorageGUI(p, targetPage).getInventory(SoundContext.SILENT));
                    });
            inventory.setItem(item.getSlot(), item);
        }
    }

    private void addConfiguredItem(Inventory inventory, String itemTag, String slot, Set<Integer> navigationSlots, boolean hasMultiplePages) {
        for (String slotString : slot.split(",")) {
            int slotNumber = Number.getInteger(slotString.trim());
            if (hasMultiplePages && navigationSlots.contains(slotNumber)) {
                continue;
            }
            ConfigurationSection section = config.getConfigurationSection("items." + itemTag);
            if (section == null) {
                continue;
            }
            ItemStack itemStack = buildConfiguredItem(itemTag, section);
            InteractiveItem item = new InteractiveItem(itemStack, slotNumber);
            if (itemTag.equalsIgnoreCase("toggle_item")) {
                item.onClick((p, clickType) -> handleToggleClick(p));
            } else if (itemTag.equalsIgnoreCase("groundstore_item")) {
                item.onClick((p, clickType) -> handleGroundStoreClick(p));
            }
            inventory.setItem(item.getSlot(), item);
        }
    }

    private ItemStack buildConfiguredItem(String itemTag, ConfigurationSection section) {
        if (itemTag.equalsIgnoreCase("toggle_item")) {
            boolean enabled = MobStorageManager.getToggleStatus(player);
            String status = File.getMessage().getString(
                    enabled ? "mobstorage.status_enabled" : "mobstorage.status_disabled",
                    enabled ? "&aEnabled" : "&cDisabled");
            return ItemManager.getItemConfigWithPlaceholders(player, section, "#status#", ChatUtils.colorizewp(status));
        }
        if (itemTag.equalsIgnoreCase("groundstore_item")) {
            boolean enabled = MobStorageManager.isGroundStoreEnabled(player);
            String status = File.getMessage().getString(
                    enabled ? "user.status.status_on" : "user.status.status_off",
                    enabled ? "ON" : "OFF");
            return ItemManager.getItemConfigWithPlaceholders(player, section, "#status#", ChatUtils.colorizewp(status));
        }
        return ItemManager.getItemConfig(section);
    }

    private void handleToggleClick(Player player) {
        SoundManager.playItemSound(player, config, "items.toggle_item", SoundContext.INITIAL_OPEN);
        boolean current = MobStorageManager.getToggleStatus(player);
        boolean applied = MobStorageManager.setToggleStatus(player, !current, true);
        if (applied != current) {
            player.sendMessage(ChatUtils.colorizewp(File.getMessage().getString(
                    applied ? "mobstorage.toggle_enabled" : "mobstorage.toggle_disabled")));
        }
        SoundManager.setShouldPlayCloseSound(player, false);
        player.openInventory(new MobStorageGUI(player, currentPage).getInventory(SoundContext.SILENT));
    }

    private void handleGroundStoreClick(Player player) {
        SoundManager.playItemSound(player, config, "items.groundstore_item", SoundContext.INITIAL_OPEN);
        if (!player.hasPermission("storage.mobstorage.groundstore")) {
            player.sendMessage(ChatUtils.colorizewp(File.getMessage().getString("mobstorage.ground_store_no_permission")));
            return;
        }
        if (!MobStorageManager.isGroundStoreSystemEnabled()) {
            player.sendMessage(ChatUtils.colorizewp(File.getMessage().getString("mobstorage.ground_store_system_disabled")));
            return;
        }
        if (File.getMobStorageConfig().getStringList("blacklist_world").contains(player.getWorld().getName())) {
            player.sendMessage(ChatUtils.colorize(File.getMessage().getString("admin.world_blacklisted")
                    .replace("#feature#", "MobStorage")
                    .replace("#world#", player.getWorld().getName())));
            return;
        }
        boolean before = MobStorageManager.isGroundStoreEnabled(player);
        boolean enabled = MobStorageManager.toggleGroundStore(player, true);
        if (enabled != before) {
            player.sendMessage(ChatUtils.colorizewp(File.getMessage().getString(
                    enabled ? "mobstorage.ground_store_toggle_on" : "mobstorage.ground_store_toggle_off")));
        }
        SoundManager.setShouldPlayCloseSound(player, false);
        player.openInventory(new MobStorageGUI(player, currentPage).getInventory(SoundContext.SILENT));
    }

    private void handleItemClick(Player player, String itemName, ClickType clickType) {
        if (clickType == ClickType.DROP) {
            boolean enabled = MobStorageManager.toggleItemAutoPickup(player, itemName, true);
            String status = File.getMessage().getString(
                    enabled ? "mobstorage.status_enabled" : "mobstorage.status_disabled",
                    enabled ? "&aEnabled" : "&cDisabled");
            player.sendMessage(ChatUtils.colorizewp(File.getMessage().getString(
                            "mobstorage.item_toggle",
                            "#prefix# &dMobStorage &bAuto-pickup for #item#: #status#")
                    .replace("#item#", MobStorageManager.getItemDisplayName(itemName))
                    .replace("#status#", ChatUtils.colorizewp(status))));
            SoundManager.setShouldPlayCloseSound(player, false);
            player.openInventory(new MobStorageGUI(player, currentPage).getInventory(SoundContext.SILENT));
            return;
        }
        SoundManager.setShouldPlayCloseSound(player, false);
        player.openInventory(new MobItemStorage(player, itemName, currentPage).getInventory(SoundContext.SILENT));
    }

    private ItemStack getNavigationItem(String itemTag, int totalPages) {
        ConfigurationSection section = config.getConfigurationSection("items." + itemTag);
        if (section == null) {
            return null;
        }
        return ItemManager.getItemConfigWithPlaceholders(player, section,
                "#current_page#", String.valueOf(currentPage + 1),
                "#total_pages#", String.valueOf(totalPages));
    }
}
