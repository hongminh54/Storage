# Special Materials

Special Materials are rare, custom drops that occasionally trigger while a player is mining. They are completely customizable and are excellent incentives for high-level players.

## How it works
- Players mine normal blocks globally.
- Based on a % chance, a configured "Special Material" drops explicitly into their Virtual Storage.
- When it drops, massive particle bursts and sound effects play to hype up the reward.

## Configuration Example (`special_material.yml`)

Located in `plugins/Storage/special_material.yml`, you define unique drops:

```yaml
special_materials:
  rare_gem:
    item:
      material: "EMERALD"
      name: "&a&lRare Gem"
      lore:
        - "&7A mysterious gem found"
        - "&7deep in the mines"
      glow: true
      
    # Drop Chance Configuration
    drop_chance: 5.0  # 5% chance
    source_blocks:
      - "DIAMOND_ORE;0"
      - "EMERALD_ORE;0"
      
    # Amount Randomization
    amount:
      min: 1
      max: 2

    # Rewards feedback
    effects:
      sound:
        enabled: true
        name: "ENTITY_EXPERIENCE_ORB_PICKUP"
      particles:
        enabled: true
        type: "VILLAGER_HAPPY"
        count: 15
```

### Synergy with Custom Enchants
The `Vein Miner` and `TNT` enchants trigger so many block breaks that special material drops could occur too rapidly. 
- You can apply drop chance modifiers exclusively to enchantments. (e.g., `-50% chance if mined using TNT enchant`).
