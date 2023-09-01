package net.danh.storage.WorldGuard;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.codemc.worldguardwrapper.WorldGuardWrapper;
import org.codemc.worldguardwrapper.flag.IWrappedFlag;
import org.codemc.worldguardwrapper.flag.WrappedState;

import java.util.Optional;
import java.util.logging.Level;

public class WorldGuard {

    static boolean registered = false;

    public static void register(final JavaPlugin plugin) {
        Plugin worldGuard = Bukkit.getServer().getPluginManager().getPlugin("WorldGuard");
        if (worldGuard != null) {
            WorldGuardWrapper wrapper = WorldGuardWrapper.getInstance();
            Optional<IWrappedFlag<WrappedState>> miningFlag = wrapper.registerFlag("storage", WrappedState.class, WrappedState.ALLOW);
            miningFlag.ifPresent(wrappedStateIWrappedFlag -> {
                plugin.getLogger().log(Level.INFO, "Registered flag " + wrappedStateIWrappedFlag.getName());
                registered = true;
            });
        }
    }

    public static boolean handleForLocation(Player player, Location loc) {
        IWrappedFlag<WrappedState> flag = getStateFlag("storage");
        if (flag == null) {
            return true;
        }

        WrappedState state = WorldGuardWrapper.getInstance().queryFlag(player, loc, flag).orElse(WrappedState.ALLOW);
        return state.equals(WrappedState.ALLOW);
    }

    public static IWrappedFlag<WrappedState> getStateFlag(String flagName) {
        Optional<IWrappedFlag<WrappedState>> flagOptional = WorldGuardWrapper.getInstance().getFlag(flagName, WrappedState.class);
        if (flagOptional.isPresent() && registered) {
            return flagOptional.get();
        } else {
            return null;
        }
    }
}
