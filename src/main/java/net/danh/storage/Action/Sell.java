package net.danh.storage.Action;

import net.danh.storage.Manager.MineManager;
import net.danh.storage.NMS.NMSAssistant;
import net.danh.storage.Storage;
import net.danh.storage.Utils.ChatUtils;
import net.danh.storage.Utils.File;
import net.danh.storage.Utils.Number;
import net.danh.storage.Utils.SchedulerUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.Objects;

public class Sell {
    private final Player p;
    private final String material;
    private final Integer amount;
    private final FileConfiguration config;

    public Sell(Player p, @NotNull String material, Integer amount) {
        this.p = p;
        String material_data = material.replace(":", ";");
        NMSAssistant nms = new NMSAssistant();
        if (nms.isVersionGreaterThanOrEqualTo(13)) {
            this.material = material_data.split(";")[0];
        } else {
            if (Number.getInteger(material_data.split(";")[1]) > 0) {
                this.material = material.replace(";", ":");
            } else {
                this.material = material_data.split(";")[0];
            }
        }
        this.amount = amount;
        config = File.getConfig();
    }

    public void doAction() {
        int amount = MineManager.getPlayerBlock(p, getMaterialData());
        if (this.amount > 0) {
            if (amount >= this.amount) {
                ConfigurationSection section = config.getConfigurationSection("worth");
                if (section != null) {
                    String worthKey = resolveWorthKey(section, getMaterialData());
                    if (worthKey != null) {
                        double worth = section.getDouble(worthKey);
                        if (worth > 0) {
                            if (MineManager.removeBlockAmount(p, getMaterialData(), this.amount)) {
                                double money = worth * this.amount;
                                String money_round_up = roundWithDecimalFormat(money);
                                double m_ru = Double.parseDouble(money_round_up);
                                runCommand(m_ru);
                                p.sendMessage(ChatUtils.colorize(File.getMessage()
                                        .getString("user.action.sell.sell_item")
                                        .replace("#amount#", String.valueOf(this.amount))
                                        .replace("#material#",
                                                File.getConfig().getString("items." + getMaterialData(),
                                                        getMaterialData().split(";")[0]))
                                        .replace("#player#", p.getName()).replace("#money#", String.valueOf(m_ru))
                                        .replace("#item_amount#",
                                                String.valueOf(MineManager.getPlayerBlock(p, getMaterialData())))
                                        .replace("#max_storage#", String.valueOf(MineManager.getMaxBlock(p)))));
                            } else {
                                p.sendMessage(ChatUtils
                                        .colorize(File.getMessage().getString("user.action.sell.failed_to_remove")));
                            }
                        } else {
                            p.sendMessage(ChatUtils
                                    .colorize(File.getMessage().getString("user.action.sell.can_not_sell")));
                        }
                    } else {
                        p.sendMessage(ChatUtils
                                .colorize(File.getMessage().getString("user.action.sell.item_not_sellable")));
                    }
                } else {
                    p.sendMessage(
                            ChatUtils.colorize(File.getMessage().getString("user.action.sell.no_worth_config")));
                }
            } else {
                p.sendMessage(
                        ChatUtils.colorize(Objects.requireNonNull(File.getMessage().getString("user.not_enough_items"))
                                .replace("<amount>", String.valueOf(amount))));
            }
        } else {
            ConfigurationSection section = config.getConfigurationSection("worth");
            if (section != null) {
                String worthKey = resolveWorthKey(section, getMaterialData());
                if (worthKey != null) {
                    double worth = section.getDouble(worthKey);
                    if (worth > 0) {
                        if (MineManager.removeBlockAmount(p, getMaterialData(), amount)) {
                            double money = worth * amount;
                            String money_round_up = roundWithDecimalFormat(money);
                            double m_ru = Double.parseDouble(money_round_up);
                            runCommand(m_ru);
                            p.sendMessage(ChatUtils.colorize(
                                    Objects.requireNonNull(File.getMessage().getString("user.action.sell.sell_item"))
                                            .replace("#amount#", String.valueOf(amount))
                                            .replace("#material#",
                                                    File.getConfig().getString("items." + getMaterialData(),
                                                            getMaterialData().split(";")[0]))
                                            .replace("#player#", p.getName()).replace("#money#", String.valueOf(m_ru))
                                            .replace("#item_amount#",
                                                    String.valueOf(MineManager.getPlayerBlock(p, getMaterialData())))
                                            .replace("#max_storage#", String.valueOf(MineManager.getMaxBlock(p)))));
                        } else {
                            p.sendMessage(ChatUtils
                                    .colorize(File.getMessage().getString("user.action.sell.failed_to_remove")));
                        }
                    } else {
                        p.sendMessage(
                                ChatUtils.colorize(File.getMessage().getString("user.action.sell.can_not_sell")));
                    }
                } else {
                    p.sendMessage(
                            ChatUtils.colorize(File.getMessage().getString("user.action.sell.item_not_sellable")));
                }
            } else {
                p.sendMessage(ChatUtils.colorize(File.getMessage().getString("user.action.sell.no_worth_config")));
            }
        }
    }

    public void runCommand(Double money) {
        String method = config.getString("sell_method", "commands");
        if ("vault".equalsIgnoreCase(method)) {
            if (Storage.depositToVault(p, money)) {
                return;
            }

            Storage.getStorage().getLogger().warning(
                    "Vault payout failed for player " + p.getName() + " (" + money + "). Falling back to commands."
            );
            p.sendMessage(ChatUtils.colorize(File.getMessage().getString(
                    "user.action.sell.payout_error",
                    "#prefix# &cAn error occurred while processing your payment. Attempting to fix it..."
            )));
        }

        config.getStringList("sell").forEach(cmd -> {
            String cmd_2 = cmd.replace("#money#", roundWithDecimalFormat(money)).replace("#player#", p.getName());
            SchedulerUtil.runTask(Storage.getStorage(), () -> {
                Storage.getStorage().getServer().dispatchCommand(Storage.getStorage().getServer().getConsoleSender(),
                        cmd_2);
            });
        });

        if ("vault".equalsIgnoreCase(method)) {
            p.sendMessage(ChatUtils.colorize(File.getMessage().getString(
                    "user.action.sell.payout_fixed",
                    "#prefix# &aFixed! You have received your money."
            )));
        }
    }

    public String roundWithDecimalFormat(double d) {
        String nf = File.getConfig().getString("number_format");
        DecimalFormat df;
        if (nf != null) {
            df = new DecimalFormat(nf);
        } else {
            df = new DecimalFormat("#.##");
        }
        return df.format(d);
    }

    public String getMaterialData() {
        if (!material.contains(";")) {
            return material + ";0";
        } else
            return material;
    }

    private String resolveWorthKey(@NotNull ConfigurationSection section,
                                   @NotNull String materialData) {
        if (section.contains(materialData)) {
            return materialData;
        }

        if (materialData.endsWith(";0")) {
            String noData = materialData.substring(0, materialData.length() - 2);
            if (section.contains(noData)) {
                return noData;
            }
        }

        return null;
    }

    public Player getPlayer() {
        return p;
    }

    public String getMaterial() {
        return material;
    }

    public Integer getAmount() {
        return amount;
    }

    public FileConfiguration getConfig() {
        return config;
    }
}
