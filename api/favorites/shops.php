<?php
/**
 * API: /api/favorites/shops.php
 * Manage favorite shops for the logged-in user.
 * GET    → list favorite shops
 * POST   → add a shop to favorites
 * DELETE → remove a shop from favorites
 */

require_once __DIR__ . '/../config/database.php';

$method = $_SERVER['REQUEST_METHOD'];
if ($method === 'OPTIONS') { json(200, []); }

$userId = getAuthUserId();
$shopId = isset($_GET['shop_id']) ? (int)$_GET['shop_id'] : 0;

try {
    $db = getDB();

    // Ensure table exists
    $db->exec("CREATE TABLE IF NOT EXISTS shop_favorites (
        id INT AUTO_INCREMENT PRIMARY KEY,
        user_id INT NOT NULL,
        shop_id INT NOT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        UNIQUE KEY unique_fav (user_id, shop_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

    if ($method === 'GET') {
        $stmt = $db->prepare("
            SELECT sf.id as fav_id, sf.shop_id, s.name, s.description, s.phone, s.location, s.logo, s.category,
                   s.is_verified, s.rating,
                   COUNT(p.id) as product_count,
                   COALESCE(SUM(p.total_sales), 0) as total_sales
            FROM shop_favorites sf
            JOIN shops s ON sf.shop_id = s.id
            LEFT JOIN products p ON p.shop_id = s.id AND p.is_active = 1
            WHERE sf.user_id = ?
            GROUP BY sf.id, s.id
            ORDER BY sf.created_at DESC
        ");
        $stmt->execute([$userId]);
        $favs = $stmt->fetchAll(PDO::FETCH_ASSOC);
        foreach ($favs as &$f) {
            $f['shop_id'] = (int)$f['shop_id'];
            $f['is_verified'] = (bool)$f['is_verified'];
            $f['rating'] = (float)$f['rating'];
            $f['product_count'] = (int)$f['product_count'];
            $f['total_sales'] = (int)$f['total_sales'];
        }
        json(200, ['favorites' => $favs]);
    }

    if ($method === 'POST') {
        if (!$shopId) json(400, ['error' => 'shop_id required']);
        // Check shop exists
        $stmt = $db->prepare("SELECT id FROM shops WHERE id = ?");
        $stmt->execute([$shopId]);
        if (!$stmt->fetch()) json(404, ['error' => 'Shop not found']);
        // Insert
        try {
            $stmt = $db->prepare("INSERT INTO shop_favorites (user_id, shop_id) VALUES (?, ?)");
            $stmt->execute([$userId, $shopId]);
            json(201, ['message' => 'Shop added to favorites', 'shop_id' => $shopId]);
        } catch (PDOException $e) {
            if ($e->getCode() == 23000) json(409, ['error' => 'Already in favorites']);
            else throw $e;
        }
    }

    if ($method === 'DELETE') {
        if (!$shopId) json(400, ['error' => 'shop_id required']);
        $stmt = $db->prepare("DELETE FROM shop_favorites WHERE user_id = ? AND shop_id = ?");
        $stmt->execute([$userId, $shopId]);
        json(200, ['message' => 'Removed from favorites']);
    }

    json(405, ['error' => 'Method not allowed']);
} catch (Exception $e) {
    json(500, ['error' => $e->getMessage()]);
}
