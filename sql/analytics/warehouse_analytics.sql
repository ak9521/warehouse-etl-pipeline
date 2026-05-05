-- ============================================================
--  Warehouse ETL Pipeline — Analytics Queries
-- ============================================================

-- 1. ORDER FULFILMENT RATE BY WAREHOUSE
SELECT
    warehouse_id,
    COUNT(*)                                               AS total_orders,
    SUM(CASE WHEN status = 'SHIPPED'   THEN 1 ELSE 0 END) AS shipped,
    SUM(CASE WHEN status = 'DELIVERED' THEN 1 ELSE 0 END) AS delivered,
    SUM(CASE WHEN status IN ('SHIPPED','DELIVERED') THEN 1 ELSE 0 END) AS fulfilled,
    ROUND(
        SUM(CASE WHEN status IN ('SHIPPED','DELIVERED') THEN 1.0 ELSE 0 END) / COUNT(*) * 100, 2
    )                                                      AS fulfilment_rate_pct,
    SUM(CASE WHEN status IN ('INVALID','INSUFFICIENT_STOCK','ERROR') THEN 1 ELSE 0 END) AS failed,
    SUM(total_amount)                                      AS total_revenue
FROM orders
GROUP BY warehouse_id
ORDER BY fulfilment_rate_pct DESC;


-- 2. DAILY ORDER VOLUME AND REVENUE TREND
SELECT
    DATE(order_date)                                       AS order_date,
    COUNT(*)                                               AS order_count,
    SUM(total_amount)                                      AS daily_revenue,
    AVG(total_amount)                                      AS avg_order_value,
    SUM(CASE WHEN status IN ('SHIPPED','DELIVERED') THEN 1 ELSE 0 END) AS fulfilled_count,
    -- 7-day rolling average revenue (window function)
    ROUND(AVG(SUM(total_amount)) OVER (
        ORDER BY DATE(order_date)
        ROWS BETWEEN 6 PRECEDING AND CURRENT ROW
    ), 2)                                                  AS rolling_7d_revenue
FROM orders
GROUP BY DATE(order_date)
ORDER BY order_date;


-- 3. TOP PRODUCTS BY REVENUE AND ORDER VOLUME
SELECT
    product_id,
    COUNT(*)                                               AS total_orders,
    SUM(quantity)                                          AS total_units_sold,
    SUM(total_amount)                                      AS total_revenue,
    ROUND(AVG(unit_price), 2)                             AS avg_unit_price,
    RANK() OVER (ORDER BY SUM(total_amount) DESC)         AS revenue_rank
FROM orders
WHERE status IN ('SHIPPED', 'DELIVERED')
GROUP BY product_id
ORDER BY total_revenue DESC
LIMIT 20;


-- 4. INVENTORY HEALTH REPORT — STOCK AT RISK
SELECT
    i.product_id,
    i.warehouse_id,
    i.quantity_available,
    i.quantity_reserved,
    (i.quantity_available - i.quantity_reserved)          AS effective_available,
    i.reorder_threshold,
    CASE
        WHEN (i.quantity_available - i.quantity_reserved) = 0 THEN 'OUT_OF_STOCK'
        WHEN (i.quantity_available - i.quantity_reserved) <= i.reorder_threshold THEN 'REORDER_NOW'
        WHEN (i.quantity_available - i.quantity_reserved) <= i.reorder_threshold * 2 THEN 'LOW_STOCK'
        ELSE 'HEALTHY'
    END                                                    AS stock_health,
    COUNT(o.order_id)                                      AS pending_orders
FROM inventory i
LEFT JOIN orders o
    ON i.product_id = o.product_id
    AND i.warehouse_id = o.warehouse_id
    AND o.status = 'PENDING'
GROUP BY i.product_id, i.warehouse_id, i.quantity_available, i.quantity_reserved, i.reorder_threshold
ORDER BY effective_available ASC;


-- 5. PIPELINE ERROR ANALYSIS
SELECT
    error_type,
    COUNT(*)                                               AS error_count,
    ROUND(COUNT(*) * 100.0 / SUM(COUNT(*)) OVER (), 2)   AS error_pct,
    MAX(occurred_at)                                       AS latest_occurrence
FROM order_errors
WHERE resolved = FALSE
GROUP BY error_type
ORDER BY error_count DESC;


-- 6. CUSTOMER ORDER FREQUENCY AND VALUE SEGMENTATION
SELECT
    customer_id,
    COUNT(*)                                               AS order_count,
    SUM(total_amount)                                      AS lifetime_value,
    AVG(total_amount)                                      AS avg_order_value,
    MIN(order_date)                                        AS first_order,
    MAX(order_date)                                        AS last_order,
    CASE
        WHEN COUNT(*) >= 10 AND SUM(total_amount) >= 100000 THEN 'PLATINUM'
        WHEN COUNT(*) >= 5  AND SUM(total_amount) >= 50000  THEN 'GOLD'
        WHEN COUNT(*) >= 2                                  THEN 'SILVER'
        ELSE 'BRONZE'
    END                                                    AS customer_tier
FROM orders
WHERE status IN ('SHIPPED', 'DELIVERED')
GROUP BY customer_id
ORDER BY lifetime_value DESC
LIMIT 50;
