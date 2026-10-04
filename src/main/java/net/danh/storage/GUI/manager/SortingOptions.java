package net.danh.storage.GUI.manager;

import org.bukkit.configuration.ConfigurationSection;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToIntFunction;

public final class SortingOptions {

    private final Map<Mode, String> labels = new LinkedHashMap<>();
    private final Map<Mode, Boolean> descending = new HashMap<>();
    private final String activePrefix;
    private final String inactivePrefix;
    private Mode mode;
    private boolean reversed;
    public SortingOptions(ConfigurationSection section, SortingOptions previous) {
        activePrefix = section.getString("active-prefix", "");
        inactivePrefix = section.getString("inactive-prefix", "");
        ConfigurationSection modes = section.getConfigurationSection("modes");
        if (modes != null) {
            for (String key : modes.getKeys(false)) {
                Mode candidate = parseMode(key);
                if (candidate == null || !modes.isConfigurationSection(key)) continue;
                labels.put(candidate, modes.getString(key + ".label", key));
                descending.put(candidate, "DESCENDING".equalsIgnoreCase(
                        modes.getString(key + ".direction")));
            }
        }
        mode = parseMode(section.getString("default-mode"));
        if (!labels.containsKey(mode)) {
            mode = labels.isEmpty() ? Mode.NONE : labels.keySet().iterator().next();
        }
        if (previous != null && labels.containsKey(previous.mode)) {
            mode = previous.mode;
            reversed = previous.reversed;
        }
    }

    private static Mode parseMode(String value) {
        if (value == null) return null;
        try {
            return Mode.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static Set<Integer> resolveSlots(ConfigurationSection section,
                                            ConfigurationSection items, int size,
                                            Consumer<String> warning) {
        Set<Integer> slots = new LinkedHashSet<>();
        if (section == null) return slots;
        Map<Integer, String> reserved = new HashMap<>();
        for (String tag : items.getKeys(false)) {
            if (tag.equalsIgnoreCase("sorting_options") || tag.equalsIgnoreCase("decorates")) continue;
            for (String value : items.getString(tag + ".slot", "").split(",")) {
                try {
                    reserved.put(Integer.parseInt(value.trim()), tag);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        String configuredSlots = section.getString("slot", "");
        if (configuredSlots.trim().isEmpty()) {
            warning.accept("Sorting options disabled: items.sorting_options.slot is missing or empty (You can ignore this warning)");
            return slots;
        }
        for (String value : configuredSlots.split(",")) {
            try {
                int slot = Integer.parseInt(value.trim());
                if (slot < 0 || slot >= size) {
                    warning.accept("Ignoring sorting slot " + slot + " in GUI/storage.yml: outside inventory bounds.");
                } else if (reserved.containsKey(slot)) {
                    warning.accept("Ignoring sorting slot " + slot + " in GUI/storage.yml: overlaps items."
                            + reserved.get(slot) + ".slot.");
                } else {
                    slots.add(slot);
                }
            } catch (NumberFormatException ignored) {
                warning.accept("Ignoring invalid sorting slot '" + value.trim() + "' in GUI/storage.yml.");
            }
        }
        return slots;
    }

    public boolean change(int step, boolean reverse) {
        if (labels.isEmpty()) return false;
        if (reverse) {
            if (mode == Mode.NONE) return false;
            reversed = !reversed;
        } else {
            List<Mode> modes = new ArrayList<>(labels.keySet());
            mode = modes.get(Math.floorMod(modes.indexOf(mode) + step, modes.size()));
            reversed = false;
        }
        return true;
    }

    public String[] placeholders() {
        List<String> replacements = new ArrayList<>();
        for (Mode candidate : Mode.values()) {
            replacements.add("#sort_" + candidate.name().toLowerCase(Locale.ROOT) + "#");
            String label = labels.get(candidate);
            replacements.add(label == null ? "" :
                    (candidate == mode ? activePrefix : inactivePrefix) + label);
        }
        return replacements.toArray(new String[0]);
    }

    public <T> void sort(List<T> items, Function<T, String> type,
                         Function<T, String> name, ToIntFunction<T> amount) {
        if (mode == Mode.NONE || items.size() < 2) return;
        Comparator<T> comparator;
        if (mode == Mode.AMOUNT) {
            Map<T, Integer> keys = new HashMap<>();
            for (T item : items) keys.put(item, amount.applyAsInt(item));
            comparator = Comparator.comparingInt(keys::get);
        } else {
            Function<T, String> extractor = mode == Mode.TYPE ? type : name;
            Map<T, String> keys = new HashMap<>();
            for (T item : items) keys.put(item, extractor.apply(item));
            comparator = Comparator.comparing(keys::get, String.CASE_INSENSITIVE_ORDER);
        }
        // Reverse only the comparison; stable sort preserves ties in configuration order.
        if (descending.getOrDefault(mode, false) != reversed) comparator = comparator.reversed();
        items.sort(comparator);
    }

    public enum Mode {TYPE, NAME, AMOUNT, NONE}
}
