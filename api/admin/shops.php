<?php
require_once __DIR__ . '/../config/database.php';

$method = $_SERVER['REQUEST_METHOD'];
$userId = getAuthUserId();

// Verify the user is admin
$db = getDB();
$stmt = $db->prepare('SELECT role FROM users WHERE id = ?');
$stmt->execute([$userId]);
$currentUser = $stmt->fetch();
if (!$currentUser || $currentUser['role'] !== 'admin') {
    json(403, ['error' => 'Accès refusé. Seuls les administrateurs peuvent accéder à cette ressource.']);
}

if ($method === 'GET') {
    // ── List all shops with vendor info ──
    $stmt = $db->query('
        SELECT 
            s.id, 
            s.name, 
            s.logo,
            s.location,
            s.phone,
            u.name AS vendor_name,
            u.email AS vendor_email,
            u.phone AS vendor_phone,
            s.category, 
            s.status,
            s.is_featured,
            COUNT(p.id) AS product_count,
            COALESCE(SUM(p.total_sales), 0) AS total_sales,
            s.is_verified, 
            s.created_at,
            s.updated_at
        FROM shops s
        JOIN users u ON s.vendor_id = u.id
        LEFT JOIN products p ON p.shop_id = s.id AND p.is_active = 1
        GROUP BY s.id
        ORDER BY s.created_at DESC
    ');
    $shops = $stmt->fetchAll();
    // Cast types
    foreach ($shops as &$s) {
        $s['is_verified'] = (bool)$s['is_verified'];
        $s['is_featured'] = (bool)$s['is_featured'];
        $s['id'] = (int)$s['id'];
        $s['product_count'] = (int)$s['product_count'];
        $s['total_sales'] = (int)$s['total_sales'];
    }
    unset($s);
    json(200, ['shops' => $shops]);

} elseif ($method === 'PUT') {
    // ── Update shop: verify, ban/unban, feature ──
    $shopId = (int)($_GET['id'] ?? 0);
    if ($shopId <= 0) json(400, ['error' => 'ID boutique requis']);

    // Verify the shop exists
    $stmt = $db->prepare('SELECT id, name, is_verified, status, is_featured FROM shops WHERE id = ?');
    $stmt->execute([$shopId]);
    $shop = $stmt->fetch();
    if (!$shop) json(404, ['error' => 'Boutique introuvable']);

    // Build dynamic update
    $updates = [];
    $params = [];

    // 1. Toggle verification
    if (isset($_GET['verified'])) {
        $v = filter_var($_GET['verified'], FILTER_VALIDATE_BOOLEAN) ? 1 : 0;
        $updates[] = 'is_verified = ?';
        $params[] = $v;
    }

    // 2. Ban / Unban
    if (isset($_GET['status'])) {
        $allowed = ['active', 'banned', 'suspended'];
        $status = $_GET['status'];
        if (!in_array($status, $allowed)) json(400, ['error' => 'Statut invalide. Utilisez: active, banned, suspended']);
        $updates[] = 'status = ?';
        $params[] = $status;
    }

    // 3. Toggle featured / promote
    if (isset($_GET['featured'])) {
        $f = filter_var($_GET['featured'], FILTER_VALIDATE_BOOLEAN) ? 1 : 0;
        $updates[] = 'is_featured = ?';
        $params[] = $f;
    }

    if (empty($updates)) {
        json(400, ['error' => 'Aucune modification spécifiée. Utilisez verified, status, ou featured.']);
    }

    $updates[] = 'updated_at = NOW()';
    $params[] = $shopId; // for WHERE id = ?

    $sql = 'UPDATE shops SET ' . implode(', ', $updates) . ' WHERE id = ?';
    $stmt = $db->prepare($sql);
    $stmt->execute($params);

    json(200, ['success' => true, 'message' => 'Boutique mise à jour']);

} elseif ($method === 'DELETE') {
    // ── Delete a shop entirely ──
    $shopId = (int)($_GET['id'] ?? 0);
    if ($shopId <= 0) json(400, ['error' => 'ID boutique requis']);

    // Verify the shop exists
    $stmt = $db->prepare('SELECT id, name, vendor_id FROM shops WHERE id = ?');
    $stmt->execute([$shopId]);
    $shop = $stmt->fetch();
    if (!$shop) json(404, ['error' => 'Boutique introuvable']);

    // Delete the shop (ON DELETE CASCADE handles products, reviews, wishlist, cart_items, favorite_shops)
    $stmt = $db->prepare('DELETE FROM shops WHERE id = ?');
    $stmt->execute([$shopId]);

    json(200, ['success' => true, 'message' => 'Boutique "' . $shop['name'] . '" supprimée avec succès']);

} else {
    json(405, ['error' => 'Méthode non autorisée']);
}
