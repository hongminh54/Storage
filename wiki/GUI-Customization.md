# GUI Customization

The Storage plugin contains a highly flexible GUI system. Every single menu, button, sound, and layout format can be fully customized by editing the files inside the `plugins/Storage/GUI/` directory.

## The `GUI` Directory
Inside this directory, you will find files for every interface in the plugin, including:
- `storage.yml` (Main storage menu)
- `mythicstorage.yml` (MythicMobs storage)
- `cropstorage.yml` (Crop storage)
- `convert-ore.yml` (Material conversion menu)
- `transfer.yml` & `transfer-multi.yml`
- `recipe-list.yml` & `recipe-editor.yml` (Crafting interfaces)
- and many more!

---

## Anatomy of a GUI File

Here is an example structure of a typical GUI file in the plugin:

```yaml
title: "&8Player Storage"
size: 54

items:
  # General filler item
  filler:
    material: "BLACK_STAINED_GLASS_PANE"
    name: " "
    slots:
      - 0, 1, 2, 3, 4, 5, 6, 7, 8
      - 45, 46, 47, 48, 49, 50, 51, 52, 53

  # Close Menu Button
  close:
    material: "BARRIER"
    name: "&cClose Menu"
    slot: 49
    action: "CLOSE"

  # Next Page Button
  next_page:
    material: "ARROW"
    name: "&aNext Page"
    slot: 53
    action: "NEXT_PAGE"
    
  # ... More custom buttons
```

### Components Supported

1. **`title`**: The display name at the top of the inventory window. Supports color codes and Placeholders.
2. **`size`**: Total slots. Must be a multiple of 9 (e.g., 9, 18, 27, 36, 45, 54).
3. **`items`**: The dynamic objects you can click.
   - `material`: The Minecraft material ID.
   - `name`: Display name of the item.
   - `lore`: Array of strings for the description. Supports Placeholders.
   - `slot` / `slots`: Set a single slot (0-53) or an array of multiple slots separated by commas.
   - `custom_model_data`: Integer value for resource pack modifications.
   - `amount`: Number of items in the stack.
   - `action`: The built-in action that triggers when the player clicks it (like `CLOSE`, `NEXT_PAGE`, `PREVIOUS_PAGE`, `BACK`, etc.).

---

## Adding Sounds to GUIs

You can bind sound effects directly to specific menus if a player opens it or interacts with certain buttons! Just add a `sounds` section to the GUI file you are editing:

```yaml
sounds:
  open:
    name: "BLOCK_CHEST_OPEN"
    volume: 1.0
    pitch: 1.2
```

## Dynamic Display of Saved Items
In files like `storage.yml` or `mythicstorage.yml`, there is an exact area dedicated to auto-filling items the player has saved. Be careful when positioning your filler glass, as you want to leave an open empty block grouping for the items to generate.

## Making Changes
Any time you modify a GUI file, you must reload the plugin config files using `/storage reload` or restart your server to apply the visual changes smoothly.
