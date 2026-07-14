<?php
require_once __DIR__ . '/../config/database.php';

try {
    $db = getDB();
    $stmt = $db->query('SELECT COUNT(*) as cnt FROM products');
    $row = $stmt->fetch();
    json(200, [
        'success' => true,
        'connected' => true,
        'products_count' => $row['cnt'],
        'db_name' => getenv('DB_NAME') ?: 'defaultdb',
        'host' => getenv('DB_HOST') ?: 'localhost',
        'port' => getenv('DB_PORT') ?: '3306',
    ]);
} catch (\Throwable $e) {
    json(500, [
        'success' => false,
        'error' => $e->getMessage(),
        'file' => basename($e->getFile()),
        'line' => $e->getLine(),
    ]);
}
