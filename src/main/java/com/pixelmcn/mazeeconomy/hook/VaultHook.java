package com.pixelmcn.mazeeconomy.hook;

import com.pixelmcn.mazeeconomy.MazeEconomy;
import com.pixelmcn.mazeeconomy.manager.LocalEconomyManager;
import com.pixelmcn.mazeeconomy.util.FormatUtil;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.ServicePriority;

import java.util.List;

/**
 * Registers MazeEconomy's local economy as the Vault Economy provider.
 * Other plugins (shops, jobs, etc.) will automatically use this.
 */
public class VaultHook implements Economy {

    private final MazeEconomy plugin;
    private final LocalEconomyManager localManager;
    private boolean registered = false;

    public VaultHook(MazeEconomy plugin, LocalEconomyManager localManager) {
        this.plugin       = plugin;
        this.localManager = localManager;
    }

    public boolean register() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) return false;
        plugin.getServer().getServicesManager().register(Economy.class, this, plugin, ServicePriority.Normal);
        registered = true;
        return true;
    }

    public void unregister() {
        if (registered) {
            plugin.getServer().getServicesManager().unregister(Economy.class, this);
        }
    }

    // ── Economy Interface ─────────────────────────────────────────────────────

    @Override
    public boolean isEnabled() { return plugin.isEnabled(); }

    @Override
    public String getName() { return "MazeEconomy"; }

    @Override
    public boolean hasBankSupport() { return false; }

    @Override
    public int fractionalDigits() {
        return plugin.getConfigManager().getLocalFormatDecimals();
    }

    @Override
    public String format(double amount) {
        return FormatUtil.formatLocal(plugin, amount);
    }

    @Override
    public String currencyNamePlural() {
        return plugin.getConfigManager().getLocalCurrencyNamePlural();
    }

    @Override
    public String currencyNameSingular() {
        return plugin.getConfigManager().getLocalCurrencyName();
    }

    // ── Account Checks ────────────────────────────────────────────────────────

    @Override
    @Deprecated
    public boolean hasAccount(String playerName) { return false; }

    @Override
    public boolean hasAccount(OfflinePlayer player) {
        return localManager.hasAccount(player.getUniqueId());
    }

    @Override
    @Deprecated
    public boolean hasAccount(String playerName, String worldName) { return hasAccount(playerName); }

    @Override
    public boolean hasAccount(OfflinePlayer player, String worldName) { return hasAccount(player); }

    // ── Account Creation ──────────────────────────────────────────────────────

    @Override
    @Deprecated
    public boolean createPlayerAccount(String playerName) { return false; }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        return localManager.createAccount(player.getUniqueId(),
                player.getName() != null ? player.getName() : player.getUniqueId().toString());
    }

    @Override
    @Deprecated
    public boolean createPlayerAccount(String playerName, String worldName) { return createPlayerAccount(playerName); }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String worldName) { return createPlayerAccount(player); }

    // ── Balance ───────────────────────────────────────────────────────────────

    @Override
    @Deprecated
    public double getBalance(String playerName) { return 0; }

    @Override
    public double getBalance(OfflinePlayer player) {
        return localManager.getWallet(player.getUniqueId());
    }

    @Override
    @Deprecated
    public double getBalance(String playerName, String world) { return getBalance(playerName); }

    @Override
    public double getBalance(OfflinePlayer player, String world) { return getBalance(player); }

    // ── Has ───────────────────────────────────────────────────────────────────

    @Override
    @Deprecated
    public boolean has(String playerName, double amount) { return false; }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return localManager.has(player.getUniqueId(), amount);
    }

    @Override
    @Deprecated
    public boolean has(String playerName, String worldName, double amount) { return has(playerName, amount); }

    @Override
    public boolean has(OfflinePlayer player, String worldName, double amount) { return has(player, amount); }

    // ── Withdraw ──────────────────────────────────────────────────────────────

    @Override
    @Deprecated
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Use OfflinePlayer overload");
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        String name = player.getName() != null ? player.getName() : player.getUniqueId().toString();
        var resp = localManager.withdraw(player.getUniqueId(), name, amount);
        return toVaultResponse(resp, amount);
    }

    @Override
    @Deprecated
    public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
        return withdrawPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
        return withdrawPlayer(player, amount);
    }

    // ── Deposit ───────────────────────────────────────────────────────────────

    @Override
    @Deprecated
    public EconomyResponse depositPlayer(String playerName, double amount) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Use OfflinePlayer overload");
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        String name = player.getName() != null ? player.getName() : player.getUniqueId().toString();
        var resp = localManager.deposit(player.getUniqueId(), name, amount);
        return toVaultResponse(resp, amount);
    }

    @Override
    @Deprecated
    public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
        return depositPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
        return depositPlayer(player, amount);
    }

    // ── Banks (not supported) ─────────────────────────────────────────────────

    @Override
    public EconomyResponse createBank(String name, String player) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    public EconomyResponse createBank(String name, OfflinePlayer player) {
        return createBank(name, "");
    }

    @Override
    public EconomyResponse deleteBank(String name) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    public EconomyResponse bankBalance(String name) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    public EconomyResponse bankHas(String name, double amount) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    public EconomyResponse isBankOwner(String name, String playerName) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    public EconomyResponse isBankOwner(String name, OfflinePlayer player) {
        return isBankOwner(name, "");
    }

    @Override
    public EconomyResponse isBankMember(String name, String playerName) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    public EconomyResponse isBankMember(String name, OfflinePlayer player) {
        return isBankMember(name, "");
    }

    @Override
    public List<String> getBanks() { return List.of(); }

    // ── Helper ────────────────────────────────────────────────────────────────

    private EconomyResponse toVaultResponse(com.pixelmcn.mazeeconomy.model.EconomyResponse resp, double amount) {
        if (resp.isSuccess()) {
            return new EconomyResponse(amount, resp.getNewBalance(), EconomyResponse.ResponseType.SUCCESS, null);
        }
        return switch (resp.getResult()) {
            case INSUFFICIENT_FUNDS -> new EconomyResponse(0, resp.getNewBalance(),
                    EconomyResponse.ResponseType.FAILURE, "Insufficient funds");
            case INVALID_AMOUNT     -> new EconomyResponse(0, 0,
                    EconomyResponse.ResponseType.FAILURE, "Invalid amount");
            default                 -> new EconomyResponse(0, 0,
                    EconomyResponse.ResponseType.FAILURE, resp.getErrorMessage());
        };
    }
}
