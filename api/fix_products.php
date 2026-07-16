<?php
/**
 * Emergency fix: restore all products for vendor Will (shop_id=15).
 * Removes story flag and reactivates all his products.
 * Run via: https://dschang-market.onrender.com/fix_products.php
 */
require_once __DIR__ . '/config/database.php';

header('Content-Type: application/json; charset=utf-8');

$key = $_GET['key'] ?? '';
if ($key !== 'fixwill2026') {
    http_response_code(403);
    echo json_encode(['error' => 'Clé invalide']);
    exit;
}

try {
    $db = getDB();
    
    // 1. Fix ALL products for shop_id = 15 (Will Shopping)
    $stmt = $db->prepare('UPDATE products SET is_story = 0, is_active = 1 WHERE shop_id = ?');
    $stmt->execute([15]);
    $affected = $stmt->rowCount();
    
    // 2. Verify
    $stmt2 = $db->prepare('SELECT id, title, is_story, is_active, created_at FROM products WHERE shop_id = ?');
    $stmt2->execute([15]);
    $products = $stmt2->fetchAll();
    
    // Cast types
    foreach ($products as &$p) {
        $p['id'] = (int)$p['id'];
        $p['is_story'] = (bool)$p['is_story'];
        $p['is_active'] = (bool)$p['is_active'];
    }
    unset($p);
    
    echo json_encode([
        'success' => true,
        'affected' => $affected,
        'products_restored' => count($products),
        'products' => $products
    ], JSON_UNESCAPED_UNICODE);
    
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(['error' => $e->getMessage()]);
}
