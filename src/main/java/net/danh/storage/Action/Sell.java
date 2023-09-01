package net.danh.storage.Action;

import net.danh.storage.Manager.MineManager;
import net.danh.storage.NMS.NMSAssistant;
import net.danh.storage.Storage;
import net.danh.storage.Utils.Chat;
import net.danh.storage.Utils.File;
import net.danh.storage.Utils.Number;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

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
        if (getAmount() > 0) {
            if (amount >= getAmount()) {
                if (MineManager.removeBlockAmount(p, getMaterialData(), getAmount())) {
                    ConfigurationSection section = config.getConfigurationSection("worth");
                    if (section != null) {
                        List<String> sell_list = new ArrayList<>(section.getKeys(false));
                        if (sell_list.contains(getMaterialData())) {
                            int worth = section.getInt(getMaterialData());
                            if (worth > 0) {
                                int money = worth * getAmount();
                                runCommand(money);
                                p.sendMessage(Chat.colorize(File.getMessage().getString("user.action.sell.sell_item"))
                                        .replace("#amount#", String.valueOf(getAmount()))
                                        .replace("#material#", material)
                                        .replace("#player#", p.getName())
                                        .replace("#money#", String.valueOf(money))
                                        .replace("#item_amount#", String.valueOf(MineManager.getPlayerBlock(p, getMaterialData())))
                                        .replace("#max_storage#", String.valueOf(MineManager.getMaxBlock(p))));
                            } else {
                                p.sendMessage(Chat.colorize(File.getMessage().getString("user.action.sell.can_not_sell")));
                            }
                        }
                    }
                }
            } else {
                p.sendMessage(Chat.colorize(File.getMessage().getString("user.not_enough_item")));
            }
        } else {
            if (MineManager.removeBlockAmount(p, getMaterialData(), amount)) {
                ConfigurationSection section = config.getConfigurationSection("worth");
                if (section != null) {
                    List<String> sell_list = new ArrayList<>(section.getKeys(false));
                    if (sell_list.contains(getMaterialData())) {
                        int worth = section.getInt(getMaterialData());
                        if (worth > 0) {
                            int money = worth * amount;
                            runCommand(money);
                            p.sendMessage(Chat.colorize(File.getMessage().getString("user.action.sell.sell_item"))
                                    .replace("#amount#", String.valueOf(amount))
                                    .replace("#material#", material)
                                    .replace("#player#", p.getName())
                                    .replace("#money#", String.valueOf(money))
                                    .replace("#item_amount#", String.valueOf(MineManager.getPlayerBlock(p, getMaterialData())))
                                    .replace("#max_storage#", String.valueOf(MineManager.getMaxBlock(p))));
                        } else {
                            p.sendMessage(Chat.colorize(File.getMessage().getString("user.action.sell.can_not_sell")));
                        }
                    }
                }
            }
        }
    }

    public void runCommand(Integer money) {
        config.getStringList("sell").forEach(cmd -> {
            String cmd_2 = cmd.replace("#money#", String.valueOf(money))
                    .replace("#player#", p.getName());
            new BukkitRunnable() {
                @Override
                public void run() {
                    Storage.getStorage().getServer().dispatchCommand(Storage.getStorage().getServer().getConsoleSender(), cmd_2);
                }
            }.runTask(Storage.getStorage());
        });
    }

    public String getMaterialData() {
        if (!material.contains(";")) {
            return material + ";0";
        } else return material;
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
