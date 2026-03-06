package com.trademanager.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents an investment portfolio holding multiple positions.
 * Tracks executed trades and calculates aggregate metrics.
 */
public class Portfolio {

    private final String portfolioId;
    private final String ownerName;
    private double cashBalance;
    private final List<Trade> executedTrades;

    public Portfolio(String portfolioId, String ownerName, double initialCash) {
        if (initialCash < 0) throw new IllegalArgumentException("Initial cash cannot be negative");
        this.portfolioId = portfolioId;
        this.ownerName = ownerName;
        this.cashBalance = initialCash;
        this.executedTrades = new ArrayList<>();
    }

    /**
     * Applies an executed trade to the portfolio, adjusting cash balance.
     */
    public void applyTrade(Trade trade) {
        if (trade.getStatus() != Trade.TradeStatus.EXECUTED) {
            throw new IllegalStateException("Only executed trades can be applied to portfolio");
        }

        double notional = trade.getNotionalValue();
        if (trade.getSide() == Trade.OrderSide.BUY) {
            if (cashBalance < notional) {
                throw new IllegalStateException("Insufficient cash: required " + notional + ", available " + cashBalance);
            }
            cashBalance -= notional;
        } else {
            cashBalance += notional;
        }
        executedTrades.add(trade);
    }

    /**
     * Returns total notional value of all executed BUY trades.
     */
    public double getTotalInvestedValue() {
        return executedTrades.stream()
                .filter(t -> t.getSide() == Trade.OrderSide.BUY)
                .mapToDouble(Trade::getNotionalValue)
                .sum();
    }

    public int getTradeCount() { return executedTrades.size(); }
    public double getCashBalance() { return cashBalance; }
    public String getPortfolioId() { return portfolioId; }
    public String getOwnerName() { return ownerName; }
    public List<Trade> getExecutedTrades() { return Collections.unmodifiableList(executedTrades); }

    @Override
    public String toString() {
        return String.format("Portfolio[%s | owner=%s | cash=%.2f | trades=%d | invested=%.2f]",
                portfolioId, ownerName, cashBalance, executedTrades.size(), getTotalInvestedValue());
    }
}
