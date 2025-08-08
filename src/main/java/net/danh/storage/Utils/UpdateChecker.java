package net.danh.storage.Utils;

import net.danh.storage.Storage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;

public class UpdateChecker implements Listener {

    private final int RESOURCE_ID = 127776;
    private final Storage plugin;
    private final String pluginVersion;
    private String spigotVersion;
    private boolean updateAvailable;
    private boolean devBuildVersion;

    public UpdateChecker(@NotNull Storage storage) {
        plugin = storage;
        pluginVersion = storage.getDescription().getVersion();
    }

    public String getSpigotVersion() {
        return spigotVersion;
    }

    public void fetch() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (File.getConfig().getBoolean("check_update")) {
                try {
                    HttpsURLConnection con = (HttpsURLConnection) new URL("https://api.spigotmc.org/legacy/update.php?resource=" + RESOURCE_ID).openConnection();
                    con.setRequestMethod("GET");
                    spigotVersion = new BufferedReader(new InputStreamReader(con.getInputStream())).readLine();
                } catch (Exception ex) {
                    plugin.getLogger().info("Failed to check for updates on spigot.");
                    return;
                }

                if (spigotVersion == null || spigotVersion.isEmpty()) {
                    return;
                }

                updateAvailable = spigotIsNewer();
                devBuildVersion = devBuildIsNewer();

                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (devBuildVersion) {
                        plugin.getLogger().warning("You are using DevBuild version of Storage Plugin");
                        plugin.getLogger().warning("Most of things in DevBuild has fix bug and new features for the next version and it can be include another issues");
                        plugin.getLogger().warning("So if you have any issues, please go to my Discord and report it to Danh!");
                    }
                    if (updateAvailable) {
                        plugin.getLogger().warning("An update for Storage (v" + getSpigotVersion() + ") is available at:");
                        plugin.getLogger().warning("https://www.spigotmc.org/resources/" + RESOURCE_ID + "/");
                        plugin.getLogger().warning("You are using version v" + pluginVersion);
                        plugin.getLogger().warning("If your plugin version higher than spigotmc version, you can ignore this notice");
                        Bukkit.getPluginManager().registerEvents(this, plugin);
                    } else {
                        plugin.getLogger().info("This is the latest version of Storage Plugin");
                    }
                });
            }
        });
    }

    private boolean spigotIsNewer() {
        if (spigotVersion == null || spigotVersion.isEmpty() || !spigotVersion.matches("[0-9].[0-9].[0-9]")) {
            return false;
        }

        int[] plV = toReadable(pluginVersion);
        int[] spV = toReadable(spigotVersion);

        if (plV == null || spV == null) return false;

        if (plV[0] < spV[0]) {
            return true;
        }
        if ((plV[1] < spV[1])) {
            return true;
        }
        return plV[2] < spV[2];
    }

    private boolean devBuildIsNewer() {
        if (spigotVersion == null || spigotVersion.isEmpty() || !spigotVersion.matches("[0-9].[0-9].[0-9]")) {
            return false;
        }

        int[] plV = toReadable(pluginVersion);
        int[] spV = toReadable(spigotVersion);

        if (plV == null || spV == null) return false;

        if (plV[0] > spV[0]) {
            return true;
        }
        if ((plV[1] > spV[1])) {
            return true;
        }
        return plV[2] > spV[2];
    }

    private int[] toReadable(@NotNull String version) {
        if (version.endsWith("-SNAPSHOT")) {
            version = version.split("-SNAPSHOT")[0];
        }
        return Arrays.stream(version.split("\\.")).mapToInt(Integer::parseInt).toArray();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(@NotNull PlayerJoinEvent e) {
        if (updateAvailable) {
            if (e.getPlayer().hasPermission("storage.admin")) {
                Player player = e.getPlayer();
                player.sendMessage(ChatColor.GREEN + String.format("An update is available for Storage at %s", "https://www.spigotmc.org/resources/100516/"));
                player.sendMessage(ChatColor.GREEN + String.format("You are using version %s", pluginVersion));
                player.sendMessage(ChatColor.GREEN + "If your plugin version higher than spigotmc version, you can ignore this notice");
            }
        }
    }
}