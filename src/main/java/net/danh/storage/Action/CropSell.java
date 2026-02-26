package net.danh.storage.Action;

import net.danh.storage.API.events.CropStorageSellEvent;
import net.danh.storage.Manager.Crop.CropStorageManager;
import net.danh.storage.Storage;
import net.danh.storage.Utils.ChatUtils;
import net.danh.storage.Utils.File;
import net.danh.storage.Utils.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;

public class CropSell {
    private final Player player;
    private final String itemName;
    private final long amount;
    private final FileConfiguration config;

    public CropSell(Player player, String itemName, long amount) {
        this.player = player;
        this.itemName = itemName;
        this.amount = amount;
        this.config = File.getCropStorageConfig();
    }

    public void doAction() {
        if (player == null || !player.isOnline()) {
            return;
        }

        if (!CropStorageManager.isSystemEnabled()) {
            String msg = File.getMessage().getString(
                    "cropstorage.system_disabled",
                    "#prefix# &cCropStorage feature is disabled!"
            );
            player.sendMessage(ChatUtils.colorizewp(msg));
            return;
        }

        if (itemName == null || itemName.trim().isEmpty()) {
            return;
        }

        long sellAmount = Math.max(0, amount);
        int current = CropStorageManager.getPlayerItem(player, itemName);

        if (sellAmount <= 0) {
            sellAmount = current;
        }

        if (current <= 0 || sellAmount <= 0 || current < sellAmount) {
            String msg = File.getMessage().getString(
                    "cropstorage.action.sell.not_enough",
                    "#prefix# &cYou haven't enough item, you only have <amount>"
            ).replace("<amount>", String.valueOf(current));
            player.sendMessage(ChatUtils.colorizewp(msg));
            return;
        }

        ConfigurationSection worthSection = config.getConfigurationSection("worth");
        if (worthSection == null) {
            String msg = File.getMessage().getString(
                    "cropstorage.action.sell.no_worth_config",
                    "#prefix# &cSell system is not configured! Please contact administrator."
            );
            player.sendMessage(ChatUtils.colorizewp(msg));
            return;
        }

        String worthKey = resolveWorthKey(worthSection, itemName);
        if (worthKey == null) {
            String msg = File.getMessage().getString(
                    "cropstorage.action.sell.item_not_sellable",
                    "#prefix# &cThis item is not sellable!"
            );
            player.sendMessage(ChatUtils.colorizewp(msg));
            return;
        }

        WorthEntry worthEntry = parseWorthEntry(worthSection, worthKey);
        double worth = worthEntry.worth;
        if (worth <= 0) {
            String msg = File.getMessage().getString(
                    "cropstorage.action.sell.can_not_sell",
                    "#prefix# &cThis item can't be sell"
            );
            player.sendMessage(ChatUtils.colorizewp(msg));
            return;
        }

        int requestedAmount;
        if (sellAmount > Integer.MAX_VALUE) {
            requestedAmount = Integer.MAX_VALUE;
        } else {
            requestedAmount = (int) sellAmount;
        }

        CropStorageSellEvent event = new CropStorageSellEvent(player, itemName.toUpperCase(), requestedAmount, worth);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }

        requestedAmount = event.getAmount();
        if (requestedAmount <= 0) {
            return;
        }
        worth = event.getWorthPerItem();
        if (worth <= 0) {
            String msg = File.getMessage().getString(
                    "cropstorage.action.sell.can_not_sell",
                    "#prefix# &cThis item can't be sell"
            );
            player.sendMessage(ChatUtils.colorizewp(msg));
            return;
        }

        if (current < requestedAmount) {
            String msg = File.getMessage().getString(
                    "cropstorage.action.sell.not_enough",
                    "#prefix# &cYou haven't enough item, you only have <amount>"
            ).replace("<amount>", String.valueOf(current));
            player.sendMessage(ChatUtils.colorizewp(msg));
            return;
        }

        if (!CropStorageManager.removeItemAmount(player, itemName, requestedAmount, false)) {
            String msg = File.getMessage().getString(
                    "cropstorage.action.sell.failed_to_remove",
                    "#prefix# &cFailed to remove items from storage! Please try again."
            );
            player.sendMessage(ChatUtils.colorizewp(msg));
            return;
        }

        long finalSellAmount = requestedAmount;
        double money = worth * finalSellAmount;
        String moneyFormatted = roundWithDecimalFormat(money);
        String method = worthEntry.methodOverride != null && !worthEntry.methodOverride.trim().isEmpty()
                ? worthEntry.methodOverride
                : (config != null ? config.getString("sell_method", "commands") : "commands");
        boolean vaultRequested = "vault".equalsIgnoreCase(method);
        boolean playerPointsRequested = "playerpoints".equalsIgnoreCase(method);
        if (!payMoney(money, moneyFormatted, method)) {
            runCommands(moneyFormatted);
            if (vaultRequested || playerPointsRequested) {
                String fixed;
                if (playerPointsRequested) {
                    fixed = File.getMessage().getString(
                            "cropstorage.action.sell.playerpoints.payout_fixed",
                            "#prefix# &aFixed! You have received your points."
                    );
                } else {
                    fixed = File.getMessage().getString(
                            "cropstorage.action.sell.payout_fixed",
                            "#prefix# &aFixed! You have received your money."
                    );
                }
                player.sendMessage(ChatUtils.colorizewp(fixed.replace("#money#", moneyFormatted)));
            }
        }

        String displayName = CropStorageManager.getItemDisplayName(itemName);
        String msg = File.getMessage().getString(
                "cropstorage.action.sell.sell_item",
                "#prefix# &aYou sold #amount# #material# and got #money#$ | #item_amount#/#max_storage# Items"
        );

        msg = msg
                .replace("#amount#", String.valueOf(finalSellAmount))
                .replace("#material#", displayName)
                .replace("#player#", player.getName())
                .replace("#money#", moneyFormatted)
                .replace("#item_amount#", String.valueOf(CropStorageManager.getPlayerItem(player, itemName)))
                .replace("#max_storage#", String.valueOf(CropStorageManager.getMaxStorage(player)));
        player.sendMessage(ChatUtils.colorizewp(msg));
    }

    private void runCommands(String moneyFormatted) {
        if (config == null) {
            return;
        }

        for (String cmd : config.getStringList("sell")) {
            if (cmd == null || cmd.trim().isEmpty()) {
                continue;
            }
            String command = cmd
                    .replace("#money#", moneyFormatted)
                    .replace("#player#", player.getName());

            SchedulerUtil.runTask(Storage.getStorage(), () -> Storage.getStorage().getServer().dispatchCommand(
                    Storage.getStorage().getServer().getConsoleSender(),
                    command
            ));
        }
    }

    private boolean payMoney(double money, String moneyFormatted, String method) {
        if ("playerpoints".equalsIgnoreCase(method)) {
            if (Storage.depositToPlayerPoints(player, money)) {
                return true;
            }
            Storage.getStorage().getLogger().warning(
                    "PlayerPoints payout failed for player " + player.getName() + " (" + money + ") in CropStorage. Falling back to commands."
            );
            String msg = File.getMessage().getString(
                    "cropstorage.action.sell.playerpoints.payout_error",
                    "#prefix# &cAn error occurred while processing your points. Attempting to fix it..."
            );
            player.sendMessage(ChatUtils.colorizewp(msg.replace("#money#", moneyFormatted)));
            return false;
        }
        if (!"vault".equalsIgnoreCase(method)) {
            return false;
        }

        if (Storage.depositToVault(player, money)) {
            return true;
        }

        Storage.getStorage().getLogger().warning(
                "Vault payout failed for player " + player.getName() + " (" + money + ") in CropStorage. Falling back to commands."
        );
        String msg = File.getMessage().getString(
                "cropstorage.action.sell.payout_error",
                "#prefix# &cAn error occurred while processing your payment. Attempting to fix it..."
        );
        player.sendMessage(ChatUtils.colorizewp(msg.replace("#money#", moneyFormatted)));
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
            if (parts.length >= 2) {
                String m = parts[1] != null ? parts[1].trim() : "";
                if (!m.isEmpty()) {
                    String normalized = m.toLowerCase();
                    if ("command".equals(normalized)) {
                        normalized = "commands";
                    }
                    if ("commands".equals(normalized) || "vault".equals(normalized) || "playerpoints".equals(normalized)) {
                        method = normalized;
                    }
                }
            }

            return new WorthEntry(worth, method);
        }

        return new WorthEntry(section.getDouble(worthKey), null);
    }

    private String roundWithDecimalFormat(double d) {
        String nf = config != null ? config.getString("number_format") : null;
        DecimalFormat df = nf != null ? new DecimalFormat(nf) : new DecimalFormat("#.##");
        return df.format(d);
    }

    private String resolveWorthKey(ConfigurationSection section, String itemName) {
        if (section == null || itemName == null) {
            return null;
        }
        if (section.contains(itemName)) {
            return itemName;
        }
        String withZero = itemName + ";0";
        if (section.contains(withZero)) {
            return withZero;
        }
        if (itemName.endsWith(";0")) {
            String noData = itemName.substring(0, itemName.length() - 2);
            if (section.contains(noData)) {
                return noData;
            }
        }
        return null;
    }

    public Player getPlayer() {
        return player;
    }

    public String getItemName() {
        return itemName;
    }

    public long getAmount() {
        return amount;
    }

    private static class WorthEntry {
        private final double worth;
        private final String methodOverride;

        private WorthEntry(double worth, String methodOverride) {
            this.worth = worth;
            this.methodOverride = methodOverride;
        }
    }
}
