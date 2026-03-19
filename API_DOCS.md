# MazeEconomy Application Programming Interface (API) Documentation

**Plugin Identifier:** MazeEconomy  
**Package Route:** `com.pixelmcn.mazeeconomy`  
**Target Architecture:** Minecraft 1.21.x (Paper / Purpur running Java 21)

---

## Overview

MazeEconomy is structurally divided into two discrete operating models:
1. **Local Economy:** Strictly isolated per server using embedded SQLite. Accessible programmatically via both the native API and the Vault interface.
2. **Global Economy:** Spans seamlessly across the entire network cluster using MariaDB. Intentionally bypasses Vault to retain cross-server logic.

---

## API Initialization

It is recommended to establish a soft dependency within your `plugin.yml`:

```yaml
softdepend:
  - MazeEconomy
```

You can then hook the API natively during the `onEnable` phase:

```java
import com.pixelmcn.mazeeconomy.MazeEconomy;
import com.pixelmcn.mazeeconomy.api.impl.MazeEconomyAPI;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class MyPlugin extends JavaPlugin {
    private MazeEconomyAPI mazeApi;

    @Override
    public void onEnable() {
        Plugin plugin = getServer().getPluginManager().getPlugin("MazeEconomy");
        if (plugin instanceof MazeEconomy instance) {
            mazeApi = instance.getApi();
        }
    }
}
```

---

## Functional Methods

### Global Economy Methods

Global transactions accept offline players and automatically queue updates to the central MariaDB cluster.

**Retrieve Balances:**
```java
// Retrieve Mazecoin total
double coins = mazeApi.getMazecoins(player);

// Retrieve Shard total
double shards = mazeApi.getShards(player);

// Generic enumeration lookup
double balance = mazeApi.getGlobalBalance(player, GlobalCurrencyType.MAZECOINS);
```

**Validate Solvency:**
```java
boolean isSolvent = mazeApi.hasGlobal(player, GlobalCurrencyType.SHARDS, 500.0);
```

**Mutate Balances:**
```java
// Injection
EconomyResponse depositResponse = mazeApi.depositGlobal(player, GlobalCurrencyType.MAZECOINS, 100.0);

// Deduction
EconomyResponse withdrawResponse = mazeApi.withdrawGlobal(player, GlobalCurrencyType.SHARDS, 50.0);
if (!withdrawResponse.isSuccess()) {
    // Escalate INSUFFICIENT_FUNDS constraint issues
}
```

---

### Local Economy Methods

While Vault is the predominantly recommended strategy for standard transactions, the native API allows granular manipulation of the Local Economy.

```java
double currentBalance = mazeApi.getLocalBalance(player);
boolean solvent = mazeApi.hasLocal(player, 250.0);
EconomyResponse credit = mazeApi.depositLocal(player, 100.0);
EconomyResponse debit = mazeApi.withdrawLocal(player, 50.0);
EconomyResponse override = mazeApi.setLocalBalance(player, 500.0);
```

---

## Event Subsystem

MazeEconomy constructs custom asynchronous and synchronous Bukkit events during transactions, enabling deep integration with analytics plugins and scoreboard systems.

### LocalBalanceChangeEvent

Dispatched when a player's local SQLite balance undergoes modification.

```java
import com.pixelmcn.mazeeconomy.api.events.LocalBalanceChangeEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class LocalEconomyListener implements Listener {
    @EventHandler
    public void onLocalBalanceChange(LocalBalanceChangeEvent event) {
        double priorBalance = event.getOldBalance();
        double updatedBalance = event.getNewBalance();
        double differential = event.getDelta();

        // Analyze motivation via Reason enum
        LocalBalanceChangeEvent.Reason motivation = event.getReason(); 
        // Example constraints: DEPOSIT, WITHDRAW, PAY_SEND, PAY_RECEIVE, ADMIN_SET
    }
}
```

### GlobalBalanceChangeEvent

Dispatched when network-level polling or manual mutations affect a player's global balance schema. Event dispatch includes cross-server differential yields.

```java
import com.pixelmcn.mazeeconomy.api.events.GlobalBalanceChangeEvent;
import com.pixelmcn.mazeeconomy.model.GlobalCurrencyType;

public class GlobalEconomyListener implements Listener {
    @EventHandler
    public void onGlobalBalanceChange(GlobalBalanceChangeEvent event) {
        GlobalCurrencyType type = event.getCurrency();
        double differential = event.getDelta();

        // The SYNC reason designates inbound balance changes propagating from other servers
        if (event.getReason() == GlobalBalanceChangeEvent.Reason.SYNC) {
            // Safe to operate Bukkit abstractions immediately upon thread return
        }
    }
}
```

---

## Response Objects

Every mutating method strictly returns an `EconomyResponse` detailing the resolution logic.

```java
EconomyResponse response = mazeApi.withdrawGlobal(player, GlobalCurrencyType.MAZECOINS, 100.0);

if (response.isSuccess()) {
    System.out.println("New total: " + response.getNewBalance());
} else {
    // Analyze constraints such as INVALID_AMOUNT, INSUFFICIENT_FUNDS, or PLAYER_NOT_FOUND
    System.out.println("Failure reason: " + response.getErrorMessage());
}
```

---

## Enumerations (`GlobalCurrencyType`)

Inputs resolving global economies scale strictly off enumeration logic:

```java
import com.pixelmcn.mazeeconomy.model.GlobalCurrencyType;

GlobalCurrencyType typeA = GlobalCurrencyType.MAZECOINS;
GlobalCurrencyType typeB = GlobalCurrencyType.SHARDS;

// Programmatic alias conversions
GlobalCurrencyType lookup = GlobalCurrencyType.fromString("mc"); 
```

---

## Maven & Gradle Dependency Structuring

Currently, the compiled artifact requires localized dependency resolution rather than repository polling. 

**Gradle (Kotlin DSL configuration):**
```kotlin
dependencies {
    compileOnly(files("libs/MazeEconomy-1.0.0.jar"))
}
```

**Maven (System scoped):**
```xml
<dependency>
    <groupId>com.pixelmcn</groupId>
    <artifactId>MazeEconomy</artifactId>
    <version>1.0.0</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/libs/MazeEconomy-1.0.0.jar</systemPath>
</dependency>
```
