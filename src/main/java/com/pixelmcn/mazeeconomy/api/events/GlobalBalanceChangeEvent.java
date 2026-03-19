package com.pixelmcn.mazeeconomy.api.events;

import com.pixelmcn.mazeeconomy.model.GlobalCurrencyType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Fired whenever a player's global economy balance (Mazecoins or Shards) changes.
 * This includes changes detected via DB sync from other servers.
 */
public class GlobalBalanceChangeEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    public enum Reason {
        DEPOSIT,
        WITHDRAW,
        ADMIN_SET,
        SYNC,      // Balance updated from another server via DB poll
        OTHER
    }

    private final UUID playerUuid;
    private final @Nullable Player player;
    private final GlobalCurrencyType currency;
    private final double oldBalance;
    private final double newBalance;
    private final Reason reason;

    public GlobalBalanceChangeEvent(UUID playerUuid, @Nullable Player player,
                                    GlobalCurrencyType currency,
                                    double oldBalance, double newBalance, Reason reason) {
        this.playerUuid = playerUuid;
        this.player     = player;
        this.currency   = currency;
        this.oldBalance = oldBalance;
        this.newBalance = newBalance;
        this.reason     = reason;
    }

    /** UUID of the player whose balance changed. */
    public UUID getPlayerUuid() { return playerUuid; }

    /** Online player instance, or null if offline. */
    public @Nullable Player getPlayer() { return player; }

    /** Which global currency changed. */
    public GlobalCurrencyType getCurrency() { return currency; }

    /** Balance before the change. */
    public double getOldBalance() { return oldBalance; }

    /** Balance after the change. */
    public double getNewBalance() { return newBalance; }

    /** The amount that changed. */
    public double getDelta() { return newBalance - oldBalance; }

    /** What caused this balance change. */
    public Reason getReason() { return reason; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
