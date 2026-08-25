<?php
/**
 * API endpoint: /api/vendor/stats.php
 * Returns vendor dashboard statistics:
 *  - Overview: products, orders, revenue, sales
 *  - Daily revenue for last 7 days (for chart)
 *  - Top selling products
 *  - Orders by status distribution
 *  - Revenue by payment method
 */

require_once __DIR__ . '/../config/database.php';

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') { http_response_code(200); exit; }
if ($_SERVER['REQUEST_METHOD'] !== 'GET') json(405, ['error' => 'GET only']);

$vendorId = getAuthUserId();

try {
    $db = getDB();

    // 1. Get the vendor's shop
    $stmt = $db->prepare("SELECT id, name FROM shops WHERE vendor_id = ?");
    $stmt->execute([$vendorId]);
    $shop = $stmt->fetch(PDO::FETCH_ASSOC);
    if (!$shop) {
        json(404, ['error' => 'No shop found']);
        exit;
    }
    $shopId = $shop['id'];

    // 2. Overview stats
    // Products count
    $stmt = $db->prepare("SELECT COUNT(*) as cnt FROM products WHERE shop_id = ? AND is_active = 1");
    $stmt->execute([$shopId]);
    $productCount = (int)$stmt->fetch(PDO::FETCH_ASSOC)['cnt'];

    // Low stock products (< 5)
    $stmt = $db->prepare("SELECT COUNT(*) as cnt FROM products WHERE shop_id = ? AND stock < 5 AND stock > 0 AND is_active = 1");
    $stmt->execute([$shopId]);
    $lowStockCount = (int)$stmt->fetch(PDO::FETCH_ASSOC)['cnt'];

    // Out of stock
    $stmt = $db->prepare("SELECT COUNT(*) as cnt FROM products WHERE shop_id = ? AND stock = 0 AND is_active = 1");
    $stmt->execute([$shopId]);
    $outOfStockCount = (int)$stmt->fetch(PDO::FETCH_ASSOC)['cnt'];

    // Total orders for this shop's products
    $stmt = $db->prepare("
        SELECT COUNT(DISTINCT oi.order_id) as cnt, 
               COALESCE(SUM(oi.price * oi.quantity), 0) as total_rev,
               COALESCE(SUM(oi.quantity), 0) as total_items
        FROM order_items oi 
        JOIN products p ON oi.product_id = p.id 
        WHERE p.shop_id = ?
    ");
    $stmt->execute([$shopId]);
    $orderStats = $stmt->fetch(PDO::FETCH_ASSOC);
    $totalOrders = (int)$orderStats['cnt'];
    $totalRevenue = (float)$orderStats['total_rev'];
    $totalItemsSold = (int)$orderStats['total_items'];

    // 3. Daily revenue for last 7 days
    $stmt = $db->prepare("
        SELECT DATE(o.created_at) as day, 
               COALESCE(SUM(oi.price * oi.quantity), 0) as revenue,
               COUNT(DISTINCT oi.order_id) as orders_count
        FROM orders o
        JOIN order_items oi ON o.id = oi.order_id
        JOIN products p ON oi.product_id = p.id
        WHERE p.shop_id = ? AND o.created_at >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)
        GROUP BY DATE(o.created_at)
        ORDER BY day ASC
    ");
    $stmt->execute([$shopId]);
    $dailyData = $stmt->fetchAll(PDO::FETCH_ASSOC);

    // Fill missing days with zeros
    $dailyRevenue = [];
    for ($i = 6; $i >= 0; $i--) {
        $day = date('Y-m-d', strtotime("-$i days"));
        $found = false;
        foreach ($dailyData as $d) {
            if ($d['day'] === $day) {
                $dailyRevenue[] = [
                    'day' => $day,
                    'revenue' => (float)$d['revenue'],
                    'orders' => (int)$d['orders_count']
                ];
                $found = true;
                break;
            }
        }
        if (!$found) {
            $dailyRevenue[] = ['day' => $day, 'revenue' => 0.0, 'orders' => 0];
        }
    }

    // 4. Top selling products
    $stmt = $db->prepare("
        SELECT p.id, p.title, p.price, 
               COALESCE(SUM(oi.quantity), 0) as total_sold,
               COALESCE(SUM(oi.price * oi.quantity), 0) as total_generated
        FROM products p
        LEFT JOIN order_items oi ON p.id = oi.product_id
        WHERE p.shop_id = ? AND p.is_active = 1
        GROUP BY p.id, p.title, p.price
        ORDER BY total_sold DESC
        LIMIT 5
    ");
    $stmt->execute([$shopId]);
    $topProducts = $stmt->fetchAll(PDO::FETCH_ASSOC);
    $topProducts = array_map(function($p) {
        return [
            'id' => (int)$p['id'],
            'title' => $p['title'],
            'price' => (float)$p['price'],
            'total_sold' => (int)$p['total_sold'],
            'total_generated' => (float)$p['total_generated']
        ];
    }, $topProducts);

    // 5. Orders by status
    $stmt = $db->prepare("
        SELECT o.status, COUNT(DISTINCT o.id) as cnt
        FROM orders o
        JOIN order_items oi ON o.id = oi.order_id
        JOIN products p ON oi.product_id = p.id
        WHERE p.shop_id = ?
        GROUP BY o.status
    ");
    $stmt->execute([$shopId]);
    $statusData = $stmt->fetchAll(PDO::FETCH_ASSOC);
    $ordersByStatus = [];
    foreach ($statusData as $s) {
        $ordersByStatus[$s['status']] = (int)$s['cnt'];
    }

    // 6. Monthly revenue (last 6 months)
    $stmt = $db->prepare("
        SELECT DATE_FORMAT(o.created_at, '%Y-%m') as month, 
               COALESCE(SUM(oi.price * oi.quantity), 0) as revenue
        FROM orders o
        JOIN order_items oi ON o.id = oi.order_id
        JOIN products p ON oi.product_id = p.id
        WHERE p.shop_id = ? AND o.created_at >= DATE_SUB(CURDATE(), INTERVAL 6 MONTH)
        GROUP BY DATE_FORMAT(o.created_at, '%Y-%m')
        ORDER BY month ASC
    ");
    $stmt->execute([$shopId]);
    $monthlyData = $stmt->fetchAll(PDO::FETCH_ASSOC);
    $monthlyRevenue = array_map(function($m) {
        return [
            'month' => $m['month'],
            'revenue' => (float)$m['revenue']
        ];
    }, $monthlyData);

    json(200, [
        'success' => true,
        'shop_name' => $shop['name'],
        'overview' => [
            'product_count' => $productCount,
            'low_stock_count' => $lowStockCount,
            'out_of_stock_count' => $outOfStockCount,
            'total_orders' => $totalOrders,
            'total_revenue' => $totalRevenue,
            'total_items_sold' => $totalItemsSold
        ],
        'daily_revenue' => $dailyRevenue,
        'monthly_revenue' => $monthlyRevenue,
        'top_products' => $topProducts,
        'orders_by_status' => $ordersByStatus
    ]);

} catch (Exception $e) {
    error_log('[TiK-Market] API error: ' . $e->getMessage());
    json(500, ['error' => 'Une erreur interne est survenue']);
}
