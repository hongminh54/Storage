package net.danh.storage.Action;

import net.danh.storage.Manager.Crop.CropStorageManager;
import net.danh.storage.Storage;
import net.danh.storage.Utils.ChatUtils;
import net.danh.storage.Utils.File;
import net.danh.storage.Utils.SchedulerUtil;
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

        double worth = worthSection.getDouble(worthKey);
        if (worth <= 0) {
            String msg = File.getMessage().getString(
                    "cropstorage.action.sell.can_not_sell",
                    "#prefix# &cThis item can't be sell"
            );
            player.sendMessage(ChatUtils.colorizewp(msg));
            return;
        }

        if (!CropStorageManager.removeItemAmount(player, itemName, (int) sellAmount)) {
            String msg = File.getMessage().getString(
                    "cropstorage.action.sell.failed_to_remove",
                    "#prefix# &cFailed to remove items from storage! Please try again."
            );
            player.sendMessage(ChatUtils.colorizewp(msg));
            return;
        }

        double money = worth * sellAmount;
        String moneyFormatted = roundWithDecimalFormat(money);
        runCommands(moneyFormatted);

        String displayName = CropStorageManager.getItemDisplayName(itemName);
        String msg = File.getMessage().getString(
                "cropstorage.action.sell.sell_item",
                "#prefix# &aYou sold #amount# #material# and got #money#$ | #item_amount#/#max_storage# Items"
        );

        msg = msg
                .replace("#amount#", String.valueOf(sellAmount))
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
}
