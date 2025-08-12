package net.danh.storage.GUI;

import net.danh.storage.Action.ConvertOre;
import net.danh.storage.GUI.manager.IGUI;
import net.danh.storage.GUI.manager.InteractiveItem;
import net.danh.storage.Manager.ConvertOreManager;
import net.danh.storage.Manager.ItemManager;
import net.danh.storage.Manager.MineManager;
import net.danh.storage.Manager.SoundManager;
import net.danh.storage.Utils.Chat;
import net.danh.storage.Utils.File;
import net.danh.storage.Utils.SoundContext;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ConvertOptionGUI implements IGUI {

    private final Player player;
    private final String material;
    private final int returnPage;
    private final FileConfiguration config;

    public ConvertOptionGUI(Player player, String material, int returnPage) {
        this.player = player;
        this.material = material;
        this.returnPage = returnPage;
        this.config = File.getConvertOreConfig();
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return getInventory(SoundContext.INITIAL_OPEN);
    }

    @NotNull
    @Override
    public Inventory getInventory(SoundContext context) {
        if (!player.hasPermission("storage.convert")) {
            player.sendMessage(Chat.colorize(Objects.requireNonNull(File.getMessage().getString("admin.no_permission"))));
            return Bukkit.createInventory(this, 9, "No Permission");
        }

        SoundManager.playItemSound(player, config, "gui_open_sound", context);

        String materialName = File.getConfig().getString("items." + material, material.split(";")[0]);
        String title = Chat.colorizewp(Objects.requireNonNull(config.getString("option_title"))
                .replace("#player#", player.getName())
                .replace("#material#", materialName));

        Inventory inventory = Bukkit.createInventory(this, config.getInt("option_size") * 9, title);

        List<ConvertOreManager.ConvertOption> options = ConvertOreManager.getConvertOptions(material);
        int playerAmount = MineManager.getPlayerBlock(player, material);

        setupItems(inventory, options, materialName, playerAmount);

        return inventory;
    }

    private void setupItems(Inventory inventory, List<ConvertOreManager.ConvertOption> options, String materialName, int playerAmount) {
        ConfigurationSection itemsSection = config.getConfigurationSection("option_items");
        if (itemsSection == null) return;

        for (String itemTag : itemsSection.getKeys(false)) {
            ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemTag);
            if (itemSection == null) continue;

            String slotConfig = itemSection.getString("slot");
            if (slotConfig == null) continue;

            switch (itemTag.toLowerCase()) {
                case "convert_option":
                    if (!options.isEmpty()) {
                        setupConvertOptions(inventory, options, slotConfig, playerAmount);
                    }
                    break;
                case "material_display":
                    setupMaterialDisplay(inventory, itemSection, slotConfig, materialName, playerAmount);
                    break;
                case "decorates":
                    setupDecorativeItems(inventory, itemSection, slotConfig);
                    break;
                default:
                    setupStaticItem(inventory, itemSection, itemTag, slotConfig);
                    break;
            }
        }
    }

    private void setupConvertOptions(Inventory inventory, List<ConvertOreManager.ConvertOption> options, String slotConfig, int playerAmount) {
        List<Integer> availableSlots = parseSlots(slotConfig);
        int slotsUsed = Math.min(options.size(), availableSlots.size());

        for (int i = 0; i < slotsUsed; i++) {
            ConvertOreManager.ConvertOption option = options.get(i);
            int slot = availableSlots.get(i);
            createConvertOptionItem(inventory, option, slot, playerAmount);
        }
    }

    private void createConvertOptionItem(Inventory inventory, ConvertOreManager.ConvertOption option, int slot, int playerAmount) {
        int maxConversions = option.calculateMaxConversions(playerAmount);
        int resultAmount = option.calculateResultAmount(maxConversions);

        String toMaterialName = getMaterialDisplayName(option.getToMaterial());
        String fromMaterialName = getMaterialDisplayName(option.getFromMaterial());

        ConfigurationSection section = config.getConfigurationSection("option_items.convert_option");
        ItemStack itemStack = ItemManager.getItemConfigWithPlaceholders(player, section,
                "#from_amount#", String.valueOf(option.getFromAmount()),
                "#from_material#", fromMaterialName,
                "#to_amount#", String.valueOf(option.getToAmount()),
                "#to_material#", toMaterialName,
                "#max_conversions#", String.valueOf(maxConversions),
                "#result_amount#", String.valueOf(resultAmount));

        if (itemStack != null) {
            try {
                itemStack.setType(org.bukkit.Material.valueOf(option.getToMaterial().split(";")[0]));
            } catch (IllegalArgumentException e) {
                itemStack.setType(org.bukkit.Material.STONE);
            }
        }

        InteractiveItem interactiveItem = new InteractiveItem(itemStack, slot)
                .onLeftClick((clickPlayer) -> handleConvertClick(clickPlayer, option, maxConversions, true))
                .onRightClick((clickPlayer) -> handleConvertClick(clickPlayer, option, maxConversions, false));

        inventory.setItem(slot, interactiveItem);
    }

    private void handleConvertClick(Player clickPlayer, ConvertOreManager.ConvertOption option, int maxConversions, boolean isLeftClick) {
        if (maxConversions <= 0) {
            SoundManager.playErrorSound(clickPlayer);
            int playerAmount = MineManager.getPlayerBlock(clickPlayer, option.getFromMaterial());
            sendMessage(clickPlayer, "convert.insufficient_materials",
                    "#required#", String.valueOf(option.getFromAmount()),
                    "#current#", String.valueOf(playerAmount),
                    "#material#", getMaterialName(option.getFromMaterial()));
            return;
        }

        playItemSound(clickPlayer, "convert_option");

        if (isLeftClick) {
            new ConvertOre(clickPlayer, option.getFromMaterial(), option.getToMaterial(), maxConversions).doAction();
            SoundManager.setShouldPlayCloseSound(clickPlayer, false);
            clickPlayer.openInventory(new ConvertOptionGUI(clickPlayer, material, returnPage).getInventory(SoundContext.SILENT));
        } else {
            net.danh.storage.Listeners.Chat.chat_convert_from.put(clickPlayer, option.getFromMaterial());
            net.danh.storage.Listeners.Chat.chat_convert_to.put(clickPlayer, option.getToMaterial());
            net.danh.storage.Listeners.Chat.chat_return_page.put(clickPlayer, returnPage);
            clickPlayer.sendMessage(Chat.colorize(File.getMessage().getString("convert.chat_amount")));
            SoundManager.setShouldPlayCloseSound(clickPlayer, false);
            clickPlayer.closeInventory();
        }
    }

    private List<Integer> parseSlots(String slotConfig) {
        List<Integer> slots = new ArrayList<>();
        String[] slotStrings = slotConfig.replace(" ", "").split(",");

        for (String slotString : slotStrings) {
            try {
                slots.add(Integer.parseInt(slotString.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return slots;
    }

    private String getMaterialDisplayName(String material) {
        return File.getConfig().getString("items." + material, material.split(";")[0]);
    }

    private void setupMaterialDisplay(Inventory inventory, ConfigurationSection section, String slotConfig, String materialName, int playerAmount) {
        List<Integer> slots = parseSlots(slotConfig);
        if (slots.isEmpty()) return;

        ItemStack itemStack = ItemManager.getItemConfigWithPlaceholders(player, section,
                "#material#", materialName,
                "#amount#", String.valueOf(playerAmount),
                "#max_storage#", String.valueOf(MineManager.getMaxBlock(player)));

        if (itemStack != null) {
            try {
                itemStack.setType(org.bukkit.Material.valueOf(material.split(";")[0]));
            } catch (IllegalArgumentException e) {
                itemStack.setType(org.bukkit.Material.STONE);
            }
        }

        for (int slot : slots) {
            InteractiveItem interactiveItem = new InteractiveItem(itemStack.clone(), slot);
            inventory.setItem(slot, interactiveItem);
        }
    }

    private void setupDecorativeItems(Inventory inventory, ConfigurationSection section, String slotConfig) {
        List<Integer> slots = parseSlots(slotConfig);
        ItemStack baseItem = ItemManager.getItemConfig(player, section);

        for (int slot : slots) {
            InteractiveItem interactiveItem = new InteractiveItem(baseItem.clone(), slot);
            inventory.setItem(slot, interactiveItem);
        }
    }

    private void setupStaticItem(Inventory inventory, ConfigurationSection section, String itemTag, String slotConfig) {
        List<Integer> slots = parseSlots(slotConfig);
        ItemStack baseItem = ItemManager.getItemConfig(player, section);

        for (int slot : slots) {
            InteractiveItem interactiveItem = new InteractiveItem(baseItem.clone(), slot)
                    .onClick((clickPlayer, clickType) -> handleStaticItemClick(clickPlayer, itemTag));
            inventory.setItem(slot, interactiveItem);
        }
    }

    private void handleStaticItemClick(Player clickPlayer, String itemTag) {
        switch (itemTag.toLowerCase()) {
            case "back":
                playItemSound(clickPlayer, itemTag);
                SoundManager.setShouldPlayCloseSound(clickPlayer, false);
                clickPlayer.openInventory(new ConvertOreGUI(clickPlayer, returnPage).getInventory(SoundContext.SILENT));
                break;
            default:
                break;
        }
    }

    private void playItemSound(Player player, String itemTag) {
        ConfigurationSection soundSection = config.getConfigurationSection("option_items." + itemTag + ".sound");
        if (soundSection != null && soundSection.getBoolean("enabled", false)) {
            String soundName = soundSection.getString("name", "UI_BUTTON_CLICK");
            float volume = (float) soundSection.getDouble("volume", 0.8);
            float pitch = (float) soundSection.getDouble("pitch", 1.0);
            SoundManager.playSound(player, soundName, volume, pitch);
        }
    }

    private String getMaterialName(String material) {
        return Objects.requireNonNull(File.getConfig().getString("items." + material, material.split(";")[0]));
    }

    private void sendMessage(Player player, String key, String... replacements) {
        String message = File.getMessage().getString(key);
        if (message != null) {
            for (int i = 0; i < replacements.length - 1; i += 2) {
                message = message.replace(replacements[i], replacements[i + 1]);
            }
            player.sendMessage(Chat.colorize(message));
        }
    }
}
