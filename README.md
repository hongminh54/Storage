# Storage [1.8.x - 1.21.10]

> A comprehensive virtual storage system for Minecraft servers that allows players to automatically store mined blocks, transfer items between players, participate in server events, and manage their resources through an intuitive GUI system.

##  Features

- **Virtual Storage System** - Automatically store mined blocks in a virtual inventory
- **MythicMobs Storage** - Automatically store in virtual inventory with MythicMobs items when players kill Mobs
- **Convert System** - Convert materials between different forms (ingots ↔ blocks) with configurable ratios
- **Transfer System** - Send items to other players with single or multi-item transfers
- **Event System** - Participate in server-wide mining contests and special events
- **Multi-Version Support** - Compatible with Minecraft 1.8.x to 1.21.10
- **WorldGuard Integration** - Respect region protections
- **PlaceholderAPI Support** - Rich placeholder system for other plugins
- **Custom Enchant System** - Custom enchantments for tools with configurable effects, particles, and sounds
- **Special Material System** - Rare materials with custom effects, particles, and sounds that drop from mining
- **Developer API** - Comprehensive API for external plugin integration

> **Note**: This is a complete rework from v1. Please reset all configuration files when updating from v1 to v2.
> 
> **Reworked by**: hongminh54

## MythicMobs Storage System (*New Feature)

A dedicated storage system for MythicMobs items that automatically stores drops from Mobs.

### How It Works

- Automatically stores configured MythicMobs items when players kill Mobs
- All command in `/mythicstorage`
- Configure which MythicMobs items can be stored in `mythicstorage.yml`
- Works with all MythicMobs versions (4.x, 5.x, and newer)
### This feature is still a work in progress and will be completed in the next update: Inventory Storage


## Commands

### User Commands
| Command | Description | Permission |
|---------|-------------|------------|
| `/storage` or `/kho` | Open the main storage GUI | - |
| `/storage help` | Show help information | - |
| `/storage toggle` | Toggle auto-pickup on/off | `storage.toggle` |
| `/storage convert` | Open material conversion GUI | `storage.convert` |
| `/storage transfer <player> <material>` | Transfer specific material to player | `storage.transfer.use` |
| `/storage transfer multi <player>` | Open multi-transfer GUI | `storage.transfer.multi` |
| `/storage transfer log [player] [page]` | View transfer history | `storage.transfer.log` |

### Admin Commands
| Command | Description | Permission |
|---------|-------------|------------|
| `/storage reload` | Reload all configuration files | `storage.admin.reload` |
| `/storage max <player> <amount>` | Set max storage for player | `storage.admin.max` |
| `/storage add <material;data> <player> <amount>` | Add items to player storage | `storage.admin.add` |
| `/storage remove <material;data> <player> <amount>` | Remove items from player storage | `storage.admin.remove` |
| `/storage set <material;data> <player> <amount>` | Set item amount for player | `storage.admin.set` |
| `/storage reset <material\|all> [player]` | Reset storage materials for players | `storage.admin.reset` |
| `/storage autosave` | Show auto-save system status | `storage.admin.reload` |
| `/storage save` | Force save all player data | `storage.admin.reload` |

### Event Commands
| Command | Description | Permission |
|---------|-------------|------------|
| `/storage event list` | List all active events | `storage.event.view` |
| `/storage event start <event>` | Start a specific event | `storage.event.admin` |
| `/storage event stop <event>` | Stop a specific event | `storage.event.admin` |

**Available Events**: `mining_contest`, `double_drop`, `community_event`

### Enchant Commands
| Command | Description | Permission              |
|---------|-------------|-------------------------|
| `/storage enchant give <enchant> <level>` | Give custom enchant to item in hand | `storage.admin.enchant` |
| `/storage enchant remove <enchant>` | Remove custom enchant from item in hand | `storage.admin.enchant` |
| `/storage enchant list` | List all available enchants | `storage.admin.enchant` |
| `/storage enchant info <enchant>` | Show detailed enchant information | `storage.admin.enchant` |
| `/storage enchant setmaxlevel <enchant> <level>` | Set maximum level for enchant | `storage.admin.enchant` |

**Available Enchants**: `tnt`, `haste`, `multiplier`, `veinminer`

### Special Material Commands
| Command | Description | Permission |
|---------|-------------|------------|
| `/storage specialmaterial list` | List all special materials | `storage.admin.specialmaterial` |
| `/storage specialmaterial info <material>` | Show detailed material information | `storage.admin.specialmaterial` |
| `/storage specialmaterial give <player> <material> <amount>` | Give special material to player | `storage.admin.specialmaterial` |

### MythicMobs Storage Commands
| Command | Description | Permission |
|---------|-------------|------------|
| `/mythicstorage` | Open MythicStorage GUI | `storage.mythicstorage.use` |
| `/mythicstorage toggle` | Toggle auto-pickup for MythicMobs items | `storage.mythicstorage.toggle` |
| `/mythicstorage view <player>` | View player's MythicStorage | `storage.mythicstorage.view` |
| `/mythicstorage add <player> <item> <amount>` | Add MythicMobs items to player storage | `storage.mythicstorage.admin` |
| `/mythicstorage remove <player> <item> <amount>` | Remove MythicMobs items from player storage | `storage.mythicstorage.admin` |
| `/mythicstorage set <player> <item> <amount>` | Set MythicMobs item amount for player | `storage.mythicstorage.admin` |
| `/mythicstorage reset <player> [item]` | Reset player's MythicStorage | `storage.mythicstorage.admin` |
| `/mythicstorage reload` | Reload MythicStorage configuration | `storage.mythicstorage.admin` |
| `/mythicstorage help` | Show MythicStorage help | - |

**Material Format**: For 1.12.2 and below use `MATERIAL;DATA` (e.g., `COAL;0`). For 1.13+ use `MATERIAL;0`.

## Permissions

### User Permissions
| Permission | Description | Default |
|------------|-------------|---------|
| `storage.toggle` | Toggle auto-pickup | `true` |
| `storage.convert` | Use material conversion feature | `true` |
| `storage.transfer.use` | Use basic transfer feature | `true` |
| `storage.transfer.multi` | Use multi-transfer feature | `true` |
| `storage.transfer.log` | View own transfer logs | `true` |
| `storage.transfer.log.others` | View other players' transfer logs | `true` |
| `storage.event.view` | View event status | `true` |
| `storage.enchant.use` | Use enchanted items | `true` |
| `storage.mythicstorage.use` | Use MythicStorage GUI | `true` |
| `storage.mythicstorage.toggle` | Toggle MythicMobs auto-pickup | `true` |
| `storage.mythicstorage.view` | View other players' MythicStorage | `op` |

### Admin Permissions
| Permission                     | Description           | Default |
|--------------------------------|-----------------------|-|
| `storage.admin`                | General admin permission | `op` |
| `storage.admin.add`            | Add items to player storage | `op` |
| `storage.admin.remove`         | Remove items from player storage | `op` |
| `storage.admin.set`            | Set item amounts for players | `op` |
| `storage.admin.max`            | Set max storage for players | `op` |
| `storage.admin.reload`         | Reload plugin configuration | `op` |
| `storage.admin.reset`          | Reset player storage materials | `op` |
| `storage.admin.specialmaterial` | Manage special materials | `op` |
| `storage.transfer.admin`       | Bypass transfer cooldowns and limits | `op` |
| `storage.event.admin`          | Manage server events  | `op` |
| `storage.enchant.admin`        | Manage custom enchants| `op` |
| `storage.mythicstorage.admin`  | Manage MythicStorage system | `op` |

## Placeholders

### Storage Placeholders
| Placeholder | Description | Example |
|-------------|-------------|---------|
| `%storage_storage_<MATERIAL>%` | Show amount of material in storage | `%storage_storage_COAL;0%` |
| `%storage_storage_<MATERIAL>_formatted%` | Show formatted amount | `15.2K` |
| `%storage_storage_<MATERIAL>_percentage%` | Show percentage of material vs max | `45` |
| `%storage_max_storage%` | Show max storage capacity | `100000` |
| `%storage_percentage%` | Show total storage percentage used | `67` |
| `%storage_price_<MATERIAL>%` | Show material's sell price | `%storage_price_COAL;0%` |
| `%storage_status%` | Show auto-pickup status (On/Off) | `On` |

#### Storage Statistics
| Placeholder | Description | Example |
|-------------|-------------|---------|
| `%storage_total_materials%` | Number of different materials stored | `12` |
| `%storage_total_blocks%` | Total blocks in storage | `50000` |
| `%storage_total_blocks_formatted%` | Total blocks formatted | `50.0K` |
| `%storage_available_space%` | Available storage space | `50000` |
| `%storage_available_space_formatted%` | Available space formatted | `50.0K` |
| `%storage_transfer_sent_total%` | Total blocks sent via transfer | `1500` |
| `%storage_transfer_received_total%` | Total blocks received | `800` |
| `%storage_transfer_count%` | Total number of transfers | `25` |

#### Storage Leaderboard Placeholders
| Placeholder | Description | Example |
|-------------|-------------|---------|
| `%storage_top_<material>_<position>_name%` | Player name at rank for specific material | `%storage_top_DIAMOND;0_1_name%` |
| `%storage_top_<material>_<position>_amount%` | Player amount at rank for specific material | `%storage_top_DIAMOND;0_1_amount%` |
| `%storage_top_all_<position>_name%` | Player name at rank for total blocks | `%storage_top_all_1_name%` |
| `%storage_top_all_<position>_amount%` | Player total blocks at rank | `%storage_top_all_1_amount%` |

**Note**: 
- Replace `<material>` with material format from config (e.g., `DIAMOND;0`, `COAL;0`)
- Replace `<position>` with rank number (1, 2, 3, etc.)
- Use `all` to show total of all blocks combined
- Leaderboard only shows online players (for performance)

### Event System Placeholders

#### General Event Placeholders
| Placeholder | Description | Example |
|-------------|-------------|---------|
| `%storage_event_active%` | Check if any event is active | `Active` / `Disabled` |
| `%storage_event_active_<event_type>%` | Check if specific event is active | `%storage_event_active_mining_contest%` |
| `%storage_event_name%` | Name of currently active event | `Mining Contest` |
| `%storage_event_type%` | Type of currently active event | `Mining Contest` |
| `%storage_event_remaining_time%` | Time remaining in current event | `1h 30m 45s` |
| `%storage_event_remaining_seconds%` | Time remaining in seconds | `5445` |
| `%storage_event_duration%` | Total event duration in seconds | `1800` |
| `%storage_event_start_time%` | Event start timestamp (milliseconds) | `1704067200000` |
| `%storage_event_start_time_formatted%` | Event start time (formatted) | `14:30:15` |
| `%storage_event_start_date%` | Event start date | `01/01/2024` |
| `%storage_event_start_datetime%` | Event start date and time | `01/01/2024 14:30:15` |
| `%storage_event_next_time%` | Time until next event | `2h 15m 30s` |
| `%storage_event_next_seconds%` | Seconds until next event | `8130` |
| `%storage_event_next_<event_type>_time%` | Time until specific event type | `%storage_event_next_mining_contest_time%` |
| `%storage_event_next_<event_type>_seconds%` | Seconds until specific event type | `%storage_event_next_mining_contest_seconds%` |
| `%storage_event_next_<event_type>_datetime%` | Full date and time of next event | `%storage_event_next_mining_contest_datetime%` |
| `%storage_event_next_<event_type>_date%` | Date of next event | `%storage_event_next_mining_contest_date%` |
| `%storage_event_next_<event_type>_schedule_info%` | Schedule configuration info | `%storage_event_next_mining_contest_schedule_info%` |

#### Mining Contest Placeholders
| Placeholder | Description | Example |
|-------------|-------------|---------|
| `%storage_mining_contest_rank%` | Player's current rank in contest | `3` |
| `%storage_mining_contest_score%` | Player's current score | `1250` |
| `%storage_mining_contest_participants%` | Number of participants | `15` |
| `%storage_mining_contest_top_<position>_name%` | Name of player at position | `%storage_mining_contest_top_1_name%` |
| `%storage_mining_contest_top_<position>_score%` | Score of player at position | `%storage_mining_contest_top_1_score%` |

#### Community Event Placeholders
| Placeholder | Description | Example |
|-------------|-------------|---------|
| `%storage_community_progress%` | Current community progress | `7500` |
| `%storage_community_goal%` | Community goal target | `10000` |
| `%storage_community_percentage%` | Progress percentage | `75.0` |
| `%storage_community_participants%` | Number of participants | `25` |
| `%storage_community_player_contribution%` | Player's contribution | `150` |

#### Double Drop Event Placeholders
| Placeholder | Description | Example |
|-------------|-------------|---------|
| `%storage_double_drop_multiplier%` | Current multiplier for double drop | `2.0` |
| `%storage_double_drop_player_blocks%` | Blocks mined by player in event | `500` |

**Note**: Event status placeholders (`active`) use message keys from `message.yml`:
- `events.status.active` = "Active"
- `events.status.disabled` = "Disabled"

You can customize these messages by editing the `message.yml` file.

### MythicStorage Placeholders

#### Basic MythicStorage Placeholders
| Placeholder | Description | Example |
|-------------|-------------|---------|
| `%storage_mythic_<item>%` | Display name of specific MythicMobs item | `&6Royal Crown` |
| `%storage_mythic_<item>_amount%` | Amount of specific MythicMobs item | `150` |
| `%storage_mythic_<item>_amount_formatted%` | Formatted amount | `15.2K` |
| `%storage_mythic_total_items%` | Total number of unique items stored | `5` |
| `%storage_mythic_total_amount%` | Total amount of all items combined | `1250` |
| `%storage_mythic_total_amount_formatted%` | Total amount formatted | `1.3K` |
| `%storage_mythic_max_storage%` | Maximum MythicStorage capacity | `5000` |
| `%storage_mythic_percentage%` | MythicStorage percentage used | `25` |
| `%storage_mythic_available_space%` | Available MythicStorage space | `3750` |
| `%storage_mythic_autopickup_status%` | Auto-pickup status (Enabled/Disabled) | `Enabled` |

#### MythicStorage Transfer Statistics
| Placeholder | Description | Example |
|-------------|-------------|---------|
| `%storage_mythic_transfer_sent_total%` | Total items sent to other players | `500` |
| `%storage_mythic_transfer_received_total%` | Total items received from others | `300` |
| `%storage_mythic_transfer_count%` | Total number of transfers | `25` |

#### MythicStorage Leaderboard
| Placeholder | Description | Example |
|-------------|-------------|---------|
| `%storage_mythic_top_<item>_<position>_name%` | Name of player at position for specific item | `%storage_mythic_top_crown_1_name%` |
| `%storage_mythic_top_<item>_<position>_amount%` | Amount of player at position for specific item | `%storage_mythic_top_crown_1_amount%` |

**Note**: 
- Replace `<item>` with your MythicMobs item ID (e.g., `crown`, `rare_gem`)
- Replace `<position>` with rank number (1, 2, 3, etc.)
- Leaderboard placeholders only show online players
- Status placeholders use message keys from `message.yml`:
  - `mythicstorage.status_enabled` = "Enabled"
  - `mythicstorage.status_disabled` = "Disabled"

## Developer API

Storage provides a comprehensive API for developers to integrate with the storage system. Perfect for creating economy plugins, custom GUIs, or extending functionality.

### Adding Storage to Your Project

#### Maven

Add repository Storage to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.hongminh54</groupId>
        <artifactId>Storage</artifactId>
        <version>2.3.3</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

#### Gradle

Add repository Storage to your `build.gradle`:

```gradle
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    compileOnly 'com.github.hongminh54:Storage:2.3.3'
}
```

#### Gradle (Kotlin DSL)

For `build.gradle.kts`:

```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("com.github.hongminh54:Storage:2.3.3")
}
```

### Quick Start

```yml
# Add to your plugin.yml
depend: [Storage]
# softdepend if you want to make your plugin optional
softdepend: [Storage]
```

### Core API Examples

#### Basic Storage Operations
```java
import net.danh.storage.API.StorageAPI;
import net.danh.storage.API.StoragePlayer;
import net.danh.storage.API.events.StorageDepositEvent;

// Check API availability
if (StorageAPI.isInitialized()) {
    // Get player storage wrapper
    StoragePlayer player = StorageAPI.getStoragePlayer(bukkitPlayer);

    // Add items to storage
    player.addItem("STONE;0", 64);

    // Transfer items between players
    player.transferTo(otherPlayer, "DIAMOND;0", 10);

    // Check storage status
    int totalItems = player.getTotalStoredItems();
    boolean isFull = player.isStorageFull();
    double usagePercent = player.getStorageUsagePercentage();
}
```

#### MythicMobs Storage API
```java
import net.danh.storage.API.MythicStorageAPI;

// Check if MythicStorage is enabled
if (MythicStorageAPI.isSystemEnabled()) {
    // Add MythicMobs items
    MythicStorageAPI.addMythicItem(player, "crown", 5);
    
    // Get item amount
    int amount = MythicStorageAPI.getMythicItemAmount(player, "crown");
    
    // Toggle auto-pickup
    MythicStorageAPI.setMythicAutoPickup(player, true);
    
    // Get all stored items
    Map<String, Integer> items = MythicStorageAPI.getPlayerMythicItems(player);
}
```

#### Event System API
```java
import net.danh.storage.API.StorageEventAPI;
import net.danh.storage.Event.EventType;

// Start/Stop events
StorageEventAPI.startEvent(EventType.MINING_CONTEST);
StorageEventAPI.stopEvent(EventType.DOUBLE_DROP);

// Check event status
boolean isActive = StorageEventAPI.isEventActive(EventType.MINING_CONTEST);
long remainingTime = StorageEventAPI.getEventRemainingTime(EventType.MINING_CONTEST);

// Get Mining Contest data
int score = StorageEventAPI.getMiningContestScore(player);
int rank = StorageEventAPI.getMiningContestRank(player);
Map<String, Integer> leaderboard = StorageEventAPI.getMiningContestLeaderboard();

// Get Community Event progress
int progress = StorageEventAPI.getCommunityEventProgress();
double percentage = StorageEventAPI.getCommunityEventPercentage();
```

#### Batch Operations API
```java
import net.danh.storage.API.StorageBatchAPI;

// Add multiple items at once
Map<String, Integer> itemsToAdd = new HashMap<>();
itemsToAdd.put("DIAMOND;0", 64);
itemsToAdd.put("EMERALD;0", 32);
Map<String, Boolean> results = StorageBatchAPI.addItems(player, itemsToAdd);

// Transfer multiple items
Map<String, Boolean> transferResults = StorageBatchAPI.transferMultipleItems(
    sender, receiver, itemsToAdd);

// Async operations for better performance
CompletableFuture<Map<String, Boolean>> future = 
    StorageBatchAPI.addItemsAsync(player, itemsToAdd);
future.thenAccept(result -> {
    // Handle results asynchronously
});
```

#### Hook System API
```java
import net.danh.storage.API.StorageHookAPI;
import net.danh.storage.API.StorageHookAPI.*;

// Register deposit hook
StorageHookAPI.registerDepositHook(new DepositHook() {
    @Override
    public boolean onBeforeDeposit(Player player, String material, int amount) {
        // Return false to cancel deposit
        return true;
    }
    
    @Override
    public void onAfterDeposit(Player player, String material, int amount) {
        // Custom logic after deposit
        player.sendMessage("Deposited " + amount + " " + material);
    }
}, HookPriority.HIGH);

// Register transfer hook
StorageHookAPI.registerTransferHook(new TransferHook() {
    @Override
    public boolean onBeforeTransfer(Player sender, Player receiver, 
                                    String material, int amount) {
        // Custom validation
        return true;
    }
    
    @Override
    public void onAfterTransfer(Player sender, Player receiver, 
                                String material, int amount) {
        // Log transfer or custom logic
    }
});
```

#### Event Listeners
```java
import net.danh.storage.API.events.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class StorageListener implements Listener {
    
    @EventHandler
    public void onDeposit(StorageDepositEvent event) {
        Player player = event.getPlayer();
        String material = event.getMaterial();
        int amount = event.getAmount();
        
        // Modify amount or cancel
        event.setAmount(amount * 2); // Double deposit
        // event.setCancelled(true); // Cancel deposit
    }
    
    @EventHandler
    public void onMythicDeposit(MythicStorageDepositEvent event) {
        // Handle MythicMobs item deposits
    }
    
    @EventHandler
    public void onTransfer(StorageTransferEvent event) {
        Player sender = event.getSender();
        Player receiver = event.getReceiver();
        // Custom transfer logic
    }
}
```

#### Material Conversion API
```java
import net.danh.storage.API.ConvertAPI;

// Check if material can be converted
boolean canConvert = ConvertAPI.isConvertibleMaterial("IRON_INGOT;0");

// Get conversion options
List<ConvertOption> options = ConvertAPI.getConversionOptions("IRON_INGOT;0");

// Convert materials
boolean success = ConvertAPI.convertMaterial(player, "IRON_INGOT;0", "IRON_BLOCK;0", 9);

// Get max conversions possible
int maxConversions = ConvertAPI.getMaxConversions(player, "IRON_INGOT;0", "IRON_BLOCK;0");

// Calculate result amount
int resultAmount = ConvertAPI.calculateResultAmount("IRON_INGOT;0", "IRON_BLOCK;0", 18);

// Batch convert multiple materials
Map<String, ConversionRequest> conversions = new HashMap<>();
conversions.put("IRON_INGOT;0", new ConversionRequest("IRON_BLOCK;0", 9));
conversions.put("GOLD_INGOT;0", new ConversionRequest("GOLD_BLOCK;0", 9));
Map<String, Boolean> results = ConvertAPI.batchConvert(player, conversions);
```

#### Special Materials API
```java
import net.danh.storage.API.SpecialMaterialAPI;

// Check if special material exists
boolean exists = SpecialMaterialAPI.hasSpecialMaterial("rare_gem");

// Get all special materials
Set<String> materials = SpecialMaterialAPI.getAllSpecialMaterials();

// Give special material to player
SpecialMaterialAPI.giveSpecialMaterial(player, "rare_gem", 5);

// Get material info
String info = SpecialMaterialAPI.getMaterialInfo("rare_gem");

// Check for drops when block is broken
SpecialMaterialAPI.checkSpecialMaterialDrop(player, block);

// With enchant modifier
SpecialMaterialAPI.checkSpecialMaterialDrop(player, block, "multiplier");
```

#### Async Operations API
```java
import net.danh.storage.API.StorageAsyncAPI;

// Async add item (non-blocking)
CompletableFuture<Boolean> future = StorageAsyncAPI.addItemAsync(player, "DIAMOND;0", 64);
future.thenAccept(success -> {
    if (success) {
        player.sendMessage("Items added successfully!");
    }
});

// Async get with callback on main thread
StorageAsyncAPI.executeWithCallback(
    StorageAsyncAPI.getItemAmountAsync(player, "DIAMOND;0"),
    new AsyncCallback<Integer>() {
        @Override
        public void onSuccess(Integer amount) {
            player.sendMessage("You have " + amount + " diamonds");
        }
        
        @Override
        public void onError(Throwable error) {
            player.sendMessage("Error: " + error.getMessage());
        }
    }
);

// Async top players
StorageAsyncAPI.getTopPlayersByMaterialAsync("DIAMOND;0", 10)
    .thenAccept(topPlayers -> {
        // Display leaderboard
    });

// Async save all players
StorageAsyncAPI.saveAllPlayersAsync()
    .thenAccept(count -> {
        System.out.println("Saved " + count + " players");
    });
```

#### Statistics & Analytics API
```java
import net.danh.storage.API.StorageStatsAPI;

// Get player statistics
StorageStats stats = StorageStatsAPI.getPlayerStats(player);
System.out.println(stats); // Formatted output

// Get top stored materials for player
Map<String, Integer> topMaterials = StorageStatsAPI.getTopStoredMaterials(player, 5);

// Get player's most stored material
Optional<String> topMaterial = StorageStatsAPI.getMostStoredMaterial(player);

// Get server-wide statistics
ServerStats serverStats = StorageStatsAPI.getServerStats();
System.out.println("Total items: " + serverStats.totalItems);
System.out.println("Average usage: " + (serverStats.averageUsage * 100) + "%");

// Get top players by material
Map<String, Integer> topPlayers = StorageStatsAPI.getTopPlayersByMaterial("DIAMOND;0", 10);

// Get top players by total storage
Map<String, Long> topByTotal = StorageStatsAPI.getTopPlayersByTotal(10);

// Get player ranking
int rank = StorageStatsAPI.getPlayerRank(player, "DIAMOND;0");
int totalRank = StorageStatsAPI.getPlayerTotalRank(player);

// Get material distribution
Map<String, Integer> distribution = StorageStatsAPI.getMaterialDistribution("DIAMOND;0");
```

#### GUI Builder API
```java
import net.danh.storage.API.StorageGUIAPI;
import net.danh.storage.API.StorageGUIAPI.GUIBuilder;

// Create a simple GUI
GUIBuilder gui = StorageGUIAPI.createGUI("&6My Custom Storage", 3)
    .setFillerPanel(new ItemStack(Material.GRAY_STAINED_GLASS_PANE))
    .setBorderPanel(new ItemStack(Material.BLACK_STAINED_GLASS_PANE))
    .addItem(13, new ItemStack(Material.DIAMOND), (player, clickType) -> {
        player.sendMessage("You clicked a diamond!");
    });

// Open for player
gui.open(player);

// Create storage viewer GUI
Inventory storageView = StorageGUIAPI.createStorageViewer(player, "&ePlayer Storage");
player.openInventory(storageView);

// Create material selector
StorageGUIAPI.createMaterialSelector("&aSelect Material", (material) -> {
    player.sendMessage("You selected: " + material);
}).open(player);

// Advanced GUI with navigation
GUIBuilder advancedGUI = StorageGUIAPI.createGUI("&6Advanced GUI", 6)
    .addStorageItem(10, player, "DIAMOND;0")
    .addStorageItem(11, player, "EMERALD;0")
    .addNavigation(45, 53, 
        (p) -> p.sendMessage("Previous page"),
        (p) -> p.sendMessage("Next page")
    );

// Pagination helper
List<ItemStack> items = new ArrayList<>();
// ... add items
PaginationHelper pagination = new PaginationHelper(items, 45);
List<ItemStack> page1 = pagination.getPage(0);
boolean hasNext = pagination.hasNextPage(0);
```

### Available APIs

| API Class | Description |
|-----------|-------------|
| `StorageAPI` | Core storage operations (add, remove, transfer) |
| `StoragePlayer` | Player-specific storage wrapper |
| `StorageItem` | Item wrapper with storage utilities |
| `MythicStorageAPI` | MythicMobs item storage operations |
| `StorageEventAPI` | Server event management (contests, double drop) |
| `StorageBatchAPI` | Bulk operations for better performance |
| `StorageHookAPI` | Plugin integration hooks |
| `ConvertAPI` | Material conversion system (ingots ↔ blocks) |
| `SpecialMaterialAPI` | Rare materials with custom effects |
| `StorageAsyncAPI` | Asynchronous operations (non-blocking) |
| `StorageStatsAPI` | Statistics and analytics |
| `StorageGUIAPI` | GUI builder for custom interfaces |

### Events

| Event | Description |
|-------|-------------|
| `StorageDepositEvent` | Fired when player deposits items |
| `StorageWithdrawEvent` | Fired when player withdraws items |
| `StorageTransferEvent` | Fired when items are transferred |
| `StorageToggleEvent` | Fired when auto-pickup is toggled |
| `MythicStorageDepositEvent` | Fired when MythicMobs items are deposited |
| `MythicStorageWithdrawEvent` | Fired when MythicMobs items are withdrawn |
| `MaterialConvertEvent` | Fired when materials are converted |
| `SpecialMaterialDropEvent` | Fired when checking for special material drops |

### Documentation

- **[JavaDoc](src/main/java/net/danh/storage/API/)** - Detailed method documentation

## Dependencies

[![placeholderapi](https://img.shields.io/badge/PlaceholderAPI-2.11.6-blue?style=badge)](https://www.spigotmc.org/resources/6245/)
[![worldguard](https://img.shields.io/badge/WorldGuard-v6/v7-blue?style=badge)](https://dev.bukkit.org/projects/worldguard)
[![mythicmobs](https://img.shields.io/badge/MythicMobs-4.x.x/5.x.x-orange?style=badge)](https://www.spigotmc.org/resources/5702/)

## Contributing

We welcome contributions from the community! Here's how you can help:

- **Bug Reports/Feature Requests** - Found an issue?/Have ideas? [Click here!](https://github.com/hongminh54/Storage/issues)
- **Code Contributions** - Submit pull requests for improvements
- **Documentation** - Help improve our documentation
- **Testing** - Test new features and report feedback

## Special Thanks

- **VoChiDanh** - Original plugin creator, thank you for allowing continued development
- **Community Contributors** - Thank you for your feedback and suggestions!
- **Plugin Developers** - Thanks for integrating with our API!

---
