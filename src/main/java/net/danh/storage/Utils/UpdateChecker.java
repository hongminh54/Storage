package net.danh.storage.Utils;

import net.danh.storage.Storage;
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
import java.nio.charset.StandardCharsets;

public class UpdateChecker implements Listener {

    private static final int RESOURCE_ID = 127776;
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 5000;
    private final Storage plugin;
    private final String pluginVersion;
    private String spigotVersion;
    private boolean updateAvailable;
    private boolean devBuildVersion;

    public UpdateChecker(@NotNull Storage storage) {
        plugin = storage;
        pluginVersion = storage.getDescription().getVersion();
    }

    private static int compareVersions(int[] a, int[] b) {
        int max = Math.max(a.length, b.length);
        for (int i = 0; i < max; i++) {
            int av = i < a.length ? a[i] : 0;
            int bv = i < b.length ? b[i] : 0;
            if (av != bv) {
                return Integer.compare(av, bv);
            }
        }
        return 0;
    }

    private static boolean isDevBuild(String version) {
        if (version == null) {
            return false;
        }
        String v = version.toUpperCase();
        return v.contains("SNAPSHOT")
                || v.contains("DEV")
                || v.contains("ALPHA")
                || v.contains("BETA")
                || v.contains("RC");
    }

    public String getSpigotVersion() {
        return spigotVersion;
    }

    public void fetch() {
        SchedulerUtil.runTaskAsynchronously(plugin, () -> {
            if (File.getConfig().getBoolean("check_update")) {
                try {
                    HttpsURLConnection con = (HttpsURLConnection) new URL(
                            "https://api.spigotmc.org/legacy/update.php?resource="
                                    + RESOURCE_ID
                    ).openConnection();
                    con.setRequestMethod("GET");
                    con.setConnectTimeout(CONNECT_TIMEOUT_MS);
                    con.setReadTimeout(READ_TIMEOUT_MS);
                    con.setUseCaches(false);

                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(
                                    con.getInputStream(),
                                    StandardCharsets.UTF_8
                            )
                    )) {
                        spigotVersion = reader.readLine();
                    }
                } catch (Exception ex) {
                    plugin.getLogger().info("Failed to check for updates on spigot.");
                    return;
                }

                if (spigotVersion == null || spigotVersion.isEmpty()) {
                    return;
                }

                updateAvailable = spigotIsNewer();
                devBuildVersion = isDevBuild(pluginVersion) || devBuildIsNewer();

                SchedulerUtil.runTask(plugin, () -> {
                    if (devBuildVersion) {
                        plugin.getLogger().warning("You are using DevBuild version of Storage Plugin");
                        plugin.getLogger().warning("Most of things in DevBuild has fix bug and new features for the next version and it can be include another issues");
                        plugin.getLogger().warning("So if you have any issues, please report at https://github.com/hongminh54/Storage/issues!");
                    }
                    if (updateAvailable) {
                        plugin.getLogger().warning("An update for Storage (v" + getSpigotVersion() + ") is available at:");
                        plugin.getLogger().warning("https://www.spigotmc.org/resources/" + RESOURCE_ID + "/");
                        plugin.getLogger().warning("You are using version v" + pluginVersion);
                        plugin.getLogger().warning("If your plugin version higher than spigotmc version, you can ignore this notice");
                    } else {
                        plugin.getLogger().info("This is the latest version of Storage Plugin");
                    }
                });
            }
        });
    }

    private boolean spigotIsNewer() {
        if (spigotVersion == null || spigotVersion.isEmpty()) {
            return false;
        }

        int[] plV = toReadable(pluginVersion);
        int[] spV = toReadable(spigotVersion);
        if (plV == null || spV == null) {
            return false;
        }

        return compareVersions(plV, spV) < 0;
    }

    private boolean devBuildIsNewer() {
        if (spigotVersion == null || spigotVersion.isEmpty()) {
            return false;
        }

        int[] plV = toReadable(pluginVersion);
        int[] spV = toReadable(spigotVersion);

        if (plV == null || spV == null) {
            return false;
        }

        return compareVersions(plV, spV) > 0;
    }

    private int[] toReadable(String version) {
        if (version == null) {
            return null;
        }

        String normalized = version.trim();
        if (normalized.isEmpty()) {
            return null;
        }

        if (normalized.startsWith("v") || normalized.startsWith("V")) {
            normalized = normalized.substring(1);
        }

        int plusIndex = normalized.indexOf('+');
        if (plusIndex >= 0) {
            normalized = normalized.substring(0, plusIndex);
        }

        int dashIndex = normalized.indexOf('-');
        if (dashIndex >= 0) {
            normalized = normalized.substring(0, dashIndex);
        }

        String[] parts = normalized.split("\\.");
        if (parts.length == 0) {
            return null;
        }

        int[] result = new int[]{0, 0, 0};
        for (int i = 0; i < Math.min(3, parts.length); i++) {
            String part = parts[i].trim();
            if (part.isEmpty()) {
                return null;
            }
            try {
                result[i] = Integer.parseInt(part);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return result;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(@NotNull PlayerJoinEvent e) {
        if (!updateAvailable) {
            return;
        }
        if (!e.getPlayer().hasPermission("storage.admin")) {
            return;
        }

        Player player = e.getPlayer();
        String message = File.getMessage().getString("admin.new_version_available");
        if (message != null) {
            String[] placeholders = new String[]{"#old_version#", "#new_version#"};
            String[] replacements = new String[]{pluginVersion, spigotVersion};
            for (int i = 0; i < placeholders.length; i++) {
                message = message.replace(placeholders[i], replacements[i]);
            }
            player.sendMessage(ChatUtils.colorize(message));
        }
        player.sendMessage(
                ChatUtils.colorize(
                        "#prefix# &ahttps://www.spigotmc.org/resources/"
                                + RESOURCE_ID
                                + "/"
                )
        );
    }
}