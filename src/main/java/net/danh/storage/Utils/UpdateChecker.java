package net.danh.storage.Utils;

import net.danh.storage.Manager.UtilsManager.FileManager;
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

    private final int RESOURCE_ID = 100516;
    private final Storage plugin;
    private final String pluginVersion;
    private String spigotVersion;
    private boolean updateAvailable;

    public UpdateChecker(@NotNull Storage storage) {
        plugin = storage;
        pluginVersion = storage.getDescription().getVersion();
    }

    public String getSpigotVersion() {
        return spigotVersion;
    }

    public void fetch() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (FileManager.getConfig().getBoolean("check_update")) {
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

                if (!updateAvailable) {
                    return;
                }

                Bukkit.getScheduler().runTask(plugin, () -> {
                    plugin.getLogger().info("An update for Storage (v" + getSpigotVersion() + ") is available at:");
                    plugin.getLogger().info("https://www.spigotmc.org/resources/" + RESOURCE_ID + "/");
                    Bukkit.getPluginManager().registerEvents(this, plugin);
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
        } else if ((plV[1] < spV[1])) {
            return true;
        } else {
            return plV[2] < spV[2];
        }
    }

    private int[] toReadable(@NotNull String version) {
        if (version.contains("-SNAPSHOT")) {
            version = version.split("-SNAPSHOT")[0];
        }
        if (version.contains("-B")) {
            version = version.split("-B")[0];
        }

        return Arrays.stream(version.split("\\.")).mapToInt(Integer::parseInt).toArray();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(@NotNull PlayerJoinEvent e) {
        if (e.getPlayer().hasPermission("storage.admin")) {
            Player player = e.getPlayer();
            player.sendMessage(ChatColor.GREEN + String.format("An update is available for Storage at %s", "https://www.spigotmc.org/resources/100516/"));
        }
    }
}