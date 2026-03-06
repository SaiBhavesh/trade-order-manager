package com.trademanager.repository;

import com.trademanager.model.Trade;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SQL-backed repository for persisting and querying Trade records.
 * Uses JDBC with parameterized queries to prevent SQL injection.
 * Implements the Repository design pattern for clean data access separation.
 */
public class TradeRepository {

    private final Connection connection;

    public TradeRepository(Connection connection) throws SQLException {
        this.connection = connection;
        initializeSchema();
    }

    private void initializeSchema() throws SQLException {
        String createTable = """
                CREATE TABLE IF NOT EXISTS trades (
                    trade_id        VARCHAR(50)    PRIMARY KEY,
                    ticker          VARCHAR(20)    NOT NULL,
                    instrument_type VARCHAR(20)    NOT NULL,
                    side            VARCHAR(10)    NOT NULL,
                    quantity        DOUBLE         NOT NULL,
                    price           DOUBLE         NOT NULL,
                    status          VARCHAR(20)    NOT NULL,
                    notional_value  DOUBLE         NOT NULL,
                    created_at      TIMESTAMP      NOT NULL,
                    updated_at      TIMESTAMP      NOT NULL
                )
                """;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTable);
        }
    }

    public void save(Trade trade) throws SQLException {
        String sql = """
                MERGE INTO trades (trade_id, ticker, instrument_type, side,
                    quantity, price, status, notional_value, created_at, updated_at)
                KEY(trade_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, trade.getTradeId());
            ps.setString(2, trade.getTicker());
            ps.setString(3, trade.getInstrumentType().name());
            ps.setString(4, trade.getSide().name());
            ps.setDouble(5, trade.getQuantity());
            ps.setDouble(6, trade.getPrice());
            ps.setString(7, trade.getStatus().name());
            ps.setDouble(8, trade.getNotionalValue());
            ps.setTimestamp(9, Timestamp.valueOf(trade.getCreatedAt()));
            ps.setTimestamp(10, Timestamp.valueOf(trade.getUpdatedAt()));
            ps.executeUpdate();
        }
    }

    public Optional<Trade> findById(String tradeId) throws SQLException {
        String sql = "SELECT * FROM trades WHERE trade_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, tradeId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapRow(rs));
        }
        return Optional.empty();
    }

    public List<Trade> findByStatus(Trade.TradeStatus status) throws SQLException {
        String sql = "SELECT * FROM trades WHERE status = ?";
        List<Trade> result = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) result.add(mapRow(rs));
        }
        return result;
    }

    public List<Trade> findByTicker(String ticker) throws SQLException {
        String sql = "SELECT * FROM trades WHERE ticker = ? ORDER BY created_at DESC";
        List<Trade> result = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, ticker);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) result.add(mapRow(rs));
        }
        return result;
    }

    public double getTotalNotionalByInstrument(Trade.InstrumentType type) throws SQLException {
        String sql = "SELECT SUM(notional_value) FROM trades WHERE instrument_type = ? AND status = 'EXECUTED'";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, type.name());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble(1);
        }
        return 0.0;
    }

    private Trade mapRow(ResultSet rs) throws SQLException {
        Trade trade = new Trade(
                rs.getString("trade_id"),
                rs.getString("ticker"),
                Trade.InstrumentType.valueOf(rs.getString("instrument_type")),
                Trade.OrderSide.valueOf(rs.getString("side")),
                rs.getDouble("quantity"),
                rs.getDouble("price")
        );
        trade.updateStatus(Trade.TradeStatus.valueOf(rs.getString("status")));
        return trade;
    }
}
