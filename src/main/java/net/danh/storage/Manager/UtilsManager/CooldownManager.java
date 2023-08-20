package net.danh.storage.Manager.UtilsManager;

import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;

public class CooldownManager {

    private static final Map<Location, Integer> cooldown = new HashMap<>();


    public static void setCooldown(Location location, int time) {
        if (time < 1) {
            cooldown.remove(location);
        } else {
            cooldown.put(location, time);
        }
    }

    public static int getCooldown(Location location) {
        return cooldown.getOrDefault(location, 0);
    }
}
