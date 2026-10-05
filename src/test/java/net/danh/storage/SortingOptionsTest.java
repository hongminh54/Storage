package net.danh.storage;

import net.danh.storage.GUI.manager.SortingOptions;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.inventory.ClickType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Collections;
import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class SortingOptionsTest {

    @Test
    void storageConfigurationsMatchTheUpdatedPersonalTemplate() {
        YamlConfiguration personal = YamlConfiguration.loadConfiguration(new File("src/main/resources/GUI/storage.yml"));
        ConfigurationSection template = personal.getConfigurationSection("items.sorting_options");
        assertNotNull(template);
        for (String file : Arrays.asList("mobstorage", "mythicstorage", "cropstorage")) {
            YamlConfiguration gui = YamlConfiguration.loadConfiguration(new File("src/main/resources/GUI/" + file + ".yml"));
            ConfigurationSection section = gui.getConfigurationSection("items.sorting_options");
            assertNotNull(section);
            assertEquals(template.getKeys(true), section.getKeys(true));
            for (String key : template.getKeys(true)) {
                if (!template.isConfigurationSection(key)) assertEquals(template.get(key), section.get(key));
            }
            assertEquals(Collections.singleton(4), SortingOptions.resolveSlots(section,
                    gui.getConfigurationSection("items"), 54, "GUI/" + file + ".yml", warning -> fail(warning)));
        }
    }

    @Test
    void warningsIdentifyEachStorageFileAndItemKey() {
        for (String type : Arrays.asList("mob", "mythic", "crop")) {
            YamlConfiguration items = new YamlConfiguration();
            items.set(type + "_item.slot", "10,11");
            ConfigurationSection sorting = items.createSection("sorting_options");
            sorting.set("slot", "10");
            List<String> warnings = new ArrayList<>();
            assertTrue(SortingOptions.resolveSlots(sorting, items, 54,
                    "GUI/" + type + "storage.yml", warnings::add).isEmpty());
            assertTrue(warnings.get(0).contains("GUI/" + type + "storage.yml"));
            assertTrue(warnings.get(0).contains(type + "_item"));
        }
    }

    @Test
    void clicksAreSharedAndSelectionsRemainIndependent() {
        SortingOptions mob = new SortingOptions(config("NONE"), null);
        SortingOptions crop = new SortingOptions(config("NONE"), null);
        assertTrue(mob.change(ClickType.LEFT));
        assertEquals(SortingOptions.Mode.TYPE, mob.getMode());
        assertEquals(SortingOptions.Mode.NONE, crop.getMode());
        assertTrue(mob.change(ClickType.SHIFT_LEFT));
        assertFalse(mob.change(ClickType.SHIFT_RIGHT));
        assertFalse(mob.change(ClickType.DROP));
        assertTrue(mob.change(ClickType.RIGHT));
        assertEquals(SortingOptions.Mode.NONE, mob.getMode());
    }

    @Test
    void optionalButtonCanBeAbsentOrMissingSlots() {
        YamlConfiguration items = new YamlConfiguration();
        List<String> warnings = new ArrayList<>();
        assertTrue(SortingOptions.resolveSlots(null, items, 54, warnings::add).isEmpty());
        assertTrue(warnings.isEmpty());
        ConfigurationSection sorting = items.createSection("sorting_options");
        assertTrue(SortingOptions.resolveSlots(sorting, items, 54, warnings::add).isEmpty());
        assertEquals(1, warnings.size());
    }

    @Test
    void sortingReplacesDecorationButProtectsStorageAndControls() {
        YamlConfiguration items = new YamlConfiguration();
        items.set("decorates.slot", "4, 5");
        items.set("storage_item.slot", "10, 11");
        items.set("next_page.slot", "50");
        ConfigurationSection sorting = items.createSection("sorting_options");
        sorting.set("slot", "4, 10, 50, 54, invalid, 4");
        List<String> warnings = new ArrayList<>();
        assertEquals(Collections.singleton(4),
                SortingOptions.resolveSlots(sorting, items, 54, warnings::add));
        assertEquals(4, warnings.size());
        assertTrue(warnings.get(0).contains("storage_item"));
        assertEquals("4, 5", items.getString("decorates.slot"));
        assertEquals("10, 11", items.getString("storage_item.slot"));
    }

    private ConfigurationSection config(String defaultMode) {
        YamlConfiguration config = new YamlConfiguration();
        config.set("default-mode", defaultMode);
        config.set("active-prefix", "&a→ ");
        config.set("inactive-prefix", "&7");
        for (String mode : Arrays.asList("TYPE", "NAME", "AMOUNT", "NONE")) {
            config.set("modes." + mode + ".label", mode);
            config.set("modes." + mode + ".direction",
                    mode.equals("AMOUNT") ? "DESCENDING" : "ASCENDING");
        }
        return config;
    }

    @Test
    void nonePreservesOrderAndCyclesBothWays() {
        SortingOptions options = new SortingOptions(config("NONE"), null);
        List<String> items = new ArrayList<>(Arrays.asList("B", "A"));
        options.sort(items, key -> key, key -> key, key -> 0);
        assertEquals(Arrays.asList("B", "A"), items);
        assertFalse(options.change(0, true));
        options.change(1, false);
        assertEquals("&a→ TYPE", options.placeholders()[1]);
        options.change(-1, false);
        assertEquals("&a→ NONE", options.placeholders()[7]);
    }

    @Test
    void amountIsDescendingAndReversePreservesTies() {
        SortingOptions options = new SortingOptions(config("AMOUNT"), null);
        List<String> items = new ArrayList<>(Arrays.asList("A", "B", "C"));
        options.sort(items, key -> key, key -> key, key -> key.equals("B") ? Integer.MAX_VALUE : 0);
        assertEquals(Arrays.asList("B", "A", "C"), items);
        options.change(0, true);
        options.sort(items, key -> key, key -> key, key -> key.equals("B") ? Integer.MAX_VALUE : 0);
        assertEquals(Arrays.asList("A", "C", "B"), items);
    }

    @Test
    void nameKeysArePreparedOnceAndComparedIgnoringCase() {
        SortingOptions options = new SortingOptions(config("NAME"), null);
        List<String> items = new ArrayList<>(Arrays.asList("B", "A", "C"));
        int[] calls = {0};
        options.sort(items, key -> key, key -> {
            calls[0]++;
            return key.equals("B") ? "zebra" : "Apple";
        }, key -> 0);
        assertEquals(3, calls[0]);
        assertEquals(Arrays.asList("A", "C", "B"), items);
    }

    @Test
    void reconstructedOptionsPreserveSelectionAndReverse() {
        SortingOptions previous = new SortingOptions(config("TYPE"), null);
        previous.change(0, true);
        SortingOptions options = new SortingOptions(config("NONE"), previous);
        List<String> items = new ArrayList<>(Arrays.asList("A", "B"));
        options.sort(items, key -> key, key -> key, key -> 0);
        assertEquals(Arrays.asList("B", "A"), items);
        assertEquals("&a→ TYPE", options.placeholders()[1]);
    }

    @Test
    void invalidAndRemovedModesFallBackWithoutCrashing() {
        ConfigurationSection section = config("INVALID");
        section.set("modes.TYPE", null);
        section.set("modes.UNSUPPORTED.label", "Ignored");
        SortingOptions options = new SortingOptions(section,
                new SortingOptions(config("TYPE"), null));
        assertEquals("&a→ NAME", options.placeholders()[3]);
        assertEquals("", options.placeholders()[1]);
        section.set("modes", null);
        options = new SortingOptions(section, options);
        assertFalse(options.change(1, false));
    }
}
