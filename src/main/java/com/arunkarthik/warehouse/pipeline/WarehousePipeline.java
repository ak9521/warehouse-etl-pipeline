package com.arunkarthik.warehouse.pipeline;

import com.arunkarthik.warehouse.model.Inventory;
import com.arunkarthik.warehouse.model.Order;
import com.arunkarthik.warehouse.service.OrderProcessingService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * WarehousePipeline — Main Entry Point
 *
 * Simulates a full warehouse order ETL batch run:
 *   1. Load inventory master data
 *   2. Ingest raw orders (CSV/API in production)
 *   3. Validate, process, and reconcile
 *   4. Print analytics summary
 *
 * Run: javac *.java && java com.arunkarthik.warehouse.pipeline.WarehousePipeline
 */
public class WarehousePipeline {

    private static final Logger log = Logger.getLogger(WarehousePipeline.class.getName());

    public static void main(String[] args) {
        log.info("╔══════════════════════════════════════════════╗");
        log.info("║   Warehouse Order ETL Pipeline               ║");
        log.info("║   Author: Arun Karthik Bodduri               ║");
        log.info("╚══════════════════════════════════════════════╝\n");

        OrderProcessingService service = new OrderProcessingService();

        // ── Step 1: Load Inventory ─────────────────────────────────
        log.info("[Step 1] Loading inventory master data...");
        List<Inventory> inventory = buildSampleInventory();
        service.loadInventory(inventory);

        // ── Step 2: Ingest Orders ──────────────────────────────────
        log.info("[Step 2] Ingesting order batch...");
        List<Order> orders = buildSampleOrders();
        log.info("  Orders to process: " + orders.size());

        // ── Step 3: Process Batch ──────────────────────────────────
        log.info("[Step 3] Running ETL processing pipeline...");
        OrderProcessingService.BatchResult result = service.processBatch(orders);

        // ── Step 4: Reconcile ──────────────────────────────────────
        log.info("[Step 4] Running reconciliation...");
        service.reconcile(orders);

        // ── Step 5: Analytics Summary ──────────────────────────────
        log.info("[Step 5] Analytics summary...");

        log.info("\n  Order Count by Status:");
        Map<String, Long> byStatus = service.getOrderCountByStatus();
        byStatus.forEach((status, count) ->
            log.info("    " + status + " : " + count));

        log.info("\n  Revenue by Warehouse:");
        Map<String, BigDecimal> byWarehouse = service.getRevenueByWarehouse();
        byWarehouse.forEach((wh, revenue) ->
            log.info("    " + wh + " : ₹" + revenue));

        log.info("\n✓ Pipeline complete.");
    }

    // ── Sample Data Builders ───────────────────────────────────────

    private static List<Inventory> buildSampleInventory() {
        return List.of(
            new Inventory("PROD001", "WH-BLR", 500, 50),
            new Inventory("PROD002", "WH-BLR", 200, 30),
            new Inventory("PROD003", "WH-BLR",  10, 20),   // low stock — will trigger reorder
            new Inventory("PROD001", "WH-MUM", 300, 40),
            new Inventory("PROD002", "WH-MUM", 150, 25)
        );
    }

    private static List<Order> buildSampleOrders() {
        List<Order> orders = new ArrayList<>();

        // Valid orders
        orders.add(new Order("ORD-001", "CUST-A", "PROD001", 10, new BigDecimal("499.00"), "WH-BLR"));
        orders.add(new Order("ORD-002", "CUST-B", "PROD002", 5,  new BigDecimal("1299.00"), "WH-BLR"));
        orders.add(new Order("ORD-003", "CUST-C", "PROD001", 25, new BigDecimal("499.00"), "WH-MUM"));
        orders.add(new Order("ORD-004", "CUST-D", "PROD002", 3,  new BigDecimal("1299.00"), "WH-MUM"));

        // Low stock — will use remaining and trigger reorder alert
        orders.add(new Order("ORD-005", "CUST-E", "PROD003", 8,  new BigDecimal("2499.00"), "WH-BLR"));

        // Insufficient stock — should fail
        orders.add(new Order("ORD-006", "CUST-F", "PROD003", 50, new BigDecimal("2499.00"), "WH-BLR"));

        // Duplicate — should be skipped
        orders.add(new Order("ORD-001", "CUST-A", "PROD001", 10, new BigDecimal("499.00"), "WH-BLR"));

        // Invalid — missing customer ID
        Order invalid = new Order();
        invalid.setOrderId("ORD-007");
        invalid.setProductId("PROD001");
        invalid.setQuantity(5);
        invalid.setUnitPrice(new BigDecimal("499.00"));
        invalid.setWarehouseId("WH-BLR");
        orders.add(invalid); // customerId is null → validation fail

        // No inventory record
        orders.add(new Order("ORD-008", "CUST-G", "PROD999", 1,  new BigDecimal("999.00"), "WH-BLR"));

        return orders;
    }
}
