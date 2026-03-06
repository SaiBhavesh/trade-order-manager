package com.trademanager;

import com.trademanager.engine.ComplianceEngine;
import com.trademanager.model.Portfolio;
import com.trademanager.model.Trade;
import com.trademanager.repository.TradeRepository;
import com.trademanager.service.TradeOrderService;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 test suite for the Trade Order Manager system.
 * Covers: model validation, compliance rules, portfolio logic, SQL persistence, and service orchestration.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TradeOrderManagerTest {

    private static Connection conn;
    private static TradeRepository repository;
    private static ComplianceEngine engine;
    private static TradeOrderService service;
    private static Portfolio portfolio;

    @BeforeAll
    static void setup() throws Exception {
        conn = DriverManager.getConnection("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1", "sa", "");
        repository = new TradeRepository(conn);

        engine = new ComplianceEngine();
        engine.addRule(new ComplianceEngine.MaxNotionalRule(500_000));
        engine.addRule(new ComplianceEngine.MinQuantityRule(5));
        engine.addRule(new ComplianceEngine.RestrictedInstrumentRule());

        service = new TradeOrderService(repository, engine);
        portfolio = new Portfolio("PORT-TEST", "Test User", 2_000_000.00);
    }

    @AfterAll
    static void teardown() throws Exception {
        conn.close();
    }

    // ========== Trade Model Tests ==========

    @Test
    @Order(1)
    void testTradeCreation_validInputs() {
        Trade t = new Trade("T001", "AAPL", Trade.InstrumentType.EQUITY,
                Trade.OrderSide.BUY, 100, 150.0);
        assertEquals("T001", t.getTradeId());
        assertEquals(Trade.TradeStatus.PENDING, t.getStatus());
        assertEquals(15000.0, t.getNotionalValue(), 0.001);
    }

    @Test
    @Order(2)
    void testTradeCreation_negativeQuantity_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                new Trade("T002", "AAPL", Trade.InstrumentType.EQUITY,
                        Trade.OrderSide.BUY, -10, 100.0));
    }

    @Test
    @Order(3)
    void testTradeCreation_blankId_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                new Trade("", "AAPL", Trade.InstrumentType.EQUITY,
                        Trade.OrderSide.BUY, 10, 100.0));
    }

    @Test
    @Order(4)
    void testTradeStatusUpdate() {
        Trade t = new Trade("T003", "MSFT", Trade.InstrumentType.EQUITY,
                Trade.OrderSide.SELL, 50, 300.0);
        t.updateStatus(Trade.TradeStatus.EXECUTED);
        assertEquals(Trade.TradeStatus.EXECUTED, t.getStatus());
    }

    // ========== Compliance Engine Tests ==========

    @Test
    @Order(5)
    void testCompliance_validTrade_passes() {
        Trade t = new Trade("TC001", "GOOG", Trade.InstrumentType.EQUITY,
                Trade.OrderSide.BUY, 100, 100.0);
        ComplianceEngine.ComplianceResult result = engine.evaluate(t);
        assertTrue(result.passed);
        assertTrue(result.violations.isEmpty());
    }

    @Test
    @Order(6)
    void testCompliance_exceedsMaxNotional_fails() {
        Trade t = new Trade("TC002", "TSLA", Trade.InstrumentType.EQUITY,
                Trade.OrderSide.BUY, 10000, 200.0); // notional = 2,000,000
        ComplianceEngine.ComplianceResult result = engine.evaluate(t);
        assertFalse(result.passed);
        assertTrue(result.violations.stream().anyMatch(v -> v.contains("MAX_NOTIONAL")));
    }

    @Test
    @Order(7)
    void testCompliance_belowMinQuantity_fails() {
        Trade t = new Trade("TC003", "IBM", Trade.InstrumentType.EQUITY,
                Trade.OrderSide.BUY, 2, 100.0); // qty < 5
        ComplianceEngine.ComplianceResult result = engine.evaluate(t);
        assertFalse(result.passed);
        assertTrue(result.violations.stream().anyMatch(v -> v.contains("MIN_QUANTITY")));
    }

    @Test
    @Order(8)
    void testCompliance_restrictedDerivative_fails() {
        Trade t = new Trade("TC004", "SPX", Trade.InstrumentType.DERIVATIVE,
                Trade.OrderSide.BUY, 10, 100.0);
        ComplianceEngine.ComplianceResult result = engine.evaluate(t);
        assertFalse(result.passed);
        assertTrue(result.violations.stream().anyMatch(v -> v.contains("RESTRICTED_INSTRUMENT")));
    }

    // ========== Portfolio Tests ==========

    @Test
    @Order(9)
    void testPortfolio_applyBuyTrade_reducesCash() {
        Portfolio p = new Portfolio("P-TEST2", "Alice", 100_000.0);
        Trade t = new Trade("TP001", "AAPL", Trade.InstrumentType.EQUITY,
                Trade.OrderSide.BUY, 10, 100.0);
        t.updateStatus(Trade.TradeStatus.EXECUTED);
        p.applyTrade(t);
        assertEquals(99_000.0, p.getCashBalance(), 0.001);
    }

    @Test
    @Order(10)
    void testPortfolio_applySellTrade_increasesCash() {
        Portfolio p = new Portfolio("P-TEST3", "Bob", 50_000.0);
        Trade t = new Trade("TP002", "MSFT", Trade.InstrumentType.EQUITY,
                Trade.OrderSide.SELL, 20, 250.0);
        t.updateStatus(Trade.TradeStatus.EXECUTED);
        p.applyTrade(t);
        assertEquals(55_000.0, p.getCashBalance(), 0.001);
    }

    @Test
    @Order(11)
    void testPortfolio_insufficientCash_throwsException() {
        Portfolio p = new Portfolio("P-TEST4", "Charlie", 500.0);
        Trade t = new Trade("TP003", "AAPL", Trade.InstrumentType.EQUITY,
                Trade.OrderSide.BUY, 100, 200.0); // notional = 20,000
        t.updateStatus(Trade.TradeStatus.EXECUTED);
        assertThrows(IllegalStateException.class, () -> p.applyTrade(t));
    }

    @Test
    @Order(12)
    void testPortfolio_applyPendingTrade_throwsException() {
        Portfolio p = new Portfolio("P-TEST5", "Dave", 100_000.0);
        Trade t = new Trade("TP004", "AAPL", Trade.InstrumentType.EQUITY,
                Trade.OrderSide.BUY, 10, 100.0); // still PENDING
        assertThrows(IllegalStateException.class, () -> p.applyTrade(t));
    }

    // ========== SQL Repository Tests ==========

    @Test
    @Order(13)
    void testRepository_saveAndFindById() throws Exception {
        Trade t = new Trade("SQL001", "META", Trade.InstrumentType.EQUITY,
                Trade.OrderSide.BUY, 50, 300.0);
        repository.save(t);
        assertTrue(repository.findById("SQL001").isPresent());
        assertEquals("META", repository.findById("SQL001").get().getTicker());
    }

    @Test
    @Order(14)
    void testRepository_findByStatus() throws Exception {
        Trade t = new Trade("SQL002", "NVDA", Trade.InstrumentType.EQUITY,
                Trade.OrderSide.BUY, 30, 400.0);
        repository.save(t);
        List<Trade> pending = repository.findByStatus(Trade.TradeStatus.PENDING);
        assertTrue(pending.stream().anyMatch(tr -> tr.getTradeId().equals("SQL002")));
    }

    @Test
    @Order(15)
    void testRepository_findByTicker() throws Exception {
        Trade t = new Trade("SQL003", "AMZN", Trade.InstrumentType.EQUITY,
                Trade.OrderSide.SELL, 20, 180.0);
        repository.save(t);
        List<Trade> result = repository.findByTicker("AMZN");
        assertFalse(result.isEmpty());
        assertEquals("AMZN", result.get(0).getTicker());
    }

    // ========== Service Integration Tests ==========

    @Test
    @Order(16)
    void testService_submitValidOrder_succeeds() throws Exception {
        Trade t = new Trade("SVC001", "AAPL", Trade.InstrumentType.EQUITY,
                Trade.OrderSide.BUY, 100, 150.0);
        TradeOrderService.OrderResult result = service.submitOrder(t, portfolio);
        assertTrue(result.success);
        assertEquals(Trade.TradeStatus.EXECUTED, result.trade.getStatus());
    }

    @Test
    @Order(17)
    void testService_submitNonCompliantOrder_rejected() throws Exception {
        Trade t = new Trade("SVC002", "SPX", Trade.InstrumentType.DERIVATIVE,
                Trade.OrderSide.BUY, 10, 200.0);
        TradeOrderService.OrderResult result = service.submitOrder(t, portfolio);
        assertFalse(result.success);
        assertEquals(Trade.TradeStatus.REJECTED, result.trade.getStatus());
    }

    @Test
    @Order(18)
    void testService_totalNotionalQuery() throws Exception {
        double notional = service.getExecutedNotional(Trade.InstrumentType.EQUITY);
        assertTrue(notional > 0);
    }
}
