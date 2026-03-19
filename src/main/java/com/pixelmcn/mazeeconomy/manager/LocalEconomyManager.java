package com.pixelmcn.mazeeconomy.manager;

import com.pixelmcn.mazeeconomy.MazeEconomy;
import com.pixelmcn.mazeeconomy.api.events.LocalBalanceChangeEvent;
import com.pixelmcn.mazeeconomy.database.sqlite.SQLiteManager;
import com.pixelmcn.mazeeconomy.model.BalanceEntry;
import com.pixelmcn.mazeeconomy.model.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages local economy: wallet (spendable) + bank (earning interest).
 * Vault always reads/writes the wallet.
 */
public class LocalEconomyManager {

    private final MazeEconomy plugin;
    private final SQLiteManager db;

    // Cache: uuid -> [wallet, bank]
    private final Map<UUID, double[]> cache = new ConcurrentHashMap<>();

    private BukkitTask interestTask;

    public LocalEconomyManager(MazeEconomy plugin, SQLiteManager db) {
        this.plugin = plugin;
        this.db = db;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    public void loadPlayer(UUID uuid, String name) {
        if (!db.hasAccount(uuid)) {
            db.createAccount(uuid, name);
            cache.put(uuid, new double[] {
                    plugin.getConfigManager().getLocalStartingWallet(),
                    plugin.getConfigManager().getLocalStartingBank()
            });
        } else {
            double w = db.getWallet(uuid);
            double b = db.getBank(uuid);
            cache.put(uuid, new double[] { Math.max(0, w), Math.max(0, b) });
            db.updatePlayerName(uuid, name);
        }
    }

    public void unloadPlayer(UUID uuid) {
        cache.remove(uuid);
    }

    public boolean hasAccount(UUID uuid) {
        return cache.containsKey(uuid) || db.hasAccount(uuid);
    }

    public boolean createAccount(UUID uuid, String name) {
        if (hasAccount(uuid))
            return true;
        boolean ok = db.createAccount(uuid, name);
        if (ok)
            cache.put(uuid, new double[] {
                    plugin.getConfigManager().getLocalStartingWallet(),
                    plugin.getConfigManager().getLocalStartingBank()
            });
        return ok;
    }

    // ── Interest Task ─────────────────────────────────────────────────────────

    public void startInterestTask() {
        if (!plugin.getConfigManager().isBankInterestEnabled())
            return;

        String intervalString = plugin.getConfigManager().getBankInterestInterval();
        long ticks = com.pixelmcn.mazeeconomy.util.TimeUtil.parseTicks(intervalString);

        if (ticks <= 0) {
            plugin.getLogger().warning("Invalid bank interest interval (" + intervalString + "), defaulting to 60m.");
            ticks = 72000L;
        }

        interestTask = org.bukkit.Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::applyInterest, ticks,
                ticks);
        plugin.getLogger().info("Bank interest task started (" + intervalString + ").");
    }

    public void stopInterestTask() {
        if (interestTask != null && !interestTask.isCancelled())
            interestTask.cancel();
    }

    private void applyInterest() {
        double rate = plugin.getConfigManager().getBankInterestRate() / 100.0;
        // Apply to all cached (online) players
        for (Map.Entry<UUID, double[]> entry : cache.entrySet()) {
            UUID uuid = entry.getKey();
            double bankBal = entry.getValue()[1];
            if (bankBal <= 0)
                continue;

            double interest = bankBal * rate;
            double newBank = bankBal + interest;
            entry.getValue()[1] = newBank;

            var player = Bukkit.getPlayer(uuid);
            String name = player != null ? player.getName() : uuid.toString();
            db.setBank(uuid, name, newBank);
            db.logTransaction(null, uuid, "SYSTEM", name, interest, "INTEREST", "Bank interest");

            // Notify player on main thread
            if (player != null && plugin.getConfigManager().isDeathPenaltyAnnounce()) {
                String symbol = plugin.getConfigManager().getLocalCurrencySymbol();
                int dec = plugin.getConfigManager().getLocalFormatDecimals();
                Bukkit.getScheduler().runTask(plugin,
                        () -> com.pixelmcn.mazeeconomy.util.FormatUtil.sendConfigMessage(plugin, player,
                                "local.bank-interest",
                                com.pixelmcn.mazeeconomy.util.FormatUtil.symbol(symbol),
                                com.pixelmcn.mazeeconomy.util.FormatUtil.amount(interest, dec)));
            }
        }
    }

    // ── Balance Reads ─────────────────────────────────────────────────────────

    public double getWallet(UUID uuid) {
        double[] cached = cache.get(uuid);
        if (cached != null)
            return cached[0];
        double w = db.getWallet(uuid);
        return w >= 0 ? w : 0;
    }

    public double getBank(UUID uuid) {
        double[] cached = cache.get(uuid);
        if (cached != null)
            return cached[1];
        double b = db.getBank(uuid);
        return b >= 0 ? b : 0;
    }

    /** Total = wallet + bank (used for Vault balance) */
    public double getBalance(UUID uuid) {
        return getWallet(uuid) + getBank(uuid);
    }

    public boolean has(UUID uuid, double amount) {
        return getWallet(uuid) >= amount;
    }

    // ── Wallet Operations (Vault-facing) ──────────────────────────────────────

    public EconomyResponse deposit(UUID uuid, String name, double amount) {
        if (amount <= 0)
            return EconomyResponse.invalidAmount();
        return depositWallet(uuid, name, amount, LocalBalanceChangeEvent.Reason.DEPOSIT);
    }

    public EconomyResponse withdraw(UUID uuid, String name, double amount) {
        if (amount <= 0)
            return EconomyResponse.invalidAmount();
        double wallet = getWallet(uuid);
        if (wallet < amount)
            return EconomyResponse.insufficientFunds(wallet);
        return withdrawWallet(uuid, name, amount, LocalBalanceChangeEvent.Reason.WITHDRAW);
    }

    public EconomyResponse setBalance(UUID uuid, String name, double amount) {
        if (amount < 0)
            return EconomyResponse.invalidAmount();
        double clamped = Math.max(0, amount);
        double old = getWallet(uuid);
        updateWalletCache(uuid, clamped);
        db.setWallet(uuid, name, clamped);
        db.logTransaction(null, uuid, "SYSTEM", name, clamped, "SET", "Admin set wallet");
        fireChangeEvent(uuid, old, clamped, LocalBalanceChangeEvent.Reason.ADMIN_SET);
        return EconomyResponse.success(clamped, clamped);
    }

    // ── Bank Operations ───────────────────────────────────────────────────────

    public EconomyResponse depositToBank(UUID uuid, String name, double amount) {
        if (amount <= 0)
            return EconomyResponse.invalidAmount();
        double wallet = getWallet(uuid);
        if (wallet < amount)
            return EconomyResponse.insufficientFunds(wallet);

        double newWallet = wallet - amount;
        double newBank = getBank(uuid) + amount;
        updateBothCache(uuid, newWallet, newBank);
        db.setBoth(uuid, name, newWallet, newBank);
        db.logTransaction(uuid, uuid, name, name, amount, "BANK_DEPOSIT", null);
        return EconomyResponse.success(amount, newWallet);
    }

    public EconomyResponse withdrawFromBank(UUID uuid, String name, double amount) {
        if (amount <= 0)
            return EconomyResponse.invalidAmount();
        double bank = getBank(uuid);
        if (bank < amount)
            return EconomyResponse.insufficientFunds(bank);

        double fee = amount * (plugin.getConfigManager().getBankWithdrawFee() / 100.0);
        double received = amount - fee;
        double newBank = bank - amount;
        double newWallet = getWallet(uuid) + received;
        updateBothCache(uuid, newWallet, newBank);
        db.setBoth(uuid, name, newWallet, newBank);
        db.logTransaction(uuid, uuid, name, name, amount, "BANK_WITHDRAW", fee > 0 ? "Fee: " + fee : null);
        return EconomyResponse.success(received, newWallet);
    }

    // ── Pay ───────────────────────────────────────────────────────────────────

    public EconomyResponse pay(UUID from, String fromName, UUID to, String toName, double amount) {
        if (amount <= 0)
            return EconomyResponse.invalidAmount();
        if (from.equals(to))
            return EconomyResponse.failure("Cannot pay yourself");
        double fromWallet = getWallet(from);
        if (fromWallet < amount)
            return EconomyResponse.insufficientFunds(fromWallet);

        double newFrom = fromWallet - amount;
        double newTo = getWallet(to) + amount;
        updateWalletCache(from, newFrom);
        updateWalletCache(to, newTo);
        db.setWallet(from, fromName, newFrom);
        db.setWallet(to, toName, newTo);
        db.logTransaction(from, to, fromName, toName, amount, "PAY", null);
        fireChangeEvent(from, fromWallet, newFrom, LocalBalanceChangeEvent.Reason.PAY_SEND);
        fireChangeEvent(to, newTo - amount, newTo, LocalBalanceChangeEvent.Reason.PAY_RECEIVE);
        return EconomyResponse.success(amount, newFrom);
    }

    // ── Death Penalty ─────────────────────────────────────────────────────────

    /**
     * Apply the general death penalty (fires on all deaths, ignores killer).
     * 
     * @return amount deducted, or 0 if disabled / threshold not met
     */
    public double applyDeathPenalty(UUID victimUuid, String victimName) {
        var cfg = plugin.getConfigManager();
        if (!cfg.isDeathPenaltyEnabled())
            return 0;
        return deductWallet(victimUuid, victimName, null, null,
                cfg.getDeathPenaltyMode(), cfg.getDeathPenaltyAmount(),
                cfg.getDeathPenaltyMinWallet(), false, "DEATH_PENALTY");
    }

    /**
     * Apply the PvP-specific penalty (fires only when killed by a player).
     * Transfers funds to killer if configured.
     * 
     * @return amount deducted, or 0 if disabled / threshold not met
     */
    public double applyPvpPenalty(UUID victimUuid, String victimName, UUID killerUuid, String killerName) {
        var cfg = plugin.getConfigManager();
        if (!cfg.isPvpPenaltyEnabled())
            return 0;
        return deductWallet(victimUuid, victimName, killerUuid, killerName,
                cfg.getPvpPenaltyMode(), cfg.getPvpPenaltyAmount(),
                cfg.getPvpPenaltyMinWallet(), cfg.isPvpPenaltyTransferToKiller(), "PVP_PENALTY");
    }

    /** Shared deduction logic for both penalty types. */
    private double deductWallet(UUID victimUuid, String victimName,
            UUID killerUuid, String killerName,
            String mode, double amount, double minWallet,
            boolean transferToKiller, String logType) {
        double wallet = getWallet(victimUuid);
        if (wallet < minWallet)
            return 0;

        double deduct = "fixed".equalsIgnoreCase(mode)
                ? amount
                : wallet * (amount / 100.0);
        deduct = Math.min(deduct, wallet);
        if (deduct <= 0)
            return 0;

        double newWallet = wallet - deduct;
        updateWalletCache(victimUuid, newWallet);
        db.setWallet(victimUuid, victimName, newWallet);
        db.logTransaction(victimUuid, killerUuid, victimName, killerName, deduct, logType, null);

        if (transferToKiller && killerUuid != null) {
            double killerNew = getWallet(killerUuid) + deduct;
            updateWalletCache(killerUuid, killerNew);
            db.setWallet(killerUuid, killerName, killerNew);
        }

        return deduct;
    }

    // ── Leaderboard ───────────────────────────────────────────────────────────

    public List<BalanceEntry> getTopBalances(int page) {
        int perPage = plugin.getConfigManager().getBaltopEntriesPerPage();
        return db.getTopBalances(perPage, (page - 1) * perPage);
    }

    public int getTotalPages() {
        int perPage = plugin.getConfigManager().getBaltopEntriesPerPage();
        int total = db.getTotalAccounts();
        return Math.max(1, (int) Math.ceil((double) total / perPage));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private EconomyResponse depositWallet(UUID uuid, String name, double amount,
            LocalBalanceChangeEvent.Reason reason) {
        double old = getWallet(uuid);
        double newBal = old + amount;
        updateWalletCache(uuid, newBal);
        db.setWallet(uuid, name, newBal);
        db.logTransaction(null, uuid, "SYSTEM", name, amount, reason.name(), null);
        fireChangeEvent(uuid, old, newBal, reason);
        return EconomyResponse.success(amount, newBal);
    }

    private EconomyResponse withdrawWallet(UUID uuid, String name, double amount,
            LocalBalanceChangeEvent.Reason reason) {
        double old = getWallet(uuid);
        double newBal = old - amount;
        updateWalletCache(uuid, newBal);
        db.setWallet(uuid, name, newBal);
        db.logTransaction(uuid, null, name, "SYSTEM", amount, reason.name(), null);
        fireChangeEvent(uuid, old, newBal, reason);
        return EconomyResponse.success(amount, newBal);
    }

    private void updateWalletCache(UUID uuid, double val) {
        cache.computeIfAbsent(uuid, k -> new double[] { 0, 0 })[0] = Math.max(0, val);
    }

    private void updateBothCache(UUID uuid, double wallet, double bank) {
        double[] arr = cache.computeIfAbsent(uuid, k -> new double[] { 0, 0 });
        arr[0] = Math.max(0, wallet);
        arr[1] = Math.max(0, bank);
    }

    private void fireChangeEvent(UUID uuid, double old, double newBal, LocalBalanceChangeEvent.Reason reason) {
        var player = Bukkit.getPlayer(uuid);
        Bukkit.getPluginManager().callEvent(new LocalBalanceChangeEvent(uuid, player, old, newBal, reason));
    }
}
