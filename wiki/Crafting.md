# Crafting System

The **Crafting System** allows players to turn their massive amount of raw virtual resources into powerful custom items without having to withdraw items into their physical inventory.

Everything can be accessed directly using the `/storage craft` command. 

## The Crafting GUI

The interface automatically categorizes recipes to make them easy to find:
- **Tools & Weapons**
- **Armor & Protection**
- **Blocks & Building**
- **Food & Consumables**
- **Materials & Resources**
- **Miscellaneous**
- **Special Items**
- **Custom**

All recipes with sufficient stored materials are highlighted. If you lack the materials, you can see exactly what you are missing.

---

## Creating Custom Recipes

Server administrators can create entirely new custom recipes through `/storage crafteditor` or by manually editing `plugins/Storage/crafting.yml`.

You can also import an item you're holding as a new crafting recipe using `/storage crafteditor import`. 

### Example Recipe

Here's an example of the configuration for a typical custom sword:

```yaml
recipes:
  example_diamond_sword:
    name: "&b&lEnhanced Diamond Sword"
    category: "tools"
    enabled: true

    # Result Item - This is what the player gets!
    result:
      material: "DIAMOND_SWORD"
      name: "&b&lEnhanced Diamond Sword"
      lore:
        - "&7A powerful sword forged"
        - "&7from rare materials"
        - ""
        - "&6&lLEGENDARY WEAPON"
      amount: 1
      custom_model_data: 0
      unbreakable: true
      enchantments:
        DAMAGE_ALL: 5
        DURABILITY: 3
        FIRE_ASPECT: 2
      flags:
        - "HIDE_ENCHANTS"
        - "HIDE_ATTRIBUTES"

    # Requirements - Items consumed from Virtual Storage
    requirements:
      materials:
        'DIAMOND;0': 10
        'IRON_INGOT;0': 5
        'GOLD_INGOT;0': 3
      permissions:
        - "storage.craft.enhanced"
```

### Explanation of Fields
- `category`: Maps to one of the predefined GUI menus.
- `result`: Defines the output item (Material, name, lore, enchants, amount).
- `requirements.materials`: The items the player needs in their **Virtual Storage**. Formatted as `MATERIAL;DATA`.
- `requirements.permissions`: A list of permission nodes required to see/craft this item.

---

## Features

- **Delay & Animations**: You can add a processing delay when a player clicks "Craft", complete with success/failure sound effects and particles!
- **MythicStorage Support**: Want to require a MythicMobs drop to craft an item? You can easily mix Mythic items and vanilla items in the required materials list.
- **In-game Import**: Hold an item you created using another plugin, run `/storage crafteditor import`, and it acts as an easy template!
