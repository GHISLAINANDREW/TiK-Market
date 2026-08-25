<?php
/**
 * API endpoint: /api/admin/dashboard.php
 * Retourne les KPIs et graphiques pour le dashboard administrateur.
 * GET uniquement, réservé aux admins.
 */
require_once __DIR__ . '/../config/database.php';

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') { http_response_code(200); exit; }
if ($_SERVER['REQUEST_METHOD'] !== 'GET') json(405, ['error' => 'GET only']);

$adminId = getAuthUserId();
$db = getDB();

// Vérifier admin
$stmt = $db->prepare('SELECT role, managed_city FROM users WHERE id = ?');
$stmt->execute([$adminId]);
$currentUser = $stmt->fetch();
if (!$currentUser || !in_array($currentUser['role'], ['admin', 'super_admin'])) {
    json(403, ['error' => 'Accès refusé']);
}

$isSuperAdmin = ($currentUser['role'] === 'super_admin');
$requestedCity = $_GET['city'] ?? null;

// Un admin de ville ne peut voir que sa ville
if (!$isSuperAdmin && $currentUser['managed_city']) {
    $city = $currentUser['managed_city'];
} else {
    $city = $requestedCity;
}

try {
    // Helper pour ajouter le filtre de ville aux requêtes
    $cityFilter = $city ? " AND s.location LIKE :city " : "";
    $cityFilterUser = $city ? " AND location LIKE :city " : "";
    $cityParams = $city ? [':city' => "%$city%"] : [];

    // ─── 1. KPIs généraux ───
    
    // Total utilisateurs
    $stmt = $db->prepare("SELECT COUNT(*) as cnt FROM users WHERE role = 'buyer' $cityFilterUser");
    $stmt->execute($cityParams);
    $row = $stmt->fetch();
    $totalBuyers = $row ? (int)$row['cnt'] : 0;

    // Total vendeurs
    $stmt = $db->prepare("SELECT COUNT(*) as cnt FROM users WHERE role = 'vendor' $cityFilterUser");
    $stmt->execute($cityParams);
    $row = $stmt->fetch();
    $totalVendorsCount = $row ? (int)$row['cnt'] : 0;

    // Utilisateurs en ligne (dernières 5 minutes)
    $onlineCount = 0;
    try {
        $stmt = $db->prepare("SELECT COUNT(*) as cnt FROM users WHERE last_seen >= DATE_SUB(NOW(), INTERVAL 5 MINUTE) $cityFilterUser");
        $stmt->execute($cityParams);
        $row = $stmt->fetch();
        if ($row) $onlineCount = (int)$row['cnt'];
    } catch (Exception $e) {}

    // Nouveaux utilisateurs (30 derniers jours)
    $stmt = $db->prepare("SELECT COUNT(*) as cnt FROM users WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY) $cityFilterUser");
    $stmt->execute($cityParams);
    $row = $stmt->fetch();
    $newUsers30d = $row ? (int)$row['cnt'] : 0;

    // Total boutiques
    $stmt = $db->prepare("SELECT COUNT(*) as cnt FROM shops s WHERE 1=1 $cityFilter");
    $stmt->execute($cityParams);
    $row = $stmt->fetch();
    $totalShops = $row ? (int)$row['cnt'] : 0;

    // Boutiques en attente de vérification
    $stmt = $db->prepare("SELECT COUNT(*) as cnt FROM shops s WHERE is_verified = 0 AND status = 'active' $cityFilter");
    $stmt->execute($cityParams);
    $row = $stmt->fetch();
    $pendingShops = $row ? (int)$row['cnt'] : 0;

    // Boutiques bannies
    $stmt = $db->prepare("SELECT COUNT(*) as cnt FROM shops s WHERE status = 'banned' $cityFilter");
    $stmt->execute($cityParams);
    $row = $stmt->fetch();
    $bannedShops = $row ? (int)$row['cnt'] : 0;

    // Total produits
    $stmt = $db->prepare("SELECT COUNT(*) as cnt FROM products p JOIN shops s ON p.shop_id = s.id WHERE p.is_active = 1 $cityFilter");
    $stmt->execute($cityParams);
    $row = $stmt->fetch();
    $totalProducts = $row ? (int)$row['cnt'] : 0;
    
    // Total commandes
    $stmt = $db->prepare("
        SELECT COUNT(DISTINCT o.id) as cnt
        FROM orders o
        JOIN order_items oi ON o.id = oi.order_id
        JOIN products p ON oi.product_id = p.id
        JOIN shops s ON p.shop_id = s.id
        WHERE 1=1 $cityFilter
    ");
    $stmt->execute($cityParams);
    $row = $stmt->fetch();
    $totalOrders = $row ? (int)$row['cnt'] : 0;

    // Commandes aujourd'hui
    $stmt = $db->prepare("
        SELECT COUNT(DISTINCT o.id) as cnt
        FROM orders o
        JOIN order_items oi ON o.id = oi.order_id
        JOIN products p ON oi.product_id = p.id
        JOIN shops s ON p.shop_id = s.id
        WHERE DATE(o.created_at) = CURDATE() $cityFilter
    ");
    $stmt->execute($cityParams);
    $row = $stmt->fetch();
    $ordersToday = $row ? (int)$row['cnt'] : 0;

    // Total revenu
    $stmt = $db->prepare("
        SELECT COALESCE(SUM(oi.price * oi.quantity), 0) as total
        FROM order_items oi
        JOIN orders o ON oi.order_id = o.id
        JOIN products p ON oi.product_id = p.id
        JOIN shops s ON p.shop_id = s.id
        WHERE o.status != 'cancelled' $cityFilter
    ");
    $stmt->execute($cityParams);
    $row = $stmt->fetch();
    $totalRevenue = $row ? (float)$row['total'] : 0.0;

    // Revenu aujourd'hui
    $stmt = $db->prepare("
        SELECT COALESCE(SUM(oi.price * oi.quantity), 0) as total
        FROM order_items oi
        JOIN orders o ON oi.order_id = o.id
        JOIN products p ON oi.product_id = p.id
        JOIN shops s ON p.shop_id = s.id
        WHERE DATE(o.created_at) = CURDATE() AND o.status != 'cancelled' $cityFilter
    ");
    $stmt->execute($cityParams);
    $row = $stmt->fetch();
    $revenueToday = $row ? (float)$row['total'] : 0.0;
    
    // ─── 2. Inscriptions par jour (30 derniers jours) ───
    $stmt = $db->prepare("
        SELECT DATE(created_at) as day, COUNT(*) as count
        FROM users
        WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY) $cityFilterUser
        GROUP BY DATE(created_at)
        ORDER BY day ASC
    ");
    $stmt->execute($cityParams);
    $rawRegistrations = $stmt->fetchAll();
    $registrations = [];
    for ($i = 29; $i >= 0; $i--) {
        $day = date('Y-m-d', strtotime("-$i days"));
        $found = false;
        foreach ($rawRegistrations as $r) {
            if ($r['day'] === $day) {
                $registrations[] = ['day' => $day, 'count' => (int)$r['count']];
                $found = true; break;
            }
        }
        if (!$found) $registrations[] = ['day' => $day, 'count' => 0];
    }
    
    // ─── 3. Revenu par mois (12 derniers mois) ───
    $stmt = $db->prepare("
        SELECT DATE_FORMAT(o.created_at, '%Y-%m') as month, COALESCE(SUM(oi.price * oi.quantity), 0) as revenue
        FROM order_items oi
        JOIN orders o ON oi.order_id = o.id
        JOIN products p ON oi.product_id = p.id
        JOIN shops s ON p.shop_id = s.id
        WHERE o.status != 'cancelled' AND o.created_at >= DATE_SUB(NOW(), INTERVAL 12 MONTH) $cityFilter
        GROUP BY DATE_FORMAT(o.created_at, '%Y-%m')
        ORDER BY month ASC
    ");
    $stmt->execute($cityParams);
    $rawMonthly = $stmt->fetchAll();
    $monthlyRevenue = [];
    for ($i = 11; $i >= 0; $i--) {
        $month = date('Y-m', strtotime("-$i months"));
        $found = false;
        foreach ($rawMonthly as $m) {
            if ($m['month'] === $month) {
                $monthlyRevenue[] = ['month' => $month, 'revenue' => (float)$m['revenue']];
                $found = true; break;
            }
        }
        if (!$found) $monthlyRevenue[] = ['month' => $month, 'revenue' => 0.0];
    }
    
    // ─── 4. Top 10 vendeurs (par CA) ───
    $stmt = $db->prepare("
        SELECT u.id, u.name, u.email, s.id as shop_id, s.name as shop_name,
               COUNT(DISTINCT o.id) as order_count,
               COALESCE(SUM(oi.price * oi.quantity), 0) as revenue
        FROM users u
        JOIN shops s ON u.id = s.vendor_id
        LEFT JOIN products p ON s.id = p.shop_id
        LEFT JOIN order_items oi ON p.id = oi.product_id
        LEFT JOIN orders o ON oi.order_id = o.id AND o.status != 'cancelled'
        WHERE u.role = 'vendor' $cityFilter
        GROUP BY u.id, s.id
        ORDER BY revenue DESC
        LIMIT 10
    ");
    $stmt->execute($cityParams);
    $topVendors = $stmt->fetchAll();
    $topVendors = array_map(function($v) {
        return [
            'id' => (int)$v['id'],
            'name' => $v['name'],
            'email' => $v['email'],
            'shop_id' => (int)$v['shop_id'],
            'shop_name' => $v['shop_name'],
            'order_count' => (int)$v['order_count'],
            'revenue' => (float)$v['revenue']
        ];
    }, $topVendors);
    
    // ─── 5. Top 10 produits les plus vendus ───
    $stmt = $db->prepare("
        SELECT p.id, p.title, p.price, s.name as shop_name,
               COALESCE(SUM(oi.quantity), 0) as total_sold,
               COALESCE(SUM(oi.price * oi.quantity), 0) as total_generated
        FROM products p
        JOIN shops s ON p.shop_id = s.id
        LEFT JOIN order_items oi ON p.id = oi.product_id
        WHERE p.is_active = 1 $cityFilter
        GROUP BY p.id
        ORDER BY total_sold DESC
        LIMIT 10
    ");
    $stmt->execute($cityParams);
    $topProducts = $stmt->fetchAll();
    $topProducts = array_map(function($p) {
        return [
            'id' => (int)$p['id'],
            'title' => $p['title'],
            'price' => (float)$p['price'],
            'shop_name' => $p['shop_name'],
            'total_sold' => (int)$p['total_sold'],
            'total_generated' => (float)$p['total_generated']
        ];
    }, $topProducts);
    
    // ─── 6. Commandes par statut ───
    $stmt = $db->prepare("
        SELECT o.status, COUNT(DISTINCT o.id) as count
        FROM orders o
        JOIN order_items oi ON o.id = oi.order_id
        JOIN products p ON oi.product_id = p.id
        JOIN shops s ON p.shop_id = s.id
        WHERE 1=1 $cityFilter
        GROUP BY o.status
    ");
    $stmt->execute($cityParams);
    $ordersByStatus = [];
    foreach ($stmt->fetchAll() as $s) {
        $ordersByStatus[$s['status']] = (int)$s['count'];
    }
    
    // ─── 7. Répartition utilisateurs par rôle ───
    $stmt = $db->prepare("SELECT role, COUNT(*) as count FROM users WHERE 1=1 $cityFilterUser GROUP BY role");
    $stmt->execute($cityParams);
    $usersByRole = [];
    foreach ($stmt->fetchAll() as $r) {
        $usersByRole[$r['role']] = (int)$r['count'];
    }
    
    json(200, [
        'success' => true,
        'city' => $city,
        'role' => $currentUser['role'],
        'kpis' => [
            'total_users' => $totalBuyers,
            'total_vendors' => $totalVendorsCount,
            'online_users' => $onlineCount,
            'new_users_30d' => $newUsers30d,
            'total_shops' => $totalShops,
            'pending_shops' => $pendingShops,
            'banned_shops' => $bannedShops,
            'total_products' => $totalProducts,
            'total_orders' => $totalOrders,
            'orders_today' => $ordersToday,
            'total_revenue' => $totalRevenue,
            'revenue_today' => $revenueToday
        ],
        'registrations' => $registrations,
        'monthly_revenue' => $monthlyRevenue,
        'top_vendors' => $topVendors,
        'top_products' => $topProducts,
        'orders_by_status' => (object)$ordersByStatus,
        'users_by_role' => (object)$usersByRole
    ]);    
} catch (Exception $e) {
    error_log('[TiK-Market] API error: ' . $e->getMessage());
    json(500, ['error' => 'Une erreur interne est survenue']);
}
