package net.danh.storage.Database;

import net.danh.storage.Data.MythicTransferData;
import net.danh.storage.Storage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class YMLMythicTransferStorage {
    private final Storage plugin;
    private final File transferFile;
    private FileConfiguration transferConfig;

    public YMLMythicTransferStorage(Storage plugin) {
        this.plugin = plugin;
        this.transferFile = new File(plugin.getDataFolder(), "mythic_transfer_logs.yml");
        loadConfig();
    }

    private void loadConfig() {
        if (!transferFile.exists()) {
            try {
                transferFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create mythic_transfer_logs.yml", e);
            }
        }
        transferConfig = YamlConfiguration.loadConfiguration(transferFile);
    }

    public void insertTransfer(MythicTransferData transferData) {
        try {
            List<String> transfers = transferConfig.getStringList("transfers");

            String transferEntry = String.format("%s|%s|%s|%d|%d|%s",
                    transferData.getSender(),
                    transferData.getReceiver(),
                    transferData.getItemName(),
                    transferData.getAmount(),
                    transferData.getTimestamp(),
                    transferData.getStatus());

            transfers.add(transferEntry);
            transferConfig.set("transfers", transfers);
            transferConfig.save(transferFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save mythic transfer to YML", e);
        }
    }

    public List<MythicTransferData> getTransferHistory(String playerName, int limit, int offset) {
        List<MythicTransferData> result = new ArrayList<>();
        List<String> allTransfers = transferConfig.getStringList("transfers");

        List<MythicTransferData> playerTransfers = new ArrayList<>();
        int id = 0;

        for (int i = allTransfers.size() - 1; i >= 0; i--) {
            String entry = allTransfers.get(i);
            String[] parts = entry.split("\\|");

            if (parts.length == 6) {
                String sender = parts[0];
                String receiver = parts[1];

                if (sender.equals(playerName) || receiver.equals(playerName)) {
                    try {
                        MythicTransferData data = new MythicTransferData(
                                id++,
                                sender,
                                receiver,
                                parts[2],
                                Integer.parseInt(parts[3]),
                                Long.parseLong(parts[4]),
                                parts[5]
                        );
                        playerTransfers.add(data);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }

        int start = Math.min(offset, playerTransfers.size());
        int end = Math.min(start + limit, playerTransfers.size());

        return playerTransfers.subList(start, end);
    }

    public int getTotalTransferCount(String playerName) {
        List<String> allTransfers = transferConfig.getStringList("transfers");
        int count = 0;

        for (String entry : allTransfers) {
            String[] parts = entry.split("\\|");
            if (parts.length >= 2) {
                if (parts[0].equals(playerName) || parts[1].equals(playerName)) {
                    count++;
                }
            }
        }

        return count;
    }
}
