package net.danh.storage.Utils;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

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
                Object globalRegionScheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                Class<?> consumerClass = Class.forName("java.util.function.Consumer");
                Object consumer = java.lang.reflect.Proxy.newProxyInstance(consumerClass.getClassLoader(), new Class[]{consumerClass}, (proxy, method, args) -> {
                    if (!wrapper.cancelled) {
                        task.run();
                    }
                    return null;
                });
                wrapper.foliaTask = globalRegionScheduler.getClass().getMethod("runDelayed", Plugin.class, consumerClass, long.class).invoke(globalRegionScheduler, plugin, consumer, delayTicks);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to create Folia delayed task: " + e.getMessage());
                throw new RuntimeException("Failed to schedule delayed task on Folia", e);
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
                Object globalRegionScheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                Class<?> consumerClass = Class.forName("java.util.function.Consumer");
                Object consumer = java.lang.reflect.Proxy.newProxyInstance(consumerClass.getClassLoader(), new Class[]{consumerClass}, (proxy, method, args) -> {
                    if (!wrapper.cancelled) {
                        task.run();
                    }
                    return null;
                });
                wrapper.foliaTask = globalRegionScheduler.getClass().getMethod("runAtFixedRate", Plugin.class, consumerClass, long.class, long.class).invoke(globalRegionScheduler, plugin, consumer, delayTicks, periodTicks);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to create Folia timer task: " + e.getMessage());
                throw new RuntimeException("Failed to schedule timer task on Folia", e);
            }
        } else {
            wrapper.bukkitTask = Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
        }
        return wrapper;
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
