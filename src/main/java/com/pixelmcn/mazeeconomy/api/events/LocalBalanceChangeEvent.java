package com.pixelmcn.mazeeconomy.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Fired whenever a player's local economy balance changes.
 * Listen to this event to react to deposits, withdrawals, payments, and admin edits.
 */
public class LocalBalanceChangeEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    public enum Reason {
        DEPOSIT,
        WITHDRAW,
        PAY_SEND,
        PAY_RECEIVE,
        ADMIN_SET,
        OTHER
    }

    private final UUID playerUuid;
    private final @Nullable Player player;
    private final double oldBalance;
    private final double newBalance;
    private final Reason reason;

    public LocalBalanceChangeEvent(UUID playerUuid, @Nullable Player player,
                                   double oldBalance, double newBalance, Reason reason) {
        this.playerUuid = playerUuid;
        this.player     = player;
        this.oldBalance = oldBalance;
        this.newBalance = newBalance;
        this.reason     = reason;
    }

    /** UUID of the player whose balance changed. */
    public UUID getPlayerUuid() { return playerUuid; }

    /** Online player instance, or null if offline. */
    public @Nullable Player getPlayer() { return player; }

    /** Balance before the change. */
    public double getOldBalance() { return oldBalance; }

    /** Balance after the change. */
    public double getNewBalance() { return newBalance; }

    /** The amount that changed (can be negative). */
    public double getDelta() { return newBalance - oldBalance; }

    /** What caused this balance change. */
    public Reason getReason() { return reason; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
