package net.danh.storage.Database;

import net.danh.storage.Data.MobTransferData;
import net.danh.storage.Storage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class YMLMobTransferStorage {

    private final Storage plugin;
    private final File transfersFolder;
    private int nextId = 1;

    public YMLMobTransferStorage(Storage plugin) {
        this.plugin = plugin;
        File dataFolder = new File(plugin.getDataFolder(), "data");
        this.transfersFolder = new File(dataFolder, "mob-transfers");
        load();
    }

    private void load() {
        if (!transfersFolder.exists()) {
            transfersFolder.mkdirs();
        }

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

        plugin.getLogger().info("Loaded YML MobTransfer Storage (Monthly files)");
    }

    public void insertTransfer(MobTransferData transferData) {
        int id = nextId++;
        File monthlyFile = getMonthlyTransferFile(transferData.getTimestamp());
        FileConfiguration config = YamlConfiguration.loadConfiguration(monthlyFile);

        String path = "transfers." + id;
        config.set(path + ".id", id);
        config.set(path + ".sender", transferData.getSender());
        config.set(path + ".receiver", transferData.getReceiver());
        config.set(path + ".item_name", transferData.getItemName());
        config.set(path + ".amount", transferData.getAmount());
        config.set(path + ".timestamp", transferData.getTimestamp());
        config.set(path + ".status", transferData.getStatus());

        saveTransferFile(monthlyFile, config);
    }

    public List<MobTransferData> getTransferHistory(String playerName, int limit, int offset) {
        List<MobTransferData> allTransfers = getAllTransfers();

        List<MobTransferData> playerTransfers = allTransfers.stream()
                .filter(transfer -> transfer.getSender().equals(playerName) || transfer.getReceiver().equals(playerName))
                .sorted((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()))
                .collect(Collectors.toList());

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

    public long getTotalSentAmount(String playerName) {
        return getTotalAmount("sender", playerName);
    }

    public long getTotalReceivedAmount(String playerName) {
        return getTotalAmount("receiver", playerName);
    }

    private long getTotalAmount(String column, String playerName) {
        long total = 0;

        File[] transferFiles = transfersFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (transferFiles == null) return 0;

        for (File file : transferFiles) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            if (!config.contains("transfers")) continue;

            for (String key : config.getConfigurationSection("transfers").getKeys(false)) {
                String path = "transfers." + key;
                String owner = config.getString(path + "." + column);
                String status = config.getString(path + ".status", "");

                if (playerName.equals(owner) && status.startsWith("SUCCESS")) {
                    total += config.getInt(path + ".amount", 0);
                }
            }
        }

        return total;
    }

    private List<MobTransferData> getAllTransfers() {
        List<MobTransferData> transfers = new ArrayList<>();

        File[] transferFiles = transfersFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (transferFiles == null) return transfers;

        for (File file : transferFiles) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            if (!config.contains("transfers")) continue;

            for (String key : config.getConfigurationSection("transfers").getKeys(false)) {
                String path = "transfers." + key;

                try {
                    MobTransferData transfer = new MobTransferData(
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
                    plugin.getLogger().log(Level.WARNING, "Invalid mob transfer data in file " + file.getName() + " for key: " + key, e);
                }
            }
        }

        return transfers;
    }

    private File getMonthlyTransferFile(long timestamp) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTimeInMillis(timestamp);

        String fileName = String.format("%04d-%02d.yml",
                cal.get(java.util.Calendar.YEAR),
                cal.get(java.util.Calendar.MONTH) + 1);

        return new File(transfersFolder, fileName);
    }

    private void saveTransferFile(File file, FileConfiguration config) {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save mob transfer file: " + file.getName(), e);
        }
    }
}
