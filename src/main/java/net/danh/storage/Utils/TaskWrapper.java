package net.danh.storage.Utils;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.InvocationTargetException;

public class TaskWrapper {

    private BukkitTask bukkitTask;
    private Object foliaTask; // ScheduledTask for Folia
    private boolean cancelled = false;

    private TaskWrapper() {
    }

    public static TaskWrapper runTaskLater(Plugin plugin, Runnable task, long delayTicks) {
        TaskWrapper wrapper = new TaskWrapper();
        if (SchedulerUtil.isFolia()) {
            try {
                long foliaDelayTicks = Math.max(1L, delayTicks);

                Object globalRegionScheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                Class<?> consumerClass = Class.forName("java.util.function.Consumer");
                Object consumer = java.lang.reflect.Proxy.newProxyInstance(consumerClass.getClassLoader(), new Class[]{consumerClass}, (proxy, method, args) -> {
                    if (!wrapper.cancelled) {
                        try {
                            task.run();
                        } catch (Exception taskEx) {
                            plugin.getLogger().warning("Error in Folia delayed task: " + taskEx.getMessage());
                        }
                    }
                    return null;
                });
                wrapper.foliaTask = globalRegionScheduler.getClass().getMethod("runDelayed", Plugin.class, consumerClass, long.class).invoke(globalRegionScheduler, plugin, consumer, foliaDelayTicks);
            } catch (Exception e) {
                Throwable cause = e instanceof InvocationTargetException ? ((InvocationTargetException) e).getTargetException() : e;
                String errorMsg = cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
                plugin.getLogger().severe("Failed to create Folia delayed task: " + errorMsg);
                throw new RuntimeException("Failed to schedule delayed task on Folia", cause);
            }
        } else {
            wrapper.bukkitTask = Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        }
        return wrapper;
    }

    public static TaskWrapper runTaskTimer(Plugin plugin, Runnable task, long delayTicks, long periodTicks) {
        TaskWrapper wrapper = new TaskWrapper();
        if (SchedulerUtil.isFolia()) {
            try {
                long foliaDelayTicks = Math.max(1L, delayTicks);
                long foliaPeriodTicks = Math.max(1L, periodTicks);

                Object globalRegionScheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                Class<?> consumerClass = Class.forName("java.util.function.Consumer");
                Object consumer = java.lang.reflect.Proxy.newProxyInstance(consumerClass.getClassLoader(), new Class[]{consumerClass}, (proxy, method, args) -> {
                    if (!wrapper.cancelled) {
                        try {
                            task.run();
                        } catch (Exception taskEx) {
                            plugin.getLogger().warning("Error in Folia timer task: " + taskEx.getMessage());
                        }
                    }
                    return null;
                });
                wrapper.foliaTask = globalRegionScheduler.getClass().getMethod("runAtFixedRate", Plugin.class, consumerClass, long.class, long.class).invoke(globalRegionScheduler, plugin, consumer, foliaDelayTicks, foliaPeriodTicks);
            } catch (Exception e) {
                Throwable cause = e instanceof InvocationTargetException ? ((InvocationTargetException) e).getTargetException() : e;
                String errorMsg = cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
                plugin.getLogger().severe("Failed to create Folia timer task: " + errorMsg);
                throw new RuntimeException("Failed to schedule timer task on Folia", cause);
            }
        } else {
            wrapper.bukkitTask = Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
        }
        return wrapper;
    }

    public static TaskWrapper runTaskTimerSafe(Plugin plugin, Runnable task, long delayTicks, long periodTicks) {
        try {
            return runTaskTimer(plugin, task, delayTicks, periodTicks);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to schedule timer task (safe mode): " + e.getMessage());
            return null;
        }
    }

    public void cancel() {
        cancelled = true;
        if (bukkitTask != null) {
            bukkitTask.cancel();
        }
        if (foliaTask != null && SchedulerUtil.isFolia()) {
            try {
                foliaTask.getClass().getMethod("cancel").invoke(foliaTask);
            } catch (Exception e) {
                // Ignore reflection errors
            }
        }
    }

    public boolean isCancelled() {
        if (bukkitTask != null) {
            return bukkitTask.isCancelled();
        }
        return cancelled;
    }
}
