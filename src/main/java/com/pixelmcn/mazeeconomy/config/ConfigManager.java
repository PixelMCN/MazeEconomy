package com.pixelmcn.mazeeconomy.config;

import com.pixelmcn.mazeeconomy.MazeEconomy;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {

    private final MazeEconomy plugin;
    private FileConfiguration config;

    public ConfigManager(MazeEconomy plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    // ── Server ────────────────────────────────────────────────────────────────
    public String getServerId() {
        return config.getString("server.id", "default");
    }

    public String getServerDisplayName() {
        return config.getString("server.display-name", "Server");
    }

    // ── Local Economy ─────────────────────────────────────────────────────────
    public boolean isLocalEconomyEnabled() {
        return config.getBoolean("local-economy.enabled", true);
    }

    public String getLocalCurrencyName() {
        return config.getString("local-economy.currency.name", "Coin");
    }

    public String getLocalCurrencyNamePlural() {
        return config.getString("local-economy.currency.name-plural", "Coins");
    }

    public String getLocalCurrencySymbol() {
        return config.getString("local-economy.currency.symbol", "$");
    }

    public double getLocalStartingWallet() {
        return config.getDouble("local-economy.currency.starting-wallet", 100.0);
    }

    public double getLocalStartingBank() {
        return config.getDouble("local-economy.currency.starting-bank", 0.0);
    }

    public int getLocalFormatDecimals() {
        return config.getInt("local-economy.currency.format-decimals", 2);
    }

    public int getBaltopEntriesPerPage() {
        return config.getInt("local-economy.leaderboard.entries-per-page", 10);
    }

    // ── Bank ──────────────────────────────────────────────────────────────────
    public boolean isBankEnabled() {
        return config.getBoolean("local-economy.bank.enabled", true);
    }

    public boolean isBankInterestEnabled() {
        return config.getBoolean("local-economy.bank.interest.enabled", true);
    }

    public double getBankInterestRate() {
        return config.getDouble("local-economy.bank.interest.rate", 0.5);
    }

    public String getBankInterestInterval() {
        return config.getString("local-economy.bank.interest.interval", "60m");
    }

    public double getBankWithdrawFee() {
        return config.getDouble("local-economy.bank.withdraw-fee", 0.0);
    }

    // ── Death Penalty ─────────────────────────────────────────────────────────
    public boolean isDeathPenaltyEnabled() {
        return config.getBoolean("local-economy.death-penalty.enabled", true);
    }

    public String getDeathPenaltyMode() {
        return config.getString("local-economy.death-penalty.mode", "percentage");
    }

    public double getDeathPenaltyAmount() {
        return config.getDouble("local-economy.death-penalty.amount", 10.0);
    }

    public double getDeathPenaltyMinWallet() {
        return config.getDouble("local-economy.death-penalty.minimum-wallet", 1000.0);
    }

    public boolean isDeathPenaltyAnnounce() {
        return config.getBoolean("local-economy.death-penalty.announce", true);
    }

    // ── PvP Penalty ───────────────────────────────────────────────────────────
    public boolean isPvpPenaltyEnabled() {
        return config.getBoolean("local-economy.pvp-penalty.enabled", true);
    }

    public String getPvpPenaltyMode() {
        return config.getString("local-economy.pvp-penalty.mode", "percentage");
    }

    public double getPvpPenaltyAmount() {
        return config.getDouble("local-economy.pvp-penalty.amount", 5.0);
    }

    public double getPvpPenaltyMinWallet() {
        return config.getDouble("local-economy.pvp-penalty.minimum-wallet", 500.0);
    }

    public boolean isPvpPenaltyTransferToKiller() {
        return config.getBoolean("local-economy.pvp-penalty.transfer-to-killer", true);
    }

    public boolean isPvpPenaltyAnnounce() {
        return config.getBoolean("local-economy.pvp-penalty.announce", true);
    }

    // ── Global Economy ────────────────────────────────────────────────────────
    public boolean isGlobalEconomyEnabled() {
        return config.getBoolean("global-economy.enabled", true);
    }

    public long getGlobalSyncIntervalTicks() {
        return config.getLong("global-economy.sync-interval-ticks", 100L);
    }

    public String getMazecoinName() {
        return config.getString("global-economy.currencies.mazecoins.name", "Mazecoin");
    }

    public String getMazecoinNamePlural() {
        return config.getString("global-economy.currencies.mazecoins.name-plural", "Mazecoins");
    }

    public String getMazecoinSymbol() {
        return config.getString("global-economy.currencies.mazecoins.symbol", "⬡");
    }

    public double getMazecoinStartingBalance() {
        return config.getDouble("global-economy.currencies.mazecoins.starting-balance", 0.0);
    }

    public int getMazecoinFormatDecimals() {
        return config.getInt("global-economy.currencies.mazecoins.format-decimals", 0);
    }

    public String getShardName() {
        return config.getString("global-economy.currencies.shards.name", "Shard");
    }

    public String getShardNamePlural() {
        return config.getString("global-economy.currencies.shards.name-plural", "Shards");
    }

    public String getShardSymbol() {
        return config.getString("global-economy.currencies.shards.symbol", "◈");
    }

    public double getShardStartingBalance() {
        return config.getDouble("global-economy.currencies.shards.starting-balance", 0.0);
    }

    public int getShardFormatDecimals() {
        return config.getInt("global-economy.currencies.shards.format-decimals", 0);
    }

    // ── MariaDB ───────────────────────────────────────────────────────────────
    public String getMariaDBHost() {
        return config.getString("database.mariadb.host", "localhost");
    }

    public int getMariaDBPort() {
        return config.getInt("database.mariadb.port", 3306);
    }

    public String getMariaDBDatabase() {
        return config.getString("database.mariadb.database", "mazeeconomy");
    }

    public String getMariaDBUsername() {
        return config.getString("database.mariadb.username", "root");
    }

    public String getMariaDBPassword() {
        return config.getString("database.mariadb.password", "");
    }

    public int getMariaDBMaxPoolSize() {
        return config.getInt("database.mariadb.pool.maximum-pool-size", 10);
    }

    public int getMariaDBMinIdle() {
        return config.getInt("database.mariadb.pool.minimum-idle", 2);
    }

    public long getMariaDBConnectionTimeout() {
        return config.getLong("database.mariadb.pool.connection-timeout", 30000L);
    }

    public long getMariaDBIdleTimeout() {
        return config.getLong("database.mariadb.pool.idle-timeout", 600000L);
    }

    public long getMariaDBMaxLifetime() {
        return config.getLong("database.mariadb.pool.max-lifetime", 1800000L);
    }

    // ── Messages ──────────────────────────────────────────────────────────────
    public String getPrefix() {
        return plugin.getLanguageManager().getPrefix();
    }

    public String getMessage(String path) {
        return plugin.getLanguageManager().getMessage(path);
    }
}
