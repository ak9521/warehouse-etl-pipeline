package com.arunkarthik.warehouse.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Order entity representing a warehouse order record.
 * Maps to the ORDERS table in the transactional database.
 */
public class Order {

    private String orderId;
    private String customerId;
    private String productId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private String status;          // PENDING | PROCESSING | SHIPPED | DELIVERED | CANCELLED
    private String warehouseId;
    private LocalDateTime orderDate;
    private LocalDateTime processedAt;
    private String errorMessage;

    // ── Constructors ──────────────────────────────────────────────

    public Order() {}

    public Order(String orderId, String customerId, String productId,
                 Integer quantity, BigDecimal unitPrice, String warehouseId) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));
        this.warehouseId = warehouseId;
        this.status = "PENDING";
        this.orderDate = LocalDateTime.now();
    }

    // ── Business logic ────────────────────────────────────────────

    public boolean isValid() {
        return orderId != null && !orderId.isBlank()
            && customerId != null && !customerId.isBlank()
            && productId != null && !productId.isBlank()
            && quantity != null && quantity > 0
            && unitPrice != null && unitPrice.compareTo(BigDecimal.ZERO) > 0;
    }

    public void recalculateTotal() {
        if (unitPrice != null && quantity != null) {
            this.totalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }

    public boolean isTerminalState() {
        return "DELIVERED".equals(status) || "CANCELLED".equals(status);
    }

    // ── Getters & Setters ─────────────────────────────────────────

    public String getOrderId()                        { return orderId; }
    public void setOrderId(String orderId)            { this.orderId = orderId; }

    public String getCustomerId()                     { return customerId; }
    public void setCustomerId(String customerId)      { this.customerId = customerId; }

    public String getProductId()                      { return productId; }
    public void setProductId(String productId)        { this.productId = productId; }

    public Integer getQuantity()                      { return quantity; }
    public void setQuantity(Integer quantity)         { this.quantity = quantity; recalculateTotal(); }

    public BigDecimal getUnitPrice()                  { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice)    { this.unitPrice = unitPrice; recalculateTotal(); }

    public BigDecimal getTotalAmount()                { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount){ this.totalAmount = totalAmount; }

    public String getStatus()                         { return status; }
    public void setStatus(String status)              { this.status = status; }

    public String getWarehouseId()                    { return warehouseId; }
    public void setWarehouseId(String warehouseId)    { this.warehouseId = warehouseId; }

    public LocalDateTime getOrderDate()               { return orderDate; }
    public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }

    public LocalDateTime getProcessedAt()             { return processedAt; }
    public void setProcessedAt(LocalDateTime t)       { this.processedAt = t; }

    public String getErrorMessage()                   { return errorMessage; }
    public void setErrorMessage(String msg)           { this.errorMessage = msg; }

    @Override
    public String toString() {
        return String.format("Order{id='%s', customer='%s', product='%s', qty=%d, total=%s, status='%s'}",
            orderId, customerId, productId, quantity, totalAmount, status);
    }
}
