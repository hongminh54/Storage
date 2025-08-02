package net.danh.storage.Action;

import net.danh.storage.Manager.ConvertOreManager;
import net.danh.storage.Manager.MineManager;
import net.danh.storage.Manager.ParticleManager;
import net.danh.storage.Manager.SoundManager;
import net.danh.storage.Utils.Chat;
import net.danh.storage.Utils.File;
import org.bukkit.entity.Player;

import java.util.Objects;

public class ConvertOre {

    private final Player player;
    private final String fromMaterial;
    private final String toMaterial;
    private final int amount;

    public ConvertOre(Player player, String fromMaterial, String toMaterial, int amount) {
        this.player = player;
        this.fromMaterial = fromMaterial;
        this.toMaterial = toMaterial;
        this.amount = amount;
    }

    public void doAction() {
        if (!ConvertOreManager.isConvertibleMaterial(fromMaterial)) {
            sendMessage("convert.invalid_material");
            return;
        }

        ConvertOreManager.ConvertOption option = ConvertOreManager.getConvertOption(fromMaterial, toMaterial);
        if (option == null) {
            sendMessage("convert.invalid_conversion");
            return;
        }

        int playerAmount = MineManager.getPlayerBlock(player, fromMaterial);
        if (playerAmount < option.getFromAmount()) {
            sendMessage("convert.insufficient_materials",
                    "#required#", String.valueOf(option.getFromAmount()),
                    "#current#", String.valueOf(playerAmount),
                    "#material#", getMaterialName(fromMaterial));
            return;
        }

        int maxConversions = option.calculateMaxConversions(playerAmount);
        int actualConversions = Math.min(maxConversions, amount > 0 ? amount : maxConversions);

        if (actualConversions <= 0) {
            sendMessage("convert.insufficient_materials",
                    "#required#", String.valueOf(option.getFromAmount()),
                    "#current#", String.valueOf(playerAmount),
                    "#material#", getMaterialName(fromMaterial));
            return;
        }

        int requiredAmount = actualConversions * option.getFromAmount();
        int resultAmount = option.calculateResultAmount(actualConversions);

        int currentToAmount = MineManager.getPlayerBlock(player, toMaterial);
        int maxStorage = MineManager.getMaxBlock(player);

        if (currentToAmount + resultAmount > maxStorage) {
            int availableSpace = maxStorage - currentToAmount;
            int maxPossibleConversions = availableSpace / option.getToAmount();

            if (maxPossibleConversions <= 0) {
                sendMessage("convert.storage_full", "#material#", getMaterialName(toMaterial));
                return;
            }

            actualConversions = maxPossibleConversions;
            requiredAmount = actualConversions * option.getFromAmount();
            resultAmount = option.calculateResultAmount(actualConversions);
        }

        if (MineManager.removeBlockAmount(player, fromMaterial, requiredAmount)) {
            if (MineManager.addBlockAmount(player, toMaterial, resultAmount)) {
                playEffects();
                sendMessage("convert.success",
                        "#from_amount#", String.valueOf(requiredAmount),
                        "#from_material#", getMaterialName(fromMaterial),
                        "#to_amount#", String.valueOf(resultAmount),
                        "#to_material#", getMaterialName(toMaterial));
            } else {
                MineManager.addBlockAmount(player, fromMaterial, requiredAmount);
                sendMessage("convert.failed");
            }
        } else {
            sendMessage("convert.failed");
        }
    }

    private void playEffects() {
        if (File.getConfig().getBoolean("convert.sounds.enabled", true)) {
            SoundManager.playConvertSound(player);
        }

        if (File.getConfig().getBoolean("convert.particles.enabled", true)) {
            ParticleManager.playConvertParticle(player);
        }
    }

    private String getMaterialName(String material) {
        return Objects.requireNonNull(File.getConfig().getString("items." + material, material.split(";")[0]));
    }

    private void sendMessage(String key, String... replacements) {
        String message = File.getMessage().getString(key);
        if (message != null) {
            for (int i = 0; i < replacements.length - 1; i += 2) {
                message = message.replace(replacements[i], replacements[i + 1]);
            }
            player.sendMessage(Chat.colorize(message));
        }
    }
}
