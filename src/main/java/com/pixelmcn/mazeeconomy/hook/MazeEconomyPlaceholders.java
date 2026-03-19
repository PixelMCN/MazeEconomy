package com.pixelmcn.mazeeconomy.hook;

import com.pixelmcn.mazeeconomy.MazeEconomy;
import com.pixelmcn.mazeeconomy.database.sqlite.SQLiteManager;
import com.pixelmcn.mazeeconomy.model.BalanceEntry;
import com.pixelmcn.mazeeconomy.model.GlobalCurrencyType;
import com.pixelmcn.mazeeconomy.util.FormatUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MazeEconomy PlaceholderAPI Expansion
 *
 * Identifier: %mazeeco_<placeholder>%
 *
 * ─── LOCAL ECONOMY ──────────────────────────────────────────────────────────
 *
 *  %mazeeco_local_balance%                  → 1,234.56
 *  %mazeeco_local_balance_short%            → 1.23K
 *  %mazeeco_local_balance_formatted%        → 1,234.56 Coins
 *  %mazeeco_local_balance_raw%              → 1234.56
 *  %mazeeco_local_symbol%                   → $
 *  %mazeeco_local_currency_name%            → Coin
 *  %mazeeco_local_currency_name_plural%     → Coins
 *  %mazeeco_local_rank%                     → 4  (player's rank on this server)
 *
 *  %mazeeco_local_top_1_name%               → Steve
 *  %mazeeco_local_top_1_balance%            → 9,500.00
 *  %mazeeco_local_top_1_formatted%          → 9,500.00 Coins
 *  (1-10 supported)
 *
 * ─── GLOBAL ECONOMY ─────────────────────────────────────────────────────────
 *
 *  %mazeeco_mazecoins_balance%              → 500
 *  %mazeeco_mazecoins_balance_short%        → 500
 *  %mazeeco_mazecoins_balance_formatted%    → 500 Mazecoins
 *  %mazeeco_mazecoins_balance_raw%          → 500
 *  %mazeeco_mazecoins_symbol%               → ⬡
 *  %mazeeco_mazecoins_currency_name%        → Mazecoin
 *  %mazeeco_mazecoins_currency_name_plural% → Mazecoins
 *
 *  %mazeeco_shards_balance%                 → 200
 *  %mazeeco_shards_balance_short%           → 200
 *  %mazeeco_shards_balance_formatted%       → 200 Shards
 *  %mazeeco_shards_balance_raw%             → 200
 *  %mazeeco_shards_symbol%                  → ◈
 *  %mazeeco_shards_currency_name%           → Shard
 *  %mazeeco_shards_currency_name_plural%    → Shards
 *
 * ─── SERVER INFO ────────────────────────────────────────────────────────────
 *
 *  %mazeeco_server_id%                      → survival
 *  %mazeeco_server_name%                    → Survival Server
 *  %mazeeco_global_enabled%                 → true / false
 */
public class MazeEconomyPlaceholders extends PlaceholderExpansion {

    private final MazeEconomy plugin;

    // Cache for leaderboard data — refreshed every 30 seconds to avoid hammering SQLite
    private List<BalanceEntry> cachedTop = List.of();
    private long lastTopFetch = 0L;
    private static final long TOP_CACHE_MS = 30_000L;

    // Cache player ranks: uuid -> rank
    private final Map<String, Integer> rankCache = new ConcurrentHashMap<>();
    private long lastRankFetch = 0L;

    public MazeEconomyPlaceholders(MazeEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "mazeeco";
    }

    @Override
    public @NotNull String getAuthor() {
        return "PixelMCN";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        // Keep registered across /papi reload
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        // ── Server info ───────────────────────────────────────────────────────
        return switch (params) {
            case "server_id"      -> plugin.getConfigManager().getServerId();
            case "server_name"    -> plugin.getConfigManager().getServerDisplayName();
            case "global_enabled" -> String.valueOf(plugin.isGlobalEconomyActive());

            // ── Local symbol / name ───────────────────────────────────────────
            case "local_symbol"              -> plugin.getConfigManager().getLocalCurrencySymbol();
            case "local_currency_name"       -> plugin.getConfigManager().getLocalCurrencyName();
            case "local_currency_name_plural"-> plugin.getConfigManager().getLocalCurrencyNamePlural();

            // ── Local balance ─────────────────────────────────────────────────
            case "local_balance"   -> {
                if (player == null) yield "";
                double bal = localBalance(player);
                int dec    = plugin.getConfigManager().getLocalFormatDecimals();
                yield FormatUtil.formatAmount(bal, dec);
            }
            case "local_balance_short" -> {
                if (player == null) yield "";
                yield shortFormat(localBalance(player));
            }
            case "local_balance_formatted" -> {
                if (player == null) yield "";
                double bal  = localBalance(player);
                int dec     = plugin.getConfigManager().getLocalFormatDecimals();
                String cur  = plugin.getConfigManager().getLocalCurrencyNamePlural();
                yield FormatUtil.formatAmount(bal, dec) + " " + cur;
            }
            case "local_balance_raw" -> {
                if (player == null) yield "";
                yield String.valueOf(localBalance(player));
            }

            // ── Local rank ────────────────────────────────────────────────────
            case "local_rank" -> {
                if (player == null) yield "";
                yield String.valueOf(getPlayerRank(player));
            }

            // ── Mazecoin symbol / name ────────────────────────────────────────
            case "mazecoins_symbol"              -> plugin.getConfigManager().getMazecoinSymbol();
            case "mazecoins_currency_name"       -> plugin.getConfigManager().getMazecoinName();
            case "mazecoins_currency_name_plural"-> plugin.getConfigManager().getMazecoinNamePlural();

            // ── Mazecoin balance ──────────────────────────────────────────────
            case "mazecoins_balance" -> {
                if (player == null || !plugin.isGlobalEconomyActive()) yield "0";
                double bal = globalBalance(player, GlobalCurrencyType.MAZECOINS);
                int dec    = plugin.getConfigManager().getMazecoinFormatDecimals();
                yield FormatUtil.formatAmount(bal, dec);
            }
            case "mazecoins_balance_short" -> {
                if (player == null || !plugin.isGlobalEconomyActive()) yield "0";
                yield shortFormat(globalBalance(player, GlobalCurrencyType.MAZECOINS));
            }
            case "mazecoins_balance_formatted" -> {
                if (player == null || !plugin.isGlobalEconomyActive()) yield "0 Mazecoins";
                double bal = globalBalance(player, GlobalCurrencyType.MAZECOINS);
                int dec    = plugin.getConfigManager().getMazecoinFormatDecimals();
                String cur = plugin.getConfigManager().getMazecoinNamePlural();
                yield FormatUtil.formatAmount(bal, dec) + " " + cur;
            }
            case "mazecoins_balance_raw" -> {
                if (player == null || !plugin.isGlobalEconomyActive()) yield "0";
                yield String.valueOf(globalBalance(player, GlobalCurrencyType.MAZECOINS));
            }

            // ── Shard symbol / name ───────────────────────────────────────────
            case "shards_symbol"              -> plugin.getConfigManager().getShardSymbol();
            case "shards_currency_name"       -> plugin.getConfigManager().getShardName();
            case "shards_currency_name_plural"-> plugin.getConfigManager().getShardNamePlural();

            // ── Shard balance ─────────────────────────────────────────────────
            case "shards_balance" -> {
                if (player == null || !plugin.isGlobalEconomyActive()) yield "0";
                double bal = globalBalance(player, GlobalCurrencyType.SHARDS);
                int dec    = plugin.getConfigManager().getShardFormatDecimals();
                yield FormatUtil.formatAmount(bal, dec);
            }
            case "shards_balance_short" -> {
                if (player == null || !plugin.isGlobalEconomyActive()) yield "0";
                yield shortFormat(globalBalance(player, GlobalCurrencyType.SHARDS));
            }
            case "shards_balance_formatted" -> {
                if (player == null || !plugin.isGlobalEconomyActive()) yield "0 Shards";
                double bal = globalBalance(player, GlobalCurrencyType.SHARDS);
                int dec    = plugin.getConfigManager().getShardFormatDecimals();
                String cur = plugin.getConfigManager().getShardNamePlural();
                yield FormatUtil.formatAmount(bal, dec) + " " + cur;
            }
            case "shards_balance_raw" -> {
                if (player == null || !plugin.isGlobalEconomyActive()) yield "0";
                yield String.valueOf(globalBalance(player, GlobalCurrencyType.SHARDS));
            }

            case "local_wallet" -> {
                if (player == null) yield "";
                int dec = plugin.getConfigManager().getLocalFormatDecimals();
                yield FormatUtil.formatAmount(localWallet(player), dec);
            }
            case "local_bank" -> {
                if (player == null) yield "";
                int dec = plugin.getConfigManager().getLocalFormatDecimals();
                yield FormatUtil.formatAmount(localBank(player), dec);
            }
            case "local_total" -> {
                if (player == null) yield "";
                int dec = plugin.getConfigManager().getLocalFormatDecimals();
                yield FormatUtil.formatAmount(localBalance(player), dec);
            }
            case "local_wallet_formatted" -> {
                if (player == null) yield "";
                int dec = plugin.getConfigManager().getLocalFormatDecimals();
                String cur = plugin.getConfigManager().getLocalCurrencyNamePlural();
                yield FormatUtil.formatAmount(localWallet(player), dec) + " " + cur;
            }
            case "local_bank_formatted" -> {
                if (player == null) yield "";
                int dec = plugin.getConfigManager().getLocalFormatDecimals();
                String cur = plugin.getConfigManager().getLocalCurrencyNamePlural();
                yield FormatUtil.formatAmount(localBank(player), dec) + " " + cur;
            }
            case "local_wallet_short" -> {
                if (player == null) yield "";
                yield shortFormat(localWallet(player));
            }
            case "local_bank_short" -> {
                if (player == null) yield "";
                yield shortFormat(localBank(player));
            }
            default -> resolveTopPlaceholder(params);
        };
    }

    /**
     * Handles dynamic leaderboard placeholders:
     *   local_top_<N>_name        → player name at rank N
     *   local_top_<N>_balance     → formatted balance at rank N
     *   local_top_<N>_formatted   → balance + currency name at rank N
     *   local_top_<N>_raw         → raw balance at rank N
     *
     * N = 1 to 10
     */
    private @Nullable String resolveTopPlaceholder(String params) {
        // Pattern: local_top_<N>_<type>
        if (!params.startsWith("local_top_")) return null;

        String[] parts = params.split("_"); // ["local","top","N","type"]
        if (parts.length < 4) return null;

        int rank;
        try {
            rank = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            return null;
        }
        if (rank < 1 || rank > 10) return null;

        // Rebuild type in case it has underscores (e.g. "balance_formatted" won't happen here but safe)
        String type = parts[3];

        List<BalanceEntry> top = getCachedTop();
        if (rank > top.size()) {
            return type.equals("name") ? "N/A" : "0";
        }

        BalanceEntry entry = top.get(rank - 1);
        int dec    = plugin.getConfigManager().getLocalFormatDecimals();
        String cur = plugin.getConfigManager().getLocalCurrencyNamePlural();

        return switch (type) {
            case "name"      -> entry.playerName();
            case "balance"   -> FormatUtil.formatAmount(entry.balance(), dec);
            case "formatted" -> FormatUtil.formatAmount(entry.balance(), dec) + " " + cur;
            case "raw"       -> String.valueOf(entry.balance());
            case "short"     -> shortFormat(entry.balance());
            default          -> null;
        };
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private double localBalance(OfflinePlayer player) {
        return plugin.getLocalEconomyManager().getBalance(player.getUniqueId());
    }

    private double localWallet(OfflinePlayer player) {
        return plugin.getLocalEconomyManager().getWallet(player.getUniqueId());
    }

    private double localBank(OfflinePlayer player) {
        return plugin.getLocalEconomyManager().getBank(player.getUniqueId());
    }

    private double globalBalance(OfflinePlayer player, GlobalCurrencyType currency) {
        return plugin.getGlobalEconomyManager().getBalance(player.getUniqueId(), currency);
    }

    /**
     * Returns a cached top-10 leaderboard, refreshed every 30 seconds.
     */
    private List<BalanceEntry> getCachedTop() {
        long now = System.currentTimeMillis();
        if (now - lastTopFetch > TOP_CACHE_MS) {
            SQLiteManager db = plugin.getSqLiteManager();
            cachedTop = db.getTopBalances(10, 0);
            // Also rebuild rank cache
            rankCache.clear();
            for (int i = 0; i < cachedTop.size(); i++) {
                rankCache.put(cachedTop.get(i).uuid().toString(), i + 1);
            }
            lastTopFetch = now;
        }
        return cachedTop;
    }

    /**
     * Get a player's rank on this server's leaderboard (1-based).
     * Returns the total count + 1 if the player is not in the top list.
     */
    private int getPlayerRank(OfflinePlayer player) {
        getCachedTop(); // Ensure cache is fresh
        return rankCache.getOrDefault(player.getUniqueId().toString(),
                plugin.getLocalEconomyManager().getTotalPages()
                        * plugin.getConfigManager().getBaltopEntriesPerPage() + 1);
    }

    /**
     * Formats a number in short K/M/B/T notation.
     * Examples: 1234 → 1.23K, 1500000 → 1.50M
     */
    private String shortFormat(double value) {
        if (value >= 1_000_000_000_000.0) return String.format("%.2fT", value / 1_000_000_000_000.0);
        if (value >= 1_000_000_000.0)     return String.format("%.2fB", value / 1_000_000_000.0);
        if (value >= 1_000_000.0)         return String.format("%.2fM", value / 1_000_000.0);
        if (value >= 1_000.0)             return String.format("%.2fK", value / 1_000.0);
        return String.format("%.2f", value);
    }
}
