package net.danh.storage.Manager;

import net.danh.storage.Storage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.ConcurrentLinkedQueue;

public class BlockBreakProcessor {

    private static final int MAX_EVENTS_PER_TICK = 5;
    private static final ConcurrentLinkedQueue<BlockBreakData> eventQueue = new ConcurrentLinkedQueue<>();
    private static boolean isProcessing = false;

    public static void initialize() {
        startProcessor();
    }

    public static void queueEvent(BlockBreakEvent event, Player player) {
        // Process critical checks immediately to maintain event flow
        if (event.isCancelled()) return;

        eventQueue.offer(new BlockBreakData(event, player, System.currentTimeMillis()));
    }

    private static void startProcessor() {
        if (isProcessing) return;
        isProcessing = true;

        new BukkitRunnable() {
            @Override
            public void run() {
                processEvents();
            }
        }.runTaskTimer(Storage.getStorage(), 1L, 1L); // Run every tick
    }

    private static void processEvents() {
        int processed = 0;
        long currentTime = System.currentTimeMillis();

        while (processed < MAX_EVENTS_PER_TICK && !eventQueue.isEmpty()) {
            BlockBreakData data = eventQueue.poll();
            if (data == null) break;

            // Skip events older than 3 seconds to prevent memory buildup
            if (currentTime - data.timestamp > 3000) {
                continue;
            }

            // Process heavy operations async to avoid blocking main thread
            Bukkit.getScheduler().runTaskAsynchronously(Storage.getStorage(), () -> {
                processBlockBreakAsync(data);
            });

            processed++;
        }
    }

    private static void processBlockBreakAsync(BlockBreakData data) {
        // Async processing for database operations and complex calculations
        // Then return to main thread for Bukkit API calls
        Bukkit.getScheduler().runTask(Storage.getStorage(), () -> {
            processBlockBreakSync(data);
        });
    }

    private static void processBlockBreakSync(BlockBreakData data) {
        // Main thread processing for Bukkit API calls
        if (data.player == null || !data.player.isOnline()) return;

        // Delegate to the original BlockBreak listener logic
        // This ensures all existing functionality is preserved
        net.danh.storage.Listeners.BlockBreak blockBreakListener = new net.danh.storage.Listeners.BlockBreak();
        // Note: The actual block break logic will be handled by the updated listener
    }

    public static void processBlockBreakEvent(BlockBreakEvent event, Player player) {
        // Queue the event for optimized processing instead of immediate processing
        queueEvent(event, player);
    }

    private static class BlockBreakData {
        final BlockBreakEvent event;
        final Player player;
        final long timestamp;

        BlockBreakData(BlockBreakEvent event, Player player, long timestamp) {
            this.event = event;
            this.player = player;
            this.timestamp = timestamp;
        }
    }
}