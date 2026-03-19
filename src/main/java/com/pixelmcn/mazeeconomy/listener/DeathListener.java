package com.pixelmcn.mazeeconomy.listener;

import com.pixelmcn.mazeeconomy.MazeEconomy;
import com.pixelmcn.mazeeconomy.util.FormatUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.PlayerDeathEvent;

public class DeathListener implements Listener {

    private final MazeEconomy plugin;

    public DeathListener(MazeEconomy plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        String vName = victim.getName();
        String kName = killer != null ? killer.getName() : null;
        String sym = plugin.getConfigManager().getLocalCurrencySymbol();
        int dec = plugin.getConfigManager().getLocalFormatDecimals();

        // ── mutually exclusive penalties ──────────────────────────────────────────
        if (killer != null) {
            // ── 1. PvP penalty (player kill only)
            double pvpDeduct = plugin.getLocalEconomyManager()
                    .applyPvpPenalty(victim.getUniqueId(), vName, killer.getUniqueId(), kName);

            if (pvpDeduct > 0 && plugin.getConfigManager().isPvpPenaltyAnnounce()) {
                final double amt = pvpDeduct;
                // Notify victim (scheduled so they're alive)
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (victim.isOnline())
                        FormatUtil.sendConfigMessage(plugin, victim, "local.pvp-penalty-victim",
                                FormatUtil.symbol(sym), FormatUtil.amount(amt, dec),
                                FormatUtil.player(kName));
                }, 21L);

                // Notify killer immediately
                if (plugin.getConfigManager().isPvpPenaltyTransferToKiller() && killer.isOnline()) {
                    FormatUtil.sendConfigMessage(plugin, killer, "local.pvp-steal",
                            FormatUtil.symbol(sym), FormatUtil.amount(amt, dec),
                            FormatUtil.player(vName));
                }
            }
        } else {
            // ── 2. General death penalty (non-player deaths)
            double deathDeduct = plugin.getLocalEconomyManager()
                    .applyDeathPenalty(victim.getUniqueId(), vName);

            if (deathDeduct > 0 && plugin.getConfigManager().isDeathPenaltyAnnounce()) {
                final double amt = deathDeduct;
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (victim.isOnline())
                        FormatUtil.sendConfigMessage(plugin, victim, "local.death-penalty",
                                FormatUtil.symbol(sym), FormatUtil.amount(amt, dec));
                }, 20L);
            }
        }
    }
}
