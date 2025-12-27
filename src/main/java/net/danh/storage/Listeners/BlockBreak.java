package net.danh.storage.Listeners;

import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.messages.ActionBar;
import com.cryptomorin.xseries.messages.Titles;
import net.danh.storage.Enchant.HasteEnchant;
import net.danh.storage.Enchant.MultiplierEnchant;
import net.danh.storage.Enchant.TNTEnchant;
import net.danh.storage.Enchant.VeinMinerEnchant;
import net.danh.storage.Manager.*;
import net.danh.storage.NMS.NMSAssistant;
import net.danh.storage.Storage;
import net.danh.storage.Utils.ChatUtils;
import net.danh.storage.Utils.File;
import net.danh.storage.Utils.Number;
import net.danh.storage.WorldGuard.WorldGuard;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.metadata.MetadataValue;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BlockBreak implements Listener {

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBreak(@NotNull BlockBreakEvent e) {
        Player p = e.getPlayer();
        Block block = e.getBlock();
        boolean breakable = MineManager.checkBreak(block);
        boolean inv_full = (p.getInventory().firstEmpty() == -1);
        if (Storage.isWorldGuardInstalled()) {
            if (!WorldGuard.handleForLocation(p, block.getLocation())) {
                return;
            }
        }
        if (File.getConfig().getBoolean("prevent_rebreak")) {
            if (isPlacedBlock(block)) return;
        }
        if (File.getConfig().contains("blacklist_world")) {
            if (File.getConfig().getStringList("blacklist_world").contains(p.getWorld().getName())) return;
        }

        if (MineManager.getToggleStatus(p) && breakable) {
            if (inv_full) {
                processInventoryItems(p);
            }
            String drop = MineManager.getDrop(block);
            if (drop != null) {
                int amount;
                ItemStack hand = p.getInventory().getItemInMainHand();
                Enchantment fortune = XEnchantment.FORTUNE.get();
                if (hand == null || hand.getType().name().equals("AIR") || hand.getAmount() <= 0 || fortune == null || !hand.containsEnchantment(fortune)) {
                    amount = getDropAmount(block);
                } else {
                    if (File.getConfig().getStringList("whitelist_fortune").contains(block.getType().name())) {
                        amount = Number.getRandomInteger(getDropAmount(block), getDropAmount(block) + hand.getEnchantmentLevel(fortune) + 2);
                    } else amount = getDropAmount(block);
                }

                // Apply multiplier enchant if present
                if (hand != null && !hand.getType().name().equals("AIR") && hand.getAmount() > 0 && EnchantManager.hasEnchant(hand, "multiplier")) {
                    int multiplierLevel = EnchantManager.getEnchantLevel(hand, "multiplier");
                    amount = MultiplierEnchant.calculateMultipliedAmount(p, amount, multiplierLevel);
                }

                int bonusAmount = EventManager.calculateDoubleDropBonus(amount);
                int totalAmount = amount + bonusAmount;

                if (MineManager.addBlockAmount(p, drop, totalAmount)) {
                    EventManager.onPlayerMine(p, drop, amount);
                    boolean actionBarEnabled = File.getConfig().getBoolean("mine.actionbar.enable");
                    boolean titleEnabled = File.getConfig().getBoolean("mine.title.enable");
                    if (actionBarEnabled || titleEnabled) {
                        String name = File.getConfig().getString("items." + drop);
                        String itemName = name != null ? name : drop.replace("_", " ");
                        String displayAmount = bonusAmount > 0 ? totalAmount + " (+" + bonusAmount + " bonus)" : String.valueOf(totalAmount);
                        int newStoredAmount = MineManager.getPlayerBlock(p, drop);
                        int maxStorage = MineManager.getMaxBlock(p);
                        String storageValue = String.valueOf(newStoredAmount);
                        String maxValue = String.valueOf(maxStorage);

                        if (actionBarEnabled) {
                            String template = File.getConfig().getString("mine.actionbar.action");
                            if (template != null) {
                                String msg = template
                                        .replace("#item#", itemName)
                                        .replace("#amount#", displayAmount)
                                        .replace("#storage#", storageValue)
                                        .replace("#max#", maxValue);
                                ActionBar.sendActionBar(Storage.getStorage(), p, ChatUtils.colorizewp(msg));
                            }
                        }
                        if (titleEnabled) {
                            String titleTemplate = File.getConfig().getString("mine.title.title");
                            String subtitleTemplate = File.getConfig().getString("mine.title.subtitle");
                            if (titleTemplate != null && subtitleTemplate != null) {
                                String title = titleTemplate
                                        .replace("#item#", itemName)
                                        .replace("#amount#", displayAmount)
                                        .replace("#storage#", storageValue)
                                        .replace("#max#", maxValue);
                                String subtitle = subtitleTemplate
                                        .replace("#item#", itemName)
                                        .replace("#amount#", displayAmount)
                                        .replace("#storage#", storageValue)
                                        .replace("#max#", maxValue);
                                Titles.sendTitle(p, ChatUtils.colorizewp(title),
                                        ChatUtils.colorizewp(subtitle));
                            }
                        }
                    }

                    if (new NMSAssistant().isVersionGreaterThanOrEqualTo(12)) {
                        e.setDropItems(false);
                    }
                    e.getBlock().getDrops().clear();
                } else {
                    StorageFullNotificationManager.sendStorageFullNotification(p);
                }
            }
        }

        // Trigger enchants regardless of autopickup status
        if (breakable) {
            ItemStack hand = p.getInventory().getItemInMainHand();
            if (hand != null && !hand.getType().name().equals("AIR") && hand.getAmount() > 0) {
                if (EnchantManager.hasEnchant(hand, "tnt")) {
                    int enchantLevel = EnchantManager.getEnchantLevel(hand, "tnt");
                    TNTEnchant.triggerExplosion(p, block.getLocation(), enchantLevel);
                }

                if (EnchantManager.hasEnchant(hand, "haste")) {
                    int enchantLevel = EnchantManager.getEnchantLevel(hand, "haste");
                    HasteEnchant.triggerHaste(p, enchantLevel);
                }

                if (EnchantManager.hasEnchant(hand, "veinminer")) {
                    int enchantLevel = EnchantManager.getEnchantLevel(hand, "veinminer");
                    VeinMinerEnchant.triggerVeinMiner(p, block.getLocation(), enchantLevel);
                }
            }
        }

        // Check for special material drops
        if (breakable) {
            SpecialMaterialManager.checkSpecialMaterialDrop(p, block);
        }
    }

    private void processInventoryItems(Player player) {
        final PlayerInventory inv = player.getInventory();
        final ItemStack[] items = inv.getContents();
        boolean inventoryChanged = false;

        for (int i = 0; i < items.length; i++) {
            final ItemStack itemStack = items[i];
            if (itemStack != null) {
                String drop = MineManager.getItemStackDrop(itemStack);
                if (drop != null) {
                    int amount = itemStack.getAmount();
                    if (MineManager.addBlockAmount(player, drop, amount)) {
                        items[i] = null;
                        inventoryChanged = true;
                    }
                }
            }
        }

        if (inventoryChanged) {
            inv.setContents(items);
            if (MineManager.isBefore9()) {
                player.updateInventory();
            }
        }
    }

    public void removeItems(Player player, ItemStack itemStack, long amount) {
        final PlayerInventory inv = player.getInventory();
        final ItemStack[] items = inv.getContents();
        int c = 0;
        for (int i = 0; i < items.length; ++i) {
            final ItemStack is = items[i];
            if (is != null) {
                if (itemStack != null) {
                    if (is.isSimilar(itemStack)) {
                        if (c + is.getAmount() > amount) {
                            final long canDelete = amount - c;
                            is.setAmount((int) (is.getAmount() - canDelete));
                            items[i] = is;
                            break;
                        }
                        c += is.getAmount();
                        items[i] = null;
                    }
                }
            }
        }
        inv.setContents(items);
        if (MineManager.isBefore9()) {
            player.updateInventory();
        }
    }

    private int getDropAmount(Block block) {
        int amount = 0;
        if (block != null) for (ItemStack itemStack : block.getDrops())
            if (itemStack != null) amount += itemStack.getAmount();
        return amount;
    }

    public boolean isPlacedBlock(Block b) {
        List<MetadataValue> metaDataValues = b.getMetadata("PlacedBlock");
        for (MetadataValue value : metaDataValues) {
            return value.asBoolean();
        }
        return false;
    }

}
