# Configuration Guide

The Storage plugin uses several configuration files found in the `plugins/Storage/` directory.

## File Breakdown
- `config.yml`: Main configuration (Database, Auto-sell, Limits, Drop Mappings).
- `mythicstorage.yml`: Configuration for the MythicMobs integration.
- `cropstorage.yml`: Configuration for Crop auto pickup and storage.
- `crafting.yml`: UI and default patterns for the Crafting GUI.
- `message.yml`: Every message displayed by the plugin can be translated here.
- `events.yml`: Configuration and schedules for server-wide events.
- `enchants.yml`: Custom enchantment settings and costs.
- `special_material.yml`: Special rare drops you can configure.

---

## Main Configuration (`config.yml`)

### Database Settings
You can choose the database type (`sqlite`, `yml`, or `mysql`).
```yaml
database:
  type: sqlite
  # If mysql, fill out the mysql section
```
*Note: A server restart is required after changing the database type.*

### Limits and Permissions
You can define max storage limits using permission nodes. This uses a priority system.
```yaml
storage_permissions:
  vip:
    max_storage: 10000
    priority: 1
  premium:
    max_storage: 50000
    priority: 2
```
If a player has `storage.storage.premium`, their max capacity will be `50,000`.

### Blocks & Drops Mapping (Crucial)
This is where you define which blocks go into the storage and what item they result in.
```yaml
blocks:
  COBBLESTONE;0:           # The block broken
    drop: COBBLESTONE;0    # The item stored
  IRON_ORE;0:
    drop: IRON_INGOT;0     # Store raw iron or ingot depending on your server version
```
- **Legacy (1.12.2 and below)**: You must use `MATERIAL;DATA`. Eg: `INK_SACK;4` for Lapis Lazuli.
- **Modern (1.13+)**: You still need to format it with `;0`. Eg: `DIAMOND;0`.

If you use **MMOItems AutoSmelt**, you can add an alternative drop:
```yaml
  IRON_ORE;0:
    drop: RAW_IRON;0
    drop_autosmelt: IRON_INGOT;0
```

### Auto-Sell & Worth
You can define the price of each block when a player auto-sells or manually sells from the GUI.
```yaml
sell_method: vault  # "vault", "playerpoints", or "commands"

worth:
  COBBLESTONE;0: 1
  DIAMOND;0: "7;vault"          # Specific override
  EMERALD;0: "8;playerpoints"   # Specific override
```

---

## MythicStorage (`mythicstorage.yml`)

Define which drops from MythicMobs should be captured into the separate virtual MythicStorage.

```yaml
# Items that can be added to the storage (MythicMobs/Items/<item>.yml)
# example:
# In MM 5.X.X
#test: <--- ID item mythicmobs will be used on configuration items_drop
#  Id: diamond
#  Display: '&dabc'
#  Lore:
#    - '&6A kingly crowl that grants'
#    - '&6the wearer unwavering power!'
#  Enchantments:
#    - PROTECTION_ENVIRONMENTAL:2
#    - PROTECTION_PROJECTILE:2
#    - PROTECTION_FIRE:2
#    - PROTECTION_EXPLOSIONS:2
#  Hide:
#    - ATTRIBUTES
#    - ENCHANTS
#  Attributes:
#    Head:
#      Health: 10
#      KnockbackResistance: 10
items_drop:
  - kingdom
```
Just like regular storage, you can set permission-based limits under `mythic_storage_permissions`.

---

## CropStorage (`cropstorage.yml`)

Works identically to main storage and mythic storage but tailored for farming.
```yaml
crop_block_mapping:
  WHEAT: WHEAT
  CARROTS: CARROT
  POTATOES: POTATO
  BEETROOTS: BEETROOT
  NETHER_WART: NETHER_WART
  SUGAR_CANE: SUGAR_CANE
  COCOA: COCOA_BEANS
  MELON: MELON_SLICE
  PUMPKIN: PUMPKIN
  SWEET_BERRY_BUSH: SWEET_BERRIES
  KELP: KELP
  KELP_PLANT: KELP
  BAMBOO: BAMBOO
  CACTUS: CACTUS
  TORCHFLOWER: TORCHFLOWER
  PITCHER_PLANT: PITCHER_PLANT
  CHORUS_PLANT: CHORUS_FRUIT
  CHORUS_FLOWER: CHORUS_FRUIT

items_drop:
  - WHEAT
  - CARROT
  - POTATO
  - BEETROOT
  - NETHER_WART
  - SUGAR_CANE
  - COCOA_BEANS
  - MELON_SLICE
  - MELON
  - PUMPKIN
  - SWEET_BERRIES
  - KELP
  - BAMBOO
  - CACTUS
  - TORCHFLOWER
  - PITCHER_PLANT
  - CHORUS_FRUIT
```
You can also configure "tall crops" like Sugar Cane or Cactus so the plugin correctly understands how to break and store them.

---

## Events (`events.yml`)

You can schedule automated events such as `mining_contest`, `double_drop`, or `community_event`.
```yaml
events:
  mining_contest:
    timing:
      type: "interval"       # or "schedule"
      interval: 7200         # Time in seconds (e.g., 2 hours)
```

By tweaking these configurations, you can fully mold the Storage plugin to your server's exact economy!
