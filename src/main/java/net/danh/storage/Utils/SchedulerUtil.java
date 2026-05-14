package net.danh.storage.Utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.util.function.Consumer;

public class SchedulerUtil {

    private static final boolean IS_FOLIA;

    static {
        boolean folia;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        } catch (ClassNotFoundException e) {
            folia = false;
        }
        IS_FOLIA = folia;
    }

    public static boolean isFolia() {
        return IS_FOLIA;
    }

    public static void runTask(Plugin plugin, Runnable task) {
        if (IS_FOLIA) {
            try {
                runGlobalTask(plugin, task);
            } catch (Exception e) {
                throw scheduleException(plugin, "task", e);
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    public static void runTask(Plugin plugin, Entity entity, Runnable task) {
        if (IS_FOLIA && entity != null) {
            try {
                runEntityTask(plugin, entity, task);
                return;
            } catch (Exception e) {
                throw scheduleException(plugin, "entity task", e);
            }
        }
        runTask(plugin, task);
    }

    public static void runTask(Plugin plugin, Location location, Runnable task) {
        if (IS_FOLIA && location != null && location.getWorld() != null) {
            try {
                runRegionTask(plugin, location, task);
                return;
            } catch (Exception e) {
                throw scheduleException(plugin, "region task", e);
            }
        }
        runTask(plugin, task);
    }

    public static void runTaskLater(Plugin plugin, Runnable task, long delayTicks) {
        if (IS_FOLIA) {
            try {
                runGlobalTaskLater(plugin, task, delayTicks);
            } catch (Exception e) {
                throw scheduleException(plugin, "delayed task", e);
            }
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        }
    }

    public static void runTaskLater(Plugin plugin, Entity entity, Runnable task, long delayTicks) {
        if (IS_FOLIA && entity != null) {
            try {
                runEntityTaskLater(plugin, entity, task, delayTicks);
                return;
            } catch (Exception e) {
                throw scheduleException(plugin, "delayed entity task", e);
            }
        }
        runTaskLater(plugin, task, delayTicks);
    }

    public static void runTaskLater(Plugin plugin, Location location, Runnable task, long delayTicks) {
        if (IS_FOLIA && location != null && location.getWorld() != null) {
            try {
                runRegionTaskLater(plugin, location, task, delayTicks);
                return;
            } catch (Exception e) {
                throw scheduleException(plugin, "delayed region task", e);
            }
        }
        runTaskLater(plugin, task, delayTicks);
    }

    public static void runTaskAsynchronously(Plugin plugin, Runnable task) {
        if (IS_FOLIA) {
            try {
                Object asyncScheduler = Bukkit.class.getMethod("getAsyncScheduler").invoke(null);
                asyncScheduler.getClass().getMethod("runNow", Plugin.class, Consumer.class)
                        .invoke(asyncScheduler, plugin, toConsumer(task));
            } catch (Exception e) {
                throw scheduleException(plugin, "async task", e);
            }
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        }
    }

    static Object runGlobalTask(Plugin plugin, Runnable task) throws Exception {
        Object scheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
        return scheduler.getClass().getMethod("run", Plugin.class, Consumer.class)
                .invoke(scheduler, plugin, toConsumer(task));
    }

    static Object runGlobalTaskLater(Plugin plugin, Runnable task, long delayTicks) throws Exception {
        Object scheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
        return scheduler.getClass().getMethod("runDelayed", Plugin.class, Consumer.class, long.class)
                .invoke(scheduler, plugin, toConsumer(task), normalizeTicks(delayTicks));
    }

    static Object runGlobalTaskTimer(Plugin plugin, Runnable task, long delayTicks, long periodTicks) throws Exception {
        Object scheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
        return scheduler.getClass().getMethod("runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class)
                .invoke(scheduler, plugin, toConsumer(task), normalizeTicks(delayTicks), normalizeTicks(periodTicks));
    }

    private static void runEntityTask(Plugin plugin, Entity entity, Runnable task) throws Exception {
        Object scheduler = entity.getClass().getMethod("getScheduler").invoke(entity);
        scheduler.getClass().getMethod("run", Plugin.class, Consumer.class, Runnable.class)
                .invoke(scheduler, plugin, toConsumer(task), null);
    }

    private static void runEntityTaskLater(Plugin plugin, Entity entity, Runnable task, long delayTicks) throws Exception {
        Object scheduler = entity.getClass().getMethod("getScheduler").invoke(entity);
        scheduler.getClass().getMethod("runDelayed", Plugin.class, Consumer.class, Runnable.class, long.class)
                .invoke(scheduler, plugin, toConsumer(task), null, normalizeTicks(delayTicks));
    }

    private static void runRegionTask(Plugin plugin, Location location, Runnable task) throws Exception {
        Object scheduler = Bukkit.class.getMethod("getRegionScheduler").invoke(null);
        scheduler.getClass().getMethod("run", Plugin.class, Location.class, Consumer.class)
                .invoke(scheduler, plugin, location, toConsumer(task));
    }

    private static void runRegionTaskLater(Plugin plugin, Location location, Runnable task, long delayTicks) throws Exception {
        Object scheduler = Bukkit.class.getMethod("getRegionScheduler").invoke(null);
        scheduler.getClass().getMethod("runDelayed", Plugin.class, Location.class, Consumer.class, long.class)
                .invoke(scheduler, plugin, location, toConsumer(task), normalizeTicks(delayTicks));
    }

    private static Consumer<Object> toConsumer(Runnable task) {
        return scheduledTask -> task.run();
    }

    private static long normalizeTicks(long ticks) {
        return Math.max(1L, ticks);
    }

    private static RuntimeException scheduleException(Plugin plugin, String taskType, Exception exception) {
        Throwable cause = exception instanceof InvocationTargetException
                ? ((InvocationTargetException) exception).getTargetException()
                : exception;
        String error = cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
        plugin.getLogger().severe("Failed to schedule Folia " + taskType + ": " + error);
        return new RuntimeException("Failed to schedule " + taskType + " on Folia", cause);
    }

}
