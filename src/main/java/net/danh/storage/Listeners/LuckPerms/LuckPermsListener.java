package net.danh.storage.Listeners.LuckPerms;

import net.danh.storage.Manager.Crop.CropStorageManager;
import net.danh.storage.Manager.MineManager;
import net.danh.storage.Manager.Mythic.MythicStorageManager;
import net.danh.storage.Storage;
import net.danh.storage.Utils.SchedulerUtil;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.event.EventBus;
import net.luckperms.api.event.node.NodeMutateEvent;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class LuckPermsListener {

    private static boolean registered;

    public static void register(Storage plugin) {
        if (registered) {
            return;
        }

        Object provider;
        try {
            provider = LuckPermsProvider.get();
        } catch (IllegalStateException ignored) {
            return;
        }

        LuckPerms luckPerms =
                (LuckPerms) provider;
        EventBus eventBus = luckPerms.getEventBus();
        eventBus.subscribe(
                plugin,
                NodeMutateEvent.class,
                event -> onNodeMutate(plugin, event)
        );
        registered = true;
    }

    private static void onNodeMutate(
            Storage plugin,
            NodeMutateEvent event
    ) {
        if (!(event.getTarget() instanceof User user)) {
            return;
        }

        UUID uuid = user.getUniqueId();

        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !player.isOnline()) {
            return;
        }

        SchedulerUtil.runTask(plugin, () -> {
            MineManager.loadPlayerData(player);
            MythicStorageManager.loadPlayerData(player);
            CropStorageManager.loadPlayerData(player);
        });
    }
}
