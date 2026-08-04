package net.danh.storage.CMD.handler.mythic.user;

import net.danh.storage.Action.MythicDeposit;
import net.danh.storage.CMD.handler.mythic.MythicCommand;
import net.danh.storage.Manager.Mythic.MythicStorageManager;
import net.danh.storage.MythicMobs.MythicMobsHelper;
import net.danh.storage.Utils.Number;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

public class MythicDepositCommand extends MythicCommand {

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!requirePlayer(sender)) {
            return;
        }

        Player player = (Player) sender;

        if (isMythicStorageWorldBlacklisted(player)) {
            sendWorldBlacklisted(sender, "MythicStorage", player.getWorld().getName());
            return;
        }

        if (args.length < 1 || args.length > 2) {
            sendUsage(sender);
            return;
        }

        String itemName = args[0];
        if (!MythicStorageManager.isConfiguredDrop(itemName)) {
            sendInvalidItem(sender, itemName);
            return;
        }

        long amount;
        if (args.length == 1) {
            amount = countEligibleItems(player, itemName);
        } else {
            if ("all".equalsIgnoreCase(args[1])) {
                amount = countEligibleItems(player, itemName);
            } else {
                amount = Number.getLong(args[1]);
                if (amount <= 0) {
                    sendInvalidNumber(sender, args[1]);
                    return;
                }
            }
        }

        if (amount <= 0) {
            return;
        }

        new MythicDeposit(player, itemName, amount).doAction();
    }

    private long countEligibleItems(Player player, String itemName) {
        if (player == null || itemName == null || itemName.trim().isEmpty()) {
            return 0;
        }

        MythicMobsHelper helper = MythicStorageManager.getMythicMobsHelper();
        if (helper == null || !helper.isInitialized()) {
            return 0;
        }

        long count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) {
                continue;
            }

            String foundItemName = helper.getMythicItemInternalName(item);
            if (foundItemName != null && foundItemName.equals(itemName)) {
                count += item.getAmount();
            }
        }
        return count;
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], getConfiguredDrops(), completions);
            return completions;
        }

        if (args.length == 2) {
            if (!(sender instanceof Player player)) {
                return completions;
            }

            String itemName = args[0];
            if (!MythicStorageManager.isConfiguredDrop(itemName)) {
                return completions;
            }

            long count = countEligibleItems(player, itemName);
            if (count <= 0) {
                return completions;
            }

            List<String> suggestions = new ArrayList<>();
            suggestions.add("all");
            suggestions.add("1");
            if (count >= 10) {
                suggestions.add("10");
            }
            if (count >= 64) {
                suggestions.add("64");
            }
            suggestions.add(String.valueOf(Math.min(count, Integer.MAX_VALUE)));

            StringUtil.copyPartialMatches(args[1], suggestions, completions);
            return completions;
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "storage.mythicstorage.use";
    }

    @Override
    public String getUsage() {
        return "/mythicstorage deposit <item> [amount|all]";
    }

    @Override
    public String getDescription() {
        return "Deposit MythicMobs items from inventory into MythicStorage";
    }
}
