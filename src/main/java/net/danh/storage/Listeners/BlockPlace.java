package net.danh.storage.Listeners;

import net.danh.storage.Storage;
import net.danh.storage.Utils.File;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.metadata.FixedMetadataValue;

public class BlockPlace implements Listener {
    public void setMetaDataPlacedBlock(Block b, boolean placedBlock) {
        b.setMetadata("PlacedBlock", new FixedMetadataValue(Storage.getStorage(), placedBlock));
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        if (File.getConfig().getBoolean("prevent_rebreak")) {
            setMetaDataPlacedBlock(e.getBlock(), true);
        }
    }
}
