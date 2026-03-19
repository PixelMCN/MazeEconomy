package com.pixelmcn.mazeeconomy.api.impl;

import com.pixelmcn.mazeeconomy.MazeEconomy;
import com.pixelmcn.mazeeconomy.model.EconomyResponse;
import com.pixelmcn.mazeeconomy.model.GlobalCurrencyType;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

/**
 * MazeEconomy Public API
 *
 * Use this class to interact with MazeEconomy from your plugin.
 *
 * <pre>
 * // Obtaining the API instance:
 * Plugin plugin = Bukkit.getPluginManager().getPlugin("MazeEconomy");
 * if (plugin instanceof MazeEconomy mazeEconomy) {
 *     MazeEconomyAPI api = mazeEconomy.getApi();
 * }
 * </pre>
 *
 * Listen to {@link com.pixelmcn.mazeeconomy.api.events.GlobalBalanceChangeEvent}
 * and {@link com.pixelmcn.mazeeconomy.api.events.LocalBalanceChangeEvent} for reactive hooks.
 */
public class MazeEconomyAPI {

    private final MazeEconomy plugin;

    public MazeEconomyAPI(MazeEconomy plugin) {
        this.plugin = plugin;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  GLOBAL ECONOMY — Mazecoins & Shards
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Get a player's balance for a global currency.
     *
     * @param player   The player (online or offline)
     * @param currency The currency (MAZECOINS or SHARDS)
     * @return The current balance (≥ 0)
     */
    public double getGlobalBalance(OfflinePlayer player, GlobalCurrencyType currency) {
        return plugin.getGlobalEconomyManager().getBalance(player.getUniqueId(), currency);
    }

    /**
     * Get a player's Mazecoin balance.
     */
    public double getMazecoins(OfflinePlayer player) {
        return getGlobalBalance(player, GlobalCurrencyType.MAZECOINS);
    }

    /**
     * Get a player's Shard balance.
     */
    public double getShards(OfflinePlayer player) {
        return getGlobalBalance(player, GlobalCurrencyType.SHARDS);
    }

    /**
     * Check if a player has at least {@code amount} of a global currency.
     */
    public boolean hasGlobal(OfflinePlayer player, GlobalCurrencyType currency, double amount) {
        return plugin.getGlobalEconomyManager().has(player.getUniqueId(), currency, amount);
    }

    /**
     * Add to a player's global balance.
     *
     * @param player   Target player
     * @param currency Currency type
     * @param amount   Positive amount to add
     * @return EconomyResponse with result details
     */
    public EconomyResponse depositGlobal(OfflinePlayer player, GlobalCurrencyType currency, double amount) {
        return plugin.getGlobalEconomyManager().deposit(
                player.getUniqueId(),
                nameOf(player),
                currency,
                amount
        );
    }

    /**
     * Remove from a player's global balance.
     *
     * @param player   Target player
     * @param currency Currency type
     * @param amount   Positive amount to remove
     * @return EconomyResponse — check {@link EconomyResponse#isSuccess()} and
     *         {@link EconomyResponse#getResult()} for insufficient funds etc.
     */
    public EconomyResponse withdrawGlobal(OfflinePlayer player, GlobalCurrencyType currency, double amount) {
        return plugin.getGlobalEconomyManager().withdraw(
                player.getUniqueId(),
                nameOf(player),
                currency,
                amount
        );
    }

    /**
     * Directly set a player's global balance (admin use).
     */
    public EconomyResponse setGlobalBalance(OfflinePlayer player, GlobalCurrencyType currency, double amount) {
        return plugin.getGlobalEconomyManager().setBalance(
                player.getUniqueId(),
                nameOf(player),
                currency,
                amount
        );
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  LOCAL ECONOMY — Server-specific currency (also accessible via Vault)
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Get a player's local economy balance.
     */
    public double getLocalBalance(OfflinePlayer player) {
        return plugin.getLocalEconomyManager().getBalance(player.getUniqueId());
    }

    /**
     * Check if a player has at least {@code amount} of local currency.
     */
    public boolean hasLocal(OfflinePlayer player, double amount) {
        return plugin.getLocalEconomyManager().has(player.getUniqueId(), amount);
    }

    /**
     * Add to a player's local balance.
     */
    public EconomyResponse depositLocal(OfflinePlayer player, double amount) {
        return plugin.getLocalEconomyManager().deposit(player.getUniqueId(), nameOf(player), amount);
    }

    /**
     * Remove from a player's local balance.
     */
    public EconomyResponse withdrawLocal(OfflinePlayer player, double amount) {
        return plugin.getLocalEconomyManager().withdraw(player.getUniqueId(), nameOf(player), amount);
    }

    /**
     * Directly set a player's local balance (admin use).
     */
    public EconomyResponse setLocalBalance(OfflinePlayer player, double amount) {
        return plugin.getLocalEconomyManager().setBalance(player.getUniqueId(), nameOf(player), amount);
    }

    // ── Meta ──────────────────────────────────────────────────────────────────

    /**
     * Get the server ID this instance is running on (from config).
     */
    public String getServerId() {
        return plugin.getConfigManager().getServerId();
    }

    /**
     * Get the local currency name (singular).
     */
    public String getLocalCurrencyName() {
        return plugin.getConfigManager().getLocalCurrencyName();
    }

    /**
     * Get the display name of a global currency (singular).
     */
    public String getGlobalCurrencyName(GlobalCurrencyType currency) {
        return switch (currency) {
            case MAZECOINS -> plugin.getConfigManager().getMazecoinName();
            case SHARDS    -> plugin.getConfigManager().getShardName();
        };
    }

    // ── Internal helper ───────────────────────────────────────────────────────

    private String nameOf(OfflinePlayer player) {
        return player.getName() != null ? player.getName() : player.getUniqueId().toString();
    }
}
