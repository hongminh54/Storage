package net.danh.storage.Listeners;

import com.cryptomorin.xseries.messages.ActionBar;
import com.cryptomorin.xseries.messages.Titles;
import net.danh.storage.Manager.GameManager.ChatManager;
import net.danh.storage.Manager.GameManager.MineManager;
import net.danh.storage.Manager.UtilsManager.FileManager;
import net.danh.storage.Storage;
import net.danh.storage.Utils.Number;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class BlockBreak implements Listener {

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBreak(@NotNull BlockBreakEvent e) {
        Player p = e.getPlayer();
        Block block = e.getBlock();
//        if (Storage.isWorldGuardInstalled()) {
//            if (!WorldGuard.handleForLocation(p, block.getLocation())) {
//                e.setCancelled(true);
//                return;
//            }
//        }
        if (MineManager.checkBreak(block)) {
            String drop = MineManager.getDrop(block);
            int amount;
            ItemStack hand = p.getInventory().getItemInMainHand();
            if (!hand.containsEnchantment(Enchantment.LOOT_BONUS_BLOCKS)) {
                amount = 1;
            } else {
                amount = Number.getRandomInteger(1, hand.getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS) + 2);
            }
            if (MineManager.addBlockAmount(p, drop, amount)) {
                if (FileManager.getConfig().getBoolean("mine.actionbar.enable")) {
                    String name = FileManager.getConfig().getString("items." + drop);
                    ActionBar.sendActionBar(Storage.getStorage(), p, ChatManager.colorizewp(Objects.requireNonNull(FileManager.getConfig().getString("mine.actionbar.action")).replace("#item#", name != null ? name : drop.replace("_", " ")).replace("#amount#", String.valueOf(amount)).replace("#storage#", String.valueOf(MineManager.getPlayerBlock(p, drop))).replace("#max#", String.valueOf(MineManager.getMaxBlock(p)))));
                }
                if (FileManager.getConfig().getBoolean("mine.title.enable")) {
                    String name = FileManager.getConfig().getString("items." + drop);
                    String replacement = name != null ? name : drop.replace("_", " ");
                    Titles.sendTitle(p, ChatManager.colorizewp(Objects.requireNonNull(FileManager.getConfig().getString("mine.title.title")).replace("#item#", replacement).replace("#amount#", String.valueOf(amount)).replace("#storage#", String.valueOf(MineManager.getPlayerBlock(p, drop))).replace("#max#", String.valueOf(MineManager.getMaxBlock(p)))), ChatManager.colorizewp(Objects.requireNonNull(FileManager.getConfig().getString("mine.title.subtitle")).replace("#item#", replacement).replace("#amount#", String.valueOf(amount)).replace("#storage#", String.valueOf(MineManager.getPlayerBlock(p, drop))).replace("#max#", String.valueOf(MineManager.getMaxBlock(p)))));
                }
            } else {
                p.sendMessage(ChatManager.colorize(FileManager.getMessage().getString("user.full_storage")));
                e.setCancelled(true);
            }
            e.setDropItems(false);
            e.getBlock().getDrops().clear();
        }
    }
}
