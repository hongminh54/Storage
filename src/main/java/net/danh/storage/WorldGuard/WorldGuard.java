package net.danh.storage.WorldGuard;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.codemc.worldguardwrapper.WorldGuardWrapper;
import org.codemc.worldguardwrapper.flag.IWrappedFlag;
import org.codemc.worldguardwrapper.flag.WrappedState;

import java.util.Optional;

public class WorldGuard {

    public static boolean handleForLocation(Player player, Location loc) {
        IWrappedFlag<WrappedState> flag = getStateFlag("block-break");
        if (flag == null) {
            return true;
        }

        WrappedState state = WorldGuardWrapper.getInstance().queryFlag(player, loc, flag).orElse(WrappedState.DENY);
        return state.equals(WrappedState.ALLOW);
    }

    public static IWrappedFlag<WrappedState> getStateFlag(String flagName) {
        Optional<IWrappedFlag<WrappedState>> flagOptional = WorldGuardWrapper.getInstance().getFlag(flagName, WrappedState.class);
        return flagOptional.orElse(null);
    }
}
