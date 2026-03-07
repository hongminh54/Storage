# API Dành Cho Lập Trình Viên (Developer API)

Plugin Storage cung cấp một API để bạn có thể hook vào các sự kiện lưu trữ, đọc dữ liệu, và cập nhật số lượng item (sync hoặc async).

## Thêm Dependency

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

Trong `plugin.yml`:
```yaml
depend: [Storage]
# Hoặc softdepend: [Storage]
```

---

## Thao Tác Cốt Lõi (`StorageAPI`) (Core Operations)

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

## Thao Tác Bất Đồng Bộ (`StorageAsyncAPI`) (Async Operations)
Với các tác vụ có thể gây lag nhẹ (đọc/ghi database), hãy dùng Async API.

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
Bạn có thể can thiệp (intercept) thao tác nạp vào kho hoặc chuyển đồ.

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

## Lắng Nghe Sự Kiện (Listening to Events)
Storage bắn ra một số Bukkit Event để bạn lắng nghe trong plugin của mình.

| Event Class | Description |
|---|---|
| `StorageDepositEvent` | Fired when a player mines/deposits into main storage. |
| `StorageWithdrawEvent` | Fired when items are withdrawn to an inventory. |
| `StorageTransferEvent` | Fired when items are transferred. |
| `RecipeCraftEvent` | Fired during all 3 phases of custom crafting. |
| `MythicStorageDepositEvent` | Special event for MythicMobs items drops. |

Mẫu:
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
> *Để xem đầy đủ các utility class (CraftingAPI, StorageStatsAPI, GUIBuilder, ConvertAPI), hãy tham khảo JavaDocs nội bộ trong `net.danh.storage.API.*`.*
