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
        ItemStack templateItem = itemStack.clone();
        templateItem.setAmount(1);
        int remainingAmount = amount;

        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() == templateItem.getType() && item.isSimilar(templateItem)) {
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

        while (remainingAmount > 0) {
            ItemStack newItem = templateItem.clone();
            int stackSize = Math.min(remainingAmount, newItem.getMaxStackSize());
            newItem.setAmount(stackSize);
            inventory.addItem(newItem);
            remainingAmount -= stackSize;
        }
    }

    private int calculateFreeSlots(Player player, ItemStack itemStack) {
        int free_slot = 0;
        ItemStack templateItem = itemStack.clone();
        templateItem.setAmount(1);

        for (int i = 0; i < player.getInventory().getStorageContents().length; i++) {
            ItemStack istack = player.getInventory().getItem(i);
            if (istack == null || istack.getType().equals(Material.AIR)) {
                free_slot += templateItem.getMaxStackSize();
            } else if (istack.isSimilar(templateItem)) {
                int spaceLeft = istack.getMaxStackSize() - istack.getAmount();
                if (spaceLeft > 0) free_slot += spaceLeft;
            }
        }
        return free_slot;
    }

    public void doAction() {
        Optional<XMaterial> xMaterial = XMaterial.matchXMaterial(material);
        if (xMaterial.isPresent()) {
            ItemStack itemStack = xMaterial.get().parseItem();
            if (itemStack != null) {
                int amount = MineManager.getPlayerBlock(p, getMaterialData());
                if (getAmount() > 0) {
                    if (amount >= getAmount()) {
                        int free_items = calculateFreeSlots(p, itemStack);
                        if (free_items >= getAmount()) {
                            if (MineManager.removeBlockAmount(p, getMaterialData(), getAmount())) {
                                addItemToInventory(p, itemStack, getAmount());
                                p.sendMessage(Chat.colorize(Objects.requireNonNull(File.getMessage().getString("user.action.withdraw.withdraw_item")).replace("#amount#", String.valueOf(getAmount())).replace("#material#", Objects.requireNonNull(File.getConfig().getString("items." + getMaterialData()))).replace("#player#", p.getName()).replace("#item_amount#", String.valueOf(MineManager.getPlayerBlock(p, getMaterialData()))).replace("#max_storage#", String.valueOf(MineManager.getMaxBlock(p)))));
                            }
                        } else {
                            p.sendMessage(Chat.colorize(Objects.requireNonNull(File.getMessage().getString("user.not_enough_slot")).replace("<slots>", String.valueOf(free_items))));
                        }
                    } else {
                        p.sendMessage(Chat.colorize(Objects.requireNonNull(File.getMessage().getString("user.action.withdraw.not_enough_in_storage")).replace("#amount#", String.valueOf(amount)).replace("#material#", Objects.requireNonNull(File.getConfig().getString("items." + getMaterialData())))));
                    }
                } else {
                    int free_items = calculateFreeSlots(p, itemStack);
                    if (amount <= free_items) {
                        if (MineManager.removeBlockAmount(p, getMaterialData(), amount)) {
                            addItemToInventory(p, itemStack, amount);
                            p.sendMessage(Chat.colorize(Objects.requireNonNull(File.getMessage().getString("user.action.withdraw.withdraw_item")).replace("#amount#", String.valueOf(amount)).replace("#material#", Objects.requireNonNull(File.getConfig().getString("items." + getMaterialData()))).replace("#player#", p.getName()).replace("#item_amount#", String.valueOf(MineManager.getPlayerBlock(p, getMaterialData()))).replace("#max_storage#", String.valueOf(MineManager.getMaxBlock(p)))));
                        }
                    } else if (free_items > 0) {
                        if (MineManager.removeBlockAmount(p, getMaterialData(), free_items)) {
                            addItemToInventory(p, itemStack, free_items);
                            p.sendMessage(Chat.colorize(Objects.requireNonNull(File.getMessage().getString("user.action.withdraw.withdraw_item")).replace("#amount#", String.valueOf(free_items)).replace("#material#", Objects.requireNonNull(File.getConfig().getString("items." + getMaterialData()))).replace("#player#", p.getName()).replace("#item_amount#", String.valueOf(MineManager.getPlayerBlock(p, getMaterialData()))).replace("#max_storage#", String.valueOf(MineManager.getMaxBlock(p)))));
                        } else
                            p.sendMessage(Chat.colorize(Objects.requireNonNull(File.getMessage().getString("user.not_enough_slot")).replace("<slots>", String.valueOf(free_items))));
                    } else {
                        p.sendMessage(Chat.colorize(Objects.requireNonNull(File.getMessage().getString("user.action.withdraw.inventory_full"))));
                    }
                }
            } else {
                p.sendMessage(Chat.colorize(Objects.requireNonNull(File.getMessage().getString("user.action.withdraw.cannot_create_item"))));
            }
        } else {
            p.sendMessage(Chat.colorize(Objects.requireNonNull(File.getMessage().getString("admin.invalid_material")).replace("#material#", material).replace("#materials#", "Please check available materials")));
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
