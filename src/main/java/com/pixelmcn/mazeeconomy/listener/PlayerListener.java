package com.pixelmcn.mazeeconomy.listener;

import com.pixelmcn.mazeeconomy.MazeEconomy;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final MazeEconomy plugin;

    public PlayerListener(MazeEconomy plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        var uuid   = player.getUniqueId();
        var name   = player.getName();

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            // Local economy — always available
            plugin.getLocalEconomyManager().loadPlayer(uuid, name);

            // Global economy — only if active
            if (plugin.isGlobalEconomyActive() && plugin.getGlobalEconomyManager() != null) {
                plugin.getGlobalEconomyManager().loadPlayer(uuid, name);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        var uuid = event.getPlayer().getUniqueId();

        plugin.getLocalEconomyManager().unloadPlayer(uuid);

        if (plugin.isGlobalEconomyActive() && plugin.getGlobalEconomyManager() != null) {
            plugin.getGlobalEconomyManager().unloadPlayer(uuid);
        }
    }
}
