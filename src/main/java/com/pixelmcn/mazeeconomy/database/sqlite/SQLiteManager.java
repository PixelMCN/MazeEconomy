package com.pixelmcn.mazeeconomy.database.sqlite;

import com.pixelmcn.mazeeconomy.MazeEconomy;
import com.pixelmcn.mazeeconomy.model.BalanceEntry;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class SQLiteManager {

    private final MazeEconomy plugin;
    private HikariDataSource dataSource;

    public SQLiteManager(MazeEconomy plugin) { this.plugin = plugin; }

    public void init() throws Exception {
        File dbFile = new File(plugin.getDataFolder(), "local_economy.db");
        plugin.getDataFolder().mkdirs();

        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.sqlite.JDBC");
        config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        config.setMaximumPoolSize(1);
        config.setConnectionTestQuery("SELECT 1");
        config.setPoolName("MazeEconomy-SQLite");

        dataSource = new HikariDataSource(config);
        createTables();
    }

    private void createTables() throws SQLException {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS local_balances (
                    uuid         TEXT    NOT NULL,
                    server_id    TEXT    NOT NULL,
                    player_name  TEXT    NOT NULL,
                    wallet       REAL    NOT NULL DEFAULT 0.0,
                    bank         REAL    NOT NULL DEFAULT 0.0,
                    last_updated INTEGER NOT NULL DEFAULT (strftime('%s','now')),
                    PRIMARY KEY (uuid, server_id)
                )
            """);
            // Migration: add wallet/bank columns if upgrading from old single-balance schema
            try { stmt.execute("ALTER TABLE local_balances ADD COLUMN wallet REAL NOT NULL DEFAULT 0.0"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE local_balances ADD COLUMN bank   REAL NOT NULL DEFAULT 0.0"); } catch (SQLException ignored) {}
            // If old 'balance' column exists, migrate it to wallet
            try {
                stmt.execute("UPDATE local_balances SET wallet = balance WHERE wallet = 0.0 AND balance > 0.0");
            } catch (SQLException ignored) {}

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS local_transactions (
                    id           INTEGER PRIMARY KEY AUTOINCREMENT,
                    server_id    TEXT    NOT NULL,
                    uuid_from    TEXT,
                    uuid_to      TEXT,
                    player_from  TEXT,
                    player_to    TEXT,
                    amount       REAL    NOT NULL,
                    type         TEXT    NOT NULL,
                    reason       TEXT,
                    timestamp    INTEGER NOT NULL DEFAULT (strftime('%s','now'))
                )
            """);
        }
    }

    public Connection getConnection() throws SQLException { return dataSource.getConnection(); }

    public void close() { if (dataSource != null && !dataSource.isClosed()) dataSource.close(); }

    // ── Account ───────────────────────────────────────────────────────────────

    public boolean hasAccount(UUID uuid) {
        String sql = "SELECT 1 FROM local_balances WHERE uuid = ? AND server_id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString()); ps.setString(2, serverId());
            return ps.executeQuery().next();
        } catch (SQLException e) { log("hasAccount", e); return false; }
    }

    public boolean createAccount(UUID uuid, String name) {
        String sql = "INSERT OR IGNORE INTO local_balances (uuid, server_id, player_name, wallet, bank) VALUES (?,?,?,?,?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString()); ps.setString(2, serverId()); ps.setString(3, name);
            ps.setDouble(4, plugin.getConfigManager().getLocalStartingWallet());
            ps.setDouble(5, plugin.getConfigManager().getLocalStartingBank());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { log("createAccount", e); return false; }
    }

    // ── Reads ─────────────────────────────────────────────────────────────────

    public double getWallet(UUID uuid) { return getColumn(uuid, "wallet"); }
    public double getBank(UUID uuid) { return getColumn(uuid, "bank"); }

    private double getColumn(UUID uuid, String col) {
        String sql = "SELECT " + col + " FROM local_balances WHERE uuid = ? AND server_id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString()); ps.setString(2, serverId());
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getDouble(1) : -1;
        } catch (SQLException e) { log("getColumn:" + col, e); return -1; }
    }

    // ── Writes ────────────────────────────────────────────────────────────────

    public boolean setWallet(UUID uuid, String name, double amount) { return setColumns(uuid, name, amount, -1); }
    public boolean setBank(UUID uuid, String name, double amount)   { return setColumns(uuid, name, -1, amount); }
    public boolean setBoth(UUID uuid, String name, double wallet, double bank) { return setColumns(uuid, name, wallet, bank); }

    /** Pass -1 for a column to leave it unchanged. */
    private boolean setColumns(UUID uuid, String name, double wallet, double bank) {
        if (wallet >= 0 && bank >= 0) {
            String sql = """
                INSERT INTO local_balances (uuid, server_id, player_name, wallet, bank, last_updated)
                VALUES (?,?,?,?,?,strftime('%s','now'))
                ON CONFLICT(uuid, server_id) DO UPDATE SET
                wallet=excluded.wallet, bank=excluded.bank,
                player_name=excluded.player_name, last_updated=excluded.last_updated
            """;
            try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, uuid.toString()); ps.setString(2, serverId()); ps.setString(3, name);
                ps.setDouble(4, Math.max(0, wallet)); ps.setDouble(5, Math.max(0, bank));
                return ps.executeUpdate() > 0;
            } catch (SQLException e) { log("setBoth", e); return false; }
        } else if (wallet >= 0) {
            return updateSingle(uuid, name, "wallet", wallet);
        } else {
            return updateSingle(uuid, name, "bank", bank);
        }
    }

    private boolean updateSingle(UUID uuid, String name, String col, double val) {
        String sql = "UPDATE local_balances SET " + col + "=?, player_name=?, last_updated=strftime('%s','now') WHERE uuid=? AND server_id=?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDouble(1, Math.max(0, val)); ps.setString(2, name);
            ps.setString(3, uuid.toString()); ps.setString(4, serverId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { log("updateSingle:" + col, e); return false; }
    }

    public boolean updatePlayerName(UUID uuid, String name) {
        String sql = "UPDATE local_balances SET player_name=? WHERE uuid=? AND server_id=?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name); ps.setString(2, uuid.toString()); ps.setString(3, serverId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { log("updatePlayerName", e); return false; }
    }

    // ── Leaderboard (by wallet + bank total) ──────────────────────────────────

    public List<BalanceEntry> getTopBalances(int limit, int offset) {
        String sql = "SELECT uuid, player_name, (wallet+bank) AS total FROM local_balances WHERE server_id=? ORDER BY total DESC LIMIT ? OFFSET ?";
        List<BalanceEntry> entries = new ArrayList<>();
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, serverId()); ps.setInt(2, limit); ps.setInt(3, offset);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) entries.add(new BalanceEntry(UUID.fromString(rs.getString("uuid")), rs.getString("player_name"), rs.getDouble("total")));
        } catch (SQLException e) { log("getTopBalances", e); }
        return entries;
    }

    public int getTotalAccounts() {
        String sql = "SELECT COUNT(*) FROM local_balances WHERE server_id=?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, serverId()); ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) { log("getTotalAccounts", e); return 0; }
    }

    // ── Transaction Log ───────────────────────────────────────────────────────

    public void logTransaction(UUID from, UUID to, String pFrom, String pTo, double amount, String type, String reason) {
        String sql = "INSERT INTO local_transactions (server_id,uuid_from,uuid_to,player_from,player_to,amount,type,reason) VALUES (?,?,?,?,?,?,?,?)";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, serverId());
            ps.setString(2, from != null ? from.toString() : null);
            ps.setString(3, to != null ? to.toString() : null);
            ps.setString(4, pFrom); ps.setString(5, pTo);
            ps.setDouble(6, amount); ps.setString(7, type); ps.setString(8, reason);
            ps.executeUpdate();
        } catch (SQLException e) { log("logTransaction", e); }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private String serverId() { return plugin.getConfigManager().getServerId(); }
    private void log(String method, Exception e) { plugin.getLogger().log(Level.SEVERE, "SQLite " + method + " error", e); }
}
