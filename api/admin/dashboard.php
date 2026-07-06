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
$stmt = $db->prepare('SELECT role FROM users WHERE id = ?');
$stmt->execute([$adminId]);
$currentUser = $stmt->fetch();
if (!$currentUser || $currentUser['role'] !== 'admin') {
    json(403, ['error' => 'Accès refusé']);
}

try {
    // ─── 1. KPIs généraux ───
    
    // Total utilisateurs
    $stmt = $db->query("SELECT COUNT(*) as cnt FROM users WHERE role = 'buyer'");
    $totalBuyers = (int)$stmt->fetch()['cnt'];

    // Total vendeurs
    $stmt = $db->query("SELECT COUNT(*) as cnt FROM users WHERE role = 'vendor'");
    $totalVendorsCount = (int)$stmt->fetch()['cnt'];

    // Utilisateurs en ligne (dernières 5 minutes)
    $onlineCount = 0;
    try {
        $stmt = $db->query("SELECT COUNT(*) as cnt FROM users WHERE last_seen >= DATE_SUB(NOW(), INTERVAL 5 MINUTE)");
        if ($stmt) $onlineCount = (int)$stmt->fetch()['cnt'];
    } catch (Exception $e) {}
    
    // Nouveaux utilisateurs (30 derniers jours)
    $stmt = $db->query("SELECT COUNT(*) as cnt FROM users WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)");
    $newUsers30d = (int)$stmt->fetch()['cnt'];
    
    // Total boutiques
    $stmt = $db->query("SELECT COUNT(*) as cnt FROM shops");
    $totalShops = (int)$stmt->fetch()['cnt'];
    
    // Boutiques en attente de vérification
    $stmt = $db->query("SELECT COUNT(*) as cnt FROM shops WHERE is_verified = 0 AND status = 'active'");
    $pendingShops = (int)$stmt->fetch()['cnt'];
    
    // Boutiques bannies
    $stmt = $db->query("SELECT COUNT(*) as cnt FROM shops WHERE status = 'banned'");
    $bannedShops = (int)$stmt->fetch()['cnt'];
    
    // Total produits
    $stmt = $db->query("SELECT COUNT(*) as cnt FROM products WHERE is_active = 1");
    $totalProducts = (int)$stmt->fetch()['cnt'];
    
    // Total commandes
    $stmt = $db->query("SELECT COUNT(*) as cnt FROM orders");
    $totalOrders = (int)$stmt->fetch()['cnt'];
    
    // Commandes aujourd'hui
    $stmt = $db->query("SELECT COUNT(*) as cnt FROM orders WHERE DATE(created_at) = CURDATE()");
    $ordersToday = (int)$stmt->fetch()['cnt'];
    
    // Total revenu (plateforme, commissions potentielles)
    $stmt = $db->query("SELECT COALESCE(SUM(total_amount), 0) as total FROM orders WHERE status != 'cancelled'");
    $totalRevenue = (float)$stmt->fetch()['total'];
    
    // Revenu aujourd'hui
    $stmt = $db->query("SELECT COALESCE(SUM(total_amount), 0) as total FROM orders WHERE DATE(created_at) = CURDATE() AND status != 'cancelled'");
    $revenueToday = (float)$stmt->fetch()['total'];
    
    // ─── 2. Inscriptions par jour (30 derniers jours) ───
    $stmt = $db->query("
        SELECT DATE(created_at) as day, COUNT(*) as count
        FROM users
        WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
        GROUP BY DATE(created_at)
        ORDER BY day ASC
    ");
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
    $stmt = $db->query("
        SELECT DATE_FORMAT(created_at, '%Y-%m') as month, COALESCE(SUM(total_amount), 0) as revenue
        FROM orders
        WHERE status != 'cancelled' AND created_at >= DATE_SUB(NOW(), INTERVAL 12 MONTH)
        GROUP BY DATE_FORMAT(created_at, '%Y-%m')
        ORDER BY month ASC
    ");
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
    $stmt = $db->query("
        SELECT u.id, u.name, u.email, s.id as shop_id, s.name as shop_name,
               COUNT(DISTINCT o.id) as order_count,
               COALESCE(SUM(oi.price * oi.quantity), 0) as revenue
        FROM users u
        JOIN shops s ON u.id = s.vendor_id
        LEFT JOIN products p ON s.id = p.shop_id
        LEFT JOIN order_items oi ON p.id = oi.product_id
        LEFT JOIN orders o ON oi.order_id = o.id AND o.status != 'cancelled'
        WHERE u.role = 'vendor'
        GROUP BY u.id, s.id
        ORDER BY revenue DESC
        LIMIT 10
    ");
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
    $stmt = $db->query("
        SELECT p.id, p.title, p.price, s.name as shop_name,
               COALESCE(SUM(oi.quantity), 0) as total_sold,
               COALESCE(SUM(oi.price * oi.quantity), 0) as total_generated
        FROM products p
        JOIN shops s ON p.shop_id = s.id
        LEFT JOIN order_items oi ON p.id = oi.product_id
        WHERE p.is_active = 1
        GROUP BY p.id
        ORDER BY total_sold DESC
        LIMIT 10
    ");
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
    $stmt = $db->query("SELECT status, COUNT(*) as count FROM orders GROUP BY status");
    $ordersByStatus = [];
    foreach ($stmt->fetchAll() as $s) {
        $ordersByStatus[$s['status']] = (int)$s['count'];
    }
    
    // ─── 7. Répartition utilisateurs par rôle ───
    $stmt = $db->query("SELECT role, COUNT(*) as count FROM users GROUP BY role");
    $usersByRole = [];
    foreach ($stmt->fetchAll() as $r) {
        $usersByRole[$r['role']] = (int)$r['count'];
    }
    
    json(200, [
        'success' => true,
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
    json(500, ['error' => $e->getMessage()]);
}
