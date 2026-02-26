package net.danh.storage.Listeners;

import net.danh.storage.Manager.MineManager;
import net.danh.storage.Storage;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.metadata.FixedMetadataValue;

public class BlockPlace implements Listener {
    public void setMetaDataPlacedBlock(Block b, boolean placedBlock) {
        b.setMetadata("PlacedBlock", new FixedMetadataValue(Storage.getStorage(), placedBlock));
        b.setMetadata("placed", new FixedMetadataValue(Storage.getStorage(), placedBlock));
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        if (!net.danh.storage.Utils.File.getConfig().getBoolean("prevent_rebreak")) {
            return;
        }
        if (e.getPlayer().hasPermission("storage.preventrebreak.bypass")) {
            return;
        }
        setMetaDataPlacedBlock(e.getBlockPlaced(), true);
        MineManager.markPersistPlacedBlock(e.getBlockPlaced());
    }
}
