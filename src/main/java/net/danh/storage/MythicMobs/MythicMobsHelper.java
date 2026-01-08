package net.danh.storage.MythicMobs;

import net.danh.storage.Manager.MythicStorageManager;
import net.danh.storage.Storage;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;
import java.util.regex.Pattern;

public class MythicMobsHelper {

    private static final Pattern HEX_PATTERN_1 =
            Pattern.compile("&#[0-9a-fA-F]{6}");
    private static final Pattern HEX_PATTERN_2 =
            Pattern.compile("<#[0-9a-fA-F]{6}>");
    private static final Pattern BUNGEE_SECTION_HEX_PATTERN =
            Pattern.compile("\\u00A7x(\\u00A7[0-9a-fA-F]){6}");
    private static final Pattern SECTION_COLOR_PATTERN =
            Pattern.compile("\\u00A7[0-9a-fk-orA-FK-OR]");
    private static final Pattern AMPERSAND_HEX_PATTERN =
            Pattern.compile("&x(&[0-9a-fA-F]){6}");
    private static final Pattern AMPERSAND_COLOR_PATTERN =
            Pattern.compile("&[0-9a-fk-orA-FK-OR]");
    private final List<String> mmPackageAPI = Arrays.asList("io.lumine.mythic.bukkit", "io.lumine.xikage.mythicmobs.api.bukkit", "io.lumine.mythic.api.bukkit");
    private final Map<String, String> strippedNameToInternalName =
            new HashMap<>();
    private Object apiInstance;
    private String packageName = "";
    private boolean initialized = false;
    private boolean warnedScanPackage;
    private boolean warnedIsMythicMob;
    private boolean warnedGetInternalName;
    private boolean warnedGetItemManager;
    private Method cachedIsMythicMobMethod;
    private Method cachedGetMythicMobInstanceMethod;
    private Method cachedGetTypeMethod;
    private Method cachedGetInternalNameMethod;
    private String lastCacheKey;
    private Method cachedGetItemsMethod;

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
            } catch (ClassNotFoundException | NoClassDefFoundError ignored) {
            } catch (Exception e) {
                if (!warnedScanPackage) {
                    warnedScanPackage = true;
                    Storage.getStorage().getLogger().log(Level.WARNING,
                            "[MythicStorage] Failed to probe MythicMobs API package: "
                                    + packagee,
                            e);
                }
            }
        }
        Storage.getStorage().getLogger().log(Level.WARNING, "[MythicStorage] MythicMobs API not found! MythicStorage feature will be disabled.");
    }

    private void cacheCommonMethods() {
        try {
            cachedIsMythicMobMethod = apiInstance.getClass().getMethod("isMythicMob", Entity.class);
            cachedGetMythicMobInstanceMethod = apiInstance.getClass().getMethod("getMythicMobInstance", Entity.class);
        } catch (Exception e) {
            Storage.getStorage().getLogger().log(Level.WARNING,
                    "[MythicStorage] Failed to cache MythicMobs API methods",
                    e);
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
            if (!warnedIsMythicMob) {
                warnedIsMythicMob = true;
                Storage.getStorage().getLogger().log(Level.WARNING,
                        "[MythicStorage] Failed to check isMythicMob for entity: "
                                + entity.getType(),
                        e);
            }
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
            if (!warnedGetInternalName) {
                warnedGetInternalName = true;
                Storage.getStorage().getLogger().log(Level.WARNING,
                        "[MythicStorage] Failed to get MythicMob internal name for entity: "
                                + entity.getType(),
                        e);
            }
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
            } catch (NoSuchMethodException ignored) {
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
        if (text == null) {
            return null;
        }
        String result = text;
        result = HEX_PATTERN_1.matcher(result).replaceAll("");
        result = HEX_PATTERN_2.matcher(result).replaceAll("");
        result = BUNGEE_SECTION_HEX_PATTERN.matcher(result).replaceAll("");
        result = SECTION_COLOR_PATTERN.matcher(result).replaceAll("");
        result = AMPERSAND_HEX_PATTERN.matcher(result).replaceAll("");
        result = AMPERSAND_COLOR_PATTERN.matcher(result).replaceAll("");
        return result;
    }

    private void rebuildInternalNameCache(Object itemManager,
                                          List<String> configuredDrops) {
        strippedNameToInternalName.clear();

        if (itemManager == null || configuredDrops == null
                || configuredDrops.isEmpty()) {
            lastCacheKey = null;
            return;
        }

        try {
            if (cachedGetItemsMethod == null) {
                cachedGetItemsMethod = itemManager.getClass().getMethod(
                        "getItems");
            }
            Object itemsCollection = cachedGetItemsMethod.invoke(itemManager);
            if (!(itemsCollection instanceof Iterable)) {
                lastCacheKey = null;
                return;
            }

            for (Object mythicItem : (Iterable<?>) itemsCollection) {
                if (mythicItem == null) {
                    continue;
                }
                try {
                    Method getInternalName = mythicItem.getClass().getMethod(
                            "getInternalName");
                    String internalName = (String) getInternalName.invoke(
                            mythicItem);
                    if (internalName == null
                            || !configuredDrops.contains(internalName)) {
                        continue;
                    }

                    Method getDisplayName = mythicItem.getClass().getMethod(
                            "getDisplayName");
                    Object displayNameObj = getDisplayName.invoke(mythicItem);
                    String mythicDisplayName = extractDisplayName(
                            displayNameObj);
                    if (mythicDisplayName == null
                            || mythicDisplayName.isEmpty()) {
                        continue;
                    }

                    String stripped = stripAllColors(mythicDisplayName);
                    if (stripped != null && !stripped.isEmpty()) {
                        strippedNameToInternalName.put(stripped,
                                internalName);
                    }
                } catch (Exception ignored) {
                }
            }

            lastCacheKey = String.valueOf(configuredDrops.hashCode());
        } catch (Exception e) {
            strippedNameToInternalName.clear();
            lastCacheKey = null;
        }
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
            if (strippedDisplayName == null || strippedDisplayName.isEmpty()) {
                return null;
            }

            Object itemManager = getItemManager();
            if (itemManager == null) return null;

            List<String> configuredDrops = MythicStorageManager.getConfiguredDrops();
            String cacheKey = configuredDrops == null ? null :
                    String.valueOf(configuredDrops.hashCode());
            if (cacheKey != null && !cacheKey.equals(lastCacheKey)) {
                rebuildInternalNameCache(itemManager, configuredDrops);
            }

            String cached = strippedNameToInternalName.get(strippedDisplayName);
            if (cached != null) {
                return cached;
            }

            if (configuredDrops != null && !configuredDrops.isEmpty()) {
                rebuildInternalNameCache(itemManager, configuredDrops);
                return strippedNameToInternalName.get(strippedDisplayName);
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

            boolean foundByMethod = false;

            try {
                Method getItem = itemManager.getClass().getMethod("getItem", String.class);
                Object item = getItem.invoke(itemManager, itemName);

                if (item != null) {
                    foundByMethod = true;
                }
            } catch (NoSuchMethodException e) {
                // Method doesn't exist, try next
            } catch (Exception e) {
                Storage.getStorage().getLogger().log(Level.WARNING, "[MythicStorage] Error checking item '" + itemName + "': " + e.getMessage());
                return false;
            }

            // Try getItemOptional method (MM 5.x)
            if (!foundByMethod) {
                try {
                    Method getItemOptional = itemManager.getClass().getMethod("getItemOptional", String.class);
                    Object optional = getItemOptional.invoke(itemManager, itemName);

                    if (optional instanceof Optional) {
                        foundByMethod = ((Optional<?>) optional).isPresent();
                    }
                } catch (NoSuchMethodException e) {
                    // Method doesn't exist, try next
                } catch (Exception e) {
                    Storage.getStorage().getLogger().log(Level.WARNING, "[MythicStorage] Error checking item '" + itemName + "': " + e.getMessage());
                    return false;
                }
            }

            if (!foundByMethod) {
                try {
                    Method hasItem = itemManager.getClass().getMethod("hasItem", String.class);
                    foundByMethod = (boolean) hasItem.invoke(itemManager, itemName);
                } catch (NoSuchMethodException e) {
                    // Method doesn't exist, try next
                } catch (Exception e) {
                    Storage.getStorage().getLogger().log(Level.WARNING, "[MythicStorage] Error checking item '" + itemName + "': " + e.getMessage());
                    return false;
                }
            }

            if (!foundByMethod) {
                try {
                    ItemStack testStack = getMythicItem(itemName);
                    foundByMethod = (testStack != null);
                } catch (Exception e) {
                    Storage.getStorage().getLogger().log(Level.WARNING, "[MythicStorage] Error generating ItemStack for '" + itemName + "': " + e.getMessage());
                    return false;
                }
            }

            if (!foundByMethod) {
                Storage.getStorage().getLogger().log(Level.WARNING, "[MythicStorage] Invalid MythicMobs item: " + itemName);
            }

            return foundByMethod;

        } catch (Exception e) {
            Storage.getStorage().getLogger().log(Level.WARNING, "[MythicStorage] Exception validating item '" + itemName + "': " + e.getMessage());
            return false;
        }
    }

    @Nullable
    private Object getItemManager() {
        String[] possibleMainClasses = {"io.lumine.mythic.bukkit.MythicBukkit", "io.lumine.mythic.core.MythicMobs", "io.lumine.xikage.mythicmobs.MythicMobs", "io.lumine.mythic.MythicMobs"};

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
            } catch (ClassNotFoundException ignored) {
            } catch (Exception e) {
                if (!warnedGetItemManager) {
                    warnedGetItemManager = true;
                    Storage.getStorage().getLogger().log(Level.WARNING,
                            "[MythicStorage] Failed to resolve MythicMobs ItemManager via reflection",
                            e);
                }
            }
        }

        return null;
    }

    @Nullable
    public String getItemDisplayName(@NotNull String itemName) {
        if (!initialized) return null;

        try {
            Object itemManager = getItemManager();
            if (itemManager == null) return null;

            Object mythicItem = null;
            try {
                Method getItem = itemManager.getClass().getMethod("getItem", String.class);
                mythicItem = getItem.invoke(itemManager, itemName);
            } catch (NoSuchMethodException e) {
                try {
                    Method getItemOptional = itemManager.getClass().getMethod("getItemOptional", String.class);
                    Object optional = getItemOptional.invoke(itemManager, itemName);
                    if (optional instanceof Optional && ((Optional<?>) optional).isPresent()) {
                        mythicItem = ((Optional<?>) optional).get();
                    }
                } catch (Exception ignored) {
                }
            }

            if (mythicItem == null) {
                ItemStack itemStack = getMythicItem(itemName);
                if (itemStack != null && itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName()) {
                    return itemStack.getItemMeta().getDisplayName();
                }
                return null;
            }

            try {
                Method getDisplayName = mythicItem.getClass().getMethod("getDisplayName");
                Object displayNameObj = getDisplayName.invoke(mythicItem);
                String displayName = extractDisplayName(displayNameObj);
                if (displayName != null && !displayName.isEmpty()) {
                    return displayName;
                }
            } catch (Exception ignored) {
            }

            ItemStack itemStack = getMythicItem(itemName);
            if (itemStack != null && itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName()) {
                return itemStack.getItemMeta().getDisplayName();
            }

        } catch (Exception e) {
            Storage.getStorage().getLogger().log(Level.WARNING,
                    "[MythicStorage] Failed to resolve display name for item '" + itemName + "'",
                    e);
        }

        return null;
    }

    public String getPackageName() {
        return packageName;
    }
}
