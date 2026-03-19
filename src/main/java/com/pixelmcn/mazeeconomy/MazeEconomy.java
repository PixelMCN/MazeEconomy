package com.pixelmcn.mazeeconomy;

import com.pixelmcn.mazeeconomy.api.impl.MazeEconomyAPI;
import com.pixelmcn.mazeeconomy.command.admin.EcoCommand;
import com.pixelmcn.mazeeconomy.command.admin.MazeEconomyCommand;
import com.pixelmcn.mazeeconomy.command.bank.BankCommand;
import com.pixelmcn.mazeeconomy.command.global.MazeCoinsCommand;
import com.pixelmcn.mazeeconomy.command.global.ShardsCommand;
import com.pixelmcn.mazeeconomy.command.local.BalanceCommand;
import com.pixelmcn.mazeeconomy.command.local.BalanceTopCommand;
import com.pixelmcn.mazeeconomy.command.local.PayCommand;
import com.pixelmcn.mazeeconomy.config.ConfigManager;
import com.pixelmcn.mazeeconomy.config.GuiManager;
import com.pixelmcn.mazeeconomy.config.LanguageManager;
import com.pixelmcn.mazeeconomy.database.mariadb.MariaDBManager;
import com.pixelmcn.mazeeconomy.database.sqlite.SQLiteManager;
import com.pixelmcn.mazeeconomy.hook.MazeEconomyPlaceholders;
import com.pixelmcn.mazeeconomy.hook.VaultHook;
import com.pixelmcn.mazeeconomy.listener.DeathListener;
import com.pixelmcn.mazeeconomy.listener.PlayerListener;
import com.pixelmcn.mazeeconomy.manager.GlobalEconomyManager;
import com.pixelmcn.mazeeconomy.manager.LocalEconomyManager;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.logging.Level;

public final class MazeEconomy extends JavaPlugin {

    private static MazeEconomy instance;

    private ConfigManager configManager;
    private LanguageManager languageManager;
    private GuiManager guiManager;
    private MariaDBManager mariaDBManager;
    private SQLiteManager sqLiteManager;
    private LocalEconomyManager localEconomyManager;
    private GlobalEconomyManager globalEconomyManager;
    private VaultHook vaultHook;
    private MazeEconomyAPI api;

    private boolean globalEconomyActive = false;

    // ANSI (safe fallback if console doesn't support it)
    private static final String RESET = "\u001B[0m";
    private static final String GOLD = "\u001B[33m";
    private static final String GREEN = "\u001B[32m";
    private static final String RED = "\u001B[31m";
    private static final String GRAY = "\u001B[37m";
    private static final String DARK_GRAY = "\u001B[90m";

    @Override
    public void onEnable() {
        instance = this;

        logHeader("MazeEconomy starting");

        configManager = new ConfigManager(this);
        configManager.load();

        languageManager = new LanguageManager(this);
        languageManager.load();

        guiManager = new GuiManager(this);
        guiManager.load();

        // SQLite (required)
        try {
            sqLiteManager = new SQLiteManager(this);
            sqLiteManager.init();
            log(GREEN + "✔ SQLite initialized");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, RED + "SQLite initialization failed" + RESET, e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        localEconomyManager = new LocalEconomyManager(this, sqLiteManager);
        localEconomyManager.startInterestTask();

        // MariaDB (optional)
        if (configManager.isGlobalEconomyEnabled() && isMariaDBConfigured()) {
            try {
                mariaDBManager = new MariaDBManager(this);
                mariaDBManager.init();

                globalEconomyManager = new GlobalEconomyManager(this, mariaDBManager);
                globalEconomyManager.startSyncTask();

                globalEconomyActive = true;
                log(GREEN + "✔ Global economy connected (MariaDB)");
            } catch (Exception e) {
                warnBlock("Global economy disabled", e.getMessage());
                mariaDBManager = null;
                globalEconomyManager = null;
            }
        } else {
            log(DARK_GRAY + "Global economy not configured (local-only mode)");
        }

        api = new MazeEconomyAPI(this);

        // Vault
        vaultHook = new VaultHook(this, localEconomyManager);
        boolean vaultRegistered = vaultHook.register();

        // PlaceholderAPI
        boolean papiFound = getServer().getPluginManager().getPlugin("PlaceholderAPI") != null;
        if (papiFound) {
            new MazeEconomyPlaceholders(this).register();
        }

        registerCommands();

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new DeathListener(this), this);

        printBanner(vaultRegistered, papiFound);
    }

    @Override
    public void onDisable() {

        logHeader("MazeEconomy shutting down");

        if (localEconomyManager != null) {
            localEconomyManager.stopInterestTask();
            log(GREEN + "✔ Interest task stopped");
        }

        if (globalEconomyManager != null) {
            globalEconomyManager.flushAll();
            globalEconomyManager.stopSyncTask();
            log(GREEN + "✔ Global data flushed & sync stopped");
        }

        if (vaultHook != null) {
            vaultHook.unregister();
            log(GRAY + "• Vault unregistered");
        }

        if (mariaDBManager != null) {
            mariaDBManager.close();
            log(GRAY + "• MariaDB connection closed");
        }

        if (sqLiteManager != null) {
            sqLiteManager.close();
            log(GRAY + "• SQLite connection closed");
        }

        log(GREEN + "✔ All data saved successfully");
        logFooter();
    }

    private boolean isMariaDBConfigured() {
        return !configManager.getMariaDBPassword().equals("changeme");
    }

    private void registerCommands() {
        reg("balance", new BalanceCommand(this), true);
        reg("pay", new PayCommand(this), true);
        reg("balancetop", new BalanceTopCommand(this), false);
        reg("bank", new BankCommand(this), true);
        reg("eco", new EcoCommand(this), true);
        reg("mazeeconomy", new MazeEconomyCommand(this), true);

        if (globalEconomyActive) {
            reg("mazecoins", new MazeCoinsCommand(this), true);
            reg("shards", new ShardsCommand(this), true);
        }
    }

    private void reg(String name, Object handler, boolean tabComplete) {
        var cmd = Objects.requireNonNull(getCommand(name));
        if (handler instanceof CommandExecutor ce)
            cmd.setExecutor(ce);
        if (tabComplete && handler instanceof TabCompleter tc)
            cmd.setTabCompleter(tc);
    }

    // ─────────────────────────────
    // LOGGING
    // ─────────────────────────────

    private void printBanner(boolean vault, boolean papi) {
        var cfg = configManager;

        logHeader(GOLD + "MazeEconomy v" + getDescription().getVersion() + " by MazecraftMCN Team");

        log(GRAY + "Server       : " + cfg.getServerId());
        log(GRAY + "Currency     : " + cfg.getLocalCurrencyNamePlural());

        log(GRAY + "Vault        : " + (vault ? GREEN + "enabled" : RED + "not found"));
        log(GRAY + "Placeholder  : " + (papi ? GREEN + "enabled" : RED + "not found"));

        log(GRAY + "Bank         : " + (cfg.isBankEnabled()
                ? GREEN + "enabled (" + cfg.getBankInterestRate() + "%)"
                : RED + "disabled"));

        log(GRAY + "Global Econ  : " + (globalEconomyActive
                ? GREEN + "enabled (MariaDB)"
                : DARK_GRAY + "disabled"));

        logFooter();
    }

    private void log(String msg) {
        getLogger().info("  " + msg + RESET);
    }

    private void logHeader(String title) {
        getLogger().info(DARK_GRAY + "---------------------" + RESET);
        getLogger().info("  " + title + RESET);
        getLogger().info(DARK_GRAY + "---------------------" + RESET);
    }

    private void logFooter() {
        getLogger().info(DARK_GRAY + "---------------------" + RESET);
    }

    private void warnBlock(String title, String reason) {
        getLogger().warning(DARK_GRAY + "---------------------" + RESET);
        getLogger().warning("  " + RED + title + RESET);
        getLogger().warning("  " + GRAY + "reason: " + reason + RESET);
        getLogger().warning(DARK_GRAY + "---" + RESET);
    }

    // ─────────────────────────────
    // GETTERS
    // ─────────────────────────────

    public static MazeEconomy getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public GuiManager getGuiManager() {
        return guiManager;
    }

    public LocalEconomyManager getLocalEconomyManager() {
        return localEconomyManager;
    }

    public GlobalEconomyManager getGlobalEconomyManager() {
        return globalEconomyManager;
    }

    public MariaDBManager getMariaDBManager() {
        return mariaDBManager;
    }

    public SQLiteManager getSqLiteManager() {
        return sqLiteManager;
    }

    public VaultHook getVaultHook() {
        return vaultHook;
    }

    public MazeEconomyAPI getApi() {
        return api;
    }

    public boolean isGlobalEconomyActive() {
        return globalEconomyActive;
    }
}