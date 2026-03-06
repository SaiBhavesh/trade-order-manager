package com.trademanager.engine;

import com.trademanager.model.Trade;
import java.util.ArrayList;
import java.util.List;

/**
 * Compliance Rule Engine using the Strategy design pattern.
 * Each ComplianceRule is independently pluggable and testable.
 */
public class ComplianceEngine {

    // Strategy interface
    public interface ComplianceRule {
        String getRuleName();
        boolean validate(Trade trade);
        String getViolationMessage(Trade trade);
    }

    // --- Concrete Rules ---

    /** Rule 1: Max notional value per trade */
    public static class MaxNotionalRule implements ComplianceRule {
        private final double maxNotional;

        public MaxNotionalRule(double maxNotional) {
            this.maxNotional = maxNotional;
        }

        @Override public String getRuleName() { return "MAX_NOTIONAL"; }

        @Override
        public boolean validate(Trade trade) {
            return trade.getNotionalValue() <= maxNotional;
        }

        @Override
        public String getViolationMessage(Trade trade) {
            return String.format("Trade %s exceeds max notional: %.2f > %.2f",
                    trade.getTradeId(), trade.getNotionalValue(), maxNotional);
        }
    }

    /** Rule 2: Minimum trade quantity */
    public static class MinQuantityRule implements ComplianceRule {
        private final double minQty;

        public MinQuantityRule(double minQty) { this.minQty = minQty; }

        @Override public String getRuleName() { return "MIN_QUANTITY"; }

        @Override
        public boolean validate(Trade trade) {
            return trade.getQuantity() >= minQty;
        }

        @Override
        public String getViolationMessage(Trade trade) {
            return String.format("Trade %s quantity %.0f is below minimum %.0f",
                    trade.getTradeId(), trade.getQuantity(), minQty);
        }
    }

    /** Rule 3: Block derivative trades (restricted instrument type) */
    public static class RestrictedInstrumentRule implements ComplianceRule {
        @Override public String getRuleName() { return "RESTRICTED_INSTRUMENT"; }

        @Override
        public boolean validate(Trade trade) {
            return trade.getInstrumentType() != Trade.InstrumentType.DERIVATIVE;
        }

        @Override
        public String getViolationMessage(Trade trade) {
            return String.format("Trade %s uses restricted instrument type: %s",
                    trade.getTradeId(), trade.getInstrumentType());
        }
    }

    // --- Engine logic ---

    public static class ComplianceResult {
        public final boolean passed;
        public final List<String> violations;

        public ComplianceResult(boolean passed, List<String> violations) {
            this.passed = passed;
            this.violations = violations;
        }
    }

    private final List<ComplianceRule> rules = new ArrayList<>();

    public void addRule(ComplianceRule rule) { rules.add(rule); }

    public ComplianceResult evaluate(Trade trade) {
        List<String> violations = new ArrayList<>();
        for (ComplianceRule rule : rules) {
            if (!rule.validate(trade)) {
                violations.add("[" + rule.getRuleName() + "] " + rule.getViolationMessage(trade));
            }
        }
        return new ComplianceResult(violations.isEmpty(), violations);
    }
}
