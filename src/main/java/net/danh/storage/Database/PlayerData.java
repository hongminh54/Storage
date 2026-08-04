package net.danh.storage.Database;

public record PlayerData(String player, String data, int max, boolean autoPickup) {
    public PlayerData(String player, String data, int max) {
        this(player, data, max, false);
    }

}
