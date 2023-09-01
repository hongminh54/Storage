package net.danh.storage.Database;

public class PlayerData {
    private final String player;
    private final String data;
    private final int max;

    public PlayerData(String player, String data, int max) {
        this.player = player;
        this.data = data;
        this.max = max;
    }

    public String getPlayer() {
        return player;
    }

    public String getData() {
        return data;
    }

    public int getMax() {
        return max;
    }
}
