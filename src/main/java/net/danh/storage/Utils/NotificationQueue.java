package net.danh.storage.Utils;

import com.cryptomorin.xseries.messages.ActionBar;
import com.cryptomorin.xseries.messages.Titles;
import net.danh.storage.Storage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class NotificationQueue {

    public static final String TYPE_STORAGE = "storage";
    public static final String TYPE_MYTHIC = "mythic";
    public static final String TYPE_CROP = "crop";
    public static final String TYPE_MOB = "mob";

    // Flush this long after the last pickup (~2 ticks, imperceptible)
    private static final long QUIET_MILLIS = 100L;

    private static final Map<UUID, Batch> BATCHES = new ConcurrentHashMap<>();
    private static final Map<String, Settings> SETTINGS = new ConcurrentHashMap<>();

    private NotificationQueue() {
    }

    public static void reload() {
        SETTINGS.clear();
    }

    public static void add(@NotNull Player player, @NotNull String type, @NotNull String itemKey,
                           @NotNull String itemName, int amount, @NotNull String amountSuffix,
                           int storage, int max, boolean actionBar, boolean title) {
        if (!actionBar && !title) {
            return;
        }

        Settings settings = settings(type);
        if (!settings.enabled) {
            Batch batch = new Batch(player, type, actionBar, title, settings);
            batch.add(itemKey, itemName, amount, amountSuffix, storage, max);
            batch.send();
            return;
        }

        UUID id = player.getUniqueId();
        Batch current = BATCHES.get(id);
        if (current != null && !current.type.equals(type)) {
            flush(id);
            current = null;
        }
        if (current == null) {
            current = new Batch(player, type, actionBar, title, settings);
            BATCHES.put(id, current);
            schedule(id, current);
        }
        if (!current.add(itemKey, itemName, amount, amountSuffix, storage, max)) {
            flush(id);
            current = new Batch(player, type, actionBar, title, settings);
            BATCHES.put(id, current);
            schedule(id, current);
            current.add(itemKey, itemName, amount, amountSuffix, storage, max);
        }
    }

    public static void cleanup(@NotNull Player player) {
        Batch batch = BATCHES.remove(player.getUniqueId());
        if (batch != null) {
            batch.cancelTask();
        }
    }

    public static void shutdown() {
        for (Batch batch : BATCHES.values()) {
            batch.cancelTask();
        }
        BATCHES.clear();
    }

    private static void flush(@NotNull UUID id) {
        Batch batch = BATCHES.remove(id);
        if (batch != null) {
            batch.cancelTask();
            batch.send();
        }
    }

    private static void schedule(@NotNull UUID id, @NotNull Batch batch) {
        long now = System.currentTimeMillis();
        long next = nextFlushMillis(batch, now);
        long delay = Math.max(1, (next - now + 49) / 50);
        batch.task = TaskWrapper.runTaskLater(Storage.getStorage(), () -> {
            if (BATCHES.get(id) != batch) {
                return;
            }
            if (nextFlushMillis(batch, System.currentTimeMillis()) <= System.currentTimeMillis()) {
                BATCHES.remove(id);
                batch.send();
            } else {
                schedule(id, batch);
            }
        }, delay);
    }

    // Flush when pickups have been quiet for QUIET_MILLIS, or once the configurable
    // deadline since the first pickup is reached (0 = no deadline, quiet decides).
    private static long nextFlushMillis(@NotNull Batch batch, long now) {
        long deadline = batch.settings.durationMillis > 0
                ? batch.firstPickupMillis + batch.settings.durationMillis
                : Long.MAX_VALUE;
        return Math.min(deadline, batch.lastPickupMillis + QUIET_MILLIS);
    }

    @NotNull
    private static Settings settings(@NotNull String type) {
        return SETTINGS.computeIfAbsent(type, NotificationQueue::loadSettings);
    }

    @NotNull
    private static Settings loadSettings(@NotNull String type) {
        String path = combinePath(type);
        FileConfiguration cfg = config(type);
        String separator = cfg.getString(path + ".separator", " ");
        return new Settings(
                cfg.getBoolean(path + ".enable", true),
                Math.max(1, cfg.getInt(path + ".max_items", 3)),
                Math.max(0, cfg.getInt(path + ".duration_seconds", 0)) * 1000L,
                separator != null ? separator : " ");
    }

    @NotNull
    private static FileConfiguration config(@NotNull String type) {
        switch (type) {
            case TYPE_STORAGE:
                return File.getConfig();
            case TYPE_MYTHIC:
                return File.getMythicStorageConfig();
            case TYPE_CROP:
                return File.getCropStorageConfig();
            default:
                return File.getMobStorageConfig();
        }
    }

    private static String combinePath(@NotNull String type) {
        return TYPE_STORAGE.equals(type) ? "mine.combine" : "notification.combine";
    }

    @Nullable
    private static String actionBarTemplate(@NotNull String type) {
        return TYPE_STORAGE.equals(type)
                ? config(type).getString("mine.actionbar.action")
                : config(type).getString("notification.actionbar.item_added");
    }

    @Nullable
    private static String titleTemplate(@NotNull String type) {
        return TYPE_STORAGE.equals(type)
                ? config(type).getString("mine.title.title")
                : config(type).getString("notification.title.item_added.title");
    }

    @Nullable
    private static String subtitleTemplate(@NotNull String type) {
        return TYPE_STORAGE.equals(type)
                ? config(type).getString("mine.title.subtitle")
                : config(type).getString("notification.title.item_added.subtitle");
    }

    private static String render(@NotNull String template, @NotNull Entry entry) {
        return template
                .replace("#amount#", entry.amount + entry.amountSuffix)
                .replace("#item#", entry.itemName)
                .replace("#storage#", String.valueOf(entry.storage))
                .replace("#max#", String.valueOf(entry.max));
    }

    private static String join(@NotNull Map<String, Entry> entries, @NotNull String template,
                               @NotNull String separator) {
        return entries.values().stream()
                .map(entry -> render(template, entry))
                .collect(Collectors.joining(separator));
    }

    private record Settings(boolean enabled, int maxItems, long durationMillis, String separator) {

    }

    private static final class Batch {

        private final Player player;
        private final String type;
        private final boolean actionBar;
        private final boolean title;
        private final Settings settings;
        private final Map<String, Entry> entries = new LinkedHashMap<>();
        private long firstPickupMillis;
        private long lastPickupMillis;
        private TaskWrapper task;

        private Batch(@NotNull Player player, @NotNull String type,
                      boolean actionBar, boolean title, @NotNull Settings settings) {
            this.player = player;
            this.type = type;
            this.actionBar = actionBar;
            this.title = title;
            this.settings = settings;
        }

        private synchronized boolean add(@NotNull String itemKey, @NotNull String itemName,
                                         int amount, @NotNull String amountSuffix,
                                         int storage, int max) {
            long now = System.currentTimeMillis();
            if (firstPickupMillis == 0L) {
                firstPickupMillis = now;
            }
            lastPickupMillis = now;
            Entry entry = entries.get(itemKey);
            if (entry != null) {
                entry.amount += amount;
                entry.amountSuffix = amountSuffix;
                entry.storage = storage;
                entry.max = max;
                return true;
            }
            if (entries.size() >= settings.maxItems) {
                return false;
            }
            entries.put(itemKey, new Entry(itemName, amount, amountSuffix, storage, max));
            return true;
        }

        private void cancelTask() {
            if (task != null) {
                task.cancel();
            }
        }

        private synchronized void send() {
            if (player == null || !player.isOnline() || entries.isEmpty()) {
                return;
            }
            String separator = settings.separator;
            if (actionBar) {
                String template = actionBarTemplate(type);
                if (template != null && !template.isEmpty()) {
                    ActionBar.sendActionBar(Storage.getStorage(), player,
                            ChatUtils.colorizewp(join(entries, template, separator)));
                }
            }
            if (title) {
                String titleTemplate = titleTemplate(type);
                String subtitleTemplate = subtitleTemplate(type);
                if (titleTemplate != null && subtitleTemplate != null) {
                    Titles.sendTitle(player,
                            ChatUtils.colorizewp(join(entries, titleTemplate, separator)),
                            ChatUtils.colorizewp(join(entries, subtitleTemplate, separator)));
                }
            }
        }
    }

    private static final class Entry {

        private final String itemName;
        private int amount;
        private String amountSuffix;
        private int storage;
        private int max;

        private Entry(@NotNull String itemName, int amount, @NotNull String amountSuffix,
                      int storage, int max) {
            this.itemName = itemName;
            this.amount = amount;
            this.amountSuffix = amountSuffix;
            this.storage = storage;
            this.max = max;
        }
    }
}
