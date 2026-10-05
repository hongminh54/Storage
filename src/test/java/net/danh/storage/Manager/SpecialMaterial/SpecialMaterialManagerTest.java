package net.danh.storage.Manager.SpecialMaterial;

import org.bukkit.entity.Player;
import org.bukkit.Location;
import net.danh.storage.API.events.SpecialMaterialDropEvent;
import org.junit.jupiter.api.Test;

import org.mockito.MockMakers;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SpecialMaterialManagerTest {

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
