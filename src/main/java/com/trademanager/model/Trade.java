package com.trademanager.model;

import java.time.LocalDateTime;

/**
 * Represents a financial trade order in the system.
 * Supports equity, fixed income, and FX instrument types.
 */
public class Trade {

    public enum InstrumentType { EQUITY, FIXED_INCOME, FX, DERIVATIVE }
    public enum OrderSide { BUY, SELL }
    public enum TradeStatus { PENDING, EXECUTED, CANCELLED, REJECTED }

    private final String tradeId;
    private final String ticker;
    private final InstrumentType instrumentType;
    private final OrderSide side;
    private double quantity;
    private double price;
    private TradeStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Trade(String tradeId, String ticker, InstrumentType instrumentType,
                 OrderSide side, double quantity, double price) {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
        if (price <= 0) throw new IllegalArgumentException("Price must be positive");
        if (tradeId == null || tradeId.isBlank()) throw new IllegalArgumentException("Trade ID cannot be null/empty");

        this.tradeId = tradeId;
        this.ticker = ticker;
        this.instrumentType = instrumentType;
        this.side = side;
        this.quantity = quantity;
        this.price = price;
        this.status = TradeStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public double getNotionalValue() {
        return quantity * price;
    }

    public void updateStatus(TradeStatus newStatus) {
        this.status = newStatus;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters
    public String getTradeId()             { return tradeId; }
    public String getTicker()              { return ticker; }
    public InstrumentType getInstrumentType() { return instrumentType; }
    public OrderSide getSide()             { return side; }
    public double getQuantity()            { return quantity; }
    public double getPrice()               { return price; }
    public TradeStatus getStatus()         { return status; }
    public LocalDateTime getCreatedAt()    { return createdAt; }
    public LocalDateTime getUpdatedAt()    { return updatedAt; }

    @Override
    public String toString() {
        return String.format("Trade[%s | %s | %s | %s | qty=%.0f | price=%.2f | notional=%.2f | status=%s]",
                tradeId, ticker, instrumentType, side, quantity, price, getNotionalValue(), status);
    }
}
