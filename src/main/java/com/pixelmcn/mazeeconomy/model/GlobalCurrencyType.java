package com.pixelmcn.mazeeconomy.model;

/**
 * Represents the two globally-synced currencies in MazeEconomy.
 */
public enum GlobalCurrencyType {

    MAZECOINS("mazecoins"),
    SHARDS("shards");

    private final String key;

    GlobalCurrencyType(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    /**
     * Parse from a string (case-insensitive). Returns null if not found.
     */
    public static GlobalCurrencyType fromString(String input) {
        if (input == null) return null;
        return switch (input.toLowerCase()) {
            case "mazecoins", "mc", "coins", "mazecoin" -> MAZECOINS;
            case "shards", "shard", "s" -> SHARDS;
            default -> null;
        };
    }
}
