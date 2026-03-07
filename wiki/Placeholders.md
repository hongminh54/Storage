# Placeholders (PlaceholderAPI)

The Storage plugin provides a comprehensive set of placeholders to use with [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/). 
Make sure you have PlaceholderAPI installed and initialized!

## Main Storage Placeholders

### Overview & Statistics

| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%storage_status%` | Check auto-pickup status | `On`/`Off` |
| `%storage_max_storage%` | Show maximum storage limit | `100000` |
| `%storage_percentage%` | Show storage filled percentage | `67.5` |
| `%storage_total_materials%` | Count of unique stored material types | `12` |
| `%storage_total_blocks%` | Total sum of all stored blocks | `50000` |
| `%storage_total_blocks_formatted%` | Formatted total blocks | `50.0K` |
| `%storage_available_space%` | Remaining storage capacity | `50000` |
| `%storage_available_space_formatted%` | Formatted available space | `50.0K` |

### Item-Specific Placeholders
*(Replace `<MATERIAL>` with the exact key from your `config.yml`, e.g., `DIAMOND;0`)*

| Placeholder | Description |
|-------------|-------------|
| `%storage_storage_<MATERIAL>%` | Show raw stored amount |
| `%storage_storage_<MATERIAL>_formatted%` | Show formatted amount (e.g., `15.2K`) |
| `%storage_storage_<MATERIAL>_percentage%` | Percentage of material relative to max capacity |
| `%storage_sellable%` | Get the configured sellable symbol (from `message.yml` keys `user.sellable.yes/no`) |
| `%storage_sellable_<MATERIAL>%` | Check if this material is sellable (returns your configured symbol from `message.yml` keys `user.sellable.yes/no`) |
| `%storage_price_<MATERIAL>%` | Show the sell price of this material |

### Transfer Placeholders

| Placeholder | Description |
|-------------|-------------|
| `%storage_transfer_sent_total%` | Total amount of blocks you've sent |
| `%storage_transfer_received_total%` | Total amount of blocks you've received |
| `%storage_transfer_count%` | Total number of transfer transactions |

### GUI Pages Placeholders

| Placeholder | Description |
|-------------|-------------|
| `%storage_page_storage_current%` | Current page in Main Storage GUI |
| `%storage_page_storage_total%` | Total pages in Main Storage GUI |
| `%storage_page_mythic_current%` | Current page in MythicStorage GUI |
| `%storage_page_mythic_total%` | Total pages in MythicStorage GUI |

---

## MythicMobs Storage Placeholders

*(Replace `<item>` with the internal MythicMobs item ID, e.g., `crown`)*

| Placeholder | Description |
|-------------|-------------|
| `%storage_mythic_<item>%` | Retrieve the display name of the Mythic item |
| `%storage_mythic_<item>_amount%` | Show stored amount of the Mythic item |
| `%storage_mythic_<item>_amount_formatted%` | Show formatted stored amount |
| `%storage_mythic_total_items%` | Count of unique stored Mythic items |
| `%storage_mythic_total_amount%` | Sum of all stored Mythic items |
| `%storage_mythic_total_amount_formatted%` | Formatted total amount |
| `%storage_mythic_max_storage%` | Maximum MythicStorage limit |
| `%storage_mythic_percentage%` | Storage filled percentage |
| `%storage_mythic_available_space%` | Remaining capacity |
| `%storage_mythic_autopickup_status%` | Auto-pickup status (`Enabled`/`Disabled`) |
| `%storage_mythic_transfer_sent_total%` | Total Mythic items sent |
| `%storage_mythic_transfer_received_total%` | Total Mythic items received |
| `%storage_mythic_transfer_count%` | Total Mythic items transfer transactions |

Note: The MythicMobs item ID can be found in the yml files at `\plugins\MythicMobs\items`

---

## CropStorage Placeholders

*(Replace `<MATERIAL>` with the crop material key, e.g., `WHEAT` or `WHEAT;0` depending on your config)*

| Placeholder | Description |
|-------------|-------------|
| `%storage_crop_sellable%` | Get the configured CropStorage sellable symbol (from `message.yml` keys `cropstorage.sellable.yes/no`) |
| `%storage_crop_sellable_<MATERIAL>%` | Check if this crop is sellable (returns your configured symbol from `message.yml` keys `cropstorage.sellable.yes/no`) |

Note: The PlaceholderAPI expansion currently exposes CropStorage sellable placeholders only.

---

## Crafting Placeholders
*(Crafting placeholders use `%storagecraft_...%` instead of `%storage_...%`)*

| Placeholder | Description |
|-------------|-------------|
| `%storagecraft_total_recipes%` | Total recipes registered |
| `%storagecraft_enabled_recipes%` | Total enabled recipes |
| `%storagecraft_available_recipes%` | Recipes the player has permission to see |
| `%storagecraft_craftable_recipes%` | Recipes the player currently has materials to craft |
| `%storagecraft_is_crafting%` | Check if player is currently crafting (`true`/`false`) |

**Recipe-Specific (`<recipeId>`):**

| Placeholder | Description |
|-------------|-------------|
| `%storagecraft_recipe_<recipeId>_exists%` | Checks if recipe exists |
| `%storagecraft_recipe_<recipeId>_enabled%` | Checks if recipe is enabled |
| `%storagecraft_recipe_<recipeId>_can_craft%` | Checks if player can craft the recipe |
| `%storagecraft_recipe_<recipeId>_max_amount%` | Max times the player can craft this recipe |
| `%storagecraft_recipe_<recipeId>_name%` | Display name of the resulting item |

---

## Event Placeholders

### General Events

| Placeholder | Description |
|-------------|-------------|
| `%storage_event_active%` | Is any event active? (`Active`/`Disabled`) |
| `%storage_event_active_<event_type>%` | Is a specific event active? |
| `%storage_event_name%` | Active event name |
| `%storage_event_type%` | Active event type (display name) |
| `%storage_event_remaining_time%` | Formatted remaining time (e.g., `1h 30m 45s`) |
| `%storage_event_remaining_seconds%` | Remaining time in seconds |
| `%storage_event_duration%` | Total event duration in seconds |
| `%storage_event_start_time%` | Event start timestamp (milliseconds) |
| `%storage_event_start_time_formatted%` | Event start time (formatted `HH:mm:ss`) |
| `%storage_event_start_date%` | Event start date (formatted `dd/MM/yyyy`) |
| `%storage_event_start_datetime%` | Event start date & time (formatted `dd/MM/yyyy HH:mm:ss`) |
| `%storage_event_next_time%` | Formatted time until next event |
| `%storage_event_next_seconds%` | Seconds until next event |

**Next event (by type):**

| Placeholder | Description |
|-------------|-------------|
| `%storage_event_next_<event_type>_time%` | Formatted time until next specific event |
| `%storage_event_next_<event_type>_seconds%` | Seconds until next specific event |
| `%storage_event_next_<event_type>_datetime%` | Next schedule datetime (`dd/MM/yyyy HH:mm:ss`) |
| `%storage_event_next_<event_type>_date%` | Next schedule date (`dd/MM/yyyy`) |
| `%storage_event_next_<event_type>_schedule_info%` | Raw schedule/interval info from config |

### Mining Contest

| Placeholder | Description |
|-------------|-------------|
| `%storage_mining_contest_rank%` | Player's rank |
| `%storage_mining_contest_score%` | Player's score (blocks mined) |
| `%storage_mining_contest_participants%` | Total players participating |
| `%storage_mining_contest_top_<position>_name%` | Player name at a specific leaderboard position |
| `%storage_mining_contest_top_<position>_score%` | Score at a specific leaderboard position |

### Community Event

| Placeholder | Description |
|-------------|-------------|
| `%storage_community_progress%` | Global block progress |
| `%storage_community_goal%` | Total block goal |
| `%storage_community_percentage%` | Goal completion percentage |
| `%storage_community_participants%` | Total players participating |
| `%storage_community_player_contribution%` | Player's contribution |

### Double Drop

| Placeholder | Description |
|-------------|-------------|
| `%storage_double_drop_multiplier%` | Active double drop multiplier |
| `%storage_double_drop_player_blocks%` | Blocks mined by player during the event |

---

## Leaderboards


| Placeholder | Description |
|-------------|-------------|
| `%storage_top_<MATERIAL>_<position>_name%` | Top player name for a specific material |
| `%storage_top_<MATERIAL>_<position>_amount%` | Top player amount for a specific material |
| `%storage_top_all_<position>_name%` | Top player name for ALL combined materials |
| `%storage_top_all_<position>_amount%` | Top player amount for ALL combined materials |
| `%storage_mythic_top_<item>_<position>_name%` | Top player name for a specific MythicMobs item |
| `%storage_mythic_top_<item>_<position>_amount%`| Top player amount for a specific MythicMobs item |

> *Note: Leaderboards typically calculate top players from online players for performance.*
