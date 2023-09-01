package net.danh.storage.Placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.danh.storage.Manager.ItemManager;
import net.danh.storage.Manager.MineManager;
import net.danh.storage.Storage;
import net.danh.storage.Utils.File;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PAPI extends PlaceholderExpansion {
    @Override
    public @NotNull String getIdentifier() {
        return "storage";
    }

    @Override
    public @NotNull String getAuthor() {
        return Storage.getStorage().getDescription().getAuthors().toString();
    }

    @Override
    public @NotNull String getVersion() {
        return Storage.getStorage().getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player p, @NotNull String args) {
        if (p == null) return null;
        if (args.equalsIgnoreCase("status")) {
            return ItemManager.getStatus(p);
        }
        if (args.startsWith("storage_")) {
            String item = args.substring(8);
            return String.valueOf(MineManager.getPlayerBlock(p, item));
        }
        if (args.equalsIgnoreCase("max_storage")) {
            return String.valueOf(MineManager.getMaxBlock(p));
        }
        if (args.startsWith("price_")) {
            String material = args.substring(6);
            ConfigurationSection section = File.getConfig().getConfigurationSection("worth");
            if (section != null) {
                List<String> sell_list = new ArrayList<>(section.getKeys(false));
                if (sell_list.contains(material)) {
                    int worth = section.getInt(material);
                    return String.valueOf(worth);
                }
            }
        }
        return null;
    }
}
