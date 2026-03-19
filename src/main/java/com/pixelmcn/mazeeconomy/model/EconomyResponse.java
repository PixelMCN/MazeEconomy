package com.pixelmcn.mazeeconomy.model;

/**
 * Result object returned by economy transaction methods.
 */
public class EconomyResponse {

    public enum Result {
        SUCCESS,
        FAILURE,
        INSUFFICIENT_FUNDS,
        PLAYER_NOT_FOUND,
        INVALID_AMOUNT
    }

    private final Result result;
    private final double amount;
    private final double newBalance;
    private final String errorMessage;

    private EconomyResponse(Result result, double amount, double newBalance, String errorMessage) {
        this.result = result;
        this.amount = amount;
        this.newBalance = newBalance;
        this.errorMessage = errorMessage;
    }

    public static EconomyResponse success(double amount, double newBalance) {
        return new EconomyResponse(Result.SUCCESS, amount, newBalance, null);
    }

    public static EconomyResponse failure(String message) {
        return new EconomyResponse(Result.FAILURE, 0, 0, message);
    }

    public static EconomyResponse insufficientFunds(double balance) {
        return new EconomyResponse(Result.INSUFFICIENT_FUNDS, 0, balance, "Insufficient funds");
    }

    public static EconomyResponse playerNotFound() {
        return new EconomyResponse(Result.PLAYER_NOT_FOUND, 0, 0, "Player not found");
    }

    public static EconomyResponse invalidAmount() {
        return new EconomyResponse(Result.INVALID_AMOUNT, 0, 0, "Invalid amount");
    }

    public boolean isSuccess() {
        return result == Result.SUCCESS;
    }

    public Result getResult() {
        return result;
    }

    public double getAmount() {
        return amount;
    }

    public double getNewBalance() {
        return newBalance;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
