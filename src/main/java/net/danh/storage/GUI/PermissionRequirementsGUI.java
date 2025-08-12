package net.danh.storage.GUI;

import com.cryptomorin.xseries.XMaterial;
import net.danh.storage.GUI.manager.IGUI;
import net.danh.storage.GUI.manager.InteractiveItem;
import net.danh.storage.Manager.CraftingManager;
import net.danh.storage.Manager.SoundManager;
import net.danh.storage.Recipe.Recipe;
import net.danh.storage.Utils.Chat;
import net.danh.storage.Utils.SoundContext;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PermissionRequirementsGUI implements IGUI {

    // Common permission suggestions
    private static final String[] COMMON_PERMISSIONS = {
            "storage.craft.enhanced",
            "storage.craft.magic",
            "storage.craft.ultimate",
            "storage.craft.legendary",
            "storage.craft.mythic",
            "storage.vip",
            "storage.premium",
            "storage.admin",
            "essentials.fly",
            "worldedit.navigation.jumpto",
            "minecraft.command.gamemode",
            "bukkit.command.teleport"
    };
    private final Player player;
    private final Recipe recipe;

    public PermissionRequirementsGUI(Player player, Recipe recipe) {
        this.player = player;
        this.recipe = recipe;
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return getInventory(SoundContext.INITIAL_OPEN);
    }

    @NotNull
    @Override
    public Inventory getInventory(SoundContext context) {
        String title = Chat.colorizewp("&0Yêu Cầu Quyền Hạn | " + recipe.getName());
        Inventory inventory = Bukkit.createInventory(this, 54, title);

        setupItems(inventory);
        return inventory;
    }

    private void setupItems(Inventory inventory) {
        // Add decorative border
        ItemStack borderItem = createBorderItem();
        int[] borderSlots = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53};
        for (int slot : borderSlots) {
            inventory.setItem(slot, borderItem);
        }

        // Display current permissions
        displayCurrentPermissions(inventory);

        // Add common permission suggestions
        addPermissionSuggestions(inventory);

        // Add control buttons
        addControlButtons(inventory);
    }

    private ItemStack createBorderItem() {
        ItemStack item = new ItemStack(XMaterial.BLACK_STAINED_GLASS_PANE.parseMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Chat.colorizewp("&7 "));
            item.setItemMeta(meta);
        }
        return item;
    }

    private void displayCurrentPermissions(Inventory inventory) {
        List<String> currentPerms = recipe.getPermissionRequirements();
        int[] permSlots = {10, 11, 12, 13, 14, 15, 16};

        for (int i = 0; i < Math.min(currentPerms.size(), permSlots.length); i++) {
            String permission = currentPerms.get(i);
            ItemStack permItem = createPermissionItem(permission, true);

            InteractiveItem interactiveItem = new InteractiveItem(permItem, permSlots[i])
                    .onClick((p, clickType) -> handlePermissionClick(p, permission, clickType));
            inventory.setItem(interactiveItem.getSlot(), interactiveItem);
        }

        // Show "No permissions" if empty
        if (currentPerms.isEmpty()) {
            ItemStack noPermsItem = new ItemStack(XMaterial.BARRIER.parseMaterial());
            ItemMeta meta = noPermsItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(Chat.colorizewp("&cKhông Có Yêu Cầu Quyền Hạn"));
                meta.setLore(Arrays.asList(
                        Chat.colorizewp("&7Công thức này không có yêu cầu"),
                        Chat.colorizewp("&7quyền hạn. Ai cũng có thể chế tạo."),
                        Chat.colorizewp("&7"),
                        Chat.colorizewp("&eThêm quyền hạn bên dưới để hạn chế player")
                ));
                noPermsItem.setItemMeta(meta);
            }
            inventory.setItem(13, noPermsItem);
        }
    }

    private void addPermissionSuggestions(Inventory inventory) {
        int[] suggestionSlots = {19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};

        for (int i = 0; i < Math.min(COMMON_PERMISSIONS.length, suggestionSlots.length); i++) {
            String permission = COMMON_PERMISSIONS[i];
            boolean alreadyAdded = recipe.getPermissionRequirements().contains(permission);

            if (!alreadyAdded) {
                ItemStack suggestionItem = createPermissionItem(permission, false);
                InteractiveItem interactiveItem = new InteractiveItem(suggestionItem, suggestionSlots[i])
                        .onLeftClick(p -> addPermission(p, permission));
                inventory.setItem(interactiveItem.getSlot(), interactiveItem);
            }
        }
    }

    private ItemStack createPermissionItem(String permission, boolean isRequired) {
        ItemStack item = new ItemStack(isRequired ? XMaterial.GREEN_WOOL.parseMaterial() : XMaterial.YELLOW_WOOL.parseMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Chat.colorizewp((isRequired ? "&a" : "&e") + permission));
            List<String> lore = new ArrayList<>();

            if (isRequired) {
                lore.add(Chat.colorizewp("&7Trạng thái: &aBắt buộc"));
                lore.add(Chat.colorizewp("&7"));
                lore.add(Chat.colorizewp("&cNhấp phải để xóa"));
            } else {
                lore.add(Chat.colorizewp("&7Trạng thái: &7Gợi ý"));
                lore.add(Chat.colorizewp("&7"));
                lore.add(Chat.colorizewp("&eNhấp trái để thêm"));
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void handlePermissionClick(Player player, String permission, ClickType clickType) {
        if (clickType == ClickType.RIGHT || clickType == ClickType.SHIFT_RIGHT) {
            removePermission(player, permission);
        }
    }

    private void addPermission(Player player, String permission) {
        List<String> permissions = new ArrayList<>(recipe.getPermissionRequirements());
        if (!permissions.contains(permission)) {
            permissions.add(permission);
            recipe.setPermissionRequirements(permissions);
            CraftingManager.updateRecipe(recipe);
            player.sendMessage(Chat.colorize("&aĐã thêm yêu cầu quyền hạn: " + permission));
            refreshGUI(player);
        }
    }

    private void removePermission(Player player, String permission) {
        List<String> permissions = new ArrayList<>(recipe.getPermissionRequirements());
        if (permissions.remove(permission)) {
            recipe.setPermissionRequirements(permissions);
            CraftingManager.updateRecipe(recipe);
            player.sendMessage(Chat.colorize("&cĐã xóa yêu cầu quyền hạn: " + permission));
            refreshGUI(player);
        }
    }

    private void addControlButtons(Inventory inventory) {
        // Add custom permission button
        ItemStack addCustomItem = new ItemStack(XMaterial.EMERALD.parseMaterial());
        ItemMeta meta = addCustomItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Chat.colorizewp("&a&lThêm Quyền Hạn Tùy Chỉnh"));
            meta.setLore(Arrays.asList(
                    Chat.colorizewp("&7Thêm một quyền hạn tùy chỉnh"),
                    Chat.colorizewp("&7không có trong danh sách"),
                    Chat.colorizewp("&7"),
                    Chat.colorizewp("&eNhấp để thêm quyền hạn tùy chỉnh")
            ));
            addCustomItem.setItemMeta(meta);
        }
        InteractiveItem addCustomButton = new InteractiveItem(addCustomItem, 37)
                .onLeftClick(this::requestCustomPermission);
        inventory.setItem(addCustomButton.getSlot(), addCustomButton);

        // Clear all permissions button
        ItemStack clearAllItem = new ItemStack(XMaterial.REDSTONE_BLOCK.parseMaterial());
        meta = clearAllItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Chat.colorizewp("&c&lXóa Tất Cả Quyền Hạn"));
            meta.setLore(Arrays.asList(
                    Chat.colorizewp("&7Xóa tất cả yêu cầu"),
                    Chat.colorizewp("&7quyền hạn khỏi công thức này"),
                    Chat.colorizewp("&7"),
                    Chat.colorizewp("&cNhấp để xóa tất cả")
            ));
            clearAllItem.setItemMeta(meta);
        }
        InteractiveItem clearAllButton = new InteractiveItem(clearAllItem, 43)
                .onLeftClick(this::clearAllPermissions);
        inventory.setItem(clearAllButton.getSlot(), clearAllButton);

        // Back button
        ItemStack backItem = new ItemStack(XMaterial.BARRIER.parseMaterial());
        meta = backItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Chat.colorizewp("&cQuay Lại"));
            meta.setLore(Collections.singletonList(
                    Chat.colorizewp("&7Quay lại trình chỉnh sửa công thức")
            ));
            backItem.setItemMeta(meta);
        }
        InteractiveItem backButton = new InteractiveItem(backItem, 40)
                .onLeftClick(this::returnToRecipeEditor);
        inventory.setItem(backButton.getSlot(), backButton);
    }

    private void requestCustomPermission(Player player) {
        net.danh.storage.Listeners.Chat.chat_recipe_edit_type.put(player, "permission_add");
        net.danh.storage.Listeners.Chat.chat_recipe_id.put(player, recipe.getId());
        player.closeInventory();
        player.sendMessage(Chat.colorize("&eNhập quyền hạn tùy chỉnh trong chat:"));
        player.sendMessage(Chat.colorize("&7Ví dụ: myplugin.special.permission"));
    }

    private void clearAllPermissions(Player player) {
        recipe.setPermissionRequirements(new ArrayList<>());
        CraftingManager.updateRecipe(recipe);
        player.sendMessage(Chat.colorize("&aĐã xóa tất cả yêu cầu quyền hạn"));
        refreshGUI(player);
    }

    private void returnToRecipeEditor(Player player) {
        SoundManager.setShouldPlayCloseSound(player, false);
        player.openInventory(new RecipeEditorGUI(player, recipe).getInventory(SoundContext.SILENT));
    }

    private void refreshGUI(Player player) {
        SoundManager.setShouldPlayCloseSound(player, false);
        player.openInventory(new PermissionRequirementsGUI(player, recipe).getInventory(SoundContext.SILENT));
    }
}
