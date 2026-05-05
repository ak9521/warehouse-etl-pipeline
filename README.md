# 🏭 Warehouse Order Processing & ETL Pipeline

![Java](https://img.shields.io/badge/Java-17+-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?style=flat-square&logo=spring&logoColor=white)
![SQL](https://img.shields.io/badge/SQL-PostgreSQL-4479A1?style=flat-square&logo=postgresql&logoColor=white)
![ETL](https://img.shields.io/badge/Pattern-ETL%20Pipeline-orange?style=flat-square)
![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)

A **production-grade order and inventory data pipeline** built in Java, simulating the backend data processing systems used in e-commerce and supply chain operations. Implements ETL validation, inventory management, reconciliation, and SQL analytics — directly mirroring enterprise data engineering patterns.

---

## 📌 What This Project Demonstrates

| Skill | Implementation |
|-------|---------------|
| Java Backend Engineering | Domain models, service layer, pipeline orchestration |
| ETL Validation Logic | Multi-rule validation engine with structured error categorization |
| Inventory Management | Stock reservation, fulfilment, reorder alert triggers |
| Data Reconciliation | Source vs target count and revenue verification |
| SQL Design | Normalized schema with indexes, generated columns, audit tables |
| Analytics SQL | Window functions, CTEs, ranking, customer segmentation |
| Production Patterns | Error logging, audit trails, status lifecycle management |

---

## 🏗️ Architecture

```
Raw Orders (CSV / REST API / Message Queue)
        │
        ▼
┌──────────────────────┐
│   VALIDATION LAYER    │  Schema · Business rules · Null checks
│   OrderProcessing     │  → Valid orders + Rejected orders (with reasons)
│   Service.java        │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│   INVENTORY CHECK     │  Stock availability · Duplicate detection
│                       │  → Reserve stock for valid orders
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│   FULFILMENT          │  Stock deduction · Status update: PENDING → PROCESSING → SHIPPED
│                       │  → Reorder alert if stock drops below threshold
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│   RECONCILIATION      │  Source count vs processed count · Revenue reconciliation
│                       │  → Reconciliation report with match rate
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│   SQL ANALYTICS       │  Fulfilment rate · Revenue trend · Inventory health · Segmentation
└──────────────────────┘
```

---

## 🚀 Quick Start

### Prerequisites
- Java 17+
- No additional dependencies required (pure Java standard library)

### Compile & Run

```bash
git clone https://github.com/arunkarthikb/warehouse-etl-pipeline.git
cd warehouse-etl-pipeline

# Compile all Java files
javac -d out $(find src -name "*.java")

# Run the pipeline
java -cp out com.arunkarthik.warehouse.pipeline.WarehousePipeline
```

### Expected Output

```
╔══════════════════════════════════════════════╗
║   Warehouse Order Processing ETL Pipeline    ║
╚══════════════════════════════════════════════╝

[Step 1] Loading inventory master data...
  Loaded 5 inventory records

[Step 2] Ingesting order batch...
  Orders to process: 9

[Step 3] Running ETL processing pipeline...
  ✓ SUCCESS [ORD-001] qty=10  total=4990.00  remaining_stock=490
  ✓ SUCCESS [ORD-002] qty=5   total=6495.00  remaining_stock=195
  ✓ SUCCESS [ORD-003] qty=25  total=12475.00 remaining_stock=275
  ✓ SUCCESS [ORD-004] qty=3   total=3897.00  remaining_stock=147
  ✓ SUCCESS [ORD-005] qty=8   total=19992.00 remaining_stock=2
  ⚠ REORDER ALERT: Product PROD003 at WH-BLR is below reorder threshold (2 remaining)
  ✗ FAILED  [ORD-006]: Insufficient stock — Available: 2, Requested: 50
  ⊘ SKIPPED [ORD-001]: duplicate order ID
  ✗ FAILED  [ORD-007]: Missing customerId
  ✗ FAILED  [ORD-008]: No inventory record for PROD999/WH-BLR

Batch Summary:
  Total   : 9
  Success : 5 (55.6%)
  Failed  : 3
  Skipped : 1

[Step 4] Reconciliation Report:
  Expected orders  : 9
  Processed        : 5
  Failed           : 3
  Revenue          : ₹47,849.00

[Step 5] Analytics:
  Order count by status:
    SHIPPED              : 5
    INSUFFICIENT_STOCK   : 1
    INVALID              : 1
    NO_INVENTORY_RECORD  : 1
    DUPLICATE            : 1
  Revenue by warehouse:
    WH-BLR : ₹31,477.00
    WH-MUM : ₹16,372.00
```

---

## 📁 Project Structure

```
warehouse-etl-pipeline/
├── src/main/java/com/arunkarthik/warehouse/
│   ├── model/
│   │   ├── Order.java               ← Order entity with business logic
│   │   └── Inventory.java           ← Inventory entity with stock operations
│   ├── service/
│   │   └── OrderProcessingService.java  ← ETL service: validate → reserve → fulfil → reconcile
│   └── pipeline/
│       └── WarehousePipeline.java   ← Main entry point — wires all stages
│
├── sql/
│   ├── ddl/
│   │   └── schema.sql               ← Full database schema with indexes
│   └── analytics/
│       └── warehouse_analytics.sql  ← 6 analytics queries (window functions, CTEs)
│
└── docs/
    └── architecture.md              ← System design decisions
```

---

## ✅ Validation Rules

Every order is validated against these rules before processing:

```
Rule 1: orderId         — must not be null or blank
Rule 2: customerId      — must not be null or blank
Rule 3: productId       — must not be null or blank
Rule 4: warehouseId     — must not be null or blank
Rule 5: quantity        — must be > 0
Rule 6: unitPrice       — must be > 0
Rule 7: Duplicate check — orderId must be unique in current run
Rule 8: Inventory check — product/warehouse combination must exist
Rule 9: Stock check     — effective available ≥ requested quantity
```

---

## 📊 SQL Analytics Included

| Query | Business Question |
|-------|------------------|
| Fulfilment Rate by Warehouse | Which warehouse is most efficient? |
| Daily Revenue Trend + 7-Day Rolling Average | How is revenue trending? |
| Top Products by Revenue | What are the best-selling SKUs? |
| Inventory Health Report | Which products need urgent reorder? |
| Error Analysis | What's causing the most pipeline failures? |
| Customer Tier Segmentation | Who are the PLATINUM / GOLD customers? |

---

## 🗺️ Production Roadmap

- [ ] Add Spring Boot REST API layer (POST /orders/process)
- [ ] Connect to PostgreSQL with JPA/Hibernate repositories
- [ ] Add async message queue processing (Kafka consumer)
- [ ] Add Spring Batch for large-scale batch ETL jobs
- [ ] Add distributed tracing with OpenTelemetry

---

## 👤 Author

**Arun Karthik Bodduri** — Data Engineer  
[LinkedIn](https://linkedin.com/in/arunkarthikb) · [GitHub](https://github.com/arunkarthikb)  
3+ years @ Capgemini building high-volume transactional data systems in Java and Spring Boot.

---

*Part of a data engineering portfolio. See also:*  
*[Fraud Detection Pipeline](https://github.com/arunkarthikb/fraud-detection-pipeline) · [Loan Default Predictor](https://github.com/arunkarthikb/loan-default-predictor)*
