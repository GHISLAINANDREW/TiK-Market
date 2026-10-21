<?php
require_once __DIR__ . '/../config/database.php';

$method = $_SERVER['REQUEST_METHOD'];
$userId = getAuthUserId();

// Verify the user is admin
$db = getDB();
$stmt = $db->prepare('SELECT role, managed_city FROM users WHERE id = ?');
$stmt->execute([$userId]);
$currentUser = $stmt->fetch();
if (!$currentUser || !in_array($currentUser['role'], ['admin', 'super_admin', 'super'])) {
    json(403, ['error' => 'Accès refusé. Seuls les administrateurs peuvent accéder à cette ressource.']);
}

$isSuperAdmin = ($currentUser['role'] === 'super_admin' || $currentUser['role'] === 'super');
$managedCity = $currentUser['managed_city'];

if ($method === 'GET') {
    try {
        $existingCols = [];
        try {
            $colsStmt = $db->query("SHOW COLUMNS FROM shops");
            while ($row = $colsStmt->fetch()) {
                $existingCols[] = $row['Field'];
            }
        } catch (Exception $e) {}

        $selectCols = [
            's.id',
            's.name',
            's.logo',
            's.location',
            's.phone',
            "COALESCE(u.name, 'Inconnu') AS vendor_name",
            "COALESCE(u.email, '') AS vendor_email",
            "COALESCE(u.phone, '') AS vendor_phone",
            's.category',
            's.status',
            's.is_featured',
            'COUNT(p.id) AS product_count',
            'COALESCE(SUM(p.total_sales), 0) AS total_sales',
            's.is_verified',
            in_array('cni_front', $existingCols) ? 's.cni_front' : "'' AS cni_front",
            in_array('cni_back', $existingCols) ? 's.cni_back' : "'' AS cni_back",
            in_array('interior_photo', $existingCols) ? 's.interior_photo' : "'' AS interior_photo",
            in_array('exterior_photo', $existingCols) ? 's.exterior_photo' : "'' AS exterior_photo",
            in_array('sign_photo', $existingCols) ? 's.sign_photo' : "'' AS sign_photo",
            in_array('kyc_status', $existingCols) ? 's.kyc_status' : "'pending' AS kyc_status",
            in_array('niu', $existingCols) ? 's.niu' : "'' AS niu",
            in_array('latitude', $existingCols) ? 's.latitude' : '0.0 AS latitude',
            in_array('longitude', $existingCols) ? 's.longitude' : '0.0 AS longitude',
            's.created_at',
            's.updated_at'
        ];
        $selectClause = implode(', ', $selectCols);

        // ── List shops with vendor info ──
        $where = [];
        $params = [];

        // Un admin de ville ne voit que les boutiques de sa ville
        if (!$isSuperAdmin && $managedCity && strtolower($managedCity) !== 'global' && strtolower($managedCity) !== 'all') {
            $where[] = '(s.location LIKE ? OR u.managed_city LIKE ?)';
            $params[] = "%$managedCity%";
            $params[] = "%$managedCity%";
        }

        $whereClause = $where ? 'WHERE ' . implode(' AND ', $where) : '';

        $stmt = $db->prepare("
            SELECT $selectClause
            FROM shops s
            LEFT JOIN users u ON s.vendor_id = u.id
            LEFT JOIN products p ON p.shop_id = s.id AND p.is_active = 1
            $whereClause
            GROUP BY s.id
            ORDER BY s.created_at DESC
        ");
        $stmt->execute($params);
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
    } catch (Exception $e) {
        json(500, ['error' => 'Erreur serveur admin shops: ' . $e->getMessage()]);
    }
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
        $updates[] = 'kyc_status = ?';
        $params[] = $v ? 'verified' : 'rejected';

        // Notify vendor when validated
        if ($v) {
            try {
                $stmtV = $db->prepare('SELECT vendor_id, name FROM shops WHERE id = ?');
                $stmtV->execute([$shopId]);
                $sData = $stmtV->fetch();
                if ($sData) {
                    sendNotification((int)$sData['vendor_id'], "Boutique validée avec succès 🎉", "Votre boutique '{$sData['name']}' a été validée par l'administration ! Vous pouvez commencer à vendre.", 'system', $shopId);
                }
            } catch (Exception $e) {}
        }
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
