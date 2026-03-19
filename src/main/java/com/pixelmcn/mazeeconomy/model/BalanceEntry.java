package com.pixelmcn.mazeeconomy.model;

import java.util.UUID;

/**
 * Represents a leaderboard entry for /baltop.
 */
public record BalanceEntry(UUID uuid, String playerName, double balance) {}
