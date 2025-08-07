# Storage [1.8.x - 1.21.x]

> A comprehensive virtual storage system for Minecraft servers that allows players to automatically store mined blocks, transfer items between players, participate in server events, and manage their resources through an intuitive GUI system.

##  Features

- **Virtual Storage System** - Automatically store mined blocks in a virtual inventory
- **Convert System** - Convert materials between different forms (ingots ↔ blocks) with configurable ratios
- **Transfer System** - Send items to other players with single or multi-item transfers
- **Event System** - Participate in server-wide mining contests and special events
- **Multi-Version Support** - Compatible with Minecraft 1.8.x to 1.21.x
- **WorldGuard Integration** - Respect region protections
- **PlaceholderAPI Support** - Rich placeholder system for other plugins
- **Custom Enchant System** - Custom enchantments for tools with configurable effects, particles, and sounds
- **Special Material System** - Rare materials with custom effects, particles, and sounds that drop from mining
- **Developer API** - Comprehensive API for external plugin integration

> **Note**: This is a complete rework from v1. Please reset all configuration files when updating from v1 to v2.
> 
> **Reworked by**: hongminh54

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
| `/storage enchant give <player> <enchant> <level>` | Give custom enchant to player's held item | `storage.enchant.admin` |
| `/storage enchant remove <player> <enchant>` | Remove custom enchant from player's held item | `storage.enchant.admin`       |
| `/storage enchant list [player]` | List available enchants or player's item enchants | `storage.enchant.admin` |
| `/storage enchant info <enchant>` | Show detailed enchant information | `storage.enchant.admin` |
| `/storage enchant setmaxlevel <enchant> <level>` | Set maximum level for enchant | `storage.enchant.admin` |

**Available Enchants**: `tnt`, `haste`, `multiplier`, `veinminer`

### Special Material Commands
| Command | Description | Permission |
|---------|-------------|------------|
| `/storage specialmaterial list` | List all special materials | `storage.admin.specialmaterial` |
| `/storage specialmaterial info <material>` | Show detailed material information | `storage.admin.specialmaterial` |
| `/storage specialmaterial give <player> <material> <amount>` | Give special material to player | `storage.admin.specialmaterial` |

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

## Placeholders

### Storage Placeholders
| Placeholder | Description | Example |
|-------------|-------------|---------|
| `%storage_storage_<MATERIAL>%` | Show amount of material in storage | `%storage_storage_COAL;0%` |
| `%storage_max_storage%` | Show max storage capacity | `100000` |
| `%storage_price_<MATERIAL>%` | Show material's sell price | `%storage_price_COAL;0%` |
| `%storage_status%` | Show auto-pickup status (On/Off) | `On` |

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
| `%storage_event_next_<event_type>_time%` | Time until specific event type | `%storage_event_next_mining_contest_time%` |
| `%storage_event_next_<event_type>_seconds%` | Seconds until specific event type | `%storage_event_next_mining_contest_seconds%` |

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

## Developer API

Storage provides a comprehensive API for developers to integrate with the storage system. Perfect for creating economy plugins, custom GUIs, or extending functionality.

### Quick Start

```yml
# Add to your plugin.yml
depend: [Storage]
# softdepend if you want to make your plugin optional
softdepend: [Storage]
```
```java
// Import the API
import net.danh.storage.API.StorageAPI;
import net.danh.storage.API.StoragePlayer;
import net.danh.storage.API.events.StorageDepositEvent;

// Check API availability
if (StorageAPI.isInitialized()) {
    // Get player storage
    StoragePlayer player = StorageAPI.getStoragePlayer(bukkitPlayer);

    // Add items to storage
    player.addItem("STONE;0", 64);

    // Transfer items between players
    player.transferTo(otherPlayer, "DIAMOND;0", 10);

    // Work with enchants
    ItemStack item = bukkitPlayer.getItemInHand();
    if (StorageAPI.hasEnchant(item, "tnt")) {
        int level = StorageAPI.getEnchantLevel(item, "tnt");
        // Custom logic for enchanted items
    }

    // Manage special materials
    if (StorageAPI.hasSpecialMaterial("rare_gem")) {
        StorageAPI.giveSpecialMaterial(bukkitPlayer, "rare_gem", 1);
    }

    // Listen to storage events
    @EventHandler
    public void onStorageDeposit(StorageDepositEvent event) {
        // Custom logic when players deposit items
    }
}
```
### Documentation

- **[JavaDoc](src/main/java/net/danh/storage/API/)** - Detailed method documentation

## Dependencies

[![placeholderapi](https://img.shields.io/badge/PlaceholderAPI-2.11.6-blue?style=badge)](https://www.spigotmc.org/resources/6245/)
[![worldguard](https://img.shields.io/badge/WorldGuard-v6/v7-blue?style=badge)](https://dev.bukkit.org/projects/worldguard)

## Contributing

We welcome contributions from the community! Here's how you can help:

- **Bug Reports/Feature Requests** - Found an issue?/Have ideas? [Click here!](https://github.com/hongminh54/Storage/issues)
- **Code Contributions** - Submit pull requests for improvements
- **Documentation** - Help improve our documentation
- **Testing** - Test new features and report feedback

## Special Thanks

- **VoChiDanh** - Original plugin creator ❤️
- **Community Contributors** - Thank you for your feedback and suggestions!
- **Plugin Developers** - Thanks for integrating with our API!

---
