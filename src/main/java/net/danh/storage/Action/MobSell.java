package net.danh.storage.Action;

import net.danh.storage.API.events.MobStorageSellEvent;
import net.danh.storage.Manager.Mob.MobStorageManager;
import net.danh.storage.Storage;
import net.danh.storage.Utils.ChatUtils;
import net.danh.storage.Utils.File;
import net.danh.storage.Utils.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;

public class MobSell {

    private final Player player;
    private final String itemName;
    private final long amount;
    private final FileConfiguration config;

    public MobSell(Player player, String itemName, long amount) {
        this.player = player;
        this.itemName = itemName;
        this.amount = amount;
        this.config = File.getMobStorageConfig();
    }

    public void doAction() {
        if (player == null || !player.isOnline() || itemName == null || itemName.trim().isEmpty()) {
            return;
        }

        if (!MobStorageManager.isSystemEnabled()) {
            player.sendMessage(ChatUtils.colorizewp(File.getMessage().getString(
                    "mobstorage.system_disabled",
                    "#prefix# &cMobStorage feature is disabled!")));
            return;
        }

        long sellAmount = amount <= 0 ? MobStorageManager.getPlayerItem(player, itemName) : amount;
        int current = MobStorageManager.getPlayerItem(player, itemName);
        if (current <= 0 || sellAmount <= 0 || current < sellAmount) {
            player.sendMessage(ChatUtils.colorizewp(File.getMessage().getString(
                            "mobstorage.action.sell.not_enough",
                            "#prefix# &cYou don't have enough items. Current: <amount>")
                    .replace("<amount>", String.valueOf(current))));
            return;
        }

        ConfigurationSection worthSection = config.getConfigurationSection("worth");
        if (worthSection == null) {
            player.sendMessage(ChatUtils.colorizewp(File.getMessage().getString(
                    "mobstorage.action.sell.no_worth_config",
                    "#prefix# &cSell system is not configured!")));
            return;
        }

        String worthKey = resolveWorthKey(worthSection, itemName.toUpperCase());
        if (worthKey == null) {
            player.sendMessage(ChatUtils.colorizewp(File.getMessage().getString(
                    "mobstorage.action.sell.item_not_sellable",
                    "#prefix# &cThis item is not sellable!")));
            return;
        }

        WorthEntry worthEntry = parseWorthEntry(worthSection, worthKey);
        double worth = worthEntry.worth;
        if (worth <= 0) {
            player.sendMessage(ChatUtils.colorizewp(File.getMessage().getString(
                    "mobstorage.action.sell.can_not_sell",
                    "#prefix# &cThis item can't be sold.")));
            return;
        }

        int requestedAmount = sellAmount > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) sellAmount;
        MobStorageSellEvent event = new MobStorageSellEvent(player, itemName.toUpperCase(), requestedAmount, worth);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }

        requestedAmount = event.getAmount();
        worth = event.getWorthPerItem();
        if (requestedAmount <= 0 || worth <= 0) {
            return;
        }
        current = MobStorageManager.getPlayerItem(player, itemName);
        if (current < requestedAmount) {
            player.sendMessage(ChatUtils.colorizewp(File.getMessage().getString(
                            "mobstorage.action.sell.not_enough",
                            "#prefix# &cYou don't have enough items. Current: <amount>")
                    .replace("<amount>", String.valueOf(current))));
            return;
        }
        if (!MobStorageManager.removeItemAmount(player, itemName, requestedAmount, false)) {
            player.sendMessage(ChatUtils.colorizewp(File.getMessage().getString(
                    "mobstorage.action.sell.failed_to_remove",
                    "#prefix# &cFailed to remove items from MobStorage.")));
            return;
        }

        double money = worth * requestedAmount;
        String moneyFormatted = roundWithDecimalFormat(money);
        String method = worthEntry.methodOverride != null && !worthEntry.methodOverride.trim().isEmpty()
                ? worthEntry.methodOverride
                : config.getString("sell_method", "commands");
        boolean vaultRequested = "vault".equalsIgnoreCase(method);
        boolean playerPointsRequested = "playerpoints".equalsIgnoreCase(method);
        if (!payMoney(money, moneyFormatted, method)) {
            runCommands(moneyFormatted);
            if (vaultRequested || playerPointsRequested) {
                String key = playerPointsRequested
                        ? "mobstorage.action.sell.playerpoints.payout_fixed"
                        : "mobstorage.action.sell.payout_fixed";
                player.sendMessage(ChatUtils.colorizewp(File.getMessage().getString(
                                key,
                                "#prefix# &aFixed! You have received your payout.")
                        .replace("#money#", moneyFormatted)));
            }
        }

        player.sendMessage(ChatUtils.colorizewp(File.getMessage().getString(
                        "mobstorage.action.sell.sell_item",
                        "#prefix# &aSold #amount# #material# for #money#.")
                .replace("#amount#", String.valueOf(requestedAmount))
                .replace("#material#", MobStorageManager.getItemDisplayName(itemName))
                .replace("#player#", player.getName())
                .replace("#money#", moneyFormatted)
                .replace("#item_amount#", String.valueOf(MobStorageManager.getPlayerItem(player, itemName)))
                .replace("#max_storage#", String.valueOf(MobStorageManager.getMaxStorage(player)))));
    }

    private void runCommands(String moneyFormatted) {
        for (String cmd : config.getStringList("sell")) {
            if (cmd == null || cmd.trim().isEmpty()) {
                continue;
            }
            String command = cmd.replace("#money#", moneyFormatted).replace("#player#", player.getName());
            SchedulerUtil.runTask(Storage.getStorage(), () -> Storage.getStorage().getServer().dispatchCommand(
                    Storage.getStorage().getServer().getConsoleSender(),
                    command));
        }
    }

    private boolean payMoney(double money, String moneyFormatted, String method) {
        if ("playerpoints".equalsIgnoreCase(method)) {
            if (Storage.depositToPlayerPoints(player, money)) {
                return true;
            }
            Storage.getStorage().getLogger().warning("PlayerPoints payout failed for player "
                    + player.getName() + " (" + money + ") in MobStorage. Falling back to commands.");
            player.sendMessage(ChatUtils.colorizewp(File.getMessage().getString(
                            "mobstorage.action.sell.playerpoints.payout_error",
                            "#prefix# &cAn error occurred while processing your points.")
                    .replace("#money#", moneyFormatted)));
            return false;
        }
        if (!"vault".equalsIgnoreCase(method)) {
            return false;
        }
        if (Storage.depositToVault(player, money)) {
            return true;
        }
        Storage.getStorage().getLogger().warning("Vault payout failed for player "
                + player.getName() + " (" + money + ") in MobStorage. Falling back to commands.");
        player.sendMessage(ChatUtils.colorizewp(File.getMessage().getString(
                        "mobstorage.action.sell.payout_error",
                        "#prefix# &cAn error occurred while processing your payment.")
                .replace("#money#", moneyFormatted)));
        return false;
    }

    private WorthEntry parseWorthEntry(ConfigurationSection section, String worthKey) {
        Object raw = section.get(worthKey);
        if (raw instanceof java.lang.Number) {
            return new WorthEntry(((java.lang.Number) raw).doubleValue(), null);
        }
        if (raw instanceof String) {
            String value = ((String) raw).trim();
            if (value.isEmpty()) {
                return new WorthEntry(0D, null);
            }
            String[] parts = value.split(";", -1);
            double worth = 0D;
            try {
                worth = Double.parseDouble(parts[0].trim());
            } catch (NumberFormatException ignored) {
                worth = 0D;
            }
            String method = null;
            if (parts.length >= 2 && parts[1] != null) {
                String normalized = parts[1].trim().toLowerCase();
                if ("command".equals(normalized)) {
                    normalized = "commands";
                }
                if ("commands".equals(normalized) || "vault".equals(normalized) || "playerpoints".equals(normalized)) {
                    method = normalized;
                }
            }
            return new WorthEntry(worth, method);
        }
        return new WorthEntry(section.getDouble(worthKey), null);
    }

    private String roundWithDecimalFormat(double value) {
        String numberFormat = config.getString("number_format");
        DecimalFormat decimalFormat = numberFormat != null ? new DecimalFormat(numberFormat) : new DecimalFormat("#.##");
        return decimalFormat.format(value);
    }

    private String resolveWorthKey(ConfigurationSection section, String itemName) {
        if (section == null || itemName == null) {
            return null;
        }
        String normalized = itemName.replace(":", ";");
        if (section.contains(normalized)) {
            return normalized;
        }
        String withZero = normalized + ";0";
        if (section.contains(withZero)) {
            return withZero;
        }
        if (normalized.endsWith(";0")) {
            String noData = normalized.substring(0, normalized.length() - 2);
            if (section.contains(noData)) {
                return noData;
            }
        }
        return null;
    }

    private record WorthEntry(double worth, String methodOverride) {
    }
}
