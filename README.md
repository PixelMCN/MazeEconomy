# MazeEconomy

MazeEconomy is a comprehensive, multi-currency, cross-server economy engine designed for Minecraft 1.21.x running on Paper or Purpur. It utilizes a hybrid database architecture (MariaDB and SQLite) to distinguish local server currencies from globally synchronized currencies.

---

## Features

- **Dual Economy Architecture:** Supports both isolated local economies (per server) and global economies synced across an entire network.
- **Global Currencies (Mazecoins & Shards):** Network-wide currencies seamlessly synchronized using MariaDB.
- **Local Currency (Vault Compatible):** Each server maintains its own spendable and bank balance via SQLite. Fully compatible with plugins utilizing the Vault Economy API.
- **Cross-Server Synchronization:** Global balances are polled from the MariaDB database at configurable intervals, ensuring consistent state across instances.
- **Transaction Auditing:** Comprehensive logging of all economic changes to both local and global databases.
- **Robust Developer API:** Provides robust Bukkit event firing and API methods for third-party integrations.
- **Localization and Customization:** Fully supports MiniMessage text formatting, dynamic Language Manager mappings via `lang/en_US.yml`, and flexible GUI composition via `gui.yml`.
- **Bank Interest Intervals:** Built-in abstraction for interval calculation supporting complex formulas like `60s`, `1d`, or `1M`.
- **Database Pooling:** Utilizes HikariCP for high-performance connection pooling.

---

## Requirements

| Requirement | Supported Version |
|-------------|-------------------|
| Java Environment | Java 21 or higher |
| Server Software | Paper or Purpur (1.21.x) |
| Database Storage | MariaDB (10.6+) is mandatory for the Global Economy feature |
| Optional Dependency | Vault (Required only if integrating Local Economy with older plugins) |

---

## Installation Guide

1. Download the latest compiled `MazeEconomy.jar` from the project repository.
2. Place the jar file inside your server's `plugins/` directory.
3. Start the server once to generate the default configuration files.
4. Stop the server and open `plugins/MazeEconomy/config.yml`.
5. Configure your database settings. For multi-server setups, guarantee that the `server.id` is strictly unique per instance.
6. Restart the server to initialize the databases and connection pools.

---

## Network Topology Configuration

For proxies like BungeeCord or Velocity managing multiple servers:

1. **Shared MariaDB:** Every server must point to the identical MariaDB credentials to share `global_balances` and `global_transactions` tables.
2. **Unique Identifiers:** The `server.id` property inside `config.yml` must explicitly differentiate each server (e.g., `survival-1`, `skyblock-main`).
3. **Local SQLite:** The plugin automatically constructs isolated `local_economy.db` files inside each server's plugin directory. Do not attempt to share these SQLite files.

---

## Command Reference

### Player Commands

| Command | Aliases | Required Permission | Description |
|---------|---------|---------------------|-------------|
| `/balance [player]` | `/bal` | `mazeeconomy.balance` | Inspect local wallet and bank balances |
| `/pay <player> <amount>` | None | `mazeeconomy.pay` | Transfer local currency to another player |
| `/balancetop [page]` | `/baltop` | `mazeeconomy.balancetop` | Display the local economy leaderboard |
| `/mazecoins [player]` | `/mc`, `/coins` | `mazeeconomy.global.mazecoins` | Inspect global Mazecoin balance |
| `/shards [player]` | None | `mazeeconomy.global.shards` | Inspect global Shard balance |

### Administrative Commands

| Command | Required Permission | Description |
|---------|---------------------|-------------|
| `/eco set <local|mazecoins|shards> <player> <amount>` | `mazeeconomy.admin.eco` | Explicitly define a player's balance |
| `/eco add <local|mazecoins|shards> <player> <amount>` | `mazeeconomy.admin.eco` | Inject currency into a player's balance |
| `/eco remove <local|mazecoins|shards> <player> <amount>` | `mazeeconomy.admin.eco` | Deduct currency from a player's balance |
| `/mazeeconomy reload` | `mazeeconomy.admin.manage` | Refresh configuration files safely |
| `/mazeeconomy info` | None | Print internal plugin telemetry |

---

## Vault Integration Overview

Upon server initiation, MazeEconomy automatically intercepts the Vault API hook. Any standard plugin requesting Vault economy access will inherently interface with the **Local Economy** wallet. The global currencies (Mazecoins and Shards) bypass Vault completely to guarantee network integrity, and must be invoked through the internal MazeEconomy Developer API.

---

## Developer API

MazeEconomy ships with an extensive developer toolkit. Please refer to `API_DOCS.md` located in the root directory for an exhaustive overview of the public methods, Bukkit events, and programmatic examples.

---

## Compilation Instructions

To build the plugin natively from the repository:

```bash
git clone https://github.com/PixelMCN/MazeEconomy.git
cd MazeEconomy
./gradlew build
```

The compiled artifact will be generated under `build/libs/`.

---

## License

This project is licensed under the MIT License. See `LICENSE` for further details.
