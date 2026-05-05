-- ============================================================
--  Warehouse ETL Pipeline — Database Schema (DDL)
--  Compatible with: PostgreSQL / MySQL / SQLite
-- ============================================================

-- Orders master table
CREATE TABLE IF NOT EXISTS orders (
    order_id        VARCHAR(50)    PRIMARY KEY,
    customer_id     VARCHAR(50)    NOT NULL,
    product_id      VARCHAR(50)    NOT NULL,
    warehouse_id    VARCHAR(20)    NOT NULL,
    quantity        INTEGER        NOT NULL CHECK (quantity > 0),
    unit_price      DECIMAL(12,2)  NOT NULL CHECK (unit_price > 0),
    total_amount    DECIMAL(14,2)  GENERATED ALWAYS AS (quantity * unit_price) STORED,
    status          VARCHAR(30)    NOT NULL DEFAULT 'PENDING',
    order_date      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at    TIMESTAMP,
    error_message   TEXT,
    created_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Inventory master table
CREATE TABLE IF NOT EXISTS inventory (
    inventory_id        VARCHAR(100)  PRIMARY KEY,   -- product_id + warehouse_id
    product_id          VARCHAR(50)   NOT NULL,
    warehouse_id        VARCHAR(20)   NOT NULL,
    quantity_available  INTEGER       NOT NULL DEFAULT 0,
    quantity_reserved   INTEGER       NOT NULL DEFAULT 0,
    reorder_threshold   INTEGER       NOT NULL DEFAULT 50,
    last_updated        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (product_id, warehouse_id)
);

-- Pipeline run audit log
CREATE TABLE IF NOT EXISTS pipeline_runs (
    run_id          VARCHAR(50)   PRIMARY KEY,
    run_timestamp   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total_input     INTEGER,
    total_success   INTEGER,
    total_failed    INTEGER,
    total_skipped   INTEGER,
    duration_ms     INTEGER,
    status          VARCHAR(20),  -- SUCCESS | PARTIAL | FAILED
    notes           TEXT
);

-- Error log for failed orders
CREATE TABLE IF NOT EXISTS order_errors (
    error_id        SERIAL        PRIMARY KEY,
    order_id        VARCHAR(50)   NOT NULL,
    run_id          VARCHAR(50),
    error_type      VARCHAR(50),  -- VALIDATION | STOCK | SYSTEM | DUPLICATE
    error_message   TEXT,
    occurred_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved        BOOLEAN       DEFAULT FALSE
);

-- Indexes for query performance
CREATE INDEX IF NOT EXISTS idx_orders_status        ON orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_customer      ON orders(customer_id);
CREATE INDEX IF NOT EXISTS idx_orders_product       ON orders(product_id);
CREATE INDEX IF NOT EXISTS idx_orders_warehouse     ON orders(warehouse_id);
CREATE INDEX IF NOT EXISTS idx_orders_date          ON orders(order_date);
CREATE INDEX IF NOT EXISTS idx_inventory_product    ON inventory(product_id);
CREATE INDEX IF NOT EXISTS idx_inventory_warehouse  ON inventory(warehouse_id);
