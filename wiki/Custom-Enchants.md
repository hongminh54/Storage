# Custom Enchants

The plugin comes with a built-in Custom Enchant system designed to make mining significantly more rewarding and efficient.

## Available Enchants

### 1. TNT
**Effect:** Creates an explosion that mines blocks in an AoE (Area of Effect).
- Completely integrates with virtual storage (all exploded blocks go to storage safely).
- **Levels 1-5**: Radius and Explosion Power scale as the level increases.
- Level 5 can mine a massive 7x7 radius instantly.

### 2. Haste
**Effect:** Gives a passive Haste potion effect purely by holding the enchanted pickaxe.
- Speeds up block breaking.
- **Levels 1-5**: Increases the intensity of Haste.

### 3. Multiplier
**Effect:** Directly multiplies the drop amount recorded by the auto-pickup system.
- If a diamond ore normally drops 1 diamond, a level 3 Multiplier pickaxe will record 4 diamonds.
- Synergizes with Fortune!
- **Levels 1-3**.

### 4. Vein Miner
**Effect:** Breaks connected blocks of the exact same type instantly.
- Finding a massive coal vein? Break one block, and the entire vein pops right into your virtual storage.
- **Levels 1-3**: Scales up the maximum amount of connected blocks that can be broken in a single tick (up to 32 blocks at level 3).

---

## How to Configuration (`enchants.yml`)

Every single enchant can be rigorously changed:
```yaml
  tnt:
    name: "TNT"
    max_level: 5
    applicable_items:
      - "DIAMOND_PICKAXE"
      - "NETHERITE_PICKAXE"
```
You can also tweak values per level, adding cooldowns to prevent lag:
```yaml
    levels:
      1:
        explosion_power: 2.0
        cooldown_ticks: 60
        radius: 3
```

- Enable/Disable `glow_effect`.
- Edit `particles` or `sounds` when the enchant triggers.
- Fully customize the `lore_format`. 
