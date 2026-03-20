package com.pixelmcn.mazeeconomy.manager;

import com.pixelmcn.mazeeconomy.MazeEconomy;
import com.pixelmcn.mazeeconomy.api.events.GlobalBalanceChangeEvent;
import com.pixelmcn.mazeeconomy.database.mariadb.MariaDBManager;
import com.pixelmcn.mazeeconomy.model.EconomyResponse;
import com.pixelmcn.mazeeconomy.model.GlobalCurrencyType;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages global economy (Mazecoins & Shards) backed by MariaDB.
 * Balances are cached per-player and synced via DB polling.
 */
public class GlobalEconomyManager {

    private final MazeEconomy plugin;
    private final MariaDBManager db;

    // cache: uuid -> { currency -> balance }
    private final Map<UUID, Map<GlobalCurrencyType, Double>> cache = new ConcurrentHashMap<>();

    // players currently online (for sync)
    private final Set<UUID> onlinePlayers = ConcurrentHashMap.newKeySet();

    private BukkitTask syncTask;

    public GlobalEconomyManager(MazeEconomy plugin, MariaDBManager db) {
        this.plugin = plugin;
        this.db = db;
    }

    // ── Sync Task ─────────────────────────────────────────────────────────────

    public void startSyncTask() {
        long interval = plugin.getConfigManager().getGlobalSyncIntervalTicks();
        syncTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::syncOnlinePlayers, interval,
                interval);
        plugin.getLogger().info("Global economy sync task started (every " + interval + " ticks).");
    }

    public void stopSyncTask() {
        if (syncTask != null && !syncTask.isCancelled()) {
            syncTask.cancel();
        }
    }

    private void syncOnlinePlayers() {
        for (UUID uuid : onlinePlayers) {
            var playerCache = cache.get(uuid);
            if (playerCache == null)
                continue;
            for (GlobalCurrencyType currency : GlobalCurrencyType.values()) {
                double dbBal = db.getBalance(uuid, currency);
                if (dbBal >= 0) {
                    double cached = playerCache.getOrDefault(currency, 0.0);
                    if (Double.compare(dbBal, cached) != 0) {
                        playerCache.put(currency, dbBal);
                        // Fire event on main thread
                        double finalDbBal = dbBal;
                        Bukkit.getScheduler().runTask(plugin, () -> fireChangeEvent(uuid, currency, cached, finalDbBal,
                                GlobalBalanceChangeEvent.Reason.SYNC));
                    }
                }
            }
        }
    }

    public void flushAll() {
        // All writes already go directly to DB, so no flush needed.
        plugin.getLogger().info("Global economy flush complete.");
    }

    // ── Player Lifecycle ──────────────────────────────────────────────────────

    public void loadPlayer(UUID uuid, String playerName) {
        onlinePlayers.add(uuid);
        Map<GlobalCurrencyType, Double> playerCache = new HashMap<>();
        for (GlobalCurrencyType currency : GlobalCurrencyType.values()) {
            ensureAccount(uuid, playerName, currency);
            double bal = db.getBalance(uuid, currency);
            playerCache.put(currency, Math.max(0.0, bal));
        }
        cache.put(uuid, playerCache);
        db.updatePlayerName(uuid, playerName);
    }

    public void unloadPlayer(UUID uuid) {
        onlinePlayers.remove(uuid);
        cache.remove(uuid);
    }

    private void ensureAccount(UUID uuid, String playerName, GlobalCurrencyType currency) {
        if (!db.hasAccount(uuid, currency)) {
            double starting = switch (currency) {
                case MAZECOINS -> plugin.getConfigManager().getMazecoinStartingBalance();
                case SHARDS -> plugin.getConfigManager().getShardStartingBalance();
            };
            db.createAccount(uuid, playerName, currency, starting);
        }
    }

    // ── Balance ───────────────────────────────────────────────────────────────

    public double getBalance(UUID uuid, GlobalCurrencyType currency) {
        var playerCache = cache.get(uuid);
        if (playerCache != null)
            return playerCache.getOrDefault(currency, 0.0);
        // Offline player: fetch directly from DB
        double bal = db.getBalance(uuid, currency);
        return bal >= 0 ? bal : 0.0;
    }

    public java.util.List<java.util.Map.Entry<String, Double>> getTopBalances(GlobalCurrencyType currency, int limit) {
        return db.getTopBalances(currency, limit);
    }

    public EconomyResponse setBalance(UUID uuid, String playerName, GlobalCurrencyType currency, double amount) {
        if (amount < 0)
            return EconomyResponse.invalidAmount();
        double clamped = Math.max(0.0, amount);
        double old = getBalance(uuid, currency);

        updateCache(uuid, currency, clamped);
        db.setBalance(uuid, playerName, currency, clamped);
        db.logTransaction(currency, null, uuid, "SYSTEM", playerName, clamped, "SET", "Admin set");

        fireChangeEvent(uuid, currency, old, clamped, GlobalBalanceChangeEvent.Reason.ADMIN_SET);
        return EconomyResponse.success(clamped, clamped);
    }

    public EconomyResponse deposit(UUID uuid, String playerName, GlobalCurrencyType currency, double amount) {
        if (amount <= 0)
            return EconomyResponse.invalidAmount();
        double old = getBalance(uuid, currency);
        double newBal = old + amount;

        updateCache(uuid, currency, newBal);
        db.setBalance(uuid, playerName, currency, newBal);
        db.logTransaction(currency, null, uuid, "SYSTEM", playerName, amount, "DEPOSIT", null);

        fireChangeEvent(uuid, currency, old, newBal, GlobalBalanceChangeEvent.Reason.DEPOSIT);
        return EconomyResponse.success(amount, newBal);
    }

    public EconomyResponse withdraw(UUID uuid, String playerName, GlobalCurrencyType currency, double amount) {
        if (amount <= 0)
            return EconomyResponse.invalidAmount();
        double old = getBalance(uuid, currency);
        if (old < amount)
            return EconomyResponse.insufficientFunds(old);

        double newBal = old - amount;
        updateCache(uuid, currency, newBal);
        db.setBalance(uuid, playerName, currency, newBal);
        db.logTransaction(currency, uuid, null, playerName, "SYSTEM", amount, "WITHDRAW", null);

        fireChangeEvent(uuid, currency, old, newBal, GlobalBalanceChangeEvent.Reason.WITHDRAW);
        return EconomyResponse.success(amount, newBal);
    }

    public boolean has(UUID uuid, GlobalCurrencyType currency, double amount) {
        return getBalance(uuid, currency) >= amount;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void updateCache(UUID uuid, GlobalCurrencyType currency, double value) {
        cache.computeIfAbsent(uuid, k -> new HashMap<>()).put(currency, value);
    }

    private void fireChangeEvent(UUID uuid, GlobalCurrencyType currency,
            double oldBal, double newBal, GlobalBalanceChangeEvent.Reason reason) {
        var player = Bukkit.getPlayer(uuid);
        var event = new GlobalBalanceChangeEvent(uuid, player, currency, oldBal, newBal, reason);
        // Fire on main thread if currently async
        if (Bukkit.isPrimaryThread()) {
            Bukkit.getPluginManager().callEvent(event);
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> Bukkit.getPluginManager().callEvent(event));
        }
    }
}
