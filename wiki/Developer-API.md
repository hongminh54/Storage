# Developer API

The Storage plugin contains a comprehensive API allowing you to hook into storage events, read data, and update item quantities asynchronously or synchronously.

## Adding the Dependency

**Maven:**
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.TheBlueSkyARL</groupId>
        <artifactId>Storage</artifactId>
        <version>VERSION</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

**Gradle:**
```gradle
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    compileOnly 'com.github.TheBlueSkyARL:Storage:VERSION'
}
```

In your `plugin.yml`:
```yaml
depend: [Storage]
# Or softdepend: [Storage]
```

---

## Core Operations (`StorageAPI`)

```java
import net.danh.storage.API.StorageAPI;
import net.danh.storage.API.StoragePlayer;

if (StorageAPI.isInitialized()) {
    StoragePlayer player = StorageAPI.getStoragePlayer(bukkitPlayer);

    // Give items
    player.addItem("STONE;0", 64);

    // Take items
    player.removeItem("STONE;0", 32);

    // Transfer items
    player.transferTo(otherBukkitPlayer, "DIAMOND;0", 10);

    // Check data
    int totalItems = player.getTotalStoredItems();
    boolean isFull = player.isStorageFull();
    double usagePercent = player.getStorageUsagePercentage();
}
```

## Async Operations (`StorageAsyncAPI`)
For tasks that might cause slight lag (database writes/reads), use the Async API.

```java
import net.danh.storage.API.StorageAsyncAPI;

// Non-blocking add
CompletableFuture<Boolean> future = StorageAsyncAPI.addItemAsync(player, "DIAMOND;0", 64);
future.thenAccept(success -> {
    if (success) {
        player.sendMessage("Items added safely in the background!");
    }
});
```

## Hook System (`StorageHookAPI`)
You can intercept deposits and transfers!

```java
import net.danh.storage.API.StorageHookAPI;
import net.danh.storage.API.StorageHookAPI.*;

StorageHookAPI.registerDepositHook(new DepositHook() {
    @Override
    public boolean onBeforeDeposit(Player player, String material, int amount) {
        // Return false to cancel the deposit
        return true; 
    }
    
    @Override
    public void onAfterDeposit(Player player, String material, int amount) {
        player.sendMessage("Successfully deposited " + amount + " " + material);
    }
}, HookPriority.HIGH);
```

## Listening to Events
Storage fires several Bukkit Events that you can listen to locally.

| Event Class | Description |
|---|---|
| `StorageDepositEvent` | Fired when a player mines/deposits into main storage. |
| `StorageWithdrawEvent` | Fired when items are withdrawn to an inventory. |
| `StorageTransferEvent` | Fired when items are transferred. |
| `RecipeCraftEvent` | Fired during all 3 phases of custom crafting. |
| `MythicStorageDepositEvent` | Special event for MythicMobs items drops. |

Example:
```java
@EventHandler
public void onDeposit(StorageDepositEvent event) {
    Player player = event.getPlayer();
    String material = event.getMaterial();
    int amount = event.getAmount();
    
    if (material.equals("DIAMOND;0")) {
        player.sendMessage("You mined a shiny diamond!");
        // You can change amounts dynamically:
        event.setAmount(amount * 2);
    }
}
```

---
> *For the full suite of utility classes (CraftingAPI, StorageStatsAPI, GUIBuilder, ConvertAPI), refer to the internal JavaDocs in `net.danh.storage.API.*`.*
