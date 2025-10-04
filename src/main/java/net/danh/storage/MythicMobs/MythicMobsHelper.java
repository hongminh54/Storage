package net.danh.storage.MythicMobs;

import net.danh.storage.Manager.MythicStorageManager;
import net.danh.storage.Storage;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

public class MythicMobsHelper {

    private final List<String> mmPackageAPI = Arrays.asList(
            "io.lumine.mythic.bukkit",
            "io.lumine.xikage.mythicmobs.api.bukkit",
            "io.lumine.mythic.api.bukkit"
    );

    private Object apiInstance;
    private String packageName = "";
    private boolean initialized = false;

    // Cache reflection methods for performance
    private Method cachedIsMythicMobMethod;
    private Method cachedGetMythicMobInstanceMethod;
    private Method cachedGetTypeMethod;
    private Method cachedGetInternalNameMethod;

    public MythicMobsHelper() {
        this.scanPackage();
    }

    private void scanPackage() {
        Storage.getStorage().getLogger().log(Level.INFO, "[MythicStorage] Scanning for MythicMobs API...");
        for (String packagee : mmPackageAPI) {
            try {
                String className = packagee + ".BukkitAPIHelper";
                apiInstance = Class.forName(className).newInstance();
                this.packageName = packagee;
                Storage.getStorage().getLogger().log(Level.INFO, "[MythicStorage] MythicMobs API detected: " + packagee);
                initialized = true;
                cacheCommonMethods();
                return;
            } catch (Exception ignored) {
            }
        }
        Storage.getStorage().getLogger().log(Level.WARNING, "[MythicStorage] MythicMobs API not found! MythicStorage feature will be disabled.");
    }

    private void cacheCommonMethods() {
        try {
            cachedIsMythicMobMethod = apiInstance.getClass().getMethod("isMythicMob", Entity.class);
            cachedGetMythicMobInstanceMethod = apiInstance.getClass().getMethod("getMythicMobInstance", Entity.class);
        } catch (Exception e) {
            Storage.getStorage().getLogger().log(Level.WARNING, "[MythicStorage] Failed to cache methods: " + e.getMessage());
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    public boolean isMythicMob(@NotNull Entity entity) {
        if (!initialized || cachedIsMythicMobMethod == null) return false;
        try {
            return (boolean) cachedIsMythicMobMethod.invoke(apiInstance, entity);
        } catch (Exception e) {
            return false;
        }
    }

    @Nullable
    public String getMythicMobInternalName(@NotNull Entity entity) {
        if (!initialized || cachedGetMythicMobInstanceMethod == null) return null;
        try {
            Object mobInstance = cachedGetMythicMobInstanceMethod.invoke(apiInstance, entity);
            if (mobInstance == null) return null;

            if (cachedGetTypeMethod == null) {
                cachedGetTypeMethod = mobInstance.getClass().getMethod("getType");
            }
            Object mobType = cachedGetTypeMethod.invoke(mobInstance);
            if (mobType == null) return null;

            if (cachedGetInternalNameMethod == null) {
                cachedGetInternalNameMethod = mobType.getClass().getMethod("getInternalName");
            }
            return (String) cachedGetInternalNameMethod.invoke(mobType);
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    public ItemStack getMythicItem(@NotNull String itemName) {
        if (!initialized) return null;
        try {
            Object itemManager = getItemManager();
            if (itemManager == null) {
                return null;
            }

            try {
                Method getItemStack = itemManager.getClass().getMethod("getItemStack", String.class);
                ItemStack result = (ItemStack) getItemStack.invoke(itemManager, itemName);

                if (result != null) {
                    return result;
                }
            } catch (NoSuchMethodException e) {
            }

            try {
                Method getItem = itemManager.getClass().getMethod("getItem", String.class);
                Object item = getItem.invoke(itemManager, itemName);

                if (item != null) {
                    try {
                        Method generateItemStack = item.getClass().getMethod("generateItemStack");
                        ItemStack result = (ItemStack) generateItemStack.invoke(item);
                        if (result != null) {
                            return result;
                        }
                    } catch (Exception ignored) {
                    }
                }
            } catch (Exception ignored) {
            }

            return null;

        } catch (Exception e) {
            Storage.getStorage().getLogger().log(Level.WARNING, "[MythicStorage] Error getting item '" + itemName + "': " + e.getMessage());
            return null;
        }
    }

    private String stripAllColors(String text) {
        if (text == null) return null;
        text = text.replaceAll("§x(§[0-9a-fA-F]){6}", "");
        text = text.replaceAll("§[0-9a-fk-orA-FK-OR]", "");
        text = text.replaceAll("&x(&[0-9a-fA-F]){6}", "");
        text = text.replaceAll("&[0-9a-fk-orA-FK-OR]", "");
        return text;
    }

    @Nullable
    public String getMythicItemInternalName(@NotNull ItemStack item) {
        if (!initialized || item == null) return null;

        try {
            if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
                return null;
            }

            String displayName = item.getItemMeta().getDisplayName();
            String strippedDisplayName = stripAllColors(displayName);

            Object itemManager = getItemManager();
            if (itemManager == null) return null;

            Method getItems = itemManager.getClass().getMethod("getItems");
            Object itemsCollection = getItems.invoke(itemManager);

            if (!(itemsCollection instanceof Iterable)) return null;

            List<String> configuredDrops = MythicStorageManager.getConfiguredDrops();

            if (configuredDrops != null && !configuredDrops.isEmpty()) {
                for (Object mythicItem : (Iterable<?>) itemsCollection) {
                    try {
                        Method getInternalName = mythicItem.getClass().getMethod("getInternalName");
                        String internalName = (String) getInternalName.invoke(mythicItem);

                        if (!configuredDrops.contains(internalName)) continue;

                        Method getDisplayName = mythicItem.getClass().getMethod("getDisplayName");
                        Object displayNameObj = getDisplayName.invoke(mythicItem);

                        String mythicDisplayName = extractDisplayName(displayNameObj);
                        if (mythicDisplayName == null) continue;

                        String strippedMythicName = stripAllColors(mythicDisplayName);

                        if (strippedMythicName.equals(strippedDisplayName)) {
                            return internalName;
                        }
                    } catch (Exception ignored) {
                    }
                }
            }

            for (Object mythicItem : (Iterable<?>) itemsCollection) {
                try {
                    Method getInternalName = mythicItem.getClass().getMethod("getInternalName");
                    String internalName = (String) getInternalName.invoke(mythicItem);

                    Method getDisplayName = mythicItem.getClass().getMethod("getDisplayName");
                    Object displayNameObj = getDisplayName.invoke(mythicItem);

                    String mythicDisplayName = extractDisplayName(displayNameObj);
                    if (mythicDisplayName == null) continue;

                    String strippedMythicName = stripAllColors(mythicDisplayName);

                    if (strippedMythicName.equals(strippedDisplayName)) {
                        return internalName;
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    private String extractDisplayName(Object displayNameObj) {
        if (displayNameObj == null) return null;

        if (displayNameObj instanceof String) {
            return (String) displayNameObj;
        }

        try {
            Method get = displayNameObj.getClass().getMethod("get");
            return (String) get.invoke(displayNameObj);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isValidMythicItem(@NotNull String itemName) {
        if (!initialized) return false;

        try {
            Object itemManager = getItemManager();
            if (itemManager == null) {
                return false;
            }

            try {
                Method getItem = itemManager.getClass().getMethod("getItem", String.class);
                Object item = getItem.invoke(itemManager, itemName);

                if (item != null) {
                    return true;
                }
            } catch (NoSuchMethodException e) {
            }

            try {
                Method getItemOptional = itemManager.getClass().getMethod("getItemOptional", String.class);
                Object optional = getItemOptional.invoke(itemManager, itemName);

                if (optional instanceof Optional) {
                    boolean present = ((Optional<?>) optional).isPresent();
                    if (present) {
                        return true;
                    }
                }
            } catch (NoSuchMethodException e) {
            }

            try {
                Method hasItem = itemManager.getClass().getMethod("hasItem", String.class);
                boolean has = (boolean) hasItem.invoke(itemManager, itemName);
                if (has) {
                    return true;
                }
            } catch (NoSuchMethodException e) {
            }

            ItemStack testStack = getMythicItem(itemName);
            if (testStack != null) {
                return true;
            }

            Storage.getStorage().getLogger().log(Level.WARNING, "[MythicStorage] Invalid MythicMobs item: " + itemName);
            return false;

        } catch (Exception e) {
            Storage.getStorage().getLogger().log(Level.WARNING, "[MythicStorage] Exception validating item '" + itemName + "': " + e.getMessage());
            return false;
        }
    }

    @Nullable
    private Object getItemManager() {
        String[] possibleMainClasses = {
                "io.lumine.mythic.bukkit.MythicBukkit",
                "io.lumine.mythic.core.MythicMobs",
                "io.lumine.xikage.mythicmobs.MythicMobs",
                "io.lumine.mythic.MythicMobs"
        };

        for (String mainClass : possibleMainClasses) {
            try {
                Class<?> mmClass = Class.forName(mainClass);

                try {
                    Method instMethod = mmClass.getMethod("inst");
                    Object mmInstance = instMethod.invoke(null);
                    Method getItemManager = mmInstance.getClass().getMethod("getItemManager");
                    Object itemManager = getItemManager.invoke(mmInstance);

                    if (itemManager != null) {
                        return itemManager;
                    }
                } catch (NoSuchMethodException e) {
                    try {
                        Method getInstance = mmClass.getMethod("getInstance");
                        Object mmInstance = getInstance.invoke(null);
                        Method getItemManager = mmInstance.getClass().getMethod("getItemManager");
                        Object itemManager = getItemManager.invoke(mmInstance);

                        if (itemManager != null) {
                            return itemManager;
                        }
                    } catch (Exception ignored) {
                    }
                }
            } catch (ClassNotFoundException e) {
            } catch (Exception e) {
            }
        }

        return null;
    }

    public String getPackageName() {
        return packageName;
    }
}
