package net.danh.storage.Manager.SpecialMaterial;

import org.bukkit.entity.Player;
import org.bukkit.Location;
import net.danh.storage.API.events.SpecialMaterialDropEvent;
import org.junit.jupiter.api.Test;

import org.mockito.MockMakers;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.configuration.file.YamlConfiguration;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.io.File;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SpecialMaterialManagerTest {

    @Test
    void inventoryDeliveryDropsOnlyReturnedLeftoversAndKeepsTemplate() {
        Player player = player();
        PlayerInventory inventory = mock(PlayerInventory.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        World world = mock(World.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        Location location = new Location(world, 1, 2, 3);
        when(player.getInventory()).thenReturn(inventory);
        when(player.getLocation()).thenReturn(location);
        ItemStack template = mock(ItemStack.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        ItemStack stack = mock(ItemStack.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        when(template.clone()).thenReturn(stack);
        when(stack.getMaxStackSize()).thenReturn(64);
        ItemStack leftover = mock(ItemStack.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        ItemStack drop = mock(ItemStack.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        when(leftover.getAmount()).thenReturn(2);
        when(leftover.getMaxStackSize()).thenReturn(64);
        when(leftover.clone()).thenReturn(drop);
        when(inventory.addItem(stack)).thenReturn(new HashMap<>(Map.of(0, leftover)));
        SpecialMaterialManager.deliverItem(player, location, template, 5, "INVENTORY");
        verify(stack).setAmount(5);
        verify(drop).setAmount(2);
        verify(world).dropItemNaturally(location, drop);
        verify(template, never()).setAmount(anyInt());
    }

    @Test
    void inventoryDeliveryDoesNotDropWhenEverythingFits() {
        Player player = player();
        PlayerInventory inventory = mock(PlayerInventory.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        when(player.getInventory()).thenReturn(inventory);
        ItemStack template = mock(ItemStack.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        ItemStack stack = mock(ItemStack.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        when(template.clone()).thenReturn(stack);
        when(stack.getMaxStackSize()).thenReturn(64);
        when(inventory.addItem(stack)).thenReturn(new HashMap<>());
        SpecialMaterialManager.deliverItem(player, null, template, 3, "INVENTORY");
        verify(inventory).addItem(stack);
        verify(player, never()).getLocation();
    }

    @Test
    void deliveryDefaultsToDropAndRewardAmountsStayDistinct() {
        YamlConfiguration config = new YamlConfiguration();
        assertEquals("DROP", SpecialMaterialManager.loadDelivery(config, "test"));
        config.set("delivery", "inventory");
        assertEquals("INVENTORY", SpecialMaterialManager.loadDelivery(config, "test"));
        assertEquals("1/16/3", SpecialMaterialManager.replacePlaceholders(
                "#special_amount#/#reward_amount#/#loot_amount#",
                Map.of("#special_amount#", "1", "#reward_amount#", "16", "#loot_amount#", "3")));
    }

    @Test
    void defaultDropDeliveryNeverTouchesInventory() {
        Player player = player();
        World world = mock(World.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        Location location = new Location(world, 1, 2, 3);
        ItemStack template = mock(ItemStack.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        ItemStack drop = mock(ItemStack.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        when(template.getMaxStackSize()).thenReturn(64);
        when(template.clone()).thenReturn(drop);
        SpecialMaterialManager.deliverItem(player, location, template, 65, "DROP");
        verify(world, times(2)).dropItemNaturally(location, drop);
        verify(drop).setAmount(64);
        verify(drop).setAmount(1);
        verify(player, never()).getInventory();
    }

    @Test
    void bundledExamplesDescribeMainItemsAndSeparateBonuses() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(new File("src/main/resources/special_material.yml"));
        String farmer = "special_materials.farmer_blessing.";
        assertEquals("GOLDEN_CARROT", config.getString(farmer + "item.material"));
        assertEquals("INVENTORY", config.getString(farmer + "delivery"));
        assertEquals(16, config.getInt(farmer + "rewards.crop.amount"));
        assertTrue(config.getString(farmer + "messages.storage-reward").contains("#reward_amount#"));
        assertFalse(config.getString(farmer + "effects.actionbar.message").contains("16 Wheat stored"));
        String beast = "special_materials.beast_core.";
        assertEquals("BLAZE_ROD", config.getString(beast + "item.material"));
        assertEquals("DROP", config.getString(beast + "delivery"));
        assertEquals(3, config.getConfigurationSection(beast + "loot_table").getKeys(false).size());
        assertTrue(config.getString(beast + "messages.loot-reward").contains("#loot_item#"));
    }

    private Player player() {
        // JDK proxies initialize every method's signature types, including Bukkit registries.
        // A subclass mock avoids requiring a running server or an inline Java agent.
        Player player = mock(Player.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        return player;
    }

    @Test
    void chanceBoundariesRejectZeroAndInvalidValues() {
        assertFalse(SpecialMaterialManager.passesChance(0, 0));
        assertFalse(SpecialMaterialManager.passesChance(-1, 0));
        assertFalse(SpecialMaterialManager.passesChance(Double.NaN, 0));
        assertFalse(SpecialMaterialManager.passesChance(Double.POSITIVE_INFINITY, 0));
        assertTrue(SpecialMaterialManager.passesChance(100, 99.999));
        assertTrue(SpecialMaterialManager.passesChance(150, 99.999));
        assertFalse(SpecialMaterialManager.passesChance(25, 25));
    }

    @Test
    void inclusiveRandomAmountDoesNotOverflow() {
        assertEquals(1, SpecialMaterialManager.randomAmount(1, 1));
        assertEquals(Integer.MAX_VALUE, SpecialMaterialManager.randomAmount(Integer.MAX_VALUE, Integer.MAX_VALUE));
        for (int i = 0; i < 100; i++) {
            int amount = SpecialMaterialManager.randomAmount(2, 4);
            assertTrue(amount >= 2 && amount <= 4);
        }
    }

    @Test
    void eventKeepsSourceSnapshotAndAllowsCancellationAndEnchantOverride() {
        Location location = new Location(null, 1, 2, 3);
        SpecialMaterialDropEvent event = new SpecialMaterialDropEvent(player(), null, "tnt",
                "block", "DIAMOND_ORE;0", location, false);
        location.setX(99);
        assertEquals(1, event.getLocation().getX());
        event.getLocation().setX(50);
        assertEquals(1, event.getLocation().getX());
        assertFalse(event.isStored());
        assertEquals("DIAMOND_ORE;0", event.getSourceKey());
        event.setEnchantType("veinminer");
        assertEquals("veinminer", event.getEnchantType());
        event.setCancelled(true);
        assertTrue(event.isCancelled());
    }

    @Test
    void limitsSurviveRepeatedStorageSnapshotReplacement() {
        Player player = player();
        try {
            SpecialMaterialManager.loadPlayerData(player, "old;spmat:ruby:3:2026-10-05");
            String saved = SpecialMaterialManager.mergePlayerData(player, "new", "old");
            assertEquals("new;spmat:ruby:3:2026-10-05", saved);
            assertEquals("next;spmat:ruby:3:2026-10-05",
                    SpecialMaterialManager.mergePlayerData(player, "next", saved));
        } finally {
            SpecialMaterialManager.cleanupPlayerData(player);
        }
    }

    @Test
    void firstSavePreservesUncachedLimitsAndIgnoresMalformedEntries() {
        Player player = player();
        try {
            String saved = SpecialMaterialManager.mergePlayerData(player, "blocks",
                    "old;spmat:ruby:2:2026-10-05;spmat:bad:abc:2026-10-05;spmat:date:1:invalid");
            assertEquals("blocks;spmat:ruby:2:2026-10-05", saved);
        } finally {
            SpecialMaterialManager.cleanupPlayerData(player);
        }
    }

    @Test
    void mergingLimitsPreservesOtherStorageSectionsAndLegacyBlockData() {
        Player player = player();
        try {
            String storage = "{STONE;0=20, WOOL;14=5};crop:WHEAT:12;mob:BONE:3;toggle:true";
            SpecialMaterialManager.loadPlayerData(player, "spmat:ruby:2:2026-10-05");
            assertEquals(storage + ";spmat:ruby:2:2026-10-05",
                    SpecialMaterialManager.mergePlayerData(player, storage, null));
        } finally {
            SpecialMaterialManager.cleanupPlayerData(player);
        }
    }
}
