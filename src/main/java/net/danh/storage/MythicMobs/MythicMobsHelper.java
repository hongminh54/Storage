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

    public MythicMobsHelper() {
        this.scanPackage();
    }

    private void scanPackage() {
        Storage.getStorage().getLogger().log(Level.INFO, "[MythicStorage] Scanning for MythicMobs API...");
        for (String packagee : mmPackageAPI) {
            try {
                String className = packagee + ".BukkitAPIHelper";
                Storage.getStorage().getLogger().log(Level.INFO, "[MythicStorage] Trying: " + className);
                apiInstance = Class.forName(className).newInstance();
                this.packageName = packagee;
                Storage.getStorage().getLogger().log(Level.INFO, "[MythicStorage] ✓ MythicMobs API detected: " + className);
                initialized = true;
                return;
            } catch (Exception ignored) {
                Storage.getStorage().getLogger().log(Level.FINE, "[MythicStorage] × Failed to load: " + packagee);
            }
        }
        Storage.getStorage().getLogger().log(Level.WARNING, "[MythicStorage] × MythicMobs API not found! MythicStorage feature will be disabled.");
    }

    public boolean isInitialized() {
        return initialized;
    }

    public boolean isMythicMob(@NotNull Entity entity) {
        if (!initialized) return false;
        try {
            Method isMythicMob = apiInstance.getClass().getMethod("isMythicMob", Entity.class);
            return (boolean) isMythicMob.invoke(apiInstance, entity);
        } catch (Exception e) {
            return false;
        }
    }

    @Nullable
    public String getMythicMobInternalName(@NotNull Entity entity) {
        if (!initialized) return null;
        try {
            Method getMythicMobInstance = apiInstance.getClass().getMethod("getMythicMobInstance", Entity.class);
            Object mobInstance = getMythicMobInstance.invoke(apiInstance, entity);
            if (mobInstance == null) return null;

            Method getType = mobInstance.getClass().getMethod("getType");
            Object mobType = getType.invoke(mobInstance);
            if (mobType == null) return null;

            Method getInternalName = mobType.getClass().getMethod("getInternalName");
            return (String) getInternalName.invoke(mobType);
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
                Storage.getStorage().getLogger().log(Level.WARNING, "[MythicStorage] ItemManager is null for item: " + itemName);
                return null;
            }

            Storage.getStorage().getLogger().log(Level.FINE, "[MythicStorage] Attempting to get item: " + itemName);

            try {
                Method getItemStack = itemManager.getClass().getMethod("getItemStack", String.class);
                ItemStack result = (ItemStack) getItemStack.invoke(itemManager, itemName);

                if (result != null) {
                    Storage.getStorage().getLogger().log(Level.FINE, "[MythicStorage] ✓ Found item via getItemStack: " + itemName);
                    return result;
                }
            } catch (NoSuchMethodException e) {
                Storage.getStorage().getLogger().log(Level.FINE, "[MythicStorage] getItemStack method not found, trying alternatives...");
            }

            try {
                Method getItem = itemManager.getClass().getMethod("getItem", String.class);
                Object item = getItem.invoke(itemManager, itemName);

                if (item != null) {
                    try {
                        Method generateItemStack = item.getClass().getMethod("generateItemStack");
                        ItemStack result = (ItemStack) generateItemStack.invoke(item);
                        if (result != null) {
                            Storage.getStorage().getLogger().log(Level.FINE, "[MythicStorage] ✓ Found item via getItem+generateItemStack: " + itemName);
                            return result;
                        }
                    } catch (Exception ignored) {
                    }
                }
            } catch (Exception ignored) {
            }

            Storage.getStorage().getLogger().log(Level.WARNING, "[MythicStorage] × Item not found: " + itemName);
            return null;

        } catch (Exception e) {
            Storage.getStorage().getLogger().log(Level.WARNING, "[MythicStorage] Error getting item '" + itemName + "': " + e.getClass().getSimpleName() + " - " + e.getMessage());
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

        Storage.getStorage().getLogger().log(Level.INFO, "[MythicStorage] Validating item: " + itemName);

        try {
            Object itemManager = getItemManager();
            if (itemManager == null) {
                Storage.getStorage().getLogger().log(Level.WARNING, "[MythicStorage] × ItemManager is null, cannot validate: " + itemName);
                return false;
            }

            try {
                Method getItem = itemManager.getClass().getMethod("getItem", String.class);
                Object item = getItem.invoke(itemManager, itemName);

                if (item != null) {
                    Storage.getStorage().getLogger().log(Level.INFO, "[MythicStorage] ✓ Validated item via getItem: " + itemName);
                    return true;
                }
            } catch (NoSuchMethodException e) {
                Storage.getStorage().getLogger().log(Level.FINE, "[MythicStorage] getItem method not available");
            }

            try {
                Method getItemOptional = itemManager.getClass().getMethod("getItemOptional", String.class);
                Object optional = getItemOptional.invoke(itemManager, itemName);

                if (optional instanceof Optional) {
                    boolean present = ((Optional<?>) optional).isPresent();
                    if (present) {
                        Storage.getStorage().getLogger().log(Level.INFO, "[MythicStorage] ✓ Validated item via getItemOptional: " + itemName);
                        return true;
                    }
                }
            } catch (NoSuchMethodException e) {
                Storage.getStorage().getLogger().log(Level.FINE, "[MythicStorage] getItemOptional method not available");
            }

            try {
                Method hasItem = itemManager.getClass().getMethod("hasItem", String.class);
                boolean has = (boolean) hasItem.invoke(itemManager, itemName);
                if (has) {
                    Storage.getStorage().getLogger().log(Level.INFO, "[MythicStorage] ✓ Validated item via hasItem: " + itemName);
                    return true;
                }
            } catch (NoSuchMethodException e) {
                Storage.getStorage().getLogger().log(Level.FINE, "[MythicStorage] hasItem method not available");
            }

            ItemStack testStack = getMythicItem(itemName);
            if (testStack != null) {
                Storage.getStorage().getLogger().log(Level.INFO, "[MythicStorage] ✓ Validated item via getMythicItem: " + itemName);
                return true;
            }

            Storage.getStorage().getLogger().log(Level.WARNING, "[MythicStorage] × Item validation failed for: " + itemName);
            Storage.getStorage().getLogger().log(Level.WARNING, "[MythicStorage] Available methods in ItemManager: " + Arrays.toString(itemManager.getClass().getMethods()));

            return false;

        } catch (Exception e) {
            Storage.getStorage().getLogger().log(Level.WARNING, "[MythicStorage] Exception validating item '" + itemName + "': " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Nullable
    private Object getItemManager() {
        String[] possibleMainClasses = {
                "io.lumine.mythic.bukkit.MythicBukkit",              // MM 5.x Free
                "io.lumine.mythic.core.MythicMobs",                  // MM 5.x Premium
                "io.lumine.xikage.mythicmobs.MythicMobs",            // MM 4.x
                "io.lumine.mythic.MythicMobs"                        // Fallback
        };

        for (String mainClass : possibleMainClasses) {
            try {
                Storage.getStorage().getLogger().log(Level.FINE, "[MythicStorage] Trying main class: " + mainClass);
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
                Storage.getStorage().getLogger().log(Level.FINE, "[MythicStorage] × Class not found: " + mainClass);
            } catch (Exception e) {
                Storage.getStorage().getLogger().log(Level.FINE, "[MythicStorage] × Failed to get ItemManager from " + mainClass + ": " + e.getMessage());
            }
        }

        Storage.getStorage().getLogger().log(Level.WARNING, "[MythicStorage] × Failed to get ItemManager from any known MythicMobs class");
        return null;
    }

    public String getPackageName() {
        return packageName;
    }
}
