# Trade Order Manager 📈

A portfolio and trade order management system built in Java — inspired by Charles River Investment Management System (CRD) internship role at State Street.

## Overview

This system simulates a real-world trade order lifecycle:
- Submit a trade order
- Run compliance validation rules
- Execute valid trades and apply them to a portfolio
- Persist all trades to a SQL database

## Features

| Feature | Implementation |
|---|---|
| OOP Design Patterns | Strategy (ComplianceEngine), Repository, Facade (TradeOrderService) |
| SQL Persistence | JDBC with H2 database, parameterized queries, schema init |
| Unit Testing | JUnit 5 — 18 tests covering model, compliance, portfolio, SQL, and service |
| Financial Instruments | EQUITY, FIXED_INCOME, FX, DERIVATIVE |
| Compliance Engine | Pluggable rules: MaxNotional, MinQuantity, RestrictedInstrument |
| Portfolio Management | Cash balance tracking, P&L, trade history |

## Project Structure

```
src/
├── main/java/com/trademanager/
│   ├── Main.java                          # Entry point / demo
│   ├── model/
│   │   ├── Trade.java                     # Trade entity (enums, validation)
│   │   └── Portfolio.java                 # Portfolio with cash tracking
│   ├── engine/
│   │   └── ComplianceEngine.java          # Strategy pattern rule engine
│   ├── repository/
│   │   └── TradeRepository.java           # SQL JDBC repository
│   └── service/
│       └── TradeOrderService.java         # Facade orchestration layer
└── test/java/com/trademanager/
    └── TradeOrderManagerTest.java         # 18 JUnit 5 tests
```

## How to Run

### Prerequisites
- Java 17+
- Maven 3.8+

### Run the demo
```bash
mvn compile exec:java -Dexec.mainClass="com.trademanager.Main"
```

### Run all tests
```bash
mvn test
```

## Sample Output

```
====== Trade Order Manager — Demo Run ======

Portfolio: Portfolio[PORT-001 | owner=Bhavesh Karnam | cash=5000000.00 | trades=0 | invested=0.00]

[✓ SUCCESS] Trade executed successfully: TRD-001
[✓ SUCCESS] Trade executed successfully: TRD-002
[✗ REJECTED] Trade rejected — compliance violations: [MAX_NOTIONAL] Trade TRD-003 exceeds max notional
[✗ REJECTED] Trade rejected — compliance violations: [RESTRICTED_INSTRUMENT] Trade TRD-004 uses restricted instrument type
[✓ SUCCESS] Trade executed successfully: TRD-005

====== Final Portfolio Summary ======
Portfolio[PORT-001 | owner=Bhavesh Karnam | cash=4912500.00 | trades=3 | invested=110750.00]
```

## Design Patterns Used

- **Strategy** — `ComplianceRule` interface with pluggable rule implementations
- **Repository** — `TradeRepository` abstracts all SQL data access
- **Facade** — `TradeOrderService` provides a clean API over complex subsystems

## Technologies
- Java 17
- JDBC / H2 SQL Database
- JUnit 5
- Maven
