package net.danh.storage.CMD.handler.crop.user;

import net.danh.storage.CMD.handler.crop.CropCommand;
import net.danh.storage.Manager.Crop.CropStorageManager;
import net.danh.storage.Utils.ChatUtils;
import net.danh.storage.Utils.File;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CropStatusCommand extends CropCommand {

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!requirePlayer(sender)) {
            return;
        }

        Player player = (Player) sender;

        if (isCropStorageWorldBlacklisted(player)) {
            sendWorldBlacklisted(sender, "CropStorage", player.getWorld().getName());
            return;
        }

        boolean autoPickup = CropStorageManager.getToggleStatus(player);
        boolean groundStoreSystemEnabled = CropStorageManager.isGroundStoreSystemEnabled();
        boolean groundStoreEnabled = CropStorageManager.isGroundStoreEnabled(player);
        int maxStorage = CropStorageManager.getMaxStorage(player);

        String autoPickupStatus = getStatusMessage(autoPickup);
        String groundStoreStatus;
        if (!groundStoreSystemEnabled) {
            groundStoreStatus = ChatUtils.colorizewp(File.getMessage().getString(
                    "cropstorage.status_disabled",
                    "&cDisabled"));
        } else {
            groundStoreStatus = getStatusMessage(groundStoreEnabled);
        }

        String[] placeholders = {
                "#autopickup#",
                "#groundstore#",
                "#max#",
                "#world#"
        };
        String[] replacements = {
                autoPickupStatus,
                groundStoreStatus,
                String.valueOf(maxStorage),
                player.getWorld().getName()
        };

        List<String> lines = File.getMessage().getStringList("cropstorage.user.status");
        if (lines == null || lines.isEmpty()) {
            lines = new ArrayList<>();
            lines.add("#prefix# &dCropStorage &7Status:");
            lines.add("&7- AutoPickup: #autopickup#");
            lines.add("&7- GroundStore: #groundstore#");
            lines.add("&7- Max: &e#max#");
            lines.add("&7- World: &f#world#");
        }

        for (String line : lines) {
            String msg = line;
            for (int i = 0; i < placeholders.length; i++) {
                msg = msg.replace(placeholders[i], replacements[i]);
            }
            sender.sendMessage(ChatUtils.colorizewp(msg));
        }
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }

    @Override
    public String getPermission() {
        return "storage.cropstorage.use";
    }

    @Override
    public String getUsage() {
        return "/cropstorage status";
    }

    @Override
    public String getDescription() {
        return "Show CropStorage status";
    }
}
