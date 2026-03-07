# Commands and Permissions

This page lists all the commands and permissions available in the Storage plugin, categorized by feature.

## Main Storage

### User Commands
| Command | Description | Permission |
|---------|-------------|------------|
| `/storage` or `/kho` | Open the main storage GUI | - |
| `/storage help` | Show help information | - |
| `/storage toggle` | Toggle auto-pickup on/off | `storage.toggle` |
| `/storage groundstore` | Toggle ground-store mode | `storage.groundstore` |
| `/storage autosell` | Toggle per-item auto sell (admin by default) | `storage.autosell` |
| `/storage convert` | Open material conversion GUI | `storage.convert` |
| `/storage craft` | Open crafting GUI | `storage.craft.use` |
| `/storage view <player>` | View another player's storage | `storage.view` |
| `/storage transfer <player> <material>` | Transfer material to another player | `storage.transfer.use` |
| `/storage transfer multi <player>` | Open multi-transfer GUI | `storage.transfer.multi` |
| `/storage transfer log [player] [page]` | View transfer history | `storage.transfer.log` (own), `storage.transfer.log.others` (others) |
| `/storage deposit <item> [amount\|all]` | Deposit items from inventory into Storage | `storage.deposit` |
| `/storage withdraw <item> [amount\|all]` | Withdraw items from Storage into inventory | `storage.withdraw` |
| `/storage sell <item> [amount\|all]` | Sell items from Storage for money | `storage.sell` |

### Admin Commands
| Command | Description | Permission |
|---------|-------------|------------|
| `/storage reload` | Reload all configuration files | `storage.admin.reload` |
| `/storage autosave` | Show auto-save system status | `storage.admin.reload` |
| `/storage save` | Force save all player data | `storage.admin.reload` |
| `/storage max <player> <amount>`| Set max storage limit for a player | `storage.admin.max` |
| `/storage resetlimit <player>` | Reset max storage limit override for a player | `storage.admin.resetlimit` |
| `/storage add <material;data> <player> <amount>` | Add items to player storage | `storage.admin.add` |
| `/storage remove <material;data> <player> <amount>` | Remove items from player storage | `storage.admin.remove` |
| `/storage set <material;data> <player> <amount>` | Set exact item count for a player | `storage.admin.set` |
| `/storage reset <material\|all> [player]` | Reset storage materials for players | `storage.admin.reset` |
| `/storage crafteditor` | Open crafting editor GUI | `storage.craft.admin` |
| `/storage crafteditor import` | Import held item to a recipe | `storage.craft.admin` |

## MythicMobs Storage

### User & Admin Commands
| Command | Description | Permission |
|---------|-------------|------------|
| `/mythicstorage` | Open MythicStorage GUI | `storage.mythicstorage.use` |
| `/mythicstorage toggle` | Toggle auto-pickup for Mythic items | `storage.mythicstorage.toggle` |
| `/mythicstorage groundstore` | Toggle MythicStorage ground-store mode | `storage.mythicstorage.groundstore` |
| `/mythicstorage view <player>` | View a player's MythicStorage | `storage.mythicstorage.view` |
| `/mythicstorage transfer <player> <item>`| Transfer a MythicMobs item | `storage.mythicstorage.transfer` |
| `/mythicstorage transfer multi <player>` | Open multi-transfer GUI for Mythic items | `storage.mythicstorage.transfer.multi` |
| `/mythicstorage transfer log [page]` | View your Mythic transfer logs | `storage.mythicstorage.transfer.log` |
| `/mythicstorage transfer log <player> <page>` | View other player's Mythic transfer logs | `storage.mythicstorage.transfer.log.others` |
| `/mythicstorage deposit <item> [amount\|all]` | Deposit Mythic items from inventory | `storage.mythicstorage.use` |
| `/mythicstorage withdraw <item> [amount\|all]` | Withdraw Mythic items to inventory | `storage.mythicstorage.use` |
| `/mythicstorage add <player> <item> <amount>` | Admin: Add Mythic items to a player | `storage.mythicstorage.admin` |
| `/mythicstorage remove <player> <item> <amount>` | Admin: Remove Mythic items from a player | `storage.mythicstorage.admin` |
| `/mythicstorage set <player> <item> <amount>` | Admin: Set Mythic item amount for a player | `storage.mythicstorage.admin` |
| `/mythicstorage max <player> <amount>` | Admin: Set MythicStorage max limit | `storage.mythicstorage.admin` |
| `/mythicstorage resetlimit <player>` | Admin: Reset MythicStorage max limit override | `storage.mythicstorage.admin` |
| `/mythicstorage reset <player> [item]` | Admin: Reset MythicStorage data | `storage.mythicstorage.admin` |
| `/mythicstorage reload` | Admin: Reload Mythic configs | `storage.mythicstorage.admin` |

## Crop Storage

### User & Admin Commands
| Command | Description | Permission |
|---------|-------------|------------|
| `/cropstorage` | Open CropStorage GUI | `storage.cropstorage.use` |
| `/cropstorage toggle` | Toggle auto-pickup for crops | `storage.cropstorage.toggle` |
| `/cropstorage groundstore` | Toggle CropStorage ground-store mode | `storage.cropstorage.groundstore` |
| `/cropstorage status` | Show CropStorage status | `storage.cropstorage.use` |
| `/cropstorage deposit <item> [amount\|all]` | Deposit crops from inventory | `storage.cropstorage.use` |
| `/cropstorage withdraw <item> [amount\|all]` | Withdraw crops to inventory | `storage.cropstorage.use` |
| `/cropstorage sell <item> [amount\|all]` | Sell crops for money | `storage.cropstorage.use` |
| `/cropstorage autosell` | Toggle per-item auto sell (admin by default) | `storage.cropstorage.autosell` |
| `/cropstorage view <player>`| View a player's CropStorage | `storage.cropstorage.view` |
| `/cropstorage add <item> <player> <amount>` | Admin: Add crops to a player | `storage.cropstorage.admin` |
| `/cropstorage remove <item> <player> <amount>` | Admin: Remove crops from a player | `storage.cropstorage.admin` |
| `/cropstorage set <item> <player> <amount>` | Admin: Set crop amount for a player | `storage.cropstorage.admin` |
| `/cropstorage max <player> <amount>` | Admin: Set CropStorage max limit | `storage.cropstorage.admin` |
| `/cropstorage resetlimit <player>` | Admin: Reset CropStorage max limit override | `storage.cropstorage.admin` |
| `/cropstorage reset <player> [item]` | Admin: Reset CropStorage data | `storage.cropstorage.admin` |
| `/cropstorage reload` | Admin: Reload CropStorage config | `storage.cropstorage.admin` |

## Event & Special Material Commands

### Event Commands
| Command | Description | Permission |
|---------|-------------|------------|
| `/storage event list` | List all active events | `storage.event.view` |
| `/storage event start <event>` | Start a specific event | `storage.event.admin` |
| `/storage event stop <event>` | Stop a specific event | `storage.event.admin` |

### Custom Enchant Commands
| Command | Description | Permission |
|---------|-------------|------------|
| `/storage enchant give <enchant> <level>` | Give custom enchant to item | `storage.admin.enchant` |
| `/storage enchant remove <enchant>` | Remove custom enchant from item | `storage.admin.enchant` |
| `/storage enchant list` | List all available enchants | `storage.admin.enchant` |
| `/storage enchant info <enchant>` | Show detailed enchant information | `storage.admin.enchant` |
| `/storage enchant setmaxlevel <enchant> <level>` | Set maximum level for enchant | `storage.admin.enchant` |

### Special Material Commands
| Command | Description | Permission |
|---------|-------------|------------|
| `/storage specialmaterial list` | List all special materials | `storage.admin.specialmaterial` |
| `/storage specialmaterial info <material>` | Show detailed material information | `storage.admin.specialmaterial` |
| `/storage specialmaterial give <player> <material> <amount>` | Give special material to player | `storage.admin.specialmaterial` |

---

## Storage Limit Permissions

Configure maximum storage capacity via permissions. Priority system: higher number = higher priority (overrides lower priority).

### Main Storage
Configured in `config.yml` under `storage_permissions`.

| Permission | Description |
|------------|-------------|
| `storage.storage.<tier>` | Role-based limit (e.g., `storage.storage.vip`) |
| `storage.storage.max.<n>` | Direct numeric limit (e.g., `storage.storage.max.50000` → storage limit will be 50,000) |

### CropStorage
Configured in `cropstorage.yml` under `crop_storage_permissions`.

| Permission | Description |
|------------|-------------|
| `storage.cropstorage.storage.<tier>` | Role-based limit (e.g., `storage.cropstorage.storage.vip`) |
| `storage.cropstorage.storage.max.<n>` | Direct numeric limit (e.g., `storage.cropstorage.storage.max.10000` → storage limit will be 10,000) |

### MythicStorage
Configured in `mythicstorage.yml` under `mythic_storage_permissions`.

| Permission | Description |
|------------|-------------|
| `storage.mythicstorage.storage.<tier>` | Role-based limit (e.g., `storage.mythicstorage.storage.vip`) |
| `storage.mythicstorage.storage.max.<n>` | Direct numeric limit (e.g., `storage.mythicstorage.storage.max.10000` → storage limit will be 10,000) |

> **Note**: If a player has multiple numeric `max.<n>` permissions, the highest value will be used.
