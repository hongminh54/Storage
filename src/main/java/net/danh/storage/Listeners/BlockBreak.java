package net.danh.storage.Listeners;

import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.messages.ActionBar;
import com.cryptomorin.xseries.messages.Titles;
import net.danh.storage.Manager.EventManager;
import net.danh.storage.Manager.MineManager;
import net.danh.storage.NMS.NMSAssistant;
import net.danh.storage.Storage;
import net.danh.storage.Utils.Chat;
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
import java.util.Objects;

public class BlockBreak implements Listener {

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBreak(@NotNull BlockBreakEvent e) {
        Player p = e.getPlayer();
        Block block = e.getBlock();
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
        if (MineManager.toggle.get(p)) {
            if (inv_full) {
                int old_data = MineManager.getPlayerBlock(p, MineManager.getDrop(block));
                int max_storage = MineManager.getMaxBlock(p);
                int count = max_storage - old_data;
                for (ItemStack itemStack : p.getInventory().getContents()) {
                    if (itemStack != null) {
                        String drop = MineManager.getItemStackDrop(itemStack);
                        int amount = itemStack.getAmount();
                        if (drop != null) {
                            int new_data = old_data + Math.toIntExact(amount);
                            int min = Math.min(count, Math.toIntExact(amount));
                            int replacement = new_data >= max_storage ? min : amount;
                            if (MineManager.addBlockAmount(p, drop, replacement)) {
                                removeItems(p, itemStack, replacement);
                            }
                        }
                    }
                }
            }
            if (MineManager.checkBreak(block)) {
                String drop = MineManager.getDrop(block);
                int amount;
                ItemStack hand = p.getInventory().getItemInMainHand();
                Enchantment fortune = XEnchantment.FORTUNE.get() != null ? XEnchantment.FORTUNE.get() : Objects.requireNonNull(XEnchantment.of(Enchantment.LOOT_BONUS_BLOCKS).get());
                if (!hand.containsEnchantment(fortune)) {
                    amount = getDropAmount(block);
                } else {
                    if (File.getConfig().getStringList("whitelist_fortune").contains(block.getType().name())) {
                        amount = Number.getRandomInteger(getDropAmount(block), getDropAmount(block) + hand.getEnchantmentLevel(fortune) + 2);
                    } else amount = getDropAmount(block);
                }

                int bonusAmount = EventManager.calculateDoubleDropBonus(amount);
                int totalAmount = amount + bonusAmount;

                if (MineManager.addBlockAmount(p, drop, totalAmount)) {
                    EventManager.onPlayerMine(p, drop, amount);
                    if (File.getConfig().getBoolean("mine.actionbar.enable")) {
                        String name = File.getConfig().getString("items." + drop);
                        String displayAmount = bonusAmount > 0 ? totalAmount + " (+" + bonusAmount + " bonus)" : String.valueOf(totalAmount);
                        ActionBar.sendActionBar(Storage.getStorage(), p, Chat.colorizewp(Objects.requireNonNull(File.getConfig().getString("mine.actionbar.action")).replace("#item#", name != null ? name : drop.replace("_", " ")).replace("#amount#", displayAmount).replace("#storage#", String.valueOf(MineManager.getPlayerBlock(p, drop))).replace("#max#", String.valueOf(MineManager.getMaxBlock(p)))));
                    }
                    if (File.getConfig().getBoolean("mine.title.enable")) {
                        String name = File.getConfig().getString("items." + drop);
                        String replacement = name != null ? name : drop.replace("_", " ");
                        String displayAmount = bonusAmount > 0 ? totalAmount + " (+" + bonusAmount + " bonus)" : String.valueOf(totalAmount);
                        Titles.sendTitle(p, Chat.colorizewp(Objects.requireNonNull(File.getConfig().getString("mine.title.title")).replace("#item#", replacement).replace("#amount#", displayAmount).replace("#storage#", String.valueOf(MineManager.getPlayerBlock(p, drop))).replace("#max#", String.valueOf(MineManager.getMaxBlock(p)))), Chat.colorizewp(Objects.requireNonNull(File.getConfig().getString("mine.title.subtitle")).replace("#item#", replacement).replace("#amount#", displayAmount).replace("#storage#", String.valueOf(MineManager.getPlayerBlock(p, drop))).replace("#max#", String.valueOf(MineManager.getMaxBlock(p)))));
                    }
                    if (new NMSAssistant().isVersionGreaterThanOrEqualTo(12)) {
                        e.setDropItems(false);
                    }
                    e.getBlock().getDrops().clear();
                }
//                else {
//                    p.sendMessage(Chat.colorize(File.getMessage().getString("user.full_storage")));
//                }
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
        player.updateInventory();
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
