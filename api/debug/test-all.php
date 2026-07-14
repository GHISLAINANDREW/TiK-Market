<?php
header('Content-Type: application/json; charset=utf-8');
require_once __DIR__ . '/../config/database.php';

$results = [];

try {
    $db = getDB();
    $results['getDB'] = 'OK';
} catch (\Throwable $e) {
    $results['getDB'] = 'FAIL: ' . $e->getMessage();
    echo json_encode($results, JSON_PRETTY_PRINT);
    exit;
}

// Test 1: simple query
try {
    $stmt = $db->query('SELECT 1 AS test');
    $results['simple_query'] = $stmt->fetch()['test'];
} catch (\Throwable $e) {
    $results['simple_query'] = 'FAIL: ' . $e->getMessage();
}

// Test 2: ALTER TABLE (same as products.php)
try {
    $db->exec("ALTER TABLE products ADD COLUMN is_story TINYINT(1) DEFAULT 0 AFTER total_sales");
    $results['alter_table'] = 'OK';
} catch (\Throwable $e) {
    $results['alter_table'] = 'ALREADY EXISTS: ' . $e->getMessage();
}

// Test 3: products JOIN query
try {
    $stmt = $db->prepare("
        SELECT p.*, s.name AS shop_name
        FROM products p
        JOIN shops s ON p.shop_id = s.id
        JOIN users u ON s.vendor_id = u.id
        WHERE p.is_active = 1 AND s.status = 'active' AND u.status = 'active'
        LIMIT 1
    ");
    $stmt->execute();
    $row = $stmt->fetch();
    $results['join_query'] = $row ? 'OK - found product' : 'OK - no products';
    if ($row) {
        $results['sample_image'] = $row['image_url'] ?? 'no image field';
    }
} catch (\Throwable $e) {
    $results['join_query'] = 'FAIL: ' . $e->getMessage();
}

// Test 4: Image URLs
try {
    $stmt = $db->query('SELECT image_url FROM products LIMIT 3');
    $rows = $stmt->fetchAll();
    $results['image_urls'] = $rows;
} catch (\Throwable $e) {
    $results['image_urls'] = 'FAIL: ' . $e->getMessage();
}

echo json_encode($results, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
