package net.danh.storage.Utils;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

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
                Object globalRegionScheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                Class<?> consumerClass = Class.forName("java.util.function.Consumer");
                Object consumer = java.lang.reflect.Proxy.newProxyInstance(consumerClass.getClassLoader(), new Class[]{consumerClass}, (proxy, method, args) -> {
                    task.run();
                    return null;
                });
                globalRegionScheduler.getClass().getMethod("run", Plugin.class, consumerClass).invoke(globalRegionScheduler, plugin, consumer);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to run Folia task: " + e.getMessage());
                throw new RuntimeException("Failed to schedule task on Folia", e);
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    public static void runTaskLater(Plugin plugin, Runnable task, long delayTicks) {
        if (IS_FOLIA) {
            try {
                long foliaDelayTicks = Math.max(1L, delayTicks);

                Object globalRegionScheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                Class<?> consumerClass = Class.forName("java.util.function.Consumer");
                Object consumer = java.lang.reflect.Proxy.newProxyInstance(consumerClass.getClassLoader(), new Class[]{consumerClass}, (proxy, method, args) -> {
                    task.run();
                    return null;
                });
                globalRegionScheduler.getClass().getMethod("runDelayed", Plugin.class, consumerClass, long.class).invoke(globalRegionScheduler, plugin, consumer, foliaDelayTicks);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to run Folia delayed task: " + e.getMessage());
                throw new RuntimeException("Failed to schedule delayed task on Folia", e);
            }
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        }
    }

    public static void runTaskAsynchronously(Plugin plugin, Runnable task) {
        if (IS_FOLIA) {
            try {
                Object asyncScheduler = Bukkit.class.getMethod("getAsyncScheduler").invoke(null);
                Class<?> consumerClass = Class.forName("java.util.function.Consumer");
                Object consumer = java.lang.reflect.Proxy.newProxyInstance(consumerClass.getClassLoader(), new Class[]{consumerClass}, (proxy, method, args) -> {
                    task.run();
                    return null;
                });
                asyncScheduler.getClass().getMethod("runNow", Plugin.class, consumerClass).invoke(asyncScheduler, plugin, consumer);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to run Folia async task: " + e.getMessage());
                throw new RuntimeException("Failed to schedule async task on Folia", e);
            }
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        }
    }

}
