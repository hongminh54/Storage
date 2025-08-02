package net.danh.storage.Manager;

import com.cryptomorin.xseries.XSound;
import net.danh.storage.Storage;
import net.danh.storage.Utils.File;
import net.danh.storage.Utils.SoundContext;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class SoundManager {

    private static final Map<Player, Map<String, Long>> soundCooldowns = new HashMap<>();
    private static final long SOUND_COOLDOWN_MS = 100;

    public static void playSound(Player player, SoundType soundType) {
        if (player == null || !player.isOnline()) return;

        FileConfiguration config = File.getConfig();

        if (!config.getBoolean("sounds.enabled", true)) return;

        String soundName = config.getString(soundType.getConfigPath() + ".sound");
        if (soundName == null || soundName.equalsIgnoreCase("none")) return;

        float volume = (float) config.getDouble(soundType.getConfigPath() + ".volume", 1.0);
        float pitch = (float) config.getDouble(soundType.getConfigPath() + ".pitch", 1.0);

        try {
            XSound sound = XSound.matchXSound(soundName).orElse(XSound.UI_BUTTON_CLICK);
            sound.play(player, volume, pitch);
        } catch (Exception e) {
            Storage.getStorage().getLogger().warning("Failed to play sound: " + soundName + " for player: " + player.getName());
        }
    }

    public static void playSound(Player player, String soundName, float volume, float pitch) {
        if (player == null || !player.isOnline()) return;

        FileConfiguration config = File.getConfig();
        if (!config.getBoolean("sounds.enabled", true)) return;

        if (!canPlaySound(player, soundName)) return;

        try {
            XSound sound = XSound.matchXSound(soundName).orElse(XSound.UI_BUTTON_CLICK);
            sound.play(player, volume, pitch);

            recordSoundPlayed(player, soundName);
        } catch (Exception e) {
            Storage.getStorage().getLogger().warning("Failed to play sound: " + soundName + " for player: " + player.getName());
        }
    }

    private static boolean canPlaySound(Player player, String soundName) {
        long currentTime = System.currentTimeMillis();

        Map<String, Long> playerCooldowns = soundCooldowns.computeIfAbsent(player, k -> new HashMap<>());
        Long lastPlayed = playerCooldowns.get(soundName);

        return lastPlayed == null || (currentTime - lastPlayed) >= SOUND_COOLDOWN_MS;
    }

    private static void recordSoundPlayed(Player player, String soundName) {
        long currentTime = System.currentTimeMillis();

        Map<String, Long> playerCooldowns = soundCooldowns.computeIfAbsent(player, k -> new HashMap<>());
        playerCooldowns.put(soundName, currentTime);
    }

    public static void playClickSound(Player player) {
        playSound(player, SoundType.GUI_CLICK);
    }

    public static void playOpenSound(Player player) {
        playSound(player, SoundType.GUI_OPEN);
    }

    public static void playCloseSound(Player player) {
        playSound(player, SoundType.GUI_CLOSE);
    }

    public static void playSuccessSound(Player player) {
        playSound(player, SoundType.ACTION_SUCCESS);
    }

    public static void playErrorSound(Player player) {
        playSound(player, SoundType.ACTION_ERROR);
    }

    public static void playNavigationSound(Player player) {
        playSound(player, SoundType.NAVIGATION);
    }

    public static void playDepositSound(Player player) {
        playSound(player, SoundType.DEPOSIT);
    }

    public static void playWithdrawSound(Player player) {
        playSound(player, SoundType.WITHDRAW);
    }

    public static void playSellSound(Player player) {
        playSound(player, SoundType.SELL);
    }

    public static void playActionSound(Player player, String configPath) {
        playActionSound(player, configPath, File.getItemStorage());
    }

    public static void playActionSound(Player player, String configPath, FileConfiguration config) {
        if (player == null || !player.isOnline()) return;

        if (!File.getConfig().getBoolean("sounds.enabled", true)) return;

        if (!config.getBoolean(configPath + ".sound.enabled", false)) return;

        String soundName = config.getString(configPath + ".sound.name");
        if (soundName == null || soundName.equalsIgnoreCase("none")) return;

        float volume = (float) config.getDouble(configPath + ".sound.volume", 1.0);
        float pitch = (float) config.getDouble(configPath + ".sound.pitch", 1.0);

        playSound(player, soundName, volume, pitch);
    }

    public static void playItemSound(Player player, FileConfiguration config, String itemPath) {
        playItemSound(player, config, itemPath, SoundContext.INITIAL_OPEN);
    }

    public static void playItemSound(Player player, FileConfiguration config, String itemPath, SoundContext context) {
        if (player == null || !player.isOnline()) return;

        if (!File.getConfig().getBoolean("sounds.enabled", true)) return;

        String soundPath = itemPath;
        if (itemPath.startsWith("items.")) {
            soundPath = itemPath + ".sound";
        }

        if (!config.getBoolean(soundPath + ".enabled", false)) return;

        String soundName = config.getString(soundPath + ".name");
        if (soundName == null || soundName.equalsIgnoreCase("none")) return;

        // Only play sound if context is not silent
        if (context == SoundContext.SILENT) return;

        float volume = (float) config.getDouble(soundPath + ".volume", 1.0);
        float pitch = (float) config.getDouble(soundPath + ".pitch", 1.0);

        playSound(player, soundName, volume, pitch);
    }

    public static void playChatActionSound(Player player, String actionType) {
        if (player == null || !player.isOnline()) return;

        FileConfiguration config = File.getConfig();
        if (!config.getBoolean("sounds.enabled", true)) return;

        String soundPath = "sounds.chat_actions." + actionType;

        if (!config.getBoolean(soundPath + ".enabled", false)) return;

        String soundName = config.getString(soundPath + ".name");
        if (soundName == null || soundName.equalsIgnoreCase("none")) return;

        float volume = (float) config.getDouble(soundPath + ".volume", 1.0);
        float pitch = (float) config.getDouble(soundPath + ".pitch", 1.0);

        playSound(player, soundName, volume, pitch);
    }

    public static void playChatDepositSound(Player player) {
        playChatActionSound(player, "deposit");
    }

    public static void playChatWithdrawSound(Player player) {
        playChatActionSound(player, "withdraw");
    }

    public static void playChatSellSound(Player player) {
        playChatActionSound(player, "sell");
    }

    public static void playChatErrorSound(Player player) {
        playChatActionSound(player, "error");
    }

    public static void playEventSound(Player player, String eventSoundType) {
        if (player == null || !player.isOnline()) return;

        FileConfiguration eventConfig = File.getEventConfig();
        if (!eventConfig.getBoolean("notifications.sounds.enabled", true)) return;

        String soundPath = "notifications.sounds." + eventSoundType;

        String soundName = eventConfig.getString(soundPath + ".name");
        if (soundName == null || soundName.equalsIgnoreCase("none")) return;

        float volume = (float) eventConfig.getDouble(soundPath + ".volume", 1.0);
        float pitch = (float) eventConfig.getDouble(soundPath + ".pitch", 1.0);

        playSound(player, soundName, volume, pitch);
    }

    public static void playEventStartSound(Player player) {
        playEventSound(player, "event_start");
    }

    public static void playEventEndSound(Player player) {
        playEventSound(player, "event_end");
    }

    public static void playRewardSound(Player player) {
        playEventSound(player, "reward_received");
    }

    public static void playConvertSound(Player player) {
        if (player == null || !player.isOnline()) return;

        FileConfiguration config = File.getConfig();
        if (!config.getBoolean("convert.sounds.enabled", true)) return;

        String soundName = config.getString("convert.sounds.name");
        if (soundName == null || soundName.equalsIgnoreCase("none")) return;

        float volume = (float) config.getDouble("convert.sounds.volume", 1.0);
        float pitch = (float) config.getDouble("convert.sounds.pitch", 1.0);

        playSound(player, soundName, volume, pitch);
    }

    public enum SoundType {
        GUI_CLICK("gui.click"),
        GUI_OPEN("gui.open"),
        GUI_CLOSE("gui.close"),
        ACTION_SUCCESS("action.success"),
        ACTION_ERROR("action.error"),
        NAVIGATION("navigation"),
        DEPOSIT("action.deposit"),
        WITHDRAW("action.withdraw"),
        SELL("action.sell");

        private final String configPath;

        SoundType(String configPath) {
            this.configPath = configPath;
        }

        public String getConfigPath() {
            return "sounds." + configPath;
        }
    }
}
