package com.arunkarthik.warehouse.model;

import java.time.LocalDateTime;

/**
 * Inventory entity representing stock levels per product per warehouse.
 */
public class Inventory {

    private String inventoryId;
    private String productId;
    private String warehouseId;
    private Integer quantityAvailable;
    private Integer quantityReserved;
    private Integer reorderThreshold;
    private LocalDateTime lastUpdated;

    public Inventory() {}

    public Inventory(String productId, String warehouseId, Integer quantityAvailable, Integer reorderThreshold) {
        this.inventoryId = productId + "_" + warehouseId;
        this.productId = productId;
        this.warehouseId = warehouseId;
        this.quantityAvailable = quantityAvailable;
        this.quantityReserved = 0;
        this.reorderThreshold = reorderThreshold;
        this.lastUpdated = LocalDateTime.now();
    }

    // ── Business logic ────────────────────────────────────────────

    public boolean hasSufficientStock(int requested) {
        return (quantityAvailable - quantityReserved) >= requested;
    }

    public boolean needsReorder() {
        return (quantityAvailable - quantityReserved) <= reorderThreshold;
    }

    public boolean reserve(int quantity) {
        if (!hasSufficientStock(quantity)) return false;
        this.quantityReserved += quantity;
        this.lastUpdated = LocalDateTime.now();
        return true;
    }

    public void fulfil(int quantity) {
        this.quantityAvailable -= quantity;
        this.quantityReserved = Math.max(0, this.quantityReserved - quantity);
        this.lastUpdated = LocalDateTime.now();
    }

    public void restock(int quantity) {
        this.quantityAvailable += quantity;
        this.lastUpdated = LocalDateTime.now();
    }

    public int getEffectiveAvailable() {
        return quantityAvailable - quantityReserved;
    }

    // ── Getters & Setters ─────────────────────────────────────────

    public String getInventoryId()                      { return inventoryId; }
    public String getProductId()                        { return productId; }
    public void setProductId(String p)                  { this.productId = p; }
    public String getWarehouseId()                      { return warehouseId; }
    public void setWarehouseId(String w)                { this.warehouseId = w; }
    public Integer getQuantityAvailable()               { return quantityAvailable; }
    public void setQuantityAvailable(Integer q)         { this.quantityAvailable = q; }
    public Integer getQuantityReserved()                { return quantityReserved; }
    public void setQuantityReserved(Integer q)          { this.quantityReserved = q; }
    public Integer getReorderThreshold()                { return reorderThreshold; }
    public void setReorderThreshold(Integer t)          { this.reorderThreshold = t; }
    public LocalDateTime getLastUpdated()               { return lastUpdated; }
    public void setLastUpdated(LocalDateTime t)         { this.lastUpdated = t; }

    @Override
    public String toString() {
        return String.format("Inventory{product='%s', warehouse='%s', available=%d, reserved=%d, needsReorder=%s}",
            productId, warehouseId, quantityAvailable, quantityReserved, needsReorder());
    }
}
