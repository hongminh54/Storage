package net.danh.storage.Database;

import net.danh.storage.Data.CropTransferData;
import net.danh.storage.Storage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class YMLCropTransferStorage {

    private final Storage plugin;
    private final File transfersFolder;
    private int nextId = 1;

    public YMLCropTransferStorage(Storage plugin) {
        this.plugin = plugin;
        File dataFolder = new File(plugin.getDataFolder(), "data");
        this.transfersFolder = new File(dataFolder, "crop-transfers");
        load();
    }

    private void load() {
        // Create transfers directory structure
        if (!transfersFolder.exists()) {
            transfersFolder.mkdirs();
        }

        // Initialize next ID based on existing data across all files
        File[] transferFiles = transfersFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (transferFiles != null) {
            for (File file : transferFiles) {
                FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                if (config.contains("transfers")) {
                    for (String key : config.getConfigurationSection("transfers").getKeys(false)) {
                        try {
                            int id = Integer.parseInt(key);
                            if (id >= nextId) {
                                nextId = id + 1;
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }
        }

        plugin.getLogger().info("Loaded YML CropTransfer Storage (Monthly files)");
    }

    public void insertTransfer(CropTransferData transferData) {
        int id = nextId++;
        File monthlyFile = getMonthlyTransferFile(transferData.getTimestamp());
        FileConfiguration config = YamlConfiguration.loadConfiguration(monthlyFile);

        String path = "transfers." + id;
        config.set(path + ".id", id);
        config.set(path + ".sender", transferData.getSender());
        config.set(path + ".receiver", transferData.getReceiver());
        config.set(path + ".item_name", transferData.getItemName());
        config.set(path + ".amount", transferData.getAmount());
        config.set(path + ".timestamp", formatTimestamp(transferData.getTimestamp()));
        config.set(path + ".status", transferData.getStatus());

        saveTransferFile(monthlyFile, config);
    }

    public List<CropTransferData> getTransferHistory(String playerName, int limit, int offset) {
        List<CropTransferData> allTransfers = getAllTransfers();

        // Filter transfers for the player
        List<CropTransferData> playerTransfers = allTransfers.stream()
                .filter(transfer -> transfer.getSender().equals(playerName) || transfer.getReceiver().equals(playerName))
                .sorted((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp())) // Sort by timestamp desc
                .collect(Collectors.toList());

        // Apply offset and limit
        int start = Math.min(offset, playerTransfers.size());
        int end = Math.min(start + limit, playerTransfers.size());

        return playerTransfers.subList(start, end);
    }

    public int getTotalTransferCount(String playerName) {
        int count = 0;

        File[] transferFiles = transfersFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (transferFiles == null) return 0;

        for (File file : transferFiles) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            if (!config.contains("transfers")) continue;

            for (String key : config.getConfigurationSection("transfers").getKeys(false)) {
                String sender = config.getString("transfers." + key + ".sender");
                String receiver = config.getString("transfers." + key + ".receiver");

                if (playerName.equals(sender) || playerName.equals(receiver)) {
                    count++;
                }
            }
        }

        return count;
    }

    public List<CropTransferData> getAllTransferHistory(int limit) {
        List<CropTransferData> allTransfers = getAllTransfers();

        // Sort by timestamp desc and apply limit
        return allTransfers.stream()
                .sorted((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    private List<CropTransferData> getAllTransfers() {
        List<CropTransferData> transfers = new ArrayList<>();

        File[] transferFiles = transfersFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (transferFiles == null) return transfers;

        for (File file : transferFiles) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            if (!config.contains("transfers")) continue;

            for (String key : config.getConfigurationSection("transfers").getKeys(false)) {
                String path = "transfers." + key;

                try {
                    CropTransferData transfer = new CropTransferData(
                            config.getInt(path + ".id"),
                            config.getString(path + ".sender"),
                            config.getString(path + ".receiver"),
                            config.getString(path + ".item_name"),
                            config.getInt(path + ".amount"),
                            config.getLong(path + ".timestamp"),
                            config.getString(path + ".status")
                    );
                    transfers.add(transfer);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Invalid crop transfer data in file " + file.getName() + " for key: " + key, e);
                }
            }
        }

        return transfers;
    }

    /**
     * Get the monthly transfer file for a given timestamp
     *
     * @param timestamp Timestamp to get file for
     * @return Monthly transfer file
     */
    private File getMonthlyTransferFile(long timestamp) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTimeInMillis(timestamp);

        String fileName = String.format("%04d-%02d.yml",
                cal.get(java.util.Calendar.YEAR),
                cal.get(java.util.Calendar.MONTH) + 1);

        return new File(transfersFolder, fileName);
    }

    /**
     * Save transfer file
     *
     * @param file   File to save
     * @param config Configuration to save
     */
    private void saveTransferFile(File file, FileConfiguration config) {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save crop transfer file: " + file.getName(), e);
        }
    }

    /**
     * Format timestamp to readable string
     *
     * @param timestamp Timestamp to format
     * @return Formatted date string
     */
    private String formatTimestamp(long timestamp) {
        java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return formatter.format(new java.util.Date(timestamp));
    }
}
