package com.trademanager.service;

import com.trademanager.engine.ComplianceEngine;
import com.trademanager.model.Portfolio;
import com.trademanager.model.Trade;
import com.trademanager.repository.TradeRepository;

import java.sql.SQLException;
import java.util.List;

/**
 * TradeOrderService: Core business logic layer.
 * Orchestrates compliance checking, trade execution, and persistence.
 * Follows the Facade design pattern to simplify client interactions.
 */
public class TradeOrderService {

    private final TradeRepository repository;
    private final ComplianceEngine complianceEngine;

    public TradeOrderService(TradeRepository repository, ComplianceEngine complianceEngine) {
        this.repository = repository;
        this.complianceEngine = complianceEngine;
    }

    public static class OrderResult {
        public final boolean success;
        public final String message;
        public final Trade trade;

        public OrderResult(boolean success, String message, Trade trade) {
            this.success = success;
            this.message = message;
            this.trade = trade;
        }
    }

    /**
     * Submits a trade order: runs compliance checks, executes if valid, persists result.
     */
    public OrderResult submitOrder(Trade trade, Portfolio portfolio) throws SQLException {
        // 1. Compliance check
        ComplianceEngine.ComplianceResult compliance = complianceEngine.evaluate(trade);
        if (!compliance.passed) {
            trade.updateStatus(Trade.TradeStatus.REJECTED);
            repository.save(trade);
            String violations = String.join("; ", compliance.violations);
            return new OrderResult(false, "Trade rejected — compliance violations: " + violations, trade);
        }

        // 2. Execute trade
        try {
            trade.updateStatus(Trade.TradeStatus.EXECUTED);
            portfolio.applyTrade(trade);
            repository.save(trade);
            return new OrderResult(true, "Trade executed successfully: " + trade.getTradeId(), trade);
        } catch (IllegalStateException e) {
            trade.updateStatus(Trade.TradeStatus.REJECTED);
            repository.save(trade);
            return new OrderResult(false, "Trade rejected — " + e.getMessage(), trade);
        }
    }

    public List<Trade> getPendingTrades() throws SQLException {
        return repository.findByStatus(Trade.TradeStatus.PENDING);
    }

    public List<Trade> getTradesByTicker(String ticker) throws SQLException {
        return repository.findByTicker(ticker);
    }

    public double getExecutedNotional(Trade.InstrumentType type) throws SQLException {
        return repository.getTotalNotionalByInstrument(type);
    }
}
