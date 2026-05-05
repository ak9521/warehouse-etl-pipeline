package com.arunkarthik.warehouse.service;

import com.arunkarthik.warehouse.model.Inventory;
import com.arunkarthik.warehouse.model.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * OrderProcessingService — Core ETL Service Layer
 *
 * Handles:
 *   1. Order validation (schema + business rules)
 *   2. Inventory reservation and fulfilment
 *   3. Order status lifecycle management
 *   4. Reconciliation and error tracking
 *
 * This mirrors a production Spring Boot @Service component.
 * In production: inject OrderRepository and InventoryRepository via @Autowired.
 */
public class OrderProcessingService {

    private static final Logger log = Logger.getLogger(OrderProcessingService.class.getName());

    // In-memory stores (replace with JPA repositories in production)
    private final Map<String, Order> orderStore = new LinkedHashMap<>();
    private final Map<String, Inventory> inventoryStore = new LinkedHashMap<>();

    // Pipeline statistics
    private int totalProcessed = 0;
    private int totalFailed = 0;
    private int totalSkipped = 0;

    // ── Inventory Setup ────────────────────────────────────────────

    public void loadInventory(List<Inventory> inventoryList) {
        for (Inventory inv : inventoryList) {
            inventoryStore.put(inv.getProductId() + "_" + inv.getWarehouseId(), inv);
        }
        log.info("Loaded " + inventoryList.size() + " inventory records");
    }

    // ── Validation ─────────────────────────────────────────────────

    public List<String> validateOrder(Order order) {
        List<String> errors = new ArrayList<>();

        if (order == null) {
            errors.add("Order object is null");
            return errors;
        }
        if (order.getOrderId() == null || order.getOrderId().isBlank())
            errors.add("Missing orderId");
        if (order.getCustomerId() == null || order.getCustomerId().isBlank())
            errors.add("Missing customerId");
        if (order.getProductId() == null || order.getProductId().isBlank())
            errors.add("Missing productId");
        if (order.getQuantity() == null || order.getQuantity() <= 0)
            errors.add("Quantity must be > 0 (got: " + order.getQuantity() + ")");
        if (order.getUnitPrice() == null || order.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0)
            errors.add("Unit price must be > 0");
        if (order.getWarehouseId() == null || order.getWarehouseId().isBlank())
            errors.add("Missing warehouseId");

        return errors;
    }

    // ── ETL Processing ─────────────────────────────────────────────

    /**
     * Process a batch of orders through the full ETL pipeline:
     * validate → check inventory → reserve stock → update status
     */
    public BatchResult processBatch(List<Order> orders) {
        log.info("=".repeat(50));
        log.info("Starting batch processing: " + orders.size() + " orders");
        log.info("=".repeat(50));

        List<Order> successful = new ArrayList<>();
        List<Order> failed = new ArrayList<>();
        List<Order> skipped = new ArrayList<>();

        for (Order order : orders) {
            try {
                ProcessingResult result = processOrder(order);
                switch (result.status) {
                    case SUCCESS  -> successful.add(order);
                    case FAILED   -> failed.add(order);
                    case SKIPPED  -> skipped.add(order);
                }
            } catch (Exception e) {
                order.setStatus("ERROR");
                order.setErrorMessage("Unexpected error: " + e.getMessage());
                failed.add(order);
                log.warning("Unexpected error processing order " + order.getOrderId() + ": " + e.getMessage());
            }
        }

        totalProcessed += successful.size();
        totalFailed    += failed.size();
        totalSkipped   += skipped.size();

        BatchResult batch = new BatchResult(successful, failed, skipped);
        log.info(batch.summary());
        return batch;
    }

    private ProcessingResult processOrder(Order order) {
        // 1. Validate
        List<String> errors = validateOrder(order);
        if (!errors.isEmpty()) {
            order.setStatus("INVALID");
            order.setErrorMessage(String.join("; ", errors));
            log.warning("  ✗ INVALID [" + order.getOrderId() + "]: " + order.getErrorMessage());
            return new ProcessingResult(ResultStatus.FAILED, errors);
        }

        // 2. Duplicate check
        if (orderStore.containsKey(order.getOrderId())) {
            order.setStatus("DUPLICATE");
            log.warning("  ⊘ SKIPPED [" + order.getOrderId() + "]: duplicate");
            return new ProcessingResult(ResultStatus.SKIPPED, List.of("Duplicate order ID"));
        }

        // 3. Inventory check
        String invKey = order.getProductId() + "_" + order.getWarehouseId();
        Inventory inventory = inventoryStore.get(invKey);

        if (inventory == null) {
            order.setStatus("NO_INVENTORY_RECORD");
            order.setErrorMessage("No inventory record found for product/warehouse combination");
            log.warning("  ✗ FAILED [" + order.getOrderId() + "]: " + order.getErrorMessage());
            return new ProcessingResult(ResultStatus.FAILED, List.of("No inventory record"));
        }

        if (!inventory.hasSufficientStock(order.getQuantity())) {
            order.setStatus("INSUFFICIENT_STOCK");
            order.setErrorMessage("Available: " + inventory.getEffectiveAvailable()
                                + ", Requested: " + order.getQuantity());
            log.warning("  ✗ FAILED [" + order.getOrderId() + "]: Insufficient stock — " + order.getErrorMessage());
            return new ProcessingResult(ResultStatus.FAILED, List.of("Insufficient stock"));
        }

        // 4. Reserve inventory & mark as processing
        inventory.reserve(order.getQuantity());
        order.setStatus("PROCESSING");
        order.setProcessedAt(LocalDateTime.now());
        orderStore.put(order.getOrderId(), order);

        // 5. Simulate fulfilment (in production: async fulfilment service)
        inventory.fulfil(order.getQuantity());
        order.setStatus("SHIPPED");

        log.info("  ✓ SUCCESS [" + order.getOrderId() + "] qty=" + order.getQuantity()
                 + " total=" + order.getTotalAmount()
                 + " remaining_stock=" + inventory.getEffectiveAvailable());

        // Reorder alert
        if (inventory.needsReorder()) {
            log.warning("  ⚠ REORDER ALERT: Product " + order.getProductId()
                      + " at warehouse " + order.getWarehouseId()
                      + " is below reorder threshold (" + inventory.getEffectiveAvailable() + " remaining)");
        }

        return new ProcessingResult(ResultStatus.SUCCESS, List.of());
    }

    // ── Reconciliation ─────────────────────────────────────────────

    public ReconciliationReport reconcile(List<Order> inputOrders) {
        log.info("\nRunning reconciliation...");
        long expectedCount = inputOrders.size();
        long processedCount = orderStore.size();
        long failedCount = totalFailed;

        BigDecimal expectedRevenue = inputOrders.stream()
            .filter(o -> "SHIPPED".equals(o.getStatus()) || "DELIVERED".equals(o.getStatus()))
            .map(Order::getTotalAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal actualRevenue = orderStore.values().stream()
            .map(Order::getTotalAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        ReconciliationReport report = new ReconciliationReport(
            expectedCount, processedCount, failedCount,
            expectedRevenue, actualRevenue
        );
        log.info(report.toString());
        return report;
    }

    // ── Analytics ──────────────────────────────────────────────────

    public Map<String, Long> getOrderCountByStatus() {
        return orderStore.values().stream()
            .collect(Collectors.groupingBy(Order::getStatus, Collectors.counting()));
    }

    public Map<String, BigDecimal> getRevenueByWarehouse() {
        return orderStore.values().stream()
            .filter(o -> o.getTotalAmount() != null)
            .collect(Collectors.groupingBy(
                Order::getWarehouseId,
                Collectors.reducing(BigDecimal.ZERO, Order::getTotalAmount, BigDecimal::add)
            ));
    }

    // ── Inner types ────────────────────────────────────────────────

    public enum ResultStatus { SUCCESS, FAILED, SKIPPED }

    public record ProcessingResult(ResultStatus status, List<String> messages) {}

    public record BatchResult(List<Order> successful, List<Order> failed, List<Order> skipped) {
        public String summary() {
            int total = successful.size() + failed.size() + skipped.size();
            return String.format(
                "\nBatch Summary:\n  Total   : %d\n  Success : %d (%.1f%%)\n  Failed  : %d\n  Skipped : %d",
                total, successful.size(), successful.size() * 100.0 / Math.max(total, 1),
                failed.size(), skipped.size()
            );
        }
    }

    public record ReconciliationReport(
        long expectedCount, long processedCount, long failedCount,
        BigDecimal expectedRevenue, BigDecimal actualRevenue
    ) {
        @Override public String toString() {
            return String.format(
                "\nReconciliation Report:\n  Expected orders : %d\n  Processed       : %d\n" +
                "  Failed          : %d\n  Expected revenue: ₹%s\n  Actual revenue  : ₹%s",
                expectedCount, processedCount, failedCount, expectedRevenue, actualRevenue
            );
        }
    }
}
