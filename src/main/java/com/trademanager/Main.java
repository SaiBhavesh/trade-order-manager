package com.trademanager;

import com.trademanager.engine.ComplianceEngine;
import com.trademanager.model.Portfolio;
import com.trademanager.model.Trade;
import com.trademanager.repository.TradeRepository;
import com.trademanager.service.TradeOrderService;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * Entry point for the Trade Order Manager application.
 * Demonstrates end-to-end trade lifecycle: submit → comply → execute → persist.
 */
public class Main {

    public static void main(String[] args) throws Exception {

        // --- Setup in-memory H2 database ---
        Connection conn = DriverManager.getConnection("jdbc:h2:mem:tradedb;DB_CLOSE_DELAY=-1", "sa", "");

        // --- Wire dependencies ---
        TradeRepository repository = new TradeRepository(conn);

        ComplianceEngine engine = new ComplianceEngine();
        engine.addRule(new ComplianceEngine.MaxNotionalRule(1_000_000));   // max $1M per trade
        engine.addRule(new ComplianceEngine.MinQuantityRule(10));           // min 10 units
        engine.addRule(new ComplianceEngine.RestrictedInstrumentRule());    // no derivatives

        TradeOrderService service = new TradeOrderService(repository, engine);

        Portfolio portfolio = new Portfolio("PORT-001", "Bhavesh Karnam", 5_000_000.00);

        System.out.println("====== Trade Order Manager — Demo Run ======\n");
        System.out.println("Portfolio: " + portfolio + "\n");

        // --- Trade 1: Valid EQUITY BUY ---
        Trade t1 = new Trade("TRD-001", "AAPL", Trade.InstrumentType.EQUITY,
                Trade.OrderSide.BUY, 500, 185.50);
        printResult(service.submitOrder(t1, portfolio));

        // --- Trade 2: Valid FIXED INCOME BUY ---
        Trade t2 = new Trade("TRD-002", "US10Y", Trade.InstrumentType.FIXED_INCOME,
                Trade.OrderSide.BUY, 100, 980.00);
        printResult(service.submitOrder(t2, portfolio));

        // --- Trade 3: Fails MaxNotional (quantity=10000, price=200 = $2M) ---
        Trade t3 = new Trade("TRD-003", "MSFT", Trade.InstrumentType.EQUITY,
                Trade.OrderSide.BUY, 10000, 200.00);
        printResult(service.submitOrder(t3, portfolio));

        // --- Trade 4: Fails RestrictedInstrument ---
        Trade t4 = new Trade("TRD-004", "SPX-OPT", Trade.InstrumentType.DERIVATIVE,
                Trade.OrderSide.BUY, 50, 300.00);
        printResult(service.submitOrder(t4, portfolio));

        // --- Trade 5: Valid SELL ---
        Trade t5 = new Trade("TRD-005", "AAPL", Trade.InstrumentType.EQUITY,
                Trade.OrderSide.SELL, 100, 190.00);
        printResult(service.submitOrder(t5, portfolio));

        // --- Portfolio Summary ---
        System.out.println("\n====== Final Portfolio Summary ======");
        System.out.println(portfolio);
        System.out.printf("Total EQUITY notional executed: $%.2f%n",
                service.getExecutedNotional(Trade.InstrumentType.EQUITY));

        // --- Trades for AAPL ---
        System.out.println("\n--- AAPL Trade History ---");
        service.getTradesByTicker("AAPL").forEach(System.out::println);

        conn.close();
    }

    private static void printResult(TradeOrderService.OrderResult result) {
        System.out.printf("[%s] %s%n", result.success ? "✓ SUCCESS" : "✗ REJECTED", result.message);
    }
}
