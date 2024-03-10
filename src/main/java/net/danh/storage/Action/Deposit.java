package net.danh.storage.Action;

import com.cryptomorin.xseries.XMaterial;
import net.danh.storage.Manager.MineManager;
import net.danh.storage.NMS.NMSAssistant;
import net.danh.storage.Utils.Chat;
import net.danh.storage.Utils.File;
import net.danh.storage.Utils.Number;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

public class Deposit {
    private final Player p;
    private final String material;
    private final Long amount;

    @Contract(pure = true)
    public Deposit(Player p, @NotNull String material, Long amount) {
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
    }

    public String getMaterialData() {
        if (!material.contains(";")) {
            return material + ";0";
        } else return material;
    }

    public void doAction() {
        ItemStack item = getItemStack();
        if (item != null) {
            int amount = getPlayerAmount();
            if (getAmount() > 0) {
                if (amount >= getAmount()) {
                    removeItems(getAmount());
                    String material = getMaterial();
                    if (material.contains(";")) {
                        if (MineManager.addBlockAmount(p, material, Math.toIntExact(getAmount()))) {
                            p.sendMessage(Chat.colorize(File.getMessage().getString("user.action.deposit.deposit_item"))
                                    .replace("#amount#", String.valueOf(getAmount()))
                                    .replace("#material#", Objects.requireNonNull(File.getConfig().getString("items." + material)))
                                    .replace("#player#", p.getName())
                                    .replace("#item_amount#", String.valueOf(MineManager.getPlayerBlock(p, material)))
                                    .replace("#max_storage#", String.valueOf(MineManager.getMaxBlock(p))));
                        }
                    } else {
                        if (MineManager.addBlockAmount(p, getMaterialData(), Math.toIntExact(getAmount()))) {
                            p.sendMessage(Chat.colorize(File.getMessage().getString("user.action.deposit.deposit_item"))
                                    .replace("#amount#", String.valueOf(getAmount()))
                                    .replace("#material#", Objects.requireNonNull(File.getConfig().getString("items." + material)))
                                    .replace("#player#", p.getName())
                                    .replace("#item_amount#", String.valueOf(MineManager.getPlayerBlock(p, getMaterialData())))
                                    .replace("#max_storage#", String.valueOf(MineManager.getMaxBlock(p))));
                        }
                    }
                } else {
                    p.sendMessage(Chat.colorize(Objects.requireNonNull(File.getMessage().getString("user.not_enough_items"))
                            .replace("<amount>", String.valueOf(amount))));
                }
            } else {
                removeItems(amount);
                String material = getMaterial();
                if (material.contains(";")) {
                    if (MineManager.addBlockAmount(p, material, amount)) {
                        p.sendMessage(Chat.colorize(File.getMessage().getString("user.action.deposit.deposit_item"))
                                .replace("#amount#", String.valueOf(amount))
                                .replace("#material#", Objects.requireNonNull(File.getConfig().getString("items." + material)))
                                .replace("#player#", p.getName())
                                .replace("#item_amount#", String.valueOf(MineManager.getPlayerBlock(p, material)))
                                .replace("#max_storage#", String.valueOf(MineManager.getMaxBlock(p))));
                    }
                } else {
                    if (MineManager.addBlockAmount(p, getMaterialData(), amount)) {
                        p.sendMessage(Chat.colorize(File.getMessage().getString("user.action.deposit.deposit_item"))
                                .replace("#amount#", String.valueOf(amount))
                                .replace("#material#", Objects.requireNonNull(File.getConfig().getString("items." + material)))
                                .replace("#player#", p.getName())
                                .replace("#item_amount#", String.valueOf(MineManager.getPlayerBlock(p, getMaterialData())))
                                .replace("#max_storage#", String.valueOf(MineManager.getMaxBlock(p))));
                    }
                }
            }
        }
    }


    public ItemStack getItemStack() {
        Optional<XMaterial> xMaterial = XMaterial.matchXMaterial(material);
        return xMaterial.map(XMaterial::parseItem).orElse(null);
    }

    public int getPlayerAmount() {
        final PlayerInventory inv = p.getInventory();
        final ItemStack[] items = inv.getContents();
        int c = 0;
        if (getItemStack() != null) {
            for (final ItemStack is : items) {
                if (is != null) {
                    if (is.isSimilar(getItemStack())) {
                        c += is.getAmount();
                    }
                }
            }
        }
        return c;
    }

    public void removeItems(long amount) {
        final PlayerInventory inv = p.getInventory();
        final ItemStack[] items = inv.getContents();
        int c = 0;
        for (int i = 0; i < items.length; ++i) {
            final ItemStack is = items[i];
            if (is != null) {
                if (getItemStack() != null) {
                    if (is.isSimilar(getItemStack())) {
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
        p.updateInventory();
    }

    public Player getPlayer() {
        return p;
    }

    public String getMaterial() {
        return material;
    }

    public Long getAmount() {
        return amount;
    }
}
