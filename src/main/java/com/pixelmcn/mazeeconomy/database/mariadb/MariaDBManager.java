package com.pixelmcn.mazeeconomy.database.mariadb;

import com.pixelmcn.mazeeconomy.MazeEconomy;
import com.pixelmcn.mazeeconomy.model.GlobalCurrencyType;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.UUID;
import java.util.logging.Level;

public class MariaDBManager {

    private final MazeEconomy plugin;
    private HikariDataSource dataSource;

    public MariaDBManager(MazeEconomy plugin) {
        this.plugin = plugin;
    }

    public void init() throws Exception {
        var cfg = plugin.getConfigManager();

        HikariConfig hikari = new HikariConfig();
        hikari.setDriverClassName("org.mariadb.jdbc.Driver");
        hikari.setJdbcUrl(String.format(
                "jdbc:mariadb://%s:%d/%s?useSSL=false&autoReconnect=true&characterEncoding=utf8",
                cfg.getMariaDBHost(), cfg.getMariaDBPort(), cfg.getMariaDBDatabase()));
        hikari.setUsername(cfg.getMariaDBUsername());
        hikari.setPassword(cfg.getMariaDBPassword());
        hikari.setMaximumPoolSize(cfg.getMariaDBMaxPoolSize());
        hikari.setMinimumIdle(cfg.getMariaDBMinIdle());
        hikari.setConnectionTimeout(cfg.getMariaDBConnectionTimeout());
        hikari.setIdleTimeout(cfg.getMariaDBIdleTimeout());
        hikari.setMaxLifetime(cfg.getMariaDBMaxLifetime());
        hikari.setPoolName("MazeEconomy-MariaDB");
        hikari.addDataSourceProperty("cachePrepStmts", "true");
        hikari.addDataSourceProperty("prepStmtCacheSize", "250");

        dataSource = new HikariDataSource(hikari);
        createTables();
    }

    private void createTables() throws SQLException {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {

            // Global balances — one row per player per currency
            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS global_balances (
                            uuid        VARCHAR(36)    NOT NULL,
                            currency    VARCHAR(32)    NOT NULL,
                            player_name VARCHAR(16)    NOT NULL,
                            balance     DECIMAL(20, 4) NOT NULL DEFAULT 0.0000,
                            last_updated BIGINT        NOT NULL DEFAULT (UNIX_TIMESTAMP()),
                            PRIMARY KEY (uuid, currency)
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """);

            // Global transaction logs
            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS global_transactions (
                            id          BIGINT        AUTO_INCREMENT PRIMARY KEY,
                            currency    VARCHAR(32)   NOT NULL,
                            uuid_from   VARCHAR(36),
                            uuid_to     VARCHAR(36),
                            player_from VARCHAR(16),
                            player_to   VARCHAR(16),
                            amount      DECIMAL(20,4) NOT NULL,
                            type        VARCHAR(32)   NOT NULL,
                            server_id   VARCHAR(64)   NOT NULL,
                            reason      VARCHAR(255),
                            timestamp   BIGINT        NOT NULL DEFAULT (UNIX_TIMESTAMP()),
                            INDEX idx_uuid_from (uuid_from),
                            INDEX idx_uuid_to   (uuid_to),
                            INDEX idx_currency  (currency),
                            INDEX idx_timestamp (timestamp)
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """);

            plugin.getLogger().info("MariaDB tables ready.");
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    // ── Balance Operations ────────────────────────────────────────────────────

    /**
     * Returns the stored balance, or -1 if no account exists.
     */
    public double getBalance(UUID uuid, GlobalCurrencyType currency) {
        String sql = "SELECT balance FROM global_balances WHERE uuid = ? AND currency = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, currency.getKey());
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return rs.getDouble("balance");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "MariaDB getBalance error", e);
        }
        return -1;
    }

    public java.util.List<java.util.Map.Entry<String, Double>> getTopBalances(GlobalCurrencyType currency, int limit) {
        java.util.List<java.util.Map.Entry<String, Double>> top = new java.util.ArrayList<>();
        String sql = "SELECT player_name, balance FROM global_balances WHERE currency = ? ORDER BY balance DESC LIMIT ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, currency.getKey());
            ps.setInt(2, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                top.add(java.util.Map.entry(rs.getString("player_name"), rs.getDouble("balance")));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "MariaDB getTopBalances error", e);
        }
        return top;
    }

    public boolean hasAccount(UUID uuid, GlobalCurrencyType currency) {
        String sql = "SELECT 1 FROM global_balances WHERE uuid = ? AND currency = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, currency.getKey());
            return ps.executeQuery().next();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "MariaDB hasAccount error", e);
        }
        return false;
    }

    public boolean createAccount(UUID uuid, String playerName, GlobalCurrencyType currency, double startingBalance) {
        String sql = """
                    INSERT IGNORE INTO global_balances (uuid, currency, player_name, balance)
                    VALUES (?, ?, ?, ?)
                """;
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, currency.getKey());
            ps.setString(3, playerName);
            ps.setDouble(4, startingBalance);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "MariaDB createAccount error", e);
        }
        return false;
    }

    public boolean setBalance(UUID uuid, String playerName, GlobalCurrencyType currency, double amount) {
        String sql = """
                    INSERT INTO global_balances (uuid, currency, player_name, balance, last_updated)
                    VALUES (?, ?, ?, ?, UNIX_TIMESTAMP())
                    ON DUPLICATE KEY UPDATE balance = VALUES(balance),
                    player_name = VALUES(player_name), last_updated = VALUES(last_updated)
                """;
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, currency.getKey());
            ps.setString(3, playerName);
            ps.setDouble(4, Math.max(0.0, amount));
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "MariaDB setBalance error", e);
        }
        return false;
    }

    public boolean updatePlayerName(UUID uuid, String playerName) {
        String sql = "UPDATE global_balances SET player_name = ? WHERE uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerName);
            ps.setString(2, uuid.toString());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "MariaDB updatePlayerName error", e);
        }
        return false;
    }

    // ── Transaction Logging ───────────────────────────────────────────────────

    public void logTransaction(GlobalCurrencyType currency, UUID from, UUID to,
            String playerFrom, String playerTo,
            double amount, String type, String reason) {
        String serverId = plugin.getConfigManager().getServerId();
        String sql = """
                    INSERT INTO global_transactions
                    (currency, uuid_from, uuid_to, player_from, player_to, amount, type, server_id, reason)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, currency.getKey());
            ps.setString(2, from != null ? from.toString() : null);
            ps.setString(3, to != null ? to.toString() : null);
            ps.setString(4, playerFrom);
            ps.setString(5, playerTo);
            ps.setDouble(6, amount);
            ps.setString(7, type);
            ps.setString(8, serverId);
            ps.setString(9, reason);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "MariaDB logTransaction error", e);
        }
    }
}
