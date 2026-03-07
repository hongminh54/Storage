# Mechanics

The mechanics of Storage simplify a player's interaction with the game world and completely automate mundane inventory sorting. Below is an explanation of the core mechanics.

## Auto-Pickup

The moment a player breaks a block that is configured in the plugin, it bypasses the ground and skips the player's physical inventory altogether.
- It goes straight into the **Virtual Storage**.
- A customizable subtitle or action bar message pops up telling the player how much they mined and their storage capacity.
- **Fortune Enchants** still apply to drops normally!

*Toggle:* `/storage toggle`

## Auto-Sell

A feature for players to sell items automatically.
- The plugin hooks into economy providers like **Vault**, **PlayerPoints**, or triggers **Commands**.
- With Auto-sell active, items are instantly sold for cash right as they are mined, skipping the storage completely.
- Perfect for those who don't want to sell resources manually.

**Note**: This feature is available for OreStorage and CropStorage only.

*Toggle (Admin permission default):* `/storage autosell`

## Ground-Store

Sometimes, items are already on the ground (e.g. from an explosion, an ally mining, or dropping from a chest).
- If the ground-store feature is enabled globally and toggled on for the player, ground items that match storage configurations will be instantly picked up into the player's virtual storage.

*Toggle:* `/storage groundstore`

## Crafting Integration

Because items are hoarded in virtual storage, you don't want players to have to withdraw items just to craft.
- The **Crafting System** (`/storage craft`) allows servers to set up custom recipes.
- These recipes actively pull materials directly from the virtual storage (and MythicStorage) behind the scenes.
- The output goes directly into the player's physical inventory.

## Convert Mechanism
Players who mine millions of blocks need compression.
- `/storage convert` opens a GUI.
- Convert Ingots into Blocks (e.g., 9 Iron Ingots -> 1 Iron Block) or Vice-Versa.
- Can be uniquely customized if some items have a `4:1` compression ratio (like Quartz).

## Protections
The plugin comes with multiple built-in protections:
- **Prevent Rebreak**: To avoid infinite money glitches, if a player places a block from their inventory and breaks it again, it will NOT go into Storage, and they will NOT gain event progress for it.
- **WorldGuard Integration**: Mining rules and block-break protections in protected regions are strictly obeyed. (For instance, you can't bypass region protection by using Auto-pickup).
