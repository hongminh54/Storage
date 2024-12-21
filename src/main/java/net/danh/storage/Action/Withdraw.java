package net.danh.storage.Action;

import com.cryptomorin.xseries.XMaterial;
import net.danh.storage.Manager.MineManager;
import net.danh.storage.NMS.NMSAssistant;
import net.danh.storage.Utils.Chat;
import net.danh.storage.Utils.File;
import net.danh.storage.Utils.Number;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

public class Withdraw {

    private final Player p;
    private final String material;
    private final String materialData;
    private final Integer amount;

    public Withdraw(Player p, @NotNull String material, Integer amount) {
        this.p = p;
        String material_data = material.replace(":", ";");
        if (material_data.contains(";")) materialData = material_data;
        else materialData = material_data + ";0";
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

    public static void addItemToInventory(Player player, ItemStack itemStack, int amount) {
        PlayerInventory inventory = player.getInventory();
        itemStack.setAmount(amount);
        int remainingAmount = amount;
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() == itemStack.getType() && item.isSimilar(itemStack)) {
                int spaceLeft = item.getMaxStackSize() - item.getAmount();
                if (spaceLeft > 0) {
                    int toAdd = Math.min(spaceLeft, remainingAmount);
                    item.setAmount(item.getAmount() + toAdd);
                    remainingAmount -= toAdd;

                    if (remainingAmount <= 0) {
                        break;
                    }
                }
            }
        }
        if (remainingAmount > 0) {
            itemStack.setAmount(remainingAmount);
            inventory.addItem(itemStack);
        }
    }

    public void doAction() {
        Optional<XMaterial> xMaterial = XMaterial.matchXMaterial(material);
        if (xMaterial.isPresent()) {
            ItemStack itemStack = xMaterial.get().parseItem();
            if (itemStack != null) {
                int amount = MineManager.getPlayerBlock(p, getMaterialData());
                if (getAmount() > 0) {
                    if (amount >= getAmount()) {
                        int free_slot = 0;
                        for (int i = 0; i < p.getInventory().getStorageContents().length; i++) {
                            ItemStack istack = p.getInventory().getItem(i);
                            if (istack == null || istack.getType().equals(Material.AIR)) {
                                free_slot += itemStack.getMaxStackSize();
                            } else if (istack.isSimilar(itemStack)) {
                                int spaceLeft = istack.getMaxStackSize() - istack.getAmount();
                                if (spaceLeft > 0)
                                    free_slot += spaceLeft;
                            }
                        }
                        int free_items = free_slot;
                        if (free_items >= getAmount()) {
                            if (MineManager.removeBlockAmount(p, getMaterialData(), getAmount())) {
                                addItemToInventory(p, itemStack, getAmount());
                                p.sendMessage(Chat.colorize(Objects.requireNonNull(File.getMessage().getString("user.action.withdraw.withdraw_item"))
                                        .replace("#amount#", String.valueOf(getAmount()))
                                        .replace("#material#", Objects.requireNonNull(File.getConfig().getString("items." + getMaterialData())))
                                        .replace("#player#", p.getName())
                                        .replace("#item_amount#", String.valueOf(MineManager.getPlayerBlock(p, getMaterialData())))
                                        .replace("#max_storage#", String.valueOf(MineManager.getMaxBlock(p)))));
                            }
                        } else {
                            p.sendMessage(Chat.colorize(Objects.requireNonNull(File.getMessage().getString("user.not_enough_slot"))
                                    .replace("<slots>", String.valueOf(free_items))));
                        }
                    } else {
                        p.sendMessage(Chat.colorize(Objects.requireNonNull(File.getMessage().getString("user.not_enough_items"))
                                .replace("<amount>", String.valueOf(amount))));
                    }
                } else {
                    int free_slot = 0;
                    for (int i = 0; i < p.getInventory().getStorageContents().length; i++) {
                        ItemStack istack = p.getInventory().getItem(i);
                        if (istack == null || istack.getType().equals(Material.AIR)) {
                            free_slot += itemStack.getMaxStackSize();
                        } else if (istack.isSimilar(itemStack)) {
                            int spaceLeft = istack.getMaxStackSize() - istack.getAmount();
                            if (spaceLeft > 0)
                                free_slot += spaceLeft;
                        }
                    }
                    int free_items = free_slot;
                    if (amount <= free_items) {
                        if (MineManager.removeBlockAmount(p, getMaterialData(), amount)) {
                            addItemToInventory(p, itemStack, amount);
                            p.sendMessage(Chat.colorize(Objects.requireNonNull(File.getMessage().getString("user.action.withdraw.withdraw_item"))
                                    .replace("#amount#", String.valueOf(amount))
                                    .replace("#material#", Objects.requireNonNull(File.getConfig().getString("items." + getMaterialData())))
                                    .replace("#player#", p.getName())
                                    .replace("#item_amount#", String.valueOf(MineManager.getPlayerBlock(p, getMaterialData())))
                                    .replace("#max_storage#", String.valueOf(MineManager.getMaxBlock(p)))));
                        }
                    } else if (free_items > 0) {
                        if (MineManager.removeBlockAmount(p, getMaterialData(), free_items)) {
                            addItemToInventory(p, itemStack, free_items);
                            p.sendMessage(Chat.colorize(Objects.requireNonNull(File.getMessage().getString("user.action.withdraw.withdraw_item"))
                                    .replace("#amount#", String.valueOf(free_items))
                                    .replace("#material#", Objects.requireNonNull(File.getConfig().getString("items." + getMaterialData())))
                                    .replace("#player#", p.getName())
                                    .replace("#item_amount#", String.valueOf(MineManager.getPlayerBlock(p, getMaterialData())))
                                    .replace("#max_storage#", String.valueOf(MineManager.getMaxBlock(p)))));
                        } else
                            p.sendMessage(Chat.colorize(Objects.requireNonNull(File.getMessage().getString("user.not_enough_slot"))
                                    .replace("<slots>", String.valueOf(free_items))));
                    }
                }
            }
        }
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
}
