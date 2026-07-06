<?php
/**
 * API endpoint: /api/vendor/export.php
 * Export vendor statistics as CSV.
 * ?type=products|orders|revenue
 */
require_once __DIR__ . '/../config/database.php';

if ($_SERVER['REQUEST_METHOD'] !== 'GET') json(405, ['error' => 'GET only']);

$vendorId = getAuthUserId();
$type = $_GET['type'] ?? 'products';

try {
    $db = getDB();

    // Get vendor's shop
    $stmt = $db->prepare("SELECT id, name FROM shops WHERE vendor_id = ?");
    $stmt->execute([$vendorId]);
    $shop = $stmt->fetch(PDO::FETCH_ASSOC);
    if (!$shop) json(404, ['error' => 'Aucune boutique trouvée']);
    $shopId = $shop['id'];

    header('Content-Type: text/csv; charset=utf-8');
    header('Content-Disposition: attachment; filename="' . $shop['name'] . '_' . $type . '_' . date('Y-m-d') . '.csv"');
    $out = fopen('php://output', 'w');

    // BOM for Excel UTF-8
    fprintf($out, chr(0xEF).chr(0xBB).chr(0xBF));

    switch ($type) {
        case 'products':
            fputcsv($out, ['ID', 'Titre', 'Catégorie', 'Prix (FCFA)', 'Stock', 'Vendu', 'Revenu (FCFA)']);
            $stmt = $db->prepare("
                SELECT p.id, p.title, p.category, p.price, p.stock,
                       COALESCE(SUM(oi.quantity), 0) as total_sold,
                       COALESCE(SUM(oi.price * oi.quantity), 0) as total_revenue
                FROM products p
                LEFT JOIN order_items oi ON p.id = oi.product_id
                WHERE p.shop_id = ? AND p.deleted_at IS NULL
                GROUP BY p.id
                ORDER BY total_sold DESC
            ");
            $stmt->execute([$shopId]);
            while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
                fputcsv($out, [$row['id'], $row['title'], $row['category'], $row['price'], $row['stock'], $row['total_sold'], $row['total_revenue']]);
            }
            break;

        case 'orders':
            fputcsv($out, ['ID Commande', 'Client', 'Produit', 'Qté', 'Prix', 'Total', 'Statut', 'Date']);
            $stmt = $db->prepare("
                SELECT o.id, u.name as client, p.title as product, oi.quantity, oi.price,
                       (oi.price * oi.quantity) as total, o.status, o.created_at
                FROM orders o
                JOIN order_items oi ON o.id = oi.order_id
                JOIN products p ON oi.product_id = p.id
                JOIN users u ON o.user_id = u.id
                WHERE p.shop_id = ?
                ORDER BY o.created_at DESC
            ");
            $stmt->execute([$shopId]);
            while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
                fputcsv($out, [$row['id'], $row['client'], $row['product'], $row['quantity'], $row['price'], $row['total'], $row['status'], $row['created_at']]);
            }
            break;

        case 'revenue':
            fputcsv($out, ['Jour', 'Revenu (FCFA)', 'Commandes']);
            $stmt = $db->prepare("
                SELECT DATE(o.created_at) as day,
                       COALESCE(SUM(oi.price * oi.quantity), 0) as revenue,
                       COUNT(DISTINCT oi.order_id) as orders
                FROM orders o
                JOIN order_items oi ON o.id = oi.order_id
                JOIN products p ON oi.product_id = p.id
                WHERE p.shop_id = ? AND o.status = 'delivered'
                GROUP BY DATE(o.created_at)
                ORDER BY day ASC
            ");
            $stmt->execute([$shopId]);
            while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
                fputcsv($out, [$row['day'], $row['revenue'], $row['orders']]);
            }
            break;

        default:
            json(400, ['error' => 'Type invalide. Utilisez products, orders ou revenue']);
    }

    fclose($out);
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(['error' => $e->getMessage()]);
}
