package net.danh.storage.API;

import com.cryptomorin.xseries.XMaterial;
import net.danh.storage.GUI.GUI;
import net.danh.storage.GUI.manager.InteractiveItem;
import net.danh.storage.Utils.Chat;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * API for creating custom GUIs with Storage data
 * Provides builder pattern for easy GUI creation
 *
 * @author hongminh54
 * @version 2.3.3
 */
public class StorageGUIAPI {

    /**
     * Create a new GUI builder
     *
     * @param title GUI title
     * @param rows  Number of rows (1-6)
     * @return GUIBuilder instance
     */
    @NotNull
    public static GUIBuilder createGUI(@NotNull String title, int rows) {
        if (rows < 1 || rows > 6) {
            throw new IllegalArgumentException("Rows must be between 1 and 6");
        }
        return new GUIBuilder(title, rows);
    }

    /**
     * Create a storage viewer GUI for player
     *
     * @param viewer Target player to view
     * @param title  GUI title
     * @return Inventory with player's storage items
     */
    @NotNull
    public static Inventory createStorageViewer(@NotNull Player viewer, @NotNull String title) {
        StoragePlayer storagePlayer = StorageAPI.getStoragePlayer(viewer);
        Map<String, Integer> materials = storagePlayer.getStoredMaterialsWithAmounts();

        int rows = Math.min(6, (materials.size() + 8) / 9);
        if (rows == 0) rows = 1;

        Inventory inv = Bukkit.createInventory(null, rows * 9, Chat.colorize(title));

        int slot = 0;
        for (Map.Entry<String, Integer> entry : materials.entrySet()) {
            if (slot >= rows * 9) break;

            String material = entry.getKey();
            int amount = entry.getValue();

            StorageItem storageItem = new StorageItem(material, amount);
            ItemStack item = storageItem.getItemStack();

            if (item != null) {
                inv.setItem(slot, item);
                slot++;
            }
        }

        return inv;
    }

    /**
     * Create a material selector GUI
     *
     * @param title    GUI title
     * @param callback Callback when material is selected
     * @return GUIBuilder with materials
     */
    @NotNull
    public static GUIBuilder createMaterialSelector(@NotNull String title,
                                                     @NotNull Consumer<String> callback) {
        List<String> materials = StorageAPI.getStorableMaterials();
        int rows = Math.min(6, (materials.size() + 8) / 9);
        if (rows == 0) rows = 1;

        GUIBuilder builder = createGUI(title, rows);

        int slot = 0;
        for (String material : materials) {
            if (slot >= rows * 9) break;

            StorageItem storageItem = new StorageItem(material, 1);
            ItemStack item = storageItem.getItemStack();

            if (item != null) {
                builder.addItem(slot, item, (player, clickType) -> {
                    callback.accept(material);
                    player.closeInventory();
                });
                slot++;
            }
        }

        return builder;
    }

    /**
     * Fill inventory with border and filler items
     *
     * @param inventory   Target inventory
     * @param fillerPanel Filler item (null to skip)
     * @param borderPanel Border item (null to skip)
     */
    public static void fillInventory(@NotNull Inventory inventory, @Nullable ItemStack fillerPanel,
                                     @Nullable ItemStack borderPanel) {
        GUI.fillInventory(inventory, fillerPanel, borderPanel, true);
    }

    /**
     * Fill inventory with border only (top/bottom frame)
     *
     * @param inventory   Target inventory
     * @param fillerPanel Filler item (null to skip)
     * @param borderPanel Border item (null to skip)
     */
    public static void fillInventoryFrame(@NotNull Inventory inventory, @Nullable ItemStack fillerPanel,
                                          @Nullable ItemStack borderPanel) {
        GUI.fillInventory(inventory, fillerPanel, borderPanel, false);
    }

    /**
     * Create an interactive item
     *
     * @param material    Material type
     * @param slot        Slot position
     * @param displayName Display name
     * @param lore        Lore lines
     * @return InteractiveItem instance
     */
    @NotNull
    public static InteractiveItem createItem(@NotNull Material material, int slot,
                                             @NotNull String displayName, @NotNull String... lore) {
        String[] coloredLore = new String[lore.length];
        for (int i = 0; i < lore.length; i++) {
            coloredLore[i] = Chat.colorize(lore[i]);
        }
        return new InteractiveItem(material, slot, Chat.colorize(displayName), coloredLore);
    }

    /**
     * Create an interactive item from XMaterial
     *
     * @param xMaterial   XMaterial type
     * @param slot        Slot position
     * @param displayName Display name
     * @param lore        Lore lines
     * @return InteractiveItem instance
     */
    @NotNull
    public static InteractiveItem createItem(@NotNull XMaterial xMaterial, int slot,
                                             @NotNull String displayName, @NotNull String... lore) {
        Material material = xMaterial.parseMaterial();
        if (material == null) {
            material = Material.STONE;
        }
        return createItem(material, slot, displayName, lore);
    }

    /**
     * GUI Builder class for easy GUI creation
     */
    public static class GUIBuilder {
        private final String title;
        private final int rows;
        private final Map<Integer, InteractiveItem> items;
        private ItemStack fillerPanel;
        private ItemStack borderPanel;
        private boolean fullBorder = true;

        private GUIBuilder(String title, int rows) {
            this.title = title;
            this.rows = rows;
            this.items = new HashMap<>();
        }

        /**
         * Add an item to the GUI
         *
         * @param slot Slot position
         * @param item ItemStack to add
         * @return This builder
         */
        @NotNull
        public GUIBuilder addItem(int slot, @NotNull ItemStack item) {
            items.put(slot, new InteractiveItem(item, slot));
            return this;
        }

        /**
         * Add an interactive item with click handler
         *
         * @param slot     Slot position
         * @param item     ItemStack to add
         * @param onClick  Click handler
         * @return This builder
         */
        @NotNull
        public GUIBuilder addItem(int slot, @NotNull ItemStack item,
                                  @NotNull BiConsumer<Player, ClickType> onClick) {
            InteractiveItem interactiveItem = new InteractiveItem(item, slot);
            interactiveItem.onClick(onClick);
            items.put(slot, interactiveItem);
            return this;
        }

        /**
         * Add an interactive item with separate left/right click handlers
         *
         * @param slot         Slot position
         * @param item         ItemStack to add
         * @param onLeftClick  Left click handler
         * @param onRightClick Right click handler
         * @return This builder
         */
        @NotNull
        public GUIBuilder addItem(int slot, @NotNull ItemStack item,
                                  @NotNull Consumer<Player> onLeftClick,
                                  @NotNull Consumer<Player> onRightClick) {
            InteractiveItem interactiveItem = new InteractiveItem(item, slot);
            interactiveItem.onLeftClick(onLeftClick);
            interactiveItem.onRightClick(onRightClick);
            items.put(slot, interactiveItem);
            return this;
        }

        /**
         * Add a storage item display
         *
         * @param slot     Slot position
         * @param player   Player whose storage to display
         * @param material Material to display
         * @return This builder
         */
        @NotNull
        public GUIBuilder addStorageItem(int slot, @NotNull Player player, @NotNull String material) {
            int amount = StorageAPI.getItemAmount(player, material);
            StorageItem storageItem = new StorageItem(material, amount);
            ItemStack item = storageItem.getItemStack();

            if (item != null) {
                items.put(slot, new InteractiveItem(item, slot));
            }
            return this;
        }

        /**
         * Set filler panel item
         *
         * @param item Filler item
         * @return This builder
         */
        @NotNull
        public GUIBuilder setFillerPanel(@Nullable ItemStack item) {
            this.fillerPanel = item;
            return this;
        }

        /**
         * Set border panel item
         *
         * @param item Border item
         * @return This builder
         */
        @NotNull
        public GUIBuilder setBorderPanel(@Nullable ItemStack item) {
            this.borderPanel = item;
            return this;
        }

        /**
         * Set whether to use full border or just frame
         *
         * @param fullBorder True for full border, false for frame only
         * @return This builder
         */
        @NotNull
        public GUIBuilder setFullBorder(boolean fullBorder) {
            this.fullBorder = fullBorder;
            return this;
        }

        /**
         * Add navigation buttons (previous/next)
         *
         * @param previousSlot Previous button slot
         * @param nextSlot     Next button slot
         * @param onPrevious   Previous button handler
         * @param onNext       Next button handler
         * @return This builder
         */
        @NotNull
        public GUIBuilder addNavigation(int previousSlot, int nextSlot,
                                        @NotNull Consumer<Player> onPrevious,
                                        @NotNull Consumer<Player> onNext) {
            // Previous button
            InteractiveItem prevItem = createItem(Material.ARROW, previousSlot,
                    "&e<< Previous Page", "&7Click to go back");
            prevItem.onLeftClick(onPrevious);
            items.put(previousSlot, prevItem);

            // Next button
            InteractiveItem nextItem = createItem(Material.ARROW, nextSlot,
                    "&eNext Page >>", "&7Click to continue");
            nextItem.onLeftClick(onNext);
            items.put(nextSlot, nextItem);

            return this;
        }

        /**
         * Build and create the inventory
         *
         * @return Created inventory
         */
        @NotNull
        public Inventory build() {
            Inventory inv = Bukkit.createInventory(null, rows * 9, Chat.colorize(title));

            // Fill with panels
            if (fillerPanel != null || borderPanel != null) {
                GUI.fillInventory(inv, fillerPanel, borderPanel, fullBorder);
            }

            // Add items
            for (Map.Entry<Integer, InteractiveItem> entry : items.entrySet()) {
                inv.setItem(entry.getKey(), entry.getValue());
            }

            return inv;
        }

        /**
         * Build and open the inventory for player
         *
         * @param player Player to open for
         */
        public void open(@NotNull Player player) {
            player.openInventory(build());
        }
    }

    /**
     * Pagination helper for large item lists
     */
    public static class PaginationHelper {
        private final List<ItemStack> items;
        private final int itemsPerPage;

        public PaginationHelper(@NotNull List<ItemStack> items, int itemsPerPage) {
            this.items = items;
            this.itemsPerPage = itemsPerPage;
        }

        public int getTotalPages() {
            return (items.size() + itemsPerPage - 1) / itemsPerPage;
        }

        @NotNull
        public List<ItemStack> getPage(int page) {
            int start = page * itemsPerPage;
            int end = Math.min(start + itemsPerPage, items.size());

            if (start >= items.size()) {
                return new ArrayList<>();
            }

            return items.subList(start, end);
        }

        public boolean hasNextPage(int currentPage) {
            return currentPage < getTotalPages() - 1;
        }

        public boolean hasPreviousPage(int currentPage) {
            return currentPage > 0;
        }
    }
}
